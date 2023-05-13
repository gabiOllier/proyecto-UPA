package ar.edu.info.lidi.upa.model;

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

}
