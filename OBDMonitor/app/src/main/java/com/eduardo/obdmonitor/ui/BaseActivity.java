/*
 **=================================================================================================
 **  @file    BaseActivity.java
 **  @author  Eduardo Lorscheiter
 **  @class   BaseActivity
 **  @date    07/09/2026
 **  @version 1.0
 **  @brief   Activity base responsável pelo gerenciamento comum dos eventos Bluetooth da aplicação.
 **  @details Controla o ciclo de vida dos listeners e do BroadcastReceiver utilizado pelo
 **           BluetoothManager.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eduardo.obdmonitor.bluetooth.BluetoothManager;
import com.eduardo.obdmonitor.bluetooth.BluetoothManagerListener;
import com.eduardo.obdmonitor.elm327.Elm327ConnectionListener;
import com.eduardo.obdmonitor.elm327.Elm327DataListener;
import com.eduardo.obdmonitor.elm327.Elm327Manager;
import com.eduardo.obdmonitor.model.Device;

public abstract class BaseActivity extends AppCompatActivity implements BluetoothManagerListener {
    // Gerenciador responsável pela comunicação Bluetooth
    protected BluetoothManager bluetoothManager;

    // Gerenciador responsável pela comunicação com o adaptador ELM327
    protected Elm327Manager elm327Manager;

    /**
     * Inicializa os recursos comuns utilizados pelas Activities.
     *
     * @param savedInstanceState Estado previamente salvo da Activity.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothManager = BluetoothManager.getInstance(this);
        elm327Manager = Elm327Manager.getInstance(this);
    }

    /**
     * Registra a Activity como listener do BluetoothManager e inicia o monitoramento do estado do
     * adaptador Bluetooth.
     */
    @Override
    protected void onStart() {
        super.onStart();

        bluetoothManager.addListener(this);
        bluetoothManager.registerReceivers();

        registerElm327Listeners();
    }

    /**
     * Registra automaticamente os listeners do ELM327 implementados pela Activity.
     *
     * <p>A Activity é registrada como listener de conexão quando implementa
     * {@link Elm327ConnectionListener} e como listener de dados quando implementa
     * {@link Elm327DataListener}.</p>
     */
    private void registerElm327Listeners() {
        if (this instanceof Elm327ConnectionListener)
            elm327Manager.addConnectionListener((Elm327ConnectionListener) this);

        if (this instanceof Elm327DataListener)
            elm327Manager.addDataListener((Elm327DataListener) this);
    }

    /**
     * Remove a Activity como listener e encerra o monitoramento do BroadcastReceiver quando não
     * houver mais componentes utilizando-o.
     */
    @Override
    protected void onStop() {
        unregisterElm327Listeners();

        bluetoothManager.removeListener(this);
        bluetoothManager.unregisterReceivers();

        super.onStop();
    }

    /**
     * Remove automaticamente os listeners do ELM327 implementados pela Activity.
     *
     * <p>Os listeners são removidos de acordo com as interfaces implementadas pela Activity.</p>
     */
    private void unregisterElm327Listeners() {
        if (this instanceof Elm327ConnectionListener)
            elm327Manager.removeConnectionListener((Elm327ConnectionListener) this);

        if (this instanceof Elm327DataListener)
            elm327Manager.removeDataListener((Elm327DataListener) this);
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
        runOnUiThread(this::onBluetoothDisabled);
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
        runOnUiThread(() -> onBluetoothConnected(device, message));
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
        runOnUiThread(() -> onBluetoothDisconnected(device, message));
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