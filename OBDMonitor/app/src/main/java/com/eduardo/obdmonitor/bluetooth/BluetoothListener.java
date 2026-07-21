/*
 **===========================================================================
 **  @file    BluetoothListener.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    Julho/2026
 **  @version 1.0
 **  @brief   Interface de callbacks para notificar eventos relacionados à
 **           conexão com o dispositivo Bluetooth.
 **===========================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

import com.eduardo.obdmonitor.model.Device;

public interface BluetoothListener {
    /**
     * Ao habilitar o Bluetooth.
     */
    void onBluetoothEnabled();

    /**
     * Ao desabilitar o Bluetooth.
     */
    void onBluetoothDisabled();

    /**
     * Ao encontrar um dispositivo Bluetooth pareado.
     *
     * @param device dispositivo Bluetooth pareado encontrado.
     */
    void onBondedDeviceFound(Device device);

    /**
     * Ao iniciar a conexão com um dispositivo Bluetooth.
     *
     * @param device dispositivo Bluetooth que iniciou a conexão.
     */
    void onConnecting(Device device);

    /**
     * Ao conectar um dispositivo Bluetooth.
     *
     * @param device dispositivo Bluetooth conectado.
     */
    void onConnected(Device device);

    /**
     * Ao desconectar um dispositivo Bluetooth.
     *
     * @param device dispositivo Bluetooth desconectado.
     */
    void onDisconnected(Device device);

    /**
     * Ao ocorrer algum erro.
     *
     * @param message mensagem de erro.
     */
    void onError(String message);
}