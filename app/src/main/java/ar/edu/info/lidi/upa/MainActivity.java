package ar.edu.info.lidi.upa;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    TTSListener listener;
    TextToSpeech tts;

    Toolbar toolbar;
    Button dondeEstoyButton;
    Button cerrarButton;

    PosAssistInterface posAssist = new WiFiPosAssistImpl();

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
        toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle("UPA");
        toolbar.inflateMenu(R.menu.options);

        dondeEstoyButton = findViewById(R.id.dondeEstoyButton);
        cerrarButton = findViewById(R.id.cerrarButton);

        listener = new TTSListener();
        tts = new TextToSpeech(getBaseContext(), listener);
    }

    protected void initEvents() {
        // Gestion de eventos
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.ajustes) {
                hideAssistButton();
            }
            else if (item.getItemId() == R.id.asistir) {
                displayAssistButton();
            }
            return false;
        });
        cerrarButton.setOnClickListener(v -> displayAssistButton());
        dondeEstoyButton.setOnClickListener(v -> assistPosition());
    }

    protected void displayAssistButton() {
        dondeEstoyButton.setVisibility(View.VISIBLE);
        dondeEstoyButton.getLayoutParams().height=-1;
    }

    protected void hideAssistButton() {
        dondeEstoyButton.setVisibility(View.GONE);
    }

    protected void assistPosition() {
        tts.speak(posAssist.locate(), TextToSpeech.QUEUE_ADD, null, ""+System.nanoTime());
    }


}