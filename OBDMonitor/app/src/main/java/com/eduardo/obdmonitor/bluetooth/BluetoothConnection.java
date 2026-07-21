/*
 **===========================================================================
 **  @file    BluetoothConnection.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    Julho/2026
 **  @version 1.0
 **  @brief   Gerencia a conexão Bluetooth com um dispositivo remoto,
 **           realizando o estabelecimento e encerramento da conexão,
 **           além do envio de comandos e gerenciamento dos recursos de
 **           comunicação.
 **===========================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.eduardo.obdmonitor.model.Device;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothConnection {
    // UUID padrão do perfil Serial Port Profile (SPP)
    private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Gerenciador Bluetooth
    private final BluetoothManager bluetoothManager;

    // Socket Bluetooth
    private BluetoothSocket bluetoothSocket;

    // Fluxo de entrada
    private InputStream inputStream;

    // Fluxo de saída
    private OutputStream outputStream;

    // Dispositivo Bluetooth conectado
    private Device connectedDevice;

    // Indica se existe conexão ativa
    private boolean connected;

    /**
     * Cria uma nova conexão Bluetooth.
     *
     * @param bluetoothManager gerenciador Bluetooth.
     */
    public BluetoothConnection(BluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
    }

    /**
     * Conecta ao dispositivo Bluetooth.
     *
     * @param bluetoothDevice dispositivo Android.
     * @param device          dispositivo utilizado pela aplicação.
     */
    public void connect(BluetoothDevice bluetoothDevice, Device device) {
        if (connected) {
            return;
        }

        new Thread(() -> {
            try {
                bluetoothManager.cancelDiscovery();
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(SERIAL_UUID);
                bluetoothSocket.connect();
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                connected = true;
                connectedDevice = device;
                bluetoothManager.notifyConnected(device);
            } catch (IOException e) {
                closeResources();
                bluetoothManager.notifyError("Falha ao conectar: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Desconecta do dispositivo Bluetooth.
     */
    public void disconnect() {
        closeResources();
        bluetoothManager.notifyDisconnected(connectedDevice);
        connectedDevice = null;
    }

    /**
     * Retorna se existe conexão ativa.
     *
     * @return true se conectado, false caso contrário.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Envia uma mensagem ao dispositivo Bluetooth.
     *
     * @param command comando a ser enviado.
     * @return resposta ao comando enviado.
     */
    public String sendCommand(String command) {
        if (!connected || outputStream == null) {
            return null;
        }

        try {
            outputStream.write(command.getBytes());
            outputStream.flush();
            StringBuilder response = new StringBuilder();
            int data;
            while ((data = inputStream.read()) != -1) {
                char c = (char) data;
                if (c == '>') {
                    break;
                }
                response.append(c);
            }
            return response.toString().trim();
        } catch (IOException e) {
            disconnect();
            bluetoothManager.notifyError("Erro na comunicação Bluetooth!");
            return null;
        }
    }

    /**
     * Fecha todos os recursos da conexão.
     */
    private void closeResources() {
        connected = false;

        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (IOException ignored) {
        }

        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
        } catch (IOException ignored) {
        }

        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
        } catch (IOException ignored) {
        }
    }
}