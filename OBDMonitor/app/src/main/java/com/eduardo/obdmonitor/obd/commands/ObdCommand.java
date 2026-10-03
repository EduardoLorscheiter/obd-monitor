/*
 **=================================================================================================
 **  @file    ObdCommand.java
 **  @author  Eduardo Lorscheiter
 **  @class   ObdCommand
 **  @date    14/08/2026
 **  @version 1.0
 **  @brief   Classe base abstrata para todos os comandos OBD-II, fornecendo métodos utilitários
 **           para montagem de requisições e processamento de respostas.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.commands;

public abstract class ObdCommand<T> {
    /**
     * Código que identifica uma requisição do Modo de Serviço 01 (Current Data).
     */
    private static final String SERVICE_CURRENT_DATA_REQUEST = "01";

    /**
     * Código que identifica uma resposta ao Modo de Serviço 01 (Current Data).
     */
    private static final String SERVICE_CURRENT_DATA_RESPONSE = "41";

    /**
     * PID do comando OBD-II.
     */
    private final String pid;

    /**
     * Número de bytes que o comando OBD-II espera como resposta.
     */
    private final int dataBytes;

    /**
     * Último valor válido obtido a partir do comando OBD-II.
     */
    private T lastValue;

    /**
     * Cria um novo comando OBD-II associado a um PID.
     *
     * @param pid          Identificador hexadecimal do PID OBD-II.
     * @param dataBytes    Quantidade de bytes de dados esperados na resposta.
     * @param initialValue Valor inicial para o comando caso a primeira resposta for inválida.
     */
    protected ObdCommand(String pid, int dataBytes, T initialValue) {
        this.pid = pid;
        this.dataBytes = dataBytes;
        this.lastValue = initialValue;
    }

    /**
     * Monta a requisição OBD-II correspondente ao PID do comando.
     *
     * <p>A requisição utiliza o Modo de Serviço 01 (Current Data), sendo composta pelo código do
     * serviço e pelo identificador do PID.</p>
     *
     * @return Requisição OBD-II pronta para ser enviada ao ELM327.
     */
    public String getRequest() {
        return SERVICE_CURRENT_DATA_REQUEST + " " + pid;
    }

    /**
     * Processa uma resposta OBD-II e converte os dados recebidos para o tipo correspondente ao
     * comando.
     *
     * <p>Cada implementação deve fornecer a lógica específica para interpretar os bytes retornados
     * pelo respectivo PID.</p>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Valor processado correspondente ao PID.
     */
    public abstract T parse(String response);

    /**
     * Retorna o último valor válido obtido pelo comando.
     *
     * @return Último valor válido ou o valor inicial caso ainda não tenha sido realizada uma
     * leitura válida.
     */
    protected T getLastValue() {
        return lastValue;
    }

    /**
     * Atualiza o último valor válido obtido pelo comando.
     *
     * @param lastValue Novo valor válido.
     */
    protected void setLastValue(T lastValue) {
        this.lastValue = lastValue;
    }

    /**
     * Obtém os bytes de dados associados ao PID em uma resposta OBD-II.
     *
     * <p>Localiza a resposta correspondente ao Modo de Serviço 01 e ao PID do comando, retornando
     * somente os bytes de dados e desconsiderando o código da resposta e o identificador do PID.
     * </p>
     *
     * @param response Resposta normalizada recebida do ELM327.
     * @return Sequência hexadecimal correspondente aos dados do PID ou {@code null} caso a resposta
     * não contenha o PID esperado ou não possua a quantidade necessária de bytes.
     */
    protected String getPidData(String response) {
        if (response == null)
            return null;

        String header = SERVICE_CURRENT_DATA_RESPONSE + pid;
        int index = response.indexOf(header);
        if (index == -1)
            return null;

        int start = index + header.length();
        int end = start + (dataBytes * 2);

        return (response.length() >= end) ? response.substring(start, end) : null;
    }

    /**
     * Converte uma sequência hexadecimal correspondente a um byte para seu valor inteiro.
     *
     * <p>O valor informado deve conter exatamente dois caracteres hexadecimais, como
     * {@code "0A"}, {@code "7F"} ou {@code "FF"}.</p>
     *
     * @param hex Byte representado em formato hexadecimal.
     * @return Valor inteiro correspondente ao byte hexadecimal.
     * @throws NumberFormatException Caso a sequência informada não represente um valor hexadecimal
     *                               válido.
     */
    protected int hexToInt(String hex) {
        return Integer.parseInt(hex, 16);
    }
}