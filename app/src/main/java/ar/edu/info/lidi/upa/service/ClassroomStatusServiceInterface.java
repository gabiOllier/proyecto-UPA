package ar.edu.info.lidi.upa.service;

import ar.edu.info.lidi.upa.exception.ProcessingException;

public interface ClassroomStatusServiceInterface {

    /**
     * Recupera la actividad actual del aula indicada por su nombre como argumento
     * @param name el nombre del aula sobre la cual buscar información
     * @return el String conteniendo la actividad. En caso de no existir informacion retornará un mensaje el mensaje correspondiente (no es un error)
     * @throws ProcessingException si por ejemplo no puede conectar contra el servicio
     */
    public String getCurrentStateFor(String name) throws ProcessingException;
}
