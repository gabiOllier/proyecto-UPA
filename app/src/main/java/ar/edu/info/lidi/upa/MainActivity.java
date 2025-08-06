package ar.edu.info.lidi.upa;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.graphics.BitmapFactory;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;
import java.io.InputStream;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;

import ar.edu.info.lidi.upa.assist.PositionAssistanceInterface;
import ar.edu.info.lidi.upa.assist.ProcessCompletedCallBackInterface;
import ar.edu.info.lidi.upa.common.ClassroomStatusFactory;
import ar.edu.info.lidi.upa.common.Observer;
import ar.edu.info.lidi.upa.common.PositionAssistanceFactory;
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

    private GestureDetector gestureDetector;
    private static final int REQUEST_IMAGE_PICK = 2;

    private static final int MODEL_INPUT_SIZE = 224;
    private static final int NUM_CLASSES = 4;
    private Interpreter tflite;
    private Bitmap capturedImageBitmap;

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    public void update() {}

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 50;
        private static final int SWIPE_VELOCITY_THRESHOLD = 50;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX < 0) {
                        estimateLocation();
                        return true;
                    } else if (diffX > 0) {
                        abrirGaleria();
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initComponents();
        initEvents();
        goAssistState();
        loadPreferences();
        initClassroomStatusService();

        gestureDetector = new GestureDetector(this, new GestureListener());
    }

    protected void initComponents() {
        posAssist = PositionAssistanceFactory.getInstance();
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
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.ajustes) {
                goConfigState();
            }
            return false;
        });
        cerrarButton.setOnClickListener(v -> goAssistState());
        dondeEstoyButton.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                // Obtener tamaño original para escalar bien sin saturar memoria
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                InputStream inputStream1 = getContentResolver().openInputStream(imageUri);
                BitmapFactory.decodeStream(inputStream1, null, options);
                if (inputStream1 != null) inputStream1.close();

                int scaleFactor = Math.max(options.outWidth / MODEL_INPUT_SIZE, options.outHeight / MODEL_INPUT_SIZE);
                if (scaleFactor < 1) scaleFactor = 1;

                options.inJustDecodeBounds = false;
                options.inSampleSize = scaleFactor;

                InputStream inputStream2 = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream2, null, options);
                if (inputStream2 != null) inputStream2.close();

                // Escalar exactamente al tamaño que espera el modelo
                capturedImageBitmap = Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true);

                runModelInference(capturedImageBitmap);

            } catch (IOException e) {
                Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error al cargar imagen: " + e.getMessage(), e);
            } catch (Exception e) {
                Toast.makeText(this, "Error inesperado", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error inesperado: " + e.getMessage(), e);
            }
        }
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[MODEL_INPUT_SIZE * MODEL_INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int pixelValue : intValues) {
            float r = ((pixelValue >> 16) & 0xFF) / 255.0f;
            float g = ((pixelValue >> 8) & 0xFF) / 255.0f;
            float b = (pixelValue & 0xFF) / 255.0f;
            byteBuffer.putFloat(r);
            byteBuffer.putFloat(g);
            byteBuffer.putFloat(b);
        }
        return byteBuffer;
    }

    private void runModelInference(Bitmap bitmap) {
        try {
            if (tflite == null) {
                MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(this, "modelo.tflite");
                tflite = new Interpreter(tfliteModel);
            }

            ByteBuffer inputBuffer = convertBitmapToByteBuffer(bitmap);

            // Debug: print buffer capacity
            Log.d("UPA", "Input buffer capacity: " + inputBuffer.capacity());
            int[] inputShape = tflite.getInputTensor(0).shape();
            Log.d("UPA", "Modelo input shape: " + java.util.Arrays.toString(inputShape));

            float[][] output = new float[1][NUM_CLASSES];
            tflite.run(inputBuffer, output);

            // Debug: print output probs
            for (int i = 0; i < NUM_CLASSES; i++) {
                Log.d("UPA", "Clase " + i + ": " + output[0][i]);
            }

            int maxIndex = 0;
            float maxProb = output[0][0];
            for (int i = 1; i < NUM_CLASSES; i++) {
                if (output[0][i] > maxProb) {
                    maxProb = output[0][i];
                    maxIndex = i;
                }
            }

            String[] labels = {"Baño", "Cocina", "Habitación", "Living"};
            String location = maxIndex >= 0 && maxIndex < labels.length ? labels[maxIndex] : "Desconocido";
            status("Ubicación detectada: " + location + " (" + String.format("%.2f", maxProb * 100) + "%)", Constants.OUTPUT_BOTH);

        } catch (IOException e) {
            status("Error cargando modelo", Constants.OUTPUT_TEXT);
            e.printStackTrace();
        }
    }

    private String getLabelForIndex(int index) {
        String[] labels = {"Baño", "Living", "Cocina", "Habitación"};
        if (index >= 0 && index < labels.length) {
            return labels[index];
        }
        return "Desconocido";
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    protected void goAssistState() {
        status("UPA", Constants.OUTPUT_TEXT);
        dondeEstoyButton.setVisibility(View.VISIBLE);
        dondeEstoyButton.getLayoutParams().height = -1;
    }

    protected void loadPreferences() {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        ubicacionEditText.setText(preferences.getString(Constants.PREFERENCE_LOCATION, "Ubicación"));
        iterationsSpinner.setSelection(preferences.getInt(Constants.PREFERENCE_ITERATIONS, 1));
        try {
            posAssist.setTrainingSet(importer.fromJSON(preferences.getString(Constants.PREFERENCE_DATA, "")));
        } catch (Exception e) {
            // Ignorar error al cargar datos
        }
    }

    protected void savePreferences() {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.PREFERENCE_LOCATION, ubicacionEditText.getText().toString());
        editor.putInt(Constants.PREFERENCE_ITERATIONS, iterationsSpinner.getSelectedItemPosition());
        editor.putString(Constants.PREFERENCE_DATA, datosEditText.getText().toString());
        editor.apply();
    }

    protected void initClassroomStatusService() {
        try {
            ClassroomStatusFactory.getInstance().getCurrentStateFor("FOOBAR");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onPause() {
        super.onPause();
        savePreferences();
    }

    protected void goConfigState() {
        status("Ajustes", Constants.OUTPUT_TEXT);
        dondeEstoyButton.setVisibility(View.GONE);
    }

    protected void showHideData() {
        datosEditText.setVisibility(datosEditText.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
    }

    protected void startTraining() {
        if (ubicacionEditText.getText().toString().trim().length() == 0) {
            status("Se requiere ubicación", Constants.OUTPUT_TEXT);
            return;
        }
        totalIterations = Integer.parseInt(iterationsSpinner.getSelectedItem().toString());
        status("Entrenando...", Constants.OUTPUT_TEXT);
        train();
    }

    public void deleteTraining() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirmación")
                .setMessage("¿Estás seguro de que deseas eliminar?")
                .setPositiveButton("Aceptar", (aDialog, which) -> posAssist.emptyTrainingSet())
                .setNegativeButton("Cancelar", (aDialog, which) -> {})
                .create();
        dialog.show();
    }

    public void train() {
        posAssist.train(getBaseContext(), ubicacionEditText.getText().toString(), this);
    }

    public void estimateLocation() {
        status("Calculando... ", Constants.OUTPUT_BOTH);
        posAssist.locate(getBaseContext(), this);
    }

    protected void status(String message, Integer output) {
        if (Constants.OUTPUT_TEXT == output || Constants.OUTPUT_BOTH == output)
            toolbar.setSubtitle(message);
        if (Constants.OUTPUT_AUDIO == output || Constants.OUTPUT_BOTH == output)
            tts.speak(message, TextToSpeech.QUEUE_ADD, null, "" + System.nanoTime());
    }

    @Override
    public void trainingCompleted(String message) {
        Integer currentIteration = Integer.parseInt(iterationsSpinner.getSelectedItem().toString());
        if (currentIteration > 0) {
            iterationsSpinner.setSelection(iterationsSpinner.getSelectedItemPosition() - 1);
            train();
            return;
        }
        iterationsSpinner.setSelection(totalIterations);
        status(message, Constants.OUTPUT_TEXT);
    }

    @Override
    public void estimationCompleted(String message) {
        status(message, Constants.OUTPUT_BOTH);
        try {
            String info = ClassroomStatusFactory.getInstance().getCurrentStateFor(message);
            status(info, Constants.OUTPUT_BOTH);
        } catch (ProcessingException e) {}
    }

    @Override
    public void processingError(ProcessingException ex) {
        status(ex.getMessage(), Constants.OUTPUT_BOTH);
        ex.getExDetails().ifPresent(x -> Log.e(TAG, x.getMessage()));
    }

    protected void exportToClipboard() {
        try {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text", exporter.toJSON(posAssist.getTrainingSet()));
            clipboardManager.setPrimaryClip(clip);
            status("Copiado al clipboard!", Constants.OUTPUT_TEXT);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            status("ERR: " + e.getMessage(), Constants.OUTPUT_TEXT);
        }
    }

    protected void importFromClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                posAssist.setTrainingSet(importer.fromJSON(item.getText().toString()));
                status("Copiado desde el clipboard!", Constants.OUTPUT_TEXT);
                return;
            }
            status("Formato inválido", Constants.OUTPUT_TEXT);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            status("ERR: " + e.getMessage(), Constants.OUTPUT_TEXT);
        }
    }

    protected void share() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, exporter.toJSON(posAssist.getTrainingSet()));
            startActivity(Intent.createChooser(shareIntent, "Compartir datos"));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            status("ERR: " + e.getMessage(), Constants.OUTPUT_TEXT);
        }
    }
}
