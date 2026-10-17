package com.rama.mako_zero.managers;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SystemGroups {
    public static final String DISABLED = "disabled";
    public static final String NOT_RUNNING = "not_running";

    public static final class Def {
        public final String id;
        public final String label;
        final Set<String> permissions;

        Def(String id, String label, String... permissions) {
            this.id = id;
            this.label = label;
            this.permissions = new HashSet<>(Arrays.asList(permissions));
        }
    }

    private static final String P = "android.permission.";

    public static final List<Def> DEFS = Arrays.asList(
            new Def("sensors", "Sensors", P + "BODY_SENSORS", P + "BODY_SENSORS_BACKGROUND",
                    P + "ACTIVITY_RECOGNITION", P + "HIGH_SAMPLING_RATE_SENSORS", P + "UWB_RANGING"),
            new Def("networks", "Networks", P + "INTERNET", P + "ACCESS_NETWORK_STATE", P + "CHANGE_NETWORK_STATE",
                    P + "ACCESS_WIFI_STATE", P + "CHANGE_WIFI_STATE", P + "NEARBY_WIFI_DEVICES"),
            new Def("camera", "Camera", P + "CAMERA"),
            new Def("microphone", "Microphone", P + "RECORD_AUDIO"),
            new Def("location", "Location", P + "ACCESS_FINE_LOCATION", P + "ACCESS_COARSE_LOCATION",
                    P + "ACCESS_BACKGROUND_LOCATION", P + "ACCESS_MEDIA_LOCATION"),
            new Def("notifications", "Notifications", P + "POST_NOTIFICATIONS"),
            // Suggested extras
            new Def("contacts", "Contacts", P + "READ_CONTACTS", P + "WRITE_CONTACTS", P + "GET_ACCOUNTS"),
            new Def("phone", "Phone", P + "READ_PHONE_STATE", P + "READ_PHONE_NUMBERS", P + "CALL_PHONE",
                    P + "ANSWER_PHONE_CALLS", P + "READ_CALL_LOG", P + "WRITE_CALL_LOG", P + "PROCESS_OUTGOING_CALLS"),
            new Def("sms", "SMS", P + "SEND_SMS", P + "RECEIVE_SMS", P + "READ_SMS", P + "RECEIVE_MMS", P + "RECEIVE_WAP_PUSH"),
            new Def("calendar", "Calendar", P + "READ_CALENDAR", P + "WRITE_CALENDAR"),
            new Def("storage", "Storage", P + "READ_EXTERNAL_STORAGE", P + "WRITE_EXTERNAL_STORAGE",
                    P + "MANAGE_EXTERNAL_STORAGE", P + "READ_MEDIA_IMAGES", P + "READ_MEDIA_VIDEO",
                    P + "READ_MEDIA_AUDIO", P + "READ_MEDIA_VISUAL_USER_SELECTED"),
            new Def("bluetooth", "Bluetooth", P + "BLUETOOTH", P + "BLUETOOTH_ADMIN", P + "BLUETOOTH_CONNECT",
                    P + "BLUETOOTH_SCAN", P + "BLUETOOTH_ADVERTISE")
    );

    private SystemGroups() {
    }

    public static List<String> allIds() {
        List<String> ids = new ArrayList<>();
        for (Def d : DEFS) ids.add(d.id);
        ids.add(DISABLED);
        ids.add(NOT_RUNNING);
        return ids;
    }

    public static String labelOf(String id) {
        if (DISABLED.equals(id)) return "Disabled";
        if (NOT_RUNNING.equals(id)) return "Not running";
        for (Def d : DEFS) if (d.id.equals(id)) return d.label;
        return id;
    }

    public static Map<String, List<AppsProvider.AppEntry>> compute(Context context, List<AppsProvider.AppEntry> apps) {
        Map<String, List<AppsProvider.AppEntry>> result = new LinkedHashMap<>();
        for (String id : allIds()) result.put(id, new ArrayList<AppsProvider.AppEntry>());
        PackageManager pm = context.getPackageManager();
        Map<String, Set<String>> grantedByPackage = new java.util.HashMap<>();

        for (int i = 0; i < apps.size(); i++) {
            AppsProvider.AppEntry app = apps.get(i);
            if (app.unavailable) {
                result.get(DISABLED).add(app);
                continue; // can't say anything reliable about permissions of a disabled/missing app
            }
            if (app.isStopped()) {
                result.get(NOT_RUNNING).add(app);
            }
            if (app.isOtherProfile) continue; // permissions of other profiles aren't readable from here
            Set<String> granted = grantedByPackage.get(app.packageName);
            if (granted == null) {
                granted = grantedPermissions(pm, app.packageName);
                grantedByPackage.put(app.packageName, granted);
            }
            for (Def def : DEFS) {
                for (String perm : def.permissions) {
                    if (granted.contains(perm)) {
                        result.get(def.id).add(app);
                        break;
                    }
                }
            }
        }
        java.util.Iterator<Map.Entry<String, List<AppsProvider.AppEntry>>> it = result.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isEmpty()) it.remove();
        }
        return result;
    }

    private static Set<String> grantedPermissions(PackageManager pm, String packageName) {
        Set<String> granted = new HashSet<>();
        try {
            PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions == null) return granted;
            for (int i = 0; i < info.requestedPermissions.length; i++) {
                boolean ok = true;
                if (Build.VERSION.SDK_INT >= 16 && info.requestedPermissionsFlags != null) {
                    ok = (info.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;
                }
                if (ok) granted.add(info.requestedPermissions[i]);
            }
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            // not visible or gone: no permissions
        }
        return granted;
    }
}
