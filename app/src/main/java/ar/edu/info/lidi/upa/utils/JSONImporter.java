package ar.edu.info.lidi.upa.utils;

import android.os.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

import ar.edu.info.lidi.upa.assist.PositionAssistanceInterface;
import ar.edu.info.lidi.upa.model.TrainingSet;

public class JSONImporter {

    public TrainingSet fromJSON(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, TrainingSet.class);
    }
}
