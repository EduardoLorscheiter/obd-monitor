/*
 **=================================================================================================
 **  @file    ObdParser.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Responsável por interpretar as respostas retornadas pela ECU através do protocolo
 **           OBD-II.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd;

public final class ObdParser {
    // Código que identifica uma resposta ao Modo de Serviço 01 (Current Data)
    private static final String SERVICE_01_RESPONSE = "41";

    /**
     * Classe utilitária. Não permite instanciação.
     */
    private ObdParser() {
    }

    /**
     * Converte a resposta do PID de rotação do motor (RPM)
     *
     * <p>O PID {@code 0x0C} retorna dois bytes (A e B), cujo valor é calculado pela expressão:</p>
     *
     * <pre>
     * RPM = ((A × 256) + B) / 4
     * </pre>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Rotação do motor em RPM ou {@code 0} caso a resposta seja inválida.
     */
    static int parseEngineRpm(String response) {
        String data = getPidData(response, ObdPids.ENGINE_RPM, 2);

        if (data == null)
            return 0;

        try {
            int a = parseByte(data.substring(0, 2));
            int b = parseByte(data.substring(2, 4));
            return ((a * 256) + b) / 4;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Converte a resposta do PID de velocidade do veículo.
     *
     * <p>O PID {@code 0x0D} retorna um único byte (A), cujo valor representa diretamente a
     * velocidade em quilômetros por hora.</p>
     *
     * <pre>
     * Velocidade (km/h) = A
     * </pre>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Velocidade do veículo em km/h ou {@code 0} caso a resposta seja inválida.
     */
    static int parseVehicleSpeed(String response) {
        String data = getPidData(response, ObdPids.VEHICLE_SPEED, 1);

        if (data == null)
            return 0;

        try {
            return parseByte(data);
        } catch (NumberFormatException e) {
            return 0;
        }
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
     * @return Temperatura do líquido de arrefecimento em ºC ou* {@code 0} caso a resposta seja
     * inválida.
     */
    static int parseEngineCoolantTemperature(String response) {
        String data = getPidData(response, ObdPids.ENGINE_COOLANT_TEMPERATURE, 1);

        if (data == null)
            return 0;

        try {
            int a = parseByte(data);
            return a - 40;
        } catch (NumberFormatException e) {
            return 0;
        }
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
     * @return Temperatura do ar admitido em ºC ou {@code 0} caso a resposta seja inválida.
     */
    static int parseIntakeAirTemperature(String response) {
        String data = getPidData(response, ObdPids.INTAKE_AIR_TEMPERATURE, 1);

        if (data == null)
            return 0;

        try {
            int a = parseByte(data);
            return a - 40;
        } catch (NumberFormatException e) {
            return 0;
        }
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
     * @return Pressão absoluta do coletor de admissão (MAP) em kPa ou {@code 0} caso a resposta
     * seja inválida.
     */
    static int parseIntakeManifoldPressure(String response) {
        String data = getPidData(response, ObdPids.INTAKE_MANIFOLD_PRESSURE, 1);

        if (data == null)
            return 0;

        try {
            return parseByte(data);
        } catch (NumberFormatException e) {
            return 0;
        }
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
     * @return Posição absoluta da borboleta de aceleração em % ou {@code 0} caso a resposta seja
     * inválida.
     */
    static int parseThrottlePosition(String response) {
        String data = getPidData(response, ObdPids.THROTTLE_POSITION, 1);

        if (data == null)
            return 0;

        try {
            int a = parseByte(data);
            return (a * 100) / 255;
        } catch (NumberFormatException e) {
            return 0;
        }
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
     * @return Tensão de alimentação do módulo de controle em V ou {@code 0.0} caso a resposta seja
     * inválida.
     */
    static double parseControlModuleVoltage(String response) {
        String data = getPidData(response, ObdPids.CONTROL_MODULE_VOLTAGE, 2);

        if (data == null)
            return 0.0;

        try {
            int a = parseByte(data.substring(0, 2));
            int b = parseByte(data.substring(2, 4));
            return ((a * 256) + b) / 1000.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Obtém os bytes de dados associados a um PID em uma resposta OBD-II.
     *
     * <p>Localiza a resposta correspondente ao PID informado e retorna apenas os bytes de dados,
     * desconsiderando o código da resposta e o identificador do PID.</p>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @param pid      PID que será localizado na resposta.
     * @param bytes    Quantidade de bytes esperada para o PID.
     * @return Sequência hexadecimal correspondente aos dados do PID ou {@code null} caso a resposta
     * seja inválida.
     */
    private static String getPidData(String response, String pid, int bytes) {
        int index = response.indexOf(SERVICE_01_RESPONSE + pid);

        if (index == -1)
            return null;

        int start = index + 4;
        int end = start + (bytes * 2);

        if (response.length() < end)
            return null;

        return response.substring(start, end);
    }

    /**
     * Converte um byte representado em hexadecimal para seu valor inteiro.
     *
     * <p>O valor informado deve conter exatamente dois caracteres* hexadecimais, por exemplo
     * {@code "0A"}, {@code "7F"} ou {@code "FF"}.</p>
     *
     * @param value Byte em formato hexadecimal.
     * @return Valor inteiro correspondente ao byte hexadecimal.
     * @throws NumberFormatException Caso a sequência não represente um valor hexadecimal válido.
     */
    private static int parseByte(String value) {
        return Integer.parseInt(value, 16);
    }
}