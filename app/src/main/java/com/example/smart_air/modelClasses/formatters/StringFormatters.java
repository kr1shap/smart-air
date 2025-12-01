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

    /*
    to format date into string
     */
    public static String datestring(int day, int month, int year) { return formatmonth(month) + " " + day + " " + year; }

        /*
    converts month number to string
     */
    public static String formatmonth(int month)  {
        if(month == 1) return "Jan";
        if(month == 2) return "Feb";
        if(month == 3) return "Mar";
        if(month == 4) return "Apr";
        if(month == 5) return "May";
        if(month == 6) return "Jun";
        if(month == 7) return "Jul";
        if(month == 8) return "Aug";
        if(month == 9) return "Sep";
        if(month == 10) return "Oct";
        if(month == 11) return "Nov";
        if(month == 12) return "Dec";
        return "Jan"; // default, not reached
    }

    public static String getLabelForKey(String key) {
        switch (key) {
            case "rescue":
                return "Rescue Logs";
            case "controller":
                return "Controller Summary";
            case "pef":
                return "PEF Trend";
            case "charts":
                return "Charts";
            case "symptoms":
                return "Symptoms";
            case "triggers":
                return "Triggers";
            case "triage":
                return "Triage Incidents";
            default:
                return key; // fallback
        }
    }
}
