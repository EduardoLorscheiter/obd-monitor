/*
 **===========================================================================
 **  @file    ObdParser.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Responsável por interpretar as respostas retornadas pela ECU
 **           através do protocolo OBD-II.
 **===========================================================================
 */

package com.eduardo.obdmonitor.obd;

public final class ObdParser {
    /**
     * Classe utilitária. Não permite instanciação.
     */
    private ObdParser() {
    }

    /**
     * Converte a resposta do PID 0x0C em RPM.
     *
     * <p>O PID 0x0C retorna dois bytes (A e B), cujo valor é calculado pela expressão:</p>
     *
     * <pre>
     * RPM = ((A × 256) + B) / 4
     *
     * Onde:
     * A = primeiro byte
     * B = segundo byte
     * </pre>
     *
     * @param response Resposta recebida do ELM327.
     * @return Rotação do motor em RPM ou zero caso a resposta seja inválida.
     */
    public static int parseRpm(String response) {
        String data = getPidData(response, "0C", 2);

        if (data == null) {
            return 0;
        }

        try {
            int a = Integer.parseInt(data.substring(0, 2), 16);
            int b = Integer.parseInt(data.substring(2, 4), 16);

            return ((a * 256) + b) / 4;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int parseVehicleSpeed(String response) {
        String data = getPidData(response, "0D", 1);

        if (data == null) {
            return 0;
        }

        try {
            return Integer.parseInt(data, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String getPidData(String response, String pid, int bytes) {
        int index = response.indexOf("41" + pid);

        if (index == -1) {
            return null;
        }

        int start = index + 4;
        int end = start + (bytes * 2);

        if (response.length() < end) {
            return null;
        }

        return response.substring(start, end);
    }
}