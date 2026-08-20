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
    // UUID padrão do perfil Serial Port Profile (SPP)
    private static final UUID SERIAL_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Tempo máximo de espera pela resposta do dispositivo, em milissegundos
    private static final long RESPONSE_TIMEOUT_MS = 5000;

    // Contexto da aplicação utilizado para acessar recursos
    private final Context context;

    // Listener utilizado para reportar eventos da conexão
    private final BluetoothConnectionListener listener;

    // Socket utilizado para estabelecer a comunicação Bluetooth
    private BluetoothSocket socket;

    // Fluxo de entrada utilizado para receber dados do dispositivo
    private InputStream input;

    // Fluxo de saída utilizado para enviar dados ao dispositivo
    private OutputStream output;

    // Indica se existe uma conexão Bluetooth ativa
    // Obs.: O modificador 'volatile' garante a visibilidade da variável entre threads
    private volatile boolean connected;

    // Indica se existe uma tentativa de conexão Bluetooth em andamento
    // Obs.: O modificador 'volatile' garante a visibilidade da variável entre threads
    private volatile boolean connecting;

    // Identificador da tentativa de conexão atual
    // Obs.1: O modificador 'volatile' garante a visibilidade da variável entre threads
    // Obs.2: Cada nova tentativa recebe um identificador diferente, permitindo invalidar tentativas
    // anteriores que ainda estejam sendo executadas em segundo plano
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
    }

    /**
     * Inicia uma tentativa de conexão com o dispositivo Bluetooth.
     *
     * <p>A conexão é realizada em uma thread separada para evitar o bloqueio da thread principal.
     * Cada tentativa recebe um identificador exclusivo, permitindo que uma tentativa anterior seja
     * invalidada caso uma nova conexão seja solicitada.</p>
     *
     * <p>Caso já exista uma conexão estabelecida ou uma tentativa de conexão em andamento, nenhuma
     * nova tentativa será iniciada.</p>
     *
     * @param bluetoothDevice Dispositivo Bluetooth remoto (físico).
     */
    protected synchronized void connect(BluetoothDevice bluetoothDevice) {
        if (connected || connecting)
            return;

        // Gera um novo identificador para a tentativa atual
        long currentConnectionId = ++connectionId;

        // Indica que uma tentativa de conexão está em andamento
        connecting = true;

        new Thread(() -> {
            // Recursos locais utilizados exclusivamente por esta tentativa de conexão
            BluetoothSocket localSocket = null;
            InputStream localInput = null;
            OutputStream localOutput = null;

            try {
                // Cria e estabelece o socket Bluetooth
                localSocket = bluetoothDevice.createRfcommSocketToServiceRecord(SERIAL_UUID);
                localSocket.connect();

                // Obtém os fluxos de comunicação do socket
                localInput = localSocket.getInputStream();
                localOutput = localSocket.getOutputStream();

                synchronized (this) {
                    // Verifica se a tentativa ainda é válida antes de assumir os recursos
                    // Obs.: Caso tenha sido cancelada ou substituída por outra tentativa, os
                    // recursos recém-criados são fechados sem gerar notificações de erro
                    if (currentConnectionId != connectionId || !connecting) {
                        closeResource(localInput, false);
                        closeResource(localOutput, false);
                        closeResource(localSocket, false);
                        return;
                    }

                    // Transfere os recursos locais para os atributos da conexão
                    socket = localSocket;
                    input = localInput;
                    output = localOutput;

                    // A conexão foi estabelecida com sucesso
                    connected = true;
                    connecting = false;
                }

                listener.onBluetoothConnectionConnected();
            } catch (IOException e) {
                // Fecha os recursos criados por esta tentativa sem gerar erro de fechamento
                closeResource(localInput, false);
                closeResource(localOutput, false);
                closeResource(localSocket, false);

                synchronized (this) {
                    // Se a tentativa já não é mais a atual, o erro pertence a uma tentativa
                    // cancelada ou substituída e, portanto, deve ser ignorado
                    if (currentConnectionId != connectionId)
                        return;

                    // A tentativa atual foi encerrada devido a uma falha
                    connected = false;
                    connecting = false;
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
    protected void disconnect() {
        if (!connected)
            return;

        connected = false;

        input = closeResource(input, true);
        output = closeResource(output, true);
        socket = closeResource(socket, true);

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
    protected synchronized void cancelConnection() {
        if (!connecting)
            return;

        // Indica que não existe mais uma tentativa de conexão ativa
        connecting = false;

        // Invalida imediatamente a tentativa atual
        connectionId++;

        // Fecha eventuais recursos associados à tentativa cancelada
        input = closeResource(input, false);
        output = closeResource(output, false);
        socket = closeResource(socket, false);
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
     * Fecha um recurso utilizado pela conexão Bluetooth.
     *
     * <p>Aceita qualquer recurso que implemente {@link Closeable}, permitindo o fechamento do
     * socket e dos fluxos de entrada e saída através de uma única implementação.</p>
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
        StringBuilder response = new StringBuilder();

        // Define o instante máximo para aguardar a resposta
        long timeout = System.currentTimeMillis() + RESPONSE_TIMEOUT_MS;

        // Lê os dados recebidos até encontrar o caractere de término ('>') ou até que o tempo
        // limite seja atingido
        while (System.currentTimeMillis() < timeout) {
            int data = input.read();

            // Encerra a leitura caso o fluxo tenha sido fechado
            if (data == -1)
                break;

            char character = (char) data;

            // O ELM327 utiliza o caractere '>' para indicar que finalizou o comando
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