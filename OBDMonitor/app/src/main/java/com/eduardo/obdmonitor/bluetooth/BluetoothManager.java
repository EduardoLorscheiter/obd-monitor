/*
 **=================================================================================================
 **  @file    BluetoothManager.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Gerencia as operações Bluetooth da aplicação, incluindo o monitoramento do estado do
 **           adaptador, carregamento de dispositivos pareados, estabelecimento e encerramento de
 **           conexões, além da comunicação com a interface por meio de eventos.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import com.eduardo.obdmonitor.R;
import com.eduardo.obdmonitor.model.Device;

import java.util.Set;

public class BluetoothManager implements BluetoothConnectionListener {
    // Instância única do gerenciador Bluetooth (Singleton)
    private static BluetoothManager instance;

    // Contexto da aplicação utilizado para acessar recursos
    private final Context context;

    // Responsável pela comunicação Bluetooth de baixo nível
    private final BluetoothConnection connection;

    // Listener utilizado para reportar eventos Bluetooth à interface
    private BluetoothManagerListener listener;

    // Indica se o BroadcastReceiver está registrado
    private boolean receiverRegistered;

    // Dispositivo cuja conexão está sendo estabelecida
    private Device connectingDevice;

    // Dispositivo atualmente conectado
    private Device connectedDevice;

    /**
     * Retorna a instância única do gerenciador Bluetooth.
     *
     * <p>O contexto recebido é convertido internamente para o contexto da aplicação, evitando
     * a retenção de referências de componentes com ciclo de vida curto, como Activities.</p>
     *
     * @param context Contexto utilizado para obter o contexto da aplicação.
     * @return Instância única de {@code BluetoothManager}.
     */
    public static synchronized BluetoothManager getInstance(Context context) {
        if (instance == null)
            instance = new BluetoothManager(context);

        return instance;
    }

    /**
     * Retorna a instância única do gerenciador Bluetooth previamente inicializada.
     *
     * <p>Deve ser utilizado após a criação da instância através de {@link #getInstance(Context)}.
     * Caso o gerenciador ainda não tenha sido inicializado, uma {@link IllegalStateException} será
     * lançada.</p>
     *
     * @return Instância única de {@code BluetoothManager}.
     * @throws IllegalStateException Caso o gerenciador ainda não tenha sido inicializado.
     */
    public static synchronized BluetoothManager getInstance() {
        if (instance == null)
            throw new IllegalStateException("BluetoothManager has not been initialized!");

        return instance;
    }

    /**
     * Cria uma nova instância do gerenciador Bluetooth.
     *
     * <p>O contexto é armazenado como contexto da aplicação para garantir que o Singleton não
     * mantenha referência a componentes com ciclo de vida curto, como Activities.</p>
     *
     * @param context Contexto da aplicação utilizado para acessar recursos.
     */
    private BluetoothManager(Context context) {
        this.context = context.getApplicationContext();
        this.connection = new BluetoothConnection(this.context, this);
    }

    /**
     * Define o listener responsável por receber os eventos do Bluetooth.
     *
     * <p>O listener de conexão recebe as alterações de estado do adaptador, descoberta de
     * dispositivos pareados, conexão, desconexão e erros durante a comunicação.</p>
     *
     * @param listener Listener que receberá eventos do Bluetooth.
     */
    public void setListener(BluetoothManagerListener listener) {
        this.listener = listener;
    }

    /**
     * Registra o BroadcastReceiver utilizado pelo gerenciador Bluetooth.
     *
     * <p>O receptor monitora alterações no estado do adaptador Bluetooth, permitindo identificar
     * quando o Bluetooth é habilitado ou desabilitado.</p>
     *
     * <p>Deve ser chamado antes de iniciar o monitoramento do Bluetooth e deve ser acompanhado da
     * chamada do {@link #unregisterReceivers()} quando não for mais necessário.</p>
     */
    public void registerReceivers() {
        if (receiverRegistered)
            return;

        IntentFilter bluetoothFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(bluetoothReceiver, bluetoothFilter);

        receiverRegistered = true;
    }

    /**
     * Remove os BroadcastReceivers previamente registrados.
     *
     * <p>Deve ser chamado quando o componente responsável pelo Bluetooth for finalizado, evitando
     * vazamentos de memória e registros duplicados dos receptores.</p>
     */
    public void unregisterReceivers() {
        if (!receiverRegistered)
            return;

        context.unregisterReceiver(bluetoothReceiver);

        receiverRegistered = false;
    }

    /**
     * Solicita ao sistema a ativação do Bluetooth através da interface de confirmação do usuário.
     *
     * <p>Se o dispositivo não possuir suporte a Bluetooth ou ocorrer alguma falha ao obter o
     * adaptador Bluetooth, a solicitação não será realizada.</p>
     *
     * <p>Quando o Bluetooth já estiver ativado, nenhum diálogo é exibido.</p>
     *
     * @param activity Activity utilizada para iniciar a solicitação de ativação do Bluetooth.
     */
    @SuppressLint("MissingPermission")
    public void requestEnableBluetooth(Activity activity) {
        if (!isBluetoothEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivity(intent);
        }
    }

    /**
     * Verifica se o Bluetooth do dispositivo está ativado.
     *
     * @return {@code true} caso o Bluetooth esteja disponível e ativado, {@code false} caso
     * contrário.
     */
    public boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Carrega os dispositivos Bluetooth previamente pareados com o sistema.
     *
     * <p>Cada dispositivo encontrado é convertido para o modelo da aplicação e enviado ao listener
     * através de callback.</p>
     */
    @SuppressLint("MissingPermission")
    public void loadBondedDevices() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        if (bluetoothAdapter == null)
            return;

        Set<BluetoothDevice> bondedBluetoothDevices = bluetoothAdapter.getBondedDevices();

        if (bondedBluetoothDevices.isEmpty()) {
            notifyError(BluetoothManagerError.NO_BONDED_DEVICES);
            return;
        }

        for (BluetoothDevice bluetoothDevice : bondedBluetoothDevices) {
            Device device = new Device(bluetoothDevice.getName(), bluetoothDevice.getAddress());

            if (connectedDevice != null &&
                    connectedDevice.getMacAddress().equals(device.getMacAddress()))
                device.setStatusConnected();

            if (listener != null)
                listener.onBluetoothManagerDeviceBondedFound(device);
        }
    }

    /**
     * Inicia uma conexão Bluetooth com um dispositivo previamente pareado.
     *
     * <p>A conexão é realizada em segundo plano e os resultados são enviados através dos callbacks
     * do {@link BluetoothManagerListener}.</p>
     *
     * @param device Dispositivo Bluetooth alvo da conexão.
     */
    public void connect(Device device) {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        if (bluetoothAdapter == null)
            return;

        // Ignora um novo clique no mesmo dispositivo que já está conectando
        if (connectingDevice != null &&
                connectingDevice.getMacAddress().equals(device.getMacAddress())) {
            return;
        }

        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getMacAddress());

        if (bluetoothDevice == null) {
            notifyError(BluetoothManagerError.DEVICE_NOT_FOUND);
            return;
        }

        cancelDiscovery();

        // Se já existe uma conexão estabelecida, encerra essa conexão antes de iniciar uma nova
        if (connectedDevice != null &&
                !connectedDevice.getMacAddress().equals(device.getMacAddress())) {

            connection.disconnect();
            connectedDevice.setStatusDisconnected();

            if (listener != null)
                listener.onBluetoothManagerDisconnected(
                        connectedDevice,
                        context.getString(R.string.device_disconnected, connectedDevice.getName())
                );

            connectedDevice = null;
        }

        // Se existe uma tentativa de conexão, cancela essa tentativa antes de iniciar a nova
        if (connectingDevice != null &&
                !connectingDevice.getMacAddress().equals(device.getMacAddress())) {

            Device previousDevice = connectingDevice;

            connection.cancelConnection();

            previousDevice.setStatusDisconnected();
            if (listener != null)
                listener.onBluetoothManagerDisconnected(
                        connectingDevice,
                        context.getString(R.string.device_disconnected, connectingDevice.getName())
                );
        }

        // Define o novo dispositivo como alvo da tentativa
        connectingDevice = device;

        device.setStatusConnecting();

        if (listener != null)
            listener.onBluetoothManagerConnecting(device);

        connection.connect(bluetoothDevice);
    }

    /**
     * Encerra a conexão Bluetooth atualmente estabelecida.
     *
     * <p>Caso não exista uma conexão ativa, nenhuma ação é realizada.</p>
     */
    public void disconnect() {
        if (connectingDevice != null) {
            connection.cancelConnection();

            Device device = connectingDevice;
            connectingDevice = null;

            device.setStatusDisconnected();

            if (listener != null)
                listener.onBluetoothManagerDisconnected(
                        device,
                        context.getString(
                                R.string.device_disconnected,
                                device.getName()
                        )
                );

            return;
        }

        if (connectedDevice != null) {
            connection.disconnect();
        }
    }

    /**
     * Recebe notificações do sistema relacionadas às alterações no estado do adaptador Bluetooth.
     */
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction()))
                return;

            if (listener == null)
                return;

            int state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
            );

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    listener.onBluetoothManagerEnabled();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    listener.onBluetoothManagerDisabled();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Envia um comando ao dispositivo Bluetooth conectado.
     *
     * <p>Utilizado para comunicação com dispositivos que utilizam comunicação serial Bluetooth,
     * como adaptadores OBD-II ELM327.</p>
     *
     * @param command Comando a ser enviado.
     * @return Resposta recebida do dispositivo ou {@code null} caso ocorra erro.
     */
    public String sendCommand(String command) {
        return connection.sendCommand(command);
    }

    @Override
    public void onBluetoothConnectionConnected() {
        if (connectingDevice == null)
            return;

        Device device = connectingDevice;
        connectingDevice = null;
        connectedDevice = device;
        device.setStatusConnected();

        if (listener != null) {
            String message = context.getString(R.string.device_connected, device.getName());
            listener.onBluetoothManagerConnected(device, message);
        }
    }

    @Override
    public void onBluetoothConnectionDisconnected() {
        if (connectedDevice == null)
            return;

        Device device = connectedDevice;
        connectedDevice = null;
        device.setStatusDisconnected();

        if (listener != null) {
            String message = context.getString(R.string.device_disconnected, device.getName());
            listener.onBluetoothManagerDisconnected(device, message);
        }
    }

    @Override
    public void onBluetoothConnectionError(String message) {
        if (connectingDevice == null)
            return;

        Device device = connectingDevice;
        connectingDevice = null;
        device.setStatusDisconnected();

        if (listener != null)
            listener.onBluetoothManagerError(device, message);
    }

    /**
     * Cancela uma eventual descoberta de dispositivos Bluetooth em andamento.
     *
     * <p>Deve ser interrompida antes de iniciar uma conexão, pois o processo de busca pode reduzir
     * significativamente o desempenho ou até impedir o estabelecimento da conexão Bluetooth.</p>
     */
    @SuppressLint("MissingPermission")
    private void cancelDiscovery() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        if (bluetoothAdapter != null)
            bluetoothAdapter.cancelDiscovery();
    }

    /**
     * Obtém e valida o adaptador Bluetooth do sistema.
     *
     * <p>Também verifica se o aplicativo possui as permissões necessárias para realizar operações
     * Bluetooth.</p>
     *
     * @return Adaptador Bluetooth válido ou {@code null} caso alguma validação falhe.
     */
    private BluetoothAdapter getBluetoothAdapter() {
        android.bluetooth.BluetoothManager systemBluetoothManager =
                context.getSystemService(android.bluetooth.BluetoothManager.class);

        if (systemBluetoothManager == null) {
            notifyError(BluetoothManagerError.BLUETOOTH_NOT_AVAILABLE);
            return null;
        }

        BluetoothAdapter systemBluetoothAdapter = systemBluetoothManager.getAdapter();

        if (systemBluetoothAdapter == null) {
            notifyError(BluetoothManagerError.BLUETOOTH_NOT_SUPPORTED);
            return null;
        }

        if (!hasBluetoothPermission()) {
            notifyError(BluetoothManagerError.BLUETOOTH_PERMISSION_NOT_GRANTED);
            return null;
        }

        return systemBluetoothAdapter;
    }

    /**
     * Verifica se o aplicativo possui permissão para realizar operações Bluetooth.
     *
     * <p>Em versões anteriores ao Android 12 (API 31), a permissão {@code BLUETOOTH_CONNECT} não é
     * necessária. No Android 12 (API 31) ou superior, a permissão é validada explicitamente.</p>
     *
     * @return {@code true} se a permissão estiver concedida, {@code false} caso contrário.
     */
    private boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            return true;

        return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Notifica o listener sobre um erro ocorrido durante uma operação Bluetooth.
     *
     * @param error Tipo do erro ocorrido.
     */
    private void notifyError(BluetoothManagerError error) {
        if (listener != null)
            listener.onBluetoothManagerError(connectedDevice, error.getMessage(context));
    }
}
