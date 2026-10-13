# Wolf Tower

Приложение для снятия DPI-блокировок для WhatsApp, Telegram и сервисов Google/YouTube.
Этап 1: Android. Далее Windows и macOS.

Подробности архитектуры: SPEC.md.

## Сборка
    git submodule update --init --recursive
    gradle -p android assembleDebug

Компоненты: ByeDPI (MIT), zapret (MIT), hev-socks5-tunnel (MIT), WinDivert (LGPL-3 / GPL-2).
