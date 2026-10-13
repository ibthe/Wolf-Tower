# SPEC — Wolf Tower (Android first)

Цель: приложение, которое снимает DPI-блокировки для WhatsApp, Telegram и сервисов Google/YouTube.
Платформы: Android (этап 1), Windows и macOS (Electron, этап 2-3).

## Архитектура Android
- profiles/profiles.json — общий для всех платформ список сервисов, доменов, пакетов и стратегий.
- ByeDPI (third_party/byedpi, MIT) собирается через CMake как исполняемый файл libciadpi.so
  и запускается из nativeLibraryDir как отдельный процесс (SOCKS5 на 127.0.0.1:1080).
- DpiVpnService (VpnService) поднимает TUN, маршрутизирует в него ТОЛЬКО пакеты включённых сервисов
  (addAllowedApplication), трафик TUN отдаётся tun2socks, а тот шлёт его в SOCKS5 ByeDPI.
- tun2socks: hev-socks5-tunnel (MIT) — ПОДКЛЮЧИТЬ (сейчас заглушка Tun2Socks.kt).
- UDP/QUIC: ByeDPI не поддерживает UDP ASSOCIATE, поэтому QUIC через туннель отваливается и клиенты
  откатываются на TCP. Для YouTube это нужное поведение.

## Решения v1
- Одна стратегия на весь сеанс (выбирается в UI из объединения стратегий включённых профилей).
- Автоподбор стратегий: этап 2 (tools/autoprobe).
- DNS: 1.1.1.1 (если провайдер подменяет DNS, нужен DoH — отдельная задача).

## Лицензии
ByeDPI MIT, zapret MIT, hev-socks5-tunnel MIT, WinDivert LGPL-3/GPL-2. ByeDPIAndroid (GPL-3) НЕ форкаем.
Тексты лицензий — в third_party/LICENSES.

## Открытые вопросы
- Проверить флаги ByeDPI в `ciadpi --help` и поправить StrategyArgs.kt.
- Подключить hev-socks5-tunnel (JNI или как исполняемый файл).
