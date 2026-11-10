/*
 **=================================================================================================
 **  @file    Elm327Commands.java
 **  @author  Eduardo Lorscheiter
 **  @class   Elm327Commands
 **  @date    21/07/2026
 **  @version 1.0
 **  @brief   Comandos AT (Attention Commands) utilizados para configuração e controle do adaptador
 **           ELM327.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.elm327;

public final class Elm327Commands {
    /**
     * Classe utilitária. Não permite instanciação.
     */
    private Elm327Commands() {
    }

    /**
     * Reinicia o adaptador ELM327.
     */
    public static final String RESET = "ATZ";

    /**
     * Desabilita o eco dos comandos enviados ao adaptador.
     */
    public static final String ECHO_OFF = "ATE0";

    /**
     * Desabilita a exibição de quebras de linha nas respostas.
     */
    public static final String LINE_FEEDS_OFF = "ATL0";

    /**
     * Desabilita a impressão de espaços nas respostas.
     */
    public static final String SPACES_OFF = "ATS0";

    /**
     * Desabilita a exibição dos cabeçalhos das mensagens CAN.
     */
    public static final String HEADERS_OFF = "ATH0";

    /**
     * Seleciona automaticamente o protocolo OBD-II utilizado pelo veículo.
     */
    public static final String AUTO_PROTOCOL = "ATSP0";

    /**
     * Solicita ao ELM327 o protocolo atualmente utilizado para comunicação com a ECU.
     */
    public static final String GET_PROTOCOL = "ATDP";

    /**
     * Solicita ao ELM327 a tensão de alimentação do adaptador.
     *
     * <p>O valor retornado corresponde à tensão medida na alimentação do próprio ELM327.</p>
     */
    public static final String READ_VOLTAGE = "ATRV";

    /**
     * Converte a tensão medida pelo adaptador, sem preservar valores antigos.
     *
     * @param response Resposta normalizada do ATRV, por exemplo {@code 12.6V}.
     * @return Tensão em volts ou {@link Double#NaN} para resposta ausente ou inválida.
     */
    public static double parseVoltage(String response) {
        if (response == null || !response.trim().matches("[0-9]{1,2}\\.[0-9]\\s*V"))
            return Double.NaN;
        return Double.parseDouble(response.trim().replace("V", "").trim());
    }
}
