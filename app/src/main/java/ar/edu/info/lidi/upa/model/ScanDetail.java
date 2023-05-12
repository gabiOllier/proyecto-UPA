package ar.edu.info.lidi.upa.model;

public class ScanDetail {

    /** Basic Service Set Identifier */
    public String bbsid;
    /** Intensidad de la señal (%) */
    public int signalStrength;

    public ScanDetail(String bbsid, int signalStrength) {
        this.bbsid = bbsid;
        this.signalStrength = signalStrength;
    }
}
