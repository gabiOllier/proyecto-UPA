package ar.edu.info.lidi.upa.exception;

import java.util.Optional;

public class ProcessingException extends Exception {

    protected Optional<Exception> exDetails;

    public ProcessingException(String message, Optional<Exception> ex) {
        super(message);
        this.exDetails = ex;
    }

    public Optional<Exception> getExDetails() {
        return exDetails;
    }
}
