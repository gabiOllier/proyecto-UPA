package ar.edu.info.lidi.upa.utils;

public class Constants {

    public static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 3;

    public static final String SHARED_PREFERENCES_NAME = "prefs";


    /** Numero de iteraciones a realizar */
    public static final String PREFERENCE_ITERATIONS = "ITERATIONS";
    /** Ubicacion */
    public static final String PREFERENCE_LOCATION = "LOCATION";
    /** Datos de entrenamiento */
    public static final String PREFERENCE_DATA = "DATOS";

    /** Status solo texto */
    public static final Integer OUTPUT_TEXT  = 1;
    /** Status solo audio */
    public static final Integer OUTPUT_AUDIO = 2;
    /** Status texto y audio */
    public static final Integer OUTPUT_BOTH  = 3;

}
