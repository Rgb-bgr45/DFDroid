#!/usr/bin/env bash
# CI'da calisir: bagimliliklari indirir, SDL java dosyalarini kopyalar, box64'u yamalar.
set -euo pipefail
TP=app/third_party
mkdir -p "$TP"
git clone --depth 1 --branch release-2.30.8 https://github.com/libsdl-org/SDL "$TP/SDL2"
git clone --depth 1 https://github.com/libsdl-org/sdl12-compat "$TP/sdl12-compat"
git clone --depth 1 https://github.com/ptitSeb/box64 "$TP/box64"
git clone --depth 1 https://github.com/ptitSeb/gl4es "$TP/gl4es"
mkdir -p app/src/main/java/org/libsdl/app
cp "$TP"/SDL2/android-project/app/src/main/java/org/libsdl/app/*.java app/src/main/java/org/libsdl/app/
python3 scripts/patch_box64.py
