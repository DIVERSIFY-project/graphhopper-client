package graphhopper.client;


import graphhopper.client.demo.Main;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
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
    protected HttpClient httpClient;
    HttpGet httpGet;
    HttpResponse httpResponse;

    Socket socket;

    String header;
    boolean newTick = true;
    boolean verbose = false;

    Map<String, HttpClient> httpClients;
    Map<String, Socket> sockets;
    Map<String, PrintWriter> socketOutputs;
    Map<String, BufferedReader> socketInputs;

    public Client(String fileName, int number) throws IOException, JSONException {
        parse(fileName);
        //initSockets();
        httpClients = new HashMap<>();
        initHttpClient(10);
        Info.info().addArchitecture(services, platforms);
        name = fileName.split(Main.regexSeparator)[fileName.split(Main.regexSeparator).length - 1].split("\\.")[0];
        if (number > 0) {
            header = "[" + name + " (" + number + ")] ";
        } else {
            header = "[" + name + "] ";
        }
        this.number = number;
    }

    protected void initSockets() {
        sockets = new HashMap<>();
        socketOutputs = new HashMap<>();
        socketInputs = new HashMap<>();
        for (Platform platform : platforms) {
            Socket socket = new Socket();
            try {
                //socket.setSoLinger(false, 0);
                socket.setKeepAlive(true);
                socket.connect(new InetSocketAddress(platform.getAddress(), platform.getPort()));
                sockets.put(platform.getHost(), socket);
                socketOutputs.put(platform.getHost(), new PrintWriter(socket.getOutputStream(), true));
                socketInputs.put(platform.getHost(), new BufferedReader(new InputStreamReader(socket.getInputStream())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    protected List<Platform> selectPlatforms(List<IAlternative> request) {
        return platforms.stream()
                .filter(platform -> platform.isProducer(request))
                .distinct()
                .collect(Collectors.toList());

    }

    public void run() {
        while (true) {
            List<IAlternative> request = createRequest();

            List<Platform> selectedPlatforms = selectPlatforms(request);
            Collections.shuffle(selectedPlatforms);

            List<Platform> platformsFailed = new ArrayList<>();

            for (Platform platform : selectedPlatforms) {
                if (verbose)
                    System.out.println(header + platform.getHost() + " : " + platformsFailed.size() + "/" + selectedPlatforms.size());
                if (!sendRequest(request, platform)) {
                    platformsFailed.add(platform);
                    if (verbose)
                        System.out.println(header + platformsFailed.stream().map(Platform::getHost).collect(Collectors.joining(";")) + " failed");
                } else {
                    if (verbose) System.out.println(header + platform.getHost() + " answered");
                    Info.info().addRequest(Main.tick, this, request, selectedPlatforms, platformsFailed, platform);
                    break;
                }
                Info.info().addRequest(Main.tick, this, request, selectedPlatforms, platformsFailed, platform);
            }
            newTick = false;
            Main.tickResults.get(Main.tick).set(number, platformsFailed.size());
            startWait();
        }
    }

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
            URI uri = new URI(formatedRequest);
            String message = "";
            String line;
            //socket = new Socket();
            //socket.setSoLinger(false, 0);
            //socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort()), 3000);
            //PrintWriter outSocket = new PrintWriter(sockets.get(platform.getHost()).getOutputStream(), true);
            //BufferedReader inSocket = new BufferedReader(new InputStreamReader(sockets.get(platform.getHost()).getInputStream()));
            //PrintWriter outSocket = socketOutputs.get(platform.getHost());
            //BufferedReader inSocket = socketInputs.get(platform.getHost());
            //outSocket.print("GET " + uri.getPath() + "?" + uri.getQuery() + " HTTP/1.0\r\n" +
            //        "Connection: close\r\n\r\n");
            //outSocket.flush();
            //while ((line = inSocket.readLine()) != null) {
            //    message += line;
            //}
            //System.out.println(message);
            //outSocket.close();
            //inSocket.close();
            //socket.close();
            //return message.contains("200 OK") && !message.contains("code_error");
            httpGet = new HttpGet(formatedRequest);
            try {
                //initHttpClient(3);
                httpResponse = /*httpClient*/httpClients.get(platform.getHost()).execute(httpGet);

                if (verbose) System.out.println(header + httpResponse.getStatusLine().getStatusCode());

                if (checkStatus(httpResponse.getStatusLine().getStatusCode())) {
                    String responseString = EntityUtils.toString(httpResponse.getEntity());
                    httpGet.abort();
                    httpGet.reset();
                    //httpClient.close();
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
            }
            httpGet.abort();
            httpGet.reset();
            //httpClient.close();
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


    protected void initHttpClient(int timeout) {
        /*httpClient = null;
        RequestConfig config = RequestConfig.custom()
                .setCircularRedirectsAllowed(false)
                .setConnectionRequestTimeout(4000)
                .setConnectTimeout(4000)
                .setMaxRedirects(0)
                .setRedirectsEnabled(false)
                .setSocketTimeout(4000)
                .build();

        SocketConfig socketConfig = SocketConfig.custom()
                .setSoKeepAlive(false)
                .setSoLinger(0)
                .setSoReuseAddress(true)
                .setSoTimeout(5000)
                .setTcpNoDelay(true)
                .build();*/

        /*RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(timeout * 1000)
                .setConnectTimeout(timeout * 1000)
                .build();*/

        /*httpClient = HttpClients.custom()
                //.setDefaultSocketConfig(SocketConfig.custom().setSoKeepAlive(false).build())
                .setDefaultSocketConfig(socketConfig)
                .setDefaultRequestConfig(config)
                .build();*/
        for(Platform platform : platforms) {
            RequestConfig config = RequestConfig.custom()
                    .setCircularRedirectsAllowed(false)
                    .setConnectionRequestTimeout(4000)
                    .setConnectTimeout(4000)
                    .setMaxRedirects(0)
                    .setRedirectsEnabled(false)
                    .setSocketTimeout(4000)
                    .build();

            SocketConfig socketConfig = SocketConfig.custom()
                    .setSoKeepAlive(false)
                    .setSoLinger(0)
                    .setSoReuseAddress(true)
                    .setSoTimeout(5000)
                    .setTcpNoDelay(true)
                    .build();
            HttpClient tmp = HttpClients.custom()
                    //.setDefaultSocketConfig(SocketConfig.custom().setSoKeepAlive(false).build())
                    .setDefaultSocketConfig(socketConfig)
                    .setDefaultRequestConfig(config)
                    .build();
            httpClients.put(platform.getHost(), tmp);
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
