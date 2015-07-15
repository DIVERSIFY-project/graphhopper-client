package graphhopper.client.demo;

import graphhopper.client.Client;
import graphhopper.client.Info;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Simon
 * Date: 02/07/15
 * Time: 14:41
 */
public class Main {

    public static void main(String[] args) throws IOException, JSONException {
        List<Client> clients = new ArrayList<>();

        File dir = new File(args[0]);

        for (File file : dir.listFiles()) {
            if (file.getName().endsWith(".json")) {
                Client client = new Client(file.getAbsolutePath());
                clients.add(client);
            }
        }

        DemoWebSocketServer server = new DemoWebSocketServer(Integer.parseInt(args[1]));
        server.start();
        Info.info().setDemoWebSocketServer(server);

        for (Client client : clients) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            client.start();
        }
    }


}
