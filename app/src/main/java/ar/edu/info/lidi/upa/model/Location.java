package ar.edu.info.lidi.upa.model;

import java.util.ArrayList;
import java.util.List;

public class Location {

    /** Nombre de la ubicacion */
    public String name;
    /** Scans realizados sobre esta ubicacion */
    public List<ScanDetail> scanDetails = new ArrayList<>();

    public Location(String name) {
        this.name = name;
    }
}
