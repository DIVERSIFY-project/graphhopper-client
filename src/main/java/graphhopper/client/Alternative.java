package graphhopper.client;


/**
 * User: Simon
 * Date: 01/07/15
 * Time: 13:37
 */
public class Alternative implements IAlternative {
    protected String name;
    protected VariationPoint variationPoint;

    public Alternative(String name, VariationPoint variationPoint) {
        this.name = name;
        this.variationPoint = variationPoint;
    }

    public String format() {
        return variationPoint.getName() + "=" + name;
    }

    @Override
    public String getName() {
        return name;
    }

    public String toString()  {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Alternative)) {
            return false;
        }
        Alternative alternative = (Alternative) obj;

        return name.equals(alternative.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
