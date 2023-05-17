package ar.edu.info.lidi.upa.exception;

import java.util.Optional;

/** Error al realizar el entrenamiento */
public class TrainingProcessingException extends ProcessingException {


    public TrainingProcessingException(String message, Optional<Exception> e) {
        super(message, e);
    }
}
