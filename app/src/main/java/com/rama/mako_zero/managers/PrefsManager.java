package com.rama.mako_zero.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.rama.mako_zero.objects.PrefTheme;
import com.rama.mako_zero.objects.Themes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class PrefsManager {
    public static final String PREVENT_ROTATION = "settings:prevent_rotation";
    private static final String PREFS_NAME = "mako_zero";
    public static final String DEFAULT_GROUP_ID = "ungrouped";
    public static final String DEFAULT_GROUP_LABEL = "Default";
    private static final String KEY_FLAVOR = "settings:flavor";
    private static final String KEY_ACCENT = "settings:accent";
    private static final String KEY_ACCENT_ROLLED = "settings:accent_rolled";
    /** Older builds stored flavor and accent as one id, e.g. "catppuccin_mocha_blue". Only read to migrate. */
    private static final String KEY_LEGACY_THEME = "settings:theme";
    private static final String KEY_LEGACY_THEME_ROLLED = "settings:theme_rolled_id";
    private static final String LEGACY_MOCHA_PREFIX = "catppuccin_mocha_";
    private static final String KEY_COLLAPSE_ON_HOME = "settings:collapse_groups_on_home";
    private static final String KEY_ONLY_ONE_GROUP_OPEN = "settings:only_one_group_open";
    private static final String KEY_SHOW_API_INDICATORS = "apps:show_api_indicators";
    private static final String APPS_SHOW_SIZE = "apps:show_size";
    private static final String KEY_PROFILE_INDICATOR = "apps:profile_indicator";
    private static final String KEY_ZOOM_PERCENT = "settings:zoom_percent";
    private static PrefsManager instance;
    private final SharedPreferences prefs;

    private PrefsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        migrateLegacyTheme();
    }

    /** Splits an old single theme id into {flavor, accent}, or null when it is not one. */
    private static String[] parseLegacyTheme(String id) {
        if (id == null) return null;
        if (id.startsWith(LEGACY_MOCHA_PREFIX)) {
            String accent = id.substring(LEGACY_MOCHA_PREFIX.length());
            if (PrefTheme.ACCENT_RANDOM.equals(accent) || Themes.isAccent(accent)) {
                return new String[]{"mocha", accent};
            }
            return null;
        }
        // Latte used to be fixed to its blue accent.
        if ("catppuccin_latte".equals(id)) return new String[]{"latte", "blue"};
        if (Themes.isFlavor(id) && !Themes.hasAccents(id)) return new String[]{id, PrefTheme.DEFAULT_ACCENT};
        return null;
    }

    private void migrateLegacyTheme() {
        if (!prefs.contains(KEY_LEGACY_THEME) && !prefs.contains(KEY_LEGACY_THEME_ROLLED)) return;
        SharedPreferences.Editor editor = prefs.edit();
        if (!prefs.contains(KEY_FLAVOR)) {
            String[] legacy = parseLegacyTheme(prefs.getString(KEY_LEGACY_THEME, null));
            if (legacy != null) {
                editor.putString(KEY_FLAVOR, legacy[0]);
                editor.putString(KEY_ACCENT, legacy[1]);
            }
        }
        String[] rolled = parseLegacyTheme(prefs.getString(KEY_LEGACY_THEME_ROLLED, null));
        if (rolled != null && Themes.isAccent(rolled[1]) && !prefs.contains(KEY_ACCENT_ROLLED)) {
            editor.putString(KEY_ACCENT_ROLLED, rolled[1]);
        }
        editor.remove(KEY_LEGACY_THEME).remove(KEY_LEGACY_THEME_ROLLED).commit();
    }

    public static synchronized PrefsManager getInstance(Context context) {
        if (instance == null) {
            instance = new PrefsManager(context);
        }
        return instance;
    }

    private String key(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(':');
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    private List<String> splitCsv(String value) {
        List<String> result = new ArrayList<String>();
        if (value == null || value.length() == 0) return result;
        String[] parts = value.split(",");
        for (String part : parts) {
            if (part.length() > 0) result.add(part);
        }
        return result;
    }

    private String joinCsv(java.util.Collection<String> values) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String value : values) {
            if (!first) sb.append(',');
            sb.append(value);
            first = false;
        }
        return sb.toString();
    }

    public List<String> getGroupIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<String>();
        ids.add(DEFAULT_GROUP_ID);
        ids.addAll(splitCsv(prefs.getString(key("groups", "ids"), "")));
        return new ArrayList<>(ids);
    }

    public void addGroupId(String groupId) {
        List<String> ids = getGroupIds();
        if (!ids.contains(groupId)) {
            ids.add(groupId);
            prefs.edit().putString(key("groups", "ids"), joinCsv(ids)).commit();
        }
    }

    public String getGroupLabel(String groupId) {
        if (DEFAULT_GROUP_ID.equals(groupId)) {
            return prefs.getString(key("group", groupId, "label"), DEFAULT_GROUP_LABEL);
        }
        return prefs.getString(key("group", groupId, "label"), groupId);
    }

    public void setGroupLabel(String groupId, String label) {
        prefs.edit().putString(key("group", groupId, "label"), label).commit();
    }

    public int getGroupOrder(String groupId) {
        int fallback = DEFAULT_GROUP_ID.equals(groupId) ? 0 : Integer.MAX_VALUE;
        return prefs.getInt(key("group", groupId, "order"), fallback);
    }

    public void setGroupOrder(String groupId, int order) {
        prefs.edit().putInt(key("group", groupId, "order"), order).commit();
    }

    public boolean isGroupExpanded(String groupId) {
        return prefs.getBoolean(key("group", groupId, "expanded"), true);
    }

    public void setGroupExpanded(String groupId, boolean expanded) {
        prefs.edit().putBoolean(key("group", groupId, "expanded"), expanded).commit();
    }

    public String getAppGroupId(String appKey) {
        return prefs.getString(key("app", appKey, "group"), DEFAULT_GROUP_ID);
    }

    public void setAppGroupId(String appKey, String groupId) {
        prefs.edit().putString(key("app", appKey, "group"), groupId).commit();
    }

    public String getCustomName(String appKey) {
        return prefs.getString(key("app", appKey, "label"), null);
    }

    public void setCustomName(String appKey, String label) {
        prefs.edit().putString(key("app", appKey, "label"), label).commit();
    }

    public void clearCustomName(String appKey) {
        prefs.edit().remove(key("app", appKey, "label")).commit();
    }

    public void removeGroupId(String groupId) {
        if (DEFAULT_GROUP_ID.equals(groupId)) return;
        List<String> ids = getGroupIds();
        ids.remove(groupId);
        prefs.edit().putString(key("groups", "ids"), joinCsv(ids)).remove(key("group", groupId, "label")).remove(key("group", groupId, "order")).remove(key("group", groupId, "expanded")).remove(key("group", groupId, "visible")).commit();
    }

    public boolean isGroupVisible(String groupId) {
        return prefs.getBoolean(key("group", groupId, "visible"), true);
    }

    public void setGroupVisible(String groupId, boolean visible) {
        prefs.edit().putBoolean(key("group", groupId, "visible"), visible).commit();
    }

    public boolean isGroupKeepExpanded(String groupId) {
        return prefs.getBoolean(key("group", groupId, "keep_expanded"), false);
    }

    public void setGroupKeepExpanded(String groupId, boolean value) {
        prefs.edit().putBoolean(key("group", groupId, "keep_expanded"), value).commit();
    }

    public List<String> getTrackedKeys() {
        return splitCsv(prefs.getString(key("apps", "tracked"), ""));
    }

    public void syncTracked(List<String[]> entries) {
        LinkedHashSet<String> keys = new LinkedHashSet<>(getTrackedKeys());
        SharedPreferences.Editor editor = prefs.edit();
        boolean dirty = false;
        for (int i = 0; i < entries.size(); i++) {
            String[] e = entries.get(i);
            if (keys.add(e[0])) dirty = true;
            dirty |= putIfChanged(editor, key("app", e[0], "pkg"), e[1]);
            dirty |= putIfChanged(editor, key("app", e[0], "act"), e[2]);
            dirty |= putIfChanged(editor, key("app", e[0], "orig_label"), e[3]);
            dirty |= putIfChanged(editor, key("app", e[0], "initial"), e[4]);
        }
        if (dirty) {
            editor.putString(key("apps", "tracked"), joinCsv(keys)).commit();
        }
    }

    private boolean putIfChanged(SharedPreferences.Editor editor, String k, String value) {
        if (value.equals(prefs.getString(k, null))) return false;
        editor.putString(k, value);
        return true;
    }

    /** { packageName, activityName, label, profileInitial } or null if unknown. */
    public String[] getTrackedMeta(String appKey) {
        String pkg = prefs.getString(key("app", appKey, "pkg"), null);
        if (pkg == null) return null;
        return new String[]{
                pkg,
                prefs.getString(key("app", appKey, "act"), ""),
                prefs.getString(key("app", appKey, "orig_label"), pkg),
                prefs.getString(key("app", appKey, "initial"), "")
        };
    }

    public void untrackApp(String appKey) {
        List<String> keys = getTrackedKeys();
        keys.remove(appKey);
        prefs.edit()
                .putString(key("apps", "tracked"), joinCsv(keys))
                .remove(key("app", appKey, "pkg"))
                .remove(key("app", appKey, "act"))
                .remove(key("app", appKey, "orig_label"))
                .remove(key("app", appKey, "initial"))
                .remove(key("app", appKey, "group"))
                .remove(key("app", appKey, "label"))
                .commit();
    }

    public static final String LIST_MODE_CUSTOM = "custom";
    public static final String LIST_MODE_SYSTEM = "system";

    public String getListMode() {
        return prefs.getString("list:mode", LIST_MODE_CUSTOM);
    }

    public void setListMode(String mode) {
        prefs.edit().putString("list:mode", mode).commit();
    }

    public boolean isSystemGroupExpanded(String id) {
        return prefs.getBoolean(key("sysgroup", id, "expanded"), false);
    }

    public void setSystemGroupExpanded(String id, boolean expanded) {
        prefs.edit().putBoolean(key("sysgroup", id, "expanded"), expanded).commit();
    }

    public String getFlavor() {
        String id = prefs.getString(KEY_FLAVOR, PrefTheme.DEFAULT_FLAVOR);
        return Themes.isFlavor(id) ? id : PrefTheme.DEFAULT_FLAVOR;
    }

    public void setFlavor(String flavorId) {
        prefs.edit().putString(KEY_FLAVOR, flavorId).commit();
    }

    public String getAccent() {
        String id = prefs.getString(KEY_ACCENT, PrefTheme.DEFAULT_ACCENT);
        return Themes.isAccent(id) || PrefTheme.ACCENT_RANDOM.equals(id) ? id : PrefTheme.DEFAULT_ACCENT;
    }

    public void setAccent(String accentId) {
        prefs.edit().putString(KEY_ACCENT, accentId).commit();
    }

    public String getRolledAccent() {
        String id = prefs.getString(KEY_ACCENT_ROLLED, Themes.DEFAULT_ACCENT_COLOR);
        return Themes.isAccent(id) ? id : Themes.DEFAULT_ACCENT_COLOR;
    }

    public void setRolledAccent(String accentId) {
        prefs.edit().putString(KEY_ACCENT_ROLLED, accentId).commit();
    }

    public boolean shouldCollapseGroupsOnHome() {
        return getBoolean(KEY_COLLAPSE_ON_HOME, false);
    }

    public void setCollapseGroupsOnHome(boolean value) {
        setBoolean(KEY_COLLAPSE_ON_HOME, value);
    }

    public boolean isOnlyOneGroupOpenEnabled() {
        return getBoolean(KEY_ONLY_ONE_GROUP_OPEN, false);
    }

    public void setOnlyOneGroupOpenEnabled(boolean value) {
        setBoolean(KEY_ONLY_ONE_GROUP_OPEN, value);
    }

    public boolean hasApiIndicatorsVisible() {
        return getBoolean(KEY_SHOW_API_INDICATORS, false);
    }

    public void setApiIndicatorsVisible(boolean value) {
        setBoolean(KEY_SHOW_API_INDICATORS, value);
    }

    public boolean hasAppSizeVisible() {
        return getBoolean(APPS_SHOW_SIZE, false);
    }

    public void setAppSizeVisible(boolean value) {
        setBoolean(APPS_SHOW_SIZE, value);
    }

    public boolean hasProfileIndicator() {
        return getBoolean(KEY_PROFILE_INDICATOR, true);
    }

    public void setProfileIndicator(boolean value) {
        setBoolean(KEY_PROFILE_INDICATOR, value);
    }

    public int getZoomPercent() {
        return prefs.getInt(KEY_ZOOM_PERCENT, 100);
    }

    public void setZoomPercent(int percent) {
        prefs.edit().putInt(KEY_ZOOM_PERCENT, percent).commit();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    public void setBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).commit();
    }
}
