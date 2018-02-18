package monitorx.monitorxethminer.statusReport;

import java.util.Date;

public class Hashrate {
    private Double hashrate;
    private Date reportTime;

    public Double getHashrate() {
        return hashrate;
    }

    public void setHashrate(Double hashrate) {
        this.hashrate = hashrate;
    }

    public Date getReportTime() {
        return reportTime;
    }

    public void setReportTime(Date reportTime) {
        this.reportTime = reportTime;
    }
}
