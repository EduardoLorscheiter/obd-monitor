/*
 **=================================================================================================
 **  @file    SupportedPidsCommand.java
 **  @author  Eduardo Lorscheiter
 **  @class   SupportedPidsCommand
 **  @date    17/08/2026
 **  @version 1.0
 **  @brief   Implementa a leitura dos blocos de PIDs OBD-II suportados pela ECU.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.commands;

public class SupportedPidsCommand extends ObdCommand<Long> {
    /**
     * PID {@code 0x00} - PIDs suportados pela ECU no intervalo de {@code 0x01} a {@code 0x20}.
     *
     * <p>Resposta: {@code 41 00 AA BB CC DD}</p>
     */
    public static final String SUPPORTED_PIDS_00_PID = "00";

    /**
     * PID {@code 0x20} - PIDs suportados pela ECU no intervalo de {@code 0x21} a {@code 0x40}.
     *
     * <p>Resposta: {@code 41 00 AA BB CC DD}</p>
     */
    public static final String SUPPORTED_PIDS_20_PID = "20";

    /**
     * PID {@code 0x40} - PIDs suportados pela ECU no intervalo de {@code 0x41} a {@code 0x60}.
     *
     * <p>Resposta: {@code 41 00 AA BB CC DD}</p>
     */
    public static final String SUPPORTED_PIDS_40_PID = "40";

    /**
     * Quantidade de PIDs representados por cada máscara de suporte.
     */
    public static final int PIDS_PER_RESPONSE = 32;

    /**
     * Cria um comando para consultar o bloco de PIDs iniciado pelo PID informado.
     *
     * @param firstPid PID de início do bloco, em hexadecimal.
     */
    public SupportedPidsCommand(String firstPid) {
        super(firstPid, 4, null);
    }

    /**
     * Verifica se o PID informado é suportado pela ECU.
     *
     * @param supportedPids Máscara de PIDs retornada pela ECU.
     * @param pid           PID a ser verificado.
     * @return {@code true} caso o PID seja suportado, {@code false} caso contrário.
     */
    public boolean isPidSupported(Long supportedPids, String pid) {
        int pidValue = Integer.parseInt(pid, 16);
        return isPidSupported(supportedPids, pidValue);
    }

    /**
     * Verifica se o PID informado é suportado pela ECU.
     *
     * @param supportedPids Máscara de PIDs retornada pela ECU.
     * @param pid           PID a ser verificado.
     * @return {@code true} caso o PID seja suportado, {@code false} caso contrário.
     */
    public boolean isPidSupported(Long supportedPids, int pid) {
        if (supportedPids == null)
            return false;

        int firstPid = Integer.parseInt(getPid(), 16);
        int offset = pid - firstPid;

        if (offset < 1 || offset > PIDS_PER_RESPONSE)
            return false;

        int bit = PIDS_PER_RESPONSE - offset;
        return (supportedPids & (1L << bit)) != 0;
    }

    /**
     * Converte a resposta do PID de PIDs suportados para uma máscara de bits.
     *
     * <p>O PID consultado retorna quatro bytes (A, B, C e D), onde cada bit representa um PID
     * suportado pela ECU.</p>
     *
     * <pre>
     * Máscara = (A × 2^24) + (B × 2^16) + (C × 2^8) + D
     * </pre>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Máscara de bits dos PIDs suportados ou {@code null} caso a resposta seja inválida.
     */
    @Override
    public Long parse(String response) {
        String data = getPidData(response);
        if (data == null)
            return getLastValue();

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
