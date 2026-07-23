/*
 **===========================================================================
 **  @file    ObdManager.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief Gerencia a comunicação entre a aplicação, o adaptador
 **         ELM327 e a ECU do veículo, realizando a inicialização,
 **         monitoramento e leitura dos dados OBD-II.
 **===========================================================================
 */

package com.eduardo.obdmonitor.obd;

import com.eduardo.obdmonitor.bluetooth.BluetoothManager;
import com.eduardo.obdmonitor.elm327.Elm327Commands;
import com.eduardo.obdmonitor.model.Device;

public class ObdManager {
    // Instância única do gerenciador OBD-II (Singleton)
    private static ObdManager instance;

    // Listener utilizado para reportar eventos Bluetooth à interface
    private ObdListener listener;

    // Gerenciador responsável pela comunicação Bluetooth
    private BluetoothManager bluetoothManager;

    // Indica se está realizando o monitoramento dos dados do OBD-II
    private boolean monitoring;

    // Indica se o Bluetooth está conectado
    private boolean bluetoothConnected;

    // Indica se a ECU está conectada
    private boolean ecuConnected;

    // Protocolo OBD-II identificado durante a conexão
    private String obdProtocol;

    /**
     * Retorna a instância única do gerenciador OBD-II.
     *
     * @return Instância única de {@code ObdManager}.
     */
    public static synchronized ObdManager getInstance() {
        if (instance == null)
            instance = new ObdManager();

        return instance;
    }

    /**
     * Cria uma nova instância do gerenciador do OBD-II.
     */
    private ObdManager() {
        listener = null;
        monitoring = false;

        bluetoothConnected = false;
        ecuConnected = false;
        obdProtocol = null;

        updateObdInformation();
    }

    /**
     * Define o listener dos eventos do OBD-II.
     *
     * @param listener Listener que receberá eventos do OBD-II.
     */
    public void setListener(ObdListener listener) {
        this.listener = listener;

        updateObdInformation();
    }

    /**
     * Inicializa a comunicação com o adaptador ELM327 e estabelece
     * a comunicação com a ECU do veículo.
     *
     * <p>Caso a inicialização seja concluída com sucesso, o monitoramento
     * contínuo dos dados da ECU é iniciado automaticamente.</p>
     *
     * @param device           Dispositivo OBD-II conectado.
     * @param bluetoothManager Gerenciador responsável pela comunicação Bluetooth.
     * @throws InterruptedException Caso a thread seja interrompida durante a inicialização.
     */
    public void connect(Device device, BluetoothManager bluetoothManager) throws InterruptedException {
        this.bluetoothManager = bluetoothManager;

        if (initializeElm327()) {
            bluetoothConnected = true;
            ecuConnected = true;
            startMonitoring();
            if (listener != null) {
                listener.onObdConnected(device);
            }
        } else {
            bluetoothManager.disconnect();
            bluetoothConnected = false;
            ecuConnected = false;
            if (listener != null) {
                listener.onObdError("O dispositivo não respondeu como um ELM327!");
            }
        }

        updateObdInformation();
    }

    /**
     * Encerra o monitoramento dos dados da ECU e finaliza a comunicação com o adaptador OBD-II.
     */
    public void disconnect() {
        monitoring = false;

        bluetoothConnected = false;
        ecuConnected = false;
        obdProtocol = null;

        updateObdInformation();

        if (bluetoothManager != null) {
            bluetoothManager.disconnect();
            bluetoothManager = null;
        }

        if (listener != null) {
            listener = null;
        }
    }

    /**
     * Inicializa o adaptador ELM327.
     *
     * <p>Durante a inicialização são enviados os comandos AT necessários
     * para configurar o adaptador antes da comunicação com a ECU.</p>
     *
     * <p>Ao final, é realizada uma leitura dos PIDs suportados para validar
     * a comunicação com o veículo.</p>
     *
     * @return {@code true} caso a inicialização seja concluída com sucesso,
     * {@code false} caso contrário.
     */
    private boolean initializeElm327() throws InterruptedException {
        if (!sendOk(Elm327Commands.RESET)) return false;
        Thread.sleep(1000);
        if (!sendOk(Elm327Commands.ECHO_OFF)) return false;
        if (!sendOk(Elm327Commands.LINE_FEEDS_OFF)) return false;
        if (!sendOk(Elm327Commands.SPACES_OFF)) return false;
        if (!sendOk(Elm327Commands.HEADERS_OFF)) return false;
        if (!sendOk(Elm327Commands.AUTO_PROTOCOL)) return false;
        Thread.sleep(10000);

        String response = executeElm327Command(ObdCommands.SUPPORTED_PIDS_01_20);

        if (response == null)
            return false;

        return response.contains("4100");
    }

    /**
     * Envia um comando AT ao ELM327 e verifica se a resposta indica sucesso.
     *
     * @param command comando AT a ser enviado.
     * @return {@code true} caso o ELM327 contenha a resposta "OK" ou durante o processo de
     * inicialização, {@code false} caso contrário.
     */
    private boolean sendOk(String command) {
        String response = executeElm327Command(command);

        if (response == null)
            return false;

        return response.contains("OK") || response.contains("ELM327");
    }

    /**
     * Inicia o monitoramento contínuo dos dados da ECU.
     *
     * <p>Enquanto o monitoramento estiver ativo, uma thread realiza
     * leituras periódicas da ECU e notifica o listener com os valores
     * obtidos.</p>
     *
     * <p>O monitoramento é executado em uma thread dedicada.</p>
     */
    private void startMonitoring() {
        if (!ecuConnected || monitoring)
            return;

        monitoring = true;

        new Thread(() -> {
            while (monitoring) {
                int rpm = readRpm();

                if (listener != null) {
                    listener.onObdRpmChanged(rpm);
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (listener != null) {
                        listener.onObdError("Monitoramento do OBD-II interrompido!");
                    }
                    return;
                }
            }
        }).start();
    }

    /**
     * Consulta o protocolo de comunicação utilizado entre o ELM327 e a ECU.
     *
     * @return Nome do protocolo detectado ou {@code null}
     * caso não seja possível identificá-lo.
     */
    private String readProtocol() {
        obdProtocol = executeElm327Command(Elm327Commands.GET_PROTOCOL);
        return obdProtocol;
    }

    /**
     * Realiza a leitura da rotação do motor (RPM).
     *
     * @return Valor da rotação do motor em RPM ou zero caso não seja
     * possível obter uma resposta válida.
     */
    private int readRpm() {
        String response = executeElm327Command(ObdCommands.ENGINE_RPM);
        return ObdParser.parseRpm(response);
    }

    /**
     * Envia um comando ao ELM327 e retorna sua resposta.
     *
     * <p>O caractere de retorno de carro ('\r') é acrescentado automaticamente
     * ao final do comando antes do envio.</p>
     *
     * <p>A resposta retornada é normalizada antes de ser entregue ao chamador.</p>
     *
     * @param command Comando a ser enviado
     * @return Resposta retornada pelo ELM327 ou {@code null} em caso de falha.
     */
    private String executeElm327Command(String command) {
        String response = bluetoothManager.sendCommand(command + "\r");

        if (response == null) {
            return null;
        }

        return normalizeResponse(response);
    }

    /**
     * Normaliza uma resposta recebida do ELM327.
     *
     * <p>Remove o prompt de término ('>'), quebras de linha, espaços
     * excedentes e converte todos os caracteres para letras maiúsculas,
     * facilitando o processamento da resposta.</p>
     *
     * @param response Resposta recebida do ELM327.
     * @return Resposta normalizada.
     */
    private static String normalizeResponse(String response) {
        response = response
                .replace(">", "")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
        return response;
    }


    /**
     * Atualiza o listener com o estado atual da conexão OBD-II.
     *
     * <p>São notificadas as informações de conexão Bluetooth,
     * comunicação com a ECU e protocolo utilizado.</p>
     */
    private void updateObdInformation() {
        if (listener != null) {
            listener.onObdBluetoothStatusChanged(bluetoothConnected);
            listener.onObdEcuStatusChanged(ecuConnected);
            listener.onObdProtocolChanged(obdProtocol);
        }
    }
}