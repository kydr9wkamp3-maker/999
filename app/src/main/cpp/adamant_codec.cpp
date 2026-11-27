#ifndef ADAMANT_STANDALONE
#include <jni.h>
#endif
#include <algorithm>
#include <array>
#include <cstdint>
#include <cstdlib>
#include <utility>
#include <limits>
#include <memory>
#include <queue>
#include <stdexcept>
#include <string>
#include <vector>

namespace {
constexpr uint16_t kVersion = 1;
constexpr uint16_t kTile = 64;
constexpr uint8_t kChannels = 4;
constexpr uint64_t kMaxRawBytes = 1024ULL * 1024ULL * 1024ULL;

struct Node {
    uint64_t frequency;
    int minSymbol;
    int serial;
    int symbol;
    Node* left;
    Node* right;
};
struct NodeOrder {
    bool operator()(const Node* a, const Node* b) const {
        if (a->frequency != b->frequency) return a->frequency > b->frequency;
        if (a->minSymbol != b->minSymbol) return a->minSymbol > b->minSymbol;
        return a->serial > b->serial;
    }
};
struct Huffman {
    std::vector<std::unique_ptr<Node>> storage;
    Node* root = nullptr;
    std::array<std::vector<uint8_t>, 256> codes;
};

Huffman makeHuffman(const std::array<uint64_t, 256>& frequencies) {
    Huffman h;
    std::priority_queue<Node*, std::vector<Node*>, NodeOrder> q;
    int serial = 0;
    for (int s = 0; s < 256; ++s) {
        if (!frequencies[s]) continue;
        h.storage.emplace_back(new Node{frequencies[s], s, serial++, s, nullptr, nullptr});
        q.push(h.storage.back().get());
    }
    if (q.empty()) throw std::runtime_error("empty Huffman input");
    while (q.size() > 1) {
        Node* a = q.top(); q.pop();
        Node* b = q.top(); q.pop();
        if (a->frequency > std::numeric_limits<uint64_t>::max() - b->frequency)
            throw std::runtime_error("frequency overflow");
        h.storage.emplace_back(new Node{a->frequency + b->frequency,
                                       std::min(a->minSymbol, b->minSymbol), serial++, -1, a, b});
        q.push(h.storage.back().get());
    }
    h.root = q.top();
    std::vector<uint8_t> path;
    auto visit = [&](auto&& self, Node* n) -> void {
        if (n->symbol >= 0) { h.codes[n->symbol] = path; return; }
        path.push_back(0); self(self, n->left); path.back() = 1; self(self, n->right); path.pop_back();
    };
    visit(visit, h.root);
    return h;
}

uint32_t crc32Range(const uint8_t* data, size_t size) {
    uint32_t crc = 0xFFFFFFFFu;
    for (size_t n = 0; n < size; ++n) {
        crc ^= data[n];
        for (int i = 0; i < 8; ++i) crc = (crc >> 1) ^ (0xEDB88320u & (0u - (crc & 1u)));
    }
    return ~crc;
}
uint32_t crc32(const std::vector<uint8_t>& data) { return crc32Range(data.data(), data.size()); }

template <typename T>
void putLE(std::vector<uint8_t>& out, T value) {
    for (size_t i = 0; i < sizeof(T); ++i) out.push_back(static_cast<uint8_t>((value >> (i * 8)) & 0xFF));
}
template <typename T>
T getLE(const std::vector<uint8_t>& in, size_t& pos) {
    if (pos + sizeof(T) > in.size()) throw std::runtime_error("truncated container");
    T value = 0;
    for (size_t i = 0; i < sizeof(T); ++i) value |= static_cast<T>(in[pos++]) << (i * 8);
    return value;
}

std::vector<uint8_t> encode(const std::vector<uint8_t>& pixels, uint32_t width, uint32_t height) {
    if (!width || !height || uint64_t(width) * height > kMaxRawBytes / kChannels ||
        uint64_t(width) * height * kChannels != pixels.size())
        throw std::runtime_error("invalid dimensions or input length");
    std::vector<uint8_t> residuals;
    residuals.reserve(pixels.size());
    const uint32_t tilesX = (width + kTile - 1) / kTile;
    const uint32_t tilesY = (height + kTile - 1) / kTile;
    for (uint32_t ty = 0; ty < tilesY; ++ty) for (uint32_t tx = 0; tx < tilesX; ++tx) {
        const uint32_t x0 = tx * kTile, y0 = ty * kTile;
        const uint32_t x1 = std::min(width, x0 + kTile), y1 = std::min(height, y0 + kTile);
        for (uint32_t y = y0; y < y1; ++y) for (uint32_t x = x0; x < x1; ++x) {
            const size_t i = (size_t(y) * width + x) * kChannels;
            for (uint32_t c = 0; c < kChannels; ++c) {
                uint8_t predictor = 0;
                if (x > x0) predictor = pixels[i - kChannels + c];
                else if (y > y0) predictor = pixels[i - size_t(width) * kChannels + c];
                residuals.push_back(static_cast<uint8_t>(pixels[i + c] - predictor));
            }
        }
    }

    std::array<uint64_t, 256> freq{};
    for (uint8_t b : residuals) ++freq[b];
    Huffman h = makeHuffman(freq);
    uint64_t bitCount = 0;
    for (uint8_t b : residuals) {
        const uint64_t n = h.codes[b].size();
        if (bitCount > std::numeric_limits<uint64_t>::max() - n) throw std::runtime_error("bitstream too large");
        bitCount += n;
    }
    std::vector<uint8_t> payload(static_cast<size_t>((bitCount + 7) / 8), 0);
    uint64_t bitPos = 0;
    for (uint8_t b : residuals) for (uint8_t bit : h.codes[b]) {
        if (bit) payload[bitPos / 8] |= static_cast<uint8_t>(1u << (7 - (bitPos % 8)));
        ++bitPos;
    }

    std::vector<uint8_t> out{'A', 'D', 'M', '1'};
    putLE<uint16_t>(out, kVersion); putLE<uint32_t>(out, width); putLE<uint32_t>(out, height);
    out.push_back(kChannels); putLE<uint16_t>(out, kTile);
    putLE<uint64_t>(out, pixels.size()); putLE<uint32_t>(out, crc32(pixels)); putLE<uint64_t>(out, bitCount);
    for (uint64_t f : freq) putLE<uint64_t>(out, f);
    out.insert(out.end(), payload.begin(), payload.end());
    return out;
}

std::vector<uint8_t> decode(const std::vector<uint8_t>& in) {
    if (in.size() < 4 || in[0] != 'A' || in[1] != 'D' || in[2] != 'M' || in[3] != '1')
        throw std::runtime_error("invalid ADAMANT prototype header");
    size_t p = 4;
    const uint16_t version = getLE<uint16_t>(in, p);
    const uint32_t width = getLE<uint32_t>(in, p), height = getLE<uint32_t>(in, p);
    if (p >= in.size()) throw std::runtime_error("truncated header");
    const uint8_t channels = in[p++];
    const uint16_t tile = getLE<uint16_t>(in, p);
    const uint64_t rawLen = getLE<uint64_t>(in, p);
    const uint32_t expectedCrc = getLE<uint32_t>(in, p);
    const uint64_t bitCount = getLE<uint64_t>(in, p);
    if (version != kVersion || channels != kChannels || tile != kTile || !width || !height ||
        uint64_t(width) * height > kMaxRawBytes / kChannels ||
        uint64_t(width) * height * channels != rawLen || rawLen > kMaxRawBytes)
        throw std::runtime_error("unsupported or invalid container metadata");
    std::array<uint64_t, 256> freq{};
    uint64_t sum = 0;
    for (auto& f : freq) {
        f = getLE<uint64_t>(in, p);
        if (sum > std::numeric_limits<uint64_t>::max() - f) throw std::runtime_error("frequency overflow");
        sum += f;
    }
    if (sum != rawLen || bitCount > uint64_t(in.size()) * 8 || p + (bitCount + 7) / 8 != in.size())
        throw std::runtime_error("invalid entropy stream length");
    Huffman h = makeHuffman(freq);
    std::vector<uint8_t> residuals;
    residuals.reserve(static_cast<size_t>(rawLen));
    uint64_t bitPos = 0;
    for (uint64_t n = 0; n < rawLen; ++n) {
        Node* node = h.root;
        while (node->symbol < 0) {
            if (bitPos >= bitCount) throw std::runtime_error("truncated Huffman data");
            const uint8_t bit = (in[p + bitPos / 8] >> (7 - (bitPos % 8))) & 1;
            ++bitPos;
            node = bit ? node->right : node->left;
            if (!node) throw std::runtime_error("invalid Huffman path");
        }
        residuals.push_back(static_cast<uint8_t>(node->symbol));
    }
    if (bitPos != bitCount) throw std::runtime_error("unused Huffman data");

    std::vector<uint8_t> pixels(static_cast<size_t>(rawLen));
    size_t rp = 0;
    const uint32_t tilesX = (width + kTile - 1) / kTile;
    const uint32_t tilesY = (height + kTile - 1) / kTile;
    for (uint32_t ty = 0; ty < tilesY; ++ty) for (uint32_t tx = 0; tx < tilesX; ++tx) {
        const uint32_t x0 = tx * kTile, y0 = ty * kTile;
        const uint32_t x1 = std::min(width, x0 + kTile), y1 = std::min(height, y0 + kTile);
        for (uint32_t y = y0; y < y1; ++y) for (uint32_t x = x0; x < x1; ++x) {
            const size_t i = (size_t(y) * width + x) * kChannels;
            for (uint32_t c = 0; c < kChannels; ++c) {
                uint8_t predictor = 0;
                if (x > x0) predictor = pixels[i - kChannels + c];
                else if (y > y0) predictor = pixels[i - size_t(width) * kChannels + c];
                pixels[i + c] = static_cast<uint8_t>(residuals[rp++] + predictor);
            }
        }
    }
    if (crc32(pixels) != expectedCrc) throw std::runtime_error("ADAMANT checksum mismatch");
    return pixels;
}

// CHIMERA-1 research experiment: a routed quantized base plus an optional exact XOR residual.
// This is deliberately a toy bounded-error profile, not a perceptual/JND codec.
constexpr uint16_t kChimeraVersion = 1;
constexpr uint8_t kChimeraExactFlag = 1;
constexpr uint8_t kChimeraQuantStep = 16;

std::pair<uint32_t, uint32_t> innerDimensions(const std::vector<uint8_t>& in) {
    if (in.size() < 14 || in[0] != 'A' || in[1] != 'D' || in[2] != 'M' || in[3] != '1')
        throw std::runtime_error("invalid inner ADM1 stream");
    size_t p = 4;
    if (getLE<uint16_t>(in, p) != kVersion) throw std::runtime_error("unsupported inner ADM1 version");
    return {getLE<uint32_t>(in, p), getLE<uint32_t>(in, p)};
}

uint8_t quantize16(uint8_t v) {
    return static_cast<uint8_t>(std::min(255, ((static_cast<int>(v) + 8) / kChimeraQuantStep) * kChimeraQuantStep));
}

std::vector<uint8_t> chimeraEncode(const std::vector<uint8_t>& pixels, uint32_t width, uint32_t height,
                                   bool exact, uint8_t tolerance) {
    if (tolerance > 8) throw std::runtime_error("CHIMERA tolerance must be in range 0..8");
    if (!width || !height || uint64_t(width) * height > kMaxRawBytes / kChannels ||
        uint64_t(width) * height * kChannels != pixels.size())
        throw std::runtime_error("invalid CHIMERA dimensions or input length");
    const uint32_t tilesX = (width + kTile - 1) / kTile;
    const uint32_t tilesY = (height + kTile - 1) / kTile;
    const uint64_t tileCount64 = uint64_t(tilesX) * tilesY;
    if (tileCount64 > std::numeric_limits<uint32_t>::max()) throw std::runtime_error("too many CHIMERA tiles");
    const uint32_t tileCount = static_cast<uint32_t>(tileCount64);
    std::vector<uint8_t> routeMap((tileCount + 7u) / 8u, 0);
    std::vector<uint8_t> base(pixels.size());

    uint32_t tileIndex = 0;
    for (uint32_t ty = 0; ty < tilesY; ++ty) for (uint32_t tx = 0; tx < tilesX; ++tx, ++tileIndex) {
        const uint32_t x0 = tx * kTile, y0 = ty * kTile;
        const uint32_t x1 = std::min(width, x0 + kTile), y1 = std::min(height, y0 + kTile);
        bool routeExact = false;
        for (uint32_t y = y0; y < y1 && !routeExact; ++y) for (uint32_t x = x0; x < x1 && !routeExact; ++x) {
            const size_t i = (size_t(y) * width + x) * kChannels;
            for (uint32_t c = 0; c < kChannels; ++c) {
                const int delta = std::abs(int(pixels[i + c]) - int(quantize16(pixels[i + c])));
                if (delta > tolerance) { routeExact = true; break; }
            }
        }
        if (routeExact) routeMap[tileIndex / 8] |= static_cast<uint8_t>(1u << (tileIndex % 8));
        for (uint32_t y = y0; y < y1; ++y) for (uint32_t x = x0; x < x1; ++x) {
            const size_t i = (size_t(y) * width + x) * kChannels;
            for (uint32_t c = 0; c < kChannels; ++c) base[i + c] = routeExact ? pixels[i + c] : quantize16(pixels[i + c]);
        }
    }

    auto baseStream = encode(base, width, height);
    std::vector<uint8_t> residualStream;
    if (exact) {
        std::vector<uint8_t> residual(pixels.size());
        for (size_t i = 0; i < pixels.size(); ++i) residual[i] = pixels[i] ^ base[i];
        residualStream = encode(residual, width, height);
    }

    std::vector<uint8_t> payload;
    payload.reserve(routeMap.size() + baseStream.size() + residualStream.size());
    payload.insert(payload.end(), routeMap.begin(), routeMap.end());
    payload.insert(payload.end(), baseStream.begin(), baseStream.end());
    payload.insert(payload.end(), residualStream.begin(), residualStream.end());

    std::vector<uint8_t> out{'C', 'H', 'M', '1'};
    putLE<uint16_t>(out, kChimeraVersion);
    putLE<uint32_t>(out, width); putLE<uint32_t>(out, height);
    putLE<uint16_t>(out, kTile);
    out.push_back(tolerance);
    out.push_back(exact ? kChimeraExactFlag : 0);
    putLE<uint32_t>(out, tileCount);
    putLE<uint64_t>(out, baseStream.size());
    putLE<uint64_t>(out, residualStream.size());
    putLE<uint32_t>(out, exact ? crc32(pixels) : 0);
    putLE<uint32_t>(out, crc32(payload));
    out.insert(out.end(), payload.begin(), payload.end());
    return out;
}

std::vector<uint8_t> chimeraDecode(const std::vector<uint8_t>& in) {
    constexpr size_t kHeaderBytes = 46;
    if (in.size() < kHeaderBytes || in[0] != 'C' || in[1] != 'H' || in[2] != 'M' || in[3] != '1')
        throw std::runtime_error("invalid CHIMERA header");
    size_t p = 4;
    const uint16_t version = getLE<uint16_t>(in, p);
    const uint32_t width = getLE<uint32_t>(in, p), height = getLE<uint32_t>(in, p);
    const uint16_t tile = getLE<uint16_t>(in, p);
    if (p + 2 > in.size()) throw std::runtime_error("truncated CHIMERA header");
    const uint8_t tolerance = in[p++], flags = in[p++];
    const uint32_t tileCount = getLE<uint32_t>(in, p);
    const uint64_t baseLen = getLE<uint64_t>(in, p), residualLen = getLE<uint64_t>(in, p);
    const uint32_t sourceCrc = getLE<uint32_t>(in, p), payloadCrc = getLE<uint32_t>(in, p);
    if (version != kChimeraVersion || tile != kTile || !width || !height || tolerance > 8 || (flags & ~kChimeraExactFlag) != 0 ||
        uint64_t(width) * height > kMaxRawBytes / kChannels ||
        tileCount != uint64_t((width + kTile - 1) / kTile) * ((height + kTile - 1) / kTile))
        throw std::runtime_error("unsupported or invalid CHIMERA metadata");
    const bool exact = (flags & kChimeraExactFlag) != 0;
    if ((exact && residualLen == 0) || (!exact && residualLen != 0) || (!exact && sourceCrc != 0))
        throw std::runtime_error("inconsistent CHIMERA mode/stream metadata");
    const uint64_t mapLen = (uint64_t(tileCount) + 7) / 8;
    if (baseLen > in.size() || residualLen > in.size() || mapLen + baseLen + residualLen != in.size() - kHeaderBytes)
        throw std::runtime_error("invalid CHIMERA payload lengths");
    const size_t mapStart = kHeaderBytes;
    const size_t baseStart = mapStart + static_cast<size_t>(mapLen);
    const size_t residualStart = baseStart + static_cast<size_t>(baseLen);
    if (crc32Range(in.data() + mapStart, in.size() - mapStart) != payloadCrc)
        throw std::runtime_error("CHIMERA payload checksum mismatch");
    if (tileCount % 8 != 0 && mapLen > 0) {
        const uint8_t used = static_cast<uint8_t>((1u << (tileCount % 8)) - 1u);
        if ((in[mapStart + mapLen - 1] & static_cast<uint8_t>(~used)) != 0)
            throw std::runtime_error("nonzero padding bits in CHIMERA route map");
    }
    std::vector<uint8_t> baseContainer(in.begin() + static_cast<ptrdiff_t>(baseStart),
                                       in.begin() + static_cast<ptrdiff_t>(residualStart));
    if (innerDimensions(baseContainer) != std::make_pair(width, height)) throw std::runtime_error("CHIMERA base dimensions mismatch");
    auto base = decode(baseContainer);
    if (base.size() != uint64_t(width) * height * kChannels) throw std::runtime_error("invalid CHIMERA base output size");
    if (!exact) return base;

    std::vector<uint8_t> residualContainer(in.begin() + static_cast<ptrdiff_t>(residualStart), in.end());
    if (innerDimensions(residualContainer) != std::make_pair(width, height)) throw std::runtime_error("CHIMERA residual dimensions mismatch");
    auto residual = decode(residualContainer);
    if (residual.size() != base.size()) throw std::runtime_error("invalid CHIMERA residual output size");
    for (size_t i = 0; i < base.size(); ++i) base[i] ^= residual[i];
    if (crc32(base) != sourceCrc) throw std::runtime_error("CHIMERA reconstructed source checksum mismatch");
    return base;
}

#ifndef ADAMANT_STANDALONE
void throwJava(JNIEnv* env, const char* message) {
    jclass cls = env->FindClass("java/lang/IllegalArgumentException");
    if (cls) env->ThrowNew(cls, message);
}
#endif
}

#ifndef ADAMANT_STANDALONE
extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_adamant_lab_AdamantNative_nativeEncode(JNIEnv* env, jobject, jbyteArray input, jint width, jint height) {
    if (!input || width <= 0 || height <= 0) { throwJava(env, "invalid input"); return nullptr; }
    const jsize len = env->GetArrayLength(input);
    std::vector<uint8_t> data(static_cast<size_t>(len));
    env->GetByteArrayRegion(input, 0, len, reinterpret_cast<jbyte*>(data.data()));
    try {
        auto result = encode(data, static_cast<uint32_t>(width), static_cast<uint32_t>(height));
        if (result.size() > static_cast<size_t>(std::numeric_limits<jsize>::max())) throw std::runtime_error("container too large for JNI");
        jbyteArray out = env->NewByteArray(static_cast<jsize>(result.size()));
        if (out) env->SetByteArrayRegion(out, 0, static_cast<jsize>(result.size()), reinterpret_cast<const jbyte*>(result.data()));
        return out;
    } catch (const std::exception& e) { throwJava(env, e.what()); return nullptr; }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_adamant_lab_AdamantNative_nativeDecode(JNIEnv* env, jobject, jbyteArray input) {
    if (!input) { throwJava(env, "empty container"); return nullptr; }
    const jsize len = env->GetArrayLength(input);
    std::vector<uint8_t> data(static_cast<size_t>(len));
    env->GetByteArrayRegion(input, 0, len, reinterpret_cast<jbyte*>(data.data()));
    try {
        auto result = decode(data);
        jbyteArray out = env->NewByteArray(static_cast<jsize>(result.size()));
        if (out) env->SetByteArrayRegion(out, 0, static_cast<jsize>(result.size()), reinterpret_cast<const jbyte*>(result.data()));
        return out;
    } catch (const std::exception& e) { throwJava(env, e.what()); return nullptr; }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_adamant_lab_AdamantNative_nativeChimeraEncode(JNIEnv* env, jobject, jbyteArray input, jint width,
                                                        jint height, jboolean exact, jint tolerance) {
    if (!input || width <= 0 || height <= 0 || tolerance < 0 || tolerance > 8) {
        throwJava(env, "invalid CHIMERA input or tolerance"); return nullptr;
    }
    const jsize len = env->GetArrayLength(input);
    std::vector<uint8_t> data(static_cast<size_t>(len));
    env->GetByteArrayRegion(input, 0, len, reinterpret_cast<jbyte*>(data.data()));
    try {
        auto result = chimeraEncode(data, static_cast<uint32_t>(width), static_cast<uint32_t>(height), exact == JNI_TRUE,
                                   static_cast<uint8_t>(tolerance));
        if (result.size() > static_cast<size_t>(std::numeric_limits<jsize>::max())) throw std::runtime_error("CHIMERA container too large for JNI");
        jbyteArray out = env->NewByteArray(static_cast<jsize>(result.size()));
        if (out) env->SetByteArrayRegion(out, 0, static_cast<jsize>(result.size()), reinterpret_cast<const jbyte*>(result.data()));
        return out;
    } catch (const std::exception& e) { throwJava(env, e.what()); return nullptr; }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_adamant_lab_AdamantNative_nativeChimeraDecode(JNIEnv* env, jobject, jbyteArray input) {
    if (!input) { throwJava(env, "empty CHIMERA container"); return nullptr; }
    const jsize len = env->GetArrayLength(input);
    std::vector<uint8_t> data(static_cast<size_t>(len));
    env->GetByteArrayRegion(input, 0, len, reinterpret_cast<jbyte*>(data.data()));
    try {
        auto result = chimeraDecode(data);
        if (result.size() > static_cast<size_t>(std::numeric_limits<jsize>::max())) throw std::runtime_error("CHIMERA decoded image too large for JNI");
        jbyteArray out = env->NewByteArray(static_cast<jsize>(result.size()));
        if (out) env->SetByteArrayRegion(out, 0, static_cast<jsize>(result.size()), reinterpret_cast<const jbyte*>(result.data()));
        return out;
    } catch (const std::exception& e) { throwJava(env, e.what()); return nullptr; }
}

#endif  // ADAMANT_STANDALONE guard
