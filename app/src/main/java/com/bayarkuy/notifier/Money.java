package com.bayarkuy.notifier;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pembaca nominal rupiah & penebak arah dana dari teks notifikasi.
 * Logikanya sama dengan NotifierCore.php di server.
 */
public class Money {

    private static final Pattern AMOUNT = Pattern.compile(
        "(?:Rp\\.?|IDR)\\s*([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE);

    private static final String[] IN_WORDS = {
        "menerima", "diterima", "dana masuk", "uang masuk", "transfer masuk", "kredit",
        "received", "credit", "incoming", "pembayaran qris", "pembayaran berhasil diterima",
        "top up berhasil", "saldo bertambah", "telah masuk", "masuk ke rekening"
    };

    private static final String[] OUT_WORDS = {
        "mengirim", "dikirim", "transfer ke", "pembayaran ke", "penarikan", "tarik tunai",
        "debit", "pembelian", "berhasil membayar", "anda membayar", "saldo berkurang",
        "sent to", "payment to"
    };

    /** Mengembalikan nominal pertama yang ditemukan, atau 0. */
    public static long parseAmount(String text) {
        if (text == null) return 0;
        Matcher m = AMOUNT.matcher(text);
        if (!m.find()) return 0;
        return normalize(m.group(1));
    }

    private static long normalize(String raw) {
        raw = raw.trim();
        boolean hasDot = raw.indexOf('.') >= 0;
        boolean hasComma = raw.indexOf(',') >= 0;
        try {
            if (hasDot && hasComma) {
                int lastDot = raw.lastIndexOf('.');
                int lastComma = raw.lastIndexOf(',');
                char dec = lastDot > lastComma ? '.' : ',';
                char thou = dec == '.' ? ',' : '.';
                String s = raw.replace(String.valueOf(thou), "").replace(dec, '.');
                return (long) Math.floor(Double.parseDouble(s));
            }
            if (hasDot || hasComma) {
                char sep = hasDot ? '.' : ',';
                String[] parts = raw.split("\\" + sep);
                if (parts.length > 2) {
                    return Long.parseLong(raw.replace(String.valueOf(sep), ""));
                }
                if (parts.length == 2 && parts[1].length() == 3) {
                    return Long.parseLong(parts[0] + parts[1]);
                }
                return Long.parseLong(parts[0]);
            }
            return Long.parseLong(raw);
        } catch (Exception e) {
            return 0;
        }
    }

    /** "in", "out", atau "unknown". */
    public static String detectDirection(String title, String text) {
        String t = ((title == null ? "" : title) + " " + (text == null ? "" : text)).toLowerCase(Locale.ROOT);
        boolean isIn = false;
        boolean isOut = false;
        for (int i = 0; i < IN_WORDS.length; i++) {
            if (t.contains(IN_WORDS[i])) { isIn = true; break; }
        }
        for (int i = 0; i < OUT_WORDS.length; i++) {
            if (t.contains(OUT_WORDS[i])) { isOut = true; break; }
        }
        if (isIn && !isOut) return "in";
        if (isOut && !isIn) return "out";
        return "unknown";
    }

    public static String rupiah(long n) {
        String s = String.valueOf(Math.abs(n));
        StringBuilder sb = new StringBuilder();
        int c = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            sb.append(s.charAt(i));
            c++;
            if (c % 3 == 0 && i > 0) sb.append('.');
        }
        return (n < 0 ? "-" : "") + "Rp " + sb.reverse().toString();
    }
}
