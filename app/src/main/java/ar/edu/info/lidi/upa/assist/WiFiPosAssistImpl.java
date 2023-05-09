package ar.edu.info.lidi.upa.assist;

import ar.edu.info.lidi.upa.exception.NoLocationAvailableException;
import ar.edu.info.lidi.upa.exception.TrainingProcessingException;

public class WiFiPosAssistImpl implements PosAssistInterface {
    @Override
    public void train(String location, int iterations) throws TrainingProcessingException {
        // TODO
    }

    @Override
    public String locate() throws NoLocationAvailableException {
        return "Oficina";
    }
}
