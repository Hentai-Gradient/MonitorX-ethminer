package monitorx.monitorxethminer.utils;

import java.math.BigDecimal;

/**
 * @author xma
 */
public class NumberUtil {
    public static Double roundUpFormatDouble(Double num, Integer scale) {
        BigDecimal bd = new BigDecimal(num);
        return bd.setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}
