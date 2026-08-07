/*
 **=================================================================================================
 **  @file    ToastUtils.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Classe utilitária responsável por exibir mensagens Toast personalizadas, aplicando
 **           automaticamente as cores do tema atual e evitando a sobreposição de múltiplas
 **           notificações.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.utils;

import android.app.Activity;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.R;

public class ToastUtils {
    // Referência para o Toast atual
    private static Toast currentToast;

    /**
     * Classe utilitária. Não permite instanciação.
     */
    private ToastUtils() {
    }

    /**
     * Exibe uma mensagem Toast personalizada utilizando as cores definidas pelo tema atual.
     *
     * <p>Caso exista um Toast previamente exibido, ele é cancelado antes da criação do novo,
     * evitando a sobreposição de múltiplas mensagens na interface.</p>
     *
     * <p>A aparência do Toast é ajustada para utilizar a cor de fundo e a cor do texto configuradas
     * no tema da aplicação.</p>
     *
     * @param activity Activity utilizada como contexto para criação e exibição do Toast.
     * @param text     Texto que será exibido na mensagem.
     */
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
            int backgroundColor = ViewUtils.getThemeAttrColor(activity, R.attr.colorSurface);
            background.setColorFilter(new BlendModeColorFilter(backgroundColor, BlendMode.SRC_IN));
            toastView.setBackground(background);

            // Define a cor do texto de acordo com o tema atual
            TextView toastText = toastView.findViewById(android.R.id.message);
            int textColor = ViewUtils.getThemeAttrColor(activity, R.attr.colorOnSurface);
            toastText.setTextColor(textColor);
        }

        // Exibe o Toast
        toast.show();

        // Guarda a referência do Toast atual para possível cancelamento futuro
        currentToast = toast;
    }
}
