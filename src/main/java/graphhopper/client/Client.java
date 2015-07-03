package graphhopper.client;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: Simon
 * Date: 01/07/15
 * Time: 11:29
 */
public class Client extends Thread {

    protected List<VariationPoint> services;
    protected Set<Platform> platforms;
    protected String REQUESTED_METHOD = "restful-graphhopper-1.0/?";
    protected HttpClient httpClient;

    public Client(String fileName) throws IOException, JSONException {
        parse(fileName);
        initHttpClient(10);
        Info.info().addArchitecture(services, platforms);

    }

    protected List<IAlternative> createRequest() {
        return services.stream()
                .map(variationPoint -> variationPoint.getShuffleAlternatives().stream()
                        .findAny()
                        .get())
                .collect(Collectors.toList());

    }

    protected List<Platform> selectPlatforms(List<IAlternative> request) {
        return platforms.stream()
                .filter(platform -> platform.isProducer(request))
                .distinct()
                .collect(Collectors.toList());

    }

    public void run()  {
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<IAlternative> request = createRequest();

            List<Platform> selectedPlatforms = selectPlatforms(request);
            Collections.shuffle(selectedPlatforms);

            List<Platform> platformsTry = new ArrayList<>(selectedPlatforms.size());

            Random random = new Random();
            for (Platform platform : selectedPlatforms) {
//                sendRequest(request, platform);
//                if(random.nextBoolean()) {
                if(sendRequest(request, platform)) {
                    platformsTry.add(platform);
                    break;
                } else {
                    Info.info().addRequest(this, request, selectedPlatforms, platformsTry, platform);
                }
            }
        }
    }

    protected boolean sendRequest(List<IAlternative> request, Platform platform)  {
        try {
            String formatedRequest = formatRequest(request, platform);
            System.out.println(formatedRequest);

            HttpGet httpGet = new HttpGet(formatedRequest);
            HttpResponse response = httpClient.execute(httpGet);

            System.out.println(response.getStatusLine().getStatusCode());

            if(checkStatus(response.getStatusLine().getStatusCode())) {
                System.out.println(EntityUtils.toString(response.getEntity()));
                return EntityUtils.toString(response.getEntity()).contains("code_error");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    protected boolean checkStatus(int status) {
        return status >= 200 && status < 300;
    }

    protected String formatRequest(List<IAlternative> request, Platform platform) {
        return platform.getHost() + REQUESTED_METHOD + request.stream().
                map(alternative -> alternative.format())
                .collect(Collectors.joining("&"));
    }


    protected void initHttpClient(int timeout)  {
        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(timeout * 1000)
                .setConnectTimeout(timeout * 1000)
                .build();

        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
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
        for(int i = 0; i < jsonPlatforms.length(); i++) {
            platforms.add(new Platform(jsonPlatforms.getJSONObject(i)));
        }

        JSONArray jsonServices = jsonObject.getJSONArray("services");
        services = new ArrayList<>(jsonServices.length());
        for(int i = 0; i < jsonServices.length(); i++) {
            services.add(new VariationPoint(jsonServices.getJSONObject(i)));
        }

    }

}
