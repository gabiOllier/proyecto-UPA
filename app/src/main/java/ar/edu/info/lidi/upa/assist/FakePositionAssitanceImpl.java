package ar.edu.info.lidi.upa.assist;

import android.content.Context;
import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.List;

import ar.edu.info.lidi.upa.model.Location;
import ar.edu.info.lidi.upa.model.ScanDetail;
import ar.edu.info.lidi.upa.model.TrainingSet;

public class FakePositionAssitanceImpl extends WiFiPositionAssistanceImpl {
    @Override
    public void train(Context ctx, String location, ProcessCompletedCallBackInterface iface) {
        loadFakeTrainingSet();
        iface.trainingCompleted("Fake training finalizado");
    }

    @Override
    public void locate(Context ctx, ProcessCompletedCallBackInterface iface) {
        this.iface = iface;
        estimateLocation(loadFakePositionScan());
    }

    /**
     * Datos de entrenamiento
     */
    protected void loadFakeTrainingSet() {
        Location locationA = new Location("Estudio A");
        locationA.getScanDetails().add(new ScanDetail("aa:aa:aa:aa:aa:01",99));
        locationA.getScanDetails().add(new ScanDetail("aa:aa:aa:aa:aa:02",80));
        locationA.getScanDetails().add(new ScanDetail("aa:aa:aa:aa:aa:03",0));
        locationA.getScanDetails().add(new ScanDetail("aa:aa:aa:aa:aa:04",30));

        Location locationB = new Location("Oficina B");
        locationB.getScanDetails().add(new ScanDetail("aa:aa:aa:aa:aa:01",10));
        locationB.getScanDetails().add(new ScanDetail("aa:aa:aa:aa:aa:02",40));
        locationB.getScanDetails().add(new ScanDetail("aa:aa:aa:aa:aa:03",99));

        trainingSet = new TrainingSet();
        trainingSet.getLocations().add(locationA);
        trainingSet.getLocations().add(locationB);

        notifyObservers();
    }

    /**
     * Lectura para estimación de posicion
     */
    protected List<ScanResult> loadFakePositionScan() {
        List<ScanResult> scan = new ArrayList<>();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            ScanResult sr1 = new ScanResult();
            sr1.BSSID = "aa:aa:aa:aa:aa:01";
            sr1.level = 20;

            ScanResult sr2 = new ScanResult();
            sr2.BSSID = "aa:aa:aa:aa:aa:02";
            sr2.level = 15;

            ScanResult sr3 = new ScanResult();
            sr3.BSSID = "aa:aa:aa:aa:aa:04";
            sr3.level = 80;

            scan.add(sr1);
            scan.add(sr2);
            scan.add(sr3);
        }

        return scan;

    }

}
