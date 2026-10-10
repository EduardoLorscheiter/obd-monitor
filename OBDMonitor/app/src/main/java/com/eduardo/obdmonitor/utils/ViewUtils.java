/*
 **=================================================================================================
 **  @file    ViewUtils.java
 **  @author  Eduardo Lorscheiter
 **  @class   ViewUtils
 **  @date    21/07/2026
 **  @version 1.0
 **  @brief   Classe utilitária que reúne métodos auxiliares para manipulação, configuração e
 **           personalização de componentes da interface gráfica.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.eduardo.obdmonitor.R;

public class ViewUtils {
    /**
     * Classe utilitária. Não permite instanciação.
     */
    private ViewUtils() {
    }

    /**
     * Obtém uma cor definida por um atributo do tema atual.
     *
     * <p>Permite que componentes personalizados utilizem cores configuradas no tema da aplicação,
     * mantendo compatibilidade com diferentes estilos e modos de exibição.</p>
     *
     * @param context Contexto utilizado para acesso ao tema.
     * @param attrId  Identificador do atributo de cor do tema.
     * @return Valor inteiro correspondente à cor resolvida.
     */
    public static int getThemeAttrColor(Context context, int attrId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrId, typedValue, true);
        return typedValue.data;
    }

    /**
     * Atualiza o indicador visual de posição.
     *
     * @param view      View utilizada como indicador.
     * @param connected Estado atual do indicador.
     */
    public static void setStatusIndicator(View view, boolean connected) {
        view.setBackgroundResource(connected ? R.drawable.status_on : R.drawable.status_off);
    }

    /**
     * Aplica as cores configuradas para componentes SwitchCompat.
     *
     * @param context      Contexto utilizado para acesso aos recursos.
     * @param switchCompat Componente SwitchCompat que receberá as cores.
     */
    public static void applySwitchColors(Context context, SwitchCompat switchCompat) {
        ColorStateList thumbColor = ContextCompat.getColorStateList(context, R.color.switch_thumb);
        ColorStateList trackColor = ContextCompat.getColorStateList(context, R.color.switch_track);

        if (thumbColor != null)
            switchCompat.setThumbTintList(thumbColor);

        if (trackColor != null)
            switchCompat.setTrackTintList(trackColor);
    }

    /**
     * Configura um Spinner utilizando as opções e os layouts visuais especificados.
     *
     * @param context         Contexto utilizado para acesso aos recursos.
     * @param spinner         Spinner que receberá as opções.
     * @param optionsResource Recurso contendo as opções exibidas.
     */
    public static void configureSpinner(Context context, Spinner spinner, int optionsResource) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context,
                optionsResource,
                R.layout.spinner_selected_item
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
}