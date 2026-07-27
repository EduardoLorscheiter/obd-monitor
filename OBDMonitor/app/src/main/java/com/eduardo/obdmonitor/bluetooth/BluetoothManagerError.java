/*
 **=================================================================================================
 **  @file    BluetoothManagerError.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Enumeração que define os erros relacionados ao gerenciamento das operações Bluetooth
 **           da aplicação, como validação do adaptador, permissões e localização de dispositivos.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

public enum BluetoothManagerError {
    DEVICE_NOT_FOUND("Dispositivo Bluetooth não encontrado!"),
    BLUETOOTH_NOT_AVAILABLE("Bluetooth não disponível!"),
    BLUETOOTH_NOT_SUPPORTED("Bluetooth não suportado!"),
    BLUETOOTH_PERMISSION_NOT_GRANTED("Permissão Bluetooth não concedida!"),
    NO_BONDED_DEVICES("Nenhum dispositivo Bluetooth pareado encontrado!");

    private final String message;

    BluetoothManagerError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
