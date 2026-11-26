package com.bayarkuy.notifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Penyimpanan pengaturan aplikasi (SharedPreferences).
 */
public class Prefs {

    private final SharedPreferences sp;

    public Prefs(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences("bayarkuy_notifier", Context.MODE_PRIVATE);
    }

    /* ---------------- koneksi ---------------- */

    public String getUrl() { return sp.getString("url", ""); }
    public String getToken() { return sp.getString("token", ""); }
    public String getSecret() { return sp.getString("secret", ""); }

    public void setConnection(String url, String token, String secret) {
        sp.edit()
            .putString("url", url == null ? "" : url.trim())
            .putString("token", token == null ? "" : token.trim())
            .putString("secret", secret == null ? "" : secret.trim())
            .putBoolean("conn_ok", false)
            .apply();
    }

    public boolean isConfigured() {
        return getUrl().startsWith("http") && getToken().length() > 0 && getSecret().length() > 0;
    }

    public boolean isConnOk() { return sp.getBoolean("conn_ok", false); }
    public void setConnOk(boolean ok) { sp.edit().putBoolean("conn_ok", ok).apply(); }

    /**
     * Menerapkan Kode Koneksi dari dashboard web (format "BKY1:" + base64url(JSON)).
     * @return null jika berhasil, atau pesan galat.
     */
    public String applyConnectCode(String code) {
        try {
            if (code == null) return "Kode kosong.";
            code = code.trim();
            if (!code.startsWith("BKY1:")) return "Kode tidak valid. Kode harus diawali BKY1:";
            String b64 = code.substring(5).trim();
            byte[] raw = Base64.decode(b64, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            JSONObject o = new JSONObject(new String(raw, "UTF-8"));
            String u = o.optString("u", "");
            String t = o.optString("t", "");
            String s = o.optString("s", "");
            if (!u.startsWith("https://") && !u.startsWith("http://")) return "URL dalam kode tidak valid.";
            if (t.length() == 0 || s.length() == 0) return "Token atau Secret kosong di dalam kode.";
            setConnection(u, t, s);
            return null;
        } catch (Exception e) {
            return "Gagal membaca kode: " + e.getMessage();
        }
    }

    /* ---------------- perangkat ---------------- */

    public String getDeviceId() {
        String id = sp.getString("device_id", "");
        if (id.length() == 0) {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            sp.edit().putString("device_id", id).apply();
        }
        return id;
    }

    public String getDeviceName() {
        String n = sp.getString("device_name", "");
        if (n.length() == 0) {
            n = (Build.MANUFACTURER == null ? "" : Build.MANUFACTURER) + " " + (Build.MODEL == null ? "" : Build.MODEL);
        }
        return n.trim();
    }

    public void setDeviceName(String n) { sp.edit().putString("device_name", n == null ? "" : n.trim()).apply(); }

    /* ---------------- tutorial ---------------- */

    public boolean isOnboarded() { return sp.getBoolean("onboarded", false); }
    public void setOnboarded(boolean v) { sp.edit().putBoolean("onboarded", v).apply(); }

    public boolean isAutostartAck() { return sp.getBoolean("autostart_ack", false); }
    public void setAutostartAck(boolean v) { sp.edit().putBoolean("autostart_ack", v).apply(); }

    /* ---------------- sumber notifikasi ---------------- */

    /** Mode menyeluruh: tangkap juga aplikasi lain yang notifikasinya tampak seperti dana masuk. */
    public boolean isBroadMode() { return sp.getBoolean("broad_mode", true); }
    public void setBroadMode(boolean v) { sp.edit().putBoolean("broad_mode", v).apply(); }

    /** Kirim juga notifikasi dana keluar (default: tidak). */
    public boolean isSendOut() { return sp.getBoolean("send_out", false); }
    public void setSendOut(boolean v) { sp.edit().putBoolean("send_out", v).apply(); }

    /** Grup aplikasi yang dimatikan (label dipisah '|'). */
    public boolean isGroupEnabled(String label) {
        String off = sp.getString("groups_off", "");
        return !(("|" + off + "|").contains("|" + label + "|"));
    }

    public void setGroupEnabled(String label, boolean enabled) {
        String off = sp.getString("groups_off", "");
        StringBuilder sb = new StringBuilder();
        String[] parts = off.split("\\|");
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.length() == 0 || p.equals(label)) continue;
            if (sb.length() > 0) sb.append('|');
            sb.append(p);
        }
        if (!enabled) {
            if (sb.length() > 0) sb.append('|');
            sb.append(label);
        }
        sp.edit().putString("groups_off", sb.toString()).apply();
    }

    public String getCustomPackages() { return sp.getString("custom_pkgs", ""); }

    public void addCustomPackage(String pkg) {
        pkg = pkg == null ? "" : pkg.trim();
        if (pkg.length() == 0) return;
        String cur = getCustomPackages();
        if ((("," + cur + ",")).contains("," + pkg + ",")) return;
        sp.edit().putString("custom_pkgs", cur.length() == 0 ? pkg : cur + "," + pkg).apply();
    }

    public void removeCustomPackage(String pkg) {
        String[] parts = getCustomPackages().split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.length() == 0 || p.equals(pkg)) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(p);
        }
        sp.edit().putString("custom_pkgs", sb.toString()).apply();
    }

    /* ---------------- statistik ---------------- */

    public long getSent() { return sp.getLong("stat_sent", 0); }
    public long getFailed() { return sp.getLong("stat_failed", 0); }
    public long getMatched() { return sp.getLong("stat_matched", 0); }

    public synchronized void incSent() { sp.edit().putLong("stat_sent", getSent() + 1).apply(); }
    public synchronized void incFailed() { sp.edit().putLong("stat_failed", getFailed() + 1).apply(); }
    public synchronized void incMatched() { sp.edit().putLong("stat_matched", getMatched() + 1).apply(); }

    public void resetStats() {
        sp.edit().putLong("stat_sent", 0).putLong("stat_failed", 0).putLong("stat_matched", 0).apply();
    }

    public long getLastHeartbeat() { return sp.getLong("last_hb", 0); }
    public void setLastHeartbeat(long t) { sp.edit().putLong("last_hb", t).apply(); }

    public long getListenerAlive() { return sp.getLong("listener_alive", 0); }
    public void setListenerAlive(long t) { sp.edit().putLong("listener_alive", t).apply(); }

    public String getLastError() { return sp.getString("last_error", ""); }
    public void setLastError(String e) { sp.edit().putString("last_error", e == null ? "" : e).apply(); }
}
