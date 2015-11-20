package graphhopper.client;


import graphhopper.client.demo.Main;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Simon
 * Date: 01/07/15
 * Time: 11:51
 */
public class Platform {
    List<VariationPoint> services;
    String host;
    String address;
    int port;

    public Platform(JSONObject jsonObject) throws JSONException {
        if (Main.forcedIPAddress != null) {
            host = Main.forcedIPAddress;
        } else {
            host = jsonObject.getString("host");
        }
        extractAddressAndPort(host);

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
    }

    public void extractAddressAndPort(String host) {
        address = host.split(":")[1].substring(2);
        port = Integer.parseInt(host.split(":")[2].substring(0, host.split(":")[2].length() - 1));
    }

    public List<VariationPoint> getServices() {
        return services;
    }

    public boolean isProducer(List<IAlternative> request) {
        for (int i = 0; i < request.size(); i++) {
            final int position = i;
            if(request.get(position) != null) {
                if (!services.get(position).getAlternatives().stream()
                        .anyMatch(alternative -> alternative.getName().equals(request.get(position).getName()))) {
                    return false;
                }
            }
        }
        return true;
    }

    public String getHost() {
        return host;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        Platform platform = (Platform) obj;

        if (!host.equals(platform.host)) {
            return false;
        }

        for (VariationPoint service : services) {
            if (!platform.services.contains(service)) {
                return false;
            }
        }

        for (VariationPoint service : platform.services) {
            if (!services.contains(service)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return services.hashCode() + host.hashCode();
    }
}
