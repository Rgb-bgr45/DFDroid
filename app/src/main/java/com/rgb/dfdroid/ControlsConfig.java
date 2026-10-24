package com.rgb.dfdroid;

import android.content.Context;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Touch kontrol yerlesimi: controls.json. (Floating menu butonu JSON'da DEGIL, launcher icinde sabit.) */
final class ControlsConfig {
    static final class Btn {
        String id, label, key;
        float x, y, s, wm = 1f;   // x,y: ekran orani (sol-ust), s: yukseklik orani, wm: genislik carpani
        boolean down;             // sadece calisma zamani
    }

    final List<Btn> buttons = new ArrayList<>();
    boolean padOn = true;
    float padX = .52f, padY = .08f, padW = .46f, padH = .9f, sens = 1.6f;

    static ControlsConfig load(Context c) {
        File f = Util.controls(c);
        if (f.exists()) {
            try { return parse(new String(Files.readAllBytes(f.toPath()))); } catch (Exception ignored) { }
        }
        return defaults();
    }

    static ControlsConfig parse(String json) throws Exception {
        JSONObject o = new JSONObject(json);
        ControlsConfig c = new ControlsConfig();
        JSONObject p = o.optJSONObject("touchpad");
        if (p != null) {
            c.padOn = p.optBoolean("enabled", true);
            c.padX = (float) p.optDouble("x", c.padX); c.padY = (float) p.optDouble("y", c.padY);
            c.padW = (float) p.optDouble("w", c.padW); c.padH = (float) p.optDouble("h", c.padH);
            c.sens = (float) p.optDouble("sensitivity", c.sens);
        }
        JSONArray a = o.optJSONArray("buttons");
        if (a != null) for (int i = 0; i < a.length(); i++) {
            JSONObject b = a.getJSONObject(i);
            Btn n = new Btn();
            n.id = b.optString("id", "b" + i); n.label = b.optString("label", "?"); n.key = b.optString("key", "SPACE");
            n.x = (float) b.optDouble("x", 0.1); n.y = (float) b.optDouble("y", 0.1);
            n.s = (float) b.optDouble("size", 0.12); n.wm = (float) b.optDouble("widthMul", 1.0);
            c.buttons.add(n);
        }
        return c;
    }

    String toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("version", 1);
            JSONObject p = new JSONObject();
            p.put("enabled", padOn).put("x", padX).put("y", padY).put("w", padW).put("h", padH).put("sensitivity", sens);
            o.put("touchpad", p);
            JSONArray a = new JSONArray();
            for (Btn b : buttons) {
                a.put(new JSONObject().put("id", b.id).put("label", b.label).put("key", b.key)
                        .put("x", b.x).put("y", b.y).put("size", b.s).put("widthMul", b.wm));
            }
            o.put("buttons", a);
            return o.toString(2);
        } catch (Exception e) { return "{}"; }
    }

    void save(Context c) {
        try { Files.write(Util.controls(c).toPath(), toJson().getBytes()); } catch (Exception ignored) { }
    }

    private void add(String id, String label, String key, float x, float y, float s) {
        Btn b = new Btn(); b.id = id; b.label = label; b.key = key; b.x = x; b.y = y; b.s = s; buttons.add(b);
    }

    static ControlsConfig defaults() {
        ControlsConfig c = new ControlsConfig();
        c.add("up", "▲", "DPAD_UP", .085f, .50f, .13f);
        c.add("down", "▼", "DPAD_DOWN", .085f, .80f, .13f);
        c.add("left", "◀", "DPAD_LEFT", .01f, .65f, .13f);
        c.add("right", "▶", "DPAD_RIGHT", .16f, .65f, .13f);
        c.add("esc", "ESC", "ESCAPE", .01f, .02f, .11f);
        c.add("enter", "ENT", "ENTER", .08f, .02f, .11f);
        c.add("space", "SPC", "SPACE", .15f, .02f, .11f);
        c.add("tab", "TAB", "TAB", .22f, .02f, .11f);
        c.add("help", "?", "SHIFT_LEFT+SLASH", .29f, .02f, .11f);
        c.add("lt", "<", "SHIFT_LEFT+COMMA", .01f, .15f, .11f);
        c.add("gt", ">", "SHIFT_LEFT+PERIOD", .08f, .15f, .11f);
        c.add("plus", "+", "PLUS", .15f, .15f, .11f);
        c.add("minus", "-", "MINUS", .22f, .15f, .11f);
        c.add("kb", "KB", "KEYBOARD", .29f, .15f, .11f);
        c.add("lmb", "LMB", "MOUSE_LEFT", .40f, .62f, .16f);
        c.add("rmb", "RMB", "MOUSE_RIGHT", .40f, .80f, .16f);
        return c;
    }
}
