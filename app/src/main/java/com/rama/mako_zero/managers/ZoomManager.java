package com.rama.mako_zero.managers;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.rama.mako_zero.R;

public final class ZoomManager {
    public static final int DEFAULT_PERCENT = 100;
    public static final int MIN_PERCENT = 50;
    public static final int MAX_PERCENT = 200;

    private ZoomManager() {
    }

    public static int clamp(int percent) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
    }

    public static int getPercent(Context context) {
        return clamp(PrefsManager.getInstance(context).getZoomPercent());
    }

    public static void apply(Context context, View view) {
        apply(view, getPercent(context) / 100f);
    }

    private static void apply(View view, float scale) {
        if (view instanceof TextView textView) {
            Object stored = textView.getTag(R.id.zoom_base_text_size);
            if (stored != null || scale != 1f) {
                float base;
                if (stored instanceof Float) {
                    base = (Float) stored;
                } else {
                    base = textView.getTextSize();
                    textView.setTag(R.id.zoom_base_text_size, base);
                }
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, base * scale);
            }
        }
        if (view instanceof ImageView || view.getClass() == FrameLayout.class) {
            scaleBox(view, scale);
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                apply(group.getChildAt(i), scale);
            }
        }
    }

    private static void scaleBox(View view, float scale) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null || (params.width <= 0 && params.height <= 0)) {
            return;
        }
        Object stored = view.getTag(R.id.zoom_base_box);
        int[] base;
        if (stored instanceof int[]) {
            base = (int[]) stored;
        } else {
            if (scale == 1f) {
                return;
            }
            base = new int[]{params.width, params.height, view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom()};
            view.setTag(R.id.zoom_base_box, base);
        }

        int width = base[0] > 0 ? scaled(base[0], scale) : params.width;
        int height = base[1] > 0 ? scaled(base[1], scale) : params.height;
        if (width != params.width || height != params.height) {
            params.width = width;
            params.height = height;
            view.setLayoutParams(params);
        }

        int left = scaled(base[2], scale);
        int top = scaled(base[3], scale);
        int right = scaled(base[4], scale);
        int bottom = scaled(base[5], scale);
        if (left != view.getPaddingLeft() || top != view.getPaddingTop() || right != view.getPaddingRight() || bottom != view.getPaddingBottom()) {
            view.setPadding(left, top, right, bottom);
        }
    }

    private static int scaled(int value, float scale) {
        if (value <= 0) return value;
        return Math.max(1, Math.round(value * scale));
    }
}
