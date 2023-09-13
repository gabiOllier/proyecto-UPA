package ar.edu.info.lidi.upa.mapper;

import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.List;

import ar.edu.info.lidi.upa.model.ScanDetail;
import ar.edu.info.lidi.upa.utils.Constants;
import ar.edu.info.lidi.upa.utils.SignalUtils;

public class ScanDetailMapper {

    public static List<ScanDetail> fromScanResult(List<ScanResult> list) {
        List<ScanDetail> newList = new ArrayList<>();
        list.forEach(sr -> newList.add(new ScanDetail(sr.BSSID, SignalUtils.normalize(sr.level), sr.level)));
        return newList;
    }
}
