package graphhopper.client.demo;

import graphhopper.client.*;
import org.apache.commons.cli.*;
import org.json.JSONException;

import java.io.*;
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
    public static Set<String> allServices;

    public static String[] dirtyHackPositionStartAlternatives = {"53.315130,-6.238099"};
    public static String[] dirtyHackPositionEndAlternatives = {"53.303086,-6.287210"};

    public static Map<String, String> dirtyHackHostById;

    public static void main(String[] args) throws IOException, JSONException, ParseException {
        File dir;
        int wsport;
        String hostListFile = null;
        Options optionsMain = new Options();
        optionsMain.addOption("h", "help", false, "display this message");
        optionsMain.addOption("c", "clients", true, "path to the folder containing the client files");
        optionsMain.addOption("p", "wsport", true, "web-socket port");
        optionsMain.addOption("l", "hostslist", true, "path to the hosts list file, triggers the use of the internal Monkey");
        optionsMain.addOption("s", "parksize", true, "requested size of the platforms park");
        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine commandLine = commandLineParser.parse(optionsMain, args);
        if (commandLine.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("GraphHopper-client", optionsMain);
            return;
        }
        if (commandLine.hasOption("clients")) {
            dir = new File(commandLine.getOptionValue("clients"));
        } else {
            System.err.println("No clients folder specified, use option -c");
            return;
        }
        if (commandLine.hasOption("wsport")) {
            wsport = Integer.parseInt(commandLine.getOptionValue("wsport"));
        } else {
            System.err.println("No websocket port specified, use option -p");
            return;
        }
        if (commandLine.hasOption("hostslist")) {
            hostListFile = commandLine.getOptionValue("hostslist");
            System.out.println("Using internal Monkey with file " + hostListFile);
        } else {
            System.out.println("No hosts list file specified, use option -l: please use external Monkey");
        }

        //File dir = new File(args[0]);
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
        allServices = new LinkedHashSet<>();
        for (Client client : clients) {
            allServices.addAll(client.getAllPossibleRequests().stream()
                    .map(client::formatRequestQuery)
                    .collect(Collectors.toList()));
        }
        System.out.println(clients.size() + " clients / " + allPlatforms.size() + " platforms / " + allServices.size() + " services");
        System.out.println("Client mean size=" + clients.stream().mapToInt(c -> c.getAllPossibleRequests().size()).average().getAsDouble());
        //System.out.println(allServices);

        //TODO dirty hack
        dirtyHackHostById = new HashMap<>();
        int parksize = -1;
        if (commandLine.hasOption("parksize")) {
            parksize = Integer.parseInt(commandLine.getOptionValue("parksize"));
        } else {
            System.out.println("No park size specified, use option -s");
        }
        if (parksize > 0 && hostListFile != null) {
            BufferedReader br = new BufferedReader(new FileReader(hostListFile));
            String line;
            int counter = 0;
            while ((line = br.readLine()) != null) {
                counter++;
            }
            br.reset();
            if (parksize > allPlatforms.size() || parksize > counter) {
                System.err.println("Invalid park size: parksize/platforms/containers is " + parksize + "/" + allPlatforms.size() + "/" + counter);
                return;
            } else {
                Iterator<Platform> iter = allPlatforms.iterator();
                for (int i = 0; i < parksize; i++) {
                    dirtyHackHostById.put(iter.next().getId(), br.readLine());
                }
            }
        }

        DemoWebSocketServer server = new DemoWebSocketServer(wsport);

        Monkey monkey = null;
        if (hostListFile != null) {
            monkey = new Monkey(hostListFile, Monkey.SINUSOIDE);
        }

        server.start();
        Info.getInstance().setDemoWebSocketServer(server);

        tick = 0;
        tickResults.put(tick, new ArrayList<>());
        System.out.println("================== TICK " + tick + " =====================");
        if (hostListFile != null) {
            monkey.monkeyRun();
        }
        for (Client client : clients) {
            tickResults.get(tick).add(-1);
            client.start();
        }

        int tempCount = 0;
        int filled;
        PrintWriter results = new PrintWriter(new File("results_" + System.currentTimeMillis()));
        while (true) {
            filled = (int) tickResults.get(tick).stream().filter(output -> output >= 0).count();
            /*if (filled > tempCount) {
                for (int i = 0; i < filled - tempCount; i++) System.out.print("-");
                tempCount = filled;
            }*/
            if (filled == clients.size()
                    && (hostListFile != null ? !monkey.newTick : true)) {
                System.out.println();
                System.out.println(tickResults.get(tick).stream()
                        .map(Object::toString)
                        .collect(Collectors.joining()));
                System.out.println(Info.getInstance().selectedPlatformsPerClient.get(tick).stream()
                        .map(list -> Integer.toString(list.size()))
                        .collect(Collectors.joining()));
                if (hostListFile != null) {
                    results.println("KillRatio=" + monkey.ratio);
                }
                System.out.println("DeadClients=" + Info.getInstance().getDeadClientsRate(tick) * 100);
                results.println("DeadClientsRatio=" + Info.getInstance().getDeadClientsRate(tick));
                System.out.println("RequestRetries=" + Info.getInstance().getRequestFailureNumber(tick));
                results.println("RequestRetries=" + Info.getInstance().getRequestFailureNumber(tick));
                System.out.println("TotalServices=" + Info.getInstance().getTotalOfferedServicesNumber(tick));
                results.println("TotalServices=" + Info.getInstance().getTotalOfferedServicesNumber(tick));
                results.flush();
                Info.getInstance().tick(tick);
                tick++;
                System.out.println("================== TICK " + tick + " =====================");
                results.println("Tick="+tick);
                if (hostListFile != null) {
                    monkey.monkeyRun();
                }
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
