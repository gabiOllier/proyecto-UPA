package ar.edu.info.lidi.upa;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import ar.edu.info.lidi.upa.assist.PositionAssistanceInterface;
import ar.edu.info.lidi.upa.assist.ProcessCompletedCallBackInterface;
import ar.edu.info.lidi.upa.assist.WiFiPositionAssistanceImpl;
import ar.edu.info.lidi.upa.tts.TTSListener;
import ar.edu.info.lidi.upa.utils.JSONExporter;

public class MainActivity extends AppCompatActivity implements ProcessCompletedCallBackInterface {

    TTSListener listener;
    TextToSpeech tts;

    Toolbar toolbar;
    Button dondeEstoyButton;
    Button entrenarButton;
    Button cerrarButton;
    Button exportarButton;
    Button exportarAlClipboardButton;
    EditText ubicacionEditText;
    Spinner iterationsSpinner;
    EditText exportarEditText;

    PositionAssistanceInterface posAssist;
    JSONExporter exporter;

    int totalIterations = 0;

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialmente la app se encuentra en modo asistencia
        initComponents();
        initEvents();
        goAssistState();
        managePermissions();
    }

    protected void initComponents() {
        posAssist = new WiFiPositionAssistanceImpl();
        exporter = new JSONExporter();

        toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle("UPA");
        toolbar.inflateMenu(R.menu.options);

        dondeEstoyButton = findViewById(R.id.dondeEstoyButton);
        entrenarButton = findViewById(R.id.entrenarButton);
        exportarButton = findViewById(R.id.exportartButton);
        exportarAlClipboardButton = findViewById(R.id.exportartAlClipboardButton);
        cerrarButton = findViewById(R.id.cerrarButton);
        ubicacionEditText = findViewById(R.id.ubicacionEditText);
        iterationsSpinner = findViewById(R.id.iterationsSpinner);
        exportarEditText = findViewById(R.id.exportarEditText);

        String[] opciones = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opciones);
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        iterationsSpinner.setAdapter(adaptador);
        iterationsSpinner.setSelection(1);

        listener = new TTSListener();
        tts = new TextToSpeech(getBaseContext(), listener);
    }

    protected void initEvents() {
        // Gestion de eventos
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.ajustes) {
                goConfigState();
            }
            return false;
        });
        cerrarButton.setOnClickListener(v -> goAssistState());
        dondeEstoyButton.setOnClickListener(v -> estimateLocation());
        entrenarButton.setOnClickListener(v -> startTraining());
        exportarButton.setOnClickListener(v -> exportToFile());
        exportarAlClipboardButton.setOnClickListener(v -> exportToClipboard());
    }

    protected void managePermissions() {
        // Solicitar permisos si corresponde
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    protected void goAssistState() {
        status("UPA");
        dondeEstoyButton.setVisibility(View.VISIBLE);
        dondeEstoyButton.getLayoutParams().height=-1;
    }

    protected void goConfigState() {
        status("Ajustes");
        dondeEstoyButton.setVisibility(View.GONE);
    }

    protected void startTraining() {
        if (ubicacionEditText.getText().toString().trim().length()==0) {
            status("Se requiere ubicacion");
            return;
        }
        totalIterations = Integer.parseInt(iterationsSpinner.getSelectedItem().toString());
        status("Entrenando...");
        train();
    }
    /** Entrenar ubicacion */
    public void train() {
        posAssist.train(getBaseContext(), ubicacionEditText.getText().toString(), this);
    }

    /** Estimar la posicion actual */
    public void estimateLocation() {
        posAssist.locate(getBaseContext(), this);
    }

    protected void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "" + System.nanoTime());
    }

    protected void status(String message) {
        toolbar.setSubtitle(message);
    }

    @Override
    public void trainingCompleted(String message) {
        Integer currentIteration = Integer.parseInt(iterationsSpinner.getSelectedItem().toString());
        if (currentIteration > 0) {
            iterationsSpinner.setSelection(iterationsSpinner.getSelectedItemPosition()-1);
            train();
            return;
        }
        iterationsSpinner.setSelection(totalIterations);
        status(message);
    }

    @Override
    public void estimationCompleted(String message) {
        speak(message);
        status(message);
    }

    @Override
    public void processingError(Exception ex) {
        speak(ex.getMessage());
        status(ex.getMessage());
    }

    protected void exportToFile() {
        try {
            String fileName = exporter.export(exportarEditText.getText().toString(), posAssist.getTrainingSet());
            status("Exportado a Documentos");
        } catch (Exception e) {
            e.printStackTrace();
            status("ERR: " + e.getMessage());
        }
    }

    protected void exportToClipboard() {
        try {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text", exporter.toJSON(posAssist.getTrainingSet()));
            clipboardManager.setPrimaryClip(clip);
        } catch (Exception e) {
            e.printStackTrace();
            status("ERR: " + e.getMessage());
        }
    }
}