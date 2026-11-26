package com.bayarkuy.notifier;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Pembantu UI (tema gelap + gradien) agar seluruh layar dibuat dari kode tanpa file layout XML.
 */
public class Ui {

    public static final int BG = Color.parseColor("#0B1020");
    public static final int CARD = Color.parseColor("#151B33");
    public static final int CARD2 = Color.parseColor("#1D2547");
    public static final int STROKE = Color.parseColor("#2A3363");
    public static final int TEXT = Color.parseColor("#E5E7EB");
    public static final int MUTED = Color.parseColor("#94A3B8");
    public static final int SKY = Color.parseColor("#38BDF8");
    public static final int VIOLET = Color.parseColor("#8B5CF6");
    public static final int GREEN = Color.parseColor("#34D399");
    public static final int AMBER = Color.parseColor("#FBBF24");
    public static final int RED = Color.parseColor("#F87171");

    public static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static GradientDrawable round(Context c, int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static GradientDrawable roundStroke(Context c, int color, int radiusDp, int strokeColor) {
        GradientDrawable g = round(c, color, radiusDp);
        g.setStroke(dp(c, 1), strokeColor);
        return g;
    }

    public static GradientDrawable gradient(Context c, int c1, int c2, int radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{c1, c2});
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static LinearLayout.LayoutParams lp(Context c, int w, int h, int l, int t, int r, int b) {
        int ww = w > 0 ? dp(c, w) : w;
        int hh = h > 0 ? dp(c, h) : h;
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ww, hh);
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        return p;
    }

    public static LinearLayout.LayoutParams matchWrap(Context c, int l, int t, int r, int b) {
        return lp(c, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, l, t, r, b);
    }

    public static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(roundStroke(c, CARD, 20, STROKE));
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        return l;
    }

    public static void pressable(final View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent e) {
                int a = e.getAction();
                if (a == MotionEvent.ACTION_DOWN) {
                    view.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.85f).setDuration(80).start();
                } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                    view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120).start();
                }
                return false;
            }
        });
    }

    /** Tombol bergradien. style: 0 = utama (biru-ungu), 1 = hijau, 2 = redup, 3 = merah */
    public static TextView button(Context c, String label, int style) {
        TextView b = text(c, label, 14, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        int pad = dp(c, 14);
        b.setPadding(pad, pad, pad, pad);
        if (style == 1) b.setBackground(gradient(c, Color.parseColor("#10B981"), Color.parseColor("#059669"), 14));
        else if (style == 2) { b.setBackground(roundStroke(c, CARD2, 14, STROKE)); b.setTextColor(TEXT); }
        else if (style == 3) b.setBackground(gradient(c, Color.parseColor("#F43F5E"), Color.parseColor("#E11D48"), 14));
        else b.setBackground(gradient(c, Color.parseColor("#0EA5E9"), Color.parseColor("#7C3AED"), 14));
        b.setClickable(true);
        pressable(b);
        return b;
    }

    public static void setButtonEnabled(TextView b, boolean enabled) {
        b.setEnabled(enabled);
        b.setAlpha(enabled ? 1f : 0.35f);
    }

    public static TextView chip(Context c, String label, int color) {
        TextView t = text(c, label, 10, color, true);
        int ph = dp(c, 8);
        int pv = dp(c, 3);
        t.setPadding(ph, pv, ph, pv);
        t.setBackground(round(c, (color & 0x00FFFFFF) | 0x26000000, 10));
        return t;
    }

    public static EditText input(Context c, String hint, boolean password, boolean multiline) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(Color.parseColor("#64748B"));
        e.setTextColor(TEXT);
        e.setTextSize(13);
        e.setSingleLine(!multiline);
        if (multiline) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            e.setMinLines(2);
            e.setGravity(Gravity.TOP);
        } else if (password) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        }
        e.setBackground(roundStroke(c, BG, 12, STROKE));
        int p = dp(c, 12);
        e.setPadding(p, p, p, p);
        return e;
    }

    public static View spacer(Context c, int h) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, h)));
        return v;
    }

    public static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_LONG).show();
    }

    /** Waktu relatif sederhana. */
    public static String ago(long ts) {
        long d = (System.currentTimeMillis() - ts) / 1000L;
        if (d < 5) return "baru saja";
        if (d < 60) return d + " dtk lalu";
        if (d < 3600) return (d / 60) + " mnt lalu";
        if (d < 86400) return (d / 3600) + " jam lalu";
        return (d / 86400) + " hari lalu";
    }
}
