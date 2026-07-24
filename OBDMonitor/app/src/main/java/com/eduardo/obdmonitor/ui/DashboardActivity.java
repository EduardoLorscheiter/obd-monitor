/*
 **=================================================================================================
 **  @file    DashboardActivity.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   OBDMonitor
 **=================================================================================================
 */

package com.eduardo.obdmonitor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.eduardo.obdmonitor.obd.ObdConnectionListener;
import com.eduardo.obdmonitor.obd.ObdDataListener;
import com.eduardo.obdmonitor.obd.ObdManager;
import com.eduardo.obdmonitor.utils.ToastUtils;
import com.eduardo.obdmonitor.utils.ViewUtils;
import com.eduardo.obdmonitor.view.ShiftLightView;

public class DashboardActivity extends AppCompatActivity implements ObdConnectionListener, ObdDataListener {
    // Componentes da janela da Activity
    private ImageButton btnConnect;
    private ShiftLightView shiftLight;
    private TextView txtRpm, txtProtocol;
    private View viewBluetooth, viewEcu;

    // Gerenciador responsável pela comunicação OBD-II
    private ObdManager obdManager;

    private ActivityResultLauncher<Intent> connectionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        obdManager.setListeners(null, null);
        obdManager.disconnect();
    }

    /**
     * Inicializa os componentes visuais da Activity.
     *
     * <p>Realiza a associação entre os elementos da interface e seus respectivos componentes
     * declarados no layout.</p>
     */
    private void initViews() {
        btnConnect = findViewById(R.id.btnConnect);
        shiftLight = findViewById(R.id.shiftLight);
        txtRpm = findViewById(R.id.txtRpm);
        viewBluetooth = findViewById(R.id.viewBluetooth);
        viewEcu = findViewById(R.id.viewEcu);
        txtProtocol = findViewById(R.id.txtProtocol);
    }

    /**
     * Inicializa os gerenciadores responsáveis pela comunicação OBD-II.
     */
    private void initConnection() {
        obdManager = ObdManager.getInstance();
        obdManager.setListeners(this, this);
    }

    /**
     * Configura os eventos dos componentes da interface.
     */
    private void setListeners() {
        btnConnect.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ConnectionActivity.class);
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
                    obdManager.setListeners(this, this);

                    if (result.getResultCode() == RESULT_OK) {
                        ToastUtils.showToast(this, "Conectado com sucesso ao dispositivo OBD-II!");
                    } else {
                        ToastUtils.showToast(this, "Não foi possível conectar ao dispositivo OBD-II.");
                    }
                }
        );
    }

    @Override
    public void onObdConnected(Device device) {

    }

    @Override
    public void onObdBluetoothStatusChanged(boolean bluetoothConnected) {
        ViewUtils.setStatusIndicator(viewBluetooth, bluetoothConnected);
    }

    @Override
    public void onObdEcuStatusChanged(boolean ecuConnected) {
        ViewUtils.setStatusIndicator(viewEcu, ecuConnected);
    }

    @Override
    public void onObdProtocolChanged(String protocol) {
        txtProtocol.setText(protocol == null ? getString(R.string.__) : protocol);
    }

    @Override
    public void onObdError(String message) {
        ToastUtils.showToast(this, message);
    }

    @Override
    public void onObdEngineRpmChanged(int rpm) {
        runOnUiThread(() -> {
            txtRpm.setText(String.valueOf(rpm));
            shiftLight.setRpm(rpm);
        });
    }

    @Override
    public void onObdVehicleSpeedChanged(int speed) {

    }

    @Override
    public void onObdEngineCoolantTemperatureChanged(int temperature) {

    }

    @Override
    public void onObdIntakeAirTemperatureChanged(int temperature) {

    }

    @Override
    public void onObdIntakeManifoldPressureChanged(int pressure) {

    }

    @Override
    public void onObdThrottlePositionChanged(int position) {

    }

    @Override
    public void onObdControlModuleVoltageChanged(double voltage) {

    }
}