package com.rama.mako_zero.helpers;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;

public final class AppSettings {
    private AppSettings() {
    }

    public static Intent appDetails(String packageName) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= 9) {
            intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", packageName, null));
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra(Build.VERSION.SDK_INT == 8 ? "pkg" : "com.android.settings.ApplicationPkgName", packageName);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
