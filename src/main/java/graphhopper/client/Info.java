package graphhopper.client;

import graphhopper.client.demo.DemoWebSocketServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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


    private Info() {
        allPlatforms = new HashSet<>();
        deadPlatforms = new HashSet<>();
        nbDeadPlatforms = new LinkedList<>();
        nbTryPerRequest = new LinkedList<>();
        nbPlatformsPerRequest = new LinkedList<>();
    }


    public synchronized void addRequest(Client client, List<IAlternative> request, List<Platform> platforms,  List<Platform> platformsTry, Platform success) {
        deadPlatforms.remove(success);
        deadPlatforms.addAll(platformsTry);
        nbDeadPlatforms.add(deadPlatforms.size());
        nbTryPerRequest.add(platformsTry.size() + 1);
        nbPlatformsPerRequest.add(platforms.size());

        System.out.println(request);
        /*System.out.println("nbDeadPlatforms: " + deadPlatforms.size());
        System.out.println("nbTryPerRequest: " + (platformsTry.size() + 1));

        System.out.println("nbPlatformsPerRequest: " + platforms.size());*/

    }

    public synchronized void addArchitecture(List<VariationPoint> services, Set<Platform> platforms) {
        allPlatforms.addAll(platforms);
    }

    public JSONObject allData() throws JSONException {
        JSONObject allData = new JSONObject();

        allData.put("title", new JSONObject("{text: plot}"));
        JSONArray data = new JSONArray();
        allData.put("data",data);
        data.put(formatList(nbDeadPlatforms, "nbDeadPlatforms"));
        data.put(formatList(nbPlatformsPerRequest, "nbPlatformsPerRequest"));
        data.put(formatList(nbTryPerRequest, "nbTryPerRequest"));

        allData.put("type", "init");
        return allData;
    }

    protected JSONObject formatList(List<Integer> list, String name) throws JSONException {
        JSONObject object = new JSONObject();
        object.put("type", "spline");
        object.put("name", name);

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
        if(info == null) {
            info = new Info();
        }
        return info;
    }

    public void setDemoWebSocketServer(DemoWebSocketServer demoWebSocketServer) {
        this.demoWebSocketServer =  demoWebSocketServer;
    }
}
