/*
 **=================================================================================================
 **  @file    DashboardActivity.java
 **  @author  Eduardo Lorscheiter
 **  @class   DashboardActivity
 **  @date    21/07/2026
 **  @version 1.0
 **  @brief   Interface principal de monitoramento do veículo (Dashboard).
 **  @details Exibe em tempo real as métricas obtidas da ECU, como rotação (RPM), velocidade,
 **           temperatura, pressão e tensão. Gerencia as animações dos componentes visuais e a
 **           integração com o gerenciador OBD-II.
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eduardo.obdmonitor.R;
import com.eduardo.obdmonitor.model.Device;
import com.eduardo.obdmonitor.elm327.Elm327ConnectionListener;
import com.eduardo.obdmonitor.elm327.Elm327DataListener;
import com.eduardo.obdmonitor.utils.SettingsUtils;
import com.eduardo.obdmonitor.utils.ToastUtils;
import com.eduardo.obdmonitor.utils.ViewUtils;
import com.eduardo.obdmonitor.view.FuelLevelView;
import com.eduardo.obdmonitor.view.MetricView;
import com.eduardo.obdmonitor.view.ShiftLightView;

import java.util.Locale;

public class DashboardActivity extends BaseActivity implements Elm327ConnectionListener,
        Elm327DataListener {
    /**
     * Componentes da janela da Activity.
     */
    private ImageButton btnSettings, btnConnection;
    private ShiftLightView shiftLight;
    private MetricView cardSpeed, cardThrottle, cardMap, cardCoolant, cardIntake, cardBattery;
    private FuelLevelView fuelLevel;
    private TextView txtProtocol;
    private View viewBluetooth, viewEcu, viewLimiter;

    /**
     * Launchers utilizados para iniciar a tela de conexão Bluetooth.
     */
    private ActivityResultLauncher<Intent> settingsLauncher;
    private ActivityResultLauncher<Intent> connectionLauncher;

    // Limite de rotação definido para o veículo
    private int maxRpm = SettingsUtils.DEFAULT_MAX_RPM;
    private int blinkStartRpm;
    private int currentRpm;
    private int appliedLanguageIndex;
    private int appliedThemeMode;

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
        applyMaxRpmSetting();
        appliedLanguageIndex = SettingsUtils.getLanguageIndex(this);
        appliedThemeMode = SettingsUtils.getThemeMode(this);
        setupActivityResult();
        setListeners();
    }

    /**
     * Inicializa os componentes visuais da Activity.
     *
     * <p>Realiza a associação entre os elementos da interface e seus respectivos componentes
     * declarados no layout.</p>
     */
    private void initViews() {
        btnSettings = findViewById(R.id.btnSettings);
        btnConnection = findViewById(R.id.btnConnection);
        shiftLight = findViewById(R.id.shiftLight);
        viewBluetooth = findViewById(R.id.viewBluetooth);
        viewEcu = findViewById(R.id.viewEcu);
        txtProtocol = findViewById(R.id.txtProtocol);
        viewLimiter = findViewById(R.id.viewLimiter);
        cardSpeed = findViewById(R.id.cardSpeed);
        cardThrottle = findViewById(R.id.cardThrottle);
        cardMap = findViewById(R.id.cardMap);
        cardCoolant = findViewById(R.id.cardCoolant);
        cardIntake = findViewById(R.id.cardIntake);
        cardBattery = findViewById(R.id.cardBattery);
        fuelLevel = findViewById(R.id.fuelLevel);
    }

    /**
     * Configura os eventos dos componentes da interface.
     */
    private void setListeners() {
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
            settingsLauncher.launch(intent);
        });

        btnConnection.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ConnectionActivity.class);
            connectionLauncher.launch(intent);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                elm327Manager.disconnect();
                finish();
            }
        });
    }

    /**
     * Configura o launcher responsável por receber o resultado da tela de conexão.
     *
     * <p>Após o retorno da {@link ConnectionActivity}, uma mensagem de sucesso é exibida somente
     * se a inicialização da ECU tiver sido concluída.</p>
     */
    private void setupActivityResult() {
        settingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        boolean themeChanged = appliedThemeMode != SettingsUtils.getThemeMode(this);
                        boolean languageChanged = appliedLanguageIndex != SettingsUtils.getLanguageIndex(this);
                        applyMaxRpmSetting();
                        if (themeChanged)
                            SettingsUtils.applyTheme(this);

                        if (languageChanged && !themeChanged)
                            recreate();
                    }
                }
        );

        connectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        ToastUtils.showToast(this, getString(R.string.obd_connection_success));
                    }
                }
        );
    }

    /**
     * Aplica as configurações de rotação definidas pelo usuário aos componentes responsáveis pelo
     * monitoramento do RPM.
     */
    private void applyMaxRpmSetting() {
        maxRpm = SettingsUtils.getMaxRpm(this);
        blinkStartRpm = SettingsUtils.getBlinkStartRpm(this);
        shiftLight.setMaxRpm(maxRpm);
        shiftLight.setRpm(currentRpm);
        updateRevLimiter(currentRpm);
    }

    @Override
    public void onElm327Connected(Device device) {

    }

    @Override
    public void onElm327BluetoothStatusChanged(boolean bluetoothConnected) {
        ViewUtils.setStatusIndicator(viewBluetooth, bluetoothConnected);
    }

    @Override
    public void onElm327EcuStatusChanged(boolean ecuConnected) {
        ViewUtils.setStatusIndicator(viewEcu, ecuConnected);
    }

    @Override
    public void onElm327ProtocolChanged(String protocol) {
        txtProtocol.setText(protocol == null ? getString(R.string.__) : protocol);
    }

    @Override
    public void onElm327Error(String message) {
        ToastUtils.showToast(this, message);
    }

    @Override
    public void onObdEngineRpmChanged(int rpm) {
        runOnUiThread(() -> {
            currentRpm = rpm;
            shiftLight.setRpm(rpm);
            updateRevLimiter(rpm);
        });
    }

    @Override
    public void onObdVehicleSpeedChanged(int speed) {
        runOnUiThread(() -> cardSpeed.setValue(String.valueOf(speed)));
    }

    @Override
    public void onObdThrottlePositionChanged(int position) {
        runOnUiThread(() -> cardThrottle.setValue(String.valueOf(position)));
    }

    @Override
    public void onObdIntakeManifoldPressureChanged(int pressure) {
        runOnUiThread(() -> cardMap.setValue(String.valueOf(pressure)));
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

    @Override
    public void onObdFuelLevelChanged(int fuel) {
        runOnUiThread(() -> fuelLevel.setFuelLevel(fuel));
    }

    /**
     * Atualiza o estado do alerta visual do limitador de rotação.
     *
     * <p>O efeito de piscar é iniciado quando a rotação atual atinge ou ultrapassa o limite
     * configurado e interrompido quando a rotação retorna para um valor inferior.</p>
     *
     * @param rpm Rotação atual do motor em RPM.
     */
    private void updateRevLimiter(int rpm) {
        if (rpm >= blinkStartRpm)
            startRevLimiterBlink();
        else
            stopRevLimiterBlink();
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