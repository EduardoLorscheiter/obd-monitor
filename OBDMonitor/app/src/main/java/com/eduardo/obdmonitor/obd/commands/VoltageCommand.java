/*
 **=================================================================================================
 **  @file    VoltageCommand.java
 **  @author  Eduardo Lorscheiter
 **  @class   VoltageCommand
 **  @date    14/08/2026
 **  @version 1.0
 **  @brief   Implementa a leitura da tensão de alimentação do módulo de controle (ECU) em (V).
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.commands;

public class VoltageCommand extends ObdCommand<Double> {
    /**
     * PID {@code 0x42} - Tensão de alimentação do módulo de controle (ECU).
     *
     * <p>Resposta: 41 42 A B</p>
     * <p>Fórmula: Tensão (V) = ((A × 256) + B) / 1000</p>
     */
    private static final String VOLTAGE_PID = "42";

    /**
     * Cria um comando para consulta da tensão de alimentação do módulo de controle (ECU).
     */
    public VoltageCommand() {
        super(VOLTAGE_PID, 2, Double.NaN);
    }

    /**
     * Converte a resposta do PID de tensão de alimentação do módulo de controle (ECU).
     *
     * <p>O PID {@code 0x42} retorna dois bytes (A e B), cujo valor é calculado pela expressão:</p>
     *
     * <pre>
     * Tensão (V) = ((A × 256) + B) / 1000
     * </pre>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Tensão atual em volts ou {@link Double#NaN} para permitir a leitura alternativa
     * quando esta resposta for inválida, mesmo após uma leitura anterior válida.
     */
    @Override
    public Double parse(String response) {
        setLastValue(Double.NaN);
        String data = getPidData(response);
        if (data == null)
            return getLastValue();

        try {
            if (!data.matches("[0-9A-F]{4}"))
                return Double.NaN;
            int a = hexToInt(data.substring(0, 2));
            int b = hexToInt(data.substring(2, 4));
            double voltage = ((a * 256) + b) / 1000.0;
            setLastValue(voltage);
            return voltage;
        } catch (NumberFormatException e) {
            return getLastValue();
        }
    }
}
