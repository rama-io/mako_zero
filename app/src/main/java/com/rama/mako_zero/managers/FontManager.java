package com.rama.mako_zero.managers;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private static Typeface jersey25;
    private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 " + "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~¡¿«»€·";

    private static final Map<Character, String> SPECIAL_LETTERS = buildSpecialLetters();

    private static Map<Character, String> buildSpecialLetters() {
        Map<Character, String> map = new HashMap<>();
        map.put('\u00C6', "AE"); // Æ
        map.put('\u00E6', "ae"); // æ
        map.put('\u0152', "OE"); // Œ
        map.put('\u0153', "oe"); // œ
        map.put('\u00D8', "O");  // Ø
        map.put('\u00F8', "o");  // ø
        map.put('\u00D0', "D");  // Ð
        map.put('\u00F0', "d");  // ð
        map.put('\u00DE', "Th"); // Þ
        map.put('\u00FE', "th"); // þ
        map.put('\u00DF', "ss"); // ß
        map.put('\u1E9E', "SS"); // ẞ
        map.put('\u0141', "L");  // Ł
        map.put('\u0142', "l");  // ł
        map.put('\u0110', "D");  // Đ
        map.put('\u0111', "d");  // đ
        map.put('\u014A', "N");  // Ŋ
        map.put('\u014B', "n");  // ŋ
        map.put('\u0126', "H");  // Ħ
        map.put('\u0127', "h");  // ħ
        map.put('\u0130', "I");  // İ
        map.put('\u0131', "i");  // ı
        return map;
    }

    public static Typeface getJersey25(Context context) {
        if (jersey25 == null) {
            try {
                jersey25 = Typeface.createFromAsset(context.getApplicationContext().getAssets(), "fonts/jersey25_regular.otf");
            } catch (Exception e) {
                jersey25 = Typeface.DEFAULT;
            }
        }
        return jersey25;
    }

    public static void apply(View view, Typeface typeface) {
        if (view instanceof TextView) {
            ((TextView) view).setTypeface(typeface);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                apply(group.getChildAt(i), typeface);
            }
        }
    }

    public static String sanitizeForFont(String text) {
        if (text == null) return null;
        return sanitize(text, Build.VERSION.SDK_INT >= 9);
    }

    static String sanitize(String text, boolean canDecompose) {
        StringBuilder mapped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String special = SPECIAL_LETTERS.get(c);
            if (special != null) {
                mapped.append(special);
            } else {
                mapped.append(c);
            }
        }
        String withoutMarks = mapped.toString();
        if (canDecompose) {
            withoutMarks = Normalizer.normalize(withoutMarks, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        }
        StringBuilder result = new StringBuilder(withoutMarks.length());
        for (int i = 0; i < withoutMarks.length(); i++) {
            char c = withoutMarks.charAt(i);
            result.append(ALLOWED_CHARS.indexOf(c) >= 0 ? c : '?');
        }
        return result.toString();
    }
}
