# Источники и что проверять перед интеграцией

Ссылки ниже — отправные точки документации, а не утверждение, что библиотека уже подключена или проверена в ADAMANT Lab. Версии AndroidX/NDK фиксируются в Gradle; перед upgrade проверьте release notes, minSdk, ABI, лицензию и воспроизводимость сборки.

## Android / CameraX / Compose

- [CameraX overview](https://developer.android.com/media/camera/camerax) — use cases, жизненный цикл, камеры.
- [CameraX release notes](https://developer.android.com/jetpack/androidx/releases/camera) — проверять доступность API и совместимость версии.
- [ImageCaptureCapabilities](https://developer.android.com/reference/androidx/camera/core/ImageCaptureCapabilities) — проверять форматы захвата на устройстве; наличие мегапикселей в спецификации телефона не подтверждает RAW/DNG.
- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photopicker) — выбор конкретных изображений без запроса полного доступа к медиатеке.
- [Android 13 media permission changes](https://developer.android.com/about/versions/13/behavior-changes-13) — разрешения изображений и видео.
- [Jetpack Compose](https://developer.android.com/develop/ui/compose/documentation) — UI.
- [Add C/C++ code to Android project](https://developer.android.com/studio/projects/add-native-code) — JNI и организация native-кода.
- [NDK CMake guide](https://developer.android.com/ndk/guides/cmake) — сборка нативной библиотеки.
- [Install NDK and CMake](https://developer.android.com/studio/projects/install-ndk) — установка локального toolchain.

## Сборка и GitHub

- [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) — версия Gradle должна быть зафиксирована и воспроизводима.
- [Gradle configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html) — включать после проверки совместимости задач/плагинов; он не требует автоматически заводить секрет `GRADLE_ENCRYPTION_KEY` для обычного CI-кэша.
- [GitHub Actions documentation](https://docs.github.com/en/actions) — workflow, permissions, artifacts, secrets.
- [Gradle setup action](https://github.com/gradle/actions) — Gradle dependency/cache support.
- [Android setup action](https://github.com/android-actions/setup-android) — подготовка Android SDK на runner.
- [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases) — релизы по тегам.
- [Git LFS](https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-git-large-file-storage) — только если лицензированный набор данных действительно необходим; не добавлять датасеты/фото пользователей в Git по умолчанию.

## Кодеки и качество

- [libjxl](https://github.com/libjxl/libjxl) — upstream JPEG XL; Android/NDK сборка, зависимости, ABI и лицензия должны быть проверены отдельно.
- [JPEG XL software](https://jpeg.org/jpegxl/software.html) — список программного обеспечения формата.
- [JPEG XL white paper](https://ds.jpeg.org/whitepapers/jpeg-xl-whitepaper.pdf) — описание формата.
- [libavif](https://github.com/AOMediaCodec/libavif) — AVIF library; её наличие в source tree не означает Android integration.
- [AVIF specification](https://aomediacodec.github.io/av1-avif/) — контейнер и совместимость.
- [SSIMULACRA2](https://github.com/cloudinary/ssimulacra2) — отдельная перцептивная метрика; проверяйте лицензирование, сборку и поведение именно на Android.
- [CoD: A Diffusion Foundation Model for Image Compression](https://arxiv.org/abs/2511.18706) — compression-oriented diffusion model research; paper reports its own training/comparison setup, not a ready Android codec.
- [DiffO](https://arxiv.org/abs/2506.16572) — single-step diffusion image compression research; evaluate the paper's exact setup and decoder cost before drawing device conclusions.
- [StableCodec](https://arxiv.org/abs/2506.21977) — one-step diffusion at extremely low rates; perceptual reconstruction is not pixel exact.
- [MoRIC, NeurIPS 2025](https://neurips.cc/virtual/2025/loc/san-diego/poster/118832) — modular region-based INR research; region fitting and side-information rates must be counted.
- [INIF, Nature Computational Science (2025)](https://www.nature.com/articles/s43588-025-00889-4) — microscopy-focused implicit neural field research; validate scientific downstream tasks, not just visual score.
- [PICO project](https://apple.github.io/ml-pico/) and [arXiv paper record](https://arxiv.org/abs/2605.05148) — practical learned codec paper; results are author-reported and not independently reproduced by this project.

## Условия использования источников и датасетов

- Не копировать тестовые изображения из случайных сайтов в репозиторий.
- Для каждого набора записать право использования, лицензию, исходную ссылку, версию/хеш и правила распространения.
- Результаты кодеков сравнивать на неизменном input corpus и фиксировать кодек/версию/параметры.
- Не писать в отчёт ожидаемую метрику или опубликованный результат как будто он получен в ADAMANT Lab.
