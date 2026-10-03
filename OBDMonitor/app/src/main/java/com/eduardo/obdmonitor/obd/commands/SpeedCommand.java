/*
 **=================================================================================================
 **  @file    SpeedCommand.java
 **  @author  Eduardo Lorscheiter
 **  @class   SpeedCommand
 **  @date    14/08/2026
 **  @version 1.0
 **  @brief   Implementa a leitura da velocidade do veículo em km/h.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.commands;

public class SpeedCommand extends ObdCommand<Integer> {
    /**
     * PID {@code 0x0D} - Velocidade do veículo.
     *
     * <p>Resposta: 41 0D A</p>
     * <p>Fórmula: Velocidade (km/h) = A</p>
     */
    private static final String SPEED_PID = "0D";

    /**
     * Cria um comando para consulta da velocidade do veículo.
     */
    public SpeedCommand() {
        super(SPEED_PID, 1, 0);
    }

    /**
     * Converte a resposta do PID de velocidade do veículo.
     *
     * <p>O PID {@code 0x0D} retorna um único byte (A), cujo valor representa diretamente a
     * velocidade em quilômetros por hora.</p>
     *
     * <pre>
     * Velocidade (km/h) = A
     * </pre>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Velocidade do veículo em km/h ou o valor anterior válido caso a resposta seja
     * inválida.
     */
    @Override
    public Integer parse(String response) {
        String data = getPidData(response);
        if (data == null)
            return getLastValue();

        try {
            int speed = hexToInt(data);
            setLastValue(speed);
            return speed;
        } catch (NumberFormatException e) {
            return getLastValue();
        }
    }
}