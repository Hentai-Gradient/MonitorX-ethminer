package monitorx.monitorxethminer;

import com.alibaba.fastjson.JSON;
import monitorx.monitorxethminer.statusReport.Metric;
import monitorx.monitorxethminer.statusReport.NodeStatus;
import monitorx.monitorxethminer.statusReport.NodeStatusUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * @author qianlifeng
 */
@Component
public class ReportSchedule {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    EthMinerService ethMinerService;

    private Date lastUploadDate;
    @Value("${code}")
    private String code;

    @Value("${url}")
    private String url;

    @Scheduled(fixedDelay = 10000)
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

            Metric currentHandlingOrders = new Metric();
            currentHandlingOrders.setTitle("是否有接受请求");
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

            logger.info("reporting to monitorx, url={}, code={}", url, code);
            HTTPUtil.sendBodyPost(url, JSON.toJSONString(statusUpload));
        } catch (IOException e) {
            logger.error("upload to monitorx error: " + e.getMessage(), e);
        }
    }
}
