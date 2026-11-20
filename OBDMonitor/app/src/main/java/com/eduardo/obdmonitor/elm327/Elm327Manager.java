/*
 **=================================================================================================
 **  @file    Elm327Manager.java
 **  @author  Eduardo Lorscheiter
 **  @class   Elm327Manager
 **  @date    21/07/2026
 **  @version 1.0
 **  @brief   Gerencia a comunicação entre a aplicação, o adaptador ELM327 e a ECU do veículo,
 **           realizando a inicialização, monitoramento e leitura dos dados do OBD-II.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.elm327;

import android.content.Context;

import com.eduardo.obdmonitor.R;
import com.eduardo.obdmonitor.bluetooth.BluetoothManager;
import com.eduardo.obdmonitor.model.Device;
import com.eduardo.obdmonitor.obd.commands.CoolantCommand;
import com.eduardo.obdmonitor.obd.commands.FuelCommand;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Elm327Manager {
    /**
     * Nome da thread de monitoramento dos dados da ECU.
     */
    private static final String THREAD_OBD_MONITOR = "OBD-Monitor";

    /**
     * Tempo de espera após o envio do comando de reset ao ELM327, em milissegundos.
     */
    private static final int ELM327_RESET_DELAY_MS = 1000;

    /**
     * Tempo de espera para identificação automática do protocolo OBD-II, em milissegundos.
     */
    private static final int ELM327_PROTOCOL_DELAY_MS = 5000;

    /**
     * Intervalos de tempo entre leituras da ECU durante o monitoramento, em milissegundos.
     */
    private static final int FAST_READ_INTERVAL_MS = 100;
    private static final int NORMAL_READ_INTERVAL_MS = 300;
    private static final int SLOW_READ_INTERVAL_MS = 1000;
    private static final int VERY_SLOW_READ_INTERVAL_MS = 2000;

    /**
     * Instância única do adaptador ELM327 (Singleton).
     */
    private static Elm327Manager instance;

    /**
     * Contexto global da aplicação utilizado para acessar serviços Android.
     */
    private final Context applicationContext;

    /**
     * Gerenciador responsável pela comunicação Bluetooth.
     */
    private final BluetoothManager bluetoothManager;

    /**
     * Lista de listeners utilizados para reportar eventos de conexão às interfaces.
     */
    private final List<Elm327ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();

    /**
     * Lista de listeners utilizados para reportar eventos de dados às interfaces.
     */
    private final List<Elm327DataListener> dataListeners = new CopyOnWriteArrayList<>();

    /**
     * Indica se o monitoramento contínuo da ECU está ativo.
     *
     * <p>O modificador 'volatile' garante a visibilidade da variável entre threads.</p>
     */
    private volatile boolean monitoring;

    /**
     * Indica que a inicialização da comunicação está em andamento.
     */
    private boolean initializing;

    /**
     * Mensagem da última etapa do handshake que falhou.
     */
    private volatile String initializationFailureMessage;

    /**
     * Evita agendar inicializações duplicadas quando Activities retomam uma conexão existente.
     */
    private boolean connectionInitializationScheduled;

    /**
     * Executor único para manter a inicialização do ELM327 fora da thread principal.
     */
    private final ExecutorService connectionExecutor = Executors.newSingleThreadExecutor();

    /**
     * Indica se o Bluetooth está conectado.
     *
     * <p>O modificador 'volatile' garante a visibilidade da variável entre threads.</p>
     */
    private volatile boolean bluetoothConnected;

    /**
     * Indica se a ECU está conectada.
     *
     * <p>O modificador 'volatile' garante a visibilidade da variável entre threads.</p>
     */
    private volatile boolean ecuConnected;

    /**
     * Protocolo OBD-II identificado durante a conexão.
     *
     * <p>O modificador 'volatile' garante a visibilidade da variável entre threads.</p>
     */
    private volatile String obdProtocol;

    /**
     * Sessão Bluetooth para a qual as máscaras de PIDs foram consultadas.
     *
     * <p>O modificador 'volatile' garante a visibilidade da variável entre threads.</p>
     */
    private volatile long initializedConnectionGeneration = -1;

    /**
     * Thread responsável pela execução contínua do ciclo de monitoramento.
     *
     * <p>O modificador 'volatile' garante a visibilidade da variável entre threads.</p>
     */
    private volatile Thread monitoringThread;

    /**
     * Scheduler responsável por controlar a periodicidade e a prioridade das leituras dos PIDs.
     */
    private final ObdScheduler obdScheduler = new ObdScheduler();

    /**
     * Comando responsável pela consulta dos PIDs suportados no intervalo de {@code 0x01} a
     * {@code 0x20}.
     */
    private final SupportedPidsCommand basePidsCommand =
            new SupportedPidsCommand(SupportedPidsCommand.SUPPORTED_PIDS_00_PID);

    /**
     * Comando responsável pela consulta dos PIDs suportados no intervalo de {@code 0x21} a
     * {@code 0x40}.
     */
    private final SupportedPidsCommand extendedPidsCommand =
            new SupportedPidsCommand(SupportedPidsCommand.SUPPORTED_PIDS_20_PID);

    /**
     * Suporte ao PID 42, distinto da habilitação da tarefa que também pode consultar ATRV.
     */
    private volatile boolean voltagePidSupported;

    /**
     * Tarefas responsáveis pelo agendamento individual da leitura dos PIDs OBD-II.
     */
    private final PidTask rpmTask =
            new PidTask(new RpmCommand(), FAST_READ_INTERVAL_MS, PidPriority.HIGH);
    private final PidTask speedTask =
            new PidTask(new SpeedCommand(), FAST_READ_INTERVAL_MS, PidPriority.HIGH);
    private final PidTask throttleTask =
            new PidTask(new ThrottleCommand(), NORMAL_READ_INTERVAL_MS, PidPriority.MEDIUM);
    private final PidTask mapTask =
            new PidTask(new MapCommand(), NORMAL_READ_INTERVAL_MS, PidPriority.MEDIUM);
    private final PidTask coolantTask =
            new PidTask(new CoolantCommand(), SLOW_READ_INTERVAL_MS, PidPriority.LOW);
    private final PidTask intakeTask =
            new PidTask(new IntakeCommand(), SLOW_READ_INTERVAL_MS, PidPriority.LOW);
    private final PidTask voltageTask =
            new PidTask(new VoltageCommand(), VERY_SLOW_READ_INTERVAL_MS, PidPriority.LOW);
    private final PidTask fuelTask =
            new PidTask(new FuelCommand(), VERY_SLOW_READ_INTERVAL_MS, PidPriority.LOW);

    /**
     * Retorna a instância única do adaptador ELM327.
     *
     * <p>O contexto recebido é convertido internamente para o contexto da aplicação, evitando
     * a retenção de referências de componentes com ciclo de vida curto, como Activities.</p>
     *
     * @param context Contexto utilizado para obter o contexto da aplicação.
     * @return Instância única de {@code Elm327Manager}.
     */
    public static synchronized Elm327Manager getInstance(Context context) {
        if (instance == null)
            instance = new Elm327Manager(context);

        return instance;
    }

    /**
     * Cria uma nova instância do adaptador ELM327.
     *
     * <p>O contexto é armazenado como contexto da aplicação para garantir que o Singleton não
     * mantenha referências a componentes com ciclo de vida curto, como Activities.</p>
     *
     * <p>Durante a inicialização, as tarefas responsáveis pela leitura dos PIDs são cadastradas
     * no {@link ObdScheduler}, que posteriormente controla sua execução conforme os respectivos
     * intervalos e prioridades.</p>
     *
     * @param context Contexto utilizado para obter o contexto da aplicação.
     */
    private Elm327Manager(Context context) {
        applicationContext = context.getApplicationContext();
        bluetoothManager = BluetoothManager.getInstance(applicationContext);
        obdScheduler.addTask(rpmTask);
        obdScheduler.addTask(speedTask);
        obdScheduler.addTask(throttleTask);
        obdScheduler.addTask(mapTask);
        obdScheduler.addTask(coolantTask);
        obdScheduler.addTask(intakeTask);
        obdScheduler.addTask(voltageTask);
        obdScheduler.addTask(fuelTask);
        resetConnectionState();
    }

    /**
     * Adiciona um listener para receber eventos relacionados à conexão com o adaptador ELM327.
     *
     * @param listener Listener que receberá os eventos de conexão com o adaptador ELM327.
     */
    public void addConnectionListener(Elm327ConnectionListener listener) {
        if (listener == null)
            return;

        if (!connectionListeners.contains(listener)) {
            connectionListeners.add(listener);
            notifyConnectionState(listener);
        }
    }

    /**
     * Remove um listener de conexão com o adaptador ELM327 previamente registrado.
     *
     * @param listener Listener que deixará de receber os eventos de conexão com o adaptador ELM327.
     */
    public void removeConnectionListener(Elm327ConnectionListener listener) {
        if (listener == null)
            return;

        connectionListeners.remove(listener);
    }

    /**
     * Adiciona um listener para receber eventos de dados da ECU.
     *
     * @param listener Listener que receberá os eventos de dados da ECU.
     */
    public void addDataListener(Elm327DataListener listener) {
        if (listener == null)
            return;

        if (!dataListeners.contains(listener))
            dataListeners.add(listener);

        if (ecuConnected)
            startMonitoring();
    }

    /**
     * Remove um listener de dados da ECU previamente registrado.
     *
     * @param listener Listener que deixará de receber os eventos de dados da ECU.
     */
    public void removeDataListener(Elm327DataListener listener) {
        if (listener == null)
            return;

        dataListeners.remove(listener);
    }

    /**
     * Inicializa a comunicação com o adaptador ELM327 e estabelece a comunicação com a ECU do
     * veículo.
     *
     * <p>Coordena o processo de conexão, delegando a preparação do estado, a inicialização do
     * ELM327 e a finalização da conexão para métodos específicos.</p>
     *
     * <p>Caso o ELM327 já esteja conectado à sessão Bluetooth atual, apenas o monitoramento dos
     * dados da ECU é retomado.</p>
     *
     * @param device Dispositivo OBD-II conectado.
     * @throws InterruptedException Caso a thread seja interrompida durante a inicialização.
     */
    public void connect(Device device) throws InterruptedException {
        if (!prepareConnection())
            return;

        initializationFailureMessage = null;

        try {
            if (initializeElm327())
                completeConnection(device);
            else
                handleInitializationError();
        } finally {
            finishInitialization();
        }
    }

    /**
     * Prepara o estado interno para uma nova conexão com o adaptador ELM327.
     *
     * <p>Verifica se já existe uma inicialização em andamento ou se a ECU já está conectada à
     * sessão Bluetooth atual. Quando uma sessão anterior continua ativa, seu estado é encerrado
     * antes de iniciar uma nova inicialização.</p>
     *
     * @return {@code true} caso uma nova inicialização deva ser realizada, {@code false} caso a
     * conexão já esteja sendo inicializada ou a sessão atual possa ser reutilizada.
     */
    private synchronized boolean prepareConnection() {
        if (initializing)
            return false;

        if (isCurrentConnectionActive()) {
            startMonitoring();
            return false;
        }

        if (ecuConnected)
            resetPreviousSession();

        initializing = true;
        bluetoothConnected = true;

        return true;
    }

    /**
     * Encerra o estado da sessão anterior da ECU antes de iniciar uma nova inicialização.
     *
     * <p>O monitoramento atual é interrompido, os dados referentes à conexão anterior são removidos
     * e o suporte aos PIDs é redefinido.</p>
     */
    private void resetPreviousSession() {
        monitoring = false;

        if (monitoringThread != null)
            monitoringThread.interrupt();

        ecuConnected = false;
        obdProtocol = null;

        clearTaskSupport();

        notifyConnectionState();
    }

    /**
     * Inicializa o adaptador ELM327.
     *
     * <p>Realiza a configuração inicial do adaptador e identifica os PIDs suportados pela ECU do
     * veículo.</p>
     *
     * @return {@code true} caso a inicialização seja concluída com sucesso, {@code false} caso
     * contrário.
     * @throws InterruptedException Caso a thread seja interrompida durante o tempo de espera entre
     *                              os comandos de inicialização.
     */
    private boolean initializeElm327() throws InterruptedException {
        if (!configureElm327())
            return false;

        return discoverSupportedPids();
    }

    /**
     * Configura o adaptador ELM327 para comunicação com a ECU.
     *
     * <p>Envia os comandos AT necessários para preparar o adaptador antes da comunicação com o
     * veículo.</p>
     *
     * @return {@code true} caso todos os comandos sejam executados com sucesso, {@code false}
     * caso algum comando falhe.
     * @throws InterruptedException Caso a thread seja interrompida durante o tempo de espera
     *                              entre os comandos.
     */
    private boolean configureElm327() throws InterruptedException {
        // Reinicia o adaptador ELM327
        if (!sendCommandOk(Elm327Commands.RESET))
            return false;

        Thread.sleep(ELM327_RESET_DELAY_MS);

        // Configura o formato das respostas do adaptador
        if (!sendCommandOk(Elm327Commands.ECHO_OFF))
            return false;

        if (!sendCommandOk(Elm327Commands.LINE_FEEDS_OFF))
            return false;

        if (!sendCommandOk(Elm327Commands.SPACES_OFF))
            return false;

        if (!sendCommandOk(Elm327Commands.HEADERS_OFF))
            return false;

        // Configura a seleção automática do protocolo OBD-II
        if (!sendCommandOk(Elm327Commands.AUTO_PROTOCOL))
            return false;

        Thread.sleep(ELM327_PROTOCOL_DELAY_MS);

        return true;
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
        if (response == null) {
            initializationFailureMessage = applicationContext.getString(
                    R.string.elm327_at_command_failed,
                    command
            );
            return false;
        }

        boolean commandAccepted =
                response.contains(applicationContext.getString(R.string.elm327)) ||
                        response.contains(applicationContext.getString(R.string.ok));

        if (!commandAccepted)
            initializationFailureMessage = applicationContext.getString(
                    R.string.elm327_at_command_failed,
                    command
            );

        return commandAccepted;
    }

    /**
     * Identifica os PIDs suportados pela ECU do veículo.
     *
     * <p>Primeiro consulta os PIDs básicos. Caso a ECU informe suporte ao PID {@code 0x20}, também
     * são consultados os PIDs estendidos.</p>
     *
     * <p>Ao final, as tarefas de leitura são configuradas conforme os PIDs identificados.</p>
     *
     * @return {@code true} caso a consulta dos PIDs básicos seja concluída com sucesso,
     * {@code false} caso contrário.
     */
    private boolean discoverSupportedPids() {
        // Consulta os PIDs básicos suportados pela ECU
        String response = executeElm327Command(basePidsCommand.getRequest());
        Long basePids = basePidsCommand.parse(response);

        if (basePids == null) {
            initializationFailureMessage = applicationContext.getString(
                    R.string.elm327_ecu_pids_unavailable
            );
            return false;
        }

        // Consulta os PIDs estendidos, caso a ECU informe suporte ao PID 0x20
        Long extendedPids = null;

        if (basePidsCommand.isPidSupported(basePids, extendedPidsCommand.getPid())) {
            extendedPids = extendedPidsCommand.parse(
                    executeElm327Command(extendedPidsCommand.getRequest())
            );
        }

        // Atualiza o suporte das tarefas conforme os PIDs identificados
        SupportedPidsCommand voltagePidsCommand =
                new SupportedPidsCommand(SupportedPidsCommand.SUPPORTED_PIDS_40_PID);

        Long voltagePids = null;
        if (extendedPidsCommand.isPidSupported(extendedPids, voltagePidsCommand.getPid()))
            voltagePids = voltagePidsCommand.parse(
                    executeElm327Command(voltagePidsCommand.getRequest()));

        voltagePidSupported = voltagePidsCommand.isPidSupported(
                voltagePids, voltageTask.getCommand().getPid());

        configurePidTasks(basePids, extendedPids);

        return true;
    }

    /**
     * Atualiza o suporte das tarefas de leitura conforme os PIDs suportados pela ECU.
     *
     * @param basePids     Máscara de PIDs de {@code 0x01} a {@code 0x20} suportados pela ECU.
     * @param extendedPids Máscara de PIDs de {@code 0x21} a {@code 0x40} suportados pela ECU,
     *                     podendo ser {@code null} caso o intervalo não seja suportado.
     */
    private void configurePidTasks(Long basePids, Long extendedPids) {
        rpmTask.updateSupport(basePidsCommand, basePids);
        speedTask.updateSupport(basePidsCommand, basePids);
        throttleTask.updateSupport(basePidsCommand, basePids);
        mapTask.updateSupport(basePidsCommand, basePids);
        coolantTask.updateSupport(basePidsCommand, basePids);
        intakeTask.updateSupport(basePidsCommand, basePids);
        // ATRV mantém a tarefa elegível mesmo quando a ECU não oferece o PID 42.
        voltageTask.setSupported(true);
        fuelTask.updateSupport(extendedPidsCommand, extendedPids);
    }

    /**
     * Completa a conexão após a inicialização do ELM327 ser concluída com sucesso.
     *
     * <p>O estado da ECU é atualizado, o protocolo OBD-II é identificado, a geração da conexão
     * Bluetooth é registrada e os listeners são notificados. Ao final, o monitoramento contínuo
     * dos dados da ECU é iniciado.</p>
     *
     * @param device Dispositivo OBD-II conectado.
     */
    private void completeConnection(Device device) {
        String protocol = readProtocol();

        synchronized (this) {
            if (bluetoothManager.getConnectedDevice() == null)
                return;

            ecuConnected = true;
            obdProtocol = protocol;
            initializedConnectionGeneration = bluetoothManager.getConnectionGeneration();

            notifyConnectionState();
            notifyElm327Connected(device);

            startMonitoring();
        }
    }

    /**
     * Notifica os listeners sobre a conexão bem-sucedida com o adaptador ELM327.
     *
     * @param device Dispositivo OBD-II conectado.
     */
    private void notifyElm327Connected(Device device) {
        for (Elm327ConnectionListener listener : connectionListeners)
            listener.onElm327Connected(device);
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

        for (Elm327ConnectionListener listener : connectionListeners)
            listener.onElm327Error(
                    initializationFailureMessage != null
                            ? initializationFailureMessage
                            : Elm327ManagerError.INVALID_ELM327_DEVICE.getMessage(
                            applicationContext
                    )
            );
    }

    /**
     * Finaliza o estado de inicialização da conexão.
     *
     * <p>Garante que novas tentativas de conexão possam ser realizadas após o término da
     * inicialização, independentemente de seu resultado.</p>
     */
    private synchronized void finishInitialization() {
        initializing = false;
    }

    /**
     * Inicializa o ELM327 em segundo plano ou retoma o monitoramento se a ECU já estiver conectada.
     *
     * @param device Dispositivo Bluetooth conectado ao adaptador ELM327.
     */
    public void connectAsync(Device device) {
        if (device == null)
            return;

        synchronized (this) {
            if (isCurrentConnectionActive()) {
                startMonitoring();
                return;
            }

            if (initializing || connectionInitializationScheduled)
                return;

            connectionInitializationScheduled = true;
        }

        connectionExecutor.execute(() -> {
            try {
                connect(device);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (SecurityException e) {
                // A permissão pode ser revogada entre a validação e o acesso ao Bluetooth.
                handleUnexpectedConnectionFailure(
                        applicationContext.getString(R.string.bluetooth_permission_not_granted)
                );
            } catch (RuntimeException e) {
                // Encerra a sessão sem deixar uma exceção inesperada derrubar o processo.
                handleUnexpectedConnectionFailure(
                        Elm327ManagerError.INVALID_ELM327_DEVICE.getMessage(applicationContext)
                );
            } finally {
                synchronized (Elm327Manager.this) {
                    connectionInitializationScheduled = false;
                }
            }
        });
    }

    /**
     * Limpa a sessão após uma falha inesperada durante a inicialização do adaptador.
     */
    private void handleUnexpectedConnectionFailure(String errorMessage) {
        bluetoothManager.disconnect();
        resetConnectionState();
        notifyConnectionState();

        for (Elm327ConnectionListener listener : connectionListeners)
            listener.onElm327Error(errorMessage);
    }

    /**
     * Verifica se a conexão atual com a ECU pertence à sessão Bluetooth ativa.
     *
     * @return {@code true} caso a ECU esteja conectada e a geração da conexão Bluetooth seja a
     * mesma utilizada durante sua inicialização, {@code false} caso contrário.
     */
    private boolean isCurrentConnectionActive() {
        return ecuConnected &&
                initializedConnectionGeneration == bluetoothManager.getConnectionGeneration();
    }

    /**
     * Encerra o monitoramento dos dados da ECU e finaliza a comunicação com o adaptador OBD-II.
     */
    public void disconnect() {
        bluetoothManager.disconnect();
        handleBluetoothDisconnection();
    }

    /**
     * Inicia o monitoramento contínuo dos dados da ECU.
     *
     * <p>Enquanto o monitoramento estiver ativo, uma thread realiza leituras periódicas da ECU e
     * notifica o listener responsável pelos dados monitorados.</p>
     *
     * <p>O monitoramento é executado em uma thread dedicada.</p>
     */
    private synchronized void startMonitoring() {
        if (!ecuConnected || monitoring)
            return;

        monitoring = true;

        monitoringThread = new Thread(() -> {
            try {
                while (monitoring && monitoringThread == Thread.currentThread()
                        && !Thread.currentThread().isInterrupted()) {
                    readVehicleData();
                    sleepMonitoring();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (monitoring) {
                    for (Elm327ConnectionListener listener : connectionListeners)
                        listener.onElm327Error(
                                Elm327ManagerError.MONITORING_INTERRUPTED.getMessage(applicationContext)
                        );
                }
            } finally {
                synchronized (Elm327Manager.this) {
                    if (monitoringThread == Thread.currentThread())
                        monitoringThread = null;
                }
            }
        }, THREAD_OBD_MONITOR);
        monitoringThread.start();
    }

    /**
     * Realiza a leitura dos principais parâmetros da ECU e notifica o listener responsável pelos
     * dados monitorados.
     *
     * <p>Os valores são obtidos por meio dos respectivos PIDs OBD-II e enviados ao
     * {@link Elm327DataListener}, quando definido.</p>
     */
    private void readVehicleData() {
        long currentTime = System.currentTimeMillis();

        List<PidTask> readyTasks = obdScheduler.getReadyTasks(currentTime);

        for (PidTask task : readyTasks) {
            if (task == rpmTask) {
                int value = readEngineRpm();
                for (Elm327DataListener listener : dataListeners)
                    listener.onObdEngineRpmChanged(value);
            } else if (task == speedTask) {
                int value = readVehicleSpeed();
                for (Elm327DataListener listener : dataListeners)
                    listener.onObdVehicleSpeedChanged(value);
            } else if (task == throttleTask) {
                int value = readThrottlePosition();
                for (Elm327DataListener listener : dataListeners)
                    listener.onObdThrottlePositionChanged(value);
            } else if (task == mapTask) {
                int value = readIntakeManifoldPressure();
                for (Elm327DataListener listener : dataListeners)
                    listener.onObdIntakeManifoldPressureChanged(value);
            } else if (task == coolantTask) {
                int value = readEngineCoolantTemperature();
                for (Elm327DataListener listener : dataListeners)
                    listener.onObdEngineCoolantTemperatureChanged(value);
            } else if (task == intakeTask) {
                int value = readIntakeTemperature();
                for (Elm327DataListener listener : dataListeners)
                    listener.onObdIntakeAirTemperatureChanged(value);
            } else if (task == voltageTask) {
                double value = readControlModuleVoltage();
                for (Elm327DataListener listener : dataListeners)
                    listener.onObdControlModuleVoltageChanged(value);
            } else if (task == fuelTask) {
                int value = readFuelLevel();
                for (Elm327DataListener listener : dataListeners)
                    listener.onObdFuelLevelChanged(value);
            }

            obdScheduler.updateTaskExecution(task, currentTime);
        }
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
     * @return Valor da rotação do motor em RPM.
     */
    private int readEngineRpm() {
        String response = executeElm327Command(rpmTask.getCommand().getRequest());
        return (Integer) rpmTask.getCommand().parse(response);
    }

    /**
     * Realiza a leitura da velocidade atual do veículo.
     *
     * @return Velocidade do veículo em km/h.
     */
    private int readVehicleSpeed() {
        String response = executeElm327Command(speedTask.getCommand().getRequest());
        return (Integer) speedTask.getCommand().parse(response);
    }

    /**
     * Realiza a leitura da posição absoluta da borboleta de aceleração.
     *
     * @return Posição da borboleta em porcentagem (%).
     */
    private int readThrottlePosition() {
        String response = executeElm327Command(throttleTask.getCommand().getRequest());
        return (Integer) throttleTask.getCommand().parse(response);
    }

    /**
     * Realiza a leitura da pressão absoluta do coletor de admissão (MAP).
     *
     * @return Pressão absoluta do coletor de admissão em kPa.
     */
    private int readIntakeManifoldPressure() {
        String response = executeElm327Command(mapTask.getCommand().getRequest());
        return (Integer) mapTask.getCommand().parse(response);
    }

    /**
     * Realiza a leitura da temperatura do líquido de arrefecimento do motor.
     *
     * @return Temperatura do líquido de arrefecimento em °C.
     */
    private int readEngineCoolantTemperature() {
        String response = executeElm327Command(coolantTask.getCommand().getRequest());
        return (Integer) coolantTask.getCommand().parse(response);
    }

    /**
     * Realiza a leitura da temperatura do ar admitido pelo motor.
     *
     * @return Temperatura do ar admitido em °C.
     */
    private int readIntakeTemperature() {
        String response = executeElm327Command(intakeTask.getCommand().getRequest());
        return (Integer) intakeTask.getCommand().parse(response);
    }

    /**
     * Realiza a leitura da tensão de alimentação do módulo de controle (ECU).
     *
     * @return Tensão de alimentação da ECU em volts (V).
     */
    private double readControlModuleVoltage() {
        // Primeira tentativa: se a tarefa suporta a leitura pelo PID OBD-II
        if (voltagePidSupported) {
            String response = executeElm327Command(voltageTask.getCommand().getRequest());

            double voltage = (Double) voltageTask.getCommand().parse(response);
            if (Double.isFinite(voltage))
                return voltage;
        }

        // Segunda tentativa: caso não tenha suporte na tarefa, faz a leitura direta pelo ELM327
        return readElm327Voltage();
    }

    /**
     * Realiza a leitura da tensão de alimentação fornecida diretamente pelo ELM327.
     *
     * <p>O comando {@code AT RV} é enviado ao adaptador ELM327, que retorna o valor da tensão em
     * volts. A resposta é convertida para {@code double} após a remoção da unidade de medida.</p>
     *
     * @return Tensão de alimentação do ELM327 em volts (V) ou {@link Double#NaN} caso não seja
     * possível obter ou converter uma resposta válida.
     */
    private double readElm327Voltage() {
        String response = executeElm327Command(Elm327Commands.READ_VOLTAGE);
        return Elm327Commands.parseVoltage(response);
    }

    /**
     * Realiza a leitura do nível de combustível.
     *
     * @return Nível de combustível em porcentagem (%).
     */
    private int readFuelLevel() {
        String response = executeElm327Command(fuelTask.getCommand().getRequest());
        return (Integer) fuelTask.getCommand().parse(response);
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
        if (response == null) {
            handleBluetoothDisconnection();
            return null;
        }

        return normalizeResponse(response);
    }

    /**
     * Encerra o estado OBD após a perda do transporte e notifica as telas.
     *
     * <p>Não solicita outra desconexão Bluetooth. Eventos atrasados são ignorados se já houver um
     * dispositivo conectado, e notificações repetidas não reiniciam a limpeza.</p>
     */
    public synchronized void handleBluetoothDisconnection() {
        if (bluetoothManager.getConnectedDevice() != null)
            return;

        if (!monitoring && !bluetoothConnected && !ecuConnected && obdProtocol == null)
            return;

        monitoring = false;

        Thread thread = monitoringThread;
        if (thread != null)
            thread.interrupt();

        resetConnectionState();
        notifyConnectionState();
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
     * Aguarda brevemente antes de iniciar o próximo ciclo de monitoramento.
     *
     * <p>Os intervalos individuais de leitura dos PIDs são controlados pelo {@link ObdScheduler}.
     * Este intervalo apenas evita que a thread de monitoramento execute ciclos continuamente sem
     * uma pequena pausa.</p>
     *
     * @throws InterruptedException Caso a thread de monitoramento seja interrompida durante a
     *                              espera.
     */
    private void sleepMonitoring() throws InterruptedException {
        Thread.sleep(10);
    }

    /**
     * Reinicia os estados da conexão OBD-II.
     */
    private void resetConnectionState() {
        monitoring = false;
        bluetoothConnected = false;
        ecuConnected = false;
        obdProtocol = null;
        initializedConnectionGeneration = -1;
        clearTaskSupport();
    }

    /**
     * Reinicia o suporte das tarefas de leitura dos PIDs OBD-II.
     */
    private void clearTaskSupport() {
        voltagePidSupported = false;
        rpmTask.setSupported(false);
        speedTask.setSupported(false);
        throttleTask.setSupported(false);
        mapTask.setSupported(false);
        coolantTask.setSupported(false);
        intakeTask.setSupported(false);
        voltageTask.setSupported(false);
        fuelTask.setSupported(false);
    }

    /**
     * Notifica os listeners da conexão com o estado atual da conexão OBD-II.
     *
     * <p>São notificadas as informações de conexão Bluetooth, comunicação com a ECU e protocolo
     * utilizado.</p>
     */
    private void notifyConnectionState() {
        for (Elm327ConnectionListener listener : connectionListeners) {
            notifyConnectionState(listener);
        }
    }

    /**
     * Notifica o listener da conexão com o estado atual da conexão OBD-II.
     *
     * @param listener Listener que receberá os eventos de conexão com o adaptador ELM327.
     */
    private void notifyConnectionState(Elm327ConnectionListener listener) {
        listener.onElm327BluetoothStatusChanged(bluetoothConnected);
        listener.onElm327EcuStatusChanged(ecuConnected);
        listener.onElm327ProtocolChanged(obdProtocol);
    }
}
