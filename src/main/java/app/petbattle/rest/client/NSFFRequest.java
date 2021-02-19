package app.petbattle.rest.client;

import java.util.ArrayList;
import java.util.List;

public class NSFFRequest {

    private ArrayList<String> instances;

    public NSFFRequest(ArrayList<String> instances) {
        this.instances = instances;
    }

    public List<String> getInstances() {
        return instances;
    }

    public void setInstances(ArrayList<String> instances) {
        this.instances = instances;
    }

    @Override
    public String toString() {
        return "{" +
                "\"instances\": " + instances +
                '}';
    }

}
