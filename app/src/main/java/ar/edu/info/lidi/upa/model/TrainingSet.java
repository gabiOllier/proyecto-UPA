package ar.edu.info.lidi.upa.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class TrainingSet {

    /** Nomina de ubicaciones */
    protected List<Location> locations = new ArrayList<>();

    public TrainingSet() {}

    @JsonCreator
    public TrainingSet(@JsonProperty("locations") List<Location> locations) {
        this.locations = locations;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }
}
