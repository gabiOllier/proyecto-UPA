package ar.edu.info.lidi.upa.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

import android.util.LruCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ar.edu.info.lidi.upa.exception.ProcessingException;
import ar.edu.info.lidi.upa.model.CurrentState;

public class ClassroomStatusServiceImpl implements ClassroomStatusServiceInterface {


    private static final long CACHE_EXPIRATION_TIME = TimeUnit.MINUTES.toMillis(15);
    private LruCache<String, Object> cache = new LruCache<>(4 * 1024 * 1024);
    private static final String STATE_KEY = "STATE";

    @Override
    public String getCurrentStateFor(String name) throws ProcessingException {
        try {
            CurrentState cs = queryCurrentState();
            for (CurrentState.Reserva r : cs.getReservas()) {
                if (name.equalsIgnoreCase(r.getAula().getNombre()))
                    return getDetailsFor(r);
            }
            return "Sin informacion de aulas";
        } catch (Exception e) {
            throw new ProcessingException(e.getMessage(), Optional.of(e));
        }
    }

    protected CurrentState queryCurrentState() throws Exception {
        CurrentState cachedState = getData(STATE_KEY);
        if (cachedState!=null)
            return cachedState;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<CurrentState> future = executor.submit(() -> {

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                // Establecer la URL de la API
                URL url = new URL("http://gestiondocente.info.unlp.edu.ar/reservas/api/consulta/estadoactual");

                // Abrir la conexión HTTP
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Leer la respuesta de la API
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                // Parsear la respuesta JSON utilizando Jackson
                ObjectMapper objectMapper = new ObjectMapper();
                CurrentState.Reserva[] reservas = objectMapper.readValue(response.toString(), CurrentState.Reserva[].class);
                CurrentState cs = new CurrentState();
                cs.setReservas(reservas);
                putData(STATE_KEY, cs);
                return cs;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Cerrar las conexiones
                if (connection != null) {
                    connection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

        });

        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private boolean isCacheExpired(CacheEntry entry) {
        long currentTime = System.currentTimeMillis();
        return (currentTime - entry.getTimestamp()) > CACHE_EXPIRATION_TIME;
    }

    public void putData(String key, CurrentState data) {
        cache.put(normalizeKey(key), new CacheEntry(data));
    }

    public CurrentState getData(String key) {
        CacheEntry entry = (CacheEntry) cache.get(normalizeKey(key));
        if (entry != null && !isCacheExpired(entry)) {
            return entry.getData();
        }
        return null;
    }


    private class CacheEntry {
        private CurrentState data;
        private long timestamp;

        public CacheEntry(CurrentState data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public CurrentState getData() {
            return data;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    protected String normalizeKey(String key) {
        return key.toLowerCase().replace(" ", "");
    }

    protected String getDetailsFor(CurrentState.Reserva r) {
        StringBuffer ret = new StringBuffer();
        ret
                .append("Materia ")
                .append(r.getMateria().getNombre())
                .append(". ")

                .append("Docente ")
                .append(r.getDocente().getNombre())
                .append(r.getDocente().getApellido())
                .append(". ")

                .append("De ")
                .append(r.getHoraDesde().getH())
                .append(" ")
                .append(r.getHoraDesde().getM())

                .append(" horas a ")
                .append(r.getHoraHasta().getH())
                .append(" ")
                .append(r.getHoraHasta().getM())
                .append(" horas. ");

        return ret.toString();
    }

}
