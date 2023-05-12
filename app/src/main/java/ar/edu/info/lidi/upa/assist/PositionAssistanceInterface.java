package ar.edu.info.lidi.upa.assist;

import android.content.Context;

import ar.edu.info.lidi.upa.exception.NoLocationAvailableException;
import ar.edu.info.lidi.upa.exception.TrainingProcessingException;

public interface PositionAssistanceInterface {

    /**
     * Entrenamiento de una ubicacion en particular
     * @param ctx contexto base
     * @param location nombre de la ubicación a entrenar
     * @param iface clase encargada de consumir el resultado del procesamiento
     */
    public void train(Context ctx, String location, ProcessCompletedCallBackInterface iface);

    /**
     * Estimación de la posición actual
     * @param ctx contexto base
     * @param iface clase encargada de consumir el resultado del procesamiento
     */
    public void locate(Context ctx, ProcessCompletedCallBackInterface iface);
}
