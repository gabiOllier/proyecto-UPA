package ar.edu.info.lidi.upa;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;


import ar.edu.info.lidi.upa.assist.PositionAssistanceInterface;
import ar.edu.info.lidi.upa.assist.ProcessCompletedCallBackInterface;
import ar.edu.info.lidi.upa.assist.WiFiPositionAssistanceImpl;
import ar.edu.info.lidi.upa.common.Observer;
import ar.edu.info.lidi.upa.exception.ProcessingException;
import ar.edu.info.lidi.upa.tts.TTSListener;
import ar.edu.info.lidi.upa.utils.Constants;
import ar.edu.info.lidi.upa.utils.JSONExporter;
import ar.edu.info.lidi.upa.utils.JSONImporter;

public class MainActivity extends AppCompatActivity implements ProcessCompletedCallBackInterface, Observer {

    protected final String TAG = "UPA";
    TTSListener listener;
    TextToSpeech tts;

    Toolbar toolbar;
    Button dondeEstoyButton;
    Button entrenarButton;
    Button eliminarButton;
    Button cerrarButton;
    Button exportarAlClipboardButton;
    Button importarDelClipboardButton;
    Button compartir;
    Button mostrarDatos;
    EditText ubicacionEditText;
    EditText datosEditText;
    Spinner iterationsSpinner;
    ScrollView scrollView;

    PositionAssistanceInterface posAssist;
    JSONExporter exporter;
    JSONImporter importer;

    int totalIterations = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialmente la app se encuentra en modo asistencia
        initComponents();
        initEvents();
        goAssistState();
        managePermissions();
        loadPreferences();
    }

    protected void initComponents() {
        posAssist = new WiFiPositionAssistanceImpl();
        exporter = new JSONExporter();
        importer = new JSONImporter();

        toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle("UPA");
        toolbar.inflateMenu(R.menu.options);

        dondeEstoyButton = findViewById(R.id.dondeEstoyButton);
        entrenarButton = findViewById(R.id.entrenarButton);
        eliminarButton = findViewById(R.id.eliminarButton);
        exportarAlClipboardButton = findViewById(R.id.exportartAlClipboardButton);
        importarDelClipboardButton = findViewById(R.id.importarDelClipboardButton);
        compartir = findViewById(R.id.compartir);
        mostrarDatos = findViewById(R.id.mostrarDatos);
        cerrarButton = findViewById(R.id.cerrarButton);
        ubicacionEditText = findViewById(R.id.ubicacionEditText);
        iterationsSpinner = findViewById(R.id.iterationsSpinner);
        datosEditText = findViewById(R.id.datosEditText);
        scrollView = findViewById(R.id.mainScrollView);


        String[] opciones = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opciones);
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        iterationsSpinner.setAdapter(adaptador);
        iterationsSpinner.setSelection(1);
        datosEditText.setVisibility(View.GONE);

        listener = new TTSListener();
        tts = new TextToSpeech(getBaseContext(), listener);

        posAssist.addObserver(this);
    }

    @SuppressLint("ClickableViewAccessibility")
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
        eliminarButton.setOnClickListener(v -> deleteTraining());
        exportarAlClipboardButton.setOnClickListener(v -> exportToClipboard());
        importarDelClipboardButton.setOnClickListener(v -> importFromClipboard());
        compartir.setOnClickListener(v -> share());
        mostrarDatos.setOnClickListener(v -> showHideData());

        scrollView.setOnTouchListener((v, event) -> {
            datosEditText.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        datosEditText.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
    }

    @Override
    public void update() {
        try {
            datosEditText.setText(exporter.toJSON(posAssist.getTrainingSet()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void managePermissions() {
        // Solicitar permisos si corresponde
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    protected void goAssistState() {
        status("UPA");
        dondeEstoyButton.setVisibility(View.VISIBLE);
        dondeEstoyButton.getLayoutParams().height=-1;
    }

    protected void loadPreferences() {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        datosEditText.setText(preferences.getString(Constants.PREFERENCE_DATA, ""));
    }

    protected void savePreferences() {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.PREFERENCE_DATA, datosEditText.getText().toString());
        editor.commit();
    }

    protected void onPause() {
        super.onPause();
        savePreferences();
    }

    protected void goConfigState() {
        status("Ajustes");
        dondeEstoyButton.setVisibility(View.GONE);
    }

    protected void showHideData() {
        datosEditText.setVisibility(datosEditText.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
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

    public void deleteTraining() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirmacion")
                .setMessage("¿Estas seguro de que deseas eliminar?")
                .setPositiveButton("Aceptar", (aDialog, which) -> posAssist.emptyTrainingSet())
                .setNegativeButton("Cancelar", (aDialog, which) -> {})
                .create();
        dialog.show();
    }

    /** Entrenar ubicacion */
    public void train() {
        posAssist.train(getBaseContext(), ubicacionEditText.getText().toString(), this);
    }

    /** Estimar la posicion actual */
    public void estimateLocation() {
        speak("Calculando...");
        status("Calculando...");
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
    public void processingError(ProcessingException ex) {
        speak(ex.getMessage());
        status(ex.getMessage());
        ex.getExDetails().ifPresent(x -> Log.e(TAG, x.getMessage()));

    }

    protected void exportToClipboard() {
        try {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text", exporter.toJSON(posAssist.getTrainingSet()));
            clipboardManager.setPrimaryClip(clip);
            status("Copiado al clipboard!");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            status("ERR: " + e.getMessage());
        }
    }

    protected void importFromClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                posAssist.setTrainingSet(importer.fromJSON(item.getText().toString()));
                status("Copiado desde el clipboard!");
                return;
            }
            status("Formato invalido");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            status("ERR: " + e.getMessage());
        }
    }

    protected void share() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, exporter.toJSON(posAssist.getTrainingSet()));
            startActivity(Intent.createChooser(shareIntent, "Compartir"));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            status("ERR:" + e.getMessage());
        }
    }


}