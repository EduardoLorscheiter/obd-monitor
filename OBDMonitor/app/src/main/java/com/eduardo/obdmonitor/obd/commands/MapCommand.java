/*
 **=================================================================================================
 **  @file    MapCommand.java
 **  @author  Eduardo Lorscheiter
 **  @class   MapCommand
 **  @date    14/08/2026
 **  @version 1.0
 **  @brief   Implementa a leitura da pressão absoluta do coletor de admissão (MAP) em kPa.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.commands;

public class MapCommand extends ObdCommand<Integer> {
    /**
     * PID {@code 0x0B} - Pressão absoluta do coletor de admissão (MAP).
     *
     * <p>Resposta: 41 0B A</p>
     * <p>Fórmula: Pressão (kPa) = A</p>
     */
    private static final String MAP_PID = "0B";

    /**
     * Cria um comando para consulta da pressão absoluta do coletor de admissão (MAP).
     */
    public MapCommand() {
        super(MAP_PID, 1, 0);
    }

    /**
     * Converte a resposta do PID de pressão absoluta do coletor de admissão (MAP).
     *
     * <p>O PID {@code 0x0B} retorna um único byte (A), cujo valor representa diretamente a pressão
     * em quilopascal.</p>
     *
     * <pre>
     * Pressão (kPa) = A
     * </pre>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Pressão absoluta do coletor de admissão (MAP) em kPa ou o último valor válido caso a
     * resposta seja inválida.
     */
    @Override
    public Integer parse(String response) {
        String data = getPidData(response);
        if (data == null)
            return getLastValue();

        try {
            int map = hexToInt(data);
            setLastValue(map);
            return map;
        } catch (NumberFormatException e) {
            return getLastValue();
        }
    }
}