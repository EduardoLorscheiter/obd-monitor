/*
 **=================================================================================================
 **  @file    BluetoothConnectionListener.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Interface de callbacks utilizados para notificar eventos relacionados à conexão
 **           Bluetooth, como estabelecimento e encerramento da conexão, além da ocorrência de erros
 **           durante a comunicação.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

import com.eduardo.obdmonitor.model.Device;

public interface BluetoothConnectionListener {
    /**
     * Chamado quando um dispositivo Bluetooth é conectado.
     *
     * @param device Dispositivo Bluetooth.
     */
    void onBluetoothConnectionConnected(Device device);

    /**
     * Chamado quando um dispositivo Bluetooth é desconectado.
     *
     * @param device Dispositivo Bluetooth.
     */
    void onBluetoothConnectionDisconnected(Device device);

    /**
     * Chamado quando ocorre um erro relacionado ao Bluetooth.
     *
     * @param device  Dispositivo Bluetooth. Pode ser {@code null} quando o erro não estiver
     *                relacionado a um dispositivo específico.
     * @param message Mensagem descritiva do erro.
     */
    void onBluetoothConnectionError(Device device, String message);
}
