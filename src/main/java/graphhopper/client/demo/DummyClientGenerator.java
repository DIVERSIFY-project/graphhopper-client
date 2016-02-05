package graphhopper.client.demo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * User: Simon
 * Date: 04/09/15
 * Time: 10:44
 */
public class DummyClientGenerator {
    List<String> hosts;
    JSONArray services;
    int minHost;
    int maxHost;

    static boolean factorBased = true;

    public DummyClientGenerator(String hostsFileName, String servicesFileName, int minHost, int maxHost) throws IOException, JSONException {
        this.hosts = parseHosts(hostsFileName);
        this.services = parseServices(servicesFileName);
        this.minHost = minHost;
        this.maxHost = maxHost;
    }

    public DummyClientGenerator(String hostsFileName, String servicesFileName) throws IOException, JSONException {
        this.hosts = parseHosts(hostsFileName);
        this.services = parseServices(servicesFileName);
        this.minHost = Math.max(hosts.size() / 50, 1);
        this.maxHost = Math.max(hosts.size() / 9, 2);
    }

    public void generateClients(int nbClient, String outputDirName) throws JSONException, IOException {
        File outputDir = new File(outputDirName);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        if (!(outputDir.listFiles().length == 0)) {
            for (File file : outputDir.listFiles()) {
                file.delete();
            }
        }

        for (int i = 0; i < nbClient; i++) {
            JSONObject jsonObject = generateClient();
            FileWriter writer = new FileWriter(new File(outputDir + "/client" + i + ".json"));
            jsonObject.write(writer);
            writer.close();
        }
        System.out.println("Using " + hosts.size() + " platforms and " + services.length() + " services, generated " + nbClient + " clients connected to " + minHost + "-" + maxHost + " platforms each");
    }

    protected JSONObject generateClient() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("services", services);

        JSONArray platforms = new JSONArray();
        object.put("platforms", platforms);
        Random r = new Random();
        for (int i = 0; i < minHost + r.nextInt(maxHost - minHost); i++) {
            JSONObject platform = new JSONObject();
            platforms.put(platform);
            platform.put("services", services);
            platform.put("host", "http://" + hosts.get(r.nextInt(hosts.size())) + "/");
        }
        return object;
    }

    protected List<String> parseHosts(String fileName) throws IOException {
        List<String> hosts = new LinkedList<>();
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = reader.readLine();
        while (line != null) {
            hosts.add(line);
            line = reader.readLine();
        }

        return hosts;
    }

    protected JSONArray parseServices(String fileName) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            sb.append(line);
            line = reader.readLine();
        }

        return new JSONArray(sb.toString());
    }

    public static void main(String[] args) throws IOException, JSONException {
        if (!factorBased) {
            System.out.println("FACTORBASED OFF: using command line values");
            DummyClientGenerator dcg = new DummyClientGenerator(args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
            dcg.generateClients(Integer.parseInt(args[4]), args[5]);
        } else {
            System.out.println("FACTORBASED ON: using factor calculated values");
            DummyClientGenerator dcg = new DummyClientGenerator(args[0], args[1]);
            dcg.generateClients(dcg.hosts.size() * 3, args[5]);
        }
    }
}
