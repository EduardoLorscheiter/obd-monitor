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

import com.eduardo.obdmonitor.model.Device;

import java.util.Set;

public class BluetoothManager {
    // Instância única do gerenciador Bluetooth (Singleton)
    private static BluetoothManager instance;

    // Contexto global da aplicação utilizado para acessar serviços Android
    private final Context applicationContext;

    // Responsável pela comunicação Bluetooth de baixo nível
    private final BluetoothConnection connection;

    // Listener utilizado para reportar eventos Bluetooth à interface
    private BluetoothListener bluetoothListener;

    // Indica se o BroadcastReceiver está registrado
    private boolean receiverRegistered;

    // Dispositivo atualmente conectado
    private Device connectedDevice;

    /**
     * Retorna a instância única do gerenciador Bluetooth.
     *
     * <p>O contexto recebido é convertido internamente para o contexto da aplicação, evitando
     * retenção de referências de Activity.</p>
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
     * Cria uma nova instância do gerenciador Bluetooth.
     *
     * <p>O contexto é armazenado como contexto da aplicação para garantir que o Singleton não
     * mantenha referência a componentes com ciclo de vida curto, como Activities.</p>
     *
     * @param context Contexto utilizado para obter o contexto da aplicação.
     */
    private BluetoothManager(Context context) {
        this.applicationContext = context.getApplicationContext();
        connection = new BluetoothConnection(this);
        bluetoothListener = null;
    }

    /**
     * Define o listener responsável por receber os eventos do Bluetooth.
     *
     * <p>O listener de conexão recebe as alterações de estado do adaptador, descoberta de
     * dispositivos pareados, conexão, desconexão e erros durante a comunicação.</p>
     *
     * @param bluetoothListener Listener que receberá eventos do Bluetooth.
     */
    public void setListener(BluetoothListener bluetoothListener) {
        this.bluetoothListener = bluetoothListener;
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
        applicationContext.registerReceiver(bluetoothReceiver, bluetoothFilter);

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

        applicationContext.unregisterReceiver(bluetoothReceiver);

        receiverRegistered = false;
    }

    /**
     * Verifica se o Bluetooth do dispositivo está ativado.
     *
     * @return {@code true} caso o Bluetooth esteja disponível e ativado, {@code false} caso
     * contrário.
     */
    @SuppressLint("MissingPermission")
    public boolean isBluetoothEnabled() {
        BluetoothAdapter adapter = getBluetoothAdapter();
        return adapter != null && adapter.isEnabled();
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
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        if (bluetoothAdapter == null)
            return;

        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivity(intent);
        }
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
            if (bluetoothListener != null)
                bluetoothListener.onBluetoothError("Nenhum dispositivo Bluetooth pareado encontrado!");
            return;
        }

        for (BluetoothDevice bluetoothDevice : bondedBluetoothDevices) {
            Device device = new Device(bluetoothDevice.getName(), bluetoothDevice.getAddress());

            if (connectedDevice != null && connectedDevice.getMacAddress().equals(device.getMacAddress()))
                device.setStatus(Device.DEVICE_STATUS_CONNECTED);

            if (bluetoothListener != null)
                bluetoothListener.onBluetoothDeviceBondedFound(device);
        }
    }

    /**
     * Inicia uma conexão Bluetooth com um dispositivo previamente pareado.
     *
     * <p>A conexão é realizada em segundo plano e os resultados são enviados através dos callbacks
     * do {@link BluetoothListener}.</p>
     *
     * @param device Dispositivo Bluetooth alvo da conexão.
     */
    @SuppressLint("MissingPermission")
    public void connect(Device device) {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        if (bluetoothAdapter == null)
            return;

        if (bluetoothListener != null)
            bluetoothListener.onBluetoothConnecting(device);

        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getMacAddress());

        if (bluetoothDevice == null) {
            if (bluetoothListener != null)
                bluetoothListener.onBluetoothError("Dispositivo não encontrado!");
            return;
        }

        connection.connect(bluetoothDevice, device);
    }

    /**
     * Encerra a conexão Bluetooth atualmente estabelecida.
     *
     * <p>Caso não exista uma conexão ativa, nenhuma ação é realizada.</p>
     */
    public void disconnect() {
        connection.disconnect();
    }

    /**
     * Verifica se existe uma conexão Bluetooth ativa.
     *
     * @return {@code true} se houver conexão ativa, {@code false} caso contrário.
     */
    public boolean isConnected() {
        return connection.isConnected();
    }

    /**
     * Recebe notificações do sistema relacionadas às alterações no estado do adaptador Bluetooth.
     */
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction()))
                return;

            if (bluetoothListener == null)
                return;

            int state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
            );

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    bluetoothListener.onBluetoothEnabled();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    bluetoothListener.onBluetoothDisabled();
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
        if (command == null || command.isEmpty())
            return null;

        return connection.sendCommand(command);
    }

    /**
     * Cancela uma eventual descoberta de dispositivos Bluetooth em andamento.
     *
     * <p>Deve ser interrompida antes de iniciar uma conexão, pois o processo de* busca pode reduzir
     * significativamente o desempenho ou até impedir o estabelecimento da conexão Bluetooth.</p>
     */
    @SuppressLint("MissingPermission")
    protected void cancelDiscovery() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        if (bluetoothAdapter != null)
            bluetoothAdapter.cancelDiscovery();
    }

    /**
     * Encaminha ao listener a notificação de que a conexão com o dispositivo Bluetooth foi
     * estabelecida.
     *
     * @param device Dispositivo conectado.
     */
    protected void notifyConnected(Device device) {
        connectedDevice = device;

        if (bluetoothListener != null)
            bluetoothListener.onBluetoothConnected(device);
    }

    /**
     * Encaminha ao listener a notificação de que a conexão com o dispositivo Bluetooth foi
     * encerrada.
     *
     * @param device Dispositivo desconectado.
     */
    protected void notifyDisconnected(Device device) {
        connectedDevice = null;

        if (bluetoothListener != null)
            bluetoothListener.onBluetoothDisconnected(device);
    }

    /**
     * Notifica a ocorrência de um erro durante uma operação Bluetooth.
     *
     * @param message Mensagem do erro.
     */
    protected void notifyError(String message) {
        if (bluetoothListener != null)
            bluetoothListener.onBluetoothError(message);
    }

    /**
     * Obtém e valida o adaptador Bluetooth do sistema.
     *
     * @return Adaptador Bluetooth válido ou {@code null} caso alguma validação falhe.
     */
    private BluetoothAdapter getBluetoothAdapter() {
        android.bluetooth.BluetoothManager systemBluetoothManager =
                applicationContext.getSystemService(android.bluetooth.BluetoothManager.class);

        if (systemBluetoothManager == null) {
            if (bluetoothListener != null)
                bluetoothListener.onBluetoothError("Bluetooth não disponível!");
            return null;
        }

        BluetoothAdapter bluetoothAdapter = systemBluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            if (bluetoothListener != null)
                bluetoothListener.onBluetoothError("Bluetooth não suportado!");
            return null;
        }

        if (!hasBluetoothPermission()) {
            if (bluetoothListener != null)
                bluetoothListener.onBluetoothError("Permissão Bluetooth não concedida!");
            return null;
        }

        return bluetoothAdapter;
    }

    /**
     * Verifica se o aplicativo possui permissão para operações Bluetooth.
     *
     * <p>Em versões anteriores ao Android 12 (API 31) a permissão {@code BLUETOOTH_CONNECT} não é
     * necessária. Em Android 12 (API 31) ou superior, a permissão é validada explicitamente.</p>
     *
     * @return {@code true} se a permissão estiver concedida, {@code false} caso contrário.
     */
    private boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            return true;

        return ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }
}
