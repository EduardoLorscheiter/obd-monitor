/*
 **=================================================================================================
 **  @file    Elm327ConnectionListener.java
 **  @author  Eduardo Lorscheiter
 **  @class   Elm327ConnectionListener
 **  @date    21/07/2026
 **  @version 1.0
 **  @brief   Interface de callbacks responsável por notificar alterações no estado da conexão
 **           Bluetooth, da comunicação com a ECU e dos eventos de inicialização do adaptador
 **           ELM327.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.elm327;

import com.eduardo.obdmonitor.model.Device;

public interface Elm327ConnectionListener {
    /**
     * Chamado quando o adaptador ELM327 é conectado.
     *
     * @param device Dispositivo adaptador ELM327 conectado
     */
    void onElm327Connected(Device device);

    /**
     * Chamado quando houver alterações no estado da conexão Bluetooth com o adaptador ELM327.
     *
     * @param bluetoothConnected Indica se o adaptador Bluetooth está conectado.
     */
    void onElm327BluetoothStatusChanged(boolean bluetoothConnected);

    /**
     * Chamado quando houver alterações no estado da comunicação entre o adaptador ELM327 e a ECU do
     * veículo.
     *
     * @param ecuConnected Indica se a comunicação com a ECU foi estabelecida com sucesso.
     */
    void onElm327EcuStatusChanged(boolean ecuConnected);

    /**
     * Chamado quando o protocolo de comunicação for detectado automaticamente pelo ELM327.
     *
     * @param protocol Nome do protocolo de comunicação utilizado pela ECU do veículo.
     */
    void onElm327ProtocolChanged(String protocol);

    /**
     * Chamado quando ocorre um erro relacionado ao adaptador ELM327.
     *
     * @param message Mensagem descritiva do erro.
     */
    void onElm327Error(String message);
}
