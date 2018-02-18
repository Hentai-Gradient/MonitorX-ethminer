package monitorx.monitorxethminer.utils;

import org.apache.commons.lang3.StringUtils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author xma
 */
public class DateUtil {
    public static final String STANDARD_TIME_FORMAT = "HH:mm:ss";
    public static final String STANDARD_DATE_FORMAT = "yyyy-MM-dd";
    public static final String STANDARD_DATE_FORMAT2 = "yyyyMMdd";

    public static Date currentDate() {
        return new Date();
    }

    public static String getStringDate(Date date, String format) {
        DateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    public static String getCurrStrDate(String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        Date currDate = new Date();
        return formatter.format(currDate);
    }

    public static Date parseDateString(String date, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.parse(date);
    }

    public static String getStringDate(Date date) {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    public static String getStringDate(Timestamp date) {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    public static String getStringDate(Timestamp date, String format) {
        DateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    public static String getCurrentDate() {
        return getStringDate(currentDate(), "yyyy-MM-dd HH:mm:ss");
    }

    public static String getTodayDate() {
        return getStringDate(currentDate(), "yyyy-MM-dd");
    }

    public static Timestamp parseStandardTimestampString(String s) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return new Timestamp(sdf.parse(s).getTime());
    }

    public static Date parseStandardDateString(String s) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return new Date(sdf.parse(s).getTime());
    }

    public static Date parseStandardDateStringOrNull(String s) throws ParseException {
        if (StringUtils.isEmpty(s)) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return new Date(sdf.parse(s).getTime());
    }

    public static Date parseStandardDateString(String s, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return new Date(sdf.parse(s).getTime());
    }

    public static Timestamp currenTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    public static Date addDays(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

    public static Date addDays(Date date, int days) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

    public static Date addMinutes(Date date, int minutes) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MINUTE, minutes);
        return cal.getTime();
    }

    public static Date addSecond(Date date, int seconds) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.SECOND, seconds);
        return cal.getTime();
    }

    public static Date addHours(Date date, int hours) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.HOUR_OF_DAY, hours);
        return cal.getTime();
    }

    public static Boolean betweenMoreThanMinutes(Date date1, Date date2, int minutes) {
        return Math.abs(date1.getTime() - date2.getTime()) > minutes * 1000 * 60;
    }

    /**
     * If ts is null, return "-"
     *
     * @param ts
     * @return
     */
    public static String tsToStandardDateStr(Timestamp ts) {
        if (ts == null) {
            return "-";
        }
        SimpleDateFormat format = new SimpleDateFormat(STANDARD_DATE_FORMAT);
        return format.format(ts);
    }

    /**
     * If ts is null, return "-"
     *
     * @param ts
     * @return
     */
    public static String tsToStandardDateStr2(Timestamp ts) {
        if (ts == null) {
            return "-";
        }
        SimpleDateFormat format = new SimpleDateFormat(STANDARD_DATE_FORMAT2);
        return format.format(ts);
    }
}