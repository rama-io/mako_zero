package com.rama.mako_zero.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.TextView;

public class BatteryStatusManager {
    private static final int NO_TEMPERATURE = Integer.MIN_VALUE;
    private final Context context;
    private final TextView view;
    private boolean registered = false;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            update(intent);
        }
    };

    public BatteryStatusManager(Context context, TextView view) {
        this.context = context.getApplicationContext();
        this.view = view;
    }

    private void update(Intent intent) {
        if (intent == null) return;
        int level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) return;
        int levelPct = Math.round(level * 100f / scale);
        int tempTenthsC = intent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, NO_TEMPERATURE);
        view.setText(format(levelPct, tempTenthsC));
    }

    static String format(int levelPct, int tempTenthsC) {
        if (tempTenthsC == NO_TEMPERATURE) {
            return levelPct + "%";
        }
        return "BATTERY: " + levelPct + "% :: " + Math.round(tempTenthsC * 9 / 50f + 32) + "F";
    }

    public void register() {
        if (registered) return;
        Intent sticky = context.registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registered = true;
        update(sticky);
    }

    public void unregister() {
        if (!registered) return;
        context.unregisterReceiver(receiver);
        registered = false;
    }
}