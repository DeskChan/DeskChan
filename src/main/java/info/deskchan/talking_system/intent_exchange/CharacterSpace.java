package info.deskchan.talking_system.intent_exchange;

import info.deskchan.talking_system.CharacterRange;

import java.util.LinkedList;
import java.util.List;

public class CharacterSpace<T extends ICompatible, K> {

    List<Precedent<T, K>> precedents = new LinkedList<>();
    int dimensions;
    private int coordsSize;

    public CharacterSpace(int dimensions){
        if (dimensions <= 0) throw new IndexOutOfBoundsException();
        this.dimensions = dimensions;
        this.coordsSize = dimensions * CharacterRange.getFeatureCount();
    }

    public void add(Precedent<T, K> precedent){
        if (precedent.coords.length != coordsSize)
            throw new RuntimeException("Dimensions is not match");

        precedents.add(precedent);
    }

    private float distance(float[] a, float[] b){
        float sum = 0;
        for (int i = 0; i < a.length; i++)
            sum += Math.abs(a[i] - b[i]);
        return sum / dimensions / CharacterRange.getFeatureCount();
    }

    private static final double IN_SPACE_DISTANCE_WEIGHT = 1, PRECEDENT_DISTANCE_WEIGHT = 3;

    public SearchResult<T, K> getNearest(InputData<T> data){
        if (data.coords.length != coordsSize)
            throw new RuntimeException("Dimensions is not match");

        SearchResult result = new SearchResult();
        for (Precedent<T, K> entry : precedents){
            double distance =
                    distance(data.coords, entry.coords) * IN_SPACE_DISTANCE_WEIGHT +
                            (1 - data.checkCompatible(entry)) * PRECEDENT_DISTANCE_WEIGHT;
            //System.out.println(entry + " " + distance);
            if (distance < result.distance){
                result.distance = distance;
                result.precedent = entry;
            }
        }
        return result;
    }

    public static class SearchResult<T extends ICompatible, K> {
        public Precedent<T, K> precedent = null;
        public double distance = Double.MAX_VALUE;
    }
}
