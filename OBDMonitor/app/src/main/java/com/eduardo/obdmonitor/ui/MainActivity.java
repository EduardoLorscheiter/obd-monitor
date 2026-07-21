/*
 **===========================================================================
 **  @file    MainActivity.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    Julho/2026
 **  @version 1.0
 **  @brief   OBDMonitor
 **===========================================================================
 */

package com.eduardo.obdmonitor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eduardo.obdmonitor.R;
import com.eduardo.obdmonitor.model.Device;
import com.eduardo.obdmonitor.obd.ObdListener;
import com.eduardo.obdmonitor.obd.ObdManager;

public class MainActivity extends AppCompatActivity implements ObdListener {
    // Componentes da janela da Activity
    private ImageButton btnSettings, btnConnect;
    private TextView txtRpm;

    // Conexão com o OBD-II
    private ObdManager obdManager;

    private ActivityResultLauncher<Intent> connectionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initConnection();
        setupActivityResult();
        setListeners();
    }

    /**
     * Inicializa os componentes visuais da Activity.
     *
     * <p>Realiza a associação entre os elementos da interface e seus respectivos
     * componentes declarados no layout.</p>
     */
    private void initViews() {
        btnSettings = findViewById(R.id.btnSettings);
        btnConnect = findViewById(R.id.btnConnect);
        txtRpm = findViewById(R.id.txtRpm);
    }

    /**
     * Inicializa os gerenciadores responsáveis pela comunicação OBD-II.
     */
    private void initConnection() {
        obdManager = ObdManager.getInstance();
        obdManager.setListener(this);
    }

    /**
     * Configura os eventos dos componentes da interface.
     */
    private void setListeners() {
        btnConnect.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ConnectionActivity.class);
            connectionLauncher.launch(intent);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void setupActivityResult() {
        connectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        obdManager.setListener(this);
                    }
                }
        );
    }

    @Override
    public void onObdConnected(Device device) {

    }

    @Override
    public void onObdConnectionFailed(String message) {

    }

    @Override
    public void onRpmChanged(int rpm) {
        runOnUiThread(() -> {
            txtRpm.setText(String.valueOf(rpm));
        });
    }
}