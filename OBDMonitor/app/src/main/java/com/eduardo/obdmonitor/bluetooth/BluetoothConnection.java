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

public class BluetoothConnection {
    /**
     * UUID padrão do perfil Serial Port Profile (SPP).
     */
    private static final UUID SERIAL_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * Tempo máximo de espera pela resposta do dispositivo, em milissegundos.
     */
    private static final long RESPONSE_TIMEOUT_MS = 5000;

    /**
     * Contexto da aplicação utilizado para acessar recursos.
     */
    private final Context context;

    /**
     * Listener utilizado para reportar eventos da conexão.
     */
    private final BluetoothConnectionListener listener;

    /**
     * Socket utilizado para estabelecer a comunicação Bluetooth.
     */
    private BluetoothSocket socket;

    /**
     * Socket pertencente à tentativa que ainda está estabelecendo a conexão.
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
     * Cria uma conexão Bluetooth.
     *
     * @param context  Contexto da aplicação utilizado para acessar recursos.
     * @param listener Listener utilizado para reportar eventos da conexão.
     */
    protected BluetoothConnection(Context context, BluetoothConnectionListener listener) {
        this.context = context;
        this.listener = listener;
        this.connectionId = 0;
    }

    /**
     * Inicia uma tentativa de conexão com o dispositivo Bluetooth.
     *
     * <p>A conexão é realizada em uma thread separada para evitar o bloqueio da thread principal.
     * Cada tentativa recebe um identificador exclusivo, permitindo que uma tentativa anterior seja
     * invalidada caso uma nova conexão seja solicitada.</p>
     *
     * <p>Uma tentativa anterior é cancelada antes de iniciar a nova, garantindo prioridade à
     * solicitação mais recente.</p>
     *
     * @param bluetoothDevice Dispositivo Bluetooth remoto (físico).
     */
    protected synchronized void connect(BluetoothDevice bluetoothDevice) {
        // Se houver uma tentativa de conexão está em andamento.
        // Obs.: Uma nova solicitação sempre substitui uma tentativa anterior.
        if (connecting)
            cancelConnection();

        // Se houver uma conexão estabelecida.
        // Obs.: Não substitui uma conexão já estabelecida sem que o gerenciador a encerre primeiro.
        if (connected)
            return;

        // Gera um novo identificador para a tentativa atual.
        long currentConnectionId = ++connectionId;

        // Indica que uma tentativa de conexão está em andamento.
        connecting = true;

        new Thread(() -> {
            // Recursos locais utilizados exclusivamente por esta tentativa de conexão.
            BluetoothSocket localSocket = null;
            InputStream localInput = null;
            OutputStream localOutput = null;

            try {
                // Cria um socket RFCOMM associado ao serviço SPP do dispositivo remoto.
                // Obs.: A conexão será estabelecida na chamada connect(), executada nesta thread
                // para evitar bloquear a thread principal da aplicação.
                localSocket = bluetoothDevice.createRfcommSocketToServiceRecord(SERIAL_UUID);
                synchronized (this) {
                    // Verifica se a tentativa ainda é válida antes de registrar o socket.
                    // Obs.: Caso tenha sido cancelada enquanto o socket era criado, fecha o recurso
                    // e encerra esta thread sem iniciar a conexão.
                    if (currentConnectionId != connectionId || !connecting) {
                        closeResource(localSocket, false);
                        return;
                    }
                    // Registra o socket antes de iniciar a conexão para permitir que
                    // cancelConnection() tente interromper a operação connect() em andamento.
                    pendingSocket = localSocket;
                }
                localSocket.connect();

                // Obtém os fluxos de comunicação do socket.
                localInput = localSocket.getInputStream();
                localOutput = localSocket.getOutputStream();

                synchronized (this) {
                    // Verifica se a tentativa ainda é válida antes de assumir os recursos.
                    // Obs.: Caso tenha sido cancelada ou substituída por outra tentativa, os
                    // recursos recém-criados são fechados sem gerar notificações de erro.
                    if (currentConnectionId != connectionId || !connecting) {
                        closeResource(localInput, false);
                        closeResource(localOutput, false);
                        closeResource(localSocket, false);
                        return;
                    }

                    // A tentativa ainda é válida. Transfere os recursos locais para a instância,
                    // que passa a ser responsável por utilizá-los e encerrá-los.
                    // A remoção de pendingSocket indica que o socket deixou de ser pendente.
                    socket = localSocket;
                    pendingSocket = null;
                    input = localInput;
                    output = localOutput;

                    // Atualiza o estado somente após a conexão e a obtenção dos fluxos terem sido
                    // concluídas com sucesso.
                    connected = true;
                    connecting = false;
                }

                listener.onBluetoothConnectionConnected();
            } catch (IOException e) {
                // Fecha os recursos criados por esta tentativa sem gerar erro de fechamento.
                closeResource(localInput, false);
                closeResource(localOutput, false);
                closeResource(localSocket, false);

                synchronized (this) {
                    // Uma tentativa cancelada ou substituída pode lançar IOException porque seu
                    // socket foi fechado. Nesse caso, o erro não pertence à tentativa atual e não
                    // deve ser reportado ao listener.
                    if (currentConnectionId != connectionId)
                        return;

                    // A tentativa atual falhou. Limpa seu estado antes de notificar o gerenciador
                    // sobre o erro de conexão.
                    connected = false;
                    connecting = false;
                    pendingSocket = null;
                }

                notifyError(BluetoothConnectionError.CONNECTION_FAILED);
            }
        }, "Bluetooth-Connection").start();
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

        listener.onBluetoothConnectionDisconnected();
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
     * socket e dos fluxos de entrada e saída através de uma única implementação.</p>
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
        } catch (IOException e) {
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
            if (data == -1)
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
            listener.onBluetoothConnectionError(error.getMessage(context));
    }
}