/*
 **===========================================================================
 **  @file    ObdListener.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    Julho/2026
 **  @version 1.0
 **  @brief   Interface de callbacks para notificar eventos relacionados à
 **           conexão e comunicação com o dispositivo OBD-II.
 **===========================================================================
 */

package com.eduardo.obdmonitor.obd;

import com.eduardo.obdmonitor.model.Device;

public interface ObdListener {
    /**
     * Ao conectar ao OBD-II.
     *
     * @param device Dispositivo de conexão com o OBD-II.
     */
    void onObdConnected(Device device);

    /**
     * Ao dar erro na conexão com o OBD-II.
     *
     * @param message Mensagem de erro.
     */
    void onObdConnectionFailed(String message);

    /**
     * Ao receber a rotação do motor (RPM).
     *
     * @param rpm RPM do motor.
     */
    void onRpmChanged(int rpm);
}
