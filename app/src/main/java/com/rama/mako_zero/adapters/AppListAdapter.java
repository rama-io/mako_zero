package com.rama.mako_zero.adapters;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rama.mako_zero.R;
import com.rama.mako_zero.managers.AppsProvider;
import com.rama.mako_zero.managers.FontManager;
import com.rama.mako_zero.managers.GroupManager;
import com.rama.mako_zero.managers.PrefsManager;
import com.rama.mako_zero.managers.SystemGroups;
import com.rama.mako_zero.managers.ThemeManager;
import com.rama.mako_zero.managers.ZoomManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AppListAdapter extends BaseAdapter {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_APP = 1;
    final long APP_SIZE_WARNING_BYTES = 200L * 1024L * 1024L;

    public interface Listener {
        void onAppLaunchFailed();

        void onOpenSettingsRequested();

        void onSelectionChanged(boolean active, int count);
    }

    public static class HeaderRow {
        public final String groupId;
        public final String label;
        public final boolean system;
        public final int count;

        HeaderRow(String groupId, String label, boolean system, int count) {
            this.groupId = groupId;
            this.label = label;
            this.system = system;
            this.count = count;
        }
    }

    private final Context context;
    private final AppsProvider appsProvider;
    private final GroupManager groupManager;
    private final List<Object> items = new ArrayList<>();
    private final Set<String> selectedKeys = new HashSet<>();
    private boolean multiSelectMode = false;
    private Listener listener;
    private final Map<String, AppsProvider.AppEntry> trackedByKey = new HashMap<>();
    private Map<String, List<AppsProvider.AppEntry>> systemSnapshot;

    public AppListAdapter(Context context, AppsProvider appsProvider, GroupManager groupManager) {
        this.context = context;
        this.appsProvider = appsProvider;
        this.groupManager = groupManager;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private static final long LONG_PRESS_TAIL_MS = 500;
    private boolean longPressHandled;
    private long lastTouchTime;

    public void onTouch(int action, long eventTime) {
        lastTouchTime = eventTime;
        if (action == MotionEvent.ACTION_DOWN) {
            longPressHandled = false;
        }
    }

    void onLongPress() {
        longPressHandled = true;
    }

    boolean isLongPressTail(long now) {
        return longPressHandled && now - lastTouchTime < LONG_PRESS_TAIL_MS;
    }

    public boolean isMultiSelectMode() {
        return multiSelectMode;
    }

    public AppsProvider.AppEntry getSingleSelectedApp() {
        if (selectedKeys.size() != 1) {
            return null;
        }
        return trackedByKey.get(selectedKeys.iterator().next());
    }

    public boolean isSystemMode() {
        return PrefsManager.LIST_MODE_SYSTEM.equals(PrefsManager.getInstance(context).getListMode());
    }

    public void toggleListMode() {
        PrefsManager prefs = PrefsManager.getInstance(context);
        prefs.setListMode(isSystemMode() ? PrefsManager.LIST_MODE_CUSTOM : PrefsManager.LIST_MODE_SYSTEM);
        if (multiSelectMode) {
            exitMultiSelectMode(); // refreshes
        } else {
            refresh();
        }
    }

    public boolean canRemoveSelection() {
        if (selectedKeys.isEmpty()) return false;
        for (String key : selectedKeys) {
            AppsProvider.AppEntry app = trackedByKey.get(key);
            if (app == null || !app.isGhost()) return false;
        }
        return true;
    }

    public int removeSelectedGhosts() {
        if (!canRemoveSelection()) return 0;
        PrefsManager prefs = PrefsManager.getInstance(context);
        int removed = selectedKeys.size();
        for (String key : selectedKeys) {
            prefs.untrackApp(key);
        }
        exitMultiSelectMode();
        return removed;
    }

    public void reload() {
        systemSnapshot = null;
        refresh();
    }

    public void exitMultiSelectMode() {
        multiSelectMode = false;
        selectedKeys.clear();
        notifySelectionChanged();
        refresh();
    }

    public void moveSelectedAppsToGroup(String groupId) {
        for (String key : selectedKeys) {
            groupManager.moveAppToGroup(key, groupId);
        }
        exitMultiSelectMode();
    }

    private void enterMultiSelectMode(String key) {
        multiSelectMode = true;
        selectedKeys.clear();
        selectedKeys.add(key);
        notifySelectionChanged();
        refresh();
    }

    private void toggleSelection(String key) {
        if (selectedKeys.contains(key)) {
            selectedKeys.remove(key);
            if (selectedKeys.isEmpty()) {
                exitMultiSelectMode();
                return;
            }
        } else {
            selectedKeys.add(key);
        }
        notifySelectionChanged();
        refresh();
    }

    private void notifySelectionChanged() {
        if (listener != null) {
            listener.onSelectionChanged(multiSelectMode, selectedKeys.size());
        }
    }

    public void refresh() {
        List<AppsProvider.AppEntry> allApps = appsProvider.getTracked();
        trackedByKey.clear();
        for (int i = 0; i < allApps.size(); i++) {
            trackedByKey.put(allApps.get(i).key, allApps.get(i));
        }
        items.clear();
        if (isSystemMode()) {
            buildSystemItems(allApps);
        } else {
            buildCustomItems(allApps);
        }
        notifyDataSetChanged();
    }

    private void buildCustomItems(List<AppsProvider.AppEntry> allApps) {
        Map<String, List<AppsProvider.AppEntry>> byGroup = new HashMap<String, List<AppsProvider.AppEntry>>();
        for (int i = 0; i < allApps.size(); i++) {
            AppsProvider.AppEntry app = allApps.get(i);
            String groupId = groupManager.getAppGroupId(app.key);
            List<AppsProvider.AppEntry> bucket = byGroup.get(groupId);
            if (bucket == null) {
                bucket = new ArrayList<>();
                byGroup.put(groupId, bucket);
            }
            bucket.add(app);
        }
        List<String> groupIds = groupManager.getGroupIds();
        for (int i = 0; i < groupIds.size(); i++) {
            String groupId = groupIds.get(i);
            if (!groupManager.isGroupVisible(groupId)) {
                continue;
            }
            List<AppsProvider.AppEntry> apps = byGroup.get(groupId);
            if (apps == null || apps.isEmpty()) {
                continue;
            }
            items.add(new HeaderRow(groupId, groupManager.getGroupLabel(groupId), false, apps.size()));
            if (groupManager.isGroupExpanded(groupId)) {
                items.addAll(apps);
            }
        }
    }

    private void buildSystemItems(List<AppsProvider.AppEntry> allApps) {
        // The permission scan is the expensive part; reuse it for header taps, redo it on reload().
        if (systemSnapshot == null) {
            systemSnapshot = SystemGroups.compute(context, allApps);
        }
        for (Map.Entry<String, List<AppsProvider.AppEntry>> group : systemSnapshot.entrySet()) {
            String id = group.getKey();
            List<AppsProvider.AppEntry> apps = new ArrayList<>(group.getValue().size());
            for (AppsProvider.AppEntry snap : group.getValue()) {
                AppsProvider.AppEntry current = trackedByKey.get(snap.key);
                if (current != null) apps.add(current); // drop apps removed since the snapshot
            }
            if (apps.isEmpty()) continue;
            items.add(new HeaderRow(id, SystemGroups.labelOf(id), true, apps.size()));
            if (groupManager.isSystemGroupExpanded(id)) {
                items.addAll(apps);
            }
        }
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof HeaderRow ? TYPE_HEADER : TYPE_APP;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Object item = items.get(position);
        if (item instanceof HeaderRow) {
            return getHeaderView((HeaderRow) item, convertView, parent);
        }
        return getAppView((AppsProvider.AppEntry) item, convertView, parent);
    }

    private View getHeaderView(HeaderRow header, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_header, parent, false);
        }
        final String groupId = header.groupId;
        TextView label = view.findViewById(R.id.header_text);
        label.setTypeface(FontManager.getJersey25(context));
        label.setTextColor(ThemeManager.currentPalette(context).accent);
        boolean pinned = !header.system && groupManager.isGroupKeepExpanded(groupId);
        boolean expanded = header.system ? groupManager.isSystemGroupExpanded(groupId) : groupManager.isGroupExpanded(groupId);
        String symbol = pinned ? "" : (context.getString(expanded ? R.string.label_collapse_indicator : R.string.label_expand_indicator) + " ");
        String indicator = symbol + "------ ";
        String suffix = header.system ? " (" + header.count + ")" : "";
        label.setText(indicator + header.label.toUpperCase(Locale.getDefault()) + suffix);
        if (pinned) {
            view.setOnClickListener(null);
            view.setClickable(false);
        } else {
            final boolean system = header.system;
            view.setOnClickListener(v -> handleHeaderClick(groupId, system));
        }
        ZoomManager.apply(context, view);
        return view;
    }

    private void handleHeaderClick(String groupId, boolean system) {
        if (system) {
            groupManager.toggleSystemGroupExpanded(groupId);
        } else {
            if (groupManager.isGroupKeepExpanded(groupId)) return;
            groupManager.toggleGroupExpanded(groupId);
        }
        refresh();
    }

    private void handleAppClick(AppsProvider.AppEntry app) {
        if (isLongPressTail(SystemClock.uptimeMillis())) return;
        if (multiSelectMode) {
            toggleSelection(app.key);
        } else if (app.unavailable || !appsProvider.launch(app)) {
            if (listener != null) {
                listener.onAppLaunchFailed();
            }
        }
    }

    public void performRowAction(int position) {
        if (position < 0 || position >= items.size()) return;
        Object item = items.get(position);
        if (item instanceof HeaderRow) {
            handleHeaderClick(((HeaderRow) item).groupId, ((HeaderRow) item).system);
        } else if (item instanceof AppsProvider.AppEntry) {
            handleAppClick((AppsProvider.AppEntry) item);
        }
    }

    private View getAppView(final AppsProvider.AppEntry app, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_app, parent, false);
        }
        final TextView label = view.findViewById(R.id.app_label);
        label.setTypeface(FontManager.getJersey25(context));
        label.setTextColor((app.unavailable || app.isStopped()) ? ThemeManager.currentPalette(context).disabled : ThemeManager.currentPalette(context).text);
        label.setText(groupManager.getDisplayLabel(app));
        View emptySpace = view.findViewById(R.id.empty_space);
        ImageView selectionCheck = view.findViewById(R.id.selection_check);
        if (!multiSelectMode) {
            selectionCheck.setVisibility(View.GONE);
        } else {
            boolean isSelected = selectedKeys.contains(app.key);
            selectionCheck.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
        }
        LinearLayout apiRow = view.findViewById(R.id.api);
        if (!app.isGhost() && PrefsManager.getInstance(context).hasApiIndicatorsVisible()) {
            TextView minApiText = view.findViewById(R.id.min_api);
            TextView apiSeparator = view.findViewById(R.id.api_separator);
            TextView targetApiText = view.findViewById(R.id.target_api);
            apiRow.setVisibility(View.VISIBLE);
            minApiText.setText(String.valueOf(app.getMinSdkVersion()));
            targetApiText.setText(String.valueOf(app.getTargetSdkVersion()));
            boolean isOutdatedTarget = app.getTargetSdkVersion() < Build.VERSION.SDK_INT;
            int apiColor = isOutdatedTarget ? ThemeManager.currentPalette(context).error : ThemeManager.currentPalette(context).disabled;
            minApiText.setTextColor(apiColor);
            apiSeparator.setTextColor(apiColor);
            targetApiText.setTextColor(apiColor);
        } else {
            apiRow.setVisibility(View.GONE);
        }
        TextView appSize = view.findViewById(R.id.app_size);
        if (!app.isGhost() && PrefsManager.getInstance(context).hasAppSizeVisible()) {
            long sizeBytes = appsProvider.getAppSizeBytes(app);
            appSize.setVisibility(View.VISIBLE);
            appSize.setText(Formatter.formatShortFileSize(context, sizeBytes));
            int sizeColor;
            if (sizeBytes > APP_SIZE_WARNING_BYTES) {
                sizeColor = ThemeManager.currentPalette(context).error;
            } else {
                sizeColor = ThemeManager.currentPalette(context).disabled;
            }
            appSize.setTextColor(sizeColor);
        } else {
            appSize.setVisibility(View.GONE);
        }
        View.OnClickListener launchOrToggle = v -> handleAppClick(app);
        view.setOnClickListener(launchOrToggle);
        label.setOnClickListener(launchOrToggle);
        emptySpace.setOnClickListener(launchOrToggle);
        View.OnLongClickListener selectOnLongPress = v -> {
            onLongPress();
            if (multiSelectMode) {
                toggleSelection(app.key);
            } else {
                enterMultiSelectMode(app.key);
            }
            return true;
        };
        view.setOnLongClickListener(selectOnLongPress);
        label.setOnLongClickListener(selectOnLongPress);
        emptySpace.setOnLongClickListener(v -> {
            onLongPress();
            if (multiSelectMode) {
                toggleSelection(app.key);
            } else if (listener != null) {
                listener.onOpenSettingsRequested();
            }
            return true;
        });
        ZoomManager.apply(context, view);
        return view;
    }
}