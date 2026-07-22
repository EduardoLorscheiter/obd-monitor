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
     * @param device Dispositivo Bluetooth pareado encontrado.
     */
    void onBondedBluetoothDeviceFound(Device device);

    /**
     * Ao iniciar a conexão com um dispositivo Bluetooth.
     *
     * @param device Dispositivo Bluetooth que iniciou a conexão.
     */
    void onBluetoothConnecting(Device device);

    /**
     * Ao conectar um dispositivo Bluetooth.
     *
     * @param device Dispositivo Bluetooth conectado.
     */
    void onBluetoothConnected(Device device);

    /**
     * Ao desconectar um dispositivo Bluetooth.
     *
     * @param device Dispositivo Bluetooth desconectado.
     */
    void onBluetoothDisconnected(Device device);

    /**
     * Ao ocorrer algum erro.
     *
     * @param message Mensagem de erro.
     */
    void onBluetoothError(String message);
}