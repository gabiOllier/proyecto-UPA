package ar.edu.info.lidi.upa.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ar.edu.info.lidi.upa.model.ScanDetail;

public class SignalUtils {

    /** Anything worse than or equal to this will show 0 bars. */
    public static final int MIN_RSSI = -100;
    /** Anything better than or equal to this will show the max bars. */
    public static final int MAX_RSSI = -55;
    /** Umbral aceptable de señal */
    public static final int MIN_ACC_LEVEL = 60;
    public static final int NUM_LEVELS = 100;

    /**
     * Calculates the level of the signal. This should be used any time a signal
     * is being shown.
     *
     * @param rssi The power of the signal measured in RSSI.
     * @return A level of the signal, given in the range of 0 to numLevels-1
     *         (both inclusive).
     */
    public static int normalize(int rssi) {
        if (rssi <= MIN_RSSI) {
            return 0;
        } else if (rssi >= MAX_RSSI) {
            return NUM_LEVELS - 1;
        } else {
            float inputRange = (MAX_RSSI - MIN_RSSI);
            float outputRange = (NUM_LEVELS - 1);
            return (int)((float)(rssi - MIN_RSSI) * outputRange / inputRange);
        }
    }

    // Método para obtener las medianas de intensidad por bssid
    public static List<ScanDetail> obtenerMedianasPorBssid(List<ScanDetail> WifiList) {
        List<ScanDetail> medianasPorBssid = new ArrayList<>();
        Map<String, List<Integer>> intensidadesPorBssid = new HashMap<>();

        // Agrupar señales por bssid
        for (ScanDetail scan : WifiList) {
            if (!intensidadesPorBssid.containsKey(scan.getBbsid())) {
                intensidadesPorBssid.put(scan.getBbsid(), new ArrayList<>());
            }
            intensidadesPorBssid.get(scan.getBbsid()).add(scan.getLevel());
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
            System.out.println("RED : " + scan.getBbsid() + " , " + scan.getLevel());
        }
        System.out.println("Tamaño de lista : " + medianasPorBssid.size());
        return medianasPorBssid;
    }

    //Ordena la lista de señales wifi de la posicion actual del usuario de forma descendente.
    public static void sortWiFiList(List<ScanDetail> userLocation){
        Collections.sort(userLocation,new Comparator<ScanDetail>() {
            @Override
            public int compare(ScanDetail scanResult1, ScanDetail scanResult2) {
                // Compara los niveles de señal de los ScanResult en orden descendente
                return Integer.compare(scanResult2.getLevel(), scanResult1.getLevel());
            }
        });
    }

    public static void sortWiFiList2(List<ScanDetail> Location){
        Collections.sort(Location,new Comparator<ScanDetail>() {
            @Override
            public int compare(ScanDetail scanDetail1, ScanDetail scanDetail2) {
                // Compara los niveles de señal de los ScanResult en orden descendente
                return Integer.compare(scanDetail2.getLevel(), scanDetail1.getLevel());
            }
        });
    }
}
