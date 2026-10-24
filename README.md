# DFDroid

Dwarf Fortress (0.47.x Linux build) için Android launcher. x86_64 binary'yi **box64** ile ARM64'te çalıştırır, SDL2 + sdl12-compat ile ekrana verir.

## Kurulum
1. Bu klasörün içindekileri GitHub reposuna yükle (`main` dalı).
2. **Actions → Build APK** otomatik çalışır (ya da "Run workflow"). Bitince **Artifacts → DFDroid-debug-apk** indir, APK'yı kur.
3. Uygulamayı aç → **Oyun klasörünü içe aktar** → telefonda `df_linux` klasörünü seç (içinde `df`, `libs`, `data`, `raw` olmalı). Kopyalama bitince "HAZIR ✔" görünür.
4. **OYUNU BAŞLAT**.

(İstersen dosyaları elle de koyabilirsin: `Android/data/com.rgb.dfdroid/files/game/`)

## Özellikler
- Dosya kontrolü (oyun dosyaları inik mi / eksik mi)
- Touch kontroller `controls.json` (launcher içinden editörle düzenlenir: sürükle, büyüt/küçült, tuş ekle/sil, touchpad hassasiyeti)
- Touchpad = sanal mouse: sürükle → imleç, dokun → sol tık, iki parmak dokun → sağ tık, LMB/RMB tuşları basılı tutulabilir (sürükleme için)
- Oyun sırasında log gösterilmez. Sağ üstte sürüklenebilir ☰ butonu: log görüntüleyici (canlı, çalışıyor/durdu bilgisi), kontrolleri gizle, ekran klavyesi, **X** (çıkış). Menü dışına dokunmak menüyü kapatır, buton geri gelir. (Bu buton JSON'da değil, launcher içinde sabit.)
- Çökme olursa bir sonraki açılışta crash logu (game.log + logcat) otomatik kaydedilir, paylaşılabilir.
- Ayarlar: 2D mod zorla, ses kapat, dynarec, FPS sınırı, kontrol saydamlığı. Oyuna girerken `init.txt` buna göre otomatik ayarlanır.

## Önemli not
Native katman (box64 + SDL2 + sdl12-compat + gl4es) test edilmeden yazıldı. Workflow hata verirse Actions logunu (özellikle `Bağımlılıkları indir` ve `APK derle` adımları) ve oyun içi `game.log` / crash logunu paylaş, birlikte düzeltiriz.
