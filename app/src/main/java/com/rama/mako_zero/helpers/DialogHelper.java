package com.rama.mako_zero.helpers;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;

import com.rama.mako_zero.R;
import com.rama.mako_zero.managers.FontManager;
import com.rama.mako_zero.managers.ThemeManager;
import com.rama.mako_zero.managers.ZoomManager;

public final class DialogHelper {
    private DialogHelper() {
        // Utility class
    }

    public static Dialog show(Activity activity, int layoutResId, DialogContent content) {
        View view = LayoutInflater.from(activity).inflate(layoutResId, null);
        final Dialog dialog = new Dialog(activity, R.style.AppDialog);
        dialog.setContentView(view);
        View cancel = view.findViewById(R.id.cancel_button);
        if (cancel != null) {
            cancel.setOnClickListener(v -> dialog.dismiss());
        }
        if (content != null) {
            content.setup(view, dialog);
        }
        FontManager.apply(view, FontManager.getJersey25(activity));
        ThemeManager.applyTheme(activity, view);
        ZoomManager.apply(activity, view);
        dialog.show();
        return dialog;
    }

    public interface DialogContent {
        void setup(View view, Dialog dialog);
    }
}