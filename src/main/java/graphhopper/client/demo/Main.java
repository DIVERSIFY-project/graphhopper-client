package graphhopper.client.demo;

import graphhopper.client.Client;
import graphhopper.client.Info;
import graphhopper.client.Monkey;
import graphhopper.client.Platform;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: Simon
 * Date: 02/07/15
 * Time: 14:41
 */
public class Main {

    public static String forcedIPAddress = null;
    public static String regexSeparator = "/";
    public static Map<Integer, List<Integer>> tickResults = new HashMap<>();
    public static int tick;
    public static List<Client> clients = new ArrayList<>();
    public static Set<Platform> allPlatforms;

    public static String[] dirtyHackPositionStartAlternatives = {"53.315130,-6.238099"};
    public static String[] dirtyHackPositionEndAlternatives = {"53.303086,-6.287210"};

    public static void main(String[] args) throws IOException, JSONException {

        File dir = new File(args[0]);
        if (!dir.isDirectory()) {
            System.err.println(dir + ": not a folder, aborting");
            System.exit(1);
        }
        if (FileSystems.getDefault().toString().contains("Windows")) {
            regexSeparator = "\\\\";
        }
        System.out.println("Using separator " + regexSeparator);

        if (Arrays.asList(args).contains("-dummy")) {
            System.out.println("Building 300 dummies");
            int counter = 0;
            while (clients.size() < 300) {
                for (File file : dir.listFiles()) {
                    if (file.getName().endsWith(".json")) {
                        Client client = new Client(file.getAbsolutePath(), counter);
                        clients.add(client);
                        counter++;
                    }
                }
                System.out.println(counter);
            }
        } else {
            System.out.println("Loading clients from folder '" + dir + "'...");
            int counter = 0;
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".json")) {
                    Client client = new Client(file.getAbsolutePath(), counter);
                    clients.add(client);
                    counter++;
                }
            }
        }

        allPlatforms = new LinkedHashSet<>();
        for (Client client : clients) {
            allPlatforms.addAll(client.getPlatforms());
        }
        System.out.println(clients.size() + " clients / " + allPlatforms.size() + " platforms");

        DemoWebSocketServer server = new DemoWebSocketServer(Integer.parseInt(args[1]));
        /*if (args.length > 2) {
            forcedIPAddress = args[2];
        }*/
        Monkey monkey = new Monkey(args[2]);

        server.start();
        Info.getInstance().setDemoWebSocketServer(server);

        tick = 0;
        tickResults.put(tick, new ArrayList<>());
        System.out.println("================== TICK " + tick + " =====================");
        monkey.monkeyRun();
        for (Client client : clients) {
            tickResults.get(tick).add(-1);
            client.start();
        }
        //monkey.start();

        int tempCount = 0;
        int filled;
        while (true) {
            filled = (int) tickResults.get(tick).stream().filter(output -> output >= 0).count();
            /*if (filled > tempCount) {
                for (int i = 0; i < filled - tempCount; i++) System.out.print("-");
                tempCount = filled;
            }*/
            if (filled == clients.size() && !monkey.newTick) {
                System.out.println();
                System.out.println(tickResults.get(tick).stream()
                        .map(Object::toString)
                        .collect(Collectors.joining()));
                System.out.println(Info.getInstance().selectedPlatformsPerClient.get(tick).stream()
                        .map(list -> Integer.toString(list.size()))
                        .collect(Collectors.joining()));
                System.out.println("DeadClients=" + Info.getInstance().getDeadClientsRate(tick) * 100 + "%");
                System.out.println("RequestRetries=" + Info.getInstance().getRequestFailureNumber(tick));
                System.out.println("TotalServices=" + Info.getInstance().getTotalOfferedServicesNumber(tick));
                Info.getInstance().tick(tick);
                tick++;
                System.out.println("================== TICK " + tick + " =====================");
                monkey.monkeyRun();
                tickResults.put(tick, new ArrayList<>());
                for (int i = 0; i < clients.size(); i++) {
                    tickResults.get(tick).add(-1);
                }
                for (Client client : clients) {
                    client.notice();
                }
                tempCount = 0;
            }
        }
    }

}
