package com.rama.mako_zero.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class WdRadioGroup extends LinearLayout {
    private LinearLayout radioContainer;
    private int checkedId = -1;
    private int nextOptionId = 1;
    private boolean protectFromCheckedChange;
    private OnCheckedChangeListener listener;

    public WdRadioGroup(Context context) {
        super(context);
        init(context);
    }

    public WdRadioGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        radioContainer = new LinearLayout(context);
        radioContainer.setOrientation(VERTICAL);
        super.addView(radioContainer, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public WdRadio addOption(String text) {
        WdRadio radio = new WdRadio(getContext());
        radio.setId(nextOptionId++);
        radio.setText(text);
        addView(radio);
        return radio;
    }

    public int getCheckedIndex() {
        if (checkedId == -1) {
            return -1;
        }
        View checked = findViewById(checkedId);
        return checked == null ? -1 : radioContainer.indexOfChild(checked);
    }

    public void check(int id) {
        if (id == -1) {
            clearCheck();
            return;
        }
        View view = findViewById(id);
        if (!(view instanceof WdRadio)) {
            return;
        }
        WdRadio radio = (WdRadio) view;
        if (checkedId == id && radio.isChecked()) {
            return;
        }
        protectFromCheckedChange = true;
        if (checkedId != -1) {
            View oldView = findViewById(checkedId);
            if (oldView instanceof WdRadio) {
                ((WdRadio) oldView).setChecked(false);
            }
        }
        radio.setChecked(true);
        protectFromCheckedChange = false;
        setCheckedId(id);
    }

    public void clearCheck() {
        if (checkedId == -1) {
            return;
        }
        protectFromCheckedChange = true;
        View view = findViewById(checkedId);
        if (view instanceof WdRadio) {
            ((WdRadio) view).setChecked(false);
        }
        protectFromCheckedChange = false;
        setCheckedId(-1);
    }

    private void setCheckedId(int id) {
        if (checkedId == id) {
            return;
        }
        checkedId = id;
        if (listener != null) {
            listener.onCheckedChanged(this, checkedId);
        }
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (child == radioContainer) {
            super.addView(child, index, params);
            return;
        }
        if (!(child instanceof WdRadio)) {
            return;
        }
        final WdRadio radio = (WdRadio) child;
        radioContainer.addView(radio, params);
        radio.setInternalCheckedChangeListener((WdRadio internalRadioBtn, boolean checked) -> {
            if (protectFromCheckedChange) {
                return;
            }
            if (checked) {
                check(internalRadioBtn.getId());
            } else if (checkedId == internalRadioBtn.getId()) {
                protectFromCheckedChange = true;
                internalRadioBtn.setChecked(true);
                protectFromCheckedChange = false;
            }
        });
        if (radio.isChecked()) {
            check(radio.getId());
        }
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(WdRadioGroup group, int checkedId);
    }
}