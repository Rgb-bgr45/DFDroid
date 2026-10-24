package com.rgb.dfdroid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

/** Hem oyun icinde (touch + touchpad/mouse) hem editorde (surukle-birak) kullanilan kontrol katmani. */
final class ControlsView extends View {
    interface Sink {
        void key(String key, boolean down);
        void mouse(int mask, int action, float x, float y);
    }

    static final Object PAD = new Object();
    ControlsConfig cfg;
    Sink sink;
    boolean edit;
    int opacity = 60;
    float cx, cy;
    int mask;
    ControlsConfig.Btn sel;
    boolean padSel;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(Paint.ANTI_ALIAS_FLAG), txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SparseArray<Object> owner = new SparseArray<>();
    private final SparseArray<float[]> last = new SparseArray<>();
    private long tapStart;
    private float moved;
    private boolean twoTap;

    ControlsView(Context c, ControlsConfig cfg, boolean edit) {
        super(c);
        this.cfg = cfg; this.edit = edit;
        line.setStyle(Paint.Style.STROKE); line.setStrokeWidth(3);
        txt.setTextAlign(Paint.Align.CENTER);
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        if (cx == 0 && cy == 0) { cx = w / 2f; cy = h / 2f; }
    }

    RectF rect(ControlsConfig.Btn b) {
        float W = getWidth(), H = getHeight(), h = b.s * H;
        return new RectF(b.x * W, b.y * H, b.x * W + h * b.wm, b.y * H + h);
    }

    RectF padRect() {
        float W = getWidth(), H = getHeight();
        return new RectF(cfg.padX * W, cfg.padY * H, (cfg.padX + cfg.padW) * W, (cfg.padY + cfg.padH) * H);
    }

    private ControlsConfig.Btn hit(float x, float y) {
        for (int i = cfg.buttons.size() - 1; i >= 0; i--) if (rect(cfg.buttons.get(i)).contains(x, y)) return cfg.buttons.get(i);
        return null;
    }

    @Override protected void onDraw(Canvas cv) {
        int a = opacity * 255 / 100;
        if (cfg.padOn) {
            RectF r = padRect();
            fill.setColor(Color.argb(a / 4, 255, 255, 255));
            cv.drawRoundRect(r, 24, 24, fill);
            line.setColor(edit && padSel ? Color.YELLOW : Color.argb(a, 255, 255, 255));
            cv.drawRoundRect(r, 24, 24, line);
            if (edit) { txt.setTextSize(40); txt.setColor(Color.WHITE); cv.drawText("TOUCHPAD", r.centerX(), r.centerY(), txt); }
        }
        for (ControlsConfig.Btn b : cfg.buttons) {
            RectF r = rect(b);
            fill.setColor(b.down ? Color.argb(Math.min(255, a + 90), 255, 255, 255) : Color.argb(a, 30, 30, 30));
            cv.drawRoundRect(r, 20, 20, fill);
            line.setColor(edit && b == sel ? Color.YELLOW : Color.argb(Math.max(a, 120), 255, 255, 255));
            cv.drawRoundRect(r, 20, 20, line);
            txt.setColor(b.down ? Color.BLACK : Color.argb(Math.max(a, 160), 255, 255, 255));
            txt.setTextSize(r.height() * 0.36f);
            cv.drawText(b.label, r.centerX(), r.centerY() + txt.getTextSize() * 0.35f, txt);
        }
        if (!edit && cfg.padOn) {
            line.setColor(Color.RED);
            cv.drawCircle(cx, cy, 10, line);
            cv.drawLine(cx - 18, cy, cx + 18, cy, line);
            cv.drawLine(cx, cy - 18, cx, cy + 18, line);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked(), i = e.getActionIndex(), id = e.getPointerId(i);
        if (edit) { editTouch(e, a, i, id); invalidate(); return true; }
        switch (a) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: down(id, e.getX(i), e.getY(i), e.getEventTime()); break;
            case MotionEvent.ACTION_MOVE:
                for (int k = 0; k < e.getPointerCount(); k++) move(e.getPointerId(k), e.getX(k), e.getY(k));
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: up(id, e.getEventTime()); break;
            case MotionEvent.ACTION_CANCEL:
                for (int k = owner.size() - 1; k >= 0; k--) up(owner.keyAt(k), 0);
                break;
        }
        invalidate();
        return true;
    }

    private int padCount() {
        int n = 0;
        for (int k = 0; k < owner.size(); k++) if (owner.valueAt(k) == PAD) n++;
        return n;
    }

    private void down(int id, float x, float y, long t) {
        ControlsConfig.Btn b = hit(x, y);
        if (b != null) { owner.put(id, b); press(b, true); return; }
        if (cfg.padOn && padRect().contains(x, y)) {
            owner.put(id, PAD); last.put(id, new float[]{x, y});
            if (padCount() == 1) { tapStart = t; moved = 0; twoTap = false; } else twoTap = true;
        }
    }

    private void move(int id, float x, float y) {
        if (owner.get(id) != PAD) return;
        float[] l = last.get(id);
        float dx = (x - l[0]) * cfg.sens, dy = (y - l[1]) * cfg.sens;
        l[0] = x; l[1] = y;
        moved += Math.abs(dx) + Math.abs(dy);
        cx = Math.max(0, Math.min(getWidth() - 1, cx + dx));
        cy = Math.max(0, Math.min(getHeight() - 1, cy + dy));
        if (sink != null) sink.mouse(mask, MotionEvent.ACTION_MOVE, cx, cy);
    }

    private void up(int id, long t) {
        Object o = owner.get(id);
        owner.remove(id); last.remove(id);
        if (o instanceof ControlsConfig.Btn) press((ControlsConfig.Btn) o, false);
        else if (o == PAD && t > 0 && tapStart > 0 && t - tapStart < 300 && moved < 40) {
            if (twoTap) click(2); else if (padCount() == 0) click(1);
            tapStart = 0;
        }
    }

    private void press(ControlsConfig.Btn b, boolean d) {
        b.down = d;
        if ("MOUSE_LEFT".equals(b.key) || "MOUSE_RIGHT".equals(b.key)) {
            int m = "MOUSE_LEFT".equals(b.key) ? 1 : 2;
            mask = d ? (mask | m) : (mask & ~m);
            if (sink != null) sink.mouse(mask, d ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP, cx, cy);
        } else if (sink != null) sink.key(b.key, d);
    }

    private void click(final int m) {
        mask |= m;
        if (sink != null) sink.mouse(mask, MotionEvent.ACTION_DOWN, cx, cy);
        postDelayed(() -> { mask &= ~m; if (sink != null) sink.mouse(mask, MotionEvent.ACTION_UP, cx, cy); }, 50);
    }

    // ---- editor ----
    private float ex, ey;

    private void editTouch(MotionEvent e, int a, int i, int id) {
        float x = e.getX(i), y = e.getY(i);
        if (a == MotionEvent.ACTION_DOWN) {
            ControlsConfig.Btn b = hit(x, y);
            sel = b; padSel = b == null && cfg.padOn && padRect().contains(x, y);
            ex = x; ey = y;
        } else if (a == MotionEvent.ACTION_MOVE) {
            float dx = (x - ex) / getWidth(), dy = (y - ey) / getHeight();
            ex = x; ey = y;
            if (sel != null) { sel.x = clamp(sel.x + dx); sel.y = clamp(sel.y + dy); }
            else if (padSel) { cfg.padX = clamp(cfg.padX + dx); cfg.padY = clamp(cfg.padY + dy); }
        }
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(0.97f, v)); }

    void scaleSelected(float f) {
        if (sel != null) sel.s = Math.max(0.05f, Math.min(0.4f, sel.s * f));
        else if (padSel) { cfg.padW = Math.min(1f, cfg.padW * f); cfg.padH = Math.min(1f, cfg.padH * f); }
        invalidate();
    }
}
