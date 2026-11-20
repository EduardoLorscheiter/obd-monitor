/*
 **=================================================================================================
 **  @file    BluetoothConnectionError.java
 **  @author  Eduardo Lorscheiter
 **  @class   BluetoothConnectionError
 **  @date    27/07/2026
 **  @version 1.0
 **  @brief   Enumeração que define os tipos de erros que podem ocorrer durante o estabelecimento,
 **           manutenção e encerramento de uma conexão Bluetooth.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

import android.content.Context;

import androidx.annotation.StringRes;

import com.eduardo.obdmonitor.R;

public enum BluetoothConnectionError {
    CONNECTION_FAILED(R.string.failed_to_connect_to_the_bluetooth_device),
    COMMUNICATION_ERROR(R.string.error_during_bluetooth_communication),
    INVALID_RESPONSE(R.string.invalid_response_received_from_the_device),
    RESOURCE_CLOSE_FAILED(R.string.failed_to_close_the_bluetooth_connection);

    @StringRes
    private final int messageResId;

    BluetoothConnectionError(@StringRes int messageResId) {
        this.messageResId = messageResId;
    }

    public String getMessage(Context context) {
        return context.getString(messageResId);
    }
}
