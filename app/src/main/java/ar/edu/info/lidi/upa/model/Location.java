package ar.edu.info.lidi.upa.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Location {


    /** Nombre de la ubicacion */
    protected String name;
    /** Scans realizados sobre esta ubicacion */
    protected List<ScanDetail> scanDetails = new ArrayList<>();

    public Location(String name) {
        this.name = name;
    }

    @JsonCreator
    public Location(@JsonProperty("name") String name, @JsonProperty("scanDetails") List<ScanDetail> scanDetails) {
        this.name = name;
        this.scanDetails = scanDetails;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ScanDetail> getScanDetails() {
        return scanDetails;
    }

    public void setScanDetails(List<ScanDetail> scanDetails) {
        this.scanDetails = scanDetails;
    }

    @Override
    public String toString() {
        return "\nLocation{" +
                "name='" + name + '\'' +
                ", scanDetails=" + scanDetails +
                '}';
    }
}
