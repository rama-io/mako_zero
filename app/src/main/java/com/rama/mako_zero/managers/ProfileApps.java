package com.rama.mako_zero.managers;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherUserInfo;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
final class ProfileApps {
    private final Context context;
    private final LauncherApps launcherApps;
    private final UserManager userManager;

    ProfileApps(Context context) {
        this.context = context;
        this.launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        this.userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    List<AppsProvider.AppEntry> getApps() {
        List<AppsProvider.AppEntry> result = new ArrayList<>();
        UserHandle self = Process.myUserHandle();
        UserHandle privateProfile = getPrivateProfile();
        List<UserHandle> profiles = userManager.getUserProfiles();
        for (int i = 0; i < profiles.size(); i++) {
            UserHandle profile = profiles.get(i);
            if (profile.equals(self)) continue;
            if (profile.equals(privateProfile) && isQuiet(profile)) continue;

            List<LauncherActivityInfo> activities;
            try {
                activities = launcherApps.getActivityList(null, profile);
            } catch (SecurityException | IllegalStateException e) {
                continue;
            }
            if (activities == null) continue;

            String initial = getProfileInitial(profile);
            long serial = userManager.getSerialNumberForUser(profile);
            for (int j = 0; j < activities.size(); j++) {
                LauncherActivityInfo info = activities.get(j);
                ApplicationInfo appInfo = info.getApplicationInfo();
                String packageName = appInfo.packageName;
                String label = FontManager.sanitizeForFont(info.getLabel().toString());
                String key = packageName + ":profile_" + serial;
                result.add(new AppsProvider.AppEntry(packageName, info.getName(), label, appInfo, profile, key, initial));
            }
        }
        return result;
    }

    boolean launch(AppsProvider.AppEntry app) {
        if (!(app.user instanceof UserHandle)) return false;
        try {
            launcherApps.startMainActivity(new ComponentName(app.packageName, app.activityName), (UserHandle) app.user, null, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    boolean openAppDetails(AppsProvider.AppEntry app) {
        if (!(app.user instanceof UserHandle)) return false;
        try {
            launcherApps.startAppDetailsActivity(new ComponentName(app.packageName, app.activityName), (UserHandle) app.user, null, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    boolean hasPrivateSpace() {
        return getPrivateProfile() != null;
    }

    boolean isPrivateSpaceLocked() {
        UserHandle handle = getPrivateProfile();
        return handle != null && isQuiet(handle);
    }

    boolean setPrivateSpaceLocked(boolean locked) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false;
        UserHandle handle = getPrivateProfile();
        if (handle == null) return false;
        try {
            return userManager.requestQuietModeEnabled(locked, handle);
        } catch (SecurityException e) {
            return false;
        }
    }

    private UserHandle getPrivateProfile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return null;
        List<UserHandle> profiles = userManager.getUserProfiles();
        for (int i = 0; i < profiles.size(); i++) {
            UserHandle profile = profiles.get(i);
            try {
                LauncherUserInfo info = launcherApps.getLauncherUserInfo(profile);
                if (info != null && UserManager.USER_TYPE_PROFILE_PRIVATE.equals(info.getUserType())) {
                    return profile;
                }
            } catch (Exception e) {
                // ignore this profile
            }
        }
        return null;
    }

    private boolean isQuiet(UserHandle profile) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && userManager.isQuietModeEnabled(profile);
    }

    private String getProfileInitial(UserHandle profile) {
        String badged = context.getPackageManager().getUserBadgedLabel("", profile).toString();
        for (int i = 0; i < badged.length(); i++) {
            char c = badged.charAt(i);
            if (Character.isLetter(c)) {
                String initial = FontManager.sanitizeForFont(String.valueOf(Character.toUpperCase(c)));
                return "?".equals(initial) ? "E" : initial;
            }
        }
        return "E";
    }
}
