package com.rama.mako_zero.managers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppsProvider {
    public static class AppEntry {
        public final String packageName;
        public final String activityName;
        public final String label;
        public final String key;
        public final boolean isOtherProfile;
        public final String profileInitial;
        final Object user;
        private final ApplicationInfo applicationInfo;

        AppEntry(String packageName, String activityName, String label, ApplicationInfo applicationInfo) {
            this(packageName, activityName, label, applicationInfo, null, packageName, null);
        }

        AppEntry(String packageName, String activityName, String label, ApplicationInfo applicationInfo,
                 Object user, String key, String profileInitial) {
            this.packageName = packageName;
            this.activityName = activityName;
            this.label = label;
            this.applicationInfo = applicationInfo;
            this.user = user;
            this.key = key;
            this.isOtherProfile = user != null;
            this.profileInitial = profileInitial;
        }

        public boolean unavailable;

        public static final int LEVEL_NONE = -1;
        public static final int LEVEL_UNINSTALLED = 0;
        public static final int LEVEL_DISABLED = 1;
        public static final int LEVEL_NOT_RUNNING = 2;

        public int ghostLevel = LEVEL_UNINSTALLED;

        public int getStatusLevel() {
            if (isGhost()) return ghostLevel;
            if (unavailable) return LEVEL_DISABLED;
            if (isStopped()) return LEVEL_NOT_RUNNING;
            return LEVEL_NONE;
        }

        public boolean isGhost() {
            return applicationInfo == null;
        }

        public boolean isStopped() {
            return applicationInfo != null && (applicationInfo.flags & ApplicationInfo.FLAG_STOPPED) != 0;
        }

        public int getMinSdkVersion() {
            if (applicationInfo == null) return 0;
            if (Build.VERSION.SDK_INT >= 24) {
                return applicationInfo.minSdkVersion;
            }
            return 0;
        }

        public int getTargetSdkVersion() {
            return applicationInfo == null ? 0 : applicationInfo.targetSdkVersion;
        }
    }

    private final Context context;
    private final Map<String, Long> appSizeCache = new HashMap<>();
    private final ProfileApps profileApps;

    public AppsProvider(Context context) {
        this.context = context.getApplicationContext();
        this.profileApps = Build.VERSION.SDK_INT >= 21 ? new ProfileApps(this.context) : null;
    }

    public long getAppSizeBytes(AppEntry app) {
        String key = appCacheKey(app);
        Long cached = appSizeCache.get(key);
        if (cached != null) {
            return cached.longValue();
        }
        long size = 0;
        if (app.isGhost()) return 0;
        try {
            ApplicationInfo info = app.applicationInfo;
            if (info.sourceDir != null) {
                size += new File(info.sourceDir).length();
            }
            if (Build.VERSION.SDK_INT >= 21 && info.splitSourceDirs != null) {
                for (int i = 0; i < info.splitSourceDirs.length; i++) {
                    String split = info.splitSourceDirs[i];
                    if (split != null) {
                        size += new File(split).length();
                    }
                }
            }
        } catch (Exception e) {
            size = 0;
        }
        appSizeCache.put(key, Long.valueOf(size));
        return size;
    }

    private String appCacheKey(AppEntry app) {
        return app.key + ":" + app.activityName;
    }

    public List<AppEntry> getAll() {
        PackageManager pm = context.getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(launcherIntent, PackageManager.GET_DISABLED_COMPONENTS);
        List<AppEntry> apps = new ArrayList<>(resolved.size());
        for (int i = 0; i < resolved.size(); i++) {
            ResolveInfo info = resolved.get(i);
            String packageName = info.activityInfo.packageName;
            String activityName = info.activityInfo.name;
            String label = FontManager.sanitizeForFont(info.loadLabel(pm).toString());
            AppEntry entry = new AppEntry(packageName, activityName, label, info.activityInfo.applicationInfo);
            entry.unavailable = !info.activityInfo.enabled || !info.activityInfo.applicationInfo.enabled;
            apps.add(entry);
        }
        if (profileApps != null) {
            apps.addAll(profileApps.getApps());
        }
        apps = dedupe(apps);
        sort(apps);
        return apps;
    }

    private static List<AppEntry> dedupe(List<AppEntry> apps) {
        Set<String> hasEnabled = new HashSet<>();
        for (int i = 0; i < apps.size(); i++) {
            if (!apps.get(i).unavailable) hasEnabled.add(apps.get(i).key);
        }
        Set<String> keptDisabled = new HashSet<>();
        List<AppEntry> out = new ArrayList<>(apps.size());
        for (int i = 0; i < apps.size(); i++) {
            AppEntry a = apps.get(i);
            if (!a.unavailable || (!hasEnabled.contains(a.key) && keptDisabled.add(a.key))) {
                out.add(a);
            }
        }
        return out;
    }

    public List<AppEntry> getTracked() {
        PrefsManager prefs = PrefsManager.getInstance(context);
        List<AppEntry> apps = getAll();
        Set<String> present = new HashSet<>();
        List<String[]> toSync = new ArrayList<>(apps.size());
        for (int i = 0; i < apps.size(); i++) {
            AppEntry a = apps.get(i);
            present.add(a.key);
            toSync.add(new String[]{a.key, a.packageName, a.activityName, a.label, a.profileInitial == null ? "" : a.profileInitial});
        }
        prefs.syncTracked(toSync);

        boolean hideProfileGhosts = isPrivateSpaceLocked();
        List<String> keys = prefs.getTrackedKeys();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            if (present.contains(key)) continue;
            boolean otherProfile = key.contains(":profile_");
            if (otherProfile && hideProfileGhosts) continue;
            String[] meta = prefs.getTrackedMeta(key);
            if (meta == null) continue;
            AppEntry ghost = new AppEntry(meta[0], meta[1], meta[2], null,
                    otherProfile ? Boolean.TRUE : null, key, meta[3].length() == 0 ? null : meta[3]);
            ghost.unavailable = true;
            ghost.ghostLevel = otherProfile ? AppEntry.LEVEL_UNINSTALLED : ghostLevelFor(meta[0]);
            apps.add(ghost);
        }
        sort(apps);
        return apps;
    }

    private int ghostLevelFor(String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return AppEntry.LEVEL_DISABLED;
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return AppEntry.LEVEL_UNINSTALLED;
        }
    }

    private static void sort(List<AppEntry> apps) {
        Collections.sort(apps, (AppEntry a, AppEntry b) -> {
            int byLabel = a.label.compareToIgnoreCase(b.label);
            if (byLabel != 0) return byLabel;
            if (a.isOtherProfile != b.isOtherProfile) return a.isOtherProfile ? 1 : -1;
            return a.key.compareTo(b.key);
        });
    }

    public boolean launch(AppEntry app) {
        if (app.unavailable) return false;
        if (app.isOtherProfile) {
            return profileApps != null && profileApps.launch(app);
        }
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(new ComponentName(app.packageName, app.activityName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean openAppDetails(AppEntry app) {
        return app.isOtherProfile && profileApps != null && profileApps.openAppDetails(app);
    }

    public boolean hasPrivateSpace() {
        return profileApps != null && profileApps.hasPrivateSpace();
    }

    public boolean isPrivateSpaceLocked() {
        return profileApps != null && profileApps.isPrivateSpaceLocked();
    }

    public boolean setPrivateSpaceLocked(boolean locked) {
        return profileApps != null && profileApps.setPrivateSpaceLocked(locked);
    }
}
