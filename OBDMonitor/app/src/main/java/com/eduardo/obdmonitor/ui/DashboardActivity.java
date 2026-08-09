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

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
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
import com.eduardo.obdmonitor.view.MetricView;
import com.eduardo.obdmonitor.view.ShiftLightView;

import java.util.Locale;

public class DashboardActivity extends AppCompatActivity implements ObdConnectionListener, ObdDataListener {
    // Componentes da janela da Activity
    private ImageButton btnConnect;
    private ShiftLightView shiftLight;
    private MetricView cardSpeed, cardBattery, cardCoolant, cardIntake;
    private TextView txtProtocol;
    private View viewBluetooth, viewEcu, viewLimiter;

    // Gerenciador responsável pela comunicação OBD-II
    private ObdManager obdManager;

    // Launcher utilizado para iniciar a tela de conexão Bluetooth
    private ActivityResultLauncher<Intent> connectionLauncher;

    // Animador responsável pelo efeito de piscar quando o motor atinge alta rotação
    private ValueAnimator blinkAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        viewBluetooth = findViewById(R.id.viewBluetooth);
        viewEcu = findViewById(R.id.viewEcu);
        txtProtocol = findViewById(R.id.txtProtocol);
        viewLimiter = findViewById(R.id.viewLimiter);
        initCardSpeed();
        initCardBattery();
        initCardCoolant();
        initCardIntake();
    }

    /**
     * Inicializa o card da métrica de velocidade.
     */
    private void initCardSpeed() {
        cardSpeed = findViewById(R.id.cardSpeed);
        cardSpeed.setIcon(R.drawable.ic_speedmeter);
        cardSpeed.setTitle(getString(R.string.speed));
        cardSpeed.setValue(getString(R.string.__));
        cardSpeed.setUnit("km/h");
    }

    /**
     * Inicializa o card da métrica da bateria.
     */
    private void initCardBattery() {
        cardBattery = findViewById(R.id.cardBattery);
        cardBattery.setIcon(R.drawable.ic_lightning_charge);
        cardBattery.setTitle(getString(R.string.battery));
        cardBattery.setValue(getString(R.string.__));
        cardBattery.setUnit("V");
    }

    /**
     * Inicializa o card da métrica de temperatura do motor.
     */
    private void initCardCoolant() {
        cardCoolant = findViewById(R.id.cardCoolant);
        cardCoolant.setIcon(R.drawable.ic_thermometer);
        cardCoolant.setTitle(getString(R.string.engine_t));
        cardCoolant.setValue(getString(R.string.__));
        cardCoolant.setUnit("°C");
    }

    /**
     * Inicializa o card da métrica de temperatura do ar de admissão.
     */
    private void initCardIntake() {
        cardIntake = findViewById(R.id.cardIntake);
        cardIntake.setIcon(R.drawable.ic_thermometer);
        cardIntake.setTitle(getString(R.string.intake_t));
        cardIntake.setValue(getString(R.string.__));
        cardIntake.setUnit("°C");
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
                obdManager.disconnect();
                finish();
            }
        });
    }

    /**
     * Configura o launcher responsável por receber o resultado da tela de conexão.
     *
     * <p>Após o retorno da {@link ConnectionActivity}, os listeners do {@link ObdManager} são
     * registrados novamente e uma mensagem é exibida informando o sucesso ou falha da conexão com
     * o dispositivo OBD-II.</p>
     */
    private void setupActivityResult() {
        connectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    obdManager.setListeners(this, this);

                    if (result.getResultCode() == RESULT_OK) {
                        ToastUtils.showToast(this, getString(R.string.obd_connection_success));
                    } else {
                        ToastUtils.showToast(this, getString(R.string.obd_connection_failed));
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
            shiftLight.setRpm(rpm);
            if (rpm >= 3500) {
                startRevLimiterBlink();
            } else {
                stopRevLimiterBlink();
            }
        });
    }

    @Override
    public void onObdVehicleSpeedChanged(int speed) {
        runOnUiThread(() -> cardSpeed.setValue(String.valueOf(speed)));
    }

    @Override
    public void onObdEngineCoolantTemperatureChanged(int temperature) {
        runOnUiThread(() -> {
            cardCoolant.setValue(String.valueOf(temperature));
            if (temperature < 60) {
                cardCoolant.setValueColor(R.color.coolant_cold);
            } else if (temperature <= 95) {
                cardCoolant.setValueColor(R.color.coolant_normal);
            } else if (temperature <= 105) {
                cardCoolant.setValueColor(R.color.coolant_warning);
            } else {
                cardCoolant.setValueColor(R.color.coolant_hot);
            }
        });
    }

    @Override
    public void onObdIntakeAirTemperatureChanged(int temperature) {
        runOnUiThread(() -> {
            cardIntake.setValue(String.valueOf(temperature));
            if (temperature < 20) {
                cardIntake.setValueColor(R.color.intake_cold);
            } else if (temperature <= 40) {
                cardIntake.setValueColor(R.color.intake_normal);
            } else if (temperature <= 60) {
                cardIntake.setValueColor(R.color.intake_warning);
            } else {
                cardIntake.setValueColor(R.color.intake_hot);
            }
        });
    }

    @Override
    public void onObdIntakeManifoldPressureChanged(int pressure) {

    }

    @Override
    public void onObdThrottlePositionChanged(int position) {

    }

    @Override
    public void onObdControlModuleVoltageChanged(double voltage) {
        runOnUiThread(() -> {
            cardBattery.setValue(String.format(Locale.US, "%.1f", voltage));
            if (voltage < 11.8) {
                cardBattery.setValueColor(R.color.battery_low);
            } else if (voltage < 12.6) {
                cardBattery.setValueColor(R.color.battery_warning);
            } else if (voltage <= 14.7) {
                cardBattery.setValueColor(R.color.battery_normal);
            } else {
                cardBattery.setValueColor(R.color.battery_over);
            }
        });
    }

    /**
     * Inicia o efeito visual de alerta de alta rotação.
     *
     * <p>Quando a rotação do motor ultrapassa o limite configurado, um overlay semitransparente é
     * animado continuamente utilizando um {@link ValueAnimator}, produzindo um efeito de piscar
     * para chamar a atenção do motorista.</p>
     */
    private void startRevLimiterBlink() {
        if (blinkAnimator != null && blinkAnimator.isRunning())
            return;

        blinkAnimator = ValueAnimator.ofFloat(0f, 1f);

        blinkAnimator.setDuration(100);

        blinkAnimator.setRepeatMode(ValueAnimator.REVERSE);
        blinkAnimator.setRepeatCount(ValueAnimator.INFINITE);

        blinkAnimator.addUpdateListener(animation ->
                viewLimiter.setAlpha((Float) animation.getAnimatedValue()));

        blinkAnimator.start();
    }

    /**
     * Interrompe o efeito visual de alerta de alta rotação.
     *
     * <p>Ao cancelar a animação, a transparência do overlay é restaurada para zero, removendo
     * completamente o efeito de piscar da interface.</p>
     */
    private void stopRevLimiterBlink() {
        if (blinkAnimator != null)
            blinkAnimator.cancel();

        viewLimiter.setAlpha(0f);
    }
}