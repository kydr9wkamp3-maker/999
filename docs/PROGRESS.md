# CHROMECORE X — PROGRESS

## §0: Доступы

```
gh auth status: ✓ Logged in as kydr9wkamp3-maker (GH_TOKEN)
git remote: origin https://github.com/kydr9wkamp3-maker/999.git
java: НЕ установлен в sandbox
ANDROID_HOME: НЕ установлен
adb: НЕ доступен
```

Вывод: gh работает — читаю логи CI сам. Локальная сборка невозможна.

---

## ЭТАП: S0.2 — Привести репо к правде
**СТАТУС**: IN PROGRESS
**ДАТА**: 2026-11-28
**ДЕЛАЕМ**:
1. ✅ README.md — убраны несуществующие модули, добавлена реальная структура
2. ✅ docs/ARCHITECTURE.md — только реальные файлы, будущее помечено PLANNED
3. ✅ scripts/build.sh — исправлен пакет на com.chromecore.camera.debug
4. ✅ CI — добавлен шаг chmod +x gradlew
5. ✅ preflight.sh — добавлены проверки: assertTrue(true), TODO в main, am start без .debug
6. ✅ BasicTest → FlashModeTest (реальный тест цикла режимов вспышки)
7. ⏳ docs/BUILD_GUIDE.md — обновить ссылки
8. ⏳ docs/AUDIT.md — обновить под текущее состояние

**КРИТЕРИЙ PASS**: CI зелёный, preflight OK, docs соответствуют коду.

---

## ЭТАП: S1 — Камера и затвор работают на телефоне
**СТАТУС**: WRITTEN, NOT VERIFIED ON DEVICE
**В КОДЕ ЕСТЬ**:
- CameraX preview + MediaStore сохранение
- Зум (щипок + слайдер)
- Вспышка (OFF/AUTO/ON — но AUTO не делает ничего, ON = фонарик)
- Переключение камер

**ПРОБЛЕМЫ** (по аудиту):
- Вспышка AUTO ничего не делает, ON включает фонарик а не вспышку при съёмке
- maxZoom из LiveData без observeAsState — слайдер может не появиться
- zoomLevel не сбрасывается при смене камеры
- bindCamera вызывается дважды (factory + update)
- ModeSelector не подключён к CameraScreen

**КРИТЕРИЙ PASS**: CI зелёный + чек-лист на Moto G85 пройден.

---

## ЭТАП: S2 — UI камеры
**СТАТУС**: PENDING
Зависит от S1 PASS.
