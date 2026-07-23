/*
 **===========================================================================
 **  @file    ObdListener.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Interface de callbacks utilizada para notificar eventos
 **           relacionados à comunicação com o adaptador OBD-II ELM327 e
 **           com a ECU do veículo.
 **===========================================================================
 */

package com.eduardo.obdmonitor.obd;

import com.eduardo.obdmonitor.model.Device;

public interface ObdListener {
    /**
     * Chamado quando o OBD-II é conectado.
     *
     * @param device Dispositivo OBD-II conectado
     */
    void onObdConnected(Device device);

    /**
     * Chamado quando houver alterações no estado da conexão Bluetooth com o adaptador ELM327.
     *
     * @param bluetoothConnected Indica se o adaptador Bluetooth está conectado.
     */
    void onObdBluetoothStatusChanged(boolean bluetoothConnected);

    /**
     * Chamado quando houver alterações no estado da comunicação entre o adaptador ELM327 e
     * a ECU do veículo.
     *
     * @param ecuConnected Indica se a comunicação com a ECU foi estabelecida com sucesso.
     */
    void onObdEcuStatusChanged(boolean ecuConnected);

    /**
     * Chamado quando o protocolo de comunicação for detectado automaticamente pelo ELM327.
     *
     * @param protocol Nome do protocolo de comunicação utilizado pela ECU do veículo.
     */
    void onObdProtocolChanged(String protocol);

    /**
     * Chamado quando receber um novo valor de rotação do motor.
     *
     * @param rpm Rotação do motor, em RPM.
     */
    void onObdRpmChanged(int rpm);

    /**
     * Chamado quando ocorre um erro relacionado ao OBD-II.
     *
     * @param message Mensagem descritiva do erro.
     */
    void onObdError(String message);
}
