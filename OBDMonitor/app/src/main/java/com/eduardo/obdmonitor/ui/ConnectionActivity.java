/*
 **===========================================================================
 **  @file    ConnectionActivity.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    Julho/2026
 **  @version 1.0
 **  @brief   Interface de gerenciamento da conexão Bluetooth com o adaptador
 **           OBD-II ELM327.
 **  @details Esta Activity permite ao usuário verificar o estado do Bluetooth,
 **           visualizar dispositivos pareados, estabelecer conexão com o
 **           adaptador OBD-II e iniciar a comunicação com a ECU do veículo.
 **           Também realiza o controle visual dos estados de conexão e erros
 **           durante o processo de comunicação.
 **===========================================================================
 */

package com.eduardo.obdmonitor.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eduardo.obdmonitor.R;
import com.eduardo.obdmonitor.bluetooth.BluetoothListener;
import com.eduardo.obdmonitor.bluetooth.BluetoothManager;
import com.eduardo.obdmonitor.model.Device;
import com.eduardo.obdmonitor.obd.ObdListener;
import com.eduardo.obdmonitor.obd.ObdManager;
import com.eduardo.obdmonitor.utils.ToastUtils;
import com.eduardo.obdmonitor.utils.ViewUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConnectionActivity extends AppCompatActivity implements BluetoothListener, ObdListener {
    // Componentes da janela da Activity
    private ImageButton btnBack;
    private TextView txtBluetoothStatus;
    private SwitchCompat switchBluetooth;
    private LinearLayout containerBluetooth, containerDevices, containerBondedDevices;

    // Conexão com o Bluetooth
    private BluetoothManager bluetoothManager;

    // Conexão com o OBD-II
    private ObdManager obdManager;

    // Lista de dispositivos Bluetooth pareados
    private final List<Device> bondedDevices = new ArrayList<>();

    // Mapa do status de cada dispositivo Bluetooth
    private final HashMap<String, TextView> statusDevices = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_connection);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initConnection();
        setListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDevices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finishConnection();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        txtBluetoothStatus = findViewById(R.id.txtBluetoothStatus);
        switchBluetooth = findViewById(R.id.switchBluetooth);
        containerBluetooth = findViewById(R.id.containerBluetooth);
        containerDevices = findViewById(R.id.containerDevices);
        containerBondedDevices = findViewById(R.id.containerBondedDevices);
    }

    private void initConnection() {
        bluetoothManager = new BluetoothManager(this, this);
        bluetoothManager.registerReceivers();
        obdManager = ObdManager.getInstance();
        obdManager.setListener(this);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> {
            obdManager.setListener(null);
            finish();
        });

        switchBluetooth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return;
            }

            if (isChecked) {
                bluetoothManager.requestEnableBluetooth(this);
            } else {
                switchBluetooth.setChecked(true);
                ToastUtils.showToast(this, "Desligue o Bluetooth pelas configurações do Android!");
            }
        });
    }

    private void finishConnection() {
        bluetoothManager.unregisterReceivers();
    }

    @Override
    public void onBluetoothEnabled() {
        runOnUiThread(() -> {
            updateBluetoothStatus(true);
            loadDevices();
        });
    }

    @Override
    public void onBluetoothDisabled() {
        runOnUiThread(() -> {
            updateBluetoothStatus(false);
            cleanDevices();
        });
    }

    @Override
    public void onBondedDeviceFound(Device device) {
        runOnUiThread(() -> addBluetoothDevice(device, bondedDevices, containerBondedDevices));
    }

    @Override
    public void onConnecting(Device device) {
        runOnUiThread(() -> {
            device.setStatus(Device.DEVICE_STATUS_CONNECTING);
            updateStatus(device);
        });
    }

    @Override
    public void onConnected(Device device) {
        runOnUiThread(() -> {
            device.setStatus(Device.DEVICE_STATUS_CONNECTED);
            updateStatus(device);
            obdManager.connect(device, bluetoothManager);
        });
    }

    @Override
    public void onDisconnected(Device device) {
        runOnUiThread(() -> {
            device.setStatus(Device.DEVICE_STATUS_NOT_CONNECTED);
            updateStatus(device);
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            ToastUtils.showToast(this, message);
            for (Device device : bondedDevices) {
                if (Device.DEVICE_STATUS_CONNECTING.equals(device.getStatus())) {
                    device.setStatus(Device.DEVICE_STATUS_NOT_CONNECTED);
                    updateStatus(device);
                    break;
                }
            }
        });
    }

    @Override
    public void onObdConnected(Device device) {
        obdManager.setListener(null);
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onObdConnectionFailed(String message) {
        runOnUiThread(() -> ToastUtils.showToast(this, message));
    }

    @Override
    public void onRpmChanged(int rpm) {

    }

    private void updateDevices() {
        if (bluetoothManager.isBluetoothEnabled()) {
            updateBluetoothStatus(true);
            loadDevices();
        } else {
            updateBluetoothStatus(false);
            cleanDevices();
        }
    }

    private void loadDevices() {
        cleanDevices();
        bluetoothManager.loadBondedDevices();
    }

    private void cleanDevices() {
        bondedDevices.clear();
        statusDevices.clear();

        containerBondedDevices.removeAllViews();
    }

    private void updateBluetoothStatus(boolean enabled) {
        switchBluetooth.setChecked(enabled);

        if (enabled) {
            txtBluetoothStatus.setText(R.string.ativado);
            containerBluetooth.setBackgroundResource(R.drawable.container_bluetooth_enabled);
            containerDevices.setVisibility(View.VISIBLE);
        } else {
            txtBluetoothStatus.setText(R.string.desativado);
            containerBluetooth.setBackgroundResource(R.drawable.container_bluetooth_disabled);
            containerDevices.setVisibility(View.GONE);
        }
    }

    private void addBluetoothDevice(Device device, List<Device> list, LinearLayout container) {
        // Adiciona o dispositivo Bluetooth na lista
        list.add(device);

        // Cria o CardView do dispositivo Bluetooth
        CardView cardView = getCardView();

        // Cria o LinearLayout vertical do dispositivo Bluetooth para nome e status
        LinearLayout verticalLayout = getVerticalLayout();

        // Cria o TextView do nome do dispositivo Bluetooth
        TextView tvName = getTvName(device.getName());
        verticalLayout.addView(tvName);
        cardView.setTag(tvName);

        // Cria o TextView do status do dispositivo Bluetooth
        TextView tvStatus = getTvStatus(device.getStatus());
        verticalLayout.addView(tvStatus);
        cardView.setTag(tvStatus);

        statusDevices.put(device.getMacAddress(), tvStatus);

        // Adiciona verticalLayout ao cardView
        cardView.addView(verticalLayout);

        // Adiciona cardView ao container dos dispositivos Bluetooth visíveis
        container.addView(cardView);

        cardView.setOnClickListener(v -> {
            // Se o dispositivo Bluetooth já estiver conectado
            if (bluetoothManager.isConnected()) {
                // Tenta realizar a desconexão com o dispositivo Bluetooth
                bluetoothManager.disconnect();
            } else {
                // Tenta realizar a conexão com o dispositivo Bluetooth
                bluetoothManager.connect(device);
            }
        });
    }

    private CardView getCardView() {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int spacing = (int) (5 * getResources().getDisplayMetrics().density);
        cardParams.setMargins(0, 0, 0, spacing);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(20);
        int color = ViewUtils.getThemeAttrColor(this, com.google.android.material.R.attr.colorPrimaryContainer);
        cardView.setCardBackgroundColor(color);
        cardView.setClipChildren(true);
        cardView.setClipToPadding(true);
        return cardView;
    }

    private LinearLayout getVerticalLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (10 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.setBackground(null);
        return layout;
    }

    private TextView getTvName(String name) {
        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(16);
        tvName.setTypeface(null, Typeface.BOLD);
        int color = ViewUtils.getThemeAttrColor(this, com.google.android.material.R.attr.colorOnSurface);
        tvName.setTextColor(color);
        return tvName;
    }

    private TextView getTvStatus(String status) {
        TextView tvStatus = new TextView(this);
        tvStatus.setText(status);
        tvStatus.setTextSize(13);
        tvStatus.setPadding(0, 8, 0, 0);
        configureStatusTextView(tvStatus, status);
        return tvStatus;
    }

    private void updateStatus(Device device) {
        TextView tvStatus = statusDevices.get(device.getMacAddress());

        if (tvStatus != null) {
            configureStatusTextView(tvStatus, device.getStatus());
        }
    }

    private void configureStatusTextView(TextView tvStatus, String status) {
        tvStatus.setText(status);
        tvStatus.setTextColor(getColor(R.color.ios_grey));
    }
}