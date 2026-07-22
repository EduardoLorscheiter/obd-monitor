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
     * Notifica alterações no estado da conexão Bluetooth com o adaptador ELM327.
     *
     * @param bluetoothConnected Indica se o adaptador Bluetooth está conectado.
     */
    void onObdBluetoothStatusChanged(boolean bluetoothConnected);

    /**
     * Notifica alterações no estado da comunicação entre o adaptador ELM327 e a ECU do veículo.
     *
     * @param ecuConnected Indica se a comunicação com a ECU foi estabelecida com sucesso.
     */
    void onObdEcuStatusChanged(boolean ecuConnected);

    /**
     * Notifica o protocolo de comunicação detectado automaticamente pelo ELM327.
     *
     * <p>Exemplos de protocolos: "CAN 500 kbps", "ISO 9141-2" e "SAE J1850 PWM".</p>
     *
     * @param protocol Nome do protocolo de comunicação utilizado pela ECU do veículo.
     */
    void onObdProtocolChanged(String protocol);

    /**
     * Ao receber a rotação do motor (RPM).
     *
     * @param rpm RPM do motor.
     */
    void onObdRpmChanged(int rpm);

    /**
     * Ao ocorrer algum erro.
     *
     * @param message Mensagem de erro.
     */
    void onObdError(String message);
}
