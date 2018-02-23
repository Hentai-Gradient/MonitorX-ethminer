package monitorx.monitorxethminer.utils;

public class ThreadSleepUtil {
    public static void sleep(Long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
        }
    }
}