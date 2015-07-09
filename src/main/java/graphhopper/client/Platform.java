package graphhopper.client;



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

    public Platform(JSONObject jsonObject) throws JSONException {
        host = jsonObject.getString("host");

        JSONArray jsonServices = jsonObject.getJSONArray("services");
        services = new ArrayList<>(jsonServices.length());
        for (int i = 0; i < jsonServices.length(); i++) {
            if(jsonServices.getJSONObject(i).getString("name").equals("position")) {
                jsonServices.getJSONObject(i).put("name", "positionStart");
                services.add(new VariationPoint(jsonServices.getJSONObject(i)));
                jsonServices.getJSONObject(i).put("name", "positionEnd");
                services.add(new VariationPoint(jsonServices.getJSONObject(i)));
            } else {
                services.add(new VariationPoint(jsonServices.getJSONObject(i)));
            }
        }
    }


    public boolean isProducer(List<IAlternative> request) {
       for(int i = 0; i < request.size(); i++) {
           final  int position = i;
           if(!services.get(position).getAlternatives().stream()
                   .anyMatch(alternative -> alternative.getName().equals(request.get(position).getName()))) {
               return false;
           }
       }
       return true;
   }

    public String getHost() {
        return host;
    }

    @Override
    public boolean equals(Object obj) {
        Platform platform = (Platform) obj;

        if(!host.equals(platform.host)) {
            return false;
        }

        for(VariationPoint service : services) {
            if(!platform.services.contains(service)) {
                return false;
            }
        }

        for(VariationPoint service : platform.services) {
            if(!services.contains(service)) {
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
