package monitorx.monitorxethminer;

import com.alibaba.fastjson.JSON;
import monitorx.monitorxethminer.statusReport.Hashrate;
import monitorx.monitorxethminer.statusReport.Metric;
import monitorx.monitorxethminer.statusReport.NodeStatus;
import monitorx.monitorxethminer.statusReport.NodeStatusUpload;
import monitorx.monitorxethminer.utils.DateUtil;
import monitorx.monitorxethminer.utils.HTTPUtil;
import monitorx.monitorxethminer.utils.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author qianlifeng
 */
@Component
public class ReportSchedule {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${code}")
    private String code;

    @Value("${url}")
    private String url;

    @Value("${log}")
    private String logFile;

    @Scheduled(fixedDelay = 3000)
    public void report() {
        upload();
    }

    private void upload() {
        try {
            NodeStatusUpload statusUpload = new NodeStatusUpload();
            NodeStatus status = new NodeStatus();
            statusUpload.setNodeCode(code);
            statusUpload.setNodeStatus(status);

            status.setStatus("up");
            List<Metric> metrics = new ArrayList<>();
            status.setMetrics(metrics);

            Metric acceptMetric = new Metric();
            acceptMetric.setTitle("是否有接受请求");
            acceptMetric.setType("number");
            if (isWorking) {
                acceptMetric.setValue("100");
            } else {
                acceptMetric.setValue("0");
            }
            metrics.add(acceptMetric);

            Metric hashrateMetric = new Metric();
            hashrateMetric.setTitle("GPU信息");
            hashrateMetric.setType("text");
            StringBuilder sb = new StringBuilder();
            List<String> keySet = new ArrayList<>(hashRateMap.keySet());
            keySet.sort(String::compareTo);
            sb.append("<table class='table table-bordered table-condensed'>");
            sb.append("     <tr>");
            sb.append("         <th width='60'>矿机号</th>");
            sb.append("         <th>算力Mh/s</th>");
            sb.append("         <th>报告时间</th>");
            sb.append("     </tr>");
            for (String key : keySet) {
                sb.append("     <tr>");
                sb.append("         <td>").append(key).append("</td>");
                sb.append("         <td>").append(NumberUtil.roundUpFormatDouble(hashRateMap.get(key).getHashrate(), 2)).append("</td>");
                sb.append("         <td>").append(DateUtil.getStringDate(hashRateMap.get(key).getReportTime())).append("</td>");
                sb.append("     </tr>");
            }
            sb.append("</table>");
            hashrateMetric.setValue(sb.toString());
            hashrateMetric.setContext(JSON.toJSONString(hashRateMap));
            metrics.add(hashrateMetric);

            Metric lagMetric = new Metric();
            lagMetric.setTitle("当前平均延迟");
            lagMetric.setType("number");
            if (!lagCount.equals(0L)) {
                lagMetric.setValue(String.valueOf(allLag / lagCount));
            } else {
                lagMetric.setValue("9999");
            }
            metrics.add(lagMetric);

            logger.info("reporting to monitorx, url={}, code={}", url, code);
            HTTPUtil.sendBodyPost(url, JSON.toJSONString(statusUpload));
        } catch (IOException e) {
            logger.error("upload to monitorx error: " + e.getMessage(), e);
        }
    }

    private long filePointer = 0;
    private List<String> fileInfo = new ArrayList<>();
    private HashMap<String, Hashrate> hashRateMap = new HashMap<>();
    private static Pattern preCommitPattern = Pattern.compile("INFO proxy # MAIN eth_submitWork.*by");
    private static Pattern acceptPattern = Pattern.compile("INFO protocol # .*from.*accepted");
    private static Pattern hashratePattern = Pattern.compile("INFO proxy # Hashrate for (.*) ");
    private static Pattern timePattern = Pattern.compile("^[1-9]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1]) (20|21|22|23|[0-1]\\d):[0-5]\\d:[0-5]\\d");
    private static Pattern lagPattern = Pattern.compile("\\[(\\d*)ms\\]");

    private Long lagCount = 0L;
    private Long allLag = 0L;

    private Boolean havePreCommit = false;
    private Boolean haveAccept = false;
    private Boolean isWorking = false;

    @Scheduled(fixedDelay = 1000 * 60 * 30)
    private void deleteOldHashRate() {
        HashMap<String, Hashrate> newHash = new HashMap<>();
        for (String key : hashRateMap.keySet()) {
            if (!DateUtil.betweenMoreThanMinutes(hashRateMap.get(key).getReportTime(), new Date(), 60)) {
                newHash.put(key, hashRateMap.get(key));
            }
        }

        hashRateMap.clear();
        newHash.forEach((k, v) -> hashRateMap.put(k, v));
    }

    @Scheduled(fixedDelay = 1000 * 30)
    private void getFileInfo() {
        // 初始化数据
        havePreCommit = false;
        haveAccept = false;
        isWorking = false;
        fileInfo = new ArrayList<>();
        File logfile = new File(logFile);
        try {
            // 创建随机读写文件
            RandomAccessFile file = new RandomAccessFile(logfile, "r");
            long fileLength = logfile.length();
            if (fileLength < filePointer) {
                file = new RandomAccessFile(logfile, "r");
                filePointer = 0;
            }
            if (fileLength > filePointer) {
                file.seek(filePointer);
                String line = file.readLine();
                while (line != null) {
                    fileInfo.add(line);
                    line = file.readLine();
                }
                filePointer = file.getFilePointer();
            }
            file.close();

            for (String str : fileInfo) {
                Matcher matcher = preCommitPattern.matcher(str);
                if (matcher.find()) {
                    havePreCommit = true;
                    break;
                }
            }

            if (havePreCommit) {
                for (String str : fileInfo) {
                    Matcher matcher = acceptPattern.matcher(str);
                    if (matcher.find()) {
                        haveAccept = true;
                        break;
                    }
                }
            }

            isWorking = havePreCommit.equals(haveAccept);

            for (String str : fileInfo) {
                Matcher matcher = hashratePattern.matcher(str);
                if (matcher.find()) {
                    String rateInfo = matcher.group(1);
                    String[] strArray = rateInfo.split("is");
                    String rigName = strArray[0].trim();
                    Double hashrate = Double.valueOf(strArray[1].trim());
                    Matcher timeMatcher = timePattern.matcher(str);
                    if (timeMatcher.find()) {
                        Date time = DateUtil.parseStandardDateString(timeMatcher.group(0));
                        Hashrate hashrate1 = new Hashrate();
                        hashrate1.setHashrate(hashrate);
                        hashrate1.setReportTime(time);
                        hashRateMap.put(rigName, hashrate1);
                    }
                }

                Matcher lagMatcher = lagPattern.matcher(str);
                if (lagMatcher.find()) {
                    Integer lag = Integer.valueOf(lagMatcher.group(1));
                    allLag += lag;
                    lagCount++;
                }
            }

            upload();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
