/*
 **=================================================================================================
 **  @file    IntakeCommand.java
 **  @author  Eduardo Lorscheiter
 **  @class   IntakeCommand
 **  @date    14/08/2026
 **  @version 1.0
 **  @brief   Implementa a leitura da temperatura do ar admitido em °C.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.commands;

public class IntakeCommand extends ObdCommand<Integer> {
    /**
     * PID {@code 0x0F} - Temperatura do ar admitido.
     *
     * <p>Resposta: 41 0F A</p>
     * <p>Fórmula: Temperatura (°C) = A - 40</p>
     */
    private static final String INTAKE_PID = "0F";

    /**
     * Cria um comando para consulta da temperatura do ar admitido.
     */
    public IntakeCommand() {
        super(INTAKE_PID, 1, 0);
    }

    /**
     * Converte a resposta do PID de temperatura do ar admitido.
     *
     * <p>O PID {@code 0x0F} retorna um único byte (A), cujo valor é calculado pela expressão:</p>
     *
     * <pre>
     * Temperatura (°C) = A - 40
     * </pre>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Temperatura do ar admitido em ºC ou o último valor válido quando a resposta for
     * inválida.
     */
    @Override
    public Integer parse(String response) {
        String data = getPidData(response);
        if (data == null)
            return getLastValue();

        try {
            int a = hexToInt(data);
            int intake = a - 40;
            setLastValue(intake);
            return intake;
        } catch (NumberFormatException e) {
            return getLastValue();
        }
    }
}