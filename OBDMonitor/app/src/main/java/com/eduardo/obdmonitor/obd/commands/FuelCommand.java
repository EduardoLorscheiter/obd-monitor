/*
 **=================================================================================================
 **  @file    FuelCommand.java
 **  @author  Eduardo Lorscheiter
 **  @class   FuelCommand
 **  @date    20/08/2026
 **  @version 1.0
 **  @brief   Implementa a leitura do nível de combustível do veículo em %.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.commands;

public class FuelCommand extends ObdCommand<Integer> {
    /**
     * PID {@code 0x2F} - Nível de combustível do veículo.
     *
     * <p>Resposta: 41 2F A</p>
     * <p>Fórmula: Combustível (%) = (A × 100) / 255</p>
     */
    private static final String FUEL_PID = "2F";

    /**
     * Cria um comando para consulta do nível de combustível do veículo.
     */
    public FuelCommand() {
        super(FUEL_PID, 1, 0);
    }

    /**
     * Converte a resposta do PID de nível de combustível do veículo.
     *
     * <p>O PID {@code 0x2F} retorna um único byte (A), cujo valor é calculado pela expressão:</p>
     *
     * <pre>
     * Combustível (%) = (A × 100) / 255
     * </pre>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Nível de combustível em % ou o último valor válido quando a resposta for inválida.
     */
    @Override
    public Integer parse(String response) {
        String data = getPidData(response);
        if (data == null)
            return getLastValue();

        try {
            int a = hexToInt(data);
            int fuel = (a * 100) / 255;
            setLastValue(fuel);
            return fuel;
        } catch (NumberFormatException e) {
            return getLastValue();
        }
    }
}