package graphhopper.client;

import graphhopper.client.demo.DemoWebSocketServer;
import graphhopper.client.demo.Main;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: Simon
 * Date: 02/07/15
 * Time: 10:25
 */
public class Info {
    private static Info instance;


    protected Set<Platform> allPlatforms;
    protected Set<Platform> deadPlatforms;

    protected List<Integer> numberDeadPlatforms;
    protected List<Integer> numberPlatformsByRequest;
    protected List<Integer> numberTryByRequest;
    private DemoWebSocketServer demoWebSocketServer;

    public Map<Integer, List<List<Platform>>> selectedPlatformsByClient = new HashMap<>();
    public Map<Integer, List<List<Platform>>> failedPlatformsByClient = new HashMap<>();
    public Map<Integer, Set<Platform>> allPlatformsByTick = new HashMap<>();

    public Map<Integer, List<Integer>> failedPlatformsBySuccessfulClient = new HashMap<>();
    public Map<Integer, List<Integer>> failedPlatformsByFailedClient = new HashMap<>();

    public boolean monkeyOn = false;
    public List<Integer> monkeyPausedPlatforms;
    public List<Double> monkeyRatio;
    public int initialClientsNumber = 0;
    public int initialPlatformsNumber = 0;
    public int initialServicesNumber = 0;
    public Map<String, Map<Integer, Double>> addedCSVData1;
    public Map<String, Map<Integer, Double>> addedCSVData2;

    private Info() {
        allPlatforms = new HashSet<>();
        deadPlatforms = new HashSet<>();
        numberDeadPlatforms = new LinkedList<>();
        numberTryByRequest = new LinkedList<>();
        numberPlatformsByRequest = new LinkedList<>();
        monkeyPausedPlatforms = new LinkedList<>();
        monkeyRatio = new LinkedList<>();
    }

    public synchronized void countRequests(int tick, Client client, int platformsFailedSuccess, int platformsFailedFailure) {
        if (!failedPlatformsBySuccessfulClient.containsKey(tick)) {
            List<Integer> dummy = new ArrayList<>();
            for (int i = 0; i < Main.clients.size(); i++) {
                dummy.add(-1);
            }
            failedPlatformsBySuccessfulClient.put(tick, new ArrayList<>(dummy));
        }
        failedPlatformsBySuccessfulClient.get(tick).set(client.number, platformsFailedSuccess);
        if (!failedPlatformsByFailedClient.containsKey(tick)) {
            List<Integer> dummy = new ArrayList<>();
            for (int i = 0; i < Main.clients.size(); i++) {
                dummy.add(-1);
            }
            failedPlatformsByFailedClient.put(tick, new ArrayList<>(dummy));
        }
        failedPlatformsByFailedClient.get(tick).set(client.number, platformsFailedFailure);
    }

    public synchronized void addRequest(int tick, Client client, List<IAlternative> request, List<Platform> platforms, List<Platform> platformsFailed, Platform success) {
        deadPlatforms.remove(success);
        deadPlatforms.addAll(platformsFailed);
        numberDeadPlatforms.add(deadPlatforms.size());
        numberTryByRequest.add(platformsFailed.size() + 1);
        numberPlatformsByRequest.add(platforms.size());

        if (!allPlatformsByTick.containsKey(tick)) {
            allPlatformsByTick.put(tick, new HashSet<>());
        }
        allPlatformsByTick.get(tick).addAll(platforms);

        if (!selectedPlatformsByClient.containsKey(tick)) {
            List<List<Platform>> dummy = new ArrayList<>();
            for (int i = 0; i < Main.clients.size(); i++) {
                dummy.add(new ArrayList<>());
            }
            selectedPlatformsByClient.put(tick, dummy);
        }
        selectedPlatformsByClient.get(tick).set(client.number, platforms);

        if (!failedPlatformsByClient.containsKey(tick)) {
            List<List<Platform>> dummy = new ArrayList<>();
            for (int i = 0; i < Main.clients.size(); i++) {
                dummy.add(new ArrayList<>());
            }
            failedPlatformsByClient.put(tick, new ArrayList<>(dummy));
        }
        failedPlatformsByClient.get(tick).set(client.number, platformsFailed);
        /*System.out.println("numberDeadPlatforms: " + deadPlatforms.size());
        System.out.println("numberTryByRequest: " + (platformsFailed.size() + 1));

        System.out.println("numberPlatformsByRequest: " + platforms.size());*/
        /*try {
            demoWebSocketServer.update(allData());
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
    }

    public synchronized void tick(int tick) {
        try {
            demoWebSocketServer.update(dashboardData(tick));
            //demoWebSocketServer.tick(tick);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public synchronized JSONObject dashboardData(int tick) throws JSONException {
        JSONObject data = new JSONObject();
        Map<String, Object> valueTimeMap;
        data.put("tick", tick);
        valueTimeMap = new HashMap<>();
        valueTimeMap.put("time_stamp", tick);
        valueTimeMap.put("value", getDeadClientsRate(tick) * 100);
        data.put("dead", valueTimeMap);
        valueTimeMap = new HashMap<>();
        valueTimeMap.put("time_stamp", tick);
        valueTimeMap.put("value", getRequestFailureNumber(tick));
        data.put("retry", valueTimeMap);
        valueTimeMap = new HashMap<>();
        valueTimeMap.put("time_stamp", tick);
        valueTimeMap.put("value", getTotalOfferedServicesNumber(tick));
        data.put("service", valueTimeMap);
        data.put("ratio", monkeyRatio.get(tick));
        data.put("monkey", monkeyOn);
        data.put("pausedplatforms", monkeyPausedPlatforms.get(tick));
        data.put("initialclients", initialClientsNumber);
        data.put("initialplatforms", initialPlatformsNumber);
        data.put("initialservices", initialServicesNumber);
        if(addedCSVData1 != null) {
            if(addedCSVData1.get("DeadClientsRatio").get(tick) != null) {
                valueTimeMap = new HashMap<>();
                valueTimeMap.put("time_stamp", tick);
                valueTimeMap.put("value", addedCSVData1.get("DeadClientsRatio").get(tick) * 100);
                data.put("csvdead1", valueTimeMap);
                valueTimeMap = new HashMap<>();
                valueTimeMap.put("time_stamp", tick);
                valueTimeMap.put("value", addedCSVData1.get("RequestRetries").get(tick));
                data.put("csvretry1", valueTimeMap);
                valueTimeMap = new HashMap<>();
                valueTimeMap.put("time_stamp", tick);
                valueTimeMap.put("value", addedCSVData1.get("TotalServices").get(tick));
                data.put("csvservice1", valueTimeMap);
            }
        }
        if(addedCSVData2 != null) {
            if(addedCSVData2.get("DeadClientsRatio").get(tick) != null) {
                valueTimeMap = new HashMap<>();
                valueTimeMap.put("time_stamp", tick);
                valueTimeMap.put("value", addedCSVData2.get("DeadClientsRatio").get(tick) * 100);
                data.put("csvdead2", valueTimeMap);
                valueTimeMap = new HashMap<>();
                valueTimeMap.put("time_stamp", tick);
                valueTimeMap.put("value", addedCSVData2.get("RequestRetries").get(tick));
                data.put("csvretry2", valueTimeMap);
                valueTimeMap = new HashMap<>();
                valueTimeMap.put("time_stamp", tick);
                valueTimeMap.put("value", addedCSVData2.get("TotalServices").get(tick));
                data.put("csvservice2", valueTimeMap);
            }
        }
        return data;
    }

    public synchronized double getConnectionSuccessRate(int tick) {
        double totalSuccess = 0;
        for (int i = 0; i < Main.clients.size(); i++) {
            totalSuccess += selectedPlatformsByClient.get(tick).get(i).size() > failedPlatformsByClient.get(tick).get(i).size() ? 1 : 0;
        }
        return totalSuccess / (double) Main.clients.size();
    }

    public synchronized double getDeadClientsRate(int tick) {
        double totalDead = 0;
        for (int i = 0; i < Main.clients.size(); i++) {
            totalDead += selectedPlatformsByClient.get(tick).get(i).size() == failedPlatformsByClient.get(tick).get(i).size() ? 1 : 0;
        }
        return totalDead / (double) Main.clients.size();
    }

    public synchronized int getDeadClients(int tick) {
        int totalDead = 0;
        for (int i = 0; i < Main.clients.size(); i++) {
            totalDead += selectedPlatformsByClient.get(tick).get(i).size() == failedPlatformsByClient.get(tick).get(i).size() ? 1 : 0;
        }
        return totalDead;
    }

    public synchronized int getRequestFailureNumber(int tick) {
        return failedPlatformsByClient.get(tick).stream()
                .mapToInt(List::size)
                .sum();
    }

    public synchronized int getTotalOfferedServicesNumber(int tick) {
        return allPlatformsByTick.get(tick).stream()
                .mapToInt(platform -> platform.getServices().stream()
                        .mapToInt(vp -> vp.getAlternatives().size())
                        .reduce(1, (a, b) -> a * b))
                .sum();
    }

    public synchronized void addArchitecture(List<VariationPoint> services, Set<Platform> platforms) {
        allPlatforms.addAll(platforms);
    }

    public JSONObject allData() throws JSONException {
        JSONObject allData = new JSONObject();
        allData.put("title", new JSONObject("{text: \"plot\"}"));
        allData.put("legend", new JSONObject("{horizontalAlign: \"right\"," + "verticalAlign: \"bottom\"," + "fontSize: 15}"));
        JSONArray data = new JSONArray();
        data.put(formatList(numberDeadPlatforms, "numberDeadPlatforms"));
        data.put(formatList(numberPlatformsByRequest, "numberPlatformsByRequest"));
        data.put(formatList(numberTryByRequest, "numberTryByRequest"));
        allData.put("data", data);
        allData.put("type", "init");
        return allData;
    }

    public void exportData() {
        try {
            PrintWriter pw = new PrintWriter(new File("results_" + System.currentTimeMillis() + ".out"));
            pw.println(allData().toString());
        } catch (FileNotFoundException | JSONException e) {
            e.printStackTrace();
        }
    }

    protected JSONObject formatList(List<Integer> list, String name) throws JSONException {
        JSONObject object = new JSONObject();
        object.put("type", "spline");
        object.put("showInLegend", "true");
        object.put("legendText", name);
        object.put("dataPoints", list.stream()
                .map(i -> {
                    JSONObject o = null;
                    try {
                        o = new JSONObject("{y: " + i + "}");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return o;
                })
                .collect(Collectors.toList()));
        return object;
    }

    public void setMonkeyOn(boolean monkeyOn) {
        this.monkeyOn = monkeyOn;
    }

    public void setInitialClientsNumber(int initialClientsNumber) {
        this.initialClientsNumber = initialClientsNumber;
    }

    public void setInitialPlatformsNumber(int initialPlatformsNumber) {
        this.initialPlatformsNumber = initialPlatformsNumber;
    }

    public void setInitialServicesNumber(int initialServicesNumber) {
        this.initialServicesNumber = initialServicesNumber;
    }

    public void setMonkeyPausedPlatforms(int tick, int monkeyPausedPlatforms) {
        this.monkeyPausedPlatforms.add(tick, monkeyPausedPlatforms);
    }

    public void setMonkeyRatio(int tick, double monkeyRatio) {
        this.monkeyRatio.add(tick, monkeyRatio);
    }

    public void addCSVData(String addedCSVDataFile, int position) {
        Map<String, Map<Integer, Double>> addedCSVData = new LinkedHashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(addedCSVDataFile));
            String line = br.readLine();
            List<String> dataNames = Arrays.asList(line.split(","));
            for(int i = 1; i < dataNames.size(); i++) {
                addedCSVData.put(dataNames.get(i), new HashMap<>());
            }
            while((line = br.readLine()) != null) {
                int tick = Integer.parseInt(line.split(",")[0]);
                for(int i = 1; i < dataNames.size(); i++) {
                    addedCSVData.get(dataNames.get(i)).put(tick, Double.parseDouble(line.split(",")[i]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(position == 0) {
            addedCSVData1 = addedCSVData;
        } else if(position == 1) {
            addedCSVData2 = addedCSVData;
        } else {
            System.err.println("Bad position for external CSV data: " + position);
            System.exit(1);
        }
    }

    public static Info getInstance() {
        if (instance == null) {
            instance = new Info();
        }
        return instance;
    }

    public void setDemoWebSocketServer(DemoWebSocketServer demoWebSocketServer) {
        this.demoWebSocketServer = demoWebSocketServer;
    }
}
