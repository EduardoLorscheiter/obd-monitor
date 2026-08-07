/*
 **=================================================================================================
 **  @file    ObdManager.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Gerencia a comunicação entre a aplicação, o adaptador ELM327 e a ECU do veículo,
 **           realizando a inicialização, monitoramento e leitura dos dados OBD-II.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd;

import android.content.Context;

import com.eduardo.obdmonitor.R;
import com.eduardo.obdmonitor.bluetooth.BluetoothManager;
import com.eduardo.obdmonitor.elm327.Elm327Commands;
import com.eduardo.obdmonitor.model.Device;

public class ObdManager {
    // Tempo de espera após o envio do comando de reset ao ELM327 (em milissegundos)
    private static final int ELM327_RESET_DELAY_MS = 1000;

    // Tempo de espera para identificação automática do protocolo OBD-II (em milissegundos)
    private static final int ELM327_PROTOCOL_DELAY_MS = 5000;

    // Intervalo entre leituras consecutivas da ECU durante o monitoramento (em milissegundos)
    private static final int MONITORING_INTERVAL_MS = 100;

    // Instância única do gerenciador OBD-II (Singleton)
    private static ObdManager instance;

    // Contexto global da aplicação utilizado para acessar serviços Android
    private final Context applicationContext;

    // Gerenciador responsável pela comunicação Bluetooth
    private final BluetoothManager bluetoothManager;

    // Listener responsável por receber eventos relacionados ao estado da conexão OBD-II
    private ObdConnectionListener connectionListener;

    // Listener responsável por receber os dados monitorados da ECU
    private ObdDataListener dataListener;

    // Indica se o monitoramento contínuo da ECU está ativo
    // Obs.: O modificador 'volatile' garante a visibilidade da variável entre threads
    private volatile boolean monitoring;

    // Indica se o Bluetooth está conectado
    private boolean bluetoothConnected;

    // Indica se a ECU está conectada
    private boolean ecuConnected;

    // Protocolo OBD-II identificado durante a conexão
    private String obdProtocol;

    // Thread responsável pelo monitoramento contínuo dos dados da ECU
    private Thread monitoringThread;

    /**
     * Retorna a instância única do gerenciador OBD-II.
     *
     * <p>O contexto recebido é convertido internamente para o contexto da aplicação, evitando
     * retenção de referências de Activity.</p>
     *
     * @param context Contexto utilizado para obter o contexto da aplicação.
     * @return Instância única de {@code ObdManager}.
     */
    public static synchronized ObdManager getInstance(Context context) {
        if (instance == null)
            instance = new ObdManager(context);

        return instance;
    }

    /**
     * Retorna a instância única do gerenciador OBD-II previamente inicializada.
     *
     * <p>Deve ser utilizado após a criação da instância através de {@link #getInstance(Context)}.
     * Caso o gerenciador ainda não tenha sido inicializado, uma {@link IllegalStateException} será
     * lançada.</p>
     *
     * @return Instância única de {@code ObdManager}.
     * @throws IllegalStateException Caso o gerenciador ainda não tenha sido inicializado.
     */
    public static synchronized ObdManager getInstance() {
        if (instance == null)
            throw new IllegalStateException("ObdManager has not been initialized!");

        return instance;
    }

    /**
     * Cria uma nova instância do gerenciador OBD-II.
     *
     * <p>O contexto é armazenado como contexto da aplicação para garantir que o Singleton não
     * mantenha referência a componentes com ciclo de vida curto, como Activities.</p>
     *
     * @param context Contexto utilizado para obter o contexto da aplicação.
     */
    private ObdManager(Context context) {
        this.applicationContext = context.getApplicationContext();
        bluetoothManager = BluetoothManager.getInstance();
        resetConnectionState();
    }

    /**
     * Define os listeners responsáveis por receber os eventos da comunicação OBD-II.
     *
     * <p>O listener de conexão recebe as alterações de estado da comunicação com o adaptador ELM327
     * e a ECU, enquanto o listener de dados recebe os valores monitorados durante a comunicação.
     * </p>
     *
     * @param connectionListener Listener que receberá os eventos de conexão OBD-II.
     * @param dataListener       Listener que receberá os dados monitorados da ECU.
     */
    public void setListeners(ObdConnectionListener connectionListener, ObdDataListener dataListener) {
        this.connectionListener = connectionListener;
        this.dataListener = dataListener;
        notifyConnectionState();
    }

    /**
     * Inicializa a comunicação com o adaptador ELM327 e estabelece a comunicação com a ECU do
     * veículo.
     *
     * <p>Caso a inicialização seja concluída com sucesso, o monitoramento contínuo dos dados da ECU
     * é iniciado automaticamente.</p>
     *
     * @param device Dispositivo OBD-II conectado.
     * @throws InterruptedException Caso a thread seja interrompida durante a inicialização.
     */
    public void connect(Device device) throws InterruptedException {
        bluetoothConnected = true;

        if (initializeElm327()) {
            finishConnection(device);
        } else {
            handleInitializationError();
        }
    }

    /**
     * Finaliza o processo de conexão com o adaptador OBD-II.
     *
     * <p>Atualiza o estado interno da conexão, identifica o protocolo de comunicação, inicia o
     * monitoramento contínuo da ECU e notifica o listener sobre o sucesso da conexão.</p>
     *
     * @param device Dispositivo OBD-II conectado.
     */
    private void finishConnection(Device device) {
        ecuConnected = true;
        obdProtocol = readProtocol();
        notifyConnectionState();

        if (connectionListener != null)
            connectionListener.onObdConnected(device);

        startMonitoring();
    }

    /**
     * Finaliza a tentativa de conexão após uma falha na inicialização do ELM327.
     *
     * <p>A conexão Bluetooth é encerrada, os estados internos são redefinidos e o listener é
     * notificado sobre o erro ocorrido.</p>
     */
    private void handleInitializationError() {
        bluetoothManager.disconnect();
        resetConnectionState();
        notifyConnectionState();

        if (connectionListener != null)
            connectionListener.onObdError(
                    ObdManagerError.INVALID_ELM327_DEVICE.getMessage(applicationContext));
    }

    /**
     * Encerra o monitoramento dos dados da ECU e finaliza a comunicação com o adaptador OBD-II.
     */
    public void disconnect() {
        monitoring = false;

        if (monitoringThread != null)
            monitoringThread.interrupt();

        bluetoothManager.disconnect();

        resetConnectionState();
        notifyConnectionState();
    }

    /**
     * Inicializa o adaptador ELM327.
     *
     * <p>Durante a inicialização são enviados os comandos AT necessários para configurar o
     * adaptador antes da comunicação com a ECU.</p>
     *
     * <p>Ao final, é realizada uma leitura dos PIDs suportados para validar a comunicação com o
     * veículo.</p>
     *
     * @return {@code true} caso a inicialização seja concluída com sucesso, {@code false} caso
     * contrário.
     * @throws InterruptedException Caso a thread seja interrompida durante o tempo de espera entre
     *                              os comandos de inicialização.
     */
    private boolean initializeElm327() throws InterruptedException {
        if (!sendCommandOk(Elm327Commands.RESET)) return false;
        Thread.sleep(ELM327_RESET_DELAY_MS);
        if (!sendCommandOk(Elm327Commands.ECHO_OFF)) return false;
        if (!sendCommandOk(Elm327Commands.LINE_FEEDS_OFF)) return false;
        if (!sendCommandOk(Elm327Commands.SPACES_OFF)) return false;
        if (!sendCommandOk(Elm327Commands.HEADERS_OFF)) return false;
        if (!sendCommandOk(Elm327Commands.AUTO_PROTOCOL)) return false;
        Thread.sleep(ELM327_PROTOCOL_DELAY_MS);

        String supportedPids = executeElm327Command(ObdCommands.SUPPORTED_PIDS_01_20);
        return supportedPids != null && supportedPids.contains("4100");
    }

    /**
     * Envia um comando ao ELM327 e verifica se a resposta indica sucesso.
     *
     * @param command Comando a ser enviado.
     * @return {@code true} caso a resposta contenha 'OK' ou a identificação do ELM327 durante o
     * processo de inicialização, {@code false} caso contrário.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean sendCommandOk(String command) {
        String response = executeElm327Command(command);

        if (response == null)
            return false;

        return response.contains(applicationContext.getString(R.string.elm327)) ||
                response.contains(applicationContext.getString(R.string.ok));
    }

    /**
     * Inicia o monitoramento contínuo dos dados da ECU.
     *
     * <p>Enquanto o monitoramento estiver ativo, uma thread realiza leituras periódicas da ECU e
     * notifica o listener responsável pelos dados monitorados.</p>
     *
     * <p>O monitoramento é executado em uma thread dedicada.</p>
     */
    private void startMonitoring() {
        if (!ecuConnected || monitoring)
            return;

        monitoring = true;

        monitoringThread = new Thread(() -> {
            try {
                while (monitoring) {
                    readVehicleData();
                    sleepMonitoring();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (monitoring && connectionListener != null)
                    connectionListener.onObdError(
                            ObdManagerError.MONITORING_INTERRUPTED.getMessage(applicationContext));
            } finally {
                monitoringThread = null;
            }
        }, "OBD-Monitor");
        monitoringThread.start();
    }

    /**
     * Realiza a leitura dos principais parâmetros da ECU e notifica o listener responsável pelos
     * dados monitorados.
     *
     * <p>Os valores são obtidos por meio dos respectivos PIDs OBD-II e enviados ao
     * {@link ObdDataListener}, quando definido.</p>
     */
    private void readVehicleData() {
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
    }

    /**
     * Aguarda o intervalo configurado entre duas leituras consecutivas da ECU.
     *
     * @throws InterruptedException Caso a thread de monitoramento seja interrompida durante a
     *                              espera.
     */
    private void sleepMonitoring() throws InterruptedException {
        Thread.sleep(MONITORING_INTERVAL_MS);
    }

    /**
     * Realiza a leitura do protocolo de comunicação utilizado entre o ELM327 e a ECU.
     *
     * @return Protocolo OBD-II identificado durante a conexão.
     */
    private String readProtocol() {
        return executeElm327Command(Elm327Commands.GET_PROTOCOL);
    }

    /**
     * Realiza a leitura da rotação do motor (RPM).
     *
     * @return Valor da rotação do motor em RPM ou zero caso não seja possível obter uma resposta
     * válida.
     */
    private int readEngineRpm() {
        String response = executeElm327Command(ObdCommands.ENGINE_RPM);
        return ObdParser.parseEngineRpm(response);
    }

    /**
     * Realiza a leitura da velocidade atual do veículo.
     *
     * @return Velocidade do veículo em km/h ou {@code 0} caso não seja possível obter uma resposta
     * válida.
     */
    private int readVehicleSpeed() {
        String response = executeElm327Command(ObdCommands.VEHICLE_SPEED);
        return ObdParser.parseVehicleSpeed(response);
    }

    /**
     * Realiza a leitura da temperatura do líquido de arrefecimento do motor.
     *
     * @return Temperatura do líquido de arrefecimento em °C ou {@code 0} caso não seja possível
     * obter uma resposta válida.
     */
    private int readEngineCoolantTemperature() {
        String response = executeElm327Command(ObdCommands.ENGINE_COOLANT_TEMPERATURE);
        return ObdParser.parseEngineCoolantTemperature(response);
    }

    /**
     * Realiza a leitura da temperatura do ar admitido pelo motor.
     *
     * @return Temperatura do ar admitido em °C ou {@code 0} caso não seja possível obter uma
     * resposta válida.
     */
    private int readIntakeAirTemperature() {
        String response = executeElm327Command(ObdCommands.INTAKE_AIR_TEMPERATURE);
        return ObdParser.parseIntakeAirTemperature(response);
    }

    /**
     * Realiza a leitura da pressão absoluta do coletor de admissão (MAP).
     *
     * @return Pressão absoluta do coletor de admissão em kPa ou {@code 0} caso não seja possível
     * obter uma resposta válida.
     */
    private int readIntakeManifoldPressure() {
        String response = executeElm327Command(ObdCommands.INTAKE_MANIFOLD_PRESSURE);
        return ObdParser.parseIntakeManifoldPressure(response);
    }

    /**
     * Realiza a leitura da posição absoluta da borboleta de aceleração.
     *
     * @return Posição da borboleta em porcentagem (%) ou {@code 0} caso não seja possível obter uma
     * resposta válida.
     */
    private int readThrottlePosition() {
        String response = executeElm327Command(ObdCommands.THROTTLE_POSITION);
        return ObdParser.parseThrottlePosition(response);
    }

    /**
     * Realiza a leitura da tensão de alimentação do módulo de controle (ECU).
     *
     * @return Tensão de alimentação da ECU em volts (V) ou {@code 0} caso não seja possível obter
     * uma resposta válida.
     */
    private double readControlModuleVoltage() {
        String response = executeElm327Command(ObdCommands.CONTROL_MODULE_VOLTAGE);
        return ObdParser.parseControlModuleVoltage(response);
    }

    /**
     * Envia um comando ao ELM327 e retorna sua resposta.
     *
     * <p>O caractere de retorno de carro ('\r') é acrescentado automaticamente ao final do comando
     * antes do envio.</p>
     *
     * <p>A resposta retornada é normalizada antes de ser entregue ao chamador.</p>
     *
     * @param command Comando a ser enviado.
     * @return Resposta retornada pelo ELM327 ou {@code null} em caso de falha.
     */
    private String executeElm327Command(String command) {
        String response = bluetoothManager.sendCommand(command + "\r");

        if (response == null)
            return null;

        return normalizeResponse(response);
    }

    /**
     * Normaliza uma resposta recebida do ELM327.
     *
     * <p>Remove o prompt de término ('>'), quebras de linha, espaços excedentes e converte todos os
     * caracteres para letras maiúsculas, facilitando o processamento da resposta.</p>
     *
     * @param response Resposta recebida do ELM327.
     * @return Resposta normalizada.
     */
    private static String normalizeResponse(String response) {
        return response
                .replace(">", "")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }

    /**
     * Reinicia os estados da conexão OBD-II.
     */
    private void resetConnectionState() {
        monitoring = false;
        bluetoothConnected = false;
        ecuConnected = false;
        obdProtocol = null;
    }

    /**
     * Notifica o listener da conexão com o estado atual da conexão OBD-II.
     *
     * <p>São notificadas as informações de conexão Bluetooth, comunicação com a ECU e protocolo
     * utilizado.</p>
     */
    private void notifyConnectionState() {
        ObdConnectionListener listener = connectionListener;

        if (listener == null)
            return;

        listener.onObdBluetoothStatusChanged(bluetoothConnected);
        listener.onObdEcuStatusChanged(ecuConnected);
        listener.onObdProtocolChanged(obdProtocol);
    }
}