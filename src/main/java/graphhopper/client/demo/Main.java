package graphhopper.client.demo;

import graphhopper.client.Client;
import graphhopper.client.Info;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: Simon
 * Date: 02/07/15
 * Time: 14:41
 */
public class Main {

    public static String forcedIPAddress = null;
    public static String regexSeparator = "/";
    
    public static void main(String[] args) throws IOException, JSONException {
        List<Client> clients = new ArrayList<>();

        File dir = new File(args[0]);
        if(FileSystems.getDefault().toString().contains("Windows")) {
            regexSeparator = "\\\\";
        }
        System.out.println("Using separator " + regexSeparator);

        if(Arrays.asList(args).contains("-dummy")) {
            System.out.println("Building 300 dummies");
            int counter = 0;
            while(clients.size() < 300) {
                for (File file : dir.listFiles()) {
                    if (file.getName().endsWith(".json")) {
                        Client client = new Client(file.getAbsolutePath(), counter);
                        clients.add(client);
                        counter ++;
                    }
                }
                System.out.println(counter);
            }
        } else {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".json")) {
                    Client client = new Client(file.getAbsolutePath());
                    clients.add(client);
                }
            }
        }

        DemoWebSocketServer server = new DemoWebSocketServer(Integer.parseInt(args[1]));
        if(args.length > 2) {
            forcedIPAddress = args[2];
        }

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
