package ar.edu.info.lidi.upa.assist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ar.edu.info.lidi.upa.model.Location;
import ar.edu.info.lidi.upa.model.ScanDetail;
import ar.edu.info.lidi.upa.utils.Constants;
import ar.edu.info.lidi.upa.utils.SignalUtils;

public class WiFiEuclideanLocationStrategy implements WiFiLocationEstimationStrategy {

    PositionAssistanceInterface pai;

    public WiFiEuclideanLocationStrategy(PositionAssistanceInterface pai) {
        this.pai = pai;
    }

    /*--------------------*/
    protected List<String> possibleLocation;

    @Override
    public String perform(List<ScanDetail> wifiList) throws Exception {
        return euclideanDistance(wifiList);
    }

    //Recorre todas las locacione guardadas. Se obtiene la distancia euclidiana entre el punto donde se encuentra el usuario hasta la locacion, utilizando
    //las 4 senales mas fuertes que se muestrean desde la locacion actual
    public String euclideanDistance(List<ScanDetail> userLocation) {
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

        for (Location location : pai.getTrainingSet().getLocations()) {
            actualLocation = location.getName();
            acc = 0;
            flag = 0;
            distance = Integer.MAX_VALUE;
            System.out.println("--------------------------------------------------------------");
            System.out.println(actualLocation);
            for (ScanDetail dataUserLocation: userLocation) {
                String bssid = dataUserLocation.getBbsid();
                //if((bssid.equalsIgnoreCase("f4:69:42:7e:78:ff")) || (bssid.equalsIgnoreCase( "f8:35:dd:65:ab:c8"))) {
                System.out.println("ENTRO");
                x1 = SignalUtils.normalize(dataUserLocation.getLevel());
                if (x1 < SignalUtils.MIN_ACC_LEVEL)
                    continue;
                if (location.getScanDetails().stream().filter(scan -> scan.getBbsid().equalsIgnoreCase(bssid)).count() == 0) {
                    x2 = 1;
                } else {
                    x2 = location.getScanDetails().stream().filter(scan -> scan.getBbsid().equalsIgnoreCase(bssid)).findFirst().get().getLevel();
                    flag++;
                }
                System.out.println("BSSID = " + dataUserLocation.getBbsid());
                System.out.println("x1 = " + x1 + " Ubicación Usuario");
                System.out.println("x2 = " + x2 + " Locación");
                diffx = x2 - x1;
                diffXSquared = Math.pow(diffx, 2);
                System.out.println("diffXSquared = " + diffXSquared);
                acc = acc + diffXSquared;
                //}
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

    public String signalComparator(List<ScanDetail> userLocation) {
        Integer x1;
        Integer x2;
        double maxPorcentaje = 0;
        String locActual = null;
        double acc;

        String actualLocation = null;
        String resultado = null;
        int flag;

        //Se pasa locacion por locacion
        for (Location location : pai.getTrainingSet().getLocations()) {
            actualLocation = location.getName();
            acc = 0;
            flag = 0;
            System.out.println("--------------------------------------------------------------");
            System.out.println(actualLocation);

            //Se pasa señal por señal
            for (ScanDetail dataUserLocation: userLocation) {
                String bssid = dataUserLocation.getBbsid();
                //Se obtiene la intensidad de la señal y se filtran los mas bajos.
                x1 = SignalUtils.normalize(dataUserLocation.getLevel());
                if (x1 < SignalUtils.MIN_ACC_LEVEL)
                    continue;
                //Si la señal no esta en la locación se pone el valor 1
                if (location.getScanDetails().stream().filter(scan -> scan.getBbsid().equalsIgnoreCase(bssid)).count() == 0) {
                    x2 = 1;
                    //Si la señal esta en la locación se pone el valor de la locación y se cuenta +1 en flag
                } else {
                    x2 = location.getScanDetails().stream().filter(scan -> scan.getBbsid().equalsIgnoreCase(bssid)).findFirst().get().getLevel();
                    flag++;
                }
                System.out.println("BSSID = " + dataUserLocation.getBbsid());
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
