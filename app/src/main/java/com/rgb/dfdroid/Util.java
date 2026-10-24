package com.rgb.dfdroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import java.io.File;
import java.io.RandomAccessFile;

final class Util {
    static final String[] REQ = {"libs/Dwarf_Fortress", "libs/libgraphics.so", "data/init/init.txt", "raw/objects", "data/art"};

    static File root(Context c) { File f = c.getExternalFilesDir(null); return f != null ? f : c.getFilesDir(); }
    static File game(Context c) { return new File(root(c), "game"); }
    static File logs(Context c) { File d = new File(root(c), "logs"); d.mkdirs(); return d; }
    static File gameLog(Context c) { return new File(logs(c), "game.log"); }
    static File flag(Context c) { return new File(root(c), "running.flag"); }
    static File controls(Context c) { return new File(root(c), "controls.json"); }

    static boolean check(Context c, StringBuilder out) {
        boolean ok = true;
        for (String r : REQ) {
            boolean e = new File(game(c), r).exists();
            ok &= e;
            out.append(e ? "✔ " : "✘ ").append(r).append('\n');
        }
        return ok;
    }

    static String tail(File f, int max) {
        try (RandomAccessFile r = new RandomAccessFile(f, "r")) {
            long len = r.length(), start = Math.max(0, len - max);
            r.seek(start);
            byte[] b = new byte[(int) (len - start)];
            r.readFully(b);
            return new String(b);
        } catch (Exception e) { return ""; }
    }

    static void share(Activity a, String subject, String text) {
        if (text.length() > 150000) text = text.substring(text.length() - 150000);
        Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, text);
        a.startActivity(Intent.createChooser(i, subject));
    }

    static String setTok(String s, String tok, String val) {
        return s.replaceAll("\\[" + tok + ":[^\\]]*\\]", java.util.regex.Matcher.quoteReplacement("[" + tok + ":" + val + "]"));
    }

    static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }
}
