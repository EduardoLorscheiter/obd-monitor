/*
 **===========================================================================
 **  @file    ObdConnectionListener.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Interface de callbacks responsável por notificar alterações
 **           no estado da conexão Bluetooth, da comunicação com a ECU
 **           e dos eventos de inicialização do OBD-II.
 **===========================================================================
 */

package com.eduardo.obdmonitor.obd;

import com.eduardo.obdmonitor.model.Device;

public interface ObdConnectionListener {
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
     * Chamado quando ocorre um erro relacionado ao OBD-II.
     *
     * @param message Mensagem descritiva do erro.
     */
    void onObdError(String message);
}
