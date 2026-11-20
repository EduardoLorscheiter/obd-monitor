/*
 **=================================================================================================
 **  @file    BluetoothManagerError.java
 **  @author  Eduardo Lorscheiter
 **  @class   BluetoothManagerError
 **  @date    27/07/2026
 **  @version 1.0
 **  @brief   Enumeração que define os erros relacionados ao gerenciamento das operações Bluetooth
 **           da aplicação, como validação do adaptador, permissões e localização de dispositivos.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

import android.content.Context;

import androidx.annotation.StringRes;

import com.eduardo.obdmonitor.R;

public enum BluetoothManagerError {
    DEVICE_NOT_FOUND(R.string.bluetooth_device_not_found),
    BLUETOOTH_NOT_AVAILABLE(R.string.bluetooth_is_not_available),
    BLUETOOTH_NOT_SUPPORTED(R.string.bluetooth_is_not_supported),
    BLUETOOTH_PERMISSION_NOT_GRANTED(R.string.bluetooth_permission_not_granted),
    NO_BONDED_DEVICES(R.string.no_paired_bluetooth_devices_found);

    @StringRes
    private final int messageResId;

    BluetoothManagerError(@StringRes int messageResId) {
        this.messageResId = messageResId;
    }

    public String getMessage(Context context) {
        return context.getString(messageResId);
    }
}
