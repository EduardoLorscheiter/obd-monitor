/*
 **=================================================================================================
 **  @file    BluetoothPermissionsTest.java
 **  @author  Eduardo Lorscheiter
 **  @class   BluetoothPermissionsTest
 **  @date    02/11/2026
 **  @version 1.0
 **  @brief   Verifica as permissões do fluxo de dispositivos pareados por versão do Android.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

import android.Manifest;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class BluetoothPermissionsTest {
    /**
     * Android 10 e 11 usam as permissões normais do manifesto, sem solicitar localização.
     */
    @Test
    public void requiresNoRuntimePermissionsOnAndroid10And11() {
        assertArrayEquals(new String[0], BluetoothPermissions.getRequiredPermissions(29));
        assertArrayEquals(new String[0], BluetoothPermissions.getRequiredPermissions(30));
    }

    /**
     * A partir do Android 12, conexão e cancelamento de descoberta exigem permissões distintas.
     */
    @Test
    public void requiresConnectAndScanOnAndroid12() {
        assertArrayEquals(new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
        }, BluetoothPermissions.getRequiredPermissions(31));
    }

    /**
     * Mantém as permissões necessárias na versão alvo do aplicativo.
     */
    @Test
    public void requiresConnectAndScanOnAndroid16() {
        assertArrayEquals(new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
        }, BluetoothPermissions.getRequiredPermissions(36));
    }
}
