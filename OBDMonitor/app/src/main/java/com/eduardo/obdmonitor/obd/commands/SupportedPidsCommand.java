/*
 **=================================================================================================
 **  @file    SupportedPidsCommand.java
 **  @author  Eduardo Lorscheiter
 **  @class   SupportedPidsCommand
 **  @date    17/08/2026
 **  @version 1.0
 **  @brief   Implementa a leitura dos PIDs OBD-II suportados pela ECU no intervalo de 0x01 a 0x20.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.commands;

public class SupportedPidsCommand extends ObdCommand<Long> {
    /**
     * PID {@code 0x00} - PIDs suportados pela ECU no intervalo de {@code 0x01} a {@code 0x20}.
     *
     * <p>Resposta: {@code 41 00 AA BB CC DD}</p>
     */
    private static final String SUPPORTED_PIDS_PID = "00";

    /**
     * Cria um comando para consulta dos PIDs suportados pela ECU.
     */
    public SupportedPidsCommand() {
        super(SUPPORTED_PIDS_PID, 4, 0L);
    }

    /**
     * Verifica se a resposta recebida corresponde a uma resposta válida do PID {@code 0x00}.
     *
     * <p>Uma resposta válida deve conter o identificador de resposta {@code 41}, o PID {@code 00} e
     * os quatro bytes que representam os PIDs suportados pela ECU.</p>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return {@code true} caso a resposta seja válida, {@code false} caso contrário.
     */
    public boolean isValid(String response) {
        return getPidData(response) != null;
    }

    /**
     * Converte a resposta do PID de PIDs suportados para uma máscara de bits.
     *
     * <p>O PID {@code 0x00} retorna quatro bytes (A, B, C e D), onde cada bit representa um PID
     * suportado pela ECU.</p>
     *
     * <pre>
     * Máscara = (A × 2^24) + (B × 2^16) + (C × 2^8) + D
     * </pre>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Máscara de bits dos PIDs suportados ou o último valor válido caso a resposta seja
     * inválida.
     */
    @Override
    public Long parse(String response) {
        String data = getPidData(response);
        if (data == null) return getLastValue();

        try {
            long a = hexToInt(data.substring(0, 2));
            long b = hexToInt(data.substring(2, 4));
            long c = hexToInt(data.substring(4, 6));
            long d = hexToInt(data.substring(6, 8));
            long supportedPids = (a << 24) | (b << 16) | (c << 8) | d;
            setLastValue(supportedPids);
            return supportedPids;
        } catch (NumberFormatException e) {
            return getLastValue();
        }
    }
}