package ar.edu.info.lidi.upa.assist;

import ar.edu.info.lidi.upa.exception.NoLocationAvailableException;
import ar.edu.info.lidi.upa.exception.TrainingProcessingException;

public interface PosAssistInterface {

    /**
     * Entrenamiento de una ubicacion en particular
     * @param location nombre de la ubicación a entrenar
     * @param iterations numero de iteraciones para el entrenamiento
     * @throws TrainingProcessingException en caso de error durante la ejecucion del entrenamiento
     */
    public void train(String location, int iterations) throws TrainingProcessingException;

    /**
     * Estimación de la posición actual
     * @return nombre de la posición actual
     * @throws NoLocationAvailableException en caso de no disponer de una ubicación a retornar
     */
    public String locate() throws NoLocationAvailableException;
}
