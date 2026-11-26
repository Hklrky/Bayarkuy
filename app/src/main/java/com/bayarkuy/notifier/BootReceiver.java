package com.bayarkuy.notifier;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;

/**
 * Menyalakan ulang layanan pembaca notifikasi setelah HP restart / aplikasi diperbarui.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                NotificationListenerService.requestRebind(new ComponentName(context, NotifListener.class));
            }
        } catch (Exception ignored) {
        }
        try {
            Dispatcher.kick(context);
        } catch (Exception ignored) {
        }
    }
}
