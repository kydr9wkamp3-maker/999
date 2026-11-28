# CHROMECORE X — КАТАЛОГ ЭФФЕКТОВ (docs/EFFECTS_CATALOG.md)

Источники: списки эффектов фото-редакторов (Photoshop, Pixlr, Samsung, imgonline), фильтры и плагины OBS, TOP-операторы TouchDesigner, Premiere/After Effects, AR-камеры. Часть пунктов составлена по общим знаниям, не по проверенным источникам. Перед реализацией сверяй алгоритмы с документацией и статьями.

## Как читать

- **Режим:** `RT` — живое превью на камере; `HQ` — полное качество только для фото/редактора/экспорта (в превью упрощённая версия); `ML` — нужна модель (MediaPipe/LiteRT); `VID` — видео/офлайн.
- **Cost:** `L` ≤ 1 проход, `M` 2–3 прохода, `H` > 3 проходов или тяжёлое ядро (в превью работает на пониженном разрешении).
- **Волна:** порядок реализации (A → H, §B).
- **Статус:** `⬜` не начато · `🟨` в работе · `✅ run <id>` сделано и доказано CI + тестами (AGENTS.md §15.2). Ставить `✅` без номера запрещено.
- **ID** = `fx.<категория>.<имя>`, папка `app/src/main/assets/effects/<id>/` (`effect.json`, `*.frag`), строка локализации `fx_<категория>_<имя>` (RU + EN).

---

## A. Примитивы (кирпичики; делаются первыми, эффекты собираются из них)

| # | Примитив | Что внутри | Используется в |
|---|---|---|---|
| P01 | Color matrix + gamma | яркость, контраст, насыщенность, экспозиция, температура/оттенок | basic, sepia, mono, nightvision |
| P02 | Levels / Curves (1D LUT) | чёрная/белая точка, кривые RGB | curves, vintage, film |
| P03 | LUT 3D | `.cube` и PNG-LUT (512×512 / 64³) | lut, lookpack |
| P04 | HSL (8 полос) / HSV mask | сдвиг/диапазоны цвета | hsl, color.replace, keys |
| P05 | Blend modes | Normal, Add, Multiply, Screen, Overlay, SoftLight, Difference, Lighten | почти везде |
| P06 | Gaussian blur (separable) | 2 прохода, радиус через mip-цепочку | bloom, glow, bgblur |
| P07 | Kawase / dual-filter blur | дешёвое широкое размытие | bloom, glow |
| P08 | Directional / radial blur | motion, zoom | motion blur, anamorphic |
| P09 | Bokeh kernel | диск/гексагон, усиление бликов | bokeh |
| P10 | Convolution 3×3/5×5 | sharpen, emboss, edge | sharpen, emboss, neon |
| P11 | Edge detect (Sobel / DoG) | контуры | ink, comic, neon, sketch |
| P12 | Threshold / Posterize / Quantize | уровни | comic, popart, photocopy |
| P13 | Noise (value/simplex/fbm) | процедурный шум | grain, glitch, displace, clouds |
| P14 | UV warp / Displace | искажение по карте/шуму | wave, twirl, chrome, heat |
| P15 | Polar / log-polar transform | планета, Droste | planet, droste |
| P16 | Mirror / Tile / Kaleidoscope | симметрии | mirror, kaleidoscope |
| P17 | Feedback (ping-pong FBO) | след предыдущего кадра | trails |
| P18 | History buffer (N кадров) | кольцо текстур (лимит памяти) | echo, time machine, slitscan |
| P19 | Ordered dither (Bayer) + palette | 4/8/16 цветов | dither, 8bit looks |
| P20 | Halftone / dot grid | точки по яркости | halftone, comic, popart |
| P21 | Glyph atlas | символы по яркости ячейки | ascii |
| P22 | Kuwahara (обычный/анизотропный) | «масляный» сглаживатель | oil, watercolor |
| P23 | Bilateral / guided filter | сглаживание с сохранением краёв | skin, hdr, denoise |
| P24 | Key (chroma/color/luma) | YCbCr-расстояние, spill suppression, softness | keys |
| P25 | Mask compositing | luma/alpha/сегментация/лицо | bgblur, skin, matte |
| P26 | Transform / Corner pin | аффин + гомография | pan-zoom-rotate, cornerpin |
| P27 | Overlay texture | процедурные пыль/царапины/бумага/утечки света | overlay, leaks |
| P28 | Generators | ramp, gradient, pattern, checker, text | фоны, маски |
| P29 | Optical flow (block matching, lite) | на ¼ разрешения | flow, datamosh-approx |
| P30 | Environment/Fresnel reflection | отражение размытого кадра как env-map | chrome, mercury |
| P31 | Mip-chain downsample/upsample | общий пул уменьшенных текстур | bloom, blur, hdr |
| P32 | Resample / Lanczos / Scaling | апскейл внутреннего разрешения | адаптивное качество |
| P33 | Luma/Chroma split, YCbCr | работа с яркостью отдельно | grain, vhs, keys |
| P34 | Time/Param signals | LFO, random, audio FFT, touch XY, accelerometer | ноды |
| P35 | Crop / Pad / Fit / Aspect | геометрия кадра | crop, frames |

---

## B. Волны реализации

- **A** — основа: примитивы P01–P05, P10, P13, P27 + базовые цвет/плёнка (≈14 эффектов).
- **B** — свет и оптика (≈12).
- **C** — глитч, ретро, пиксели (≈16).
- **D** — геометрия и размытия (≈16).
- **E** — время (feedback, echo, delay) (≈7).
- **F** — стилизация тяжёлая (комикс, акварель, масло, скетч) (≈10).
- **G** — ключи, маски, ML (фон, кожа, шумоподавление, HDR) (≈12).
- **H** — фирменные «хром» эффекты + кино-паки (≈10).

---

## C. Каталог

### C1. Цвет и тон

| ID | Название | Техника | Режим | Cost | Волна | Статус |
|---|---|---|---|---|---|---|
| fx.basic.adjust | Базовая коррекция: яркость, контраст, насыщенность, vibrance, экспозиция, гамма, температура, оттенок | P01 | RT | L | A | ⬜ |
| fx.color.mono | Чёрно-белое | P01 (Rec.709), микс каналов | RT | L | A | ⬜ |
| fx.color.sepia | Сепия | P01 | RT | L | A | ⬜ |
| fx.color.duotone | Дуотон | luma → рампа 2 цвета | RT | L | A | ⬜ |
| fx.color.tritone | Тритон | luma → рампа 3 цвета | RT | L | A | ⬜ |
| fx.color.vintage | Винтаж/ретро | P02 + тон + P13 + виньетка | RT | L | A | ⬜ |
| fx.color.curves | Кривые RGB | P02 | RT | L | A | ⬜ |
| fx.color.hsl | HSL-микшер | P04 | RT | L | A | ⬜ |
| fx.color.replace | Замена цвета | P04 (диапазон + сдвиг) | RT | L | G | ⬜ |
| fx.color.lut | LUT-импорт (.cube / PNG) | P03 | RT | L | A | ⬜ |
| fx.color.nightvision | Ночное видение | зелёная рампа + P13 + виньетка | RT | L | B | ⬜ |
| fx.color.hdr | HDR / локальный тонмэппинг | P23 + P31 на малом разрешении | HQ | M | G | ⬜ |
| fx.color.lookpack | Кино-паки: Teal&Orange, Bleach Bypass, Noir, Cross-process, Kodak-like и др. (≥ 20 собственных процедурных LUT, без чужих паков) | P02 + P03 | RT | L | H | ⬜ |

### C2. Размытие и глубина

| ID | Название | Техника | Режим | Cost | Волна | Статус |
|---|---|---|---|---|---|---|
| fx.blur.gaussian | Размытие (Gaussian) | P06 | RT | M | D | ⬜ |
| fx.blur.motion | Размытие в движении | P08 | RT | M | D | ⬜ |
| fx.blur.radial | Радиальное / zoom-размытие | P08 | RT | M | D | ⬜ |
| fx.blur.tiltshift | Миниатюра (tilt-shift) | P06 + градиентная маска | RT | M | D | ⬜ |
| fx.blur.bokeh | Боке | P09 + усиление бликов | RT (пониж. разр.) | H | D | ⬜ |
| fx.blur.frame | Размытая рамка | P06 + P25 | RT | M | D | ⬜ |
| fx.blur.lumablur | Luma blur (размытие по яркости) | P06 + P33 | RT | M | D | ⬜ |
| fx.blur.background | Размытие фона (портрет) | сегментация + P06 | RT, ML | M | G | ⬜ |

### C3. Свет и оптика

| ID | Название | Техника | Режим | Cost | Волна | Статус |
|---|---|---|---|---|---|---|
| fx.light.bloom | Блум | порог + P07/P31 + Add | RT | M | B | ⬜ |
| fx.light.glow | Свечение | P06 + Screen | RT | M | B | ⬜ |
| fx.light.flare | Блики объектива (lens flare) | процедурный по ярчайшей точке / касанию | RT | M | B | ⬜ |
| fx.light.leaks | Утечки света | P27 + Screen | RT | L | B | ⬜ |
| fx.light.lensdist | Искажение объектива (бочка/подушка) | P14 | RT | L | B | ⬜ |
| fx.light.chromab | Хроматическая аберрация | радиальный сдвиг RGB | RT | L | B | ⬜ |
| fx.light.neon | Неон / цветной свет | P11 + P06 + цвета | RT | M | B | ⬜ |
| fx.light.fog | Туман / дымка | подъём чёрного + градиент + P13 | RT | L | B | ⬜ |
| fx.light.vignette | Виньетка | радиальная маска | RT | L | A | ⬜ |
| fx.light.anamorphic | Анаморфные полосы | порог + горизонтальный P08 + Add | RT | M | B | ⬜ |
| fx.light.godrays | Лучи света | радиальный blur от яркого центра | RT | M | B | ⬜ |
| fx.light.colorlight | Цветная подсветка / gel | градиент + Overlay/Screen | RT | L | B | ⬜ |

### C4. Текстура и плёнка

| ID | Название | Техника | Режим | Cost | Волна | Статус |
|---|---|---|---|---|---|---|
| fx.tex.grain | Зерно | P13 по luma | RT | L | A | ⬜ |
| fx.tex.noise | Цветной шум | P13 | RT | L | A | ⬜ |
| fx.tex.film | Плёнка (кривые + halation + зерно + gate weave) | P02 + P07 + P13 | RT | M | A | ⬜ |
| fx.tex.overlay | Пыль, царапины, бумага | P27 | RT | L | A | ⬜ |
| fx.tex.vhs | VHS (дрожание, tracking, chroma bleed) | P33 + P13 + P14 | RT | M | C | ⬜ |
| fx.tex.crt | ЭЛТ (scanlines, кривизна, RGB-маска) | P14 + P13 + P07 | RT | M | C | ⬜ |
| fx.tex.photocopy | Фотокопия | P12 + P13 + P19 | RT | L | C | ⬜ |
| fx.tex.polaroid | Полароид (рамка + fade) | P01 + P35 | RT/HQ | L | C | ⬜ |
| fx.tex.8mm | Плёнка 8 мм (мерцание, дрожание) | P13 + P14 | RT | L | C | ⬜ |
| fx.tex.sharpen | Резкость (unsharp / CAS) | P10 | RT | L | A | ⬜ |
| fx.tex.denoise | Шумоподавление | P23 (+ позже LiteRT) | HQ | M | G | ⬜ |

### C5. Глитч, ретро, пиксели

| ID | Название | Техника | Режим | Cost | Волна | Статус |
|---|---|---|---|---|---|---|
| fx.glitch.rgbsplit | RGB Split | сдвиг каналов | RT | L | C | ⬜ |
| fx.glitch.blocks | Блочные искажения | P13 + блоки | RT | L | C | ⬜ |
| fx.glitch.slice | Сдвиг полос | P13 по Y | RT | L | C | ⬜ |
| fx.glitch.signal | Потеря сигнала (шум-полосы) | P13 + P33 | RT | L | C | ⬜ |
| fx.glitch.scanlines | Scanlines | периодическая маска | RT | L | C | ⬜ |
| fx.glitch.pixelsort | Pixel Sort (ограниченный сортировочный проход, приближение) | сортировка в окне по строке | RT (пониж. разр.) / HQ полная | H | C | ⬜ |
| fx.glitch.datamosh | Datamosh (приближение) | P29 + P17 | VID | H | E | ⬜ |
| fx.style.pixelate | Пикселизация | квантование UV | RT | L | C | ⬜ |
| fx.style.mosaic | Мозаика (квадраты/гекс/треугольники) | сетка + усреднение | RT | L | C | ⬜ |
| fx.style.halftone | Полутон | P20 | RT | M | C | ⬜ |
| fx.style.dither | Дизеринг (Bayer + палитры) | P19 | RT | L | C | ⬜ |
| fx.style.ascii | ASCII-арт | P21 | RT | M | C | ⬜ |
| fx.style.posterize | Постеризация | P12 | RT | L | C | ⬜ |
| fx.style.emboss | Тиснение | P10 | RT | L | C | ⬜ |
| fx.style.thermal | Тепловизор | luma → палитра «жара» | RT | L | C | ⬜ |
| fx.style.8bit | 8-бит / игровая палитра | P19 + P12 + пикселизация | RT | L | C | ⬜ |

### C6. Арт-стилизация

| ID | Название | Техника | Режим | Cost | Волна | Статус |
|---|---|---|---|---|---|---|
| fx.art.comic | Комикс | P12 + P11 + P20 | RT | M | F | ⬜ |
| fx.art.popart | Поп-арт | P12 + дуотон-сетка 2×2 | RT | M | F | ⬜ |
| fx.art.ink | Тушь / линии | P11 + порог | RT | M | F | ⬜ |
| fx.art.sketch | Карандашный скетч | DoG + штриховка | RT | M | F | ⬜ |
| fx.art.cartoon | Мультяшный | P23 + P11 + P12 | RT (пониж. разр.) | H | F | ⬜ |
| fx.art.watercolor | Акварель | P22 + бумага + затемнение краёв | HQ (в превью упрощён) | H | F | ⬜ |
| fx.art.oil | Масляная живопись | P22 (анизотропный) | HQ (в превью упрощён) | H | F | ⬜ |
| fx.art.impasto | Мазки / рельеф краски | P22 + P10 (освещение по высоте) | HQ | H | F | ⬜ |
| fx.art.slope | Рельеф по яркости (Slope/нормали) | P10 + освещение | RT | M | F | ⬜ |
| fx.art.risograph | Ризограф (2–3 цвета, зерно, смещение) | P12 + P20 + P13 | RT | M | F | ⬜ |

### C7. Композиция и геометрия

| ID | Название | Техника | Режим | Cost | Волна | Статус |
|---|---|---|---|---|---|---|
| fx.geo.mirror | Зеркало (вертикаль/горизонталь/диагональ) | P16 | RT | L | D | ⬜ |
| fx.geo.kaleidoscope | Калейдоскоп | P16 (+ P15) | RT | M | D | ⬜ |
| fx.geo.twirl | Закручивание | P14 | RT | L | D | ⬜ |
| fx.geo.bulge | Выпуклость / вогнутость | P14 | RT | L | D | ⬜ |
| fx.geo.planet | «Планета» (полярные координаты) | P15 | RT | M | D | ⬜ |
| fx.geo.droste | Эффект Дросте | P15 (log-polar) | RT | M | D | ⬜ |
| fx.geo.doubleexp | Двойная экспозиция | Screen/Lighten со вторым слоем (прошлый кадр / фото из галереи) | RT | L | D | ⬜ |
| fx.geo.frames | Рамки и тени | P35 + P27 | HQ/RT | L | D | ⬜ |
| fx.geo.wave | Волна / рябь | P14 | RT | L | D | ⬜ |
| fx.geo.tile | Плитка / повтор | P16 | RT | L | D | ⬜ |
| fx.geo.fisheye | Рыбий глаз | P14 | RT | L | D | ⬜ |
| fx.geo.transform | Pan / Zoom / Rotate | P26 | RT | L | D | ⬜ |
| fx.geo.cornerpin | Corner pin (гомография) | P26 | RT | L | D | ⬜ |
| fx.geo.scroll | Прокрутка / бегущий кадр | P26 + wrap | RT | L | D | ⬜ |
| fx.geo.crop | Обрезка / Pad / соотношение сторон | P35 | RT | L | D | ⬜ |
| fx.geo.heat | Тепловое марево (heat haze) | P14 + P13 | RT | L | D | ⬜ |

### C8. Время

| ID | Название | Техника | Режим | Cost | Волна | Статус |
|---|---|---|---|---|---|---|
| fx.time.trails | Следы (feedback) | P17 | RT | M | E | ⬜ |
| fx.time.echo | Эхо (N кадров) | P18 | RT (лимит N) | H | E | ⬜ |
| fx.time.delay | Задержка кадра (render delay) | P18 | RT | M | E | ⬜ |
| fx.time.machine | Time Machine (разные задержки по областям) | P18 + маска | RT (лимит) | H | E | ⬜ |
| fx.time.slitscan | Slit-scan | P18 | RT (пониж. разр.) | H | E | ⬜ |
| fx.time.flow | Optical flow (визуализация/смазывание) | P29 | RT (¼ разр.) | H | E | ⬜ |
| fx.time.loop | Boomerang / Loop | запись + реверс | VID | M | E | ⬜ |

### C9. Ключи, маски, ML

| ID | Название | Техника | Режим | Cost | Волна | Статус |
|---|---|---|---|---|---|---|
| fx.key.chroma | Хромакей (Ultra Key-класс: spill suppression, softness) | P24 | RT | M | G | ⬜ |
| fx.key.color | Color Key | P24 | RT | L | G | ⬜ |
| fx.key.luma | Luma Key | P24 | RT | L | G | ⬜ |
| fx.key.imagemask | Маска/смешивание по картинке | P25 | RT | L | G | ⬜ |
| fx.key.matte | Track Matte (слои, редактор) | P25 | HQ | L | G | ⬜ |
| fx.ml.bgreplace | Замена фона (картинка/видео/GIF) | сегментация + P25 | RT, ML | M | G | ⬜ |
| fx.ml.skin | Сглаживание кожи (без пластика, с регулятором) | лицо + P23 | RT, ML | M | G | ⬜ |
| fx.ml.hdr_face | Тон лица (exposure по лицу) | лицо + P01 | RT, ML | L | G | ⬜ |
| fx.ml.person_fx | Эффект только на силуэт | сегментация + любой эффект | RT, ML | M | G | ⬜ |

### C10. Фирменные «хром» эффекты и кино-паки

| ID | Название | Техника | Режим | Cost | Волна | Статус |
|---|---|---|---|---|---|---|
| fx.chrome.liquid | Liquid Chrome (жидкий металл по кадру) | P14 + P30 + P13 | RT | M | H | ⬜ |
| fx.chrome.metalheart | Metalheart (хромовое лицо/силуэт) | лицо/сегм. + P30 | RT, ML | H | H | ⬜ |
| fx.chrome.mercury | Mercury (ртуть) | P14 + P30 + P06 | RT | M | H | ⬜ |
| fx.chrome.titanium | Titanium Mono (матовый металл ч/б) | P01 + P10 + зерно | RT | L | H | ⬜ |
| fx.chrome.obsidian | Obsidian (тёмное стекло/зеркало) | P30 + виньетка + bloom | RT | M | H | ⬜ |
| fx.chrome.iridescent | Радужная плёнка / голограмма | P14 + hue-градиент по нормали | RT | M | H | ⬜ |
| fx.chrome.hologram | Голограмма (scanlines + chromab + glow) | комбинация | RT | M | H | ⬜ |

---

## D. Что из исходных списков НЕ делается как есть (честно)

- **Warp Stabilizer / Twixtor / Neat Video (Premiere/AE):** в живой камере не воспроизводятся. Замена: стабилизация видео средствами платформы/CameraX (если поддерживается) + собственная офлайн-стабилизация в редакторе позже (этап `PLANNED`); замедление через интерполяцию кадров (VID, офлайн); шумоподавление — `fx.tex.denoise` (+ LiteRT позже).
- **After Effects-эффекты «как есть»:** не рассчитаны на реальное время. Покрываем их функциями по смыслу (блум, глитч, деформации и т.д.) и таймлайном слоёв (этап S12).
- **OBS-плагины (StreamFX, Shaderfilter, BackgroundRemoval и др.):** код не переносится. Аналоги: нод-движок с пользовательским GLSL (с валидацией и лимитами) вместо Shaderfilter; сегментация MediaPipe вместо BackgroundRemoval; Composite Blur → P06/P07.
- **Аудиофильтры OBS (Noise Gate, Compressor и т.д.):** относятся к видео-режиму; PLANNED после S8.
- **Snap Camera / Camera Effects / VTuber-инструменты:** сторонние программы, не цель; AR-часть делается в S6–S7.
- **Нейросетевые стилизации (Stable Diffusion/StreamDiffusion):** тяжело для Adreno 619; PLANNED как опциональный офлайн-режим, если появится подходящая лёгкая модель (искать и проверять отдельно).

## E. Счёт

Каталог: ~115 записей. Статус по волнам агент ведёт здесь же; в `docs/PROGRESS.md` указывает долю `✅` и номера CI-run.
