package monitorx.monitorxethminer.jsonrpc;

import monitorx.monitorxethminer.HTTPUtil;

import java.io.IOException;

/**
 * @author xma
 */
public class JsonRPCImpl {

    public static String doRequest(String url, Integer id, String jsonrpc, String method) throws IOException {
        return HTTPUtil.sendGet(url);
    }
}