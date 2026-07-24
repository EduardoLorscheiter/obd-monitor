/*
 **===========================================================================
 **  @file    ObdManager.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Gerencia a comunicação entre a aplicação, o adaptador
 **           ELM327 e a ECU do veículo, realizando a inicialização,
 **           monitoramento e leitura dos dados OBD-II.
 **===========================================================================
 */

package com.eduardo.obdmonitor.obd;

import com.eduardo.obdmonitor.bluetooth.BluetoothManager;
import com.eduardo.obdmonitor.elm327.Elm327Commands;
import com.eduardo.obdmonitor.model.Device;

public class ObdManager {
    // Instância única do gerenciador OBD-II (Singleton)
    private static ObdManager instance;

    // Listener responsável por receber eventos relacionados ao estado da conexão OBD-II
    private ObdConnectionListener connectionListener;

    // Listener responsável por receber os dados monitorados da ECU
    private ObdDataListener dataListener;

    // Gerenciador responsável pela comunicação Bluetooth
    private BluetoothManager bluetoothManager;

    // Indica se está realizando o monitoramento dos dados do OBD-II
    // Obs.: O 'volatile' garante visibilidade entre threads
    private volatile boolean monitoring;

    // Indica se o Bluetooth está conectado
    private boolean bluetoothConnected;

    // Indica se a ECU está conectada
    private boolean ecuConnected;

    // Protocolo OBD-II identificado durante a conexão
    private String obdProtocol;

    // Thread de monitoramento contínuo dos dados da ECU.
    private Thread monitoringThread;

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
        connectionListener = null;
        dataListener = null;
        bluetoothManager = null;

        resetConnectionState();
    }

    /**
     * Define os listeners responsáveis por receber os eventos da comunicação OBD-II.
     *
     * <p>O listener de conexão recebe as alterações de estado da comunicação
     * com o adaptador ELM327 e a ECU, enquanto o listener de dados recebe
     * os valores monitorados durante a comunicação.</p>
     *
     * @param connectionListener Listener que receberá os eventos de conexão OBD-II.
     * @param dataListener       Listener que receberá os dados monitorados da ECU.
     */
    public void setListeners(ObdConnectionListener connectionListener,
                             ObdDataListener dataListener) {
        this.connectionListener = connectionListener;
        this.dataListener = dataListener;

        notifyConnectionState();
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
            readProtocol();
            notifyConnectionState();
            startMonitoring();
            if (connectionListener != null)
                connectionListener.onObdConnected(device);
        } else {
            bluetoothManager.disconnect();
            resetConnectionState();
            if (connectionListener != null)
                connectionListener.onObdError("O dispositivo não respondeu como um ELM327!");
        }
    }

    /**
     * Encerra o monitoramento dos dados da ECU e finaliza a comunicação com o adaptador OBD-II.
     */
    public void disconnect() {
        monitoring = false;

        if (monitoringThread != null) {
            monitoringThread.interrupt();
            monitoringThread = null;
        }

        if (bluetoothManager != null) {
            bluetoothManager.disconnect();
            bluetoothManager = null;
        }

        resetConnectionState();
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
     * @throws InterruptedException Caso a thread seja interrompida durante
     *                              o tempo de espera entre os comandos de inicialização.
     */
    private boolean initializeElm327() throws InterruptedException {
        if (!sendCommandOk(Elm327Commands.RESET)) return false;
        Thread.sleep(1000);
        if (!sendCommandOk(Elm327Commands.ECHO_OFF)) return false;
        if (!sendCommandOk(Elm327Commands.LINE_FEEDS_OFF)) return false;
        if (!sendCommandOk(Elm327Commands.SPACES_OFF)) return false;
        if (!sendCommandOk(Elm327Commands.HEADERS_OFF)) return false;
        if (!sendCommandOk(Elm327Commands.AUTO_PROTOCOL)) return false;
        Thread.sleep(5000);

        String response = executeElm327Command(ObdCommands.SUPPORTED_PIDS_01_20);
        return response != null && response.contains("4100");
    }

    /**
     * Envia um comando AT ao ELM327 e verifica se a resposta indica sucesso.
     *
     * @param command comando AT a ser enviado.
     * @return {@code true} caso o ELM327 contenha a resposta "OK" ou durante o processo de
     * inicialização, {@code false} caso contrário.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean sendCommandOk(String command) {
        String response = executeElm327Command(command);

        if (response == null)
            return false;

        return response.contains("ELM327") || response.contains("OK");
    }

    /**
     * Inicia o monitoramento contínuo dos dados da ECU.
     *
     * <p>Enquanto o monitoramento estiver ativo, uma thread realiza leituras periódicas
     * da ECU e notifica o listener responsável pelos dados monitorados.</p>
     *
     * <p>O monitoramento é executado em uma thread dedicada.</p>
     */
    @SuppressWarnings("BusyWait")
    private void startMonitoring() {
        if (!ecuConnected || monitoring)
            return;

        monitoring = true;

        monitoringThread = new Thread(() -> {
            while (monitoring) {
                int engineRpm = readEngineRpm();
                int vehicleSpeed = readVehicleSpeed();
                int engineCoolantTemperature = readEngineCoolantTemperature();
                int intakeAirTemperature = readIntakeAirTemperature();
                int intakeManifoldPressure = readIntakeManifoldPressure();
                int throttlePosition = readThrottlePosition();
                double controlModuleVoltage = readControlModuleVoltage();

                if (dataListener != null) {
                    dataListener.onObdEngineRpmChanged(engineRpm);
                    dataListener.onObdVehicleSpeedChanged(vehicleSpeed);
                    dataListener.onObdEngineCoolantTemperatureChanged(engineCoolantTemperature);
                    dataListener.onObdIntakeAirTemperatureChanged(intakeAirTemperature);
                    dataListener.onObdIntakeManifoldPressureChanged(intakeManifoldPressure);
                    dataListener.onObdThrottlePositionChanged(throttlePosition);
                    dataListener.onObdControlModuleVoltageChanged(controlModuleVoltage);
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (monitoring && connectionListener != null)
                        connectionListener.onObdError("Monitoramento do OBD-II interrompido!");
                    return;
                } finally {
                    monitoringThread = null;
                }
            }
        }, "OBD-Monitor");
        monitoringThread.start();
    }

    /**
     * Consulta o protocolo de comunicação utilizado entre o ELM327 e a ECU.
     */
    private void readProtocol() {
        obdProtocol = executeElm327Command(Elm327Commands.GET_PROTOCOL);
    }

    /**
     * Realiza a leitura da rotação do motor (RPM).
     *
     * @return Valor da rotação do motor em RPM ou zero caso não seja
     * possível obter uma resposta válida.
     */
    private int readEngineRpm() {
        String response = executeElm327Command(ObdCommands.ENGINE_RPM);
        return ObdParser.parseEngineRpm(response);
    }

    /**
     * Realiza a leitura da velocidade atual do veículo.
     *
     * @return Velocidade do veículo em km/h ou {@code 0} caso não seja
     * possível obter uma resposta válida.
     */
    private int readVehicleSpeed() {
        String response = executeElm327Command(ObdCommands.VEHICLE_SPEED);
        return ObdParser.parseVehicleSpeed(response);
    }

    /**
     * Realiza a leitura da temperatura do líquido de arrefecimento do motor.
     *
     * @return Temperatura do líquido de arrefecimento em °C ou
     * {@code 0} caso não seja possível obter uma resposta válida.
     */
    private int readEngineCoolantTemperature() {
        String response = executeElm327Command(ObdCommands.ENGINE_COOLANT_TEMPERATURE);
        return ObdParser.parseEngineCoolantTemperature(response);
    }

    /**
     * Realiza a leitura da temperatura do ar admitido pelo motor.
     *
     * @return Temperatura do ar admitido em °C ou {@code 0}
     * caso não seja possível obter uma resposta válida.
     */
    private int readIntakeAirTemperature() {
        String response = executeElm327Command(ObdCommands.INTAKE_AIR_TEMPERATURE);
        return ObdParser.parseIntakeAirTemperature(response);
    }

    /**
     * Realiza a leitura da pressão absoluta do coletor de admissão (MAP).
     *
     * @return Pressão absoluta do coletor de admissão em kPa ou
     * {@code 0} caso não seja possível obter uma resposta válida.
     */
    private int readIntakeManifoldPressure() {
        String response = executeElm327Command(ObdCommands.INTAKE_MANIFOLD_PRESSURE);
        return ObdParser.parseIntakeManifoldPressure(response);
    }

    /**
     * Realiza a leitura da posição absoluta da borboleta de aceleração.
     *
     * @return Posição da borboleta em porcentagem (%) ou
     * {@code 0} caso não seja possível obter uma resposta válida.
     */
    private int readThrottlePosition() {
        String response = executeElm327Command(ObdCommands.THROTTLE_POSITION);
        return ObdParser.parseThrottlePosition(response);
    }

    /**
     * Realiza a leitura da tensão de alimentação do módulo de controle (ECU).
     *
     * @return Tensão de alimentação da ECU em volts (V) ou
     * {@code 0} caso não seja possível obter uma resposta válida.
     */
    private double readControlModuleVoltage() {
        String response = executeElm327Command(ObdCommands.CONTROL_MODULE_VOLTAGE);
        return ObdParser.parseControlModuleVoltage(response);
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
        if (bluetoothManager == null)
            return null;

        String response = bluetoothManager.sendCommand(command + "\r");

        if (response == null)
            return null;

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
     * Reinicia os estados da conexão OBD-II.
     */
    private void resetConnectionState() {
        monitoring = false;
        bluetoothConnected = false;
        ecuConnected = false;
        obdProtocol = null;
        notifyConnectionState();
    }

    /**
     * Notifica o listener da conexão com o estado atual da conexão OBD-II.
     *
     * <p>São notificadas as informações de conexão Bluetooth,
     * comunicação com a ECU e protocolo utilizado.</p>
     */
    private void notifyConnectionState() {
        if (connectionListener != null) {
            connectionListener.onObdBluetoothStatusChanged(bluetoothConnected);
            connectionListener.onObdEcuStatusChanged(ecuConnected);
            connectionListener.onObdProtocolChanged(obdProtocol);
        }
    }
}