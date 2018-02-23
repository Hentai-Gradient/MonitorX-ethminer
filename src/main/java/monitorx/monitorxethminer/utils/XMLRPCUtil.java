package monitorx.monitorxethminer.utils;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

public class XMLRPCUtil {
    public static String requestXmlRPC(String url, String userName, String password, String function, String processName) {
        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(url));
            if (StringUtils.isNotEmpty(userName)) {
                config.setBasicUserName(userName);
            }
            if (StringUtils.isNotEmpty(password)) {
                config.setBasicPassword(password);
            }
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            // create parameter list
            Vector<String> params = new Vector<>();
            params.add(processName);
            // execute XML-RPC call
            return JSON.toJSONString(client.execute(function, params));
        } catch (MalformedURLException | XmlRpcException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}
