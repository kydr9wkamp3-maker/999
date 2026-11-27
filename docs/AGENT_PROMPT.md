# Рабочий промт для следующего coding agent

Ты работаешь с ADAMANT Lab — исследовательским Android-прототипом, а не готовым универсальным кодеком. Перед правками прочитай `AGENTS.md`, `README.md`, `docs/VISION.md`, `SECURITY.md`, `PRIVACY.md`, `docs/SPEC.md`, `docs/FORMAT.md`, `docs/API.md`, `docs/DEVICE_PROFILE.md`, `docs/TESTPLAN.md`, `docs/BENCHMARK.md`, `docs/RELEASE.md` и `.github/workflows/android.yml`. Для гибридных идей прочитай `docs/RESEARCH_TRACK_CHIMERA.md` и `docs/CHIMERA_FORMAT.md`: реализован только минимальный CHM1 tile-router + quantized-base + optional XOR-residual эксперимент; diffusion, INR, semantic/machine channel и accelerator backend не реализованы. Не обобщай этот прототип на все идеи CHIMERA.

## Непереговорные правила

1. Не выдумывай метрики, результаты сборки, тесты, API-возможности телефона, статьи и проценты.
2. Если тест не запускался, обозначай его **не проверено**. Синтетический native round-trip не равен Android APK/device test.
3. Никогда не подменяй ADAMANT JPEG/WebP. Отдельно называй кодек и его режим.
4. Не называй визуально похожее изображение bit-exact. В текущем MVP `bit_exact` относится к normalized RGBA; исходный файл хешируется отдельно.
5. Не загружай изображение/CSV и не добавляй `INTERNET`, telemetry, ads или cloud SDK.
6. Не коммить пользовательские фото, DNG, keystore, APK, секреты и закрытые датасеты.
7. Работай на ветке, выданной Arena; не переименовывай, не переключай и не создавай другую ветку.

## Целевой телефон

Пользователь предоставил скриншот moto g85 5G: 12 GB физической RAM, 58/256 GB storage, rear 50 MP + 8 MP, front 32 MP, display 2400×1080. Это не подтверждает Android API, app heap, активные CameraX resolutions, SoC или RAW/DNG. Используй `docs/DEVICE_PROFILE.md`; не вставляй скриншотные значения как runtime results.

## Перед завершением изменения

1. Сформулируй риск, влияющий на точность, память, совместимость, офлайн-работу и формат.
2. Запусти подходящие доступные тесты: `./tests/native/run.sh`, `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew assembleDebug`.
3. Для камеры/памяти/thermal поведения нужны реальные device tests; не выдавай сборку на runner за них.
4. При изменении контейнера обнови `docs/FORMAT.md`, fixtures и версию формата только с обоснованием совместимости.
5. Для release workflow не обходи отсутствие signing secrets и не выпускай неподписанный APK под видом релиза.
6. Заверши отчётом: сделано, файлы, команды, результаты, непройденные/непроверенные тесты, ограничения, следующий шаг.
