/*
 **=================================================================================================
 **  @file    BluetoothListener.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Interface de callbacks utilizada para notificar eventos relacionados ao estado do
 **           Bluetooth e à conexão com dispositivos remotos.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

import com.eduardo.obdmonitor.model.Device;

public interface BluetoothListener {
    /**
     * Chamado quando o Bluetooth é habilitado.
     */
    void onBluetoothEnabled();

    /**
     * Chamado quando o Bluetooth é desabilitado.
     */
    void onBluetoothDisabled();

    /**
     * Chamado quando um dispositivo Bluetooth pareado é encontrado.
     *
     * @param device Dispositivo Bluetooth.
     */
    void onBluetoothDeviceBondedFound(Device device);

    /**
     * Chamado quando a conexão com um dispositivo Bluetooth é iniciada.
     *
     * @param device Dispositivo Bluetooth.
     */
    void onBluetoothConnecting(Device device);

    /**
     * Chamado quando um dispositivo Bluetooth é conectado.
     *
     * @param device Dispositivo Bluetooth.
     */
    void onBluetoothConnected(Device device);

    /**
     * Chamado quando um dispositivo Bluetooth é desconectado.
     *
     * @param device Dispositivo Bluetooth.
     */
    void onBluetoothDisconnected(Device device);

    /**
     * Chamado quando ocorre um erro relacionado ao Bluetooth.
     *
     * @param message Mensagem descritiva do erro.
     */
    void onBluetoothError(String message);
}