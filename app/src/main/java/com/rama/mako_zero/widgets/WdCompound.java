package com.rama.mako_zero.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rama.mako_zero.R;

public abstract class WdCompound extends LinearLayout {
    private final ImageView check;
    private final TextView textView;
    private final boolean canUncheck;
    private final String accessibilityClassName;
    private boolean checked;
    private OnCheckedChangeListener listener;

    WdCompound(Context context, AttributeSet attrs, int layoutResId, boolean canUncheck, String accessibilityClassName) {
        super(context, attrs);
        this.canUncheck = canUncheck;
        this.accessibilityClassName = accessibilityClassName;
        inflate(context, layoutResId, this);
        check = findViewById(R.id.check);
        textView = findViewById(R.id.text);
        setClickable(true);
        setFocusable(true);
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.text, android.R.attr.checked});
            CharSequence text = ta.getText(0);
            if (text != null) {
                textView.setText(text);
            }
            checked = ta.getBoolean(1, false);
            ta.recycle();
        }
        updateCheck();
    }

    public void setText(String text) {
        textView.setText(text);
        updateAccessibility();
    }

    public String getText() {
        return textView.getText().toString();
    }

    public void setTextColor(int color) {
        textView.setTextColor(color);
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        if (this.checked == checked) {
            return;
        }
        this.checked = checked;
        updateCheck();
        updateAccessibility();
        if (listener != null) {
            listener.onCheckedChanged(checked);
        }
        onCheckedChanged(checked);
    }

    void onCheckedChanged(boolean checked) {
    }

    public void toggle() {
        if (!isEnabled() || (checked && !canUncheck)) {
            return;
        }
        setChecked(!checked);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    private void updateCheck() {
        check.setVisibility(checked ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateCheck();
        textView.setEnabled(enabled);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        toggle();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                return true;
            case MotionEvent.ACTION_UP:
                setPressed(false);
                if (isInside(event.getX(), event.getY())) {
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                return true;
        }
        return true;
    }

    private boolean isInside(float x, float y) {
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!isEnabled()) {
            return false;
        }
        if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            performClick();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(accessibilityClassName);
        info.setCheckable(true);
        info.setChecked(checked);
        info.setClickable(isEnabled());
        info.setEnabled(isEnabled());
        CharSequence label = textView.getText();
        if (label != null && label.length() > 0) {
            info.setText(label);
        }
    }

    private void updateAccessibility() {
        if (Build.VERSION.SDK_INT >= 14) {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        }
        invalidate();
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(boolean isChecked);
    }
}
