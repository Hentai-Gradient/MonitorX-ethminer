package monitorx.monitorxethminer;

import monitorx.monitorxethminer.tail.LogTail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private Pattern pattern = Pattern.compile("Total Speed.* Mh\\/s");

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
        //remove ascii color
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String val = matcher.group(0).replaceAll("Total Speed: ", "").replaceAll(" Mh/s", "");
            return new BigDecimal(val).intValue();
        }

        return null;
    }
}
