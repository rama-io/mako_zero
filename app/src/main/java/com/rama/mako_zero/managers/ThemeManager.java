package com.rama.mako_zero.managers;

import static com.rama.mako_zero.objects.Themes.getColor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.rama.mako_zero.R;
import com.rama.mako_zero.helpers.SystemBars;
import com.rama.mako_zero.objects.PrefTheme;
import com.rama.mako_zero.objects.Themes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ThemeManager {
    private static final Random RANDOM = new Random();

    private ThemeManager() {
    }

    public static Themes.Palette paletteFor(String themeId) {
        return Themes.byId(themeId);
    }

    public static Themes.Palette currentPalette(Context context) {
        PrefsManager prefs = PrefsManager.getInstance(context);
        String id = prefs.getTheme();
        if (PrefTheme.CATPPUCCIN_MOCHA_RANDOM.equals(id)) {
            id = prefs.getRolledThemeId();
        }
        return paletteFor(id);
    }

    public static void rollIfRandom(Context context) {
        if (PrefTheme.CATPPUCCIN_MOCHA_RANDOM.equals(PrefsManager.getInstance(context).getTheme())) {
            roll(context);
        }
    }

    public static void roll(Context context) {
        PrefsManager prefs = PrefsManager.getInstance(context);
        List<Themes.Palette> pool = Themes.rotationPool();
        String previous = prefs.getRolledThemeId();
        String next;
        do {
            next = pool.get(RANDOM.nextInt(pool.size())).id;
        } while (next.equals(previous) && pool.size() > 1);
        prefs.setRolledTheme(next);
    }

    public static void applyTheme(Context context, View root) {
        Themes.Palette palette = currentPalette(context);
        if (context instanceof Activity activity) {
            SystemBars.apply(activity, palette);
        }
        Map<Integer, Integer> colorMap = buildColorMap(context, palette);
        applyRecursively(root, palette, colorMap);
    }

    private static void applyRecursively(View view, Themes.Palette palette, Map<Integer, Integer> map) {
        applyToView(view, palette, map);
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                applyRecursively(group.getChildAt(i), palette, map);
            }
        }
    }

    private static void applyToView(View view, Themes.Palette palette, Map<Integer, Integer> map) {
        Object tag = view.getTag();
        int themeColor = palette.text;
        if (tag instanceof String) {
            themeColor = getColor(palette, (String) tag);
        }
        if (view instanceof AbsListView) {
            ((AbsListView) view).setSelector(buildSelector(palette));
        }
        if (view instanceof TextView textView) {
            if (tag instanceof String) {
                textView.setTextColor(themeColor);
            } else {
                Integer mapped = map.get(textView.getCurrentTextColor());
                if (mapped != null) {
                    textView.setTextColor(mapped);
                }
            }
        }
        if (view instanceof ImageView) {
            ((ImageView) view).setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
        }
        Drawable background = view.getBackground();
        if (background instanceof ColorDrawable) {
            int color = getColorDrawableColor((ColorDrawable) background);
            Integer mapped = map.get(color);

            if (mapped != null) {
                view.setBackgroundColor(mapped);
            }
        } else if (background != null) {
            background.mutate().setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
        }
    }

    private static Drawable buildSelector(Themes.Palette palette) {
        int highlight = withAlpha(palette.accent, 110);
        StateListDrawable selector = new StateListDrawable();
        selector.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(highlight));
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            selector.addState(new int[]{android.R.attr.state_focused}, new ColorDrawable(highlight));
        }
        selector.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        return selector;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static int getColorDrawableColor(ColorDrawable drawable) {
        try {
            return (Integer) ColorDrawable.class.getMethod("getColor").invoke(drawable);
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }

    private static Map<Integer, Integer> buildColorMap(Context context, Themes.Palette target) {
        Map<Integer, Integer> map = new HashMap<>();
        List<Themes.Palette> all = Themes.all();
        for (int i = 0; i < all.size(); i++) {
            Themes.Palette p = all.get(i);
            map.put(p.text, target.text);
            map.put(p.base, target.base);
            map.put(p.surface_0, target.surface_0);
            map.put(p.surface_1, target.surface_1);
            map.put(p.disabled, target.disabled);
            map.put(p.border, target.border);
            map.put(p.accent, target.accent);
            map.put(p.success, target.success);
            map.put(p.warning, target.warning);
            map.put(p.error, target.error);
        }
        android.content.res.Resources res = context.getResources();
        map.put(res.getColor(R.color.text), target.text);
        map.put(res.getColor(R.color.base), target.base);
        map.put(res.getColor(R.color.surface_0), target.surface_0);
        map.put(res.getColor(R.color.surface_1), target.surface_1);
        map.put(res.getColor(R.color.disabled), target.disabled);
        map.put(res.getColor(R.color.border), target.border);
        map.put(res.getColor(R.color.accent), target.accent);
        map.put(res.getColor(R.color.success), target.success);
        map.put(res.getColor(R.color.warning), target.warning);
        map.put(res.getColor(R.color.error), target.error);
        return map;
    }
}
