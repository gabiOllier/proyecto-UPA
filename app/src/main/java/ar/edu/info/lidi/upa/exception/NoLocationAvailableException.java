package ar.edu.info.lidi.upa.exception;

/** Imposible recuperar una ubicacion */
public class NoLocationAvailableException extends Exception {

    public NoLocationAvailableException(String message) {
        super(message);
    }
}
