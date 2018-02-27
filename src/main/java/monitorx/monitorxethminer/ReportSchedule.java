package monitorx.monitorxethminer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import monitorx.monitorxethminer.statusReport.Metric;
import monitorx.monitorxethminer.statusReport.NodeStatus;
import monitorx.monitorxethminer.statusReport.NodeStatusUpload;
import monitorx.monitorxethminer.utils.HTTPUtil;
import monitorx.monitorxethminer.utils.NumberUtil;
import monitorx.monitorxethminer.utils.SocketUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author qianlifeng
 */
@Component
public class ReportSchedule {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    EthMinerService ethMinerService;

    private Date lastUploadDate;

    private Map<String, String> xhInfo;

    private long filePointer = 0;
    private List<String> errorInfo = new ArrayList<>();

    private List<Map<String, String>> gpuInfo = new ArrayList<>();

    @Value("${log}")
    String logFile;

    @Value("${gpuFolder}")
    private String gpuFolder;

    @Value("${api.url}")
    private String apiUrl;

    @Value("${code}")
    private String code;

    @Value("${url}")
    private String url;

    @Value("${report.url}")
    private String reportUrl;

    @Value("${wallet}")
    private String wallet;

    private static Pattern GPUTemperaturePattern = Pattern.compile("GPU Temperature:(.*)C");
    private static Pattern GPULoadPattern = Pattern.compile("GPU Load:(.*) %");
    private static Pattern GPUPowerPattern = Pattern.compile("(.*?) W \\(average GPU\\)");
    private static Pattern GPUMCLKPattern = Pattern.compile("(.*?) MHz \\(MCLK\\)");
    private static Pattern GPUSCLKPattern = Pattern.compile("(.*?) MHz \\(SCLK\\)");
    private static Pattern GPUInvalidSolutionPattern = Pattern.compile("Invalid solution");
    private static Pattern GPUIncorrectResultPattern = Pattern.compile("incorrect result");

    @Scheduled(fixedDelay = 6000)
    public void report() {
        upload();
    }

    NodeStatusUpload lastUpload = null;

    private void upload() {
        try {
            NodeStatusUpload statusUpload = new NodeStatusUpload();
            NodeStatus status = new NodeStatus();
            statusUpload.setNodeCode(code);
            statusUpload.setNodeStatus(status);

            status.setStatus("up");
            List<Metric> metrics = new ArrayList<>();
            status.setMetrics(metrics);

            Metric currentHandlingOrders = new Metric();
            currentHandlingOrders.setTitle("实时算力");
            currentHandlingOrders.setType("number");
            metrics.add(currentHandlingOrders);
            Date lastTailDate = ethMinerService.getLastTailDate();
            Integer lastTailMh = ethMinerService.getLastTailMh();
            if (lastTailDate != null && (lastUploadDate == null || lastUploadDate.compareTo(lastTailDate) != 0)) {
                if (ethMinerService.getLastTailMh() != null) {
                    lastUploadDate = lastTailDate;
                    currentHandlingOrders.setValue(lastTailMh.toString());
                }
            } else {
                currentHandlingOrders.setValue("0");
            }

            Metric gpuMetric = new Metric();
            gpuMetric.setTitle("GPU信息");
            gpuMetric.setType("text");
            StringBuilder sb = new StringBuilder();
            sb.append("<table class='table table-bordered table-condensed'>");
            sb.append("     <tr>");
            sb.append("         <th width='50'>序号</th>");
            sb.append("         <th>温度℃</th>");
            sb.append("         <th>算力Mh/s</th>");
            sb.append("         <th>负载%</th>");
            sb.append("         <th>显存频率</th>");
            sb.append("         <th>核心频率</th>");
            sb.append("         <th>平均功率W</th>");
            sb.append("     </tr>");
            for (Map<String, String> gpu : gpuInfo) {
                sb.append("     <tr>");
                sb.append("         <td>").append(gpu.get("index")).append("</td>");
                sb.append("         <td>").append(gpu.get("temperature")).append("</td>");
                sb.append("         <td>").append(gpu.get("hashRate")).append("</td>");
                sb.append("         <td>").append(gpu.get("load")).append("</td>");
                sb.append("         <td>").append(gpu.get("mclk")).append("</td>");
                sb.append("         <td>").append(gpu.get("sclk")).append("</td>");
                sb.append("         <td>").append(gpu.get("power")).append("</td>");
                sb.append("     </tr>");
            }
            sb.append("</table>");
            gpuMetric.setValue(sb.toString());
            gpuMetric.setContext(JSON.toJSONString(gpuInfo));
            metrics.add(gpuMetric);

            if (errorInfo != null && errorInfo.size() != 0) {
                Map<String, Integer> errorMap = new HashMap<>(10);
                errorInfo.forEach(err -> {
                    if (errorMap.containsKey(err)) {
                        errorMap.put(err, errorMap.get(err) + 1);
                    } else {
                        errorMap.put(err, 1);
                    }
                });
                Metric errInfoMetric = new Metric();
                errInfoMetric.setTitle("出现错误");
                errInfoMetric.setType("text");
                sb = new StringBuilder();
                sb.append("<table class='table table-bordered table-condensed'>");
                sb.append("     <tr>");
                sb.append("         <th width='70'>序号</th>");
                sb.append("         <th>出现次数</th>");
                sb.append("     </tr>");
                for (String err : errorMap.keySet()) {
                    sb.append("     <tr>");
                    sb.append("         <td>").append(err).append("</td>");
                    sb.append("         <td>").append(errorMap.get(err)).append("</td>");
                    sb.append("     </tr>");
                }
                sb.append("</table>");
                errInfoMetric.setValue(sb.toString());
                errInfoMetric.setContext(JSON.toJSONString(errorInfo));
                metrics.add(errInfoMetric);
            }

            logger.info("reporting to monitorx, url={}, code={}", url, code);
            lastUpload = statusUpload;
            HTTPUtil.sendBodyPost(url, JSON.toJSONString(statusUpload));
        } catch (IOException e) {
            logger.error("upload to monitorx error: " + e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 30 * 1000)
    private void internalUpload() {
        try {
            if (lastUpload != null) {
                HTTPUtil.sendBodyPost(reportUrl, JSON.toJSONString(lastUpload));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedDelay = 10000)
    private void getGPUInfo() {
        List<Map<String, String>> info = new ArrayList<>();

        String res = null;
        String[] hashRateList = null;
        try {
            res = SocketUtil.sendRequest(apiUrl);
            List<String> resList = JSON.parseArray(JSON.parseObject(res).getString("result"), String.class);
            String[] tempList = resList.get(6).split("; ");
            hashRateList = resList.get(3).split(";");
        } catch (IOException e) {
            e.printStackTrace();
        }

        int hashRateIndex = 0;
        if (StringUtils.isNotEmpty(gpuFolder) && hashRateList != null) {
            for (int i = 0; i < 20; i++) {
                Path path = Paths.get(gpuFolder, i + "", "amdgpu_pm_info");
                try {
                    String content = new String(Files.readAllBytes(path));
                    Matcher temperatureMatcher = GPUTemperaturePattern.matcher(content);
                    Matcher loadMatcher = GPULoadPattern.matcher(content);
                    Matcher powerMatcher = GPUPowerPattern.matcher(content);
                    Matcher mclkMatcher = GPUMCLKPattern.matcher(content);
                    Matcher sclkMatcher = GPUSCLKPattern.matcher(content);
                    if (temperatureMatcher.find() && loadMatcher.find() && powerMatcher.find() && mclkMatcher.find() && sclkMatcher.find()) {
                        Map<String, String> infoMap = new HashMap<>();
                        String temperature = temperatureMatcher.group(1).trim();
                        String load = loadMatcher.group(1).trim();
                        String power = powerMatcher.group(1).trim();
                        String mclk = mclkMatcher.group(1).trim();
                        String sclk = sclkMatcher.group(1).trim();

                        infoMap.put("index", i + "");
                        infoMap.put("temperature", temperature);
                        if (!"0".equals(load)) {
                            infoMap.put("hashRate", String.valueOf(NumberUtil.roundUpFormatDouble(Double.valueOf(hashRateList[hashRateIndex++]) / 1000D, 4)));
                        }
                        infoMap.put("load", load);
                        infoMap.put("power", power);
                        infoMap.put("mclk", mclk);
                        infoMap.put("sclk", sclk);
                        info.add(infoMap);
                    } else {
                        logger.info("didn't find");
                    }
                } catch (NoSuchFileException ignore) {
                } catch (IOException e) {
                    logger.error("read gpu info failed, path={}", path.toString(), e);
                }
            }
        }

        gpuInfo = info;
    }

    @Scheduled(fixedDelay = 100000)
    private void getFailedInfo() {
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
                    if (checkIfInvalidResolution(line)) {
                        errorInfo.add(getErrorCardId(line));
                    }
                    line = file.readLine();
                }
                filePointer = file.getFilePointer();
            }
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Boolean checkIfInvalidResolution(String line) {
        Matcher matcher = GPUInvalidSolutionPattern.matcher(line);
        Matcher matcher1 = GPUIncorrectResultPattern.matcher(line);
        return matcher.find() || matcher1.find();
    }

    private static Pattern findErrorCardPattern = Pattern.compile("cl-\\d");

    private String getErrorCardId(String line) {
        Matcher matcher = findErrorCardPattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return "line";
    }
}