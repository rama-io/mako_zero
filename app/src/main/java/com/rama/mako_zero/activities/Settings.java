package com.rama.mako_zero.activities;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rama.mako_zero.R;
import com.rama.mako_zero.helpers.DialogHelper;
import com.rama.mako_zero.helpers.SystemBars;
import com.rama.mako_zero.managers.FontManager;
import com.rama.mako_zero.managers.GroupManager;
import com.rama.mako_zero.managers.PrefsManager;
import com.rama.mako_zero.managers.ThemeManager;
import com.rama.mako_zero.managers.ZoomManager;
import com.rama.mako_zero.objects.PrefTheme;
import com.rama.mako_zero.objects.Themes;
import com.rama.mako_zero.widgets.WdCheckbox;
import com.rama.mako_zero.widgets.WdRadio;
import com.rama.mako_zero.widgets.WdRadioGroup;

import java.util.ArrayList;
import java.util.List;

public class Settings extends BaseActivity {
    private GroupManager groupManager;
    private LinearLayout groupsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        groupManager = new GroupManager(this);
        groupsContainer = findViewById(R.id.groups_container);
        View root = findViewById(R.id.root);
        SystemBars.applyInsets(root);
        FontManager.apply(root, FontManager.getJersey25(this));
        setupSystemSection();
        setupZoomSection();
        setupGroupsSection();
        setupAppearanceSection();
        ThemeManager.applyTheme(this, root);
        if (Build.VERSION.SDK_INT < 11) {
            findViewById(R.id.themes_section).setVisibility(View.GONE);
            findViewById(R.id.themes_separator).setVisibility(View.GONE);
        }
        findViewById(R.id.go_about).setOnClickListener(v -> startActivity(new Intent(Settings.this, About.class)));
        findViewById(R.id.go_back).setOnClickListener(v -> startActivity(new Intent(Settings.this, Main.class)));
    }

    private void setupSystemSection() {
        findViewById(R.id.activate_button).setOnClickListener(v -> setLauncherAsDefault());
        findViewById(R.id.reset_button).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.this, Main.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        findViewById(R.id.change_apps_button).setOnClickListener(v -> {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(Settings.this, R.string.toast_unable_open_settings, Toast.LENGTH_SHORT).show();
            }
        });
        final WdCheckbox preventRotation = findViewById(R.id.prevent_home_screen_rotation);
        preventRotation.setChecked(PrefsManager.getInstance(this).getBoolean(PrefsManager.PREVENT_ROTATION, false));
        preventRotation.setOnCheckedChangeListener(isChecked -> PrefsManager.getInstance(Settings.this).setBoolean(PrefsManager.PREVENT_ROTATION, isChecked));
        final WdCheckbox showApiIndicators = findViewById(R.id.show_api_indicators);
        showApiIndicators.setChecked(PrefsManager.getInstance(this).hasApiIndicatorsVisible());
        showApiIndicators.setOnCheckedChangeListener(isChecked -> PrefsManager.getInstance(Settings.this).setApiIndicatorsVisible(isChecked));
        final WdCheckbox showAppSize = findViewById(R.id.show_app_size);
        showAppSize.setChecked(PrefsManager.getInstance(this).hasAppSizeVisible());
        showAppSize.setOnCheckedChangeListener(isChecked -> PrefsManager.getInstance(Settings.this).setAppSizeVisible(isChecked));
        final WdCheckbox showProfileIndicator = findViewById(R.id.show_profile_indicator);
        showProfileIndicator.setChecked(PrefsManager.getInstance(this).hasProfileIndicator());
        showProfileIndicator.setOnCheckedChangeListener(isChecked -> PrefsManager.getInstance(Settings.this).setProfileIndicator(isChecked));
    }

    private void setupZoomSection() {
        final EditText zoom = findViewById(R.id.zoom);
        zoom.setText(String.valueOf(ZoomManager.getPercent(this)));
        zoom.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                applyZoom();
            }
            return false;
        });
        zoom.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                applyZoom();
            }
        });
    }

    private void applyZoom() {
        saveZoomFromField();
        ZoomManager.apply(this, findViewById(R.id.root));
    }

    private void saveZoomFromField() {
        EditText zoom = findViewById(R.id.zoom);
        int percent;
        try {
            percent = ZoomManager.clamp(Integer.parseInt(zoom.getText().toString().trim()));
        } catch (NumberFormatException e) {
            percent = ZoomManager.getPercent(this);
        }
        String normalized = String.valueOf(percent);
        if (!normalized.equals(zoom.getText().toString())) {
            zoom.setText(normalized);
        }
        PrefsManager.getInstance(this).setZoomPercent(percent);
    }

    @Override
    protected void onPause() {
        saveZoomFromField();
        super.onPause();
    }

    private void setLauncherAsDefault() {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_HOME_SETTINGS));
            return;
        } catch (Exception ignored) {
            // No direct default-home-app screen before Android 10, fall through.
        }
        try {
            getPackageManager().clearPackagePreferredActivities(getPackageName());
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_unable_open_settings, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupGroupsSection() {
        renderGroups();
        final PrefsManager prefs = PrefsManager.getInstance(this);
        final WdCheckbox collapseOnHome = findViewById(R.id.collapse_groups_on_home);
        collapseOnHome.setChecked(prefs.shouldCollapseGroupsOnHome());
        collapseOnHome.setOnCheckedChangeListener(prefs::setCollapseGroupsOnHome);
        final WdCheckbox onlyOneOpen = findViewById(R.id.only_one_group_open);
        onlyOneOpen.setChecked(prefs.isOnlyOneGroupOpenEnabled());
        onlyOneOpen.setOnCheckedChangeListener(prefs::setOnlyOneGroupOpenEnabled);
        findViewById(R.id.add_group_button).setOnClickListener(v -> {
            groupManager.createGroup(getString(R.string.new_group_header));
            renderGroups();
        });
    }

    private void setupAppearanceSection() {
        final PrefsManager prefs = PrefsManager.getInstance(this);
        WdRadioGroup themeGroup = findViewById(R.id.theme_group);
        boolean randomMode = PrefTheme.CATPPUCCIN_MOCHA_RANDOM.equals(prefs.getTheme());
        String currentTheme = ThemeManager.currentPalette(this).id;
        WdRadio randomRadio = new WdRadio(this);
        randomRadio.setId(999);
        randomRadio.setText("Catppuccin Mocha (Random)");
        randomRadio.setTextColor(getResources().getColor(R.color.text));
        randomRadio.setChecked(randomMode);
        themeGroup.addView(randomRadio);
        randomRadio.setOnClickListener(v -> {
            prefs.setTheme(PrefTheme.CATPPUCCIN_MOCHA_RANDOM);
            ThemeManager.roll(Settings.this);
            ThemeManager.applyTheme(Settings.this, findViewById(R.id.root));
        });
        List<Themes.Palette> palettes = Themes.all();
        for (int i = 0; i < palettes.size(); i++) {
            final Themes.Palette palette = palettes.get(i);
            WdRadio radio = new WdRadio(this);
            radio.setId(1000 + i);
            radio.setText(palette.label);
            radio.setTextColor(getResources().getColor(R.color.text));
            radio.setChecked(!randomMode && palette.id.equals(currentTheme));
            themeGroup.addView(radio);
            radio.setOnClickListener(v -> {
                prefs.setTheme(palette.id);
                ThemeManager.applyTheme(Settings.this, findViewById(R.id.root));
            });
        }
    }

    private void renderGroups() {
        groupsContainer.removeAllViews();
        List<String> groupIds = groupManager.getGroupIds();
        for (int i = 0; i < groupIds.size(); i++) {
            addGroupRow(groupIds.get(i));
        }
        ThemeManager.applyTheme(this, groupsContainer);
    }

    private void setActionEnabled(View button, ImageView icon, boolean enabled) {
        button.setEnabled(enabled);
        icon.setTag(enabled ? null : "disabled");
        Themes.Palette palette = ThemeManager.currentPalette(this);
        icon.setColorFilter(enabled ? palette.text : palette.disabled, PorterDuff.Mode.SRC_IN);
    }

    private void addGroupRow(final String groupId) {
        View row = getLayoutInflater().inflate(R.layout.list_item_group, groupsContainer, false);
        FontManager.apply(row, FontManager.getJersey25(this));
        ZoomManager.apply(this, row);
        final EditText name = row.findViewById(R.id.group_name);
        View delete = row.findViewById(R.id.delete_group);
        View toggleVisibility = row.findViewById(R.id.toggle_visibility);
        final ImageView toggleVisibilityIcon = row.findViewById(R.id.toggle_visibility_img);
        View toggleKeepExpanded = row.findViewById(R.id.toggle_keep_expanded);
        final ImageView toggleKeepExpandedIcon = row.findViewById(R.id.toggle_keep_expanded_img);
        final View saveButton = row.findViewById(R.id.save_changes_button);
        final ImageView saveIcon = row.findViewById(R.id.save_changes_img);
        ImageView deleteIcon = row.findViewById(R.id.delete_group_img);
        View ascend = row.findViewById(R.id.ascend_group);
        View descend = row.findViewById(R.id.descend_group);
        name.setText(groupManager.getGroupLabel(groupId));
        setActionEnabled(saveButton, saveIcon, false);
        updateVisibilityIcon(toggleVisibilityIcon, groupId);
        updateKeepExpandedIcon(toggleKeepExpandedIcon, groupId);
        toggleVisibility.setOnClickListener(v -> {
            groupManager.toggleGroupVisible(groupId);
            updateVisibilityIcon(toggleVisibilityIcon, groupId);
        });
        toggleKeepExpanded.setOnClickListener(v -> {
            groupManager.toggleGroupKeepExpanded(groupId);
            updateKeepExpandedIcon(toggleKeepExpandedIcon, groupId);
            renderGroups();
        });
        final String originalText = name.getText().toString();
        name.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String current = s.toString();
                boolean changed = !current.equals(originalText) && current.trim().length() > 0;
                setActionEnabled(saveButton, saveIcon, changed);
            }

            public void afterTextChanged(Editable s) {
            }
        });

        saveButton.setOnClickListener(v -> {
            String newLabel = name.getText().toString().trim();
            if (newLabel.length() > 0) {
                groupManager.renameGroup(groupId, newLabel);
                Toast.makeText(Settings.this, R.string.toast_group_label_updated, Toast.LENGTH_SHORT).show();
                renderGroups();
            }
        });
        ascend.setOnClickListener(v -> {
            groupManager.moveGroup(groupId, -1);
            renderGroups();
        });
        descend.setOnClickListener(v -> {
            groupManager.moveGroup(groupId, 1);
            renderGroups();
        });
        boolean deletable = !PrefsManager.DEFAULT_GROUP_ID.equals(groupId);
        setActionEnabled(delete, deleteIcon, deletable);
        if (deletable) {
            delete.setOnClickListener(v -> showDeleteGroupDialog(groupId, name.getText().toString()));
        }
        groupsContainer.addView(row);
    }

    private void updateVisibilityIcon(ImageView icon, String groupId) {
        icon.setImageResource(groupManager.isGroupVisible(groupId) ? R.drawable.px_eye : R.drawable.px_eye_cross);
    }

    private void updateKeepExpandedIcon(ImageView icon, String groupId) {
        icon.setImageResource(groupManager.isGroupKeepExpanded(groupId) ? R.drawable.px_lock : R.drawable.px_lock_open);
    }

    private void showDeleteGroupDialog(final String groupId, String groupLabel) {
        DialogHelper.show(this, R.layout.dialog_groups_delete, (view, dialog) -> {
            ((TextView) view.findViewById(R.id.group_name)).setText(groupLabel);
            final WdRadioGroup radioGroup = view.findViewById(R.id.groups);
            final List<String> targetGroups = new ArrayList<>();
            List<String> allGroups = groupManager.getGroupIds();
            for (int i = 0; i < allGroups.size(); i++) {
                String targetId = allGroups.get(i);
                if (targetId.equals(groupId)) continue;
                targetGroups.add(targetId);
                WdRadio radio = radioGroup.addOption(groupManager.getGroupLabel(targetId));
                if (targetGroups.size() == 1) radio.setChecked(true);
            }
            view.findViewById(R.id.yes_button).setOnClickListener(v -> {
                int index = radioGroup.getCheckedIndex();
                if (index < 0) {
                    Toast.makeText(Settings.this, R.string.toast_select_target_group, Toast.LENGTH_SHORT).show();
                    return;
                }
                groupManager.deleteGroup(groupId, targetGroups.get(index));
                renderGroups();
                dialog.dismiss();
            });
        });
    }

}
