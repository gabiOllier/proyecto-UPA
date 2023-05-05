package ar.edu.info.lidi.upa;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle("UPA");
        toolbar.inflateMenu(R.menu.options);

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.entrenar) {
                // TODO
            } else if (item.getItemId() == R.id.importar) {
                // TODO
            } else if (item.getItemId() == R.id.exportar) {
                // TODO
            }
            return false;
        });
    }

}