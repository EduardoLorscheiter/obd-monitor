/*
 **=================================================================================================
 **  @file    SettingsActivity.java
 **  @author  Eduardo Lorscheiter
 **  @class   SettingsActivity
 **  @date    28/09/2026
 **  @version 1.0
 **  @brief   Tela de configurações do aplicativo.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.ui;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.View;

import com.eduardo.obdmonitor.utils.DialogUtils;
import com.eduardo.obdmonitor.utils.ViewUtils;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eduardo.obdmonitor.R;
import com.eduardo.obdmonitor.utils.SettingsUtils;

public class SettingsActivity extends BaseActivity {
    /**
     * Componentes da janela da Activity.
     */
    private ImageButton btnBack, btnSave;
    private Spinner spinnerTheme;
    private Spinner spinnerLanguage;
    private TextInputLayout layoutMaxRpm;
    private TextInputEditText editMaxRpm;
    private TextInputLayout layoutBlinkStartRpm;
    private TextInputEditText editBlinkStartRpm;
    private MaterialButtonToggleGroup groupSettingsCategory;
    private View cardAppearance;
    private View cardVehicle;
    private View cardAlerts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setListeners();
    }

    /**
     * Inicializa os componentes visuais da Activity.
     *
     * <p>Realiza a associação entre os elementos da interface e seus respectivos componentes
     * declarados no layout.</p>
     */
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);
        groupSettingsCategory = findViewById(R.id.groupSettingsCategory);
        cardAppearance = findViewById(R.id.cardAppearance);
        cardVehicle = findViewById(R.id.cardVehicle);
        cardAlerts = findViewById(R.id.cardAlerts);
        spinnerTheme = findViewById(R.id.spinnerTheme);
        ViewUtils.configureSpinner(this, spinnerTheme, R.array.theme_options);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        ViewUtils.configureSpinner(this, spinnerLanguage, R.array.language_options);
        layoutMaxRpm = findViewById(R.id.layoutMaxRpm);
        editMaxRpm = findViewById(R.id.editMaxRpm);
        editMaxRpm.setText(String.valueOf(SettingsUtils.getMaxRpm(this)));
        layoutBlinkStartRpm = findViewById(R.id.layoutBlinkStartRpm);
        editBlinkStartRpm = findViewById(R.id.editBlinkStartRpm);
        editBlinkStartRpm.setText(String.valueOf(SettingsUtils.getBlinkStartRpm(this)));
        TextView txtRpmRange = findViewById(R.id.txtRpmRange);
        txtRpmRange.setText(getString(
                R.string.vehicle_max_rpm_range,
                SettingsUtils.MIN_MAX_RPM,
                SettingsUtils.MAX_MAX_RPM
        ));
        TextView txtBlinkRpmRange = findViewById(R.id.txtBlinkRpmRange);
        txtBlinkRpmRange.setText(getString(
                R.string.rpm_blink_start_range,
                SettingsUtils.MIN_BLINK_START_RPM
        ));
        spinnerTheme.setSelection(Math.max(SettingsUtils.THEME_SYSTEM,
                Math.min(SettingsUtils.getThemeMode(this), SettingsUtils.THEME_DARK)));
        spinnerLanguage.setSelection(Math.max(0, Math.min(SettingsUtils.getLanguageIndex(this), 1)));
    }

    /**
     * Configura os eventos dos componentes da interface.
     */
    private void setListeners() {
        setClickSoundListener(btnBack, v -> finish());
        setClickSoundListener(btnSave, v -> saveSettings());
        setClickSoundListener(findViewById(R.id.btnAppearance), v -> { });
        setClickSoundListener(findViewById(R.id.btnVehicle), v -> { });
        setClickSoundListener(findViewById(R.id.btnAlerts), v -> { });
        setClickSoundListener(findViewById(R.id.btnVehicleMaxRpmHelp), v ->
                DialogUtils.show(this, R.string.vehicle_max_rpm_title,
                        R.string.vehicle_max_rpm_description));
        setClickSoundListener(findViewById(R.id.btnBlinkRpmHelp), v ->
                DialogUtils.show(this, R.string.rpm_blink_start_title,
                        R.string.rpm_blink_start_description));
        groupSettingsCategory.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            cardAppearance.setVisibility(checkedId == R.id.btnAppearance
                    ? View.VISIBLE : View.GONE);
            cardVehicle.setVisibility(checkedId == R.id.btnVehicle
                    ? View.VISIBLE : View.GONE);
            cardAlerts.setVisibility(checkedId == R.id.btnAlerts
                    ? View.VISIBLE : View.GONE);
        });
    }

    /**
     * Valida e salva as configurações do usuário.
     */
    private void saveSettings() {
        int maxRpm = parseRpm(editMaxRpm);
        if (maxRpm < SettingsUtils.MIN_MAX_RPM || maxRpm > SettingsUtils.MAX_MAX_RPM) {
            groupSettingsCategory.check(R.id.btnVehicle);
            showMaxRpmError();
            return;
        }

        int blinkStartRpm = parseRpm(editBlinkStartRpm);
        if (blinkStartRpm < SettingsUtils.MIN_BLINK_START_RPM || blinkStartRpm > maxRpm) {
            groupSettingsCategory.check(R.id.btnAlerts);
            showBlinkStartRpmError(maxRpm);
            return;
        }

        SettingsUtils.setThemeMode(this, spinnerTheme.getSelectedItemPosition());
        SettingsUtils.setLanguageIndex(this, spinnerLanguage.getSelectedItemPosition());
        SettingsUtils.setMaxRpm(this, maxRpm);
        SettingsUtils.setBlinkStartRpm(this, blinkStartRpm);

        layoutMaxRpm.setError(null);
        layoutBlinkStartRpm.setError(null);

        setResult(RESULT_OK);
        finish();
    }

    /**
     * Converte o conteúdo de um campo de entrada em um valor inteiro de RPM.
     *
     * <p>Quando o campo estiver vazio, contiver um valor inválido ou não puder ser convertido para
     * inteiro, retorna {@code -1}, permitindo que o valor seja tratado como inválido pelas regras
     * de validação das configurações.</p>
     *
     * @param input campo de entrada contendo o valor de RPM.
     * @return valor inteiro informado ou {@code -1} quando o conteúdo for inválido.
     */
    private int parseRpm(TextInputEditText input) {
        String value = input.getText() == null
                ? ""
                : input.getText().toString().trim();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Exibe uma mensagem de erro para o limite máximo de RPM.
     *
     * <p>A mensagem informa a faixa de valores permitida e mantém o foco no campo correspondente
     * para que o usuário possa corrigir o valor informado.</p>
     */
    private void showMaxRpmError() {
        layoutMaxRpm.setError(getString(
                R.string.vehicle_max_rpm_invalid,
                SettingsUtils.MIN_MAX_RPM,
                SettingsUtils.MAX_MAX_RPM
        ));
        editMaxRpm.requestFocus();
    }

    /**
     * Exibe uma mensagem de erro para a rotação de início do alerta visual.
     *
     * <p>O valor deve respeitar a rotação mínima definida pela aplicação e não pode ultrapassar o
     * limite máximo de RPM configurado pelo usuário.</p>
     *
     * @param maxRpm limite máximo de rotação configurado para o veículo.
     */
    private void showBlinkStartRpmError(int maxRpm) {
        layoutBlinkStartRpm.setError(getString(
                R.string.rpm_blink_start_invalid,
                SettingsUtils.MIN_BLINK_START_RPM,
                maxRpm
        ));
        editBlinkStartRpm.requestFocus();
    }
}
