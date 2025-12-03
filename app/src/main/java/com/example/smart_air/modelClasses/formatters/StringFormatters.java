package com.example.smart_air.modelClasses.formatters;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    /* gets label for key */
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

    /** calendar day of week to the string day */
    public static String dayNameForCalendar(Calendar cal) {
        switch (cal.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "Monday";
            case Calendar.TUESDAY:
                return "Tuesday";
            case Calendar.WEDNESDAY:
                return "Wednesday";
            case Calendar.THURSDAY:
                return "Thursday";
            case Calendar.FRIDAY:
                return "Friday";
            case Calendar.SATURDAY:
                return "Saturday";
            case Calendar.SUNDAY:
            default:
                return "Sunday";
        }
    }

    /*Function returns day of week for scheduled days*/
    public static Set<DayOfWeek> extractScheduledDays(Map<String, Boolean> weeklySchedule) {
        Set<DayOfWeek> result = new HashSet<>();
        for (Map.Entry<String, Boolean> entry : weeklySchedule.entrySet()) {

            if (!Boolean.TRUE.equals(entry.getValue())) continue;

            String key = entry.getKey().trim().toLowerCase(Locale.US);

            DayOfWeek dow = null;

            switch (key) {
                case "mon":
                case "monday":
                    dow = DayOfWeek.MONDAY;
                    break;
                case "tue":
                case "tues":
                case "tuesday":
                    dow = DayOfWeek.TUESDAY;
                    break;
                case "wed":
                case "weds":
                case "wednesday":
                    dow = DayOfWeek.WEDNESDAY;
                    break;
                case "thu":
                case "thur":
                case "thurs":
                case "thursday":
                    dow = DayOfWeek.THURSDAY;
                    break;
                case "fri":
                case "friday":
                    dow = DayOfWeek.FRIDAY;
                    break;
                case "sat":
                case "saturday":
                    dow = DayOfWeek.SATURDAY;
                    break;
                case "sun":
                case "sunday":
                    dow = DayOfWeek.SUNDAY;
                    break;
            }

            if (dow != null) { result.add(dow); }
        }

        return result;
    }

}
