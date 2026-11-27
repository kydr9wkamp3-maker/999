#define ADAMANT_STANDALONE
#include "../../app/src/main/cpp/adamant_codec.cpp"
#include <cassert>
#include <iostream>

int main() {
    const std::pair<uint32_t, uint32_t> sizes[] = {{131, 77}, {1, 1}, {64, 64}, {65, 65}, {129, 17}};
    for (const auto& [width, height] : sizes) {
        std::vector<uint8_t> pixels(static_cast<size_t>(width) * height * 4);
        for (size_t i = 0; i < pixels.size(); ++i) pixels[i] = static_cast<uint8_t>((i * 31 + i / 11) & 255);
        const auto container = encode(pixels, width, height);
        assert(decode(container) == pixels);
        std::cout << width << 'x' << height << " raw=" << pixels.size()
                  << " container=" << container.size() << " roundtrip=ok\n";
    }
    std::vector<uint8_t> solid(16 * 9 * 4, 42);
    assert(decode(encode(solid, 16, 9)) == solid);

    auto damaged = encode(solid, 16, 9);
    damaged[25] ^= 1; // CRC field
    bool rejected = false;
    try { (void)decode(damaged); } catch (const std::runtime_error&) { rejected = true; }
    assert(rejected);

    const uint32_t cw = 131, ch = 77;
    std::vector<uint8_t> source(static_cast<size_t>(cw) * ch * 4);
    for (size_t i = 0; i < source.size(); ++i) source[i] = static_cast<uint8_t>((i * 13 + i / 7) & 255);
    auto chimeraExact = chimeraEncode(source, cw, ch, true, 6);
    assert(chimeraDecode(chimeraExact) == source);
    auto chimeraBounded = chimeraDecode(chimeraEncode(source, cw, ch, false, 6));
    assert(chimeraBounded.size() == source.size());
    for (size_t i = 0; i < source.size(); ++i)
        assert(std::abs(int(source[i]) - int(chimeraBounded[i])) <= 6);

    std::vector<uint8_t> routeExactTile(64 * 64 * 4, 8); // step-16 quantizer error is 8 > tolerance 6
    auto routed = chimeraEncode(routeExactTile, 64, 64, false, 6);
    assert((routed[46] & 1u) == 1u);
    assert(chimeraDecode(routed) == routeExactTile);

    auto brokenChimera = chimeraExact;
    brokenChimera.back() ^= 1;
    rejected = false;
    try { (void)chimeraDecode(brokenChimera); } catch (const std::runtime_error&) { rejected = true; }
    assert(rejected);
    std::cout << "CHIMERA exact/residual, bounded-error and route-map checks: ok\n";
}
