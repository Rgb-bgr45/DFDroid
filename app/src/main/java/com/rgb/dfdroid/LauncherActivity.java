package com.rgb.dfdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class LauncherActivity extends Activity {
    SharedPreferences sp;
    TextView status, prog;
    Button launch;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("s", 0);
        ScrollView sv = new ScrollView(this);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(0xFF15191E);
        int p = Util.dp(this, 18);
        l.setPadding(p, p, p, p);
        sv.addView(l);
        sv.setBackgroundColor(0xFF15191E);

        TextView t = text("DFDroid", 26, Color.WHITE);
        l.addView(t);
        l.addView(text("Dwarf Fortress (Linux build) → Android • box64", 13, 0xFF9AA5B1));
        status = text("", 13, 0xFFDDDDDD);
        status.setTypeface(android.graphics.Typeface.MONOSPACE);
        status.setPadding(0, p, 0, p);
        l.addView(status);
        prog = text("", 12, 0xFFFFD54F);
        l.addView(prog);

        launch = btn("▶  OYUNU BAŞLAT", v -> startActivity(new Intent(this, GameActivity.class)));
        l.addView(launch);
        l.addView(btn("Oyun klasörünü içe aktar (df_linux)", v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(i, 1);
        }));
        l.addView(btn("Touch kontrol ayarla", v -> startActivity(new Intent(this, EditorActivity.class))));
        l.addView(btn("Launcher ayarları", v -> settings()));
        l.addView(btn("Loglar / crash logları", v -> logs()));
        l.addView(text("Oyun klasörü:\n" + Util.game(this).getAbsolutePath(), 11, 0xFF78909C));
        setContentView(sv);
    }

    @Override protected void onResume() { super.onResume(); refresh(); checkCrash(); }

    TextView text(String s, int sp, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); return t; }
    Button btn(String s, View.OnClickListener l) { Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setOnClickListener(l); return b; }

    void refresh() {
        StringBuilder sb = new StringBuilder("Oyun dosyaları:\n");
        boolean ok = Util.check(this, sb);
        sb.append(ok ? "\nHAZIR ✔" : "\nEKSİK ✘  → önce klasörü içe aktar");
        status.setText(sb.toString());
        launch.setEnabled(ok);
    }

    // ---------------------------------------------------------------- import
    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req != 1 || res != RESULT_OK || data == null) return;
        final Uri tree = data.getData();
        launch.setEnabled(false);
        new Thread(() -> {
            try {
                File dst = Util.game(this);
                dst.mkdirs();
                copyTree(tree, DocumentsContract.getTreeDocumentId(tree), dst);
                runOnUiThread(() -> { prog.setText("Kopyalama bitti."); refresh(); });
            } catch (Exception e) {
                runOnUiThread(() -> { prog.setText("Hata: " + e); refresh(); });
            }
        }).start();
    }

    void copyTree(Uri tree, String docId, File dst) throws IOException {
        Uri kids = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId);
        try (Cursor c = getContentResolver().query(kids, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null)) {
            while (c != null && c.moveToNext()) {
                String id = c.getString(0), name = c.getString(1), mime = c.getString(2);
                File f = new File(dst, name);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) { f.mkdirs(); copyTree(tree, id, f); }
                else {
                    final String n = name;
                    runOnUiThread(() -> prog.setText("Kopyalanıyor: " + n));
                    try (InputStream in = getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(tree, id));
                         OutputStream out = new FileOutputStream(f)) {
                        byte[] buf = new byte[65536]; int r;
                        while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- settings
    void settings() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(40, 20, 40, 0);
        final CheckBox f2d = cb(l, "2D yazdırma modu zorla (önerilir, OpenGL gerekmez)", "force2d", true);
        final CheckBox ns = cb(l, "Sesi kapat", "nosound", true);
        final CheckBox dyn = cb(l, "box64 dynarec (hızlı JIT)", "dynarec", true);
        final CheckBox keep = cb(l, "Ekranı açık tut", "keepon", true);
        final EditText fps = num(l, "FPS sınırı", sp.getInt("fps", 30));
        final EditText op = num(l, "Touch kontrol saydamlığı (30-100)", sp.getInt("opacity", 60));
        new AlertDialog.Builder(this).setTitle("Launcher ayarları").setView(l)
                .setPositiveButton("Kaydet", (d, w) -> sp.edit().putBoolean("force2d", f2d.isChecked()).putBoolean("nosound", ns.isChecked())
                        .putBoolean("dynarec", dyn.isChecked()).putBoolean("keepon", keep.isChecked())
                        .putInt("fps", parse(fps, 30, 5, 200)).putInt("opacity", parse(op, 60, 30, 100)).apply())
                .setNegativeButton("İptal", null).show();
    }

    CheckBox cb(LinearLayout l, String t, String key, boolean def) { CheckBox c = new CheckBox(this); c.setText(t); c.setChecked(sp.getBoolean(key, def)); l.addView(c); return c; }
    EditText num(LinearLayout l, String hint, int v) {
        TextView t = new TextView(this); t.setText(hint); l.addView(t);
        EditText e = new EditText(this); e.setInputType(InputType.TYPE_CLASS_NUMBER); e.setText(String.valueOf(v)); l.addView(e); return e;
    }
    int parse(EditText e, int def, int lo, int hi) { try { return Math.max(lo, Math.min(hi, Integer.parseInt(e.getText().toString()))); } catch (Exception x) { return def; } }

    // ---------------------------------------------------------------- loglar
    void logs() {
        File[] fs = Util.logs(this).listFiles();
        if (fs == null || fs.length == 0) { Toast.makeText(this, "Henüz log yok", Toast.LENGTH_SHORT).show(); return; }
        Arrays.sort(fs, Collections.reverseOrder());
        final File[] files = fs;
        String[] names = new String[fs.length];
        for (int i = 0; i < fs.length; i++) names[i] = fs[i].getName() + "  (" + fs[i].length() / 1024 + " KB)";
        new AlertDialog.Builder(this).setTitle("Loglar").setItems(names, (d, w) -> show(files[w])).show();
    }

    void show(final File f) {
        final String txt = Util.tail(f, 100000);
        TextView tv = new TextView(this); tv.setText(txt); tv.setTextSize(10); tv.setTypeface(android.graphics.Typeface.MONOSPACE); tv.setTextIsSelectable(true);
        ScrollView sv = new ScrollView(this); sv.addView(tv); sv.setPadding(20, 10, 20, 0);
        new AlertDialog.Builder(this).setTitle(f.getName()).setView(sv).setPositiveButton("Kapat", null)
                .setNeutralButton("Paylaş", (d, w) -> Util.share(this, f.getName(), Util.tail(f, 150000))).show();
    }

    // ---------------------------------------------------------------- crash
    void checkCrash() {
        final File flag = Util.flag(this);
        if (!flag.exists()) return;
        flag.delete();
        new Thread(() -> {
            final File out = new File(Util.logs(this), "crash-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".txt");
            try (FileWriter w = new FileWriter(out)) {
                w.write("=== game.log (son kısım) ===\n" + Util.tail(Util.gameLog(this), 200000) + "\n\n=== logcat ===\n");
                Process p = new ProcessBuilder("logcat", "-d", "-t", "2000").redirectErrorStream(true).start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String s; while ((s = r.readLine()) != null) w.write(s + "\n");
                }
            } catch (Exception ignored) { }
            runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("Önceki oyun beklenmedik kapandı")
                    .setMessage("Crash logu kaydedildi: " + out.getName())
                    .setPositiveButton("Görüntüle", (d, w) -> show(out))
                    .setNeutralButton("Paylaş", (d, w) -> Util.share(this, out.getName(), Util.tail(out, 150000)))
                    .setNegativeButton("Tamam", null).show());
        }).start();
    }
}
