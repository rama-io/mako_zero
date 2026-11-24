package com.rama.mako_zero.helpers;

import android.view.HapticFeedbackConstants;
import android.view.View;

public final class Haptics {
    private Haptics() {
    }

    public static void tap(View view) {
        if (view != null) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    public static void longPress(View view) {
        if (view != null) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    public static View.OnClickListener onClick(final View.OnClickListener listener) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tap(v);
                listener.onClick(v);
            }
        };
    }

    public static View.OnLongClickListener onLongClick(final View.OnLongClickListener listener) {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                longPress(v);
                return listener.onLongClick(v);
            }
        };
    }
}
