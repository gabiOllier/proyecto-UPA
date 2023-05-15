package ar.edu.info.lidi.upa.utils;

import android.os.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

import ar.edu.info.lidi.upa.assist.PositionAssistanceInterface;
import ar.edu.info.lidi.upa.model.TrainingSet;

public class JSONImporter {

    public void importIt(String filename, PositionAssistanceInterface iface) throws Exception {
        File file = new File(filename);
        ObjectMapper mapper = new ObjectMapper();
        TrainingSet ts = mapper.readValue(file, TrainingSet.class);
        iface.setTrainingSet(ts);
    }
}
