/*
 **=================================================================================================
 **  @file    RpmCommand.java
 **  @author  Eduardo Lorscheiter
 **  @class   RpmCommand
 **  @date    14/08/2026
 **  @version 1.0
 **  @brief   Implementa a leitura da rotação do motor em RPM.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.commands;

public class RpmCommand extends ObdCommand<Integer> {
    /**
     * PID {@code 0x0C} - Rotação do motor (RPM).
     *
     * <p>Resposta: 41 0C A B</p>
     * <p>Fórmula: RPM = ((A × 256) + B) / 4</p>
     */
    private static final String RPM_PID = "0C";

    /**
     * Cria um comando para consulta da rotação do motor.
     */
    public RpmCommand() {
        super(RPM_PID, 2, 0);
    }

    /**
     * Converte a resposta do PID de rotação do motor (RPM).
     *
     * <p>O PID {@code 0x0C} retorna dois bytes (A e B), cujo valor é calculado pela expressão:</p>
     *
     * <pre>
     * RPM = ((A × 256) + B) / 4
     * </pre>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Rotação do motor em RPM ou o valor anterior válido caso a resposta seja inválida.
     */
    @Override
    public Integer parse(String response) {
        String data = getPidData(response);
        if (data == null)
            return getLastValue();

        try {
            int a = hexToInt(data.substring(0, 2));
            int b = hexToInt(data.substring(2, 4));
            int rpm = ((a * 256) + b) / 4;
            setLastValue(rpm);
            return rpm;
        } catch (NumberFormatException e) {
            return getLastValue();
        }
    }
}