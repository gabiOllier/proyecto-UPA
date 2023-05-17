package ar.edu.info.lidi.upa.assist;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import ar.edu.info.lidi.upa.common.Observer;
import ar.edu.info.lidi.upa.exception.NoLocationAvailableException;
import ar.edu.info.lidi.upa.exception.TrainingProcessingException;
import ar.edu.info.lidi.upa.model.Location;
import ar.edu.info.lidi.upa.model.ScanDetail;
import ar.edu.info.lidi.upa.model.TrainingSet;

public class WiFiPositionAssistanceImpl implements PositionAssistanceInterface {

    /** Niveles de intensidad normalizado a porcentaje */
    static final int MAX_LEVELS = 100;
    /** Conjunto de emtrenamiento */
    protected TrainingSet trainingSet = new TrainingSet();
    protected IntentFilter intentFilter;

    /** Flag de registracion  */
    boolean registered = false;
    /** Estoy entrenando o determinando ubicacion? */
    boolean evaluatingWhereAmI = false;
    /** Clase que debe recibir el callback */
    protected ProcessCompletedCallBackInterface iface;
    protected Context ctx;
    protected String location;

    protected List<Observer> observers = new ArrayList<>();


    public void process(Context ctx, String location, ProcessCompletedCallBackInterface iface)  {
        this.iface = iface;
        this.location = location;
        this.ctx=ctx;
        WifiManager wifiManager = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent)  {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                } else {
                    iface.processingError(new TrainingProcessingException("Error durante el entrenamiento. Reintente.", Optional.empty()));
                }
            }
        };

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if (!registered) {
            ctx.getApplicationContext().registerReceiver(wifiScanReceiver, intentFilter);
            registered = true;
        }

        // Escanear la red, ya sea para entrenar o para estimar ubicacion
        if (!wifiManager.startScan()) {
            iface.processingError(new TrainingProcessingException("Error al iniciar entrenamiento. Demasiados intentos o validar permisos.", Optional.empty()));
        }
    }

    @Override
    public void train(Context ctx, String location, ProcessCompletedCallBackInterface iface)  {
        evaluatingWhereAmI = false;
        process(ctx, location, iface);
    }

    @Override
    public void locate(Context ctx, ProcessCompletedCallBackInterface iface) {
        evaluatingWhereAmI = true;
        if (trainingSet.getLocations().isEmpty()) {
            iface.processingError(new NoLocationAvailableException("No hay informacion de entrenamiento", Optional.empty()));
            return;
        }
        process(ctx, null, iface);
    }

    protected void scanSuccess()  {
        WifiManager wifiManager = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        List<ScanResult> wifiList = wifiManager.getScanResults();
        Collections.sort(wifiList, (ScanResult sc1, ScanResult sc2) ->  { return new Integer(sc2.level).compareTo(new Integer(sc1.level)); } );
        if (wifiList.size()==0) {
            return;
        }
        if (evaluatingWhereAmI) {
            estimateLocation(wifiList);
        } else {
            try {
                // recuperar o crear nueva location para el training set
                Location targetLocation =
                        trainingSet.getLocations().stream()
                                .filter(loc -> loc.getName().equalsIgnoreCase(location.toLowerCase()))
                                .findFirst()
                                .orElseGet(() -> {
                                    Location newLocation = new Location(location.toLowerCase());
                                    trainingSet.getLocations().add(newLocation);
                                    return newLocation;
                                });
                // Carga de los resultados del scan
                for (ScanResult scanResult : wifiList) {
                    int level = WifiManager.calculateSignalLevel(scanResult.level, MAX_LEVELS);
                    targetLocation.getScanDetails().add(new ScanDetail(scanResult.BSSID, level));
                }
            } catch (Exception e) {
                iface.processingError(new NoLocationAvailableException("Error procesando resultados. ", Optional.of(e)));
                return;
            }
            notifyObservers();
            iface.trainingCompleted("Finalizado");
        }
    }

    protected void estimateLocation(List<ScanResult> wifiList)  {
        String minLoc = null;
        Integer minValue = Integer.MAX_VALUE;

        try {
            for (Location location : trainingSet.getLocations()) {
                String curMinLoc = location.getName();
                Integer curMinValue = 0;
                for (ScanResult result : wifiList) {
                    curMinValue += location.getScanDetails().stream()
                            .filter(scan -> scan.getBbsid().equalsIgnoreCase(result.BSSID))
                            .reduce(0, (acc, scan) -> acc + Math.abs(scan.getSs() - result.level), Integer::sum);
                }
                // Tenemos un nuevo mínimo?
                if (curMinValue < minValue) {
                    minValue = curMinValue;
                    minLoc = curMinLoc;
                }
            }

            if (minLoc == null) {
                iface.processingError(new NoLocationAvailableException("No se ha podido determinar la ubicacion", Optional.empty()));
                return;
            }
        } catch (Exception e) {
            iface.processingError(new NoLocationAvailableException("Error en estimacion: ", Optional.of(e)));
            return;
        }
        iface.estimationCompleted(minLoc);
    }

    @Override
    public TrainingSet getTrainingSet() {
        return trainingSet;
    }

    public void setTrainingSet(TrainingSet ts) {
        trainingSet = ts;
        notifyObservers();
    }

    @Override
    public void emptyTrainingSet() {
        trainingSet = new TrainingSet();
        notifyObservers();
    }

    public void notifyObservers() {
        observers.forEach(o -> o.update());
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }
}
