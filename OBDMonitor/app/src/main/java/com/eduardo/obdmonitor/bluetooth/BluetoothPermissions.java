/*
 **=================================================================================================
 **  @file    BluetoothPermissions.java
 **  @author  Eduardo Lorscheiter
 **  @class   BluetoothPermissions
 **  @date    02/11/2026
 **  @version 1.0
 **  @brief   Centraliza as permissões necessárias para acessar dispositivos Bluetooth pareados.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

public final class BluetoothPermissions {
    /**
     * Classe utilitária. Não permite instanciação.
     */
    private BluetoothPermissions() {
    }

    /**
     * Verifica todas as permissões antes de consultar o adaptador ou conectar um dispositivo.
     *
     * @param context Contexto usado para consultar as permissões do aplicativo.
     * @return {@code true} quando todas as permissões necessárias estão concedidas.
     */
    public static boolean areGranted(Context context) {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    /**
     * Retorna as permissões em runtime necessárias na versão atual do Android.
     *
     * @return Permissões de conexão e cancelamento de descoberta, ou um array vazio antes da API 31.
     */
    public static String[] getRequiredPermissions() {
        return getRequiredPermissions(Build.VERSION.SDK_INT);
    }

    /**
     * Define as permissões necessárias por versão, sem solicitar acesso à localização.
     *
     * @param sdkInt Nível de API do Android.
     * @return Permissões exigidas em runtime para essa versão.
     */
    @SuppressLint("InlinedApi")
    static String[] getRequiredPermissions(int sdkInt) {
        if (sdkInt < Build.VERSION_CODES.S)
            return new String[0];

        return new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
        };
    }
}
