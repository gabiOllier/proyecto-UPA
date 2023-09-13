package ar.edu.info.lidi.upa.common;

import ar.edu.info.lidi.upa.assist.PositionAssistanceInterface;
import ar.edu.info.lidi.upa.assist.WiFiNearestLocationStrategy;
import ar.edu.info.lidi.upa.assist.WiFiPositionAssistanceImpl;

public class PositionAssistanceFactory {

    public static PositionAssistanceInterface instance = null;

    public static PositionAssistanceInterface getInstance() {
        if (instance == null) {
            instance = new WiFiPositionAssistanceImpl();
            instance.setStrategy(new WiFiNearestLocationStrategy(instance));
        }
        return instance;
    }
}
