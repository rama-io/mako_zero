package com.rama.mako_zero.helpers;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.rama.mako_zero.objects.Themes;

public final class SystemBars {
    private SystemBars() {
    }

    @SuppressWarnings("deprecation")
    public static void apply(Activity activity, Themes.Palette palette) {
        Window window = activity.getWindow();
        int base = palette.base;

        window.setBackgroundDrawable(new ColorDrawable(base));

        int sdk = Build.VERSION.SDK_INT;
        if (sdk < 21) return;

        boolean lightBg = isLight(base);

        int statusColor = (lightBg && sdk < 23) ? scrim(base) : Color.TRANSPARENT;
        int navColor = (lightBg && sdk < 26) ? scrim(base) : Color.TRANSPARENT;

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(statusColor);
        window.setNavigationBarColor(navColor);

        if (sdk >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        if (sdk >= 30) {
            window.setDecorFitsSystemWindows(false);
        }

        View decor = window.getDecorView();
        int flags = decor.getSystemUiVisibility();
        if (sdk < 30) {
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }
        if (sdk >= 23) {
            flags = lightBg ? flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (sdk >= 26) {
            flags = lightBg ? flags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : flags & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        decor.setSystemUiVisibility(flags);

        if (sdk >= 30) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(lightBg ? mask : 0, mask);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static void applyInsets(final View root) {
        if (Build.VERSION.SDK_INT < 21) return;

        final int padLeft = root.getPaddingLeft();
        final int padTop = root.getPaddingTop();
        final int padRight = root.getPaddingRight();
        final int padBottom = root.getPaddingBottom();

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets in = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                left = in.left;
                top = in.top;
                right = in.right;
                bottom = in.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(padLeft + left, padTop + top, padRight + right, padBottom + bottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    private static boolean isLight(int color) {
        double luminance = 0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color);
        return luminance > 128;
    }

    private static int scrim(int color) {
        float f = 0.35f;
        return Color.rgb((int) (Color.red(color) * f), (int) (Color.green(color) * f), (int) (Color.blue(color) * f));
    }
}
