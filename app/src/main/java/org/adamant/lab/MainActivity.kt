package org.adamant.lab

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.Locale
import kotlin.math.min

private enum class Screen(val title: String, val icon: String) {
    CAMERA("Камера", "📷"), GALLERY("Галерея", "▧"), COMPRESS("Сжатие", "⇄"), RESULT("Результат", "✓"), LOG("Журнал", "≡")
}
private data class CompressionAttempt(val codec: Codec, val record: ExperimentRecord?, val result: CompressionResult?, val error: String?)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AdamantApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdamantApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(Screen.CAMERA) }
    var selected by remember { mutableStateOf<ImageInput?>(null) }
    var result by remember { mutableStateOf<CompressionResult?>(null) }
    var selectedCodec by remember { mutableStateOf(Codec.ADAMANT) }
    var selectedMode by remember { mutableStateOf(Mode.EXACT) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var records by remember { mutableStateOf(ExperimentLog.read(context)) }
    var pendingCsv by remember { mutableStateOf("") }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(pendingCsv) }
                    ?: error("Не удалось открыть файл для записи")
            }.onSuccess { status = "CSV экспортирован" }.onFailure { status = "Ошибка экспорта CSV: ${it.message}" }
        }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { readImage(context.contentResolver, uri) } }
                    .onSuccess { selected = it; result = null; status = "Изображение загружено"; screen = Screen.GALLERY }
                    .onFailure { status = "Не удалось прочитать изображение: ${it.message}" }
            }
        }
    }

    MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF126E5B), secondary = Color(0xFF52665F), background = Color(0xFFF4F7F4), surface = Color.White
    )) {
        Scaffold(
            topBar = { TopAppBar(title = { Column { Text("ADAMANT Lab", fontWeight = FontWeight.Bold); Text("Исследовательский прототип", style = MaterialTheme.typography.labelSmall) } }) },
            bottomBar = {
                NavigationBar {
                    Screen.entries.forEach { item ->
                        NavigationBarItem(selected = screen == item, onClick = { screen = item },
                            icon = { Text(item.icon) }, label = { Text(item.title) })
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 10.dp)) {
                if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                when (screen) {
                    Screen.CAMERA -> CameraScreen(
                        onCaptured = { input -> selected = input; result = null; status = "Фото получено: JPEG (уже сжатый камерой)"; screen = Screen.GALLERY },
                        onError = { status = it }
                    )
                    Screen.GALLERY -> GalleryScreen(selected, onPick = { pickImage.launch(ActivityResultContracts.PickVisualMedia.ImageOnly) })
                    Screen.COMPRESS -> CompressScreen(
                        input = selected, codec = selectedCodec, mode = selectedMode, busy = busy,
                        onCodec = { selectedCodec = it }, onMode = { selectedMode = it },
                        onRun = { compareAll ->
                            val source = selected ?: return@CompressScreen
                            if (busy) return@CompressScreen
                            busy = true; status = if (compareAll) "Сравнение кодеков…" else "Сжатие…"
                            scope.launch {
                                val codecs = if (compareAll) Codec.entries.toList() else listOf(selectedCodec)
                                val outcomes = withContext(Dispatchers.IO) {
                                    codecs.map { codec ->
                                        try {
                                            val mode = if (compareAll) Mode.EXACT else selectedMode
                                            val item = CompressionEngine.compress(source, codec, mode)
                                            CompressionAttempt(codec, ExperimentRecord.from(source, mode, item), item, null)
                                        } catch (e: Exception) { CompressionAttempt(codec, null, null, e.message ?: e.javaClass.simpleName) }
                                    }
                                }
                                val success = outcomes.filter { it.result != null }
                                success.forEach { ExperimentLog.append(context, it.record!!) }
                                records = ExperimentLog.read(context)
                                result = success.lastOrNull()?.result
                                val failures = outcomes.filter { it.error != null }
                                status = buildString {
                                    append("Готово: ${success.size} результат(а)")
                                    if (failures.isNotEmpty()) append("; недоступно/ошибка: ").append(failures.joinToString { "${it.codec.display}: ${it.error}" })
                                }
                                busy = false
                                if (success.isNotEmpty()) screen = Screen.RESULT
                            }
                        }
                    )
                    Screen.RESULT -> ResultScreen(selected, result)
                    Screen.LOG -> LogScreen(records, onExport = {
                        pendingCsv = ExperimentLog.toCsv(records)
                        createCsv.launch("adamant-experiments.csv")
                    }, onClear = { ExperimentLog.clear(context); records = emptyList(); status = "Журнал очищен" })
                }
            }
        }
    }
}

@Composable
private fun GalleryScreen(input: ImageInput?, onPick: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Импорт изображения", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onPick) { Text("Выбрать из галереи") }
        if (input != null) {
            ImagePreview(input.bitmap, Modifier.fillMaxWidth().height(300.dp))
            InfoCard("${input.name}\n${input.bitmap.width} × ${input.bitmap.height}\n${input.mime} · ${formatBytes(input.bytes.size.toLong())}")
        } else Text("Выберите PNG или другое изображение через системный picker. Доступ к библиотеке целиком не запрашивается.")
    }
}

@Composable
private fun CompressScreen(input: ImageInput?, codec: Codec, mode: Mode, busy: Boolean,
                           onCodec: (Codec) -> Unit, onMode: (Mode) -> Unit, onRun: (Boolean) -> Unit) {
    var codecOpen by remember { mutableStateOf(false) }
    var modeOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Параметры эксперимента", style = MaterialTheme.typography.headlineSmall)
        if (input == null) InfoCard("Сначала сделайте снимок или выберите изображение на экране «Галерея».")
        else InfoCard("${input.name} · ${input.bitmap.width} × ${input.bitmap.height} · ${formatBytes(input.bytes.size.toLong())}")
        Text("Режим")
        Box {
            OutlinedButton(onClick = { modeOpen = true }) { Text(mode.display) }
            DropdownMenu(expanded = modeOpen, onDismissRequest = { modeOpen = false }) {
                Mode.entries.forEach { m -> DropdownMenuItem(text = { Text(m.display) }, onClick = { onMode(m); modeOpen = false }) }
            }
        }
        val modeSupported = when {
            mode == Mode.EXACT -> true
            codec == Codec.CHIMERA && mode == Mode.BOUNDED_ERROR -> true
            else -> false
        }
        if (!modeSupported) InfoCard("Этот режим для выбранного кодека не реализован. Visual-lossless и Progressive пока недоступны; для CHIMERA есть отдельный Bounded-error (lossy), без заявления о perceptual-lossless.")
        if (mode == Mode.BOUNDED_ERROR && codec == Codec.CHIMERA) InfoCard("CHIMERA lab: route tile losslessly if 4-bit quantization exceeds 6/255 channel error; otherwise use quantized base. This is a numeric bound, not a visual/JND guarantee.")
        Text("Кодек")
        Box {
            OutlinedButton(onClick = { codecOpen = true }) { Text(codec.display) }
            DropdownMenu(expanded = codecOpen, onDismissRequest = { codecOpen = false }) {
                Codec.entries.forEach { c -> DropdownMenuItem(text = { Text(c.display) }, onClick = { onCodec(c); codecOpen = false }) }
            }
        }
        Button(enabled = input != null && !busy && modeSupported, onClick = { onRun(false) }) { Text("Сжать выбранным кодеком") }
        OutlinedButton(enabled = input != null && !busy, onClick = { onRun(true) }) { Text("Сравнить все кодеки · Exact") }
        if (busy) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.CircularProgressIndicator(Modifier.size(24.dp)); Text("Обработка на устройстве…")
        }
        Text("ADAMANT-1 prototype использует обратимое преобразование RGBA и Huffman-кодирование. Exact сравнивает SHA-256 нормализованных RGBA-пикселей, не байты PNG/JPEG исходного файла.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ResultScreen(input: ImageInput?, result: CompressionResult?) {
    var showDiff by remember { mutableStateOf(false) }
    var diff by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Результат проверки", style = MaterialTheme.typography.headlineSmall)
        if (input == null || result == null) {
            InfoCard("Результат появится после сжатия.")
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ImagePreview(input.bitmap, Modifier.weight(1f).height(170.dp))
            ImagePreview(if (showDiff) (diff ?: makeDiff(input.bitmap, result.decoded).also { diff = it }) else result.decoded,
                Modifier.weight(1f).height(170.dp))
        }
        TextButton(onClick = { showDiff = !showDiff }) { Text(if (showDiff) "Показать результат" else "Показать разницу") }
        val ratio = if (result.encoded.isEmpty()) "—" else String.format(Locale.US, "%.3f×", input.bytes.size.toDouble() / result.encoded.size)
        InfoCard("Кодек: ${result.codec.display}\nРежим: ${result.mode.display}\nИсходный файл: ${formatBytes(input.bytes.size.toLong())}\nКонтейнер/выход: ${formatBytes(result.encoded.size.toLong())}\nОтношение размеров: $ratio\nКодирование: ${fmt(result.encodeMs)} мс · декодирование: ${fmt(result.decodeMs)} мс\nPSNR RGBA: ${result.quality.psnr?.let { fmt(it) + " dB" } ?: if (result.bitExact) "∞ (идентичные пиксели)" else "не рассчитан"}\nSSIM глобальный: ${result.quality.ssim?.let { fmt(it) } ?: "не рассчитан"}\nМаксимальная ошибка сэмпла: ${result.quality.maxSampleError?.toString() ?: "не рассчитана"} / 255\nПиковая память: не измерена в MVP")
        InfoCard("SHA-256 исходного файла: ${result.sourceFileSha}\nSHA-256 RGBA до кодирования: ${result.sourceSha}\nSHA-256 RGBA после декодирования: ${result.decodedSha}\nСтатус пикселей: ${if (result.bitExact) "бит-в-бит совпадают (RGBA)" else "есть отличия"}")
        if (result.note.isNotBlank()) Text(result.note, style = MaterialTheme.typography.bodySmall)
        Button(onClick = {
            val share = "ADAMANT Lab report\nCodec: ${result.codec.display}\nMode: ${result.mode.display}\nSize: ${input.bytes.size} -> ${result.encoded.size} bytes\nEncode/decode: ${fmt(result.encodeMs)}/${fmt(result.decodeMs)} ms\nRGBA SHA-256 match: ${result.bitExact}\n${result.note}"
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, share)
            context.startActivity(android.content.Intent.createChooser(intent, "Поделиться отчётом"))
        }) { Text("Поделиться отчётом") }
    }
}

@Composable
private fun LogScreen(records: List<ExperimentRecord>, onExport: () -> Unit, onClear: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Эксперименты · ${records.size}", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onExport, enabled = records.isNotEmpty()) { Text("Экспорт CSV") }
            OutlinedButton(onClick = onClear, enabled = records.isNotEmpty()) { Text("Очистить") }
        }
        if (records.isEmpty()) Text("Записей пока нет.") else {
            Row(Modifier.horizontalScroll(rememberScrollState())) { Text("Время | Изображение | Кодек | Режим | байты → байты | ratio | ms encode/decode | PSNR | SSIM | SHA\n", fontWeight = FontWeight.Bold) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records.reversed()) { r ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${r.imageName} · ${r.width}×${r.height}", fontWeight = FontWeight.Bold)
                            Text("${r.codec} · ${r.mode} · ${r.originalBytes} → ${r.compressedBytes} байт · ratio ${r.ratio?.let { fmt(it) } ?: "—"}")
                            Text("${fmt(r.encodeMs)}/${fmt(r.decodeMs)} мс · PSNR ${r.psnr?.let { fmt(it) } ?: "∞/—"} · SSIM ${r.ssim?.let { fmt(it) } ?: "—"} · maxΔ ${r.maxSampleError?.toString() ?: "—"}/255 · SHA ${if (r.shaMatch) "совпадает" else "отличается"}")
                            Text(r.timestamp, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraScreen(onCaptured: (ImageInput) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var lensFront by remember { mutableStateOf(false) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Камера", style = MaterialTheme.typography.headlineSmall)
        if (hasPermission) {
            CameraPreview(lensFront, onCaptureReady = { capture = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { lensFront = !lensFront }) { Text("Переключить камеру") }
                Button(onClick = {
                    val imageCapture = capture
                    if (imageCapture == null) { onError("Камера ещё запускается"); return@Button }
                    val file = File(context.cacheDir, "adamant-camera-${System.currentTimeMillis()}.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            if (!scope.isActive) { file.delete(); return }
                            scope.launch(Dispatchers.IO) {
                                val captured = runCatching {
                                    require(file.length() <= currentInputLimitBytes()) { "Снимок превышает динамический лимит входного файла MVP" }
                                    val bytes = file.readBytes()
                                    val decoded = decodeImageSafely(bytes)
                                    val notes = buildList {
                                        add("CameraX capture: JPEG already compressed by camera pipeline; not sensor RAW/DNG.")
                                        if (decoded.second) add("EXIF orientation normalized for pixel processing.")
                                    }.joinToString(" ")
                                    ImageInput(decoded.first, bytes, file.name, "image/jpeg", notes)
                                }
                                file.delete()
                                withContext(Dispatchers.Main) {
                                    captured.onSuccess(onCaptured).onFailure { onError("Ошибка обработки снимка: ${it.message}") }
                                }
                            }
                        }
                        override fun onError(exception: ImageCaptureException) { onError("Ошибка камеры: ${exception.message}") }
                    })
                }) { Text("Сделать фото") }
            }
            Text("Сохранённый CameraX кадр обычно JPEG: это не сырые данные сенсора. RAW/DNG в MVP не реализован.", style = MaterialTheme.typography.bodySmall)
        } else {
            InfoCard("Для предпросмотра и съёмки требуется разрешение камеры.")
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Разрешить камеру") }
        }
    }
}

@Composable
private fun CameraPreview(front: Boolean, onCaptureReady: (ImageCapture?) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    DisposableEffect(previewView, front, lifecycleOwner) {
        var provider: ProcessCameraProvider? = null
        val view = previewView
        if (view != null) {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                runCatching {
                    provider = future.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                    val photo = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                    val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                    provider?.unbindAll()
                    provider?.bindToLifecycle(lifecycleOwner, selector, preview, photo)
                    onCaptureReady(photo)
                }.onFailure { onCaptureReady(null) }
            }, ContextCompat.getMainExecutor(context))
        }
        onDispose { provider?.unbindAll(); onCaptureReady(null) }
    }
    AndroidView(factory = { ctx -> PreviewView(ctx).also { it.scaleType = PreviewView.ScaleType.FILL_CENTER; previewView = it } },
        modifier = Modifier.fillMaxWidth().height(350.dp))
}

@Composable
private fun ImagePreview(bitmap: Bitmap, modifier: Modifier = Modifier) {
    Image(bitmap.asImageBitmap(), contentDescription = "Предпросмотр изображения", modifier = modifier.background(Color(0xFFE8ECE8)))
}

@Composable
private fun InfoCard(text: String) {
    Card(Modifier.fillMaxWidth()) { Text(text, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium) }
}

private const val MAX_INPUT_BYTES = 128L * 1024L * 1024L
private const val MAX_MVP_PIXELS = 12_000_000L

private fun currentInputLimitBytes(): Long = minOf(MAX_INPUT_BYTES, Runtime.getRuntime().maxMemory() / 8L)

private fun readImage(resolver: ContentResolver, uri: Uri): ImageInput {
    val bytes = resolver.openInputStream(uri)?.use { readBounded(it, currentInputLimitBytes()) } ?: error("Cannot open image")
    val (bitmap, orientationApplied) = decodeImageSafely(bytes)
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    } ?: "gallery-image"
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    val notes = if (orientationApplied) "EXIF orientation normalized for pixel processing." else ""
    return ImageInput(bitmap, bytes, name, mime, notes)
}

private fun readBounded(input: InputStream, maxBytes: Long): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    var count = 0L
    while (true) {
        val n = input.read(buffer)
        if (n < 0) break
        count += n
        require(count <= maxBytes) { "Файл больше динамического лимита ${maxBytes / (1024 * 1024)} MiB: ограничение MVP" }
        output.write(buffer, 0, n)
    }
    return output.toByteArray()
}

/** Bounds-checks dimensions before allocating a Bitmap; exact mode never silently downsamples. */
private fun decodeImageSafely(bytes: ByteArray): Pair<Bitmap, Boolean> {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Формат изображения не распознан" }
    val pixels = bounds.outWidth.toLong() * bounds.outHeight.toLong()
    val heapBudgetPixels = Runtime.getRuntime().maxMemory() / 40L
    val allowedPixels = minOf(MAX_MVP_PIXELS, heapBudgetPixels)
    require(pixels <= allowedPixels) {
        "Изображение ${bounds.outWidth}×${bounds.outHeight} превышает безопасный MVP-предел ${allowedPixels} пикселей; автоматическое уменьшение отключено для Exact"
    }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: error("Не удалось декодировать изображение")
    val orientation = runCatching {
        ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
    }
    val hasOrientationTransform = orientation in 2..8
    if (!hasOrientationTransform) return decoded to false
    val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    if (oriented !== decoded) decoded.recycle()
    return oriented to true
}

private fun makeDiff(a: Bitmap, b: Bitmap): Bitmap {
    val w = min(a.width, b.width); val h = min(a.height, b.height)
    val aa = IntArray(w * h); val bb = IntArray(w * h); val out = IntArray(w * h)
    a.getPixels(aa, 0, w, 0, 0, w, h); b.getPixels(bb, 0, w, 0, 0, w, h)
    for (i in out.indices) {
        val dr = kotlin.math.abs(android.graphics.Color.red(aa[i]) - android.graphics.Color.red(bb[i]))
        val dg = kotlin.math.abs(android.graphics.Color.green(aa[i]) - android.graphics.Color.green(bb[i]))
        val db = kotlin.math.abs(android.graphics.Color.blue(aa[i]) - android.graphics.Color.blue(bb[i]))
        out[i] = android.graphics.Color.rgb((dr * 4).coerceAtMost(255), (dg * 4).coerceAtMost(255), (db * 4).coerceAtMost(255))
    }
    return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KiB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.2f MiB", bytes / (1024.0 * 1024.0))
}
private fun fmt(value: Double): String = String.format(Locale.US, "%.3f", value)
