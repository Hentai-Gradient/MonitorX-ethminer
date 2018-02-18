package monitorx.monitorxethminer.utils;

import java.math.BigDecimal;

/**
 * @author xma
 */
public class NumberUtil {

    /**
     * 把字符串转化成Integer类型
     * 如果输入参数是 null值或空值，返回null
     *
     * @param str
     * @return
     */
    public static Integer parseStringToInteger(String str) {
        if (null == str || "".equals(str.trim())) {
            return null;
        }
        return Integer.parseInt(str);
    }

    /**
     * 把字符串转化成Double类型
     * 如果输入参数是 null值或空值，返回null
     *
     * @param str
     * @return
     */
    public static Double parseStringToDouble(String str) {
        if (null == str || "".equals(str.trim())) {
            return null;
        }
        return Double.parseDouble(str);
    }

    /**
     * 把null转换成 0
     *
     * @param number
     * @return
     */
    public static Double parseNullToZero(Double number) {
        if (null == number) {
            return 0d;
        }
        return number;
    }

    /**
     * 把null转换成 0
     *
     * @param number
     * @return
     */
    public static Integer parseNullToZero(Integer number) {
        if (null == number) {
            return 0;
        }
        return number;
    }

    public static Double roundUpFormatDouble(Double num) {
        return roundUpFormatDouble(num, 4);
    }

    public static Double roundUpFormatDouble(Double num, Integer scale) {
        BigDecimal bd = new BigDecimal(num);
        return bd.setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static String formatMoney(Double money) {
        if (null == money) {
            money = 0.0;
        }
        return String.format("%3.2f", money);
    }

    public static String formatMoney(Double money, int floatNbr) {
        if (null == money) {
            money = 0.0;
        }
        return String.format("%3." + floatNbr + "f", money);
    }
}