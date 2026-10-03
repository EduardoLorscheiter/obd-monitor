/*
 **=================================================================================================
 **  @file    BluetoothConnectionListener.java
 **  @author  Eduardo Lorscheiter
 **  @class   BluetoothConnectionListener
 **  @date    27/07/2026
 **  @version 1.0
 **  @brief   Interface de callbacks utilizados para notificar eventos relacionados à conexão
 **           Bluetooth, como estabelecimento e encerramento da conexão, além da ocorrência de erros
 **           durante a comunicação.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

public interface BluetoothConnectionListener {
    /**
     * Chamado quando um dispositivo Bluetooth é conectado.
     */
    void onBluetoothConnectionConnected();

    /**
     * Chamado quando um dispositivo Bluetooth é desconectado.
     */
    void onBluetoothConnectionDisconnected();

    /**
     * Chamado quando ocorre um erro relacionado ao Bluetooth.
     *
     * @param message Mensagem descritiva do erro.
     */
    void onBluetoothConnectionError(String message);
}