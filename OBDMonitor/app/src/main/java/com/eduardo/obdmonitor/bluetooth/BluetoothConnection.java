/*
 **=================================================================================================
 **  @file    BluetoothConnection.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
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

import com.eduardo.obdmonitor.model.Device;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothConnection {
    // UUID padrão do perfil Serial Port Profile (SPP)
    private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Tempo máximo de espera pela resposta do dispositivo (ms)
    private static final long RESPONSE_TIMEOUT_MS = 5000;

    // Contexto global da aplicação utilizado para acessar serviços Android
    private final Context applicationContext;

    // Listener utilizado para reportar eventos da conexão Bluetooth
    private final BluetoothConnectionListener bluetoothConnectionListener;

    // Socket da conexão Bluetooth
    private BluetoothSocket bluetoothSocket;

    // Fluxo de entrada da conexão
    private InputStream inputStream;

    // Fluxo de saída da conexão
    private OutputStream outputStream;

    // Dispositivo atualmente conectado
    private Device connectedDevice;

    // Indica se existe uma conexão Bluetooth ativa
    // Obs.: O modificador 'volatile' garante a visibilidade da variável entre threads
    private volatile boolean connected;

    // Indica se está ocorrendo uma tentativa de conexão Bluetooth
    // Obs.: O modificador 'volatile' garante a visibilidade da variável entre threads
    private volatile boolean connecting;

    /**
     * Cria uma conexão Bluetooth.
     *
     * @param applicationContext          Contexto global da aplicação utilizado para acessar
     *                                    serviços Android.
     * @param bluetoothConnectionListener Listener utilizado para reportar eventos da conexão
     *                                    Bluetooth.
     */
    protected BluetoothConnection(Context applicationContext,
                                  BluetoothConnectionListener bluetoothConnectionListener) {
        this.applicationContext = applicationContext;
        this.bluetoothConnectionListener = bluetoothConnectionListener;
    }

    /**
     * Conecta o dispositivo Bluetooth.
     *
     * <p>A conexão é realizada em uma thread separada e o resultado é informado através do
     * {@link BluetoothConnectionListener}.</p>
     *
     * @param bluetoothDevice Dispositivo Bluetooth remoto.
     * @param device          Dispositivo utilizado pela aplicação.
     */
    protected void connect(BluetoothDevice bluetoothDevice, Device device) {
        if (isConnected() || connecting)
            return;

        connecting = true;

        new Thread(() -> {
            try {
                openResources(bluetoothDevice, device);
                bluetoothConnectionListener.onBluetoothConnectionConnected(device);
            } catch (IOException e) {
                closeResources();
                bluetoothConnectionListener.onBluetoothConnectionError(
                        device,
                        BluetoothConnectionError.CONNECTION_FAILED.getMessage(applicationContext));
            } finally {
                connecting = false;
            }
        }).start();
    }

    /**
     * Encerra a conexão Bluetooth ativa e libera todos os recursos utilizados.
     *
     * <p>Após o encerramento, o listener é notificado sobre a desconexão.</p>
     */
    protected void disconnect() {
        if (!isConnected())
            return;

        Device device = connectedDevice;
        closeResources();
        bluetoothConnectionListener.onBluetoothConnectionDisconnected(device);
    }

    /**
     * Retorna se existe conexão ativa.
     *
     * @return {@code true} se conectado, {@code false} caso contrário.
     */
    protected boolean isConnected() {
        return connected;
    }

    /**
     * Envia um comando ao dispositivo Bluetooth e aguarda a resposta.
     *
     * @param command Comando a ser enviado.
     * @return Resposta do dispositivo ou {@code null} caso ocorra erro.
     */
    protected String sendCommand(String command) {
        if (!isConnected() || command == null || command.trim().isEmpty())
            return null;

        try {
            writeCommand(command);
            String response = readResponse();
            if (response.isEmpty()) {
                bluetoothConnectionListener.onBluetoothConnectionError(
                        connectedDevice,
                        BluetoothConnectionError.INVALID_RESPONSE.getMessage(applicationContext));
                return null;
            }
            return response;
        } catch (IOException e) {
            bluetoothConnectionListener.onBluetoothConnectionError(
                    connectedDevice,
                    BluetoothConnectionError.COMMUNICATION_ERROR.getMessage(applicationContext));
            disconnect();
            return null;
        }
    }

    /**
     * Estabelece a conexão Bluetooth e abre os fluxos de comunicação.
     *
     * @param bluetoothDevice Dispositivo Bluetooth remoto.
     * @param device          Dispositivo utilizado pela aplicação.
     * @throws IOException Caso ocorra falha na conexão.
     */
    private void openResources(BluetoothDevice bluetoothDevice, Device device) throws IOException {
        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(SERIAL_UUID);
        bluetoothSocket.connect();

        inputStream = bluetoothSocket.getInputStream();
        outputStream = bluetoothSocket.getOutputStream();

        connectedDevice = device;
        connected = true;
    }

    /**
     * Fecha todos os recursos associados à conexão Bluetooth e redefine o estado interno da
     * conexão.
     */
    private void closeResources() {
        close(inputStream);
        inputStream = null;

        close(outputStream);
        outputStream = null;

        close(bluetoothSocket);
        bluetoothSocket = null;

        connectedDevice = null;
        connected = false;
    }

    /**
     * Fecha o recurso da conexão.
     *
     * @param resource Recurso a ser fechado.
     */
    private void close(Closeable resource) {
        if (resource == null)
            return;

        try {
            resource.close();
        } catch (IOException e) {
            bluetoothConnectionListener.onBluetoothConnectionError(
                    connectedDevice,
                    BluetoothConnectionError.RESOURCE_CLOSE_FAILED.getMessage(applicationContext));
        }
    }

    /**
     * Envia um comando ao dispositivo Bluetooth.
     *
     * @param command Comando a ser enviado.
     * @throws IOException Caso ocorra erro durante o envio.
     */
    private void writeCommand(String command) throws IOException {
        outputStream.write(command.getBytes());
        outputStream.flush();
    }

    /**
     * Lê a resposta enviada pelo dispositivo Bluetooth.
     *
     * <p>A leitura é encerrada quando o adaptador ELM327 envia o caractere {@code '>'}, indicando
     * que terminou o processamento do comando, ou quando o tempo limite de espera é atingido.</p>
     *
     * @return Resposta recebida do dispositivo.
     * @throws IOException Caso ocorra erro durante a leitura.
     */
    private String readResponse() throws IOException {
        StringBuilder response = new StringBuilder();

        // Define o instante máximo para aguardar a resposta
        long timeout = System.currentTimeMillis() + RESPONSE_TIMEOUT_MS;

        // Lê os dados recebidos até encontrar o caractere de término ('>') ou até que o tempo
        // limite seja atingido
        while (System.currentTimeMillis() < timeout) {
            // Lê um byte do fluxo de entrada
            int data = inputStream.read();

            // Encerra a leitura caso o fluxo tenha sido fechado
            if (data == -1)
                break;

            char character = (char) data;

            // O ELM327 utiliza o caractere '>' para indicar que finalizou o processamento do
            // comando
            if (character == '>')
                break;

            response.append(character);
        }

        return response.toString().trim();
    }
}