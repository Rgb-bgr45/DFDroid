package com.rgb.dfdroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.system.Os;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import org.libsdl.app.SDLActivity;

public class GameActivity extends SDLActivity {
    static String loadErr = "";

    static {
        // box64'un host tarafinda arayacagi kutuphaneler (soname eslesmesi ile bulunur)
        for (String n : new String[]{"SDL2", "SDL12", "GL"}) {
            try { System.loadLibrary(n); } catch (Throwable t) { loadErr += "loadLibrary(" + n + ") basarisiz: " + t.getMessage() + "\n"; }
        }
    }

    @Override protected String[] getLibraries() { return new String[]{"SDL2", "main"}; }

    private SharedPreferences sp;
    private FrameLayout overlay;
    private ControlsView controls;
    private TextView floatBtn;
    private View scrim;
    private LinearLayout panel;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle b) {
        sp = getSharedPreferences("s", 0);
        File game = Util.game(this);
        try {
            patchInit(game);
            File log = Util.gameLog(this);
            try (FileWriter w = new FileWriter(log, false)) {
                w.write("=== DFDroid oturum ===\nCihaz: " + Build.MANUFACTURER + " " + Build.MODEL + " Android " + Build.VERSION.RELEASE + " abi=" + Build.SUPPORTED_ABIS[0] + "\n" + loadErr);
            }
            Os.setenv("DF_GAME_DIR", game.getAbsolutePath(), true);
            Os.setenv("DF_LOG", log.getAbsolutePath(), true);
            Os.setenv("HOME", game.getAbsolutePath(), true);
            String libs = game.getAbsolutePath() + "/libs";
            Os.setenv("LD_LIBRARY_PATH", libs, true);
            Os.setenv("BOX64_LD_LIBRARY_PATH", libs, true);
            Os.setenv("BOX64_LIBGL", "libGL.so", true);
            Os.setenv("BOX64_NOBANNER", "1", true);
            Os.setenv("BOX64_LOG", "1", true);
            Os.setenv("BOX64_DYNAREC", sp.getBoolean("dynarec", true) ? "1" : "0", true);
            Util.flag(this).createNewFile();
        } catch (Throwable t) {
            Toast.makeText(this, "Hazırlık hatası: " + t, Toast.LENGTH_LONG).show();
        }
        super.onCreate(b);
        if (sp.getBoolean("keepon", true)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildOverlay();
    }

    /** init.txt'yi telefona uygun hale getirir (2D mod, ses kapali, FPS siniri, pencere boyutu). */
    private void patchInit(File game) throws Exception {
        File f = new File(game, "data/init/init.txt");
        if (!f.exists()) return;
        String s = new String(Files.readAllBytes(f.toPath()));
        DisplayMetrics m = getResources().getDisplayMetrics();
        int w = Math.max(m.widthPixels, m.heightPixels), hh = Math.min(m.widthPixels, m.heightPixels);
        if (sp.getBoolean("force2d", true)) s = Util.setTok(s, "PRINT_MODE", "2D");
        if (sp.getBoolean("nosound", true)) s = Util.setTok(s, "SOUND", "NO");
        s = Util.setTok(s, "INTRO", "NO");
        s = Util.setTok(s, "TRUETYPE", "NO");
        s = Util.setTok(s, "WINDOWED", "YES");
        s = Util.setTok(s, "WINDOWEDX", String.valueOf(w));
        s = Util.setTok(s, "WINDOWEDY", String.valueOf(hh));
        s = Util.setTok(s, "FPS_CAP", String.valueOf(sp.getInt("fps", 30)));
        s = Util.setTok(s, "G_FPS_CAP", String.valueOf(sp.getInt("fps", 30)));
        Files.write(f.toPath(), s.getBytes());
    }

    // ------------------------------------------------------------ overlay
    private void buildOverlay() {
        overlay = new FrameLayout(this);
        controls = new ControlsView(this, ControlsConfig.load(this), false);
        controls.opacity = sp.getInt("opacity", 60);
        controls.sink = new ControlsView.Sink() {
            @Override public void key(String k, boolean down) { sendKey(k, down); }
            @Override public void mouse(int mask, int action, float x, float y) { SDLActivity.onNativeMouse(mask, action, x, y, false); }
        };
        overlay.addView(controls, new FrameLayout.LayoutParams(-1, -1));

        scrim = new View(this);
        scrim.setBackgroundColor(0x66000000);
        scrim.setVisibility(View.GONE);
        scrim.setOnClickListener(v -> closeMenu());
        overlay.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        int sz = Util.dp(this, 46);
        floatBtn = new TextView(this);
        floatBtn.setText("☰"); floatBtn.setTextSize(20); floatBtn.setTextColor(Color.WHITE); floatBtn.setGravity(android.view.Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(); g.setShape(GradientDrawable.OVAL); g.setColor(0xAA222222); g.setStroke(3, Color.WHITE);
        floatBtn.setBackground(g);
        overlay.addView(floatBtn, new FrameLayout.LayoutParams(sz, sz));
        floatBtn.post(() -> { floatBtn.setX(overlay.getWidth() - sz - Util.dp(this, 10)); floatBtn.setY(Util.dp(this, 10)); });
        floatBtn.setOnTouchListener(new View.OnTouchListener() {
            float dx, dy, sx, sy;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: dx = v.getX() - e.getRawX(); dy = v.getY() - e.getRawY(); sx = e.getRawX(); sy = e.getRawY(); return true;
                    case MotionEvent.ACTION_MOVE:
                        v.setX(Math.max(0, Math.min(overlay.getWidth() - v.getWidth(), e.getRawX() + dx)));
                        v.setY(Math.max(0, Math.min(overlay.getHeight() - v.getHeight(), e.getRawY() + dy)));
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (Math.hypot(e.getRawX() - sx, e.getRawY() - sy) < Util.dp(GameActivity.this, 8)) openMenu();
                        return true;
                }
                return false;
            }
        });

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(0xEE1B1F24);
        panel.setPadding(Util.dp(this, 14), Util.dp(this, 8), Util.dp(this, 14), Util.dp(this, 12));
        panel.setClickable(true);
        panel.setVisibility(View.GONE);

        LinearLayout top = new LinearLayout(this);
        TextView title = new TextView(this); title.setText("DFDroid menü"); title.setTextColor(Color.WHITE); title.setTextSize(16);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        Button x = new Button(this); x.setText("X"); x.setTextColor(Color.WHITE); x.setBackgroundColor(0xFFB3261E);
        x.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("Oyundan çıkılsın mı? Kaydedilmemiş ilerleme kaybolabilir.")
                .setPositiveButton("Çık", (d, w) -> quit()).setNegativeButton("Vazgeç", null).show());
        top.addView(x, new LinearLayout.LayoutParams(Util.dp(this, 48), Util.dp(this, 40)));
        panel.addView(top);
        menuItem("Log görüntüleyici", v -> showLog());
        menuItem("Kontrolleri göster/gizle", v -> { controls.setVisibility(controls.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE); closeMenu(); });
        menuItem("Ekran klavyesi", v -> { toggleKeyboard(); closeMenu(); });
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(Util.dp(this, 260), -2);
        pl.gravity = android.view.Gravity.CENTER;
        overlay.addView(panel, pl);

        mLayout.addView(overlay, new ViewGroup.LayoutParams(-1, -1));
    }

    private void menuItem(String t, View.OnClickListener l) {
        Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setOnClickListener(l);
        panel.addView(b);
    }

    private void openMenu() { floatBtn.setVisibility(View.GONE); scrim.setVisibility(View.VISIBLE); panel.setVisibility(View.VISIBLE); }
    private void closeMenu() { panel.setVisibility(View.GONE); scrim.setVisibility(View.GONE); floatBtn.setVisibility(View.VISIBLE); }

    private void toggleKeyboard() {
        try { SDLActivity.showTextInput(0, 0, 10, 10); } catch (Throwable t) {
            InputMethodHelper.toggle(this, mSurface);
        }
    }

    // ------------------------------------------------------------ tus gonderme
    private int metaOf(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_SHIFT_LEFT: case KeyEvent.KEYCODE_SHIFT_RIGHT: return KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
            case KeyEvent.KEYCODE_CTRL_LEFT: case KeyEvent.KEYCODE_CTRL_RIGHT: return KeyEvent.META_CTRL_ON;
            case KeyEvent.KEYCODE_ALT_LEFT: case KeyEvent.KEYCODE_ALT_RIGHT: return KeyEvent.META_ALT_ON;
        }
        return 0;
    }

    private void sendKey(String combo, boolean down) {
        if ("KEYBOARD".equals(combo)) { if (down) toggleKeyboard(); return; }
        String[] parts = combo.split("\\+");
        int[] codes = new int[parts.length];
        int meta = 0;
        for (int i = 0; i < parts.length; i++) {
            codes[i] = KeyEvent.keyCodeFromString("KEYCODE_" + parts[i].trim());
            meta |= metaOf(codes[i]);
        }
        long now = android.os.SystemClock.uptimeMillis();
        if (down) {
            int m = 0;
            for (int c : codes) { if (c == 0) continue; m |= metaOf(c); mSurface.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, c, 0, m)); }
        } else {
            int m = meta;
            for (int i = codes.length - 1; i >= 0; i--) { if (codes[i] == 0) continue; mSurface.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, codes[i], 0, m)); m &= ~metaOf(codes[i]); }
        }
    }

    // ------------------------------------------------------------ log gorüntüleyici
    private void showLog() {
        closeMenu();
        final TextView status = new TextView(this); status.setTextSize(12); status.setTextColor(Color.WHITE);
        final TextView tv = new TextView(this); tv.setTextSize(10); tv.setTypeface(android.graphics.Typeface.MONOSPACE); tv.setTextColor(0xFFDDDDDD); tv.setTextIsSelectable(true);
        final ScrollView sv = new ScrollView(this); sv.addView(tv);
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(20, 10, 20, 0); l.setBackgroundColor(0xFF111111);
        l.addView(status); l.addView(sv, new LinearLayout.LayoutParams(-1, Util.dp(this, 260)));
        final File log = Util.gameLog(this);
        final Runnable[] r = new Runnable[1];
        r[0] = () -> {
            boolean alive = mSDLThread != null && mSDLThread.isAlive();
            long age = (System.currentTimeMillis() - log.lastModified()) / 1000;
            status.setText("Oyun işlemi: " + (alive ? "ÇALIŞIYOR" : "DURDU") + "  |  log " + log.length() / 1024 + " KB, son yazım " + age + " sn önce");
            tv.setText(Util.tail(log, 40000));
            sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
            h.postDelayed(r[0], 1500);
        };
        r[0].run();
        new AlertDialog.Builder(this).setView(l)
                .setPositiveButton("Kapat", null)
                .setNeutralButton("Paylaş", (d, w) -> Util.share(this, "DFDroid log", Util.tail(log, 150000)))
                .setOnDismissListener(d -> h.removeCallbacks(r[0])).show();
    }

    private void quit() {
        Util.flag(this).delete();
        finishAndRemoveTask();
        Process.killProcess(Process.myPid());
    }

    @Override public void onBackPressed() { openMenu(); }
}
