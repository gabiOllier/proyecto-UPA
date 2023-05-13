package ar.edu.info.lidi.upa.model;

import java.util.ArrayList;
import java.util.List;

public class TrainingSet {

    /** Nomina de ubicaciones */
    protected List<Location> locations = new ArrayList<>();

    public List<Location> getLocations() {
        return locations;
    }
    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }
}
