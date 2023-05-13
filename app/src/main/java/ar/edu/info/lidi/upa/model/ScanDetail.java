package ar.edu.info.lidi.upa.model;

public class ScanDetail {

    /** Basic Service Set Identifier */
    protected String bbsid;
    /** Intensidad de la señal (%) */
    protected int ss;

    public ScanDetail(String bbsid, int signalStrength) {
        this.bbsid = bbsid;
        this.ss = signalStrength;
    }

    public String getBbsid() {
        return bbsid;
    }

    public void setBbsid(String bbsid) {
        this.bbsid = bbsid;
    }

    public int getSs() {
        return ss;
    }

    public void setSs(int ss) {
        this.ss = ss;
    }


}
