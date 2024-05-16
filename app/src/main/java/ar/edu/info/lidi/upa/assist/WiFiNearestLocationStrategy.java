package ar.edu.info.lidi.upa.assist;

import java.util.List;

import ar.edu.info.lidi.upa.model.Location;
import ar.edu.info.lidi.upa.model.ScanDetail;
import ar.edu.info.lidi.upa.utils.SignalUtils;

public class WiFiNearestLocationStrategy implements WiFiLocationEstimationStrategy {

    PositionAssistanceInterface pai;

    public WiFiNearestLocationStrategy(PositionAssistanceInterface pai) {
        this.pai = pai;
    }

    @Override
    public String perform(List<ScanDetail> wifiList) throws Exception {
        return findNearestLocation(wifiList);
    }

    /**
     * Buscar el mejor match entre el scan actual y el entrenamiento previo
     * @param wifiList escaneo actual
     * @return nombre de la ubicacion con menor distanci
     * @throws Exception en caso de error en el procesamiento
     */
    public String findNearestLocation(List<ScanDetail> wifiList) throws Exception {

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

        for (Location location : pai.getTrainingSet().getLocations()) {
            String curMinLoc = location.getName();
            Integer curMinValue = 0;
            for (ScanDetail result : wifiList) {
                // Omitir niveles de señal bajos
                if (result.getLevel() < SignalUtils.MIN_ACC_LEVEL)
                    continue;
                curMinValue += location.getScanDetails().stream()
                        .filter(scan -> scan.getBbsid().equalsIgnoreCase(result.getBbsid()))
                        .reduce(0, (acc, scan) -> acc + Math.abs(scan.getLevel() - result.getLevel()), Integer::sum );
            }
            // Tenemos un nuevo mínimo?
            if (curMinValue < minValue) {
                minValue = curMinValue;
                minLoc = curMinLoc;
            }
        }
        return minLoc;
    }

}
