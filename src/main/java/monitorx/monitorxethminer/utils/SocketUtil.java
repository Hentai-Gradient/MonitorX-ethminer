package monitorx.monitorxethminer.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketUtil {
    private static Logger logger = LoggerFactory.getLogger(HTTPUtil.class);

    public static String sendRequest(String url) throws IOException {
        Socket socket = new Socket(url.split(":")[0], Integer.valueOf(url.split(":")[1]));
        socket.setSoTimeout(2000);
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeBytes("{\"id\":\"17\",\"jsonrpc\":\"2.0\",\"method\":\"miner_getstat1\"}\n");
        String res = input.readLine();
        out.close();
        input.close();
        return res;
    }
}