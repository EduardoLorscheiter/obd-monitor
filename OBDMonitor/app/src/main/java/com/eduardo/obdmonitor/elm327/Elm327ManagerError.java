/*
 **=================================================================================================
 **  @file    Elm327ManagerError.java
 **  @author  Eduardo Lorscheiter
 **  @class   Elm327ManagerError
 **  @date    03/08/2026
 **  @version 1.0
 **  @brief   Enumeração que define os erros relacionados ao gerenciamento das operações OBD-II
 **           da aplicação, como validação do dispositivo e interrupções.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.elm327;

import android.content.Context;

import androidx.annotation.StringRes;

import com.eduardo.obdmonitor.R;

public enum Elm327ManagerError {
    INVALID_ELM327_DEVICE(R.string.invalid_elm327_device),
    MONITORING_INTERRUPTED(R.string.obd_monitoring_interrupted);

    @StringRes
    private final int messageResId;

    Elm327ManagerError(@StringRes int messageResId) {
        this.messageResId = messageResId;
    }

    public String getMessage(Context context) {
        return context.getString(messageResId);
    }
}
