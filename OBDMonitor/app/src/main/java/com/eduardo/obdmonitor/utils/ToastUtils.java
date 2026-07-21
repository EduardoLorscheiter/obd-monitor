/*
 **===========================================================================
 **  @file    ToastUtils.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    Julho/2026
 **  @version 1.0
 **  @brief   OBDMonitor
 **===========================================================================
 */

package com.eduardo.obdmonitor.utils;

import android.app.Activity;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ToastUtils {
    private static Toast currentToast;

    public static void showToast(Activity activity, String text) {
        // Cancela o Toast anterior caso esteja sendo exibido
        if (currentToast != null) {
            currentToast.cancel();
        }

        // Cria um novo Toast com duração longa
        Toast toast = Toast.makeText(activity, text, Toast.LENGTH_LONG);
        @SuppressWarnings("deprecation") View toastView = toast.getView();

        // Se definiu o toast
        if (toastView != null) {
            // Obtém o fundo padrão do Toast (normalmente um drawable arredondado)
            Drawable background = toastView.getBackground();

            // Define a cor de fundo de acordo com o tema atual
            int backgroundColor = ViewUtils.getThemeAttrColor(activity, com.google.android.material.R.attr.colorSurface);
            background.setColorFilter(new BlendModeColorFilter(backgroundColor, BlendMode.SRC_IN));
            toastView.setBackground(background);

            // Define a cor do texto de acordo com o tema atual
            TextView toastText = toastView.findViewById(android.R.id.message);
            int textColor = ViewUtils.getThemeAttrColor(activity, com.google.android.material.R.attr.colorOnSurface);
            toastText.setTextColor(textColor);
        }

        // Exibe o Toast
        toast.show();

        // Guarda a referência do Toast atual para possível cancelamento futuro
        currentToast = toast;
    }
}
