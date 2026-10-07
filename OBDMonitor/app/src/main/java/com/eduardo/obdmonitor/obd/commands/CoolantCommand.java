/*
 **=================================================================================================
 **  @file    CoolantCommand.java
 **  @author  Eduardo Lorscheiter
 **  @class   CoolantCommand
 **  @date    14/08/2026
 **  @version 1.0
 **  @brief   Implementa a leitura da temperatura do líquido de arrefecimento do motor em °C.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.commands;

public class CoolantCommand extends ObdCommand<Integer> {
    /**
     * PID {@code 0x05} - Temperatura do líquido de arrefecimento.
     *
     * <p>Resposta: 41 05 A</p>
     * <p>Fórmula: Temperatura (°C) = A - 40</p>
     */
    static final String COOLANT_PID = "05";

    /**
     * Cria um comando para consulta da temperatura do líquido de arrefecimento.
     */
    public CoolantCommand() {
        super(COOLANT_PID, 1, 0);
    }

    /**
     * Converte a resposta do PID de temperatura do líquido de arrefecimento.
     *
     * <p>O PID {@code 0x05} retorna um único byte (A), cujo valor é calculado pela expressão:</p>
     *
     * <pre>
     * Temperatura (°C) = A - 40
     * </pre>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Temperatura do líquido de arrefecimento em ºC ou o último valor válido quando a
     * resposta for inválida.
     */
    @Override
    public Integer parse(String response) {
        String data = getPidData(response);
        if (data == null)
            return getLastValue();

        try {
            int a = hexToInt(data);
            int coolant = a - 40;
            setLastValue(coolant);
            return coolant;
        } catch (NumberFormatException e) {
            return getLastValue();
        }
    }
}