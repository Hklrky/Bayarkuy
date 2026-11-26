package com.bayarkuy.notifier;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Tutorial interaktif berbentuk scene. Scene penting (izin, baterai, koneksi) WAJIB diselesaikan
 * sebelum tombol "Lanjut" aktif.
 */
public class OnboardingActivity extends Activity {

    private static final int TOTAL = 8;

    private Prefs prefs;
    private Handler handler = new Handler();
    private int step = 0;

    private SceneView scene;
    private TextView tvTitle;
    private TextView tvDesc;
    private TextView tvStep;
    private LinearLayout content;
    private LinearLayout dots;
    private TextView btnNext;
    private TextView btnBack;
    private ScrollView scroll;

    private TextView connResult;

    private final Runnable ticker = new Runnable() {
        public void run() {
            updateNext();
            if (step == 2 || step == 3) {
                render(false);
            }
            handler.postDelayed(this, 1500L);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        buildFrame();
        render(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker, 1500L);
        if (step == 2 || step == 3) render(false);
        updateNext();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    @Override
    public void onBackPressed() {
        if (step > 0) {
            step--;
            render(true);
        } else {
            super.onBackPressed();
        }
    }

    /* ======================= kerangka ======================= */

    private void buildFrame() {
        final Context c = this;
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        // Bar atas: titik progres + lewati
        LinearLayout top = new LinearLayout(c);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(c, 20), Ui.dp(c, 16), Ui.dp(c, 20), Ui.dp(c, 4));

        dots = new LinearLayout(c);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        top.addView(dots, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        tvStep = Ui.text(c, "1/" + TOTAL, 12, Ui.MUTED, true);
        top.addView(tvStep);

        TextView skip = Ui.text(c, "Lewati", 12, Ui.SKY, true);
        skip.setPadding(Ui.dp(c, 16), Ui.dp(c, 6), 0, Ui.dp(c, 6));
        skip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                confirmSkip();
            }
        });
        top.addView(skip);
        root.addView(top, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Area gulir
        scroll = new ScrollView(c);
        scroll.setFillViewport(false);
        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(c, 20), Ui.dp(c, 4), Ui.dp(c, 20), Ui.dp(c, 24));

        scene = new SceneView(c);
        body.addView(scene, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(c, 200)));

        tvTitle = Ui.text(c, "", 24, Color.WHITE, true);
        body.addView(tvTitle, Ui.matchWrap(c, 0, 8, 0, 6));

        tvDesc = Ui.text(c, "", 14, Ui.MUTED, false);
        body.addView(tvDesc, Ui.matchWrap(c, 0, 0, 0, 14));

        content = new LinearLayout(c);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, Ui.matchWrap(c, 0, 0, 0, 0));

        scroll.addView(body, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // Bar bawah
        LinearLayout bottom = new LinearLayout(c);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setPadding(Ui.dp(c, 20), Ui.dp(c, 10), Ui.dp(c, 20), Ui.dp(c, 18));

        btnBack = Ui.button(c, "Kembali", 2);
        btnBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (step > 0) {
                    step--;
                    render(true);
                }
            }
        });
        bottom.addView(btnBack, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        btnNext = Ui.button(c, "Lanjut", 0);
        btnNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!gateOk()) return;
                if (step < TOTAL - 1) {
                    step++;
                    render(true);
                } else {
                    finishTutorial();
                }
            }
        });
        LinearLayout.LayoutParams lpn = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f);
        lpn.leftMargin = Ui.dp(c, 10);
        bottom.addView(btnNext, lpn);

        root.addView(bottom, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void confirmSkip() {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle("Lewati tutorial?");
        b.setMessage("Aplikasi baru berfungsi jika Akses Notifikasi aktif dan koneksi ke web sudah diisi. Tutorial bisa diulang dari menu Bantuan.");
        b.setPositiveButton("Lewati", new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface d, int w) {
                finishTutorial();
            }
        });
        b.setNegativeButton("Lanjutkan", null);
        b.show();
    }

    private void finishTutorial() {
        prefs.setOnboarded(true);
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    /* ======================= gerbang ======================= */

    private boolean gateOk() {
        switch (step) {
            case 2: return Perm.isListenerEnabled(this);
            case 3: return Perm.isIgnoringBattery(this) && prefs.isAutostartAck();
            case 5: return prefs.isConfigured() && prefs.isConnOk();
            default: return true;
        }
    }

    private String gateHint() {
        switch (step) {
            case 2: return "Aktifkan Akses Notifikasi dulu";
            case 3: return "Selesaikan baterai & autostart";
            case 5: return "Tes koneksi harus berhasil";
            default: return "";
        }
    }

    private void updateNext() {
        boolean ok = gateOk();
        Ui.setButtonEnabled(btnNext, ok);
        if (step == TOTAL - 1) btnNext.setText("Mulai Pakai");
        else btnNext.setText(ok ? "Lanjut" : gateHint());
        btnBack.setVisibility(step == 0 ? View.INVISIBLE : View.VISIBLE);
    }

    /* ======================= render scene ======================= */

    private void render(boolean animate) {
        final Context c = this;
        tvStep.setText((step + 1) + "/" + TOTAL);
        scene.setType(step);

        dots.removeAllViews();
        for (int i = 0; i < TOTAL; i++) {
            View d = new View(c);
            int w = i == step ? 22 : 7;
            d.setBackground(Ui.round(c, i <= step ? Ui.SKY : Ui.STROKE, 4));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(Ui.dp(c, w), Ui.dp(c, 7));
            p.rightMargin = Ui.dp(c, 5);
            dots.addView(d, p);
        }

        content.removeAllViews();

        switch (step) {
            case 0: sceneWelcome(); break;
            case 1: sceneFlow(); break;
            case 2: scenePermission(); break;
            case 3: sceneBattery(); break;
            case 4: sceneApps(); break;
            case 5: sceneConnect(); break;
            case 6: sceneParser(); break;
            default: sceneDone(); break;
        }

        updateNext();

        if (animate) {
            scroll.scrollTo(0, 0);
            content.setAlpha(0f);
            content.setTranslationY(Ui.dp(c, 18));
            content.animate().alpha(1f).translationY(0).setDuration(320).start();
            tvTitle.setAlpha(0f);
            tvTitle.animate().alpha(1f).setDuration(320).start();
        }
    }

    private void info(String emoji, String head, String body) {
        Context c = this;
        LinearLayout card = Ui.card(c);
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView e = Ui.text(c, emoji, 22, Color.WHITE, false);
        row.addView(e, new LinearLayout.LayoutParams(Ui.dp(c, 40), LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(Ui.text(c, head, 14, Color.WHITE, true));
        col.addView(Ui.text(c, body, 12, Ui.MUTED, false));
        row.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        content.addView(card, Ui.matchWrap(c, 0, 0, 0, 10));
    }

    private void statusLine(boolean ok, String okText, String badText) {
        Context c = this;
        LinearLayout card = Ui.card(c);
        card.setBackground(Ui.roundStroke(c, ok ? Color.parseColor("#0F2A24") : Color.parseColor("#2A1620"), 16, ok ? Ui.GREEN : Ui.RED));
        TextView t = Ui.text(c, (ok ? "✅  " : "⛔  ") + (ok ? okText : badText), 14, ok ? Ui.GREEN : Ui.RED, true);
        card.addView(t);
        content.addView(card, Ui.matchWrap(c, 0, 0, 0, 12));
    }

    /* ---- Scene 0 ---- */
    private void sceneWelcome() {
        tvTitle.setText("Selamat datang 👋");
        tvDesc.setText("BayarKuy Notifier membaca notifikasi pembayaran masuk di HP ini (GoBiz, bank, e-wallet) lalu meneruskannya ke website BayarKuy kamu secara otomatis dan aman.");
        info("⚡", "Konfirmasi otomatis", "Tagihan berubah PAID begitu notifikasi dana masuk terbaca.");
        info("🔐", "Bertanda tangan", "Setiap kiriman memakai tanda tangan HMAC-SHA256, tidak bisa dipalsukan.");
        info("📦", "Anti hilang", "Jika internet mati, notifikasi disimpan lalu dikirim ulang otomatis.");
        info("🧭", "Tutorial singkat", "Ada 8 scene. Beberapa scene harus diselesaikan sebelum lanjut.");
    }

    /* ---- Scene 1 ---- */
    private void sceneFlow() {
        tvTitle.setText("Begini cara kerjanya");
        tvDesc.setText("Alurnya sederhana, dan kamu tidak perlu membuka aplikasi ini setiap saat.");
        info("1️⃣", "Pembeli membayar", "Lewat QRIS atau transfer dengan nominal unik, misalnya Rp 10.009.");
        info("2️⃣", "HP menerima notifikasi", "Dari aplikasi GoBiz, m-banking, atau e-wallet.");
        info("3️⃣", "APK membaca & memfilter", "Hanya notifikasi dana masuk berisi nominal Rp yang diteruskan.");
        info("4️⃣", "Webhook dikirim ke web", "Web mencocokkan nominal unik, mengubah status jadi PAID, lalu memanggil callback tokomu.");
    }

    /* ---- Scene 2 ---- */
    private void scenePermission() {
        final Context c = this;
        tvTitle.setText("Izin Akses Notifikasi");
        tvDesc.setText("Ini izin utama. Tanpa izin ini APK tidak bisa membaca notifikasi pembayaran. Data hanya dikirim ke URL webhook milikmu sendiri.");
        boolean ok = Perm.isListenerEnabled(c);
        statusLine(ok, "Akses Notifikasi AKTIF", "Akses Notifikasi belum aktif");

        TextView open = Ui.button(c, ok ? "Buka pengaturan (sudah aktif)" : "Buka Akses Notifikasi", ok ? 2 : 0);
        open.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Perm.openListenerSettings(c);
            }
        });
        content.addView(open, Ui.matchWrap(c, 0, 0, 0, 10));

        info("1", "Cari \"BayarKuy Notifier\"", "Di daftar, geser tombolnya ke posisi aktif lalu setujui peringatan.");
        info("🔓", "Android 13+ (tombol abu-abu)", "Buka Info Aplikasi → titik tiga (⋮) → Izinkan setelan terbatas, lalu ulangi.");

        TextView det = Ui.button(c, "Buka Info Aplikasi", 2);
        det.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Perm.openAppDetails(c);
            }
        });
        content.addView(det, Ui.matchWrap(c, 0, 0, 0, 0));
    }

    /* ---- Scene 3 ---- */
    private void sceneBattery() {
        final Context c = this;
        tvTitle.setText("Agar tidak dimatikan sistem");
        tvDesc.setText("Banyak HP (Xiaomi, Oppo, Vivo, Realme, Samsung) mematikan aplikasi latar belakang. Dua pengaturan ini mencegahnya.");

        boolean bat = Perm.isIgnoringBattery(c);
        statusLine(bat, "Penghematan baterai: tidak dibatasi", "Penghematan baterai masih membatasi aplikasi");

        TextView b1 = Ui.button(c, "Izinkan tanpa batasan baterai", bat ? 2 : 0);
        b1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Perm.requestIgnoreBattery(c);
            }
        });
        content.addView(b1, Ui.matchWrap(c, 0, 0, 0, 14));

        boolean ack = prefs.isAutostartAck();
        statusLine(ack, "Autostart sudah dikonfirmasi", "Autostart belum dikonfirmasi");

        TextView b2 = Ui.button(c, "Buka pengaturan Autostart", 2);
        b2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean found = Perm.openAutostart(c);
                if (!found) Ui.toast(c, "Menu Autostart tidak ditemukan di HP ini. Cari manual: Pengaturan → Aplikasi → BayarKuy Notifier → Autostart / Mulai otomatis.");
            }
        });
        content.addView(b2, Ui.matchWrap(c, 0, 0, 0, 10));

        TextView b3 = Ui.button(c, ack ? "Batalkan konfirmasi" : "Saya sudah aktifkan Autostart / HP saya tidak punya menu ini", ack ? 2 : 1);
        b3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prefs.setAutostartAck(!prefs.isAutostartAck());
                render(false);
            }
        });
        content.addView(b3, Ui.matchWrap(c, 0, 0, 0, 10));

        info("📌", "Kunci di Recent Apps", "Di layar aplikasi terbaru, tahan kartu BayarKuy Notifier lalu pilih Kunci (Lock) bila tersedia.");
    }

    /* ---- Scene 4 ---- */
    private void sceneApps() {
        final Context c = this;
        tvTitle.setText("Pilih aplikasi yang dipantau");
        tvDesc.setText("Aktifkan hanya aplikasi yang kamu pakai menerima pembayaran. Nama paket bisa berbeda antar versi; aplikasi lain bisa ditambah di menu Atur.");

        LinearLayout card = Ui.card(c);
        for (int i = 0; i < Sources.GROUPS.length; i++) {
            final String label = Sources.GROUPS[i][0];
            Switch sw = new Switch(c);
            sw.setText(label + "  ·  " + Sources.GROUPS[i][1]);
            sw.setTextColor(Ui.TEXT);
            sw.setTextSize(13);
            sw.setChecked(prefs.isGroupEnabled(label));
            sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean on) {
                    prefs.setGroupEnabled(label, on);
                }
            });
            sw.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8));
            card.addView(sw);
        }
        content.addView(card, Ui.matchWrap(c, 0, 0, 0, 12));

        LinearLayout card2 = Ui.card(c);
        Switch broad = new Switch(c);
        broad.setText("Mode menyeluruh");
        broad.setTextColor(Color.WHITE);
        broad.setTextSize(14);
        broad.setChecked(prefs.isBroadMode());
        broad.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) {
                prefs.setBroadMode(on);
            }
        });
        card2.addView(broad);
        card2.addView(Ui.text(c, "Ikut menangkap aplikasi LAIN yang notifikasinya jelas berisi dana masuk (kata \"diterima/masuk\" + nominal Rp). Berguna bila nama paket aplikasi bank berbeda.", 12, Ui.MUTED, false));
        content.addView(card2, Ui.matchWrap(c, 0, 0, 0, 0));
    }

    /* ---- Scene 5 ---- */
    private void sceneConnect() {
        final Context c = this;
        tvTitle.setText("Hubungkan ke website");
        tvDesc.setText("Buka web BayarKuy → menu Notifier → salin Kode Koneksi, lalu tempel di sini. URL webhook, token, dan secret terisi otomatis.");

        final EditText code = Ui.input(c, "Tempel Kode Koneksi (BKY1:...)", false, true);
        content.addView(code, Ui.matchWrap(c, 0, 0, 0, 8));

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView paste = Ui.button(c, "Tempel", 2);
        paste.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String s = readClipboard();
                if (s.length() == 0) Ui.toast(c, "Clipboard kosong. Salin dulu kode dari web.");
                else code.setText(s);
            }
        });
        row.addView(paste, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView apply = Ui.button(c, "Terapkan & Tes", 0);
        apply.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String err = prefs.applyConnectCode(code.getText().toString());
                if (err != null) {
                    showConn(false, err);
                    return;
                }
                runTest();
            }
        });
        LinearLayout.LayoutParams lpa = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f);
        lpa.leftMargin = Ui.dp(c, 8);
        row.addView(apply, lpa);
        content.addView(row, Ui.matchWrap(c, 0, 0, 0, 12));

        connResult = Ui.text(c, "", 13, Ui.MUTED, true);
        content.addView(connResult, Ui.matchWrap(c, 0, 0, 0, 12));

        if (prefs.isConfigured()) {
            if (prefs.isConnOk()) showConn(true, "Koneksi sudah teruji ✔  (" + prefs.getUrl() + ")");
            else showConn(false, "Tersimpan: " + prefs.getUrl() + " — tekan Terapkan & Tes untuk menguji.");
        }

        info("💡", "Tips", "Jam HP harus otomatis (Pengaturan → Tanggal & waktu → Otomatis) agar tanda tangan diterima server.");
    }

    private void runTest() {
        showConn(false, "Menguji koneksi…");
        Dispatcher.testConnection(this, new Dispatcher.Callback() {
            public void onResult(boolean ok, String message) {
                showConn(ok, message);
                updateNext();
            }
        });
    }

    private void showConn(boolean ok, String msg) {
        if (connResult == null) return;
        connResult.setTextColor(ok ? Ui.GREEN : (msg.startsWith("Menguji") || msg.startsWith("Tersimpan") ? Ui.AMBER : Ui.RED));
        connResult.setText((ok ? "✅ " : "") + msg);
    }

    private String readClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData cd = cm.getPrimaryClip();
                if (cd != null && cd.getItemCount() > 0) {
                    CharSequence cs = cd.getItemAt(0).coerceToText(this);
                    return cs == null ? "" : cs.toString().trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /* ---- Scene 6 ---- */
    private void sceneParser() {
        final Context c = this;
        tvTitle.setText("Coba pembaca nominal");
        tvDesc.setText("Ketik atau tempel contoh isi notifikasi untuk melihat bagaimana APK membaca nominal dan arah dana. Tidak ada yang dikirim ke server.");

        final EditText in = Ui.input(c, "Isi notifikasi", false, true);
        in.setText("Pembayaran QRIS Rp10.009 berhasil diterima");
        content.addView(in, Ui.matchWrap(c, 0, 0, 0, 10));

        final LinearLayout res = Ui.card(c);
        final TextView r1 = Ui.text(c, "", 20, Ui.GREEN, true);
        final TextView r2 = Ui.text(c, "", 13, Ui.MUTED, false);
        res.addView(r1);
        res.addView(r2);
        content.addView(res, Ui.matchWrap(c, 0, 0, 0, 10));

        final Runnable calc = new Runnable() {
            public void run() {
                String s = in.getText().toString();
                long amt = Money.parseAmount(s);
                String dir = Money.detectDirection("", s);
                r1.setText(amt > 0 ? Money.rupiah(amt) : "Nominal tidak ditemukan");
                r1.setTextColor(amt > 0 ? Ui.GREEN : Ui.AMBER);
                String dTxt = dir.equals("in") ? "Dana MASUK → akan dikirim" : (dir.equals("out") ? "Dana KELUAR → diabaikan" : "Arah tidak jelas");
                r2.setText(dTxt);
            }
        };
        in.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            public void onTextChanged(CharSequence s, int a, int b, int d) {}
            public void afterTextChanged(Editable e) { calc.run(); }
        });
        calc.run();

        info("🎯", "Kenapa nominal unik?", "Server mencocokkan nominal ini dengan tagihan PENDING. Nominal Rp 10.009 hanya cocok dengan tagihan Rp 10.009.");
    }

    /* ---- Scene 7 ---- */
    private void sceneDone() {
        final Context c = this;
        tvTitle.setText("Siap digunakan 🎉");
        tvDesc.setText("Biarkan HP tetap menyala dan terhubung internet. Status bisa dilihat di Beranda, dan notifikasi yang terbaca muncul di tab Log.");
        statusLine(Perm.isListenerEnabled(c), "Akses Notifikasi aktif", "Akses Notifikasi belum aktif");
        statusLine(Perm.isIgnoringBattery(c), "Baterai tidak dibatasi", "Baterai masih dibatasi (disarankan dimatikan)");
        statusLine(prefs.isConfigured() && prefs.isConnOk(), "Terhubung ke website", "Belum terhubung ke website");
        info("🧪", "Langkah terakhir", "Di web, buka Notifier → Tutorial scene 6, buat tagihan tes, lalu bayar nominal uniknya.");
    }
}
