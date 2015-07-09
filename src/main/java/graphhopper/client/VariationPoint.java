package graphhopper.client;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: Simon
 * Date: 01/07/15
 * Time: 12:05
 */
public class VariationPoint {
    protected String name;
    protected List<IAlternative> alternatives = new ArrayList();

    public VariationPoint(JSONObject jsonObject) throws JSONException {
        name = jsonObject.getString("name");

        JSONArray jsonAlternatives = jsonObject.getJSONArray("alternatives");
        alternatives = new ArrayList<>(jsonAlternatives.length());
        for (int i = 0; i < jsonAlternatives.length(); i++) {
            IAlternative alternative;
            if (name.equals("positionStart")) {
                alternative = new AlternativePosition(jsonAlternatives.getString(i).split(",")[0].trim(), jsonAlternatives.getString(i).split(",")[1].trim(), true);
            } else if (name.equals("positionEnd")) {
                alternative = new AlternativePosition(jsonAlternatives.getString(i).split(",")[0].trim(), jsonAlternatives.getString(i).split(",")[1].trim(), false);
            } else {
                alternative = new Alternative(jsonAlternatives.getString(i), this);
            }
            alternatives.add(alternative);
        }
    }

    public String getName() {
        return this.name;
    }

    public VariationPoint(String name) {
        this.name = name;
    }

    public void addAlternative(Alternative a) {
        this.alternatives.add(a);
    }

    public List<IAlternative> getAlternatives() {
        return alternatives;
    }

    public List<IAlternative> getShuffleAlternatives() {
        ArrayList<IAlternative> shuffleAlternative = new ArrayList<>(alternatives);
        Collections.shuffle(shuffleAlternative);
        return shuffleAlternative;

    }

    @Override
    public boolean equals(Object obj) {
        VariationPoint variationPoint = (VariationPoint) obj;

        if (!name.equals(variationPoint.name)) {
            return false;
        }

        for (IAlternative alternative : alternatives) {
            if (!(variationPoint.alternatives.contains(alternative))) {
                return false;
            }
        }

        for (IAlternative alternative : variationPoint.alternatives) {
            if (!(alternatives.contains(alternative))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return alternatives.hashCode() + name.hashCode();
    }
}
