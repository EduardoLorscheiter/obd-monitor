/*
 **=================================================================================================
 **  @file    ThrottleCommand.java
 **  @author  Eduardo Lorscheiter
 **  @class   ThrottleCommand
 **  @date    14/08/2026
 **  @version 1.0
 **  @brief   Implementa a leitura da posição absoluta da borboleta de aceleração em %.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.commands;

public class ThrottleCommand extends ObdCommand<Integer> {
    /**
     * PID {@code 0x11} - Posição absoluta da borboleta de aceleração.
     *
     * <p>Resposta: 41 11 A</p>
     * <p>Fórmula: Posição (%) = (A × 100) / 255</p>
     */
    private static final String THROTTLE_PID = "11";

    /**
     * Cria um comando para consulta da posição absoluta da borboleta de aceleração.
     */
    public ThrottleCommand() {
        super(THROTTLE_PID, 1, 0);
    }

    /**
     * Converte a resposta do PID de posição absoluta da borboleta de aceleração.
     *
     * <p>O PID {@code 0x11} retorna um único byte (A), cujo valor é calculado pela expressão:</p>
     *
     * <pre>
     * Posição (%) = (A × 100) / 255
     * </pre>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Posição absoluta da borboleta de aceleração em % ou o último valor válido quando a
     * resposta for inválida.
     */
    @Override
    public Integer parse(String response) {
        String data = getPidData(response);
        if (data == null)
            return getLastValue();

        try {
            int a = hexToInt(data);
            int throttle = (a * 100) / 255;
            setLastValue(throttle);
            return throttle;
        } catch (NumberFormatException e) {
            return getLastValue();
        }
    }
}
