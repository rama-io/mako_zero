package com.rama.mako_zero.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rama.mako_zero.R;

public class WdLabel extends LinearLayout {
    private final ImageView iconImage;
    private final TextView iconText;

    public WdLabel(Context context) {
        this(context, null);
    }

    public WdLabel(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.wd_label, this, true);
        iconImage = findViewById(R.id.icon_image);
        iconText = findViewById(R.id.icon_text);
        if (attrs != null) {
            setAttrs(context, attrs);
        }
    }

    private void setAttrs(Context context, AttributeSet attrs) {
        for (int i = 0; i < attrs.getAttributeCount(); i++) {
            String name = attrs.getAttributeName(i);
            String value = attrs.getAttributeValue(i);
            if ("text".equals(name)) {
                int resId = attrs.getAttributeResourceValue(i, 0);
                if (resId != 0) {
                    iconText.setText(context.getString(resId));
                } else {
                    iconText.setText(value);
                }
            } else if ("icon".equals(name)) {
                int resId = attrs.getAttributeResourceValue(i, 0);

                if (resId != 0) {
                    iconImage.setImageResource(resId);
                }
            }
        }
    }
}