package com.rama.mako_zero.managers;

import android.os.Handler;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ClockManager {
    private static final long MINUTE_MS = 60000;
    private final TextView timeView;
    private final TextView dateView;
    private final Handler handler = new Handler();
    private Locale locale;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    private String[] weekdays;
    private final Runnable tick = new Runnable() {
        public void run() {
            update();
            handler.postDelayed(this, delayToNextMinute(System.currentTimeMillis()));
        }
    };

    public ClockManager(TextView timeView, TextView dateView) {
        this.timeView = timeView;
        this.dateView = dateView;
    }

    public void start() {
        locale = timeView.getResources().getConfiguration().locale;
        timeFormat = new SimpleDateFormat("HH:mm", locale);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", locale);
        weekdays = new DateFormatSymbols(locale).getWeekdays();
        handler.post(tick);
    }

    public void stop() {
        handler.removeCallbacks(tick);
    }

    static long delayToNextMinute(long nowMs) {
        return MINUTE_MS - nowMs % MINUTE_MS;
    }

    private void update() {
        Calendar calendar = Calendar.getInstance();
        TimeZone zone = calendar.getTimeZone();
        timeFormat.setTimeZone(zone);
        dateFormat.setTimeZone(zone);
        Date now = calendar.getTime();
        timeView.setText(timeFormat.format(now));
        String weekday = weekdays[calendar.get(Calendar.DAY_OF_WEEK)];
        String date = dateFormat.format(now);
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        int totalDays = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
        String weekOfYear = "w-" + calendar.get(Calendar.WEEK_OF_YEAR);
        String yearDay = dayOfYear + "/" + totalDays;
        String line = weekday + " :: " + date + " :: " + weekOfYear + " :: " + yearDay;
        dateView.setText(line.toUpperCase(locale));
    }
}
