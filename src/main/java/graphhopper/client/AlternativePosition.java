package graphhopper.client;

/**
 * User: Simon
 * Date: 01/07/15
 * Time: 13:43
 */
public class AlternativePosition implements IAlternative {
    String lon;
    String lat;
    boolean start;

    public AlternativePosition(String lon, String lat, boolean start) {
        this.lon = lon;
        this.lat = lat;
        this.start = start;
    }

    @Override
    public String format() {
        if(start) {
            return "lon1=" + lon + "&lat1=" + lat;
        } else {
            return "lon2=" + lon + "&lat2=" + lat;
        }
    }

    @Override
    public String getName() {
        return "position";
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof AlternativePosition)) {
            return false;
        }
        AlternativePosition alternativePosition = (AlternativePosition) obj;

        return lon.equals(alternativePosition.lon)
                && lat.equals(alternativePosition.lat)
                && start == alternativePosition.start;
    }

    @Override
    public int hashCode() {
        return lat.hashCode() + lon.hashCode() + (start ? -1 : 1);
    }
}
