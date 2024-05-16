package ar.edu.info.lidi.upa.common;

import ar.edu.info.lidi.upa.service.ClassroomStatusServiceImpl;
import ar.edu.info.lidi.upa.service.ClassroomStatusServiceInterface;

public class ClassroomStatusFactory {

    public static ClassroomStatusServiceInterface instance = null;

    public static ClassroomStatusServiceInterface getInstance() {
        if (instance == null) {
            instance = new ClassroomStatusServiceImpl();
        }
        return instance;
    }

}
