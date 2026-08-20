/*
 **=================================================================================================
 **  @file    ObdDataListener.java
 **  @author  Eduardo Lorscheiter
 **  @class   ObdDataListener
 **  @date    23/07/2026
 **  @version 1.0
 **  @brief   Interface de callbacks responsável por notificar os valores dos parâmetros monitorados
 **           da ECU, como RPM, velocidade, temperatura e demais PIDs OBD-II.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd;

public interface ObdDataListener {
    /**
     * Chamado quando receber um novo valor de rotação do motor (RPM).
     *
     * @param rpm Rotação do motor, em RPM.
     */
    void onObdEngineRpmChanged(int rpm);

    /**
     * Chamado quando receber um novo valor de velocidade do veículo.
     *
     * @param speed Velocidade do veículo, em km/h.
     */
    void onObdVehicleSpeedChanged(int speed);

    /**
     * Chamado quando receber um novo valor de temperatura do líquido de arrefecimento.
     *
     * @param temperature Temperatura do líquido de arrefecimento, em ºC.
     */
    void onObdEngineCoolantTemperatureChanged(int temperature);

    /**
     * Chamado quando receber um novo valor de temperatura do ar admitido.
     *
     * @param temperature Temperatura do ar admitido, em ºC.
     */
    void onObdIntakeAirTemperatureChanged(int temperature);

    /**
     * Chamado quando receber um novo valor de pressão absoluta do coletor de admissão (MAP).
     *
     * @param pressure Pressão absoluta do coletor de admissão (MAP), em kPa.
     */
    void onObdIntakeManifoldPressureChanged(int pressure);

    /**
     * Chamado quando receber um novo valor de posição absoluta da borboleta de aceleração.
     *
     * @param position Posição absoluta da borboleta de aceleração, em %.
     */
    void onObdThrottlePositionChanged(int position);

    /**
     * Chamado quando receber um novo valor de tensão de alimentação do módulo de controle (ECU).
     *
     * @param voltage Tensão de alimentação do módulo de controle (ECU), em V.
     */
    void onObdControlModuleVoltageChanged(double voltage);
}