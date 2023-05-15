package ar.edu.info.lidi.upa.utils;

import android.content.ClipboardManager;
import android.os.Environment;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.FileOutputStream;

import ar.edu.info.lidi.upa.model.TrainingSet;

public class JSONExporter {

    public String toJSON(TrainingSet ts) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        prettyPrinter.indentObjectsWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        mapper.setDefaultPrettyPrinter(prettyPrinter);
        return mapper.writeValueAsString(ts);
    }

}
