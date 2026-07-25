/*
 **=================================================================================================
 **  @file    MetricCardView.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Componente gráfico responsável por exibir uma métrica do veículo em formato de cartão.
 **  @details O componente encapsula um layout contendo título, valor e unidade de medida,
 **           permitindo reutilização em diferentes telas do aplicativo para exibição de informações
 **           provenientes da ECU.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eduardo.obdmonitor.R;

public class MetricCardView extends LinearLayout {
    // Texto responsável por identificar a métrica exibida
    private TextView txtTitle;

    // Texto responsável por exibir o valor atual da métrica
    private TextView txtValue;

    // Texto responsável por exibir a unidade de medida da métrica
    private TextView txtUnit;

    public MetricCardView(Context context) {
        super(context);
        init(context);
    }

    public MetricCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MetricCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Inicializa o componente carregando seu layout interno e obtendo as referências dos elementos
     * visuais utilizados pelo cartão.
     *
     * <p>O layout é inflado automaticamente a partir de {@code R.layout.view_metric_card},
     * permitindo que o componente seja utilizado diretamente em arquivos XML ou criado
     * dinamicamente em tempo de execução.</p>
     *
     * @param context Contexto utilizado para inflar o layout.
     */
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_metric_card, this, true);
        txtTitle = findViewById(R.id.txtTitle);
        txtValue = findViewById(R.id.txtValue);
        txtUnit = findViewById(R.id.txtUnit);
    }

    /**
     * Define o título da métrica exibida pelo cartão.
     *
     * <p>Normalmente representa o nome do parâmetro proveniente da ECU, como "Velocidade", "RPM",
     * "Temperatura" ou "Bateria".</p>
     *
     * @param title Texto do título.
     */
    public void setTitle(String title) {
        txtTitle.setText(title);
    }

    /**
     * Atualiza o valor principal exibido pelo cartão.
     *
     * <p>Este valor representa a leitura atual da métrica monitorada.</p>
     *
     * @param value Valor a ser exibido.
     */
    public void setValue(String value) {
        txtValue.setText(value);
    }

    /**
     * Define a unidade de medida apresentada ao lado do valor.
     *
     * <p>Exemplos: km/h, RPM, °C, %, V, L/h.</p>
     *
     * @param unit Unidade de medida da métrica.
     */
    public void setUnit(String unit) {
        txtUnit.setText(unit);
    }
}