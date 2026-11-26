package com.bayarkuy.notifier;

import android.content.Context;

/**
 * Daftar aplikasi pembayaran yang dikenali. Paket bisa berbeda antar versi/vendor,
 * karena itu pengguna dapat menambah paket sendiri di menu Atur.
 */
public class Sources {

    /** {label, source, paket1,paket2,...} */
    public static final String[][] GROUPS = {
        {"GoBiz", "gobiz", "com.gojek.gobiz,com.gojek.resto,com.gojek.merchant"},
        {"BCA / myBCA", "bank", "com.bca,com.bca.mybca.omni.android"},
        {"BRImo", "bank", "id.co.bri.brimo"},
        {"BNI Mobile", "bank", "src.com.bni"},
        {"Livin' by Mandiri", "bank", "id.bmri.livin"},
        {"BSI Mobile", "bank", "com.bsm.activity2"},
        {"OCTO / CIMB Niaga", "bank", "id.co.cimbniaga.mobile.android"},
        {"Bank Jago", "bank", "com.jago.digitalBanking"},
        {"PermataMobile X", "bank", "net.myinfosys.PermataMobileX"},
        {"BTN Mobile", "bank", "com.btn.mobile"},
        {"blu by BCA Digital", "bank", "com.bcadigital.blu"},
        {"Neobank", "bank", "com.bnc.finance"},
        {"Gojek / GoPay", "ewallet", "com.gojek.app"},
        {"DANA", "ewallet", "id.dana"},
        {"OVO", "ewallet", "ovo.id"},
        {"ShopeePay", "ewallet", "com.shopee.id"},
        {"LinkAja", "ewallet", "com.telkom.mwallet"}
    };

    /**
     * Mencari sumber untuk paket tertentu.
     * @return {source, label} atau null jika paket tidak dipantau.
     */
    public static String[] lookup(Prefs prefs, String pkg) {
        for (int i = 0; i < GROUPS.length; i++) {
            String[] g = GROUPS[i];
            if (!prefs.isGroupEnabled(g[0])) continue;
            String[] pk = g[2].split(",");
            for (int j = 0; j < pk.length; j++) {
                if (pk[j].equals(pkg)) return new String[]{g[1], g[0]};
            }
        }
        String[] custom = prefs.getCustomPackages().split(",");
        for (int i = 0; i < custom.length; i++) {
            if (custom[i].trim().equals(pkg)) return new String[]{"bank", "Kustom"};
        }
        return null;
    }

    /** Apakah paket ini termasuk daftar bawaan (walau grupnya dimatikan)? */
    public static boolean isKnownPackage(String pkg) {
        for (int i = 0; i < GROUPS.length; i++) {
            String[] pk = GROUPS[i][2].split(",");
            for (int j = 0; j < pk.length; j++) {
                if (pk[j].equals(pkg)) return true;
            }
        }
        return false;
    }

    public static String appLabel(Context ctx, String pkg) {
        try {
            android.content.pm.PackageManager pm = ctx.getPackageManager();
            android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            CharSequence l = pm.getApplicationLabel(ai);
            return l == null ? pkg : l.toString();
        } catch (Exception e) {
            return pkg;
        }
    }
}
