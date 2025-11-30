package com.example.smart_air.modelClasses.formatters;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StringFormatters {
    /*
     * pre: N/A
     * post: returns string of yesterday's date in YYYY-MM-DD format
     * */
    public static String getYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }
    /*
     * pre: N/A
     * post: returns string of today's date in YYYY-MM-DD format
     * */
    public static String getToday() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return fmt.format(new Date()); // today's date string
    }
}
