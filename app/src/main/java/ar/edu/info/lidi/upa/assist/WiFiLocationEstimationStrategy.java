package ar.edu.info.lidi.upa.assist;

import java.util.List;

import ar.edu.info.lidi.upa.model.ScanDetail;

public interface WiFiLocationEstimationStrategy {

    String perform(List<ScanDetail> wifiList) throws Exception;
}
