package com.rama.mako_zero.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rama.mako_zero.R;
import com.rama.mako_zero.managers.PrefsManager;

public class WdCollapsibleSection extends LinearLayout {
    private LinearLayout header;
    private TextView indicator;
    private TextView labelView;
    private LinearLayout content;
    private View contentSeparator;
    private String key;
    private boolean defaultExpanded = true;
    private final PrefsManager prefs;

    public WdCollapsibleSection(Context context) {
        this(context, null);
    }

    public WdCollapsibleSection(Context context, AttributeSet attrs) {
        super(context, attrs);
        prefs = PrefsManager.getInstance(context);
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.wd_collapsible_section, this, true);
        header = findViewById(R.id.section_header);
        indicator = findViewById(R.id.section_indicator);
        labelView = findViewById(R.id.section_label);
        content = findViewById(R.id.section_content);
        contentSeparator = findViewById(R.id.section_content_separator);
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.WdCollapsibleSection);
            String headerText = ta.getString(R.styleable.WdCollapsibleSection_header);
            if (headerText == null) {
                headerText = "";
            }
            labelView.setText(headerText);
            key = ta.getString(R.styleable.WdCollapsibleSection_key);
            defaultExpanded = ta.getBoolean(R.styleable.WdCollapsibleSection_defaultExpanded, true);
            ta.recycle();
        }
        applyState(loadState());
        header.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            boolean next = !isExpanded();
            applyState(next);
            saveState(next);
        });
        header.setFocusable(true);
        header.setFocusableInTouchMode(false);
        header.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                boolean next = !isExpanded();
                applyState(next);
                saveState(next);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child.getId() != R.id.section_root) {
                removeView(child);
                content.addView(child, 0);
            }
        }
    }

    private boolean isExpanded() {
        return content.getVisibility() == View.VISIBLE;
    }

    private void applyState(boolean expanded) {
        if (expanded) {
            content.setVisibility(View.VISIBLE);
            contentSeparator.setVisibility(View.VISIBLE);
            indicator.setText(R.string.label_collapse_indicator);
        } else {
            content.setVisibility(View.GONE);
            contentSeparator.setVisibility(View.GONE);
            indicator.setText(R.string.label_expand_indicator);
        }
    }

    private void saveState(boolean expanded) {
        if (key != null) {
            prefs.setBoolean(key, expanded);
        }
    }

    private boolean loadState() {
        if (key != null) {
            return prefs.getBoolean(key, defaultExpanded);
        }
        return defaultExpanded;
    }
}