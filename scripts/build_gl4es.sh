#!/usr/bin/env bash
# gl4es -> libGL.so (box64 BOX64_LIBGL ile kullanir). Basarisiz olursa APK yine de derlenir.
NDK="${ANDROID_HOME}/ndk/26.3.11579264"
mkdir -p app/src/main/jniLibs/arm64-v8a
cmake -S app/third_party/gl4es -B build_gl4es \
  -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 \
  -DNOX11=ON -DDEFAULT_ES=2 -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
 && cmake --build build_gl4es -j"$(nproc)" \
 && cp "$(find build_gl4es -name 'libGL*.so' | head -1)" app/src/main/jniLibs/arm64-v8a/libGL.so \
 || echo "UYARI: gl4es derlenemedi (2D modda sorun cikarabilir)"
exit 0
