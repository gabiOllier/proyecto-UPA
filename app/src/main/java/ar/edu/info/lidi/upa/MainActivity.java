package ar.edu.info.lidi.upa;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button dondeEstoyButton;
    Button cerrarButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle("UPA");
        toolbar.inflateMenu(R.menu.options);

        // Inicialmente la app se encuentra en modo asistencia
        initComponents();
        displayAssistButton();

        // Gestion de eventos
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.entrenar) {
                hideAssistButton();
            }
            else if (item.getItemId() == R.id.asistir) {
                displayAssistButton();
            }
            else if (item.getItemId() == R.id.importar) {
                // TODO
            } else if (item.getItemId() == R.id.exportar) {
                // TODO
            }
            return false;
        });
        cerrarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayAssistButton();
            }
        });
    }

    protected void initComponents() {
        dondeEstoyButton = findViewById(R.id.dondeEstoyButton);
        cerrarButton = findViewById(R.id.cerrarButton);
    }

    protected void displayAssistButton() {
        dondeEstoyButton.setVisibility(View.VISIBLE);
        dondeEstoyButton.getLayoutParams().height=-1;
    }

    protected void hideAssistButton() {
        dondeEstoyButton.setVisibility(View.GONE);
    }

}