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

/*----------------*/
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
/*----------------*/

public class WiFiPositionAssistanceImpl implements PositionAssistanceInterface {

    /** Niveles de intensidad normalizado a porcentaje */
    static final int MAX_LEVELS = 100;
    /** Umbral aceptable */
    static final int MIN_SS = 60;       //////
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

    /*--------------------*/
    protected List<String> possibleLocation;

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
        List<ScanResult> wifiList = wifiManager.getScanResults();
        Collections.sort(wifiList, (ScanResult sc1, ScanResult sc2) ->  { return new Integer(sc2.level).compareTo(new Integer(sc1.level)); } );
                System.out.println("-------------------WIFILIST-----------------------");
                for (ScanResult scan: wifiList) {              ////////////////////////////////
                    System.out.println("RED: " + scan.BSSID + " INTENSIDAD: " + WifiManager.calculateSignalLevel(scan.level, MAX_LEVELS));
                }
                System.out.println("----------------------------------------------------");
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
                    if ((level >= MIN_SS)) //&& (targetLocation.getScanDetails().size() < 10))
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
            //String target = findNearestLocation(wifiList);
            String target = euclideanDistance(wifiList);
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

/*-------------------------------------------------------------------------------------------*/

    //Ordena la lista de señales wifi de la posicion actual del usuario de forma descendente.
    public static void sortWiFiList(List<ScanResult> userLocation){
        Collections.sort(userLocation,new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult scanResult1, ScanResult scanResult2) {
                // Compara los niveles de señal de los ScanResult en orden descendente
                return Integer.compare(scanResult2.level, scanResult1.level);
            }
        });
    }

    public static void sortWiFiList2(List<ScanDetail> Location){
        Collections.sort(Location,new Comparator<ScanDetail>() {
            @Override
            public int compare(ScanDetail scanDetail1, ScanDetail scanDetail2) {
                // Compara los niveles de señal de los ScanResult en orden descendente
                return Integer.compare(scanDetail2.getSs(), scanDetail1.getSs());
            }
        });
    }

    //Recorre todas las locacione guardadas. Se obtiene la distancia euclidiana entre el punto donde se encuentra el usuario hasta la locacion, utilizando
    //las 4 senales mas fuertes que se muestrean desde la locacion actual
    public String euclideanDistance(List<ScanResult> userLocation) {
        Integer x1;
        Integer x2;
        Integer diffx;

        double diffXSquared;
        double acc;

        double distance;
        double minDistance = Integer.MAX_VALUE;
        double minDistance2 = 0;

        String actualLocation = null;
        String minLocation = null;
        String minLocation2 = null;
        String resultado = null;
        int flag;

        for (Location location : trainingSet.getLocations()) {
            actualLocation = location.getName();
            acc = 0;
            flag = 0;
            distance = Integer.MAX_VALUE;
            System.out.println("--------------------------------------------------------------");
            System.out.println(actualLocation);
            for (ScanResult dataUserLocation: userLocation) {
                String bssid = dataUserLocation.BSSID;
                if((bssid.equalsIgnoreCase("f4:69:42:7e:78:ff")) || (bssid.equalsIgnoreCase( "f8:35:dd:65:ab:c8"))) {
                    System.out.println("ENTRO");
                    x1 = WifiManager.calculateSignalLevel(dataUserLocation.level, MAX_LEVELS);
                    if (x1 < MIN_SS)
                        continue;
                    if (location.getScanDetails().stream().filter(scan -> scan.getBbsid().equalsIgnoreCase(bssid)).count() == 0) {
                        x2 = 1;
                    } else {
                        x2 = location.getScanDetails().stream().filter(scan -> scan.getBbsid().equalsIgnoreCase(bssid)).findFirst().get().getSs();
                        flag++;
                    }
                    System.out.println("BSSID = " + dataUserLocation.BSSID);
                    System.out.println("x1 = " + x1 + " Ubicación Usuario");
                    System.out.println("x2 = " + x2 + " Locación");
                    diffx = x2 - x1;
                    diffXSquared = Math.pow(diffx, 2);
                    System.out.println("diffXSquared = " + diffXSquared);
                    acc = acc + diffXSquared;
                }
            }
            System.out.println("Acumulador = " + acc);
            if((acc != 0)||(flag != 0)){
                distance = Math.sqrt(acc);
            }
            System.out.println("Distancia a " + actualLocation + " = " + distance);
            System.out.println("--------------------------------------------------------------");
            if(distance < minDistance){
                minDistance2 = minDistance;
                minDistance = distance;     //se actualiza la minima distancia.
                minLocation2 = minLocation;
                minLocation = actualLocation;
            }
            if ((minLocation2 != null)&&(minDistance2 - 10) >= minDistance){
                resultado = "Cerca de " + minLocation2 + " y " + minLocation;
            }else {
                resultado = minLocation;
            }
        }
        System.out.println("Locacion mas cercana " + minLocation + " a " + minDistance + " distancia");
        return resultado;
    }

    /*----------------------------Metodos para varias iteraciones (ScanDetails)---------------------------------*/

    // Método para obtener las medianas de intensidad por bssid
    public List<ScanDetail> obtenerMedianasPorBssid(List<ScanDetail> WifiList) {
        List<ScanDetail> medianasPorBssid = new ArrayList<>();
        Map<String, List<Integer>> intensidadesPorBssid = new HashMap<>();

        // Agrupar señales por bssid
        for (ScanDetail scan : WifiList) {
            if (!intensidadesPorBssid.containsKey(scan.getBbsid())) {
                intensidadesPorBssid.put(scan.getBbsid(), new ArrayList<>());
            }
            intensidadesPorBssid.get(scan.getBbsid()).add(scan.getSs());
        }

        for (String nombre : intensidadesPorBssid.keySet()) {
            List<Integer> intensidades = intensidadesPorBssid.get(nombre);
            Collections.sort(intensidades);

            int mediana;
            int tamaño = intensidades.size();
            if (tamaño % 2 == 0) {
                int valorMedio1 = intensidades.get(tamaño / 2 - 1);
                int valorMedio2 = intensidades.get(tamaño / 2);
                mediana = (valorMedio1 + valorMedio2) / 2;
            } else {
                mediana = intensidades.get(tamaño / 2);
            }

            medianasPorBssid.add(new ScanDetail(nombre,mediana));
        }
        sortWiFiList2(medianasPorBssid);

        for (ScanDetail scan: medianasPorBssid) {
            System.out.println("RED : " + scan.getBbsid() + " , " + scan.getSs());
        }
        System.out.println("Tamaño de lista : " + medianasPorBssid.size());
        return medianasPorBssid;
    }

    public void medianas(){
        Location targetLocation = trainingSet.getLocations().stream().filter(loc -> loc.getName().equalsIgnoreCase(location.toLowerCase())).findFirst().get();
        targetLocation.setScanDetails(obtenerMedianasPorBssid(targetLocation.getScanDetails()));
        notifyObservers();
    }

    public double porcentajeDeCoincidencia(int x1, int x2)
    {
        if (x1 == x2) {
            return 100.0;
        }
        int diferencia = Math.abs(x1 - x2);
        double maximoValor = Math.max(x1, x2);

        double porcentaje = (1 - (diferencia / maximoValor)) * 100;
        return porcentaje;
    }

    public String signalComparator(List<ScanResult> userLocation) {
        Integer x1;
        Integer x2;
        double maxPorcentaje = 0;
        String locActual = null;
        double acc;

        String actualLocation = null;
        String resultado = null;
        int flag;

        //Se pasa locacion por locacion
        for (Location location : trainingSet.getLocations()) {
            actualLocation = location.getName();
            acc = 0;
            flag = 0;
            System.out.println("--------------------------------------------------------------");
            System.out.println(actualLocation);

            //Se pasa señal por señal
            for (ScanResult dataUserLocation: userLocation) {
                String bssid = dataUserLocation.BSSID;
                    //Se obtiene la intensidad de la señal y se filtran los mas bajos.
                    x1 = WifiManager.calculateSignalLevel(dataUserLocation.level, MAX_LEVELS);
                    if (x1 < MIN_SS)
                        continue;
                    //Si la señal no esta en la locación se pone el valor 1
                    if (location.getScanDetails().stream().filter(scan -> scan.getBbsid().equalsIgnoreCase(bssid)).count() == 0) {
                        x2 = 1;
                    //Si la señal esta en la locación se pone el valor de la locación y se cuenta +1 en flag
                    } else {
                        x2 = location.getScanDetails().stream().filter(scan -> scan.getBbsid().equalsIgnoreCase(bssid)).findFirst().get().getSs();
                        flag++;
                    }
                    System.out.println("BSSID = " + dataUserLocation.BSSID);
                    System.out.println("x1 = " + x1 + " Ubicación Usuario");
                    System.out.println("x2 = " + x2 + " Locación");
                    //Se ve el porcentaje de coincidencia de la señal y se suma en el acumulador
                    acc = porcentajeDeCoincidencia(x1,x2);
            }
            System.out.println("Acumulador = " + acc);
            acc = acc % userLocation.size();
            System.out.println("Porcentaje de igualdad = " + acc);

            // Si coinciden todas las señales con la locacion
            if( flag == userLocation.size())
            {

            }
            else
            {

            }


            if( acc > maxPorcentaje)
            {
                maxPorcentaje = acc;
                locActual = actualLocation;
            }
        }
        return resultado;
    }
}
