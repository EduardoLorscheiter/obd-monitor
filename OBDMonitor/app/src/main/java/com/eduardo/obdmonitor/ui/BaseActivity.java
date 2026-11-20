/*
 **=================================================================================================
 **  @file    BaseActivity.java
 **  @author  Eduardo Lorscheiter
 **  @class   BaseActivity
 **  @date    07/09/2026
 **  @version 1.0
 **  @brief   Activity base responsável pelo gerenciamento comum dos eventos Bluetooth da aplicação.
 **  @details Gerencia o ciclo de vida dos listeners do BluetoothManager e do Elm327Manager, além do
 **           registro e cancelamento dos BroadcastReceivers utilizados pelo BluetoothManager.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.ui;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eduardo.obdmonitor.bluetooth.BluetoothManager;
import com.eduardo.obdmonitor.bluetooth.BluetoothManagerListener;
import com.eduardo.obdmonitor.bluetooth.BluetoothPermissions;
import com.eduardo.obdmonitor.elm327.Elm327ConnectionListener;
import com.eduardo.obdmonitor.elm327.Elm327DataListener;
import com.eduardo.obdmonitor.elm327.Elm327Manager;
import com.eduardo.obdmonitor.model.Device;
import com.eduardo.obdmonitor.utils.SettingsUtils;

public abstract class BaseActivity extends AppCompatActivity implements BluetoothManagerListener {
    /**
     * Gerenciador responsável pela comunicação Bluetooth.
     */
    protected BluetoothManager bluetoothManager;

    /**
     * Gerenciador responsável pela comunicação com o adaptador ELM327.
     */
    protected Elm327Manager elm327Manager;

    /**
     * Gerenciador responsável pela reprodução dos sons de interação da interface.
     */
    private SoundPool clickSoundPool;

    /**
     * Identificador do recurso de áudio utilizado nos cliques.
     */
    private int clickSoundId;

    /**
     * Indica se o recurso de áudio foi carregado com sucesso.
     */
    private boolean clickSoundLoaded;

    /**
     * Aplica as configurações do aplicativo ao contexto base da Activity.
     *
     * <p>As configurações aplicadas incluem o modo de aparência e o idioma selecionado pelo
     * usuário.</p>
     *
     * @param newBase Contexto base fornecido pelo Android durante a criação da Activity.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(SettingsUtils.applyConfig(newBase));
    }

    /**
     * Inicializa os recursos comuns utilizados pelas Activities.
     *
     * @param savedInstanceState Estado previamente salvo da Activity.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        clickSoundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build();
        clickSoundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (sampleId == clickSoundId && status == 0)
                clickSoundLoaded = true;
        });
        clickSoundId = clickSoundPool.load(this, com.eduardo.obdmonitor.R.raw.ui_click, 1);

        initializeManagers();
    }

    /**
     * Inicializa os gerenciadores utilizados pela Activity.
     */
    private void initializeManagers() {
        bluetoothManager = BluetoothManager.getInstance(this);
        elm327Manager = Elm327Manager.getInstance(this);
    }

    /**
     * Registra a Activity como listener do {@link BluetoothManager} e registra os listeners do
     * {@link Elm327Manager] de acordo com a implementação da Activity.
     *
     * <p>Também consulta o estado atual do Bluetooth e retoma a inicialização do ELM327 quando já
     * existe uma conexão Bluetooth ativa no aplicativo.</p>
     */
    @Override
    protected void onStart() {
        super.onStart();

        registerListeners();
        registerBluetoothReceivers();
        synchronizeConnectionState();
    }

    /**
     * Registra os listeners utilizados pela Activity.
     *
     * <p>A Activity é registrada como listener de conexão quando implementa
     * {@link Elm327ConnectionListener} e como listener de dados quando implementa
     * {@link Elm327DataListener}.</p>
     */
    private void registerListeners() {
        bluetoothManager.addListener(this);

        if (this instanceof Elm327ConnectionListener)
            elm327Manager.addConnectionListener((Elm327ConnectionListener) this);

        if (this instanceof Elm327DataListener)
            elm327Manager.addDataListener((Elm327DataListener) this);
    }

    /**
     * Registra os BroadcastReceivers utilizados pelo {@link BluetoothManager}.
     */
    private void registerBluetoothReceivers() {
        bluetoothManager.registerReceivers();
    }

    /**
     * Sincroniza o estado da Activity com o estado atual do {@link BluetoothManager}.
     *
     * <p>Notifica a Activity sobre o estado atual do Bluetooth e retoma a conexão com o ELM327
     * quando já existe um dispositivo Bluetooth conectado.</p>
     */
    protected final void synchronizeConnectionState() {
        if (!BluetoothPermissions.areGranted(this)) {
            onBluetoothManagerDisabled();
            return;
        }

        if (!bluetoothManager.isBluetoothEnabled()) {
            onBluetoothManagerDisabled();
            return;
        }

        onBluetoothManagerEnabled();

        reconnectElm327();
    }

    /**
     * Retoma a conexão com o ELM327 quando existe um dispositivo Bluetooth conectado.
     */
    private void reconnectElm327() {
        Device connectedDevice = bluetoothManager.getConnectedDevice();

        if (connectedDevice != null)
            elm327Manager.connectAsync(connectedDevice);
        else
            elm327Manager.handleBluetoothDisconnection();
    }

    /**
     * Remove a Activity dos listeners do {@link BluetoothManager} e do {@link Elm327Manager} e
     * solicita o cancelamento dos BroadcastReceivers gerenciados pelo {@link BluetoothManager}.
     */
    @Override
    protected void onStop() {
        unregisterListeners();
        unregisterBluetoothReceivers();

        super.onStop();
    }

    /**
     * Remove os listeners registrados pela Activity.
     *
     * <p>Os listeners de conexão e de dados do ELM327 do {@link Elm327Manager} são removidos
     * conforme as interfaces implementadas pela Activity.</p>
     */
    private void unregisterListeners() {
        if (this instanceof Elm327ConnectionListener)
            elm327Manager.removeConnectionListener((Elm327ConnectionListener) this);

        if (this instanceof Elm327DataListener)
            elm327Manager.removeDataListener((Elm327DataListener) this);

        bluetoothManager.removeListener(this);
    }

    /**
     * Cancela os BroadcastReceivers utilizados pelo {@link BluetoothManager}.
     */
    private void unregisterBluetoothReceivers() {
        bluetoothManager.unregisterReceivers();
    }

    /**
     * Libera os recursos de áudio quando a Activity é destruída.
     */
    @Override
    protected void onDestroy() {
        if (clickSoundPool != null) {
            clickSoundPool.release();
            clickSoundPool = null;
            clickSoundLoaded = false;
        }
        super.onDestroy();
    }

    /**
     * Reproduz o som de clique quando o áudio está carregado e seus recursos estão disponíveis.
     */
    public void playClickSound() {
        if (clickSoundLoaded && clickSoundPool != null)
            clickSoundPool.play(clickSoundId, 1f, 1f, 1, 0, 1f);
    }

    /**
     * Chamado quando o Bluetooth do dispositivo é habilitado.
     *
     * <p>Executa o tratamento na thread principal e disponibiliza uma forma de notificação através
     * do {@link #onBluetoothEnabled()} para que cada Activity possa definir seu comportamento
     * específico.</p>
     */
    @Override
    public final void onBluetoothManagerEnabled() {
        runOnUiThread(this::onBluetoothEnabled);
    }

    /**
     * Permite que Activities filhas definam o comportamento quando o Bluetooth do dispositivo é
     * habilitado.
     */
    protected void onBluetoothEnabled() {
    }

    /**
     * Chamado quando o Bluetooth do dispositivo é desabilitado.
     *
     * <p>Executa o tratamento na thread principal e disponibiliza uma forma de notificação através
     * do {@link #onBluetoothDisabled()} para que cada Activity possa definir seu comportamento
     * específico.</p>
     */
    @Override
    public final void onBluetoothManagerDisabled() {
        runOnUiThread(() -> {
            elm327Manager.disconnect();
            onBluetoothDisabled();
        });
    }

    /**
     * Permite que Activities filhas definam o comportamento quando o Bluetooth do dispositivo é
     * desabilitado.
     */
    protected void onBluetoothDisabled() {
    }

    /**
     * Chamado quando um dispositivo Bluetooth pareado é encontrado.
     *
     * <p>Executa o tratamento na thread principal e disponibiliza uma forma de notificação através
     * do {@link #onBluetoothDeviceBondedFound(Device)} para que cada Activity possa definir seu
     * comportamento específico.</p>
     *
     * @param device Dispositivo Bluetooth.
     */
    @Override
    public final void onBluetoothManagerDeviceBondedFound(Device device) {
        runOnUiThread(() -> onBluetoothDeviceBondedFound(device));
    }

    /**
     * Permite que Activities filhas definam o comportamento quando um dispositivo Bluetooth pareado
     * é encontrado.
     *
     * @param device Dispositivo Bluetooth.
     */
    protected void onBluetoothDeviceBondedFound(Device device) {
    }

    /**
     * Chamado quando a conexão com um dispositivo Bluetooth é iniciada.
     *
     * <p>Executa o tratamento na thread principal e disponibiliza uma forma de notificação através
     * do {@link #onBluetoothConnecting(Device)} para que cada Activity possa definir seu
     * comportamento específico.</p>
     *
     * @param device Dispositivo Bluetooth.
     */
    @Override
    public final void onBluetoothManagerConnecting(Device device) {
        runOnUiThread(() -> onBluetoothConnecting(device));
    }

    /**
     * Permite que Activities filhas definam o comportamento quando a conexão com um dispositivo
     * Bluetooth é iniciada.
     *
     * @param device Dispositivo Bluetooth.
     */
    protected void onBluetoothConnecting(Device device) {
    }

    /**
     * Chamado quando um dispositivo Bluetooth é conectado.
     *
     * <p>Executa o tratamento na thread principal e disponibiliza uma forma de notificação através
     * do {@link #onBluetoothConnected(Device, String)} para que cada Activity possa definir seu
     * comportamento específico.</p>
     *
     * @param device  Dispositivo Bluetooth.
     * @param message Mensagem informativa sobre a conexão.
     */
    @Override
    public final void onBluetoothManagerConnected(Device device, String message) {
        runOnUiThread(() -> {
            onBluetoothConnected(device, message);
            elm327Manager.connectAsync(device);
        });
    }

    /**
     * Permite que Activities filhas definam o comportamento quando um dispositivo Bluetooth é
     * conectado.
     *
     * @param device  Dispositivo Bluetooth.
     * @param message Mensagem informativa sobre a conexão.
     */
    protected void onBluetoothConnected(Device device, String message) {
    }

    /**
     * Chamado quando um dispositivo Bluetooth é desconectado.
     *
     * <p>Executa o tratamento na thread principal e disponibiliza uma forma de notificação através
     * do {@link #onBluetoothDisconnected(Device, String)} para que cada Activity possa definir seu
     * comportamento específico.</p>
     *
     * @param device  Dispositivo Bluetooth.
     * @param message Mensagem informativa sobre a desconexão.
     */
    @Override
    public final void onBluetoothManagerDisconnected(Device device, String message) {
        runOnUiThread(() -> {
            elm327Manager.handleBluetoothDisconnection();
            onBluetoothDisconnected(device, message);
        });
    }

    /**
     * Permite que Activities filhas definam o comportamento quando um dispositivo Bluetooth é
     * desconectado.
     *
     * @param device  Dispositivo Bluetooth.
     * @param message Mensagem informativa sobre a desconexão.
     */
    protected void onBluetoothDisconnected(Device device, String message) {
    }

    /**
     * Chamado quando ocorre um erro relacionado ao Bluetooth.
     *
     * <p>Executa o tratamento na thread principal e disponibiliza uma forma de notificação através
     * do {@link #onBluetoothError(Device, String)} para que cada Activity possa definir seu
     * comportamento específico.</p>
     *
     * @param device  Dispositivo Bluetooth.
     * @param message Mensagem descritiva do erro.
     */
    @Override
    public final void onBluetoothManagerError(Device device, String message) {
        runOnUiThread(() -> onBluetoothError(device, message));
    }

    /**
     * Permite que Activities filhas definam o comportamento quando ocorre um erro relacionado ao
     * Bluetooth.
     *
     * @param device  Dispositivo Bluetooth.
     * @param message Mensagem descritiva do erro.
     */
    protected void onBluetoothError(Device device, String message) {
    }
}
