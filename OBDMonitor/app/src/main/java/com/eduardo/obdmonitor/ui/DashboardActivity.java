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

    private int blinkStartRpm;
    private int currentRpm;
    private int appliedLanguageIndex;

    /**
     * Animador responsável pelo efeito de piscar quando o motor atinge alta rotação.
     */
    private ValueAnimator blinkAnimator;

    /**
     * Impede que callbacks atrasados iniciem animações após a saída do painel.
     */
    private boolean dashboardResumed;

    /**
     * Indica se o painel pode apresentar medições da ECU.
     */
    private boolean ecuConnected;

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
     * Retoma o alerta somente quando o painel volta a ficar ativo.
     */
    @Override
    protected void onResume() {
        super.onResume();
        dashboardResumed = true;
        updateRevLimiter(currentRpm);
    }

    /**
     * Interrompe a animação ao sair do painel.
     */
    @Override
    protected void onPause() {
        dashboardResumed = false;
        stopRevLimiterBlink();
        super.onPause();
    }

    /**
     * Libera os callbacks do animador durante o encerramento definitivo da Activity.
     */
    @Override
    protected void onDestroy() {
        dashboardResumed = false;
        stopRevLimiterBlink();
        super.onDestroy();
    }

    /**
     * Configura os eventos dos componentes da interface.
     */
    private void setListeners() {
        ViewUtils.setClickSoundListener(btnSettings, v -> {
            Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
            settingsLauncher.launch(intent);
        });

        ViewUtils.setClickSoundListener(btnConnection, v -> {
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
                        boolean languageChanged = appliedLanguageIndex != SettingsUtils.getLanguageIndex(this);
                        applyMaxRpmSetting();
                        if (languageChanged)
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
        int maxRpm = SettingsUtils.getMaxRpm(this);
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
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed())
                ViewUtils.setStatusIndicator(viewBluetooth, bluetoothConnected);
        });
    }

    @Override
    public void onElm327EcuStatusChanged(boolean ecuConnected) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed())
                return;

            this.ecuConnected = ecuConnected;

            ViewUtils.setStatusIndicator(viewEcu, ecuConnected);

            if (!ecuConnected)
                clearVehicleData();
        });
    }

    /**
     * Limpa os valores e interrompe o alerta quando a ECU fica desconectada.
     */
    private void clearVehicleData() {
        currentRpm = 0;
        shiftLight.setRpm(0);
        fuelLevel.setFuelLevel(0);
        stopRevLimiterBlink();
        txtProtocol.setText(R.string.__);
        cardSpeed.reset();
        cardThrottle.reset();
        cardMap.reset();
        cardCoolant.reset();
        cardIntake.reset();
        cardBattery.reset();
    }

    @Override
    public void onElm327ProtocolChanged(String protocol) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed() && (ecuConnected || protocol == null))
                txtProtocol.setText(protocol == null ? getString(R.string.__) : protocol);
        });
    }

    @Override
    public void onElm327Error(String message) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed())
                ToastUtils.showToast(this, message);
        });
    }

    @Override
    public void onObdEngineRpmChanged(int rpm) {
        updateVehicleData(() -> {
            currentRpm = rpm;
            shiftLight.setRpm(rpm);
            updateRevLimiter(rpm);
        });
    }

    @Override
    public void onObdVehicleSpeedChanged(int speed) {
        updateVehicleData(() -> cardSpeed.setValue(String.valueOf(speed)));
    }

    @Override
    public void onObdThrottlePositionChanged(int position) {
        updateVehicleData(() -> cardThrottle.setValue(String.valueOf(position)));
    }

    @Override
    public void onObdIntakeManifoldPressureChanged(int pressure) {
        updateVehicleData(() -> cardMap.setValue(String.valueOf(pressure)));
    }

    @Override
    public void onObdEngineCoolantTemperatureChanged(int temperature) {
        updateVehicleData(() -> {
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
        updateVehicleData(() -> {
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
        updateVehicleData(() -> {
            cardBattery.setValue(String.format(Locale.US, "%.1f", voltage));
            if (!Double.isFinite(voltage)) {
                cardBattery.setValue(getString(R.string.__));
                cardBattery.setValueColor(R.color.ios_grey);
                return;
            }
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
        updateVehicleData(() -> fuelLevel.setFuelLevel(fuel));
    }

    /**
     * Aplica medições na thread principal quando a ECU está conectada.
     *
     * @param update Atualização visual da medição recebida.
     */
    private void updateVehicleData(Runnable update) {
        runOnUiThread(() -> {
            if (ecuConnected && !isFinishing() && !isDestroyed())
                update.run();
        });
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
        if (ecuConnected && dashboardResumed
                && !isFinishing() && !isDestroyed() && rpm >= blinkStartRpm)
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
        if (blinkAnimator != null) {
            blinkAnimator.cancel();
            blinkAnimator.removeAllUpdateListeners();
            blinkAnimator.removeAllListeners();
            blinkAnimator = null;
        }

        viewLimiter.setAlpha(0f);
    }
}
