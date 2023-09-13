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
    protected List<ScanDetail> loadFakePositionScan() {
        List<ScanDetail> scan = new ArrayList<>();
        scan.add(new ScanDetail("aa:aa:aa:aa:aa:01", 20));
        scan.add(new ScanDetail("aa:aa:aa:aa:aa:02", 15));
        scan.add(new ScanDetail("aa:aa:aa:aa:aa:03", 80));
        return scan;
    }

}
