/*
 **===========================================================================
 **  @file    BluetoothManager.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    Julho/2026
 **  @version 1.0
 **  @brief   Gerencia as operações Bluetooth da aplicação, incluindo o
 **           monitoramento do estado do adaptador, carregamento de
 **           dispositivos pareados, estabelecimento e encerramento de
 **           conexões, além da comunicação com a interface por meio de
 **           eventos.
 **===========================================================================
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
    // Contexto da aplicação ou Activity
    private final Context context;

    // Responsável pela comunicação Bluetooth de baixo nível
    private final BluetoothConnection connection;

    // Listener utilizado para reportar eventos Bluetooth à interface
    private final BluetoothListener listener;

    /**
     * Cria uma nova instância do gerenciador Bluetooth.
     *
     * @param context  Contexto da aplicação ou Activity.
     * @param listener Listener que receberá eventos do Bluetooth.
     */
    public BluetoothManager(Context context, BluetoothListener listener) {
        this.context = context;
        this.listener = listener;
        connection = new BluetoothConnection(this);
    }

    /**
     * Registra os BroadcastReceivers utilizados pelo gerenciador Bluetooth.
     *
     * <p>São registrados dois receptores:
     * <ul>
     *     <li>Alterações no estado do adaptador Bluetooth (ligado/desligado);</li>
     * </ul>
     *
     * <p>Deve ser chamado antes de iniciar o monitoramento do Bluetooth e deve ser acompanhado da
     * chamada do {@link #unregisterReceivers()} quando não for mais necessário.</p>
     */
    public void registerReceivers() {
        IntentFilter bluetoothFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(bluetoothReceiver, bluetoothFilter);
    }

    /**
     * Remove os BroadcastReceivers previamente registrados.
     *
     * <p>Deve ser chamado quando o componente responsável pelo Bluetooth for finalizado,
     * evitando vazamentos de memória e registros duplicados dos receptores.</p>
     */
    public void unregisterReceivers() {
        context.unregisterReceiver(bluetoothReceiver);
    }

    /**
     * Monitora alterações no estado do Bluetooth.
     */
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }

            int state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
            );

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    listener.onBluetoothEnabled();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    listener.onBluetoothDisabled();
                    break;
            }
        }
    };

    /**
     * Verifica se o Bluetooth do dispositivo está ativado.
     *
     * @return True caso o Bluetooth esteja disponível e ativado, false caso contrário.
     */
    @SuppressLint("MissingPermission")
    public boolean isBluetoothEnabled() {
        BluetoothAdapter adapter = getBluetoothAdapter();
        return adapter != null && adapter.isEnabled();
    }

    /**
     * Solicita ao usuário a ativação do Bluetooth, caso ele esteja desativado.
     *
     * <p>Se o dispositivo não possuir suporte a Bluetooth ou ocorrer alguma
     * falha ao obter o adaptador Bluetooth, a solicitação não será realizada.</p>
     *
     * <p>Quando o Bluetooth já estiver ativado, nenhum diálogo é exibido.</p>
     *
     * @param activity Activity utilizada para iniciar a solicitação de ativação do Bluetooth.
     */
    @SuppressLint("MissingPermission")
    public void requestEnableBluetooth(Activity activity) {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        if (bluetoothAdapter == null) {
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivity(intent);
        }
    }

    /**
     * Carrega todos os dispositivos Bluetooth previamente pareados.
     */
    @SuppressLint("MissingPermission")
    public void loadBondedDevices() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        if (bluetoothAdapter == null) {
            return;
        }

        Set<BluetoothDevice> bondedBluetoothDevices = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice bluetoothDevice : bondedBluetoothDevices) {
            Device device = new Device(
                    bluetoothDevice.getName(),
                    bluetoothDevice.getAddress());
            listener.onBondedDeviceFound(device);
        }
    }

    /**
     * Inicia a conexão com um dispositivo Bluetooth.
     *
     * @param device Dispositivo a ser conectado.
     */
    @SuppressLint("MissingPermission")
    public void connect(Device device) {
        listener.onConnecting(device);

        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        if (bluetoothAdapter == null) {
            return;
        }

        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getMacAddress());

        if (bluetoothDevice == null) {
            listener.onError("Dispositivo não encontrado!");
            return;
        }

        connection.connect(bluetoothDevice, device);
    }

    /**
     * Encerra a conexão Bluetooth ativa.
     */
    public void disconnect() {
        connection.disconnect();
    }

    /**
     * Retorna se existe uma conexão Bluetooth ativa.
     *
     * @return True se houver conexão ativa, false caso contrário.
     */
    public boolean isConnected() {
        return connection.isConnected();
    }

    /**
     * Cancela uma eventual descoberta de dispositivos Bluetooth em andamento.
     *
     * <p>É recomendado interromper a descoberta antes de iniciar uma conexão,
     * pois o processo de busca pode reduzir significativamente o desempenho
     * ou até impedir o estabelecimento da conexão Bluetooth.</p>
     */
    @SuppressLint("MissingPermission")
    protected void cancelDiscovery() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    /**
     * Envia um comando ao dispositivo Bluetooth conectado.
     *
     * @param command Comando a ser enviado.
     * @return Resposta do comando enviado ao dispositivo.
     */
    public String sendCommand(String command) {
        return connection.sendCommand(command);
    }

    /**
     * Obtém e valida o adaptador Bluetooth do sistema.
     *
     * @return Instância válida de BluetoothAdapter ou null caso alguma validação falhe.
     */
    private BluetoothAdapter getBluetoothAdapter() {
        android.bluetooth.BluetoothManager bluetoothManager = context.getSystemService(android.bluetooth.BluetoothManager.class);

        if (bluetoothManager == null) {
            listener.onError("Bluetooth não disponível!");
            return null;
        }

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            listener.onError("Bluetooth não suportado!");
            return null;
        }

        if (!hasBluetoothPermission()) {
            listener.onError("Permissão Bluetooth não concedida!");
            return null;
        }

        return bluetoothAdapter;
    }

    /**
     * Verifica se o aplicativo possui permissão para operações Bluetooth.
     *
     * <p>Em Android 10 e 11 a permissão BLUETOOTH_CONNECT não é
     * necessária. Em Android 12 (API 31) ou superior, a permissão é validada
     * explicitamente.</p>
     *
     * @return True se a permissão estiver concedida, false caso contrário.
     */
    private boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }

        return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Notifica que a conexão com o dispositivo Bluetooth foi estabelecida.
     *
     * @param device Dispositivo conectado.
     */
    protected void notifyConnected(Device device) {
        listener.onConnected(device);
    }

    /**
     * Notifica que a conexão Bluetooth foi encerrada.
     *
     * @param device Dispositivo desconectado.
     */
    protected void notifyDisconnected(Device device) {
        listener.onDisconnected(device);
    }

    /**
     * Notifica a ocorrência de um erro durante uma operação Bluetooth.
     *
     * @param message Mensagem do erro.
     */
    protected void notifyError(String message) {
        listener.onError(message);
    }
}
