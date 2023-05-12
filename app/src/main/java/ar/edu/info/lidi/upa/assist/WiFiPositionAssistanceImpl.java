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

import java.util.Collections;
import java.util.List;

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

    public void process(Context ctx, String location)  {
        WifiManager wifiManager = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent)  {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess(ctx, location);
                } else {
                    iface.processingError(new TrainingProcessingException("Error durante el entrenamiento. Reintente."));
                    return;
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
            // scan failure handling
            iface.processingError(new TrainingProcessingException("Error al iniciar entrenamiento. Demasiados intentos o validar permisos."));
            return;
        }
    }

    @Override
    public void train(Context ctx, String location, ProcessCompletedCallBackInterface iface)  {
        this.iface = iface;
        evaluatingWhereAmI = false;
        process(ctx, location);
    }

    @Override
    public void locate(Context ctx, ProcessCompletedCallBackInterface iface) {
        this.iface = iface;
        evaluatingWhereAmI = true;
        process(ctx, null);
    }

    protected void scanSuccess(Context ctx, String location)  {
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
                        trainingSet.locations.stream()
                                .filter(loc -> loc.name.equalsIgnoreCase(location.toLowerCase()))
                                .findFirst()
                                .orElseGet(() -> {
                                    Location newLocation = new Location(location.toLowerCase());
                                    trainingSet.locations.add(newLocation);
                                    return newLocation;
                                });

                // Carga de los resultados del scan
                for (ScanResult scanResult : wifiList) {
                    int level = WifiManager.calculateSignalLevel(scanResult.level, MAX_LEVELS);
                    targetLocation.scanDetails.add(new ScanDetail(scanResult.BSSID, level));
                }
            } catch (Exception e) {
                iface.processingError(new NoLocationAvailableException("Error en entrenamiento"));
                return;
            }
            iface.trainingCompleted("Finalizado");
        }
    }

    protected void estimateLocation(List<ScanResult> wifiList)  {
        String minLoc = null;
        Integer minValue = Integer.MAX_VALUE;

        for (Location location : trainingSet.locations) {
            String curMinLoc = location.name;
            Integer curMinValue = 0;
            for (ScanResult result : wifiList) {
                curMinValue += location.scanDetails.stream()
                        .filter(scan -> scan.bbsid.equalsIgnoreCase(result.BSSID))
                        .reduce(0, (acc, scan) -> acc + Math.abs(scan.signalStrength - result.level), Integer::sum);
            }
            // Tenemos un nuevo mínimo?
            if (curMinValue < minValue) {
                minValue = curMinValue;
                minLoc = curMinLoc;
            }
        }

        if (minLoc == null) {
            iface.processingError(new NoLocationAvailableException("No se ha podido determinar la ubicacion"));
            return;
        }
        iface.estimationCompleted(minLoc);
    }

}
