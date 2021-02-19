package app.petbattle.rest.client;

import java.util.ArrayList;
import java.util.List;

public class NSFFResponse {

    private List<ArrayList<String>> predictions;

    public List<ArrayList<String>> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<ArrayList<String>> predictions) {
        this.predictions = predictions;
    }

    @Override
    public String toString() {
        return "NSFFResponse{" +
                "predictions=" + predictions +
                '}';
    }

    // {"predictions": [[0.992924571, 0.00707544712]]}
    public boolean isSff(Double limit) {
        if (predictions.get(0).size() != 2)
            return false;
        if (Double.valueOf(predictions.get(0).get(0)) >= limit)
            return true;
        return false;
    }
}
