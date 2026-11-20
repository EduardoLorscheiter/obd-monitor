/*
 **=================================================================================================
 **  @file    BluetoothManagerListener.java
 **  @author  Eduardo Lorscheiter
 **  @class   BluetoothManagerListener
 **  @date    21/07/2026
 **  @version 1.0
 **  @brief   Interface de callbacks utilizada para notificar eventos relacionados ao estado do
 **           Bluetooth e à conexão com dispositivos remotos.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

import com.eduardo.obdmonitor.model.Device;

public interface BluetoothManagerListener {
    /**
     * Chamado quando o Bluetooth do dispositivo é habilitado.
     */
    void onBluetoothManagerEnabled();

    /**
     * Chamado quando o Bluetooth do dispositivo é desabilitado.
     */
    void onBluetoothManagerDisabled();

    /**
     * Chamado quando um dispositivo Bluetooth pareado é encontrado.
     *
     * @param device Dispositivo Bluetooth.
     */
    void onBluetoothManagerDeviceBondedFound(Device device);

    /**
     * Chamado quando a conexão com um dispositivo Bluetooth é iniciada.
     *
     * @param device Dispositivo Bluetooth.
     */
    void onBluetoothManagerConnecting(Device device);

    /**
     * Chamado quando um dispositivo Bluetooth é conectado.
     *
     * @param device  Dispositivo Bluetooth.
     * @param message Mensagem informativa sobre a conexão.
     */
    void onBluetoothManagerConnected(Device device, String message);

    /**
     * Chamado quando um dispositivo Bluetooth é desconectado.
     *
     * @param device  Dispositivo Bluetooth.
     * @param message Mensagem informativa sobre a desconexão.
     */
    void onBluetoothManagerDisconnected(Device device, String message);

    /**
     * Chamado quando ocorre um erro relacionado ao Bluetooth.
     *
     * @param device  Dispositivo Bluetooth.
     * @param message Mensagem descritiva do erro.
     */
    void onBluetoothManagerError(Device device, String message);
}
