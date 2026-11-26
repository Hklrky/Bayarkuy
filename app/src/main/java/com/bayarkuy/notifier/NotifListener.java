package com.bayarkuy.notifier;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.json.JSONObject;

import java.util.LinkedHashSet;

/**
 * Inti aplikasi: membaca SEMUA notifikasi yang muncul di HP, memfilter yang berupa
 * pembayaran masuk (GoBiz / bank / e-wallet), lalu mengirimnya ke webhook.
 */
public class NotifListener extends NotificationListenerService {

    public static volatile NotifListener instance;

    private static final long HEARTBEAT_MS = 60000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LinkedHashSet<String> seen = new LinkedHashSet<String>();
    private boolean connected = false;

    private final Runnable heartbeatTask = new Runnable() {
        public void run() {
            if (connected) {
                Prefs p = new Prefs(NotifListener.this);
                p.setListenerAlive(System.currentTimeMillis());
                Dispatcher.heartbeat(NotifListener.this);
                handler.postDelayed(this, HEARTBEAT_MS);
            }
        }
    };

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
        connected = true;
        new Prefs(this).setListenerAlive(System.currentTimeMillis());
        handler.removeCallbacks(heartbeatTask);
        handler.post(heartbeatTask);

        // Pindai menyeluruh: notifikasi yang sudah ada di bilah notifikasi saat layanan aktif.
        handler.postDelayed(new Runnable() {
            public void run() {
                sweep();
            }
        }, 3000L);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        connected = false;
        handler.removeCallbacks(heartbeatTask);
        requestRebindSafe();
    }

    @Override
    public void onDestroy() {
        connected = false;
        handler.removeCallbacks(heartbeatTask);
        if (instance == this) instance = null;
        super.onDestroy();
    }

    private void requestRebindSafe() {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                NotificationListenerService.requestRebind(new ComponentName(this, NotifListener.class));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            handle(sbn, false);
        } catch (Exception e) {
            new Prefs(this).setLastError("Listener: " + e.getMessage());
        }
    }

    /** Memindai semua notifikasi aktif. Mengembalikan jumlah yang diproses. */
    public int sweep() {
        int n = 0;
        try {
            StatusBarNotification[] list = getActiveNotifications();
            if (list == null) return 0;
            for (int i = 0; i < list.length; i++) {
                try {
                    if (handle(list[i], true)) n++;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            new Prefs(this).setLastError("Pindai: " + e.getMessage());
        }
        return n;
    }

    /** @return true jika notifikasi diteruskan ke antrean. */
    private boolean handle(StatusBarNotification sbn, boolean fromSweep) throws Exception {
        if (sbn == null) return false;
        String pkg = sbn.getPackageName();
        if (pkg == null || pkg.equals(getPackageName())) return false;

        Notification n = sbn.getNotification();
        if (n == null) return false;
        Bundle ex = n.extras;
        if (ex == null) return false;

        String title = str(ex.getCharSequence(Notification.EXTRA_TITLE));
        String text = str(ex.getCharSequence(Notification.EXTRA_TEXT));
        String big = str(ex.getCharSequence(Notification.EXTRA_BIG_TEXT));
        String sub = str(ex.getCharSequence(Notification.EXTRA_SUB_TEXT));
        CharSequence[] lines = ex.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);

        StringBuilder body = new StringBuilder();
        if (big.length() > text.length()) body.append(big); else body.append(text);
        if (lines != null) {
            for (int i = 0; i < lines.length; i++) {
                String ln = str(lines[i]);
                if (ln.length() > 0 && body.indexOf(ln) < 0) {
                    if (body.length() > 0) body.append('\n');
                    body.append(ln);
                }
            }
        }
        if (body.length() == 0 && sub.length() > 0) body.append(sub);
        if (body.length() == 0 && n.tickerText != null) body.append(n.tickerText.toString());

        String fullText = body.toString().trim();
        if (title.length() == 0 && fullText.length() == 0) return false;

        Prefs prefs = new Prefs(this);
        String[] src = Sources.lookup(prefs, pkg);
        String source;
        String appName;

        if (src != null) {
            source = src[0];
            appName = src[1].equals("Kustom") ? Sources.appLabel(this, pkg) : src[1];
        } else if (prefs.isBroadMode()) {
            // Mode menyeluruh: aplikasi tak dikenal, hanya diteruskan jika tampak seperti dana masuk.
            long probe = Money.parseAmount(title + " " + fullText);
            String dir = Money.detectDirection(title, fullText);
            if (probe <= 0 || !dir.equals("in")) return false;
            source = "other";
            appName = Sources.appLabel(this, pkg);
        } else {
            return false;
        }

        long amount = Money.parseAmount(title + " " + fullText);
        String direction = Money.detectDirection(title, fullText);
        long postTime = sbn.getPostTime();

        // Deduplikasi lokal (notifikasi yang di-update sistem akan memanggil callback lagi).
        String key = pkg + "|" + postTime + "|" + title.hashCode() + "|" + fullText.hashCode();
        synchronized (seen) {
            if (seen.contains(key)) return false;
            seen.add(key);
            if (seen.size() > 300) {
                java.util.Iterator<String> it = seen.iterator();
                if (it.hasNext()) { it.next(); it.remove(); }
            }
        }

        Store store = Store.get(this);

        if (amount <= 0) {
            store.addLog(appName, title, fullText, 0, direction, "ABAIKAN", "Tidak ada nominal Rp");
            return false;
        }
        if (direction.equals("out") && !prefs.isSendOut()) {
            store.addLog(appName, title, fullText, amount, direction, "ABAIKAN", "Dana keluar tidak dikirim");
            return false;
        }

        long logId = store.addLog(appName, title, fullText, amount, direction, "ANTRE", fromSweep ? "Hasil pindai" : "");

        JSONObject o = Dispatcher.basePayload(this, "notification");
        o.put("package", pkg);
        o.put("app_name", appName);
        o.put("source", source);
        o.put("title", title);
        o.put("text", fullText);
        o.put("amount", amount);
        o.put("direction", direction);
        o.put("posted_at", postTime);
        o.put("sweep", fromSweep);

        Dispatcher.submit(this, o.toString(), logId);
        return true;
    }

    private static String str(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
