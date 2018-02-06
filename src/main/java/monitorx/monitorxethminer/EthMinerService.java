package monitorx.monitorxethminer;

import monitorx.monitorxethminer.tail.LogTail;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author qianlifeng
 */
@Service
public class EthMinerService {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${wallet}")
    String walletAddress;

    @Value("${api.url}")
    String apiUrl;

    private Date lastTailDate;
    private Integer lastTailMh;

    public Date getLastTailDate() {
        return lastTailDate;
    }

    public void setLastTailDate(Date lastTailDate) {
        this.lastTailDate = lastTailDate;
    }

    public Integer getLastTailMh() {
        return lastTailMh;
    }

    public void setLastTailMh(Integer lastTailMh) {
        this.lastTailMh = lastTailMh;
    }

    public void run() {
        LogTail tailer = new LogTail(1000, apiUrl);
        tailer.add(msg -> {
            Integer mh = parseMh(msg);
            if (mh != null) {
                lastTailDate = new Date();
                lastTailMh = mh;
            }
        });
        new Thread(tailer).start();
    }

    private Integer parseMh(String line) {
        if (NumberUtils.isNumber(line)) {
            return Integer.valueOf(line) / 1000;
        }

        return null;
    }
}
