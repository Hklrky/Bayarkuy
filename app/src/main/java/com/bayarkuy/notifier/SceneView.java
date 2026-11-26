package com.bayarkuy.notifier;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * Ilustrasi animasi untuk setiap scene tutorial (digambar dengan Canvas, tanpa gambar eksternal).
 *
 * type: 0 sambutan, 1 alur kerja, 2 izin notifikasi, 3 baterai, 4 pilih aplikasi,
 *       5 koneksi, 6 uji parser, 7 selesai
 */
public class SceneView extends View {

    private int type = 0;
    private final long start = System.currentTimeMillis();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private final Path path = new Path();

    public SceneView(Context c) {
        super(c);
        stroke.setStyle(Paint.Style.STROKE);
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setFakeBoldText(true);
    }

    public void setType(int t) {
        this.type = t;
        invalidate();
    }

    private float t() {
        return (System.currentTimeMillis() - start) / 1000f;
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float u = Math.min(w, h) / 100f;
        float tm = t();

        // Lingkaran cahaya latar
        p.setShader(new LinearGradient(0, 0, w, h, Color.parseColor("#1E2A5A"), Color.parseColor("#2B1E5A"), Shader.TileMode.CLAMP));
        rf.set(cx - 46 * u, cy - 46 * u, cx + 46 * u, cy + 46 * u);
        c.drawOval(rf, p);
        p.setShader(null);

        for (int i = 0; i < 3; i++) {
            float ph = (tm * 0.6f + i / 3f) % 1f;
            stroke.setColor(Color.argb((int) (90 * (1 - ph)), 56, 189, 248));
            stroke.setStrokeWidth(2 * u / 2f);
            c.drawCircle(cx, cy, (30 + ph * 22) * u, stroke);
        }

        switch (type) {
            case 0: drawWelcome(c, cx, cy, u, tm); break;
            case 1: drawFlow(c, cx, cy, u, tm); break;
            case 2: drawLock(c, cx, cy, u, tm); break;
            case 3: drawBattery(c, cx, cy, u, tm); break;
            case 4: drawApps(c, cx, cy, u, tm); break;
            case 5: drawLink(c, cx, cy, u, tm); break;
            case 6: drawParser(c, cx, cy, u, tm); break;
            default: drawDone(c, cx, cy, u, tm); break;
        }

        postInvalidateOnAnimation();
    }

    /* ---------------- komponen ---------------- */

    private void phone(Canvas c, float cx, float cy, float u, float tilt) {
        c.save();
        c.rotate(tilt, cx, cy);
        p.setColor(Color.parseColor("#0F1530"));
        rf.set(cx - 14 * u, cy - 24 * u, cx + 14 * u, cy + 24 * u);
        c.drawRoundRect(rf, 5 * u, 5 * u, p);
        stroke.setColor(Color.parseColor("#38BDF8"));
        stroke.setStrokeWidth(0.8f * u);
        c.drawRoundRect(rf, 5 * u, 5 * u, stroke);
        p.setColor(Color.parseColor("#1D2547"));
        rf.set(cx - 11.5f * u, cy - 20 * u, cx + 11.5f * u, cy + 20 * u);
        c.drawRoundRect(rf, 3 * u, 3 * u, p);
        c.restore();
    }

    private void bell(Canvas c, float x, float y, float u, int color, float swing) {
        c.save();
        c.rotate(swing, x, y - 5 * u);
        p.setColor(color);
        path.reset();
        path.moveTo(x - 5 * u, y + 3 * u);
        path.quadTo(x - 5 * u, y - 6 * u, x, y - 6 * u);
        path.quadTo(x + 5 * u, y - 6 * u, x + 5 * u, y + 3 * u);
        path.close();
        c.drawPath(path, p);
        c.drawRect(x - 6.5f * u, y + 3 * u, x + 6.5f * u, y + 4.5f * u, p);
        c.drawCircle(x, y + 6.5f * u, 1.6f * u, p);
        c.restore();
    }

    private void cloud(Canvas c, float x, float y, float u, int color) {
        p.setColor(color);
        c.drawCircle(x - 5 * u, y, 5 * u, p);
        c.drawCircle(x + 1 * u, y - 3 * u, 6.5f * u, p);
        c.drawCircle(x + 7 * u, y, 5 * u, p);
        rf.set(x - 10 * u, y, x + 12 * u, y + 5 * u);
        c.drawRoundRect(rf, 2.5f * u, 2.5f * u, p);
    }

    private void tick(Canvas c, float x, float y, float u, int color, float scale) {
        stroke.setColor(color);
        stroke.setStrokeWidth(2.2f * u * scale);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(x - 6 * u * scale, y, x - 1.5f * u * scale, y + 5 * u * scale, stroke);
        c.drawLine(x - 1.5f * u * scale, y + 5 * u * scale, x + 7 * u * scale, y - 5 * u * scale, stroke);
        stroke.setStrokeCap(Paint.Cap.BUTT);
    }

    /* ---------------- scene ---------------- */

    private void drawWelcome(Canvas c, float cx, float cy, float u, float tm) {
        phone(c, cx, cy, u, (float) Math.sin(tm * 1.4f) * 4f);
        bell(c, cx, cy - 2 * u, u * 1.4f, Color.parseColor("#FBBF24"), (float) Math.sin(tm * 6f) * 14f);
        // Koin melayang
        for (int i = 0; i < 4; i++) {
            float a = tm * 0.9f + i * 1.57f;
            float x = cx + (float) Math.cos(a) * 30 * u;
            float y = cy + (float) Math.sin(a) * 18 * u;
            p.setColor(Color.parseColor("#34D399"));
            c.drawCircle(x, y, 3.2f * u, p);
            txt.setColor(Color.parseColor("#0B1020"));
            txt.setTextSize(3.6f * u);
            c.drawText("$", x, y + 1.3f * u, txt);
        }
    }

    private void drawFlow(Canvas c, float cx, float cy, float u, float tm) {
        float lx = cx - 30 * u;
        float rx = cx + 30 * u;
        phone(c, lx, cy, u * 0.8f, 0);
        bell(c, lx, cy - 1 * u, u * 1.0f, Color.parseColor("#FBBF24"), (float) Math.sin(tm * 6f) * 10f);
        cloud(c, rx, cy, u * 1.3f, Color.parseColor("#38BDF8"));
        // paket data bergerak
        for (int i = 0; i < 4; i++) {
            float ph = (tm * 0.55f + i / 4f) % 1f;
            float x = lx + 12 * u + ph * (rx - lx - 26 * u);
            p.setColor(Color.argb((int) (255 * (1 - Math.abs(ph - 0.5f) * 1.2f)), 52, 211, 153));
            rf.set(x - 2.2f * u, cy - 1.6f * u, x + 2.2f * u, cy + 1.6f * u);
            c.drawRoundRect(rf, u, u, p);
        }
        float pulse = (float) (0.9 + 0.1 * Math.sin(tm * 4f));
        tick(c, rx + 2 * u, cy + 14 * u, u, Color.parseColor("#34D399"), pulse);
    }

    private void drawLock(Canvas c, float cx, float cy, float u, float tm) {
        // Gembok -> kunci terbuka bergantian
        float open = (float) (0.5 + 0.5 * Math.sin(tm * 1.8f));
        p.setColor(Color.parseColor("#8B5CF6"));
        rf.set(cx - 13 * u, cy - 4 * u, cx + 13 * u, cy + 20 * u);
        c.drawRoundRect(rf, 4 * u, 4 * u, p);
        stroke.setColor(Color.parseColor("#E5E7EB"));
        stroke.setStrokeWidth(3 * u);
        rf.set(cx - 8 * u, cy - 20 * u - open * 5 * u, cx + 8 * u, cy + 2 * u - open * 5 * u);
        c.drawArc(rf, 180, 180, false, stroke);
        c.drawLine(cx - 8 * u, cy - 9 * u - open * 5 * u, cx - 8 * u, cy - 4 * u, stroke);
        p.setColor(Color.parseColor("#0B1020"));
        c.drawCircle(cx, cy + 9 * u, 2.4f * u, p);
        bell(c, cx + 24 * u, cy - 16 * u, u * 0.9f, Color.parseColor("#FBBF24"), (float) Math.sin(tm * 6f) * 12f);
    }

    private void drawBattery(Canvas c, float cx, float cy, float u, float tm) {
        stroke.setColor(Color.parseColor("#E5E7EB"));
        stroke.setStrokeWidth(1.6f * u);
        rf.set(cx - 22 * u, cy - 11 * u, cx + 20 * u, cy + 11 * u);
        c.drawRoundRect(rf, 3 * u, 3 * u, stroke);
        p.setColor(Color.parseColor("#E5E7EB"));
        rf.set(cx + 20 * u, cy - 4 * u, cx + 24 * u, cy + 4 * u);
        c.drawRoundRect(rf, u, u, p);
        float lvl = 0.35f + 0.65f * (float) (0.5 + 0.5 * Math.sin(tm * 1.5f));
        p.setColor(lvl > 0.5f ? Color.parseColor("#34D399") : Color.parseColor("#FBBF24"));
        rf.set(cx - 19.5f * u, cy - 8.5f * u, cx - 19.5f * u + 36 * u * lvl, cy + 8.5f * u);
        c.drawRoundRect(rf, 2 * u, 2 * u, p);
        // petir
        p.setColor(Color.parseColor("#0B1020"));
        path.reset();
        path.moveTo(cx - 2 * u, cy - 8 * u);
        path.lineTo(cx - 8 * u, cy + 1 * u);
        path.lineTo(cx - 3 * u, cy + 1 * u);
        path.lineTo(cx - 5 * u, cy + 8 * u);
        path.lineTo(cx + 3 * u, cy - 2 * u);
        path.lineTo(cx - 2 * u, cy - 2 * u);
        path.close();
        c.drawPath(path, p);
    }

    private void drawApps(Canvas c, float cx, float cy, float u, float tm) {
        int[] cols = {Color.parseColor("#10B981"), Color.parseColor("#0EA5E9"), Color.parseColor("#F59E0B"),
                      Color.parseColor("#8B5CF6"), Color.parseColor("#F43F5E"), Color.parseColor("#14B8A6")};
        for (int i = 0; i < 6; i++) {
            int col = i % 3;
            int row = i / 3;
            float x = cx + (col - 1) * 20 * u;
            float y = cy + (row - 0.5f) * 22 * u;
            float s = 1f + 0.08f * (float) Math.sin(tm * 2.4f + i);
            p.setColor(cols[i]);
            rf.set(x - 8 * u * s, y - 8 * u * s, x + 8 * u * s, y + 8 * u * s);
            c.drawRoundRect(rf, 3.5f * u, 3.5f * u, p);
            boolean on = ((int) (tm * 1.2f + i) % 2) == 0;
            if (on) tick(c, x, y, u * 0.6f, Color.WHITE, 1f);
        }
    }

    private void drawLink(Canvas c, float cx, float cy, float u, float tm) {
        float k = (float) (0.5 + 0.5 * Math.sin(tm * 2f));
        float gap = 8 * u * (1 - k);
        stroke.setColor(Color.parseColor("#38BDF8"));
        stroke.setStrokeWidth(3 * u);
        rf.set(cx - 22 * u - gap, cy - 7 * u, cx + 2 * u - gap, cy + 7 * u);
        c.drawRoundRect(rf, 7 * u, 7 * u, stroke);
        stroke.setColor(Color.parseColor("#8B5CF6"));
        rf.set(cx - 2 * u + gap, cy - 7 * u, cx + 22 * u + gap, cy + 7 * u);
        c.drawRoundRect(rf, 7 * u, 7 * u, stroke);
        if (k > 0.8f) tick(c, cx, cy - 18 * u, u, Color.parseColor("#34D399"), 1f);
    }

    private void drawParser(Canvas c, float cx, float cy, float u, float tm) {
        p.setColor(Color.parseColor("#0F1530"));
        rf.set(cx - 30 * u, cy - 16 * u, cx + 30 * u, cy + 16 * u);
        c.drawRoundRect(rf, 5 * u, 5 * u, p);
        txt.setColor(Color.parseColor("#94A3B8"));
        txt.setTextSize(4 * u);
        c.drawText("Dana masuk Rp 10.009", cx, cy - 3 * u, txt);
        float ph = (tm * 0.7f) % 1f;
        p.setColor(Color.argb(70, 52, 211, 153));
        rf.set(cx + 4 * u - 20 * u * (1 - ph), cy - 8 * u, cx + 24 * u, cy + 0.5f * u);
        c.drawRoundRect(rf, 2 * u, 2 * u, p);
        txt.setColor(Color.parseColor("#34D399"));
        txt.setTextSize(6 * u);
        c.drawText("= 10009", cx, cy + 11 * u, txt);
    }

    private void drawDone(Canvas c, float cx, float cy, float u, float tm) {
        float s = Math.min(1f, tm * 1.5f % 3f);
        p.setColor(Color.parseColor("#10B981"));
        c.drawCircle(cx, cy, 22 * u * (0.92f + 0.08f * (float) Math.sin(tm * 3f)), p);
        tick(c, cx, cy, u * 1.6f, Color.WHITE, 1f);
        for (int i = 0; i < 10; i++) {
            float a = i * 0.628f + tm * 0.6f;
            float r = (30 + 6 * (float) Math.sin(tm * 3f + i)) * u;
            p.setColor(i % 2 == 0 ? Color.parseColor("#FBBF24") : Color.parseColor("#38BDF8"));
            c.drawCircle(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, 1.6f * u, p);
        }
    }
}
