package ar.edu.info.lidi.upa;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import ar.edu.info.lidi.upa.assist.PositionAssistanceInterface;
import ar.edu.info.lidi.upa.assist.ProcessCompletedCallBackInterface;
import ar.edu.info.lidi.upa.assist.WiFiPositionAssistanceImpl;
import ar.edu.info.lidi.upa.exception.NoLocationAvailableException;
import ar.edu.info.lidi.upa.exception.TrainingProcessingException;
import ar.edu.info.lidi.upa.tts.TTSListener;

public class MainActivity extends AppCompatActivity implements ProcessCompletedCallBackInterface {

    TTSListener listener;
    TextToSpeech tts;

    Toolbar toolbar;
    Button dondeEstoyButton;
    Button entrenarButton;
    Button cerrarButton;
    EditText ubicacionEditText;
    EditText iterationsEditText;

    PositionAssistanceInterface posAssist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialmente la app se encuentra en modo asistencia
        initComponents();
        initEvents();
        displayAssistButton();
    }

    protected void initComponents() {
        posAssist = new WiFiPositionAssistanceImpl();

        toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle("UPA");
        toolbar.inflateMenu(R.menu.options);

        dondeEstoyButton = findViewById(R.id.dondeEstoyButton);
        entrenarButton = findViewById(R.id.entrenarButton);
        cerrarButton = findViewById(R.id.cerrarButton);
        ubicacionEditText = findViewById(R.id.ubicacionEditText);
        iterationsEditText = findViewById(R.id.iteracionesEditText);

        listener = new TTSListener();
        tts = new TextToSpeech(getBaseContext(), listener);
    }

    protected void initEvents() {
        // Gestion de eventos
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.ajustes) {
                hideAssistButton();
            }
            return false;
        });
        cerrarButton.setOnClickListener(v -> displayAssistButton());
        dondeEstoyButton.setOnClickListener(v -> estimateLocation());
        entrenarButton.setOnClickListener(v -> train());
    }

    protected void displayAssistButton() {
        dondeEstoyButton.setVisibility(View.VISIBLE);
        dondeEstoyButton.getLayoutParams().height=-1;
    }

    protected void hideAssistButton() {
        dondeEstoyButton.setVisibility(View.GONE);
    }

    /** Entrenar ubicacion */
    public void train() {
        String ubicacion = ubicacionEditText.getText().toString();
        Toast.makeText(getBaseContext(), "Realizando entrenamiento...", Toast.LENGTH_LONG).show();
        posAssist.train(getBaseContext(), ubicacion, this);
    }

    /** Estimar la posicion actual */
    public void estimateLocation() {
        posAssist.locate(getBaseContext(), this);
    }

    protected void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "" + System.nanoTime());
    }


    @Override
    public void trainingCompleted(String message) {
        try {
            Integer currentIteration = Integer.parseInt(iterationsEditText.getText().toString());
            if (currentIteration > 0) {
                int pendingIterations = currentIteration - 1;
                iterationsEditText.setText(""+pendingIterations);
                train();
                return;
            }
            Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
        } catch (Exception e) { /* Ignorar por el momento */}
    }

    @Override
    public void estimationCompleted(String message) {
        speak(message);
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void processingError(Exception ex) {
        speak(ex.getMessage());
        Toast.makeText(getBaseContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
    }
}