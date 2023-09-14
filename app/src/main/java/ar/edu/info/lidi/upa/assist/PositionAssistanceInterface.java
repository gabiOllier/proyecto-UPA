package ar.edu.info.lidi.upa.assist;

import android.content.Context;

import ar.edu.info.lidi.upa.common.Observer;
import ar.edu.info.lidi.upa.model.TrainingSet;

public interface PositionAssistanceInterface {

    /**
     * Entrenamiento de una ubicacion en particular
     * @param ctx contexto base
     * @param location nombre de la ubicación a entrenar
     * @param iface clase encargada de consumir el resultado del procesamiento
     */
    void train(Context ctx, String location, ProcessCompletedCallBackInterface iface);

    /**
     * Estimación de la posición actual
     * @param ctx contexto base
     * @param iface clase encargada de consumir el resultado del procesamiento
     */
    void locate(Context ctx, ProcessCompletedCallBackInterface iface);

    /**
     * @return los datos del entrenamiento
     */
    TrainingSet getTrainingSet();

    /**
     * @return el location asignado al procesamiento
     */
    String getLocation();

    /**
     * Asignacion de datos de entrenamiento
     */
    void setTrainingSet(TrainingSet ts);

    /**
     * Eliminacion de datos de entrenamiento
     */
    void emptyTrainingSet();

    /**
     * Observer del modelo de entrenamiento
     * @param o observer
     */
    void addObserver(Observer o);

    /**
     * Notificar a los observadores sobre cambio en el modelo
     */
    public void notifyObservers();

    /* Estrategia para la determinacion de ubicacion */
    void setStrategy(WiFiLocationEstimationStrategy strategy);

}
