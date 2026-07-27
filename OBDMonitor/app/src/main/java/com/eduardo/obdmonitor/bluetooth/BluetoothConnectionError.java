/*
 **=================================================================================================
 **  @file    BluetoothConnectionError.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Enumeração que define os tipos de erros que podem ocorrer durante o estabelecimento,
 **           manutenção e encerramento de uma conexão Bluetooth.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

public enum BluetoothConnectionError {
    CONNECTION_FAILED("Falha ao conectar ao dispositivo Bluetooth!"),
    COMMUNICATION_ERROR("Erro durante a comunicação Bluetooth!"),
    RESOURCE_CLOSE_FAILED("Erro ao encerrar a conexão Bluetooth!"),
    INVALID_RESPONSE("Resposta inválida recebida do dispositivo!");

    private final String message;

    BluetoothConnectionError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}