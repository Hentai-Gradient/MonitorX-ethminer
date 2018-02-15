package monitorx.monitorxethminer.tail;

import com.alibaba.fastjson.JSON;
import monitorx.monitorxethminer.utils.SocketUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author qianlifeng
 */
public class LogTail implements Runnable {
    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 存储TailLog侦听器
     */
    private final Set<TailNotify> listeners = new HashSet<>();

    /**
     * 当读到文件结尾后暂停的时间间隔
     */
    private long sampleInterval = 10;

    /**
     * 设置API地址
     */
    private String apiUrl;

    /**
     * 设置tail运行标记
     */
    private boolean tailing = false;

    public LogTail(long sampleInterval, String apiUrl) {
        super();
        this.sampleInterval = sampleInterval;
        this.apiUrl = apiUrl;
    }

    /**
     * 将侦听器加入TailLog中
     *
     * @param tailListener
     */
    public void add(TailNotify tailListener) {
        listeners.add(tailListener);
    }

    /**
     * 通知所有注册的侦听
     *
     * @param line
     */
    protected void notify(String line) {
        for (TailNotify tail : listeners) {
            tail.notifyMsg(line);
        }
    }

    @Override
    public void run() {
        this.tailing = true;
        try {
            while (this.tailing) {
                String res = null;
                try {
                    res = SocketUtil.sendRequest(apiUrl);
                    List<String> resList = JSON.parseArray(JSON.parseObject(res).getString("result"), String.class);
                    this.notify(resList.get(2).split(";")[0]);
                    Thread.sleep(this.sampleInterval);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止tail
     */
    public void stop() {
        this.tailing = false;
    }
}
