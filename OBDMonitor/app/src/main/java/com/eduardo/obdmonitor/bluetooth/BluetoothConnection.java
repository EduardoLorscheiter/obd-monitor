/*
 **=================================================================================================
 **  @file    BluetoothConnection.java
 **  @author  Eduardo Lorscheiter
 **  @class   BluetoothConnection
 **  @date    21/07/2026
 **  @version 1.0
 **  @brief   Gerencia a conexão Bluetooth com um dispositivo remoto, realizando o estabelecimento e
 **           encerramento da conexão, além do envio de comandos e gerenciamento do socket e dos
 **           fluxos de comunicação.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothConnection {
    /**
     * UUID padrão do perfil Serial Port Profile (SPP).
     */
    private static final UUID SERIAL_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * Nome da thread de conexão.
     */
    private static final String THREAD_CONNECTION = "Bluetooth-Connection";

    /**
     * Tempo máximo de espera pela resposta do dispositivo, em milissegundos.
     */
    private static final long RESPONSE_TIMEOUT_MS = 5000;

    /**
     * Valor inicial do identificador de tentativa, antes da primeira conexão.
     */
    private static final long INITIAL_CONNECTION_ID = 0L;

    /**
     * Número máximo de tentativas para estabelecer a conexão Bluetooth.
     */
    private static final int MAX_CONNECTION_ATTEMPTS = 2;

    /**
     * Espera após o cancelamento de uma tentativa, para liberar os recursos RFCOMM.
     */
    private static final long CANCELLATION_RECOVERY_DELAY_MS = 750;

    /**
     * Espera entre tentativas de conexão RFCOMM.
     */
    private static final long CONNECTION_RETRY_DELAY_MS = 1000;

    /**
     * Valor retornado por {@link InputStream#read()} quando o fluxo chega ao fim.
     */
    private static final int END_OF_STREAM = -1;

    /**
     * Contexto da aplicação utilizado para acessar recursos.
     */
    private final Context context;

    /**
     * Listener utilizado para reportar eventos da conexão.
     */
    private final BluetoothConnectionListener listener;

    /**
     * Executa conexões em série para não sobrepor sockets RFCOMM durante uma troca rápida.
     */
    private final ExecutorService connectionExecutor = Executors.newSingleThreadExecutor(
            runnable -> new Thread(runnable, THREAD_CONNECTION)
    );

    /**
     * Socket utilizado para estabelecer a comunicação Bluetooth.
     */
    private BluetoothSocket socket;

    /**
     * Socket pertencente à tentativa que continua estabelecendo a conexão.
     */
    private BluetoothSocket pendingSocket;

    /**
     * Fluxo de entrada utilizado para receber dados do dispositivo.
     */
    private InputStream input;

    /**
     * Fluxo de saída utilizado para enviar dados ao dispositivo.
     */
    private OutputStream output;

    /**
     * Indica se existe uma conexão Bluetooth ativa.
     *
     * <p>O modificador 'volatile' garante a visibilidade da variável entre threads.</p>
     */
    private volatile boolean connected;

    /**
     * Indica se existe uma tentativa de conexão Bluetooth em andamento.
     *
     * <p>O modificador 'volatile' garante a visibilidade da variável entre threads.</p>
     */
    private volatile boolean connecting;

    /**
     * Identificador da tentativa de conexão atual.
     *
     * <p>O modificador 'volatile' garante a visibilidade da variável entre threads.</p>
     * <p>Cada nova tentativa recebe um identificador diferente, permitindo invalidar tentativas
     * anteriores que ainda estejam sendo executadas em segundo plano.</p>
     */
    private volatile long connectionId;

    /**
     * Indica que a próxima conexão substitui uma tentativa cancelada e pode exigir recuperação.
     */
    private boolean recoverNextConnectionAfterCancellation;

    /**
     * Cria uma conexão Bluetooth.
     *
     * @param context  Contexto da aplicação utilizado para acessar recursos.
     * @param listener Listener utilizado para reportar eventos da conexão.
     */
    protected BluetoothConnection(Context context, BluetoothConnectionListener listener) {
        this.context = context;
        this.listener = listener;
        this.connectionId = INITIAL_CONNECTION_ID;
    }

    /**
     * Inicia uma tentativa assíncrona de conexão RFCOMM com um dispositivo Bluetooth.
     *
     * <p>O estado da tentativa é atualizado sob o bloqueio deste objeto, mas a abertura do socket
     * é executada no executor de conexão para não bloquear a thread chamadora. Retorna antes de a
     * conexão terminar; o resultado é informado por
     * {@link BluetoothConnectionListener#onBluetoothConnectionConnected(long)} ou
     * {@link BluetoothConnectionListener#onBluetoothConnectionError(long, String)}.</p>
     *
     * <p>Uma nova solicitação cancela e invalida uma tentativa ainda em andamento. Se já houver
     * uma conexão ativa, seus fluxos e socket são fechados antes de iniciar a substituição. Cada
     * solicitação recebe um identificador próprio, usado para descartar resultados atrasados de
     * tentativas canceladas ou substituídas.</p>
     *
     * <p>Após uma falha de conexão, é feita uma segunda tentativa após uma pausa curta. Uma
     * tentativa que substitui outra cancelada também aguarda a liberação dos recursos RFCOMM antes
     * de começar.</p>
     *
     * @param bluetoothDevice Dispositivo Bluetooth remoto ao qual estabelecer a conexão.
     * @return Identificador da solicitação, disponível para correlacionar os callbacks assíncronos
     * com esta tentativa.
     */
    protected synchronized long connect(BluetoothDevice bluetoothDevice) {
        // Se houver uma tentativa de conexão está em andamento.
        // Obs.: Uma nova solicitação sempre substitui uma tentativa anterior.
        if (connecting)
            cancelConnection();

        // Registra se a nova tentativa precisa aguardar a liberação dos recursos RFCOMM devido ao
        // cancelamento de uma tentativa anterior.
        // Obs.: A indicação é consumida nesta solicitação para não ser reutilizada indevidamente.
        boolean recoverAfterCancellation = recoverNextConnectionAfterCancellation;
        recoverNextConnectionAfterCancellation = false;

        // Pode ocorrer de o socket já ter sido encerrado enquanto o callback de sucesso
        // ainda aguarda para ser processado pelo gerenciador.
        // Nesse intervalo, uma nova solicitação deve substituir os recursos anteriores,
        // em vez de manter a conexão antiga como se estivesse disponível para reutilização.
        if (connected) {
            // Fecha os fluxos e os sockets anteriores, limpando suas referências.
            // O fechamento não gera notificações de erro porque faz parte da substituição
            // intencional da conexão.
            connected = false;
            input = closeResource(input, false);
            output = closeResource(output, false);
            socket = closeResource(socket, false);
            pendingSocket = closeResource(pendingSocket, false);
        }

        // Cria um novo identificador para a solicitação.
        // Ele permite reconhecer e descartar resultados pertencentes a tentativas antigas.
        long currentConnectionId = ++connectionId;

        // Marca a conexão como em andamento antes de agendar a tarefa.
        // Isso impede que outra operação considere o gerenciador disponível para uma nova tentativa
        // enquanto esta solicitação aguarda sua execução.
        connecting = true;

        // Executa o estabelecimento da conexão em uma thread dedicada, mantendo a thread chamadora
        // livre para continuar o processamento da aplicação.
        connectionExecutor.execute(() -> {
            // Antes de iniciar qualquer operação de conexão, verifica se esta tarefa ainda
            // corresponde à solicitação atual.
            // Obs.: Uma tarefa pode ter permanecido na fila enquanto outra solicitação a
            // substituía.
            synchronized (this) {
                if (currentConnectionId != connectionId || !connecting)
                    return;
            }

            // Se a tentativa anterior foi cancelada, aguarda um intervalo adicional para permitir
            // que o Android libere os recursos RFCOMM utilizados anteriormente.
            // Obs.: A espera é interrompida logicamente se esta solicitação deixar de ser válida.
            if (recoverAfterCancellation && !waitBeforeRetry(
                    currentConnectionId,
                    CANCELLATION_RECOVERY_DELAY_MS
            ))
                return;

            // Executa as tentativas de conexão.
            // Obs.: Cada tentativa utiliza recursos locais, que só serão publicados como recursos
            // ativos depois que a conexão e a obtenção dos fluxos forem concluídas.
            for (int attemptNumber = 1; attemptNumber <= MAX_CONNECTION_ATTEMPTS; attemptNumber++) {
                BluetoothSocket localSocket = null;
                InputStream localInput = null;
                OutputStream localOutput = null;

                try {
                    // Cria um socket RFCOMM associado ao serviço Serial Port Profile (SPP).
                    // Obs.: O socket ainda não está conectado neste ponto.
                    localSocket = bluetoothDevice.createRfcommSocketToServiceRecord(SERIAL_UUID);

                    // Registra o socket pendente para permitir que outra operação o feche caso a
                    // tentativa seja cancelada enquanto connect() estiver bloqueado.
                    // A validação também evita iniciar uma tentativa que já foi substituída.
                    synchronized (this) {
                        if (currentConnectionId != connectionId || !connecting) {
                            closeResource(localSocket, false);
                            return;
                        }
                        pendingSocket = localSocket;
                    }

                    // Estabelece a conexão RFCOMM.
                    // Obs.: Esta operação pode bloquear até conectar ou ocorrer uma falha.
                    localSocket.connect();

                    // Obtém os fluxos utilizados para enviar comandos ao dispositivo e receber as
                    // respectivas respostas.
                    localInput = localSocket.getInputStream();
                    localOutput = localSocket.getOutputStream();

                    // Confirma que a tentativa ainda é válida antes de publicar os recursos.
                    // Obs.: Se outra solicitação tiver substituído esta conexão durante a operação,
                    // os recursos recém-criados serão fechados e não serão reutilizados.
                    synchronized (this) {
                        if (currentConnectionId != connectionId || !connecting) {
                            closeResource(localInput, false);
                            closeResource(localOutput, false);
                            closeResource(localSocket, false);
                            return;
                        }

                        // Transfere os recursos locais para os atributos da conexão.
                        // A partir deste ponto, eles passam a representar a sessão ativa.
                        socket = localSocket;
                        pendingSocket = null;
                        input = localInput;
                        output = localOutput;

                        // Atualiza o estado interno após a conexão e a obtenção dos fluxos terem
                        // sido concluídas com sucesso.
                        connected = true;
                        connecting = false;
                    }

                    // Informa ao listener que a conexão foi estabelecida.
                    // Obs.: O identificador permite ao gerenciador validar se o evento ainda
                    // pertence à tentativa que ele considera atual.
                    listener.onBluetoothConnectionConnected(currentConnectionId);
                    return;
                } catch (SecurityException e) {
                    // Permissões Bluetooth podem ser revogadas enquanto a tentativa está em
                    // andamento. Isso não deve encerrar o processo nem disparar uma repetição.
                    closeResource(localInput, false);
                    closeResource(localOutput, false);
                    closeResource(localSocket, false);

                    synchronized (this) {
                        if (currentConnectionId != connectionId || !connecting)
                            return;

                        pendingSocket = null;
                        connected = false;
                        connecting = false;
                    }

                    listener.onBluetoothConnectionError(
                            currentConnectionId,
                            BluetoothConnectionError.CONNECTION_FAILED.getMessage(context)
                    );
                    return;
                } catch (IOException e) {
                    // Libera os recursos criados pela tentativa que falhou.
                    // Obs.: Os recursos locais podem não ter sido transferidos para os atributos
                    // da conexão, portanto devem ser tratados independentemente.
                    closeResource(localInput, false);
                    closeResource(localOutput, false);
                    closeResource(localSocket, false);

                    // Verifica se a tentativa continua válida antes de atualizar o estado ou
                    // decidir se deve executar uma nova tentativa.
                    synchronized (this) {
                        if (currentConnectionId != connectionId || !connecting)
                            return;

                        // A tentativa atual não possui mais um socket pendente válido.
                        pendingSocket = null;

                        // Somente a falha da última tentativa encerra o estado de conexão
                        // em andamento. Antes disso, ainda poderá ocorrer uma nova tentativa.
                        if (attemptNumber == MAX_CONNECTION_ATTEMPTS) {
                            connected = false;
                            connecting = false;
                        }
                    }

                    // Se ainda houver uma tentativa disponível, aguarda antes de recriar o socket.
                    // Isso dá ao sistema tempo para liberar os recursos RFCOMM da operação
                    // anterior.
                    if (attemptNumber < MAX_CONNECTION_ATTEMPTS) {
                        // Reconstrói o socket após uma pausa curta; o Android pode ainda
                        // estar liberando recursos RFCOMM da tentativa anterior.
                        if (!waitBeforeRetry(currentConnectionId, CONNECTION_RETRY_DELAY_MS))
                            return;
                        continue;
                    }

                    // Todas as tentativas falharam.
                    // Informa o erro ao listener somente se esta ainda for a tentativa válida.
                    listener.onBluetoothConnectionError(
                            currentConnectionId,
                            BluetoothConnectionError.CONNECTION_FAILED.getMessage(context)
                    );
                }
            }
        });

        // Retorna imediatamente o identificador da solicitação.
        // O resultado efetivo será comunicado posteriormente pelos callbacks do listener.
        return currentConnectionId;
    }

    /**
     * Aguarda a liberação do RFCOMM e abandona a espera se outra seleção substituir a tentativa.
     *
     * <p>Se a thread for interrompida, restaura seu estado de interrupção e encerra a espera.</p>
     *
     * @param attemptId Identificador da tentativa que deve continuar válida.
     * @param delayMs   Tempo de espera antes de verificar novamente a tentativa.
     * @return {@code true} se a tentativa ainda for atual e estiver em andamento, {@code false}
     * caso contrário.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean waitBeforeRetry(long attemptId, long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        synchronized (this) {
            return attemptId == connectionId && connecting;
        }
    }

    /**
     * Encerra a conexão Bluetooth ativa e libera todos os recursos utilizados.
     *
     * <p>Após o encerramento dos recursos, o listener é notificado sobre a desconexão.</p>
     *
     * <p>Caso não exista uma conexão ativa, nenhuma ação é realizada.</p>
     */
    protected synchronized void disconnect() {
        if (connecting) {
            cancelConnection();
            return;
        }

        if (!connected)
            return;

        connected = false;

        input = closeResource(input, true);
        output = closeResource(output, true);
        socket = closeResource(socket, true);
        pendingSocket = closeResource(pendingSocket, false);

        listener.onBluetoothConnectionDisconnected(connectionId);
    }

    /**
     * Cancela uma tentativa de conexão que esteja em andamento.
     *
     * <p>A tentativa atual é invalidada imediatamente através da alteração do identificador da
     * conexão. Dessa forma, quando a thread responsável pela tentativa terminar, ela identificará
     * que já não é mais válida e encerrará seus recursos sem gerar uma notificação de erro.</p>
     *
     * <p>Caso não exista uma tentativa de conexão em andamento, nenhuma ação é realizada.</p>
     */
    private synchronized void cancelConnection() {
        if (!connecting)
            return;

        // Indica que não existe mais uma tentativa de conexão ativa.
        connecting = false;

        // Invalida a tentativa atual. Quando a thread de conexão verificar connectionId,
        // reconhecerá que não deve mais concluir essa tentativa.
        connectionId++;
        recoverNextConnectionAfterCancellation = true;

        // O cancelamento é intencional, portanto falhas ao fechar esses recursos não devem ser
        // reportadas como erros de comunicação ao usuário.
        input = closeResource(input, false);
        output = closeResource(output, false);
        socket = closeResource(socket, false);
        pendingSocket = closeResource(pendingSocket, false);
    }

    /**
     * Fecha um recurso utilizado pela conexão Bluetooth.
     *
     * <p>Aceita qualquer recurso que implemente {@link Closeable}, permitindo o fechamento do
     * socket e dos fluxos de entrada e saída por meio de uma única implementação.</p>
     *
     * <p>Fecha um recurso e retorna null para permitir limpar a referência armazenada pelo chamador
     * em uma única atribuição.</p>
     *
     * <p>Exemplo: input = closeResource(input, true).</p>
     *
     * @param <T>         Tipo do recurso que implementa {@link Closeable}.
     * @param resource    Recurso a ser fechado.
     * @param notifyError {@code true} para notificar o listener caso ocorra erro durante o
     *                    fechamento do recurso, ou {@code false} para ignorar esse erro.
     * @return {@code null}, permitindo limpar a referência ao recurso após o fechamento.
     */
    private <T extends Closeable> T closeResource(T resource, boolean notifyError) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException e) {
                if (notifyError)
                    notifyError(BluetoothConnectionError.RESOURCE_CLOSE_FAILED);
            }
        }

        return null;
    }

    /**
     * Envia um comando ao dispositivo Bluetooth conectado e aguarda sua resposta.
     *
     * <p>Caso a comunicação falhe, o erro é notificado ao listener e a conexão é encerrada.</p>
     *
     * @param command Comando a ser enviado.
     * @return Resposta recebida do dispositivo ou {@code null} caso ocorra erro ou o comando seja
     * inválido.
     */
    protected String sendCommand(String command) {
        if (!connected || command == null || command.trim().isEmpty())
            return null;

        try {
            writeCommand(command);
            String response = readResponse();
            if (response.isEmpty()) {
                notifyError(BluetoothConnectionError.INVALID_RESPONSE);
                return null;
            }
            return response;
        } catch (SecurityException | IOException e) {
            notifyError(BluetoothConnectionError.COMMUNICATION_ERROR);
            disconnect();
            return null;
        }
    }

    /**
     * Envia um comando ao dispositivo Bluetooth.
     *
     * @param command Comando a ser enviado.
     * @throws IOException Caso ocorra falha durante o envio dos dados.
     */
    private void writeCommand(String command) throws IOException {
        output.write(command.getBytes());
        output.flush();
    }

    /**
     * Lê a resposta enviada pelo dispositivo Bluetooth.
     *
     * <p>A leitura é encerrada quando o adaptador ELM327 envia o caractere {@code '>'}, indicando
     * que terminou o processamento do comando, ou quando o tempo limite de espera é atingido.</p>
     *
     * @return Resposta recebida do dispositivo.
     * @throws IOException Caso ocorra falha durante a leitura dos dados.
     */
    private String readResponse() throws IOException {
        // Acumula os caracteres recebidos até o ELM327 indicar o fim da resposta com '>' ou até
        // ocorrer o encerramento da leitura.
        StringBuilder response = new StringBuilder();

        // Define o instante máximo para aguardar a resposta.
        long timeout = System.currentTimeMillis() + RESPONSE_TIMEOUT_MS;

        // Lê os dados recebidos até encontrar o caractere de término ('>') ou até que o tempo
        // limite seja atingido.
        while (System.currentTimeMillis() < timeout) {
            int data = input.read();

            // Encerra a leitura caso o fluxo tenha sido fechado.
            if (data == END_OF_STREAM)
                break;

            char character = (char) data;

            // O ELM327 utiliza o caractere '>' para indicar que finalizou o comando.
            if (character == '>')
                break;

            response.append(character);
        }

        return response.toString().trim();
    }

    /**
     * Notifica o listener sobre um erro ocorrido durante a conexão Bluetooth.
     *
     * @param error Tipo do erro ocorrido.
     */
    private void notifyError(BluetoothConnectionError error) {
        if (listener != null)
            listener.onBluetoothConnectionError(connectionId, error.getMessage(context));
    }
}
