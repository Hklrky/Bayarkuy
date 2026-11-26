package com.bayarkuy.notifier;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pengirim webhook: antrean persisten + retry dengan backoff.
 */
public class Dispatcher {

    private static final ExecutorService EX = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int MAX_TRIES = 12;
    private static boolean retryScheduled = false;

    public interface Callback {
        void onResult(boolean ok, String message);
    }

    /* ---------------- payload ---------------- */

    public static JSONObject basePayload(Context ctx, String type) throws Exception {
        Prefs p = new Prefs(ctx);
        JSONObject o = new JSONObject();
        o.put("type", type);
        o.put("device_id", p.getDeviceId());
        o.put("device_name", p.getDeviceName());
        o.put("app_version", appVersion(ctx));
        int b = batteryLevel(ctx);
        if (b >= 0) o.put("battery", b);
        return o;
    }

    public static String appVersion(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    public static int batteryLevel(Context ctx) {
        try {
            Intent i = ctx.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (i == null) return -1;
            int level = i.getIntExtra("level", -1);
            int scale = i.getIntExtra("scale", -1);
            if (level < 0 || scale <= 0) return -1;
            return (int) (level * 100L / scale);
        } catch (Exception e) {
            return -1;
        }
    }

    /* ---------------- antrean ---------------- */

    public static void submit(Context ctx, String json, long logId) {
        Store.get(ctx).enqueue(json, logId);
        kick(ctx);
    }

    public static void kick(Context ctx) {
        final Context app = ctx.getApplicationContext();
        EX.execute(new Runnable() {
            public void run() {
                processQueue(app);
            }
        });
    }

    private static void processQueue(final Context ctx) {
        Prefs prefs = new Prefs(ctx);
        Store st = Store.get(ctx);
        if (!prefs.isConfigured()) {
            return;
        }

        Store.Item it;
        int guard = 0;
        while ((it = st.nextDue()) != null && guard < 200) {
            guard++;
            Net.Res r = Net.postSigned(prefs, it.payload);

            if (r.code == 200) {
                String status = "TERKIRIM";
                String detail = "";
                try {
                    JSONObject o = new JSONObject(r.body);
                    JSONArray arr = o.optJSONArray("results");
                    if (arr != null && arr.length() > 0) {
                        JSONObject first = arr.getJSONObject(0);
                        status = first.optString("result", "TERKIRIM");
                        String ref = first.optString("matched_reference_id", "");
                        if (ref != null && !ref.equals("null") && ref.length() > 0) {
                            detail = ref;
                        }
                    }
                } catch (Exception ignored) {
                }
                st.updateLog(it.logId, status, detail);
                st.deleteItem(it.id);
                prefs.incSent();
                if (status.equals("MATCHED")) prefs.incMatched();
                prefs.setLastError("");
                continue;
            }

            boolean fatal = (r.code == 400 || r.code == 401 || r.code == 402 || r.code == 403 || r.code == 413);
            int tries = it.tries + 1;

            if (fatal || tries >= MAX_TRIES) {
                String msg = describeError(r);
                st.updateLog(it.logId, "GAGAL", msg);
                st.deleteItem(it.id);
                prefs.incFailed();
                prefs.setLastError(msg);
                continue;
            }

            st.retryLater(it.id, tries, backoff(tries));
            st.updateLog(it.logId, "ANTRE", "Coba lagi (" + tries + "/" + MAX_TRIES + "): " + describeError(r));
        }

        if (st.queueSize() > 0) {
            scheduleRetry(ctx);
        }
    }

    private static long backoff(int tries) {
        long[] steps = {10000L, 30000L, 60000L, 120000L, 300000L, 600000L, 900000L};
        int idx = Math.min(tries - 1, steps.length - 1);
        return steps[Math.max(0, idx)];
    }

    private static synchronized void scheduleRetry(final Context ctx) {
        if (retryScheduled) return;
        retryScheduled = true;
        MAIN.postDelayed(new Runnable() {
            public void run() {
                synchronized (Dispatcher.class) {
                    retryScheduled = false;
                }
                kick(ctx);
            }
        }, 15000L);
    }

    public static String describeError(Net.Res r) {
        if (r.error != null && r.error.length() > 0) {
            return "Jaringan: " + r.error;
        }
        try {
            JSONObject o = new JSONObject(r.body);
            String code = o.optString("code", "");
            String msg = o.optString("message", "");
            return "HTTP " + r.code + (code.length() > 0 ? " " + code : "") + (msg.length() > 0 ? " - " + msg : "");
        } catch (Exception e) {
            return "HTTP " + r.code;
        }
    }

    /* ---------------- heartbeat & tes ---------------- */

    public static void heartbeat(final Context ctx) {
        final Context app = ctx.getApplicationContext();
        EX.execute(new Runnable() {
            public void run() {
                Prefs p = new Prefs(app);
                if (!p.isConfigured()) return;
                try {
                    JSONObject o = basePayload(app, "heartbeat");
                    Net.Res r = Net.postSigned(p, o.toString());
                    if (r.code == 200) {
                        p.setLastHeartbeat(System.currentTimeMillis());
                        p.setConnOk(true);
                    } else {
                        p.setLastError(describeError(r));
                    }
                } catch (Exception ignored) {
                }
                processQueue(app);
            }
        });
    }

    /** Tes koneksi lengkap: GET (waktu server) lalu POST type=test bertanda tangan. */
    public static void testConnection(final Context ctx, final Callback cb) {
        final Context app = ctx.getApplicationContext();
        EX.execute(new Runnable() {
            public void run() {
                final Prefs p = new Prefs(app);
                String msg;
                boolean ok = false;

                if (!p.isConfigured()) {
                    msg = "Koneksi belum diisi. Tempel Kode Koneksi dari dashboard web.";
                } else {
                    Net.Res g = Net.get(p.getUrl());
                    if (g.error.length() > 0) {
                        msg = "Tidak bisa menjangkau server: " + g.error;
                    } else {
                        try {
                            JSONObject o = basePayload(app, "test");
                            Net.Res r = Net.postSigned(p, o.toString());
                            if (r.code == 200) {
                                String who = "";
                                try { who = new JSONObject(r.body).optString("user", ""); } catch (Exception ignored) {}
                                ok = true;
                                msg = "Terhubung" + (who.length() > 0 ? " sebagai " + who : "") + ". Webhook merespons normal.";
                                p.setLastHeartbeat(System.currentTimeMillis());
                            } else {
                                msg = describeError(r);
                            }
                        } catch (Exception e) {
                            msg = "Galat: " + e.getMessage();
                        }
                    }
                }

                p.setConnOk(ok);
                final boolean fOk = ok;
                final String fMsg = msg;
                MAIN.post(new Runnable() {
                    public void run() {
                        if (cb != null) cb.onResult(fOk, fMsg);
                    }
                });
            }
        });
    }
}
