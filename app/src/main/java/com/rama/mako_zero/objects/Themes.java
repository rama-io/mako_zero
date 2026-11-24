package com.rama.mako_zero.objects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Themes {
    public static final class Palette {
        public final String id;
        public final String label;
        public final int text;
        public final int base;
        public final int surface_0;
        public final int surface_1;
        public final int disabled;
        public final int border;
        public final int accent;
        public final int success;
        public final int warning;
        public final int error;

        Palette(String id, String label, int text, int base, int surface_0, int surface_1, int disabled, int border, int accent, int success, int warning, int error) {
            this.id = id;
            this.label = label;
            this.text = text;
            this.base = base;
            this.surface_0 = surface_0;
            this.surface_1 = surface_1;
            this.disabled = disabled;
            this.border = border;
            this.accent = accent;
            this.success = success;
            this.warning = warning;
            this.error = error;
        }
    }

    private Themes() {
    }

    /** A base color scheme. Catppuccin flavors carry the 14 accents, the mono ones are a single fixed palette. */
    public static final class Flavor {
        public final String id;
        public final String label;
        final int text, base, surface0, surface1, disabled, border;
        final int[] accents;
        final Palette fixed;

        Flavor(String id, String label, Palette fixed) {
            this.id = id;
            this.label = label;
            this.fixed = fixed;
            this.text = this.base = this.surface0 = this.surface1 = this.disabled = this.border = 0;
            this.accents = null;
        }

        Flavor(String id, String label, int text, int base, int surface0, int surface1, int disabled, int border, int[] accents) {
            this.fixed = null;
            this.id = id;
            this.label = label;
            this.text = text;
            this.base = base;
            this.surface0 = surface0;
            this.surface1 = surface1;
            this.disabled = disabled;
            this.border = border;
            this.accents = accents;
        }
    }

    public static final class Accent {
        public final String id;
        public final String label;

        Accent(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    public static final String RANDOM = "random";
    public static final String DEFAULT_FLAVOR = "mocha";
    public static final String DEFAULT_ACCENT_COLOR = "mauve";

    private static final String[] ACCENT_IDS = {"rosewater", "flamingo", "pink", "mauve", "red", "maroon", "peach", "yellow", "green", "teal", "sky", "sapphire", "blue", "lavender"};
    private static final String[] ACCENT_LABELS = {"Rosewater", "Flamingo", "Pink", "Mauve", "Red", "Maroon", "Peach", "Yellow", "Green", "Teal", "Sky", "Sapphire", "Blue", "Lavender"};
    private static final int RED = 4, GREEN = 8, YELLOW = 7;

    private static final Flavor[] FLAVORS = {
            new Flavor("mocha", "Catppuccin Mocha",
                    0xFFCDD6F4, 0xFF1E1E2E, 0xFF45475A, 0xFF585B71, 0xFF6C7086, 0xFF6C7088,
                    new int[]{0xFFF5E0DC, 0xFFF2CDCD, 0xFFF5C2E7, 0xFFCBA6F7, 0xFFF38BA8, 0xFFEBA0AC, 0xFFFAB387, 0xFFF9E2AF, 0xFFA6E3A1, 0xFF94E2D5, 0xFF89DCEB, 0xFF74C7EC, 0xFF89B4FA, 0xFFB4BEFE}),
            new Flavor("macchiato", "Catppuccin Macchiato",
                    0xFFCAD3F5, 0xFF24273A, 0xFF494D64, 0xFF5B6079, 0xFF6E738D, 0xFF6E738F,
                    new int[]{0xFFF4DBD6, 0xFFF0C6C6, 0xFFF5BDE6, 0xFFC6A0F6, 0xFFED8796, 0xFFEE99A0, 0xFFF5A97F, 0xFFEED49F, 0xFFA6DA95, 0xFF8BD5CA, 0xFF91D7E3, 0xFF7DC4E4, 0xFF8AADF4, 0xFFB7BDF8}),
            new Flavor("frappe", "Catppuccin Frappe",
                    0xFFC6D0F5, 0xFF303446, 0xFF51576D, 0xFF626881, 0xFF737994, 0xFF737996,
                    new int[]{0xFFF2D5CF, 0xFFEEBEBE, 0xFFF4B8E4, 0xFFCA9EE6, 0xFFE78284, 0xFFEA999C, 0xFFEF9F76, 0xFFE5C890, 0xFFA6D189, 0xFF81C8BE, 0xFF99D1DB, 0xFF85C1DC, 0xFF8CAAEE, 0xFFBABBF1}),
            new Flavor("latte", "Catppuccin Latte",
                    0xFF4C4F69, 0xFFEFF1F5, 0xFFCCD0DA, 0xFFBCC0CC, 0xFF9CA0B1, 0xFF9CA0B2,
                    new int[]{0xFFDC8A78, 0xFFDD7878, 0xFFEA76CB, 0xFF8839EF, 0xFFD20F39, 0xFFE64553, 0xFFFE640B, 0xFFDF8E1D, 0xFF40A02B, 0xFF179299, 0xFF04A5E5, 0xFF209FB5, 0xFF1E66F5, 0xFF7287FD}),
            new Flavor("mono_dark", "Mono Dark", new Palette("mono_dark", "Mono Dark",
                    0xFFFFFFF9, 0xFF040100, 0xFF040101, 0xFF040200, 0xFF555555, 0xFF555556,
                    0xFFFFF9F9, 0xFFFBFFFF, 0xFFFBFEFF, 0xFFFBFFFB)),
            new Flavor("mono_light", "Mono Light", new Palette("mono_light", "Mono Light",
                    0xFF000040, 0xFFFFFFFF, 0xFFFFFEFF, 0xFFFEFFFE, 0xFFAAAAAA, 0xFFAAAAAC,
                    0xFF020000, 0xFF000001, 0xFF010000, 0xFF020002)),
    };

    private static final Map<String, Palette> PALETTES = new LinkedHashMap<String, Palette>();

    static {
        for (Flavor flavor : FLAVORS) {
            if (flavor.fixed != null) {
                PALETTES.put(flavor.id, flavor.fixed);
                continue;
            }
            for (int i = 0; i < ACCENT_IDS.length; i++) {
                // The semantic colors are nudged by one blue step so they never equal an accent,
                // ThemeManager maps colors by value and two roles sharing a value would clash.
                Palette p = new Palette(key(flavor.id, ACCENT_IDS[i]), flavor.label + " " + ACCENT_LABELS[i],
                        flavor.text, flavor.base, flavor.surface0, flavor.surface1, flavor.disabled, flavor.border,
                        flavor.accents[i],
                        shiftBlue(flavor.accents[GREEN], 1), shiftBlue(flavor.accents[YELLOW], -1), shiftBlue(flavor.accents[RED], 1));
                PALETTES.put(p.id, p);
            }
        }
    }

    private static String key(String flavorId, String accentId) {
        return flavorId + ":" + accentId;
    }

    private static int shiftBlue(int color, int delta) {
        int b = Math.max(0, Math.min(255, (color & 0xFF) + delta));
        return (color & 0xFFFFFF00) | b;
    }

    public static int getColor(Palette palette, String name) {
        switch (name) {
            case "text":
                return palette.text;
            case "base":
                return palette.base;
            case "surface_0":
                return palette.surface_0;
            case "surface_1":
                return palette.surface_1;
            case "disabled":
                return palette.disabled;
            case "border":
                return palette.border;
            case "accent":
                return palette.accent;
            case "success":
                return palette.success;
            case "warning":
                return palette.warning;
            case "error":
                return palette.error;
            default:
                return palette.text;
        }
    }

    public static List<Flavor> flavors() {
        List<Flavor> list = new ArrayList<Flavor>();
        for (Flavor f : FLAVORS) list.add(f);
        return list;
    }

    public static List<Accent> accents() {
        List<Accent> list = new ArrayList<Accent>();
        for (int i = 0; i < ACCENT_IDS.length; i++)
            list.add(new Accent(ACCENT_IDS[i], ACCENT_LABELS[i]));
        return list;
    }

    public static boolean isFlavor(String id) {
        for (Flavor f : FLAVORS) if (f.id.equals(id)) return true;
        return false;
    }

    public static boolean isAccent(String id) {
        for (String a : ACCENT_IDS) if (a.equals(id)) return true;
        return false;
    }

    public static List<String> accentIds() {
        List<String> list = new ArrayList<String>();
        for (String a : ACCENT_IDS) list.add(a);
        return list;
    }

    public static boolean hasAccents(String flavorId) {
        for (Flavor f : FLAVORS) if (f.id.equals(flavorId)) return f.fixed == null;
        return true;
    }

    public static Palette palette(String flavorId, String accentId) {
        Palette p = hasAccents(flavorId) ? PALETTES.get(key(flavorId, accentId)) : PALETTES.get(flavorId);
        return p != null ? p : PALETTES.get(key(DEFAULT_FLAVOR, DEFAULT_ACCENT_COLOR));
    }

    public static List<Palette> all() {
        return new ArrayList<Palette>(PALETTES.values());
    }
}
