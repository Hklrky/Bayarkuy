package com.bayarkuy.notifier;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HTTP sederhana + tanda tangan HMAC-SHA256 untuk webhook BayarKuy.
 */
public class Net {

    public static class Res {
        public int code = 0;
        public String body = "";
        public String error = "";
    }

    public static String sign(String secret, String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        byte[] out = mac.doFinal((timestamp + "." + body).getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < out.length; i++) {
            String h = Integer.toHexString(out[i] & 0xff);
            if (h.length() == 1) sb.append('0');
            sb.append(h);
        }
        return sb.toString();
    }

    public static Res postSigned(Prefs p, String json) {
        Res r = new Res();
        HttpURLConnection c = null;
        try {
            String ts = String.valueOf(System.currentTimeMillis() / 1000L);
            String sig = sign(p.getSecret(), ts, json);

            c = (HttpURLConnection) new URL(p.getUrl()).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("User-Agent", "BayarKuyNotifier/1.0 Android");
            c.setRequestProperty("X-Notifier-Token", p.getToken());
            c.setRequestProperty("X-Timestamp", ts);
            c.setRequestProperty("X-Signature", sig);

            byte[] data = json.getBytes("UTF-8");
            c.setFixedLengthStreamingMode(data.length);
            OutputStream os = c.getOutputStream();
            os.write(data);
            os.flush();
            os.close();

            r.code = c.getResponseCode();
            InputStream is = r.code >= 400 ? c.getErrorStream() : c.getInputStream();
            r.body = readAll(is);
        } catch (Exception e) {
            r.error = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
        } finally {
            if (c != null) c.disconnect();
        }
        return r;
    }

    public static Res get(String url) {
        Res r = new Res();
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setRequestMethod("GET");
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("User-Agent", "BayarKuyNotifier/1.0 Android");
            r.code = c.getResponseCode();
            InputStream is = r.code >= 400 ? c.getErrorStream() : c.getInputStream();
            r.body = readAll(is);
        } catch (Exception e) {
            r.error = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
        } finally {
            if (c != null) c.disconnect();
        }
        return r;
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) {
            bo.write(buf, 0, n);
            if (bo.size() > 262144) break;
        }
        is.close();
        return bo.toString("UTF-8");
    }
}
