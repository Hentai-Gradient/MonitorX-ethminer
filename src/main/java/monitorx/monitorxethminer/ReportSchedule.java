package monitorx.monitorxethminer;

import com.alibaba.fastjson.JSON;
import monitorx.monitorxethminer.statusReport.Metric;
import monitorx.monitorxethminer.statusReport.NodeStatus;
import monitorx.monitorxethminer.statusReport.NodeStatusUpload;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
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

    private List<Map<String, String>> gpuInfo = new ArrayList<>();

    @Value("${gpuFolder}")
    private String gpuFolder;

    @Value("${api.url}")
    private String apiUrl;

    @Value("${code}")
    private String code;

    @Value("${url}")
    private String url;

    @Value("${report.url:}")
    private String reportUrl;

    private static Pattern GPUTemperaturePattern = Pattern.compile("GPU Temperature:(.*)C");
    private static Pattern GPULoadPattern = Pattern.compile("GPU Load:(.*) %");
    private static Pattern GPUPowerPattern = Pattern.compile("(.*?) W \\(average GPU\\)");
    private static Pattern GPUMCLKPattern = Pattern.compile("(.*?) MHz \\(MCLK\\)");
    private static Pattern GPUSCLKPattern = Pattern.compile("(.*?) MHz \\(SCLK\\)");

    @Scheduled(fixedDelay = 6000)
    public void report() {
        upload();
    }

    private NodeStatusUpload lastUpload = null;

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
            if (mh != null) {
                currentHandlingOrders.setValue(mh);
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

    private static Pattern mhLinePattern = Pattern.compile("ETH: .*Mh/s");
    private static Pattern mhPattern = Pattern.compile("\\d*\\.\\d*");
    private String mh = null;
    private List<String> hashRateList = new ArrayList<>();

    @Scheduled(fixedDelay = 10000)
    private void getGPUInfo() {
        List<Map<String, String>> info = new ArrayList<>();

        String res = null;
        try {
            res = HTTPUtil.sendGet(apiUrl);
            String[] resList = res.split("<br>");
            for (String str : resList) {
                Matcher mhLineMatcher = mhLinePattern.matcher(str);
                if (mhLineMatcher.find()) {
                    String mhLine = mhLineMatcher.group(0);
                    Matcher mhMatcher = mhPattern.matcher(mhLine);
                    hashRateList = new ArrayList<>();
                    while (mhMatcher.find()) {
                        hashRateList.add(mhMatcher.group());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Double mhs = 0D;
        for (String hashRate : hashRateList) {
            mhs += Double.valueOf(hashRate);
        }

        mh = String.valueOf(mhs.intValue());

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
                            infoMap.put("hashRate", hashRateList.get(hashRateIndex++));
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
}