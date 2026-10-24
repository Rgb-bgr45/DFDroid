package com.rgb.dfdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;

/** Touch kontrol ayarlama menusu: tus surukle, boyutlandir, ekle/sil, touchpad ayari, JSON'a kaydet. */
public class EditorActivity extends Activity {
    ControlsConfig cfg;
    ControlsView view;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        cfg = ControlsConfig.load(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF202830);
        view = new ControlsView(this, cfg, true);
        view.opacity = getSharedPreferences("s", 0).getInt("opacity", 60);
        if (view.opacity < 30) view.opacity = 30;
        root.addView(view, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout bar = new LinearLayout(this);
        bar.setBackgroundColor(0xCC000000);
        addBtn(bar, "+ Tuş", v -> { ControlsConfig.Btn n = new ControlsConfig.Btn(); n.id = "b" + System.currentTimeMillis(); n.label = "?"; n.key = "SPACE"; n.x = .4f; n.y = .4f; n.s = .12f; cfg.buttons.add(n); view.sel = n; view.padSel = false; view.invalidate(); editKey(); });
        addBtn(bar, "Tuşu düzenle", v -> editKey());
        addBtn(bar, "Büyüt", v -> view.scaleSelected(1.1f));
        addBtn(bar, "Küçült", v -> view.scaleSelected(0.9f));
        addBtn(bar, "Sil", v -> { if (view.sel != null) { cfg.buttons.remove(view.sel); view.sel = null; view.invalidate(); } });
        addBtn(bar, "Touchpad aç/kapa", v -> { cfg.padOn = !cfg.padOn; view.invalidate(); });
        addBtn(bar, "Hassasiyet", v -> sens());
        addBtn(bar, "Sıfırla", v -> { ControlsConfig d = ControlsConfig.defaults(); cfg.buttons.clear(); cfg.buttons.addAll(d.buttons); cfg.padOn = true; cfg.padX = d.padX; cfg.padY = d.padY; cfg.padW = d.padW; cfg.padH = d.padH; cfg.sens = d.sens; view.sel = null; view.invalidate(); });
        addBtn(bar, "KAYDET", v -> { cfg.save(this); Toast.makeText(this, "controls.json kaydedildi", Toast.LENGTH_SHORT).show(); finish(); });
        addBtn(bar, "İptal", v -> finish());
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.addView(bar);
        root.addView(hs, new FrameLayout.LayoutParams(-1, -2));
        setContentView(root);
        Toast.makeText(this, "Tuşa dokunup sürükle. Touchpad alanını da sürükleyebilirsin.", Toast.LENGTH_LONG).show();
    }

    void addBtn(LinearLayout bar, String t, View.OnClickListener l) {
        Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setOnClickListener(l);
        bar.addView(b, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    void editKey() {
        final ControlsConfig.Btn s = view.sel;
        if (s == null) { Toast.makeText(this, "Önce bir tuş seç", Toast.LENGTH_SHORT).show(); return; }
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(40, 20, 40, 0);
        final EditText lab = new EditText(this); lab.setHint("Yazı"); lab.setText(s.label); l.addView(lab);
        final EditText key = new EditText(this); key.setHint("Tuş"); key.setText(s.key); l.addView(key);
        TextView h = new TextView(this);
        h.setText("Tuş adları: ENTER, ESCAPE, SPACE, TAB, DPAD_UP/DOWN/LEFT/RIGHT, A-Z, 0-9, PLUS, MINUS, SLASH, COMMA, PERIOD, F1-F12, PAGE_UP, PAGE_DOWN.\nKombinasyon: SHIFT_LEFT+SLASH (= ?).\nÖzel: MOUSE_LEFT, MOUSE_RIGHT, KEYBOARD (ekran klavyesi).");
        h.setTextSize(12); l.addView(h);
        new AlertDialog.Builder(this).setTitle("Tuş ayarı").setView(l)
                .setPositiveButton("Tamam", (d, w) -> { s.label = lab.getText().toString(); s.key = key.getText().toString().trim().toUpperCase(); view.invalidate(); })
                .setNegativeButton("İptal", null).show();
    }

    void sens() {
        final EditText e = new EditText(this); e.setText(String.valueOf(cfg.sens));
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new AlertDialog.Builder(this).setTitle("Touchpad hassasiyeti (0.5 - 5)").setView(e)
                .setPositiveButton("Tamam", (d, w) -> { try { cfg.sens = Math.max(0.3f, Math.min(6f, Float.parseFloat(e.getText().toString()))); } catch (Exception ignored) { } })
                .setNegativeButton("İptal", null).show();
    }
}
