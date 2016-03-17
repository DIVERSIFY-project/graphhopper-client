package graphhopper.client;


import graphhopper.client.demo.Main;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: Simon
 * Date: 01/07/15
 * Time: 11:29
 */
public class Client extends Thread {

    String name;
    int number = -1;
    protected List<VariationPoint> services;
    protected Set<Platform> platforms;
    protected String REQUESTED_METHOD = "route?locale=en&";//"route?locale=en&algoStr=astar&";//"restful-graphhopper-1.0/route?locale=en&algoStr=astar&";
    HttpGet httpGet;
    CloseableHttpResponse httpResponse;

    String header;
    boolean newTick = true;
    boolean verbose = false;

    Map<String, CloseableHttpClient> httpClients;

    public Client(String fileName, int number) throws IOException, JSONException {
        parse(fileName);
        httpClients = new HashMap<>();
        initHttpClient(0.2f);
        Info.getInstance().addArchitecture(services, platforms);
        name = fileName.split(Main.regexSeparator)[fileName.split(Main.regexSeparator).length - 1].split("\\.")[0];
        if (number > 0) {
            header = "[" + name + " (" + number + ")] ";
        } else {
            header = "[" + name + "] ";
        }
        this.number = number;
        //System.out.println(number + " : " + getAllPossibleRequests().size() + " / " + platforms.size());
    }

    protected List<IAlternative> createRequest() {
        // dirty hack
        List<IAlternative> result = new ArrayList<>();
        for (VariationPoint service : services) {
            if (!service.getAlternatives().isEmpty()) {
                result.add(service.getShuffleAlternatives().get(0));
            } else {
                result.add(new Alternative(service.getName(), null));
            }
        }
        return result;
    }

    public List<List<IAlternative>> getAllPossibleRequests() {
        List<List<IAlternative>> result = new ArrayList<>();
        int amount = 1;
        for (VariationPoint service : services) {
            amount *= service.getAlternatives().size();
        }
        for (int i = 0; i < amount; i++) {
            result.add(new ArrayList<>());
        }
        int mult = 1;
        for (int i = 0; i < services.size(); i++) {
            for (int k = 0; k < amount; k++) {
                result.get(k).add(services.get(i).getAlternatives().get((k / mult) % services.get(i).getAlternatives().size()));
            }
            mult *= services.get(i).getAlternatives().size();
        }
        return result;
    }

    protected List<Platform> selectPlatforms(List<IAlternative> request) {
        return platforms.stream()
                .filter(platform -> platform.isProducer(request))
                .distinct()
                .collect(Collectors.toList());

    }

    public void run() {
        while (true) {
            double startTick = System.currentTimeMillis();
            List<IAlternative> request = createRequest();

            List<Platform> selectedPlatforms = selectPlatforms(request);
            Collections.shuffle(selectedPlatforms);

            List<Platform> platformsFailed = new ArrayList<>();
            boolean isConnected = false;
            List<Platform> platformsFailedSuccess = new ArrayList<>();
            List<Platform> platformsFailedFailure = new ArrayList<>();

            for (Platform platform : selectedPlatforms) {
                if (verbose)
                    System.out.println(header + platform.getHost() + " : " + platformsFailed.size() + "/" + selectedPlatforms.size());
                if (!sendRequest(request, platform)) {
                    platformsFailed.add(platform);
                    if (verbose)
                        System.out.println(header + platformsFailed.stream().map(Platform::getHost).collect(Collectors.joining(";")) + " failed on request " + formatRequest(request, platform));
                } else {
                    if (verbose) System.out.println(header + platform.getHost() + " answered");
                    Info.getInstance().addRequest(Main.tick, this, request, selectedPlatforms, platformsFailed, platform);
                    isConnected = true;
                    platformsFailedSuccess = platformsFailed;
                    break;
                }
                Info.getInstance().addRequest(Main.tick, this, request, selectedPlatforms, platformsFailed, platform);
            }
            if (!isConnected) {
                platformsFailedFailure = platformsFailed;
            }
            Info.getInstance().countRequests(Main.tick, this, platformsFailedSuccess.size(), platformsFailedFailure.size());
            newTick = false;
            Main.tickResults.get(Main.tick).set(number, platformsFailed.size());
            //System.out.println(this.number + " : " + (System.currentTimeMillis() - startTick) + " (" + platformsFailed.size() + "/" + selectedPlatforms.size() + ") " + request);
            //System.out.print("-");
            startWait();
        }
    }

    /*public void run() {
        while (true) {
            List<List<IAlternative>> requests = getAllPossibleRequests();
            List<List<IAlternative>> failedRequests = new ArrayList<>();
            for(List<IAlternative> request : requests) {
                boolean answered = false;
                Iterator<Platform> iterPlatform = platforms.iterator();
                while(!answered && iterPlatform.hasNext()) {
                    Platform platform = iterPlatform.next();
                    if (verbose)
                        System.out.println(header + platform.getHost() + " : " + failedRequests.size() + "/" + requests.size());
                    if (!sendRequest(request, platform)) {
                        failedRequests.add(request);
                        if (verbose)
                            System.out.println(header + failedRequests.stream().map(Collection::toString).collect(Collectors.joining(";")) + " failed on request " + formatRequest(request, platform) + " on platform " + platform.getHost());
                    } else {
                        if (verbose) System.out.println(header + platform.getHost() + " answered");
                        //Info.getInstance().addRequest(Main.tick, this, request, selectedPlatforms, platformsFailed, platform);
                        //break;
                        answered = true;
                    }
                    //Info.getInstance().addRequest(Main.tick, this, request, selectedPlatforms, platformsFailed, platform);
                }
            }
        }
    }*/

    synchronized void startWait() {
        try {
            while (!newTick) wait();
        } catch (InterruptedException exc) {
            System.out.println("wait() interrupted");
        }
    }

    synchronized public void notice() {
        newTick = true;
        notify();
    }

    protected boolean sendRequest(List<IAlternative> request, Platform platform) {
        try {
            String formatedRequest = formatRequest(request, platform);
            if (verbose) System.out.println(header + formatedRequest);
            httpGet = new HttpGet(formatedRequest);
            try {
                httpResponse = httpClients.get(platform.getHost()).execute(httpGet);

                if (verbose) System.out.println(header + httpResponse.getStatusLine().getStatusCode());

                if (checkStatus(httpResponse.getStatusLine().getStatusCode())) {
                    String responseString;
                    try {
                        responseString = EntityUtils.toString(httpResponse.getEntity());
                    } catch (IllegalArgumentException iae) {
                        responseString = "";
                    }
                    httpGet.abort();
                    httpGet.reset();
                    return !responseString.contains("code_error");
                }
            } catch (SocketTimeoutException ste) {
                if (verbose) System.err.println(platform.getHost() + " socket timed out");
                return false;
            } catch (HttpHostConnectException hhce) {
                if (verbose) System.err.println(platform.getHost() + " refused");
                return false;
            } catch (ConnectTimeoutException cte) {
                if (verbose) System.err.println(platform.getHost() + " connect timed out");
                return false;
            } catch (SocketException se) {
                if (verbose) System.err.println(platform.getHost() + " socket unreachable");
            } catch (IllegalStateException ise) {
                if (verbose) System.err.println(platform.getHost() + " connection pool shut down");
            } catch (InterruptedIOException iioe) {
                if (verbose) System.err.println(platform.getHost() + " connection shut down");
            }
            httpGet.abort();
            httpGet.reset();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected boolean checkStatus(int status) {
        return status >= 200 && status < 300;
    }

    protected String formatRequest(List<IAlternative> request, Platform platform) {
        return platform.getHost() + REQUESTED_METHOD + request.stream()
                .filter(alt -> alt != null)
                .map(IAlternative::format)
                .collect(Collectors.joining("&"));
    }

    public String formatRequestQuery(List<IAlternative> request) {
        return request.stream()
                .filter(alt -> alt != null)
                .map(IAlternative::format)
                .collect(Collectors.joining("&"));
    }

    protected void initHttpClient(float timeout) {
        for (Platform platform : platforms) {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setCircularRedirectsAllowed(false)
                    .setConnectionRequestTimeout((int)(timeout * 1000))
                    .setConnectTimeout((int)(timeout * 1000))
                    .setMaxRedirects(0)
                    .setRedirectsEnabled(false)
                    .setSocketTimeout((int)(timeout * 1000))
                    .build();
            SocketConfig socketConfig = SocketConfig.custom()
                    .setSoKeepAlive(false)
                    .setSoLinger(0)
                    .setSoReuseAddress(true)
                    .setSoTimeout((int)(timeout * 1000))
                    .setTcpNoDelay(true)
                    .build();

            CloseableHttpClient tmp = HttpClients.custom()
                    .setDefaultSocketConfig(socketConfig)
                    .setDefaultRequestConfig(requestConfig)
                    .build();
            httpClients.put(platform.getHost(), tmp);
            //httpClients.put(platform.getHost(), HttpClientBuilder.create().build());
        }
    }

    public void disconnect() throws IOException {
        if(httpGet != null) httpGet.abort();
        if(httpResponse != null) httpResponse.close();
        for (HttpClient httpClient : httpClients.values()) {
            if(httpClient != null) ((CloseableHttpClient) httpClient).close();
        }
    }

    protected void parse(String fileName) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            sb.append(line);
            line = reader.readLine();
        }

        JSONObject jsonObject = new JSONObject(sb.toString());

        JSONArray jsonPlatforms = jsonObject.getJSONArray("platforms");
        platforms = new HashSet<>(jsonPlatforms.length());
        for (int i = 0; i < jsonPlatforms.length(); i++) {
            platforms.add(new Platform(jsonPlatforms.getJSONObject(i)));
        }

        JSONArray jsonServices = jsonObject.getJSONArray("services");
        services = new ArrayList<>(jsonServices.length());
        for (int i = 0; i < jsonServices.length(); i++) {
            if (jsonServices.getJSONObject(i).getString("name").equals("position")) {
                jsonServices.getJSONObject(i).put("name", "positionStart");
                services.add(new VariationPoint(jsonServices.getJSONObject(i)));
                jsonServices.getJSONObject(i).put("name", "positionEnd");
                services.add(new VariationPoint(jsonServices.getJSONObject(i)));
            } else {
                services.add(new VariationPoint(jsonServices.getJSONObject(i)));
            }
        }
        //dirty hack
        JSONObject pos = new JSONObject();
        pos.put("name", "positionStart");
        pos.put("alternatives", new JSONArray(Main.dirtyHackPositionStartAlternatives));
        services.add(new VariationPoint(pos));
        pos = new JSONObject();
        pos.put("name", "positionEnd");
        pos.put("alternatives", new JSONArray(Main.dirtyHackPositionEndAlternatives));
        services.add(new VariationPoint(pos));

        reader.close();
    }

    public Set<Platform> getPlatforms() {
        return platforms;
    }

    @Override
    public String toString() {
        return name;
    }
}
