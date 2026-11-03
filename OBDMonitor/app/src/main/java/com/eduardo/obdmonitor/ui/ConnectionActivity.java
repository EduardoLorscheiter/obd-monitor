/*
 **=================================================================================================
 **  @file    ConnectionActivity.java
 **  @author  Eduardo Lorscheiter
 **  @class   ConnectionActivity
 **  @date    21/07/2026
 **  @version 1.0
 **  @brief   Interface de gerenciamento da conexão Bluetooth com o adaptador ELM327.
 **  @details Esta Activity permite ao usuário verificar o estado do Bluetooth, visualizar
 **           dispositivos pareados, estabelecer conexão com o adaptador ELM327 e iniciar a
 **           comunicação com a ECU do veículo através do protocolo OBD-II. Também realiza o
 **           controle visual dos estados de conexão e erros durante o processo de comunicação.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eduardo.obdmonitor.R;
import com.eduardo.obdmonitor.bluetooth.BluetoothPermissions;
import com.eduardo.obdmonitor.model.Device;
import com.eduardo.obdmonitor.elm327.Elm327ConnectionListener;
import com.eduardo.obdmonitor.utils.ToastUtils;
import com.eduardo.obdmonitor.utils.ViewUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConnectionActivity extends BaseActivity implements Elm327ConnectionListener {
    /**
     * Chaves usadas para preservar uma solicitação durante a recriação da Activity.
     */
    private static final String STATE_PERMISSION_REQUESTED = "bluetooth_permission_requested";
    private static final String STATE_PERMISSION_IN_PROGRESS = "bluetooth_permission_in_progress";

    /**
     * Componentes da janela da Activity.
     */
    private ImageButton btnBack;
    private TextView txtBluetoothStatus, txtBluetoothPermission;
    private SwitchCompat switchBluetooth;
    private View containerBluetoothPermission;
    private MaterialButton btnBluetoothPermission;
    private ConstraintLayout containerBluetooth;
    private LinearLayout containerDevices, containerBondedDevices;

    /**
     * Solicitação de acesso Bluetooth e estado do diálogo de permissões do Android.
     */
    private ActivityResultLauncher<String[]> bluetoothPermissionLauncher;
    private ActivityResultLauncher<Intent> permissionSettingsLauncher;
    private boolean permissionRequested;
    private boolean permissionRequestInProgress;

    /**
     * Lista de dispositivos do tipo Bluetooth pareados.
     */
    private final List<Device> bondedDevices = new ArrayList<>();

    /**
     * Mapa de posição de cada dispositivo Bluetooth.
     */
    private final HashMap<String, TextView> statusDevices = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_connection);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupBluetoothPermissions(savedInstanceState);
        setListeners();
    }

    /**
     * Solicita acesso ao abrir a tela pela primeira vez, após registrar os listeners comuns.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (!BluetoothPermissions.areGranted(this) && !permissionRequested)
            requestBluetoothAccess();
    }

    /**
     * Preserva o estado para não duplicar o diálogo após uma recriação da tela.
     *
     * @param outState Estado que será restaurado pelo Android.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_PERMISSION_REQUESTED, permissionRequested);
        outState.putBoolean(STATE_PERMISSION_IN_PROGRESS, permissionRequestInProgress);
        super.onSaveInstanceState(outState);
    }

    /**
     * Registra o resultado da solicitação e restaura uma eventual solicitação em andamento.
     *
     * @param savedInstanceState Estado anterior da Activity, ou {@code null} na primeira abertura.
     */
    private void setupBluetoothPermissions(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            permissionRequested = savedInstanceState.getBoolean(
                    STATE_PERMISSION_REQUESTED
            );
            permissionRequestInProgress = savedInstanceState.getBoolean(
                    STATE_PERMISSION_IN_PROGRESS
            );
        }

        bluetoothPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    permissionRequestInProgress = false;
                    if (!isFinishing() && !isDestroyed())
                        synchronizeConnectionState();
                });
        permissionSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (!isFinishing() && !isDestroyed())
                        synchronizeConnectionState();
                });
    }

    /**
     * Solicita as permissões ou abre as configurações quando o Android não permite outro diálogo.
     */
    private void requestBluetoothAccess() {
        if (permissionRequestInProgress)
            return;

        if (BluetoothPermissions.areGranted(this)) {
            synchronizeConnectionState();
        } else if (shouldOpenPermissionSettings()) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            permissionSettingsLauncher.launch(intent);
        } else {
            permissionRequested = true;
            permissionRequestInProgress = true;
            showBluetoothPermissionRequired();
            bluetoothPermissionLauncher.launch(BluetoothPermissions.getRequiredPermissions());
        }
    }

    /**
     * Identifica uma recusa para a qual o sistema já não apresenta a solicitação de permissão.
     *
     * @return {@code true} quando alguma permissão recusada precisa ser alterada nas configurações.
     */
    private boolean shouldOpenPermissionSettings() {
        if (!permissionRequested)
            return false;

        for (String permission : BluetoothPermissions.getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
                    && !ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
                return true;
        }
        return false;
    }

    /**
     * Apresenta a falta de acesso sem confundi-lá com o rádio Bluetooth desligado.
     */
    private void showBluetoothPermissionRequired() {
        updateBluetoothStatus(false);
        cleanDevices();
        switchBluetooth.setEnabled(false);
        txtBluetoothStatus.setText(R.string.bluetooth_permission_required);
        containerBluetoothPermission.setVisibility(View.VISIBLE);
        boolean openSettings = !permissionRequestInProgress && shouldOpenPermissionSettings();
        txtBluetoothPermission.setText(openSettings
                ? R.string.bluetooth_permission_settings_description
                : R.string.bluetooth_permission_description);
        btnBluetoothPermission.setText(openSettings
                ? R.string.open_app_settings : R.string.allow_bluetooth);
        btnBluetoothPermission.setEnabled(!permissionRequestInProgress);
    }

    /**
     * Inicializa os componentes visuais da Activity.
     *
     * <p>Realiza a associação entre os elementos da interface e seus respectivos componentes
     * declarados no layout.</p>
     */
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        txtBluetoothStatus = findViewById(R.id.txtBluetoothStatus);
        switchBluetooth = findViewById(R.id.switchBluetooth);
        ViewUtils.applySwitchColors(this, switchBluetooth);
        containerBluetoothPermission = findViewById(R.id.containerBluetoothPermission);
        txtBluetoothPermission = findViewById(R.id.txtBluetoothPermission);
        btnBluetoothPermission = findViewById(R.id.btnBluetoothPermission);
        containerBluetooth = findViewById(R.id.containerBluetooth);
        containerDevices = findViewById(R.id.containerDevices);
        containerBondedDevices = findViewById(R.id.containerBondedDevices);
    }

    /**
     * Configura os eventos dos componentes da interface.
     */
    private void setListeners() {
        setClickSoundListener(btnBack, v -> finish());
        setClickSoundListener(btnBluetoothPermission, v -> requestBluetoothAccess());

        switchBluetooth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed())
                return;

            if (isChecked) {
                bluetoothManager.requestEnableBluetooth(this);
                switchBluetooth.setChecked(bluetoothManager.isBluetoothEnabled());
            } else {
                switchBluetooth.setChecked(true);
                ToastUtils.showToast(this, getString(R.string.turn_off_bluetooth_in_android_settings));
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    @Override
    protected void onBluetoothEnabled() {
        if (!BluetoothPermissions.areGranted(this)) {
            showBluetoothPermissionRequired();
            return;
        }
        containerBluetoothPermission.setVisibility(View.GONE);
        switchBluetooth.setEnabled(true);
        updateBluetoothStatus(true);
        loadDevices();
    }

    @Override
    protected void onBluetoothDisabled() {
        if (!BluetoothPermissions.areGranted(this)) {
            showBluetoothPermissionRequired();
            return;
        }
        containerBluetoothPermission.setVisibility(View.GONE);
        switchBluetooth.setEnabled(true);
        updateBluetoothStatus(false);
        cleanDevices();
    }

    @Override
    protected void onBluetoothDeviceBondedFound(Device device) {
        addBluetoothDevice(device, bondedDevices, containerBondedDevices);
    }

    @Override
    protected void onBluetoothConnecting(Device device) {
        updateStatus(device);
    }

    @Override
    protected void onBluetoothConnected(Device device, String message) {
        updateStatus(device);
        ToastUtils.showToast(this, message);
    }

    @Override
    protected void onBluetoothDisconnected(Device device, String message) {
        updateStatus(device);
        ToastUtils.showToast(this, message);
    }

    @Override
    protected void onBluetoothError(Device device, String message) {
        if (!BluetoothPermissions.areGranted(this)) {
            showBluetoothPermissionRequired();
            return;
        }
        updateStatus(device);
        ToastUtils.showToast(this, message);
    }

    @Override
    public void onElm327Connected(Device device) {
        runOnUiThread(() -> {
            if (!isFinishing())
                setResult(RESULT_OK);
        });
    }

    @Override
    public void onElm327BluetoothStatusChanged(boolean bluetoothConnected) {

    }

    @Override
    public void onElm327EcuStatusChanged(boolean obdConnected) {

    }

    @Override
    public void onElm327ProtocolChanged(String protocol) {

    }

    @Override
    public void onElm327Error(String message) {
        runOnUiThread(() -> ToastUtils.showToast(this, message));
    }

    /**
     * Limpa a lista de dispositivos e solicita novamente os dispositivos Bluetooth pareados ao
     * sistema.
     */
    private void loadDevices() {
        cleanDevices();
        bluetoothManager.loadBondedDevices();
    }

    /**
     * Remove todos os dispositivos armazenados e limpa os componentes visuais responsáveis por
     * exibi-los na interface.
     */
    private void cleanDevices() {
        bondedDevices.clear();
        statusDevices.clear();

        containerBondedDevices.removeAllViews();
    }

    /**
     * Atualiza os componentes visuais relacionados ao estado do Bluetooth.
     *
     * @param enabled Indica se o Bluetooth está ativado.
     */
    private void updateBluetoothStatus(boolean enabled) {
        switchBluetooth.setChecked(enabled);

        if (enabled) {
            txtBluetoothStatus.setText(R.string.enabled);
            int color = ViewUtils.getThemeAttrColor(
                    this,
                    com.google.android.material.R.attr.colorSecondaryVariant
            );
            txtBluetoothStatus.setTextColor(color);
            containerBluetooth.setBackgroundResource(R.drawable.container_bluetooth_enabled);
            containerDevices.setVisibility(View.VISIBLE);
        } else {
            txtBluetoothStatus.setText(R.string.disabled);
            int color = ViewUtils.getThemeAttrColor(
                    this,
                    com.google.android.material.R.attr.colorOnPrimary
            );
            txtBluetoothStatus.setTextColor(color);
            containerBluetooth.setBackgroundResource(R.drawable.container_bluetooth_disabled);
            containerDevices.setVisibility(View.GONE);
        }
    }

    /**
     * Adiciona um dispositivo Bluetooth à interface e configura sua ação de conexão ou desconexão
     * quando selecionado pelo usuário.
     *
     * @param device    Dispositivo Bluetooth.
     * @param list      Lista onde o dispositivo será armazenado.
     * @param container Container responsável pela exibição do dispositivo.
     */
    private void addBluetoothDevice(Device device, List<Device> list, LinearLayout container) {
        // Adiciona o dispositivo Bluetooth na lista
        list.add(device);

        // Cria o CardView do dispositivo Bluetooth
        CardView cardView = getCardView();

        // Cria o LinearLayout vertical do dispositivo Bluetooth para nome e posição
        LinearLayout verticalLayout = getVerticalLayout();

        // Cria o TextView do nome do dispositivo Bluetooth
        TextView tvName = getTvName(device.getName());
        verticalLayout.addView(tvName);

        // Cria o TextView da posição do dispositivo Bluetooth
        TextView tvStatus = getTvStatus(device.getStatus());
        verticalLayout.addView(tvStatus);

        statusDevices.put(device.getMacAddress(), tvStatus);

        // Adiciona verticalLayout ao cardView
        cardView.addView(verticalLayout);

        // Adiciona cardView ao container dos dispositivos com Bluetooth visíveis
        container.addView(cardView);

        setClickSoundListener(cardView, v -> bluetoothManager.connect(device));
    }

    /**
     * Cria e configura o CardView utilizado para representar um dispositivo Bluetooth na interface.
     *
     * @return CardView configurado.
     */
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
        int color = ViewUtils.getThemeAttrColor(
                this,
                com.google.android.material.R.attr.colorPrimaryContainer
        );
        cardView.setCardBackgroundColor(color);
        cardView.setClipChildren(true);
        cardView.setClipToPadding(true);
        return cardView;
    }

    /**
     * Cria o layout vertical utilizado para organizar o nome e a posição do dispositivo Bluetooth.
     *
     * @return Layout configurado.
     */
    private LinearLayout getVerticalLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (10 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.setBackground(null);
        return layout;
    }

    /**
     * Cria o componente responsável por exibir o nome do dispositivo.
     *
     * @param name Nome do dispositivo Bluetooth.
     * @return TextView configurado.
     */
    private TextView getTvName(String name) {
        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(16);
        tvName.setTypeface(null, Typeface.BOLD);
        int color = ViewUtils.getThemeAttrColor(
                this,
                com.google.android.material.R.attr.colorOnSurface
        );
        tvName.setTextColor(color);
        return tvName;
    }

    /**
     * Cria o componente responsável por exibir a posição do dispositivo.
     *
     * @param status Status inicial do dispositivo.
     * @return TextView configurado.
     */
    private TextView getTvStatus(int status) {
        TextView tvStatus = new TextView(this);
        tvStatus.setText(status);
        tvStatus.setTextSize(13);
        tvStatus.setPadding(0, 8, 0, 0);
        configureStatusTextView(tvStatus, status);
        return tvStatus;
    }

    /**
     * Atualiza a posição exibido na interface para um determinado dispositivo.
     *
     * @param device Dispositivo cuja posição será atualizado.
     */
    private void updateStatus(Device device) {
        if (device == null)
            return;

        TextView tvStatus = statusDevices.get(device.getMacAddress());

        if (tvStatus != null)
            configureStatusTextView(tvStatus, device.getStatus());
    }

    /**
     * Configura o texto e a aparência visual do componente responsável por exibir a posição de um
     * dispositivo Bluetooth.
     *
     * @param tvStatus Componente que exibirá o status.
     * @param status   Novo status do dispositivo.
     */
    private void configureStatusTextView(TextView tvStatus, int status) {
        tvStatus.setText(status);
        tvStatus.setTextColor(getColor(R.color.ios_grey));
    }
}
