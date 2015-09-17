package graphhopper.client;

import graphhopper.client.demo.DemoWebSocketServer;
import graphhopper.client.demo.Main;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * User: Simon
 * Date: 02/07/15
 * Time: 10:25
 */
public class Info {
    private static Info info;


    protected Set<Platform> allPlatforms;
    protected Set<Platform> deadPlatforms;

    protected List<Integer> nbDeadPlatforms;
    protected List<Integer> nbPlatformsPerRequest;
    protected List<Integer> nbTryPerRequest;
    private DemoWebSocketServer demoWebSocketServer;

    public Map<Integer, List<List<Platform>>> selectedPlatformsPerClient = new HashMap<>();
    public Map<Integer, List<List<Platform>>> failedPlatformsPerClient = new HashMap<>();
    public Map<Integer, Set<Platform>> allPlatformsPerTick = new HashMap<>();

    private Info() {
        allPlatforms = new HashSet<>();
        deadPlatforms = new HashSet<>();
        nbDeadPlatforms = new LinkedList<>();
        nbTryPerRequest = new LinkedList<>();
        nbPlatformsPerRequest = new LinkedList<>();
    }


    public synchronized void addRequest(int tick, Client client, List<IAlternative> request, List<Platform> platforms, List<Platform> platformsTry, Platform success) {
        deadPlatforms.remove(success);
        deadPlatforms.addAll(platformsTry);
        nbDeadPlatforms.add(deadPlatforms.size());
        nbTryPerRequest.add(platformsTry.size() + 1);
        nbPlatformsPerRequest.add(platforms.size());

        if(!allPlatformsPerTick.containsKey(tick)) {
            allPlatformsPerTick.put(tick, new HashSet<>());
        }
        allPlatformsPerTick.get(tick).addAll(platforms);

        if(!selectedPlatformsPerClient.containsKey(tick)) {
            List<List<Platform>> dummy = new ArrayList<>();
            for (int i = 0; i < Main.clients.size(); i++) {
                dummy.add(new ArrayList<>());
            }
            selectedPlatformsPerClient.put(tick, dummy);
        }
        selectedPlatformsPerClient.get(tick).set(client.number, platforms);

        if(!failedPlatformsPerClient.containsKey(tick)) {
            List<List<Platform>> dummy = new ArrayList<>();
            for (int i = 0; i < Main.clients.size(); i++) {
                dummy.add(new ArrayList<>());
            }
            failedPlatformsPerClient.put(tick, new ArrayList<>(dummy));
        }
        failedPlatformsPerClient.get(tick).set(client.number, platformsTry);
        /*System.out.println("nbDeadPlatforms: " + deadPlatforms.size());
        System.out.println("nbTryPerRequest: " + (platformsTry.size() + 1));

        System.out.println("nbPlatformsPerRequest: " + platforms.size());*/
        /*try {
            demoWebSocketServer.update(allData());
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
    }

    public synchronized void tick(int tick) {
        try {
            demoWebSocketServer.update(dashboardData(tick));
            demoWebSocketServer.tick();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public synchronized JSONObject dashboardData(int tick) throws JSONException {
        JSONObject data = new JSONObject();
        data.put("dead", getDeadClientsRate(tick));
        data.put("retry", getRequestFailureNumber(tick));
        data.put("service", getTotalOfferedServicesNumber(tick));
        return data;
    }

    public synchronized double getConnectionSuccessRate(int tick) {
        double totalSuccess = 0;
        for (int i = 0; i < Main.clients.size(); i++) {
            totalSuccess += selectedPlatformsPerClient.get(tick).get(i).size() > failedPlatformsPerClient.get(tick).get(i).size() ? 1 : 0;
        }
        return totalSuccess / (double)Main.clients.size();
    }

    public synchronized double getDeadClientsRate(int tick) {
        double totalDead = 0;
        for (int i = 0; i < Main.clients.size(); i++) {
            totalDead += selectedPlatformsPerClient.get(tick).get(i).size() == failedPlatformsPerClient.get(tick).get(i).size() ? 1 : 0;
        }
        return totalDead / (double)Main.clients.size();
    }

    public synchronized int getRequestFailureNumber(int tick) {
        return failedPlatformsPerClient.get(tick).stream()
                .mapToInt(List::size)
                .sum();
    }

    public synchronized int getTotalOfferedServicesNumber (int tick) {
        return allPlatformsPerTick.get(tick).stream()
                .mapToInt(platform -> platform.getServices().stream()
                        .mapToInt(vp -> vp.getAlternatives().size())
                        .reduce(1, (a, b) -> a *b))
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
        data.put(formatList(nbDeadPlatforms, "nbDeadPlatforms"));
        data.put(formatList(nbPlatformsPerRequest, "nbPlatformsPerRequest"));
        data.put(formatList(nbTryPerRequest, "nbTryPerRequest"));
        allData.put("data", data);
        allData.put("type", "init");
        return allData;
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

    public static Info info() {
        if (info == null) {
            info = new Info();
        }
        return info;
    }

    public void setDemoWebSocketServer(DemoWebSocketServer demoWebSocketServer) {
        this.demoWebSocketServer = demoWebSocketServer;
    }
}
