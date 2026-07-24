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
    private static final long RESPONSE_TIMEOUT = 5000;

    // Gerenciador da conexão Bluetooth
    private final BluetoothManager bluetoothManager;

    // Socket da conexão Bluetooth
    private BluetoothSocket bluetoothSocket;

    // Fluxo de entrada da conexão
    private InputStream inputStream;

    // Fluxo de saída da conexão
    private OutputStream outputStream;

    // Dispositivo atualmente conectado
    private Device connectedDevice;

    // Estado da conexão
    private boolean connected;

    /**
     * Cria uma conexão Bluetooth.
     *
     * @param bluetoothManager Gerenciador Bluetooth.
     */
    protected BluetoothConnection(BluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
        closeResources();
    }

    /**
     * Conecta o dispositivo Bluetooth.
     *
     * @param bluetoothDevice Dispositivo Bluetooth remoto.
     * @param device          Dispositivo utilizado pela aplicação.
     */
    protected void connect(BluetoothDevice bluetoothDevice, Device device) {
        if (connected)
            return;

        new Thread(() -> {
            try {
                bluetoothManager.cancelDiscovery();
                openResources(bluetoothDevice, device);
                bluetoothManager.notifyConnected(device);
            } catch (IOException e) {
                closeResources();
                bluetoothManager.notifyError("Falha ao conectar: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Desconecta o dispositivo Bluetooth.
     */
    protected void disconnect() {
        if (!connected)
            return;

        Device device = connectedDevice;
        closeResources();
        bluetoothManager.notifyDisconnected(device);
    }

    /**
     * Retorna se existe conexão ativa.
     *
     * @return {@code true} se conectado, {@code false} caso contrário.
     */
    protected boolean isConnected() {
        return connected
                && bluetoothSocket != null
                && inputStream != null
                && outputStream != null;
    }

    /**
     * Envia um comando ao dispositivo Bluetooth e aguarda a resposta.
     *
     * @param command Comando a ser enviado.
     * @return Resposta do dispositivo ou {@code null} caso ocorra erro.
     */
    protected String sendCommand(String command) {
        if (command == null || command.isEmpty())
            return null;

        if (!isConnected())
            return null;

        try {
            writeCommand(command);
            return readResponse();
        } catch (IOException e) {
            bluetoothManager.notifyError("Erro na comunicação Bluetooth!");
            disconnect();
            return null;
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
        long timeout = System.currentTimeMillis() + RESPONSE_TIMEOUT;

        // Lê os dados recebidos até encontrar o caractere de término ('>') ou até que o tempo
        // limite seja atingido
        while (System.currentTimeMillis() < timeout) {
            // Lê um byte do fluxo de entrada
            int data = inputStream.read();

            // Encerra a leitura caso o fluxo tenha sido fechado
            if (data == -1)
                break;

            char receivedChar = (char) data;

            // O ELM327 utiliza o caractere '>' para indicar que finalizou o processamento do
            // comando
            if (receivedChar == '>')
                break;

            response.append(receivedChar);
        }

        return response.toString().trim();
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
     * Fecha todos os recursos da conexão.
     */
    private void closeResources() {
        connectedDevice = null;
        connected = false;

        close(inputStream, "InputStream");
        inputStream = null;

        close(outputStream, "OutputStream");
        outputStream = null;

        close(bluetoothSocket, "BluetoothSocket");
        bluetoothSocket = null;
    }

    /**
     * Fecha o recurso da conexão.
     *
     * @param resource Recurso a ser fechado.
     * @param name     Nome do recurso para exibição.
     */
    private void close(Closeable resource, String name) {
        if (resource == null)
            return;

        try {
            resource.close();
        } catch (IOException e) {
            bluetoothManager.notifyError("Erro ao fechar o " + name + ": " + e.getMessage());
        }
    }
}