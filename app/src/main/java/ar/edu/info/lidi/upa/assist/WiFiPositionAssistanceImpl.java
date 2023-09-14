package ar.edu.info.lidi.upa.assist;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import ar.edu.info.lidi.upa.common.Observer;
import ar.edu.info.lidi.upa.exception.NoLocationAvailableException;
import ar.edu.info.lidi.upa.exception.TrainingProcessingException;
import ar.edu.info.lidi.upa.mapper.ScanDetailMapper;
import ar.edu.info.lidi.upa.model.Location;
import ar.edu.info.lidi.upa.model.ScanDetail;
import ar.edu.info.lidi.upa.model.TrainingSet;
import ar.edu.info.lidi.upa.utils.SignalUtils;


public class WiFiPositionAssistanceImpl implements PositionAssistanceInterface {

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

    protected WiFiLocationEstimationStrategy strategy;


    protected WiFiLocationEstimationStrategy getStrategy() {
        if (strategy==null)
            strategy = new WiFiNearestLocationStrategy(this);
        return strategy;
    }

    public void setStrategy(WiFiLocationEstimationStrategy strategy) {
        this.strategy = strategy;
    }

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
                    iface.processingError(new TrainingProcessingException("Error durante el entrenamiento. Reintente en un minuto 15 segundos.", Optional.empty()));
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
        List<ScanDetail> wifiList = ScanDetailMapper.fromScanResult(wifiManager.getScanResults());
        Collections.sort(wifiList, (ScanDetail sc1, ScanDetail sc2) -> new Integer(sc2.getLevel()).compareTo(new Integer(sc1.getLevel())));
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
                for (ScanDetail scanResult : wifiList) {
                    // Considerar unicamente escaneos por encima de cierto umbral aceptable
                    if ((scanResult.getLevel() >= SignalUtils.MIN_ACC_LEVEL)) //&& (targetLocation.getScanDetails().size() < 10))
                        targetLocation.getScanDetails().add(new ScanDetail(scanResult.getBbsid(), scanResult.getLevel(), scanResult.getRssi()));
                }
            } catch (Exception e) {
                iface.processingError(new NoLocationAvailableException("Error procesando resultados. ", Optional.of(e)));
                return;
            }
            notifyObservers();
            iface.trainingCompleted("Finalizado");
        }
    }

    /**
     * Estimación de la ubicación y notificación del evento resultante
     * @param wifiList escaneo actual
     * @throws NoLocationAvailableException ante un error o si el entrenamiento no contiene informacion para estimar
     */
    protected String estimateLocation(List<ScanDetail> wifiList)  {
        try {
            String target = getStrategy().perform(wifiList);
            if (target == null) {
                iface.processingError(new NoLocationAvailableException("No se ha podido determinar la ubicacion", Optional.empty()));
                return null;
            }
            iface.estimationCompleted(target);
            return target;
        } catch (Exception e) {
            iface.processingError(new NoLocationAvailableException("Error en estimacion: ", Optional.of(e)));
            return null;
        }
    }

    @Override
    public TrainingSet getTrainingSet() {
        return trainingSet;
    }

    @Override
    public String getLocation() {
        return location;
    }

    public void setTrainingSet(TrainingSet ts) {
        trainingSet = ts;
        notifyObservers();
    }

    public void setIface(ProcessCompletedCallBackInterface iface) {
        this.iface = iface;
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
