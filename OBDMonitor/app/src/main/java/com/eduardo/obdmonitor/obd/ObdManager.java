/*
 **=================================================================================================
 **  @file    ObdManager.java
 **  @author  Eduardo Lorscheiter
 **  @class   ObdManager
 **  @date    21/07/2026
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
import com.eduardo.obdmonitor.obd.commands.CoolantCommand;
import com.eduardo.obdmonitor.obd.commands.IntakeCommand;
import com.eduardo.obdmonitor.obd.commands.MapCommand;
import com.eduardo.obdmonitor.obd.commands.RpmCommand;
import com.eduardo.obdmonitor.obd.commands.SpeedCommand;
import com.eduardo.obdmonitor.obd.commands.SupportedPidsCommand;
import com.eduardo.obdmonitor.obd.commands.ThrottleCommand;
import com.eduardo.obdmonitor.obd.commands.VoltageCommand;
import com.eduardo.obdmonitor.obd.scheduler.ObdScheduler;
import com.eduardo.obdmonitor.obd.scheduler.PidPriority;
import com.eduardo.obdmonitor.obd.scheduler.PidTask;

import java.util.List;

public class ObdManager {
    // Tempo de espera após o envio do comando de reset ao ELM327, em milissegundos
    private static final int ELM327_RESET_DELAY_MS = 1000;

    // Tempo de espera para identificação automática do protocolo OBD-II, em milissegundos
    private static final int ELM327_PROTOCOL_DELAY_MS = 5000;

    // Intervalos de tempo entre leituras da ECU durante o monitoramento, em milissegundos
    private static final int HIGH_INTERVAL_MS = 100;
    private static final int MEDIUM_INTERVAL_MS = 300;
    private static final int LOW_INTERVAL_MS = 1000;
    private static final int VERY_LOW_INTERVAL_MS = 2000;

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

    // Thread responsável pela execução contínua do ciclo de monitoramento
    private Thread monitoringThread;

    // Scheduler responsável por controlar a periodicidade e a prioridade das leituras dos PIDs
    private final ObdScheduler obdScheduler = new ObdScheduler();

    // Comando utilizado durante a inicialização para validar a comunicação com a ECU
    private final SupportedPidsCommand supportedPidsCommand = new SupportedPidsCommand();

    // Tarefas responsáveis pelo agendamento individual da leitura dos PIDs OBD-II
    private final PidTask rpmTask =
            new PidTask(new RpmCommand(), HIGH_INTERVAL_MS, PidPriority.HIGH);
    private final PidTask speedTask =
            new PidTask(new SpeedCommand(), HIGH_INTERVAL_MS, PidPriority.HIGH);
    private final PidTask throttleTask =
            new PidTask(new ThrottleCommand(), MEDIUM_INTERVAL_MS, PidPriority.MEDIUM);
    private final PidTask mapTask =
            new PidTask(new MapCommand(), MEDIUM_INTERVAL_MS, PidPriority.MEDIUM);
    private final PidTask coolantTask =
            new PidTask(new CoolantCommand(), LOW_INTERVAL_MS, PidPriority.LOW);
    private final PidTask intakeAirTask =
            new PidTask(new IntakeCommand(), LOW_INTERVAL_MS, PidPriority.LOW);
    private final PidTask voltageTask =
            new PidTask(new VoltageCommand(), VERY_LOW_INTERVAL_MS, PidPriority.LOW);

    /**
     * Retorna a instância única do gerenciador OBD-II.
     *
     * <p>O contexto recebido é convertido internamente para o contexto da aplicação, evitando
     * a retenção de referências de componentes com ciclo de vida curto, como Activities.</p>
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
     * mantenha referências a componentes com ciclo de vida curto, como Activities.</p>
     *
     * <p>Durante a inicialização, as tarefas responsáveis pela leitura dos PIDs são cadastradas
     * no {@link ObdScheduler}, que posteriormente controla sua execução de acordo com os
     * respectivos intervalos e prioridades.</p>
     *
     * @param context Contexto utilizado para obter o contexto da aplicação.
     */
    private ObdManager(Context context) {
        this.applicationContext = context.getApplicationContext();
        bluetoothManager = BluetoothManager.getInstance();
        obdScheduler.addTask(rpmTask);
        obdScheduler.addTask(speedTask);
        obdScheduler.addTask(throttleTask);
        obdScheduler.addTask(mapTask);
        obdScheduler.addTask(coolantTask);
        obdScheduler.addTask(intakeAirTask);
        obdScheduler.addTask(voltageTask);
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

        String response = executeElm327Command(supportedPidsCommand.getRequest());
        return supportedPidsCommand.isValid(response);
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
        long currentTime = System.currentTimeMillis();

        List<PidTask> readyTasks = obdScheduler.getReadyTasks(currentTime);

        for (PidTask task : readyTasks) {
            if (task == rpmTask) {
                int engineRpm = readEngineRpm();
                if (dataListener != null)
                    dataListener.onObdEngineRpmChanged(engineRpm);
            } else if (task == speedTask) {
                int vehicleSpeed = readVehicleSpeed();
                if (dataListener != null)
                    dataListener.onObdVehicleSpeedChanged(vehicleSpeed);
            } else if (task == throttleTask) {
                int throttlePosition = readThrottlePosition();
                if (dataListener != null)
                    dataListener.onObdThrottlePositionChanged(throttlePosition);
            } else if (task == mapTask) {
                int intakeManifoldPressure = readIntakeManifoldPressure();
                if (dataListener != null)
                    dataListener.onObdIntakeManifoldPressureChanged(intakeManifoldPressure);
            } else if (task == coolantTask) {
                int engineCoolantTemperature = readEngineCoolantTemperature();
                if (dataListener != null)
                    dataListener.onObdEngineCoolantTemperatureChanged(engineCoolantTemperature);
            } else if (task == intakeAirTask) {
                int intakeAirTemperature = readIntakeAirTemperature();
                if (dataListener != null)
                    dataListener.onObdIntakeAirTemperatureChanged(intakeAirTemperature);
            } else if (task == voltageTask) {
                double controlModuleVoltage = readControlModuleVoltage();
                if (dataListener != null)
                    dataListener.onObdControlModuleVoltageChanged(controlModuleVoltage);
            }

            obdScheduler.updateTaskExecution(task, currentTime);
        }
    }

    /**
     * Aguarda o intervalo configurado entre duas leituras consecutivas da ECU.
     *
     * @throws InterruptedException Caso a thread de monitoramento seja interrompida durante a
     *                              espera.
     */
    private void sleepMonitoring() throws InterruptedException {
        Thread.sleep(10);
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
        String response = executeElm327Command(rpmTask.getCommand().getRequest());
        return (Integer) rpmTask.getCommand().parse(response);
    }

    /**
     * Realiza a leitura da velocidade atual do veículo.
     *
     * @return Velocidade do veículo em km/h ou {@code 0} caso não seja possível obter uma resposta
     * válida.
     */
    private int readVehicleSpeed() {
        String response = executeElm327Command(speedTask.getCommand().getRequest());
        return (Integer) speedTask.getCommand().parse(response);
    }

    /**
     * Realiza a leitura da posição absoluta da borboleta de aceleração.
     *
     * @return Posição da borboleta em porcentagem (%) ou {@code 0} caso não seja possível obter uma
     * resposta válida.
     */
    private int readThrottlePosition() {
        String response = executeElm327Command(throttleTask.getCommand().getRequest());
        return (Integer) throttleTask.getCommand().parse(response);
    }

    /**
     * Realiza a leitura da pressão absoluta do coletor de admissão (MAP).
     *
     * @return Pressão absoluta do coletor de admissão em kPa ou {@code 0} caso não seja possível
     * obter uma resposta válida.
     */
    private int readIntakeManifoldPressure() {
        String response = executeElm327Command(mapTask.getCommand().getRequest());
        return (Integer) mapTask.getCommand().parse(response);
    }

    /**
     * Realiza a leitura da temperatura do líquido de arrefecimento do motor.
     *
     * @return Temperatura do líquido de arrefecimento em °C ou {@code 0} caso não seja possível
     * obter uma resposta válida.
     */
    private int readEngineCoolantTemperature() {
        String response = executeElm327Command(coolantTask.getCommand().getRequest());
        return (Integer) coolantTask.getCommand().parse(response);
    }

    /**
     * Realiza a leitura da temperatura do ar admitido pelo motor.
     *
     * @return Temperatura do ar admitido em °C ou {@code 0} caso não seja possível obter uma
     * resposta válida.
     */
    private int readIntakeAirTemperature() {
        String response = executeElm327Command(intakeAirTask.getCommand().getRequest());
        return (Integer) intakeAirTask.getCommand().parse(response);
    }

    /**
     * Realiza a leitura da tensão de alimentação do módulo de controle (ECU).
     *
     * @return Tensão de alimentação da ECU em volts (V) ou {@code 0} caso não seja possível obter
     * uma resposta válida.
     */
    private double readControlModuleVoltage() {
        String response = executeElm327Command(voltageTask.getCommand().getRequest());
        return (Double) voltageTask.getCommand().parse(response);
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