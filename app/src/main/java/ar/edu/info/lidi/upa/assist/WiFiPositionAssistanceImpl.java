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
    /** Umbral aceptable */
    static final int MIN_SS = 50;
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
                    // Considerar unicamente escaneos por encima de cierto umbral aceptable
                    if (level >= MIN_SS)
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

    /**
     * Estimación de la ubicación y notificación del evento resultante
     * @param wifiList escaneo actual
     * @throws NoLocationAvailableException ante un error o si el entrenamiento no contiene informacion para estimar
     */
    protected void estimateLocation(List<ScanResult> wifiList)  {
        try {
            String target = findNearestLocation(wifiList);
            if (target == null) {
                iface.processingError(new NoLocationAvailableException("No se ha podido determinar la ubicacion", Optional.empty()));
                return;
            }
            iface.estimationCompleted(target);
        } catch (Exception e) {
            iface.processingError(new NoLocationAvailableException("Error en estimacion: ", Optional.of(e)));
        }
    }

    /**
     * Buscar el mejor match entre el scan actual y el entrenamiento previo
     * @param wifiList escaneo actual
     * @return nombre de la ubicacion con menor distanci
     * @throws Exception en caso de error en el procesamiento
     */
    public String findNearestLocation(List<ScanResult> wifiList) throws Exception {

        /*
         * TODO: Optimizar cálculo de la distancia.
         *          - Considerar diferentes enfoques para mejorar la precisión y eficiencia del cálculo de la distancia,
         *          como utilizar algoritmos de distancia más avanzados (por ejemplo, distancia euclidiana o distancia de Mahalanobis)
         *          o realizar filtrado previo de los puntos de acceso WiFi para limitar el cálculo solo a los más relevantes.
         *          - Considerar la influencia de la intensidad de la señal: ajustar la ponderación de la intensidad de la señal WiFi
         *          en el cálculo de la distancia.  Por ejemplo aplicar un factor de peso a las diferencias de intensidad para
         *          resaltar o atenuar su importancia en la determinación de la ubicación más cercana.
         */
        String minLoc = null;
        Integer minValue = Integer.MAX_VALUE;

        for (Location location : trainingSet.getLocations()) {
            String curMinLoc = location.getName();
            Integer curMinValue = 0;
            for (ScanResult result : wifiList) {
                // Omitir niveles de señal bajos
                if (result.level < MIN_SS)
                    continue;
                curMinValue += location.getScanDetails().stream()
                        .filter(scan -> scan.getBbsid().equalsIgnoreCase(result.BSSID))
                        .reduce(0, (acc, scan) -> acc + Math.abs(scan.getSs() - WifiManager.calculateSignalLevel(result.level, MAX_LEVELS)), Integer::sum );
            }
            // Tenemos un nuevo mínimo?
            if (curMinValue < minValue) {
                minValue = curMinValue;
                minLoc = curMinLoc;
            }
        }
       return minLoc;
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
