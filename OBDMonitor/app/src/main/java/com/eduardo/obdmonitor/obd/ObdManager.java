/*
 **===========================================================================
 **  @file    ObdManager.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    Julho/2026
 **  @version 1.0
 **  @brief   OBDMonitor
 **===========================================================================
 */

package com.eduardo.obdmonitor.obd;

import androidx.annotation.NonNull;

import com.eduardo.obdmonitor.bluetooth.BluetoothManager;
import com.eduardo.obdmonitor.elm327.Elm327Commands;
import com.eduardo.obdmonitor.model.Device;

public class ObdManager {
    // Instância única da classe (singleton)
    private static ObdManager instance;

    // Listener utilizado para reportar eventos Bluetooth à interface
    private ObdListener listener;

    // Gerenciador responsável pela comunicação Bluetooth
    private BluetoothManager bluetoothManager;

    // Indica se o Bluetooth está conectado
    private boolean bluetoothConnected;

    // Indica se a ECU está conectada
    private boolean ecuConnected;

    // Protocolo OBD-II identificado durante a conexão
    private String protocol;

    // Indica se está realizando o monitoramento dos dados do OBD-II
    private boolean monitoring;

    /**
     * Retorna a instância única do gerenciador OBD-II.
     *
     * @return Instância única de {@code ObdManager}.
     */
    public static synchronized ObdManager getInstance() {
        if (instance == null) {
            instance = new ObdManager();
        }

        return instance;
    }

    /**
     * Cria uma nova instância do gerenciador do OBD-II.
     */
    private ObdManager() {
        bluetoothConnected = false;
        ecuConnected = false;
        protocol = null;
        monitoring = false;
    }

    /**
     * Define o listener dos eventos do OBD-II
     *
     * @param listener listener que receberá eventos do OBD-II
     */
    public void setListener(ObdListener listener) {
        this.listener = listener;

        if (listener != null) {
            listener.onObdBluetoothStatusChanged(bluetoothConnected);
            listener.onObdEcuStatusChanged(ecuConnected);
            listener.onObdProtocolChanged(protocol);
        }
    }

    /**
     * Solicita conexão com um dispositivo OBD-II através de um ELM327.
     *
     * <p>O BluetoothManager fará a conexão Bluetooth e retornará o resultado.
     * O dispositivo Bluetooth já deve estar conectado neste momento.</p>
     */
    public void connect(Device device, BluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
        bluetoothConnected = true;

        if (initializeElm327()) {
            ecuConnected = true;
            startMonitoring();
            if (listener != null) {
                listener.onObdConnected(device);
            }
        } else {
            bluetoothManager.disconnect();
            ecuConnected = false;
            if (listener != null) {
                listener.onObdConnectionFailed("O dispositivo não respondeu como um ELM327!");
            }
        }
    }

    /**
     * Inicializa o adaptador ELM327 para comunicação com a ECU.
     *
     * <p>São enviados os comandos de configuração do ELM327 e, em seguida,
     * é realizada uma requisição dos PIDs suportados pelo veículo para
     * validar a comunicação com a ECU.</p>
     *
     * @return True caso a inicialização seja concluída com sucesso, false caso contrário
     */
    private boolean initializeElm327() {
        if (!sendOk(Elm327Commands.RESET)) return false;
        if (!sendOk(Elm327Commands.ECHO_OFF)) return false;
        if (!sendOk(Elm327Commands.LINE_FEEDS_OFF)) return false;
        if (!sendOk(Elm327Commands.SPACES_OFF)) return false;
        if (!sendOk(Elm327Commands.HEADERS_OFF)) return false;
        if (!sendOk(Elm327Commands.AUTO_PROTOCOL)) return false;

        String response = executeElm327Command(ObdCommands.SUPPORTED_PIDS_01_20);

        if (response == null) {
            return false;
        }

        response = normalizeResponse(response);
        return response.contains("41 00");
    }

    /**
     * Envia um comando AT ao ELM327 e verifica se a resposta indica sucesso.
     *
     * @param command comando AT a ser enviado.
     * @return True caso o ELM327 responda com "OK" ou durante o processo de
     * inicialização, false caso contrário.
     */
    private boolean sendOk(String command) {
        String response = executeElm327Command(command);

        if (response == null) {
            return false;
        }

        response = normalizeResponse(response);
        return response.contains("OK") || response.contains("ELM327");
    }

    /**
     * Normaliza uma resposta recebida do ELM327.
     *
     * <p>Remove o caractere de prompt, quebras de linha, espaços
     * excedentes e converte todos os caracteres para letras maiúsculas,
     * facilitando o processamento da resposta.</p>
     *
     * @param response Resposta recebida do ELM327.
     * @return Resposta normalizada.
     */
    @NonNull
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
     * Inicia o monitoramento contínuo dos dados da ECU.
     *
     * <p>Enquanto o monitoramento estiver ativo, uma thread realiza
     * leituras periódicas da ECU e notifica o listener com os valores
     * obtidos.</p>
     */
    private void startMonitoring() {
        if (!ecuConnected || monitoring) {
            return;
        }

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
     * Realiza a leitura da rotação do motor (RPM).
     *
     * @return valor da rotação do motor em RPM ou zero caso não seja
     * possível obter uma resposta válida.
     */
    private int readRpm() {
        String response = executeElm327Command(ObdCommands.ENGINE_RPM);
        return parseRpm(response);
    }

    /**
     * Converte a resposta do PID de RPM em seu valor numérico.
     *
     * <p>O PID 0x0C retorna dois bytes (A e B), cujo valor é calculado
     * pela expressão:</p>
     *
     * <pre>
     * RPM = ((A × 256) + B) / 4
     * </pre>
     *
     * @param response Resposta recebida do ELM327.
     * @return Rotação do motor em RPM ou zero caso a resposta seja inválida.
     */
    private int parseRpm(String response) {
        if (response == null) {
            return 0;
        }

        response = normalizeResponse(response);

        String[] data = response.split(" ");

        for (int i = 0; i < data.length - 3; i++) {
            if (data[i].equals("41") && data[i + 1].equals("0C")) {
                try {
                    int A = Integer.parseInt(data[i + 2], 16);
                    int B = Integer.parseInt(data[i + 3], 16);
                    return ((A * 256) + B) / 4;
                } catch (Exception e) {
                    return 0;
                }
            }
        }

        return 0;
    }

    /**
     * Envia um comando ao ELM327 e retorna sua resposta.
     *
     * <p>O caractere de retorno de carro ('\r') é acrescentado
     * automaticamente ao final do comando antes do envio.</p>
     *
     * @param command Comando a ser enviado
     * @return Resposta retornada pelo ELM327 ou {@code null} em caso de falha.
     */
    private String executeElm327Command(String command) {
        return bluetoothManager.sendCommand(command + "\r");
    }

    /**
     * Desconecta do dispositivo atual.
     */
    public void disconnect() {
        bluetoothConnected = false;
        ecuConnected = false;
        protocol = null;
        monitoring = false;

        if (bluetoothManager != null) {
            bluetoothManager.disconnect();
            bluetoothManager = null;
        }

        if (listener != null) {
            listener = null;
        }
    }
}