package com.bayarkuy.notifier;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Dashboard utama: Beranda (status), Log, Atur, Bantuan.
 */
public class MainActivity extends Activity {

    private Prefs prefs;
    private Handler handler = new Handler();

    private FrameLayout bodyHost;
    private LinearLayout navBar;
    private TextView headerChip;
    private int tab = 0;
    private boolean visible = false;

    private TextView testResult;
    private String testMsg = "";
    private int testColor = Ui.MUTED;

    private final Runnable refresher = new Runnable() {
        public void run() {
            if (!visible) return;
            updateHeaderChip();
            if (tab == 0 || tab == 1) {
                showTab(tab, false);
            }
            handler.postDelayed(this, 3000L);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);

        if (!prefs.isOnboarded()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        buildFrame();
        showTab(0, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bodyHost == null) return;
        visible = true;
        updateHeaderChip();
        handler.removeCallbacks(refresher);
        handler.postDelayed(refresher, 3000L);
        if (tab == 0 || tab == 1) showTab(tab, false);
        Dispatcher.kick(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        visible = false;
        handler.removeCallbacks(refresher);
    }

    /* ======================= kerangka ======================= */

    private void buildFrame() {
        final Context c = this;
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        // Header
        LinearLayout header = new LinearLayout(c);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(Ui.dp(c, 20), Ui.dp(c, 16), Ui.dp(c, 20), Ui.dp(c, 10));

        TextView logo = Ui.text(c, "🔔", 18, Color.WHITE, false);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(Ui.gradient(c, Color.parseColor("#0EA5E9"), Color.parseColor("#7C3AED"), 14));
        header.addView(logo, new LinearLayout.LayoutParams(Ui.dp(c, 42), Ui.dp(c, 42)));

        LinearLayout titles = new LinearLayout(c);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(Ui.dp(c, 12), 0, 0, 0);
        titles.addView(Ui.text(c, "BayarKuy Notifier", 17, Color.WHITE, true));
        titles.addView(Ui.text(c, "Payment gateway notifier • GoBiz & Bank", 11, Ui.MUTED, false));
        header.addView(titles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        headerChip = Ui.chip(c, "…", Ui.MUTED);
        header.addView(headerChip);

        root.addView(header, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        bodyHost = new FrameLayout(c);
        root.addView(bodyHost, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // Navigasi bawah
        navBar = new LinearLayout(c);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setBackgroundColor(Ui.CARD);
        navBar.setPadding(Ui.dp(c, 8), Ui.dp(c, 6), Ui.dp(c, 8), Ui.dp(c, 10));
        root.addView(navBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void buildNav() {
        final Context c = this;
        navBar.removeAllViews();
        String[] icons = {"🏠", "📜", "⚙️", "📘"};
        String[] labels = {"Beranda", "Log", "Atur", "Bantuan"};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(c);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 6));
            boolean on = i == tab;
            if (on) item.setBackground(Ui.round(c, Ui.CARD2, 14));
            TextView ic = Ui.text(c, icons[i], 18, Color.WHITE, false);
            ic.setGravity(Gravity.CENTER);
            TextView lb = Ui.text(c, labels[i], 10, on ? Ui.SKY : Ui.MUTED, true);
            lb.setGravity(Gravity.CENTER);
            item.addView(ic);
            item.addView(lb);
            item.setClickable(true);
            Ui.pressable(item);
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showTab(idx, true);
                }
            });
            navBar.addView(item, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
    }

    private void updateHeaderChip() {
        if (headerChip == null) return;
        int[] st = computeState();
        String[] names = {"AKTIF", "MENUNGGU", "BELUM TERHUBUNG", "IZIN BELUM AKTIF"};
        int[] cols = {Ui.GREEN, Ui.AMBER, Ui.AMBER, Ui.RED};
        headerChip.setText(names[st[0]]);
        headerChip.setTextColor(cols[st[0]]);
        headerChip.setBackground(Ui.round(this, (cols[st[0]] & 0x00FFFFFF) | 0x26000000, 10));
    }

    /** 0 aktif, 1 menunggu layanan, 2 belum terhubung, 3 izin belum aktif */
    private int[] computeState() {
        boolean perm = Perm.isListenerEnabled(this);
        if (!perm) return new int[]{3};
        if (!prefs.isConfigured()) return new int[]{2};
        long alive = prefs.getListenerAlive();
        long hb = prefs.getLastHeartbeat();
        long now = System.currentTimeMillis();
        if (alive > 0 && now - alive < 150000L && hb > 0 && now - hb < 180000L) return new int[]{0};
        return new int[]{1};
    }

    private void showTab(int idx, boolean animate) {
        tab = idx;
        buildNav();

        int scrollY = 0;
        if (!animate && bodyHost.getChildCount() > 0 && bodyHost.getChildAt(0) instanceof ScrollView) {
            scrollY = bodyHost.getChildAt(0).getScrollY();
        }

        bodyHost.removeAllViews();

        final ScrollView sv = new ScrollView(this);
        sv.setFillViewport(false);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(this, 20), Ui.dp(this, 6), Ui.dp(this, 20), Ui.dp(this, 24));
        sv.addView(col, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        bodyHost.addView(sv, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        if (idx == 0) buildHome(col);
        else if (idx == 1) buildLog(col);
        else if (idx == 2) buildSettings(col);
        else buildHelp(col);

        if (animate) {
            AlphaAnimation a = new AlphaAnimation(0f, 1f);
            a.setDuration(260);
            col.startAnimation(a);
        } else if (scrollY > 0) {
            final int fy = scrollY;
            sv.post(new Runnable() {
                public void run() {
                    sv.scrollTo(0, fy);
                }
            });
        }
    }

    /* ======================= BERANDA ======================= */

    private void buildHome(LinearLayout col) {
        final Context c = this;
        int state = computeState()[0];

        // Kartu status utama
        LinearLayout hero = new LinearLayout(c);
        hero.setOrientation(LinearLayout.VERTICAL);
        int c1;
        int c2;
        String big;
        String small;
        if (state == 0) {
            c1 = Color.parseColor("#059669"); c2 = Color.parseColor("#0EA5E9");
            big = "Aktif memantau"; small = "Layanan berjalan dan terhubung ke web.";
        } else if (state == 1) {
            c1 = Color.parseColor("#D97706"); c2 = Color.parseColor("#7C3AED");
            big = "Menunggu layanan"; small = "Belum ada detak jantung terbaru. Pastikan internet aktif & baterai tidak dibatasi.";
        } else if (state == 2) {
            c1 = Color.parseColor("#D97706"); c2 = Color.parseColor("#DB2777");
            big = "Belum terhubung"; small = "Tempel Kode Koneksi dari web di menu Atur.";
        } else {
            c1 = Color.parseColor("#E11D48"); c2 = Color.parseColor("#7C3AED");
            big = "Izin belum aktif"; small = "Aktifkan Akses Notifikasi agar notifikasi bisa dibaca.";
        }
        hero.setBackground(Ui.gradient(c, c1, c2, 24));
        hero.setPadding(Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 20));

        TextView dot = Ui.text(c, state == 0 ? "● LIVE" : "● STATUS", 11, Color.WHITE, true);
        hero.addView(dot);
        if (state == 0) {
            AlphaAnimation pulse = new AlphaAnimation(1f, 0.25f);
            pulse.setDuration(900);
            pulse.setRepeatMode(AlphaAnimation.REVERSE);
            pulse.setRepeatCount(AlphaAnimation.INFINITE);
            dot.startAnimation(pulse);
        }
        hero.addView(Ui.text(c, big, 26, Color.WHITE, true), Ui.matchWrap(c, 0, 6, 0, 4));
        hero.addView(Ui.text(c, small, 13, Color.parseColor("#E0F2FE"), false));
        col.addView(hero, Ui.matchWrap(c, 0, 4, 0, 14));

        // Statistik
        LinearLayout stats = new LinearLayout(c);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(statBox("Terkirim", String.valueOf(prefs.getSent()), Ui.SKY), statLp(c, false));
        stats.addView(statBox("Cocok", String.valueOf(prefs.getMatched()), Ui.GREEN), statLp(c, true));
        stats.addView(statBox("Gagal", String.valueOf(prefs.getFailed()), Ui.RED), statLp(c, true));
        stats.addView(statBox("Antrean", String.valueOf(Store.get(c).queueSize()), Ui.AMBER), statLp(c, true));
        col.addView(stats, Ui.matchWrap(c, 0, 0, 0, 14));

        // Daftar periksa
        LinearLayout check = Ui.card(c);
        check.addView(Ui.text(c, "Daftar periksa", 14, Color.WHITE, true), Ui.matchWrap(c, 0, 0, 0, 8));
        addCheck(check, Perm.isListenerEnabled(c), "Akses Notifikasi", "Aktif", "Belum aktif", new Runnable() {
            public void run() { Perm.openListenerSettings(c); }
        });
        addCheck(check, Perm.isIgnoringBattery(c), "Baterai tidak dibatasi", "Aman", "Masih dibatasi", new Runnable() {
            public void run() { Perm.requestIgnoreBattery(c); }
        });
        long hb = prefs.getLastHeartbeat();
        addCheck(check, prefs.isConfigured() && hb > 0 && System.currentTimeMillis() - hb < 180000L,
            "Koneksi ke web", hb > 0 ? "Terakhir " + Ui.ago(hb) : "OK", prefs.isConfigured() ? "Belum ada respons" : "Belum diisi", new Runnable() {
            public void run() { showTab(2, true); }
        });
        col.addView(check, Ui.matchWrap(c, 0, 0, 0, 14));

        // Galat terakhir
        String err = prefs.getLastError();
        if (err.length() > 0) {
            LinearLayout ec = Ui.card(c);
            ec.setBackground(Ui.roundStroke(c, Color.parseColor("#2A1620"), 16, Ui.RED));
            ec.addView(Ui.text(c, "Galat terakhir", 12, Ui.RED, true));
            ec.addView(Ui.text(c, err, 12, Ui.TEXT, false));
            col.addView(ec, Ui.matchWrap(c, 0, 0, 0, 14));
        }

        // Aksi
        testResult = Ui.text(c, testMsg, 12, testColor, true);

        TextView bTest = Ui.button(c, "Tes koneksi ke web", 0);
        bTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                testColor = Ui.AMBER;
                testMsg = "Menguji koneksi…";
                testResult.setTextColor(testColor);
                testResult.setText(testMsg);
                Dispatcher.testConnection(c, new Dispatcher.Callback() {
                    public void onResult(boolean ok, String message) {
                        testColor = ok ? Ui.GREEN : Ui.RED;
                        testMsg = (ok ? "✅ " : "⚠️ ") + message;
                        if (testResult != null) {
                            testResult.setTextColor(testColor);
                            testResult.setText(testMsg);
                        }
                        updateHeaderChip();
                    }
                });
            }
        });
        col.addView(bTest, Ui.matchWrap(c, 0, 0, 0, 8));
        col.addView(testResult, Ui.matchWrap(c, 2, 0, 2, 10));

        TextView bSweep = Ui.button(c, "Pindai semua notifikasi sekarang", 2);
        bSweep.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                NotifListener l = NotifListener.instance;
                if (l == null) {
                    Ui.toast(c, "Layanan pembaca notifikasi belum aktif. Periksa Akses Notifikasi.");
                    return;
                }
                int n = l.sweep();
                Ui.toast(c, n + " notifikasi dari bilah notifikasi diteruskan ke antrean.");
                showTab(0, false);
            }
        });
        col.addView(bSweep, Ui.matchWrap(c, 0, 0, 0, 8));

        TextView bQueue = Ui.button(c, "Kirim ulang antrean", 2);
        bQueue.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Dispatcher.kick(c);
                Ui.toast(c, "Mencoba mengirim antrean…");
            }
        });
        col.addView(bQueue, Ui.matchWrap(c, 0, 0, 0, 8));

        TextView bTut = Ui.button(c, "Ulangi tutorial", 2);
        bTut.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(c, OnboardingActivity.class));
            }
        });
        col.addView(bTut, Ui.matchWrap(c, 0, 0, 0, 0));
    }

    private LinearLayout.LayoutParams statLp(Context c, boolean margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        if (margin) p.leftMargin = Ui.dp(c, 8);
        return p;
    }

    private View statBox(String label, String value, int color) {
        Context c = this;
        LinearLayout b = new LinearLayout(c);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.roundStroke(c, Ui.CARD, 16, Ui.STROKE));
        b.setPadding(Ui.dp(c, 4), Ui.dp(c, 12), Ui.dp(c, 4), Ui.dp(c, 12));
        TextView v = Ui.text(c, value, 20, color, true);
        v.setGravity(Gravity.CENTER);
        TextView l = Ui.text(c, label, 10, Ui.MUTED, true);
        l.setGravity(Gravity.CENTER);
        b.addView(v);
        b.addView(l);
        return b;
    }

    private void addCheck(LinearLayout parent, boolean ok, String name, String okText, String badText, final Runnable fix) {
        Context c = this;
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8));

        TextView ic = Ui.text(c, ok ? "✅" : "⚠️", 16, Color.WHITE, false);
        row.addView(ic, new LinearLayout.LayoutParams(Ui.dp(c, 32), LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout t = new LinearLayout(c);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(Ui.text(c, name, 13, Ui.TEXT, true));
        t.addView(Ui.text(c, ok ? okText : badText, 11, ok ? Ui.GREEN : Ui.AMBER, false));
        row.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (!ok) {
            TextView fixBtn = Ui.chip(c, "PERBAIKI", Ui.SKY);
            fixBtn.setClickable(true);
            fixBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    fix.run();
                }
            });
            row.addView(fixBtn);
        }
        parent.addView(row);
    }

    /* ======================= LOG ======================= */

    private void buildLog(LinearLayout col) {
        final Context c = this;

        LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(Ui.text(c, "Log notifikasi", 18, Color.WHITE, true), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView clear = Ui.chip(c, "HAPUS LOG", Ui.RED);
        clear.setClickable(true);
        clear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Store.get(c).clearLogs();
                showTab(1, false);
            }
        });
        head.addView(clear);
        col.addView(head, Ui.matchWrap(c, 0, 4, 0, 12));

        List<Store.Log> logs = Store.get(c).recentLogs(80);
        if (logs.isEmpty()) {
            LinearLayout empty = Ui.card(c);
            empty.setGravity(Gravity.CENTER);
            empty.addView(Ui.text(c, "📭", 36, Color.WHITE, false));
            TextView t = Ui.text(c, "Belum ada notifikasi terbaca.\nSaat ada dana masuk di aplikasi yang dipantau, akan muncul di sini.", 13, Ui.MUTED, false);
            t.setGravity(Gravity.CENTER);
            empty.addView(t);
            col.addView(empty, Ui.matchWrap(c, 0, 0, 0, 0));
            return;
        }

        for (int i = 0; i < logs.size(); i++) {
            Store.Log l = logs.get(i);
            LinearLayout card = Ui.card(c);

            LinearLayout r1 = new LinearLayout(c);
            r1.setOrientation(LinearLayout.HORIZONTAL);
            r1.setGravity(Gravity.CENTER_VERTICAL);
            r1.addView(Ui.text(c, l.app == null ? "-" : l.app, 12, Ui.SKY, true), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            r1.addView(Ui.chip(c, l.status == null ? "-" : l.status, statusColor(l.status)));
            card.addView(r1);

            if (l.amount > 0) {
                card.addView(Ui.text(c, Money.rupiah(l.amount) + (l.direction != null && l.direction.equals("out") ? "  ↗ keluar" : "  ↙ masuk"), 18, Color.WHITE, true), Ui.matchWrap(c, 0, 6, 0, 0));
            }
            if (l.title != null && l.title.length() > 0) {
                card.addView(Ui.text(c, l.title, 12, Ui.TEXT, true), Ui.matchWrap(c, 0, 4, 0, 0));
            }
            if (l.text != null && l.text.length() > 0) {
                String tx = l.text.length() > 180 ? l.text.substring(0, 180) + "…" : l.text;
                card.addView(Ui.text(c, tx, 12, Ui.MUTED, false));
            }
            String foot = Ui.ago(l.ts) + (l.detail != null && l.detail.length() > 0 ? "  •  " + l.detail : "");
            card.addView(Ui.text(c, foot, 10, Ui.MUTED, false), Ui.matchWrap(c, 0, 6, 0, 0));

            col.addView(card, Ui.matchWrap(c, 0, 0, 0, 10));
        }
    }

    private int statusColor(String s) {
        if (s == null) return Ui.MUTED;
        if (s.equals("MATCHED")) return Ui.GREEN;
        if (s.equals("UNMATCHED")) return Ui.AMBER;
        if (s.equals("TERKIRIM")) return Ui.SKY;
        if (s.equals("ANTRE")) return Ui.VIOLET;
        if (s.equals("GAGAL")) return Ui.RED;
        return Ui.MUTED;
    }

    /* ======================= ATUR ======================= */

    private void buildSettings(LinearLayout col) {
        final Context c = this;
        col.addView(Ui.text(c, "Pengaturan", 18, Color.WHITE, true), Ui.matchWrap(c, 0, 4, 0, 12));

        // ---- Koneksi ----
        LinearLayout conn = Ui.card(c);
        conn.addView(Ui.text(c, "Koneksi ke website", 14, Color.WHITE, true), Ui.matchWrap(c, 0, 0, 0, 4));
        conn.addView(Ui.text(c, "Tempel Kode Koneksi dari web (menu Notifier), atau isi manual.", 12, Ui.MUTED, false), Ui.matchWrap(c, 0, 0, 0, 10));

        final EditText code = Ui.input(c, "Kode Koneksi (BKY1:...)", false, true);
        conn.addView(code, Ui.matchWrap(c, 0, 0, 0, 8));

        final TextView msg = Ui.text(c, "", 12, Ui.MUTED, true);

        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        TextView paste = Ui.button(c, "Tempel", 2);
        paste.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                        CharSequence cs = cm.getPrimaryClip().getItemAt(0).coerceToText(c);
                        code.setText(cs == null ? "" : cs.toString().trim());
                    }
                } catch (Exception ignored) {
                }
            }
        });
        r.addView(paste, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView apply = Ui.button(c, "Terapkan & Tes", 0);
        apply.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String err = prefs.applyConnectCode(code.getText().toString());
                if (err != null) {
                    msg.setTextColor(Ui.RED);
                    msg.setText(err);
                    return;
                }
                msg.setTextColor(Ui.AMBER);
                msg.setText("Menguji koneksi…");
                Dispatcher.testConnection(c, new Dispatcher.Callback() {
                    public void onResult(boolean ok, String message) {
                        msg.setTextColor(ok ? Ui.GREEN : Ui.RED);
                        msg.setText((ok ? "✅ " : "⚠️ ") + message);
                        updateHeaderChip();
                    }
                });
            }
        });
        LinearLayout.LayoutParams lpa = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f);
        lpa.leftMargin = Ui.dp(c, 8);
        r.addView(apply, lpa);
        conn.addView(r, Ui.matchWrap(c, 0, 0, 0, 10));

        conn.addView(Ui.text(c, "Manual", 12, Ui.SKY, true), Ui.matchWrap(c, 0, 4, 0, 6));
        final EditText fUrl = Ui.input(c, "Webhook URL (https://…/webhook)", false, false);
        fUrl.setText(prefs.getUrl());
        final EditText fTok = Ui.input(c, "Device Token", false, false);
        fTok.setText(prefs.getToken());
        final EditText fSec = Ui.input(c, "Device Secret", true, false);
        fSec.setText(prefs.getSecret());
        conn.addView(fUrl, Ui.matchWrap(c, 0, 0, 0, 6));
        conn.addView(fTok, Ui.matchWrap(c, 0, 0, 0, 6));
        conn.addView(fSec, Ui.matchWrap(c, 0, 0, 0, 8));

        TextView save = Ui.button(c, "Simpan manual & tes", 1);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String u = fUrl.getText().toString().trim();
                if (!u.startsWith("https://") && !u.startsWith("http://")) {
                    msg.setTextColor(Ui.RED);
                    msg.setText("URL harus diawali https://");
                    return;
                }
                prefs.setConnection(u, fTok.getText().toString(), fSec.getText().toString());
                msg.setTextColor(Ui.AMBER);
                msg.setText("Menguji koneksi…");
                Dispatcher.testConnection(c, new Dispatcher.Callback() {
                    public void onResult(boolean ok, String message) {
                        msg.setTextColor(ok ? Ui.GREEN : Ui.RED);
                        msg.setText((ok ? "✅ " : "⚠️ ") + message);
                        updateHeaderChip();
                    }
                });
            }
        });
        conn.addView(save, Ui.matchWrap(c, 0, 0, 0, 8));
        conn.addView(msg, Ui.matchWrap(c, 2, 0, 2, 0));

        conn.addView(Ui.text(c, "Nama perangkat (tampil di dashboard web)", 12, Ui.SKY, true), Ui.matchWrap(c, 0, 12, 0, 6));
        final EditText fName = Ui.input(c, "Nama perangkat", false, false);
        fName.setText(prefs.getDeviceName());
        conn.addView(fName, Ui.matchWrap(c, 0, 0, 0, 8));
        TextView saveName = Ui.button(c, "Simpan nama", 2);
        saveName.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prefs.setDeviceName(fName.getText().toString());
                Ui.toast(c, "Nama perangkat disimpan.");
            }
        });
        conn.addView(saveName);
        col.addView(conn, Ui.matchWrap(c, 0, 0, 0, 14));

        // ---- Sumber ----
        LinearLayout src = Ui.card(c);
        src.addView(Ui.text(c, "Aplikasi yang dipantau", 14, Color.WHITE, true), Ui.matchWrap(c, 0, 0, 0, 6));
        for (int i = 0; i < Sources.GROUPS.length; i++) {
            final String label = Sources.GROUPS[i][0];
            Switch sw = new Switch(c);
            sw.setText(label);
            sw.setTextColor(Ui.TEXT);
            sw.setTextSize(13);
            sw.setChecked(prefs.isGroupEnabled(label));
            sw.setPadding(0, Ui.dp(c, 7), 0, Ui.dp(c, 7));
            sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean on) {
                    prefs.setGroupEnabled(label, on);
                }
            });
            src.addView(sw);
        }

        Switch broad = new Switch(c);
        broad.setText("Mode menyeluruh (aplikasi lain yang tampak dana masuk)");
        broad.setTextColor(Ui.TEXT);
        broad.setTextSize(13);
        broad.setChecked(prefs.isBroadMode());
        broad.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 7));
        broad.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) {
                prefs.setBroadMode(on);
            }
        });
        src.addView(broad);

        Switch out = new Switch(c);
        out.setText("Kirim juga notifikasi dana KELUAR");
        out.setTextColor(Ui.TEXT);
        out.setTextSize(13);
        out.setChecked(prefs.isSendOut());
        out.setPadding(0, Ui.dp(c, 7), 0, Ui.dp(c, 7));
        out.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) {
                prefs.setSendOut(on);
            }
        });
        src.addView(out);
        col.addView(src, Ui.matchWrap(c, 0, 0, 0, 14));

        // ---- Paket kustom ----
        LinearLayout cus = Ui.card(c);
        cus.addView(Ui.text(c, "Aplikasi kustom", 14, Color.WHITE, true), Ui.matchWrap(c, 0, 0, 0, 4));
        cus.addView(Ui.text(c, "Tambahkan aplikasi bank/e-wallet yang belum ada di daftar. Dikirim ke server sebagai sumber \"bank\".", 12, Ui.MUTED, false), Ui.matchWrap(c, 0, 0, 0, 8));

        String[] custom = prefs.getCustomPackages().split(",");
        for (int i = 0; i < custom.length; i++) {
            final String pkg = custom[i].trim();
            if (pkg.length() == 0) continue;
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(Ui.text(c, Sources.appLabel(c, pkg) + "\n" + pkg, 11, Ui.TEXT, false), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView del = Ui.chip(c, "HAPUS", Ui.RED);
            del.setClickable(true);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    prefs.removeCustomPackage(pkg);
                    showTab(2, false);
                }
            });
            row.addView(del);
            cus.addView(row, Ui.matchWrap(c, 0, 0, 0, 8));
        }

        final EditText fPkg = Ui.input(c, "Nama paket, mis. com.contoh.bank", false, false);
        cus.addView(fPkg, Ui.matchWrap(c, 0, 4, 0, 8));
        LinearLayout r2 = new LinearLayout(c);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        TextView add = Ui.button(c, "Tambah", 2);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prefs.addCustomPackage(fPkg.getText().toString());
                showTab(2, false);
            }
        });
        r2.addView(add, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView pick = Ui.button(c, "Pilih dari aplikasi terpasang", 0);
        pick.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickInstalledApp();
            }
        });
        LinearLayout.LayoutParams lpp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.6f);
        lpp.leftMargin = Ui.dp(c, 8);
        r2.addView(pick, lpp);
        cus.addView(r2);
        col.addView(cus, Ui.matchWrap(c, 0, 0, 0, 14));

        // ---- Lainnya ----
        TextView reset = Ui.button(c, "Reset statistik", 2);
        reset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prefs.resetStats();
                Ui.toast(c, "Statistik direset.");
            }
        });
        col.addView(reset, Ui.matchWrap(c, 0, 0, 0, 8));

        TextView clearQ = Ui.button(c, "Kosongkan antrean pengiriman", 3);
        clearQ.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Store.get(c).clearQueue();
                Ui.toast(c, "Antrean dikosongkan.");
            }
        });
        col.addView(clearQ, Ui.matchWrap(c, 0, 0, 0, 0));
    }

    private void pickInstalledApp() {
        final Context c = this;
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);

        final List<String[]> items = new ArrayList<String[]>();
        for (int i = 0; i < apps.size(); i++) {
            ApplicationInfo ai = apps.get(i);
            if (ai.packageName.equals(getPackageName())) continue;
            if (pm.getLaunchIntentForPackage(ai.packageName) == null) continue;
            String label = String.valueOf(pm.getApplicationLabel(ai));
            items.add(new String[]{label, ai.packageName});
        }
        Collections.sort(items, new Comparator<String[]>() {
            public int compare(String[] a, String[] b) {
                return a[0].compareToIgnoreCase(b[0]);
            }
        });

        CharSequence[] names = new CharSequence[items.size()];
        for (int i = 0; i < items.size(); i++) {
            names[i] = items.get(i)[0] + "\n" + items.get(i)[1];
        }

        AlertDialog.Builder b = new AlertDialog.Builder(c);
        b.setTitle("Pilih aplikasi");
        b.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int which) {
                prefs.addCustomPackage(items.get(which)[1]);
                Ui.toast(c, "Ditambahkan: " + items.get(which)[0]);
                showTab(2, false);
            }
        });
        b.setNegativeButton("Batal", null);
        b.show();
    }

    /* ======================= BANTUAN ======================= */

    private void faq(LinearLayout col, String q, String a) {
        Context c = this;
        LinearLayout card = Ui.card(c);
        card.addView(Ui.text(c, q, 13, Color.WHITE, true));
        card.addView(Ui.text(c, a, 12, Ui.MUTED, false), Ui.matchWrap(c, 0, 6, 0, 0));
        col.addView(card, Ui.matchWrap(c, 0, 0, 0, 10));
    }

    private void buildHelp(LinearLayout col) {
        final Context c = this;
        col.addView(Ui.text(c, "Bantuan", 18, Color.WHITE, true), Ui.matchWrap(c, 0, 4, 0, 12));

        TextView tut = Ui.button(c, "🎬  Ulangi tutorial langkah demi langkah", 0);
        tut.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(c, OnboardingActivity.class));
            }
        });
        col.addView(tut, Ui.matchWrap(c, 0, 0, 0, 10));

        TextView perm = Ui.button(c, "Buka pengaturan Akses Notifikasi", 2);
        perm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Perm.openListenerSettings(c);
            }
        });
        col.addView(perm, Ui.matchWrap(c, 0, 0, 0, 14));

        faq(col, "Notifikasi pembayaran tidak terbaca",
            "1) Pastikan Akses Notifikasi aktif. 2) Pastikan notifikasi aplikasi GoBiz/bank tidak dimatikan dan isinya tidak disembunyikan. 3) Cek tab Log: bila ada status ABAIKAN berarti notifikasi terbaca tapi tidak berisi nominal Rp atau berupa dana keluar. 4) Tambahkan paket aplikasi di Atur bila bank kamu belum ada di daftar.");
        faq(col, "Di web status APK OFFLINE",
            "APK mengirim detak jantung tiap 60 detik selama layanan hidup. Pastikan internet aktif, baterai tidak dibatasi, Autostart aktif, dan aplikasi dikunci di Recent Apps. Lalu matikan dan nyalakan lagi Akses Notifikasi.");
        faq(col, "Galat CLOCK_SKEW / BAD_SIGNATURE",
            "CLOCK_SKEW: aktifkan tanggal & waktu otomatis di HP. BAD_SIGNATURE/BAD_TOKEN: tempel ulang Kode Koneksi terbaru dari web (menu Notifier → Koneksi APK).");
        faq(col, "Pembayaran sudah masuk tapi tagihan masih PENDING",
            "Nominal harus PERSIS sama dengan nominal unik pada tagihan (mis. Rp 10.009). Jika notifikasi terbaca tetapi tidak cocok, buka web → Notifier → Notifikasi lalu klik Pindai ulang atau Kaitkan manual.");
        faq(col, "Apa saja yang dikirim ke server?",
            "Hanya notifikasi dari aplikasi yang dipantau (atau yang jelas berisi dana masuk pada Mode menyeluruh): nama aplikasi, judul, isi, nominal, dan waktu. Dikirim ke URL webhook milikmu dengan tanda tangan HMAC. Tidak ada data lain yang dibaca.");
        faq(col, "Bagaimana jika internet mati?",
            "Notifikasi masuk ke antrean lokal dan dicoba kirim ulang otomatis (maksimal 12 kali dengan jeda bertahap). Tekan Kirim ulang antrean di Beranda untuk memaksa.");
        faq(col, "HP Xiaomi / Oppo / Vivo / Realme / Samsung",
            "Aktifkan Autostart, set baterai ke Tanpa batasan, kunci aplikasi di Recent Apps, dan matikan fitur pembersih memori otomatis untuk aplikasi ini.");

        TextView ver = Ui.text(c, "BayarKuy Notifier v" + Dispatcher.appVersion(c) + "  •  ID perangkat: " + prefs.getDeviceId(), 10, Ui.MUTED, false);
        ver.setGravity(Gravity.CENTER);
        col.addView(ver, Ui.matchWrap(c, 0, 10, 0, 0));
    }
}
