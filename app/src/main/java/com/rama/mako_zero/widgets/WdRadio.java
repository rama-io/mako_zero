package com.rama.mako_zero.widgets;

import android.content.Context;
import android.util.AttributeSet;

import com.rama.mako_zero.R;

public class WdRadio extends WdCompound {
    private InternalOnCheckedChangeListener internalListener;

    public WdRadio(Context context) {
        this(context, null);
    }

    public WdRadio(Context context, AttributeSet attrs) {
        super(context, attrs, R.layout.wd_radio, false, "android.widget.RadioButton");
    }

    void setInternalCheckedChangeListener(InternalOnCheckedChangeListener listener) {
        this.internalListener = listener;
    }

    @Override
    void onCheckedChanged(boolean checked) {
        if (internalListener != null) {
            internalListener.onCheckedChanged(this, checked);
        }
    }

    interface InternalOnCheckedChangeListener {
        void onCheckedChanged(WdRadio radio, boolean checked);
    }
}
