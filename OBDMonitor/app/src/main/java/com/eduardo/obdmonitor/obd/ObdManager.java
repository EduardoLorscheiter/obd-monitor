/*
 **===========================================================================
 **  @file    ObdManager.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    Julho/2026
 **  @version 1.0
 **  @brief   OBDMonitor
 **===========================================================================
 */

package com.eduardo.obdmonitor.obd;

import androidx.annotation.NonNull;

import com.eduardo.obdmonitor.bluetooth.BluetoothManager;
import com.eduardo.obdmonitor.elm327.Elm327Commands;
import com.eduardo.obdmonitor.model.Device;

public class ObdManager {
    // Instância única da classe (singleton)
    private static ObdManager instance;

    // Listener utilizado para reportar eventos Bluetooth à interface
    private ObdListener listener;

    private BluetoothManager bluetoothManager;

    // Indica se o OBD-II está conectado
    private boolean connected;

    private boolean monitoring;

    public static synchronized ObdManager getInstance() {
        if (instance == null) {
            instance = new ObdManager();
        }

        return instance;
    }

    /**
     * Cria uma nova instância do gerenciador do OBD-II.
     */
    private ObdManager() {
        connected = false;
        monitoring = false;
    }

    /**
     * Define o listener dos eventos do OBD-II
     *
     * @param listener listener que receberá eventos do OBD-II
     */
    public void setListener(ObdListener listener) {
        this.listener = listener;
    }

    /**
     * Solicita conexão com um dispositivo OBD-II através de um ELM327
     *
     * <p>O BluetoothManager fará a conexão Bluetooth e retornará o resultado.
     * O dispositivo Bluetooth já deve estar conectado neste momento.</p>
     */
    public void connect(Device device, BluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;

        if (initializeElm327()) {
            connected = true;
            startMonitoring();
            if (listener != null) {
                listener.onObdConnected(device);
            }
        } else {
            bluetoothManager.disconnect();
            connected = false;
            if (listener != null) {
                listener.onObdConnectionFailed("O dispositivo não respondeu como um ELM327!");
            }
        }
    }

    private boolean initializeElm327() {
        if (!sendOk(Elm327Commands.RESET)) return false;
        if (!sendOk(Elm327Commands.ECHO_OFF)) return false;
        if (!sendOk(Elm327Commands.LINE_FEEDS_OFF)) return false;
        if (!sendOk(Elm327Commands.SPACES_OFF)) return false;
        if (!sendOk(Elm327Commands.HEADERS_OFF)) return false;
        if (!sendOk(Elm327Commands.AUTO_PROTOCOL)) return false;

        String response = executeElm327Command(ObdCommands.SUPPORTED_PIDS_01_20);

        if (response == null) {
            return false;
        }

        response = cleanResponse(response);
        return response.contains("41 00");
    }

    private boolean sendOk(String command) {
        String response = executeElm327Command(command);

        if (response == null) {
            return false;
        }

        response = cleanResponse(response);
        return response.contains("OK") || response.contains("ELM327");
    }

    @NonNull
    private static String cleanResponse(String response) {
        response = response
                .replace(">", "")
                .replace("\r", "")
                .replace("\n", "")
                .trim()
                .toUpperCase();
        return response;
    }

    private void startMonitoring() {
        if (!connected || monitoring) {
            return;
        }

        monitoring = true;

        new Thread(() -> {
            while (monitoring) {
                int rpm = readRpm();

                if (listener != null) {
                    listener.onRpmChanged(rpm);
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private int readRpm() {
        String response = executeElm327Command(ObdCommands.ENGINE_RPM);

        return parseRpm(response);
    }

    private int parseRpm(String response) {
        if (response == null)
            return 0;

        response = cleanResponse(response);

        String[] data = response.split(" ");

        for (int i = 0; i < data.length - 3; i++) {
            if (data[i].equals("41") && data[i + 1].equals("0C")) {
                try {
                    int A = Integer.parseInt(data[i + 2], 16);
                    int B = Integer.parseInt(data[i + 3], 16);
                    return ((A * 256) + B) / 4;
                } catch (Exception e) {
                    return 0;
                }
            }
        }

        return 0;
    }

    private String executeElm327Command(String command) {
        return bluetoothManager.sendCommand(command + "\r");
    }

    /**
     * Desconecta do dispositivo atual.
     */
    public void disconnect() {
        monitoring = false;

        connected = false;

        if (bluetoothManager != null) {
            bluetoothManager.disconnect();
        }
    }

    public boolean isConnected() {
        return connected;
    }
}