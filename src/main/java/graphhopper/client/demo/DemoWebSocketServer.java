package graphhopper.client.demo;

import graphhopper.client.Info;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.NotYetConnectedException;
import java.util.Collection;

/**
 * User: Simon
 * Date: 02/07/15
 * Time: 11:42
 */
public class DemoWebSocketServer extends WebSocketServer {
    public DemoWebSocketServer(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }


    @Override
    public void onOpen(WebSocket ws, ClientHandshake ch) {
        /*try {
            ws.send(String.valueOf(Info.getInstance().allData()));
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public void onClose(WebSocket ws, int i, String string, boolean bln) {
    }

    @Override
    public void onMessage(WebSocket ws, String string) {

    }

    @Override
    public void onError(WebSocket ws, Exception excptn) {

    }

    public void update(JSONObject object) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                try {
                    c.send(object.toString());
                } catch (NotYetConnectedException ignored) {
                }
            }
        }
    }

    public void tick(int tick) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                try {
                    JSONObject object = new JSONObject();
                    object.put("tick", tick);
                    c.send(object.toString());
                } catch (NotYetConnectedException ignored) {
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
