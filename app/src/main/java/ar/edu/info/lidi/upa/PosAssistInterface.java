package ar.edu.info.lidi.upa;

public interface PosAssistInterface {

    /**
     * Entrenamiento de una ubicacion en particular
     * @param location nombre de la ubicación a entrenar
     */
    public void train(String location);

    /**
     * Estimación de la posición actual
     * @return nombre de la posición actual
     */
    public String locate();
}
