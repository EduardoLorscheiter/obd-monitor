/*
 **=================================================================================================
 **  @file    MetricView.java
 **  @author  Eduardo Lorscheiter
 **  @class   MetricView
 **  @date    25/07/2026
 **  @version 1.0
 **  @brief   Componente gráfico responsável por exibir uma métrica do veículo em formato de cartão.
 **  @details O componente encapsula um layout contendo imagem, título, valor e unidade de medida,
 **           permitindo reutilização em diferentes telas do aplicativo para exibição de informações
 **           provenientes da ECU.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.eduardo.obdmonitor.R;

public class MetricView extends LinearLayout {
    /**
     * Texto responsável por exibir o valor atual da métrica.
     */
    private TextView txtValue;

    /**
     * Valor inicial definido no XML da métrica, restaurado ao limpar a medição.
     */
    private String initialValue;

    /**
     * Cor inicial do valor, restaurada junto com a medição.
     */
    private ColorStateList initialValueColor;

    public MetricView(Context context) {
        super(context);
        init(context, null);
    }

    public MetricView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MetricView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Inicializa o componente carregando seu layout interno e obtendo as referências dos elementos
     * visuais utilizados pelo cartão.
     *
     * <p>O layout é inflado automaticamente a partir de {@code R.layout.view_metric},
     * permitindo que o componente seja utilizado diretamente em arquivos XML ou criado
     * dinamicamente em tempo de execução.</p>
     *
     * @param context Contexto utilizado para inflar o layout.
     * @param attrs   Atributos personalizados definidos no XML para o componente.
     */
    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_metric, this, true);
        ImageView imgIcon = findViewById(R.id.imgIcon);
        TextView txtTitle = findViewById(R.id.txtTitle);
        TextView txtUnit = findViewById(R.id.txtUnit);
        txtValue = findViewById(R.id.txtValue);

        if (attrs != null) {
            try (TypedArray attributes = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.MetricView)) {

                Drawable icon = attributes.getDrawable(R.styleable.MetricView_metricIcon);
                String title = attributes.getString(R.styleable.MetricView_metricTitle);
                String unit = attributes.getString(R.styleable.MetricView_metricUnit);

                if (icon != null)
                    imgIcon.setImageDrawable(icon);
                if (title != null)
                    txtTitle.setText(title);
                if (unit != null)
                    txtUnit.setText(unit);
            }
        }
        initialValue = txtValue.getText().toString();
        initialValueColor = txtValue.getTextColors();
    }

    /**
     * Atualiza o valor principal exibido pelo cartão.
     *
     * @param value Valor a ser exibido.
     */
    public void setValue(String value) {
        txtValue.setText(value);
    }

    /**
     * Restaura o estado visual original do cartão: valor e cor da medição.
     */
    public void reset() {
        txtValue.setText(initialValue);
        txtValue.setTextColor(initialValueColor);
    }

    /**
     * Define a cor do valor exibido no cartão.
     *
     * @param color Recurso de cor.
     */
    public void setValueColor(int color) {
        txtValue.setTextColor(ContextCompat.getColor(getContext(), color));
    }
}
