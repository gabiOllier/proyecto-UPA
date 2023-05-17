package ar.edu.info.lidi.upa.exception;

import java.util.Optional;

/** Imposible recuperar una ubicacion */
public class NoLocationAvailableException extends ProcessingException {

    public NoLocationAvailableException(String message, Optional<Exception> e) {
        super(message, e);
    }
}
