/*
 **=================================================================================================
 **  @file    DialogUtils.java
 **  @author  Eduardo Lorscheiter
 **  @class   DialogUtils
 **  @date    24/10/2026
 **  @version 1.0
 **  @brief   Exibe diálogos informativos com o tema visual do aplicativo.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.eduardo.obdmonitor.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.MaterialShapeDrawable;

/**
 * Centraliza a criação e a apresentação de diálogos informativos padronizados do aplicativo.
 */
public final class DialogUtils {
    /**
     * Classe utilitária. Não permite instanciação.
     */
    private DialogUtils() {
    }

    /**
     * Exibe um diálogo informativo com título, mensagem e botão de confirmação.
     *
     * @param context   Contexto temático da tela que apresenta o diálogo.
     * @param titleId   Recurso de texto usado como título.
     * @param messageId Recurso de texto usado como mensagem.
     */
    public static void show(Context context, int titleId, int messageId) {
        show(context, context.getText(titleId), context.getText(messageId));
    }

    /**
     * Exibe um diálogo informativo a partir de textos resolvidos pelo componente chamador.
     *
     * @param context Contexto temático da tela que apresenta o diálogo.
     * @param title   Texto usado como título.
     * @param message Texto usado como mensagem.
     */
    public static void show(Context context, CharSequence title, CharSequence message) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        int surfaceColor = ContextCompat.getColor(context, R.color.metric_card_background);
        assert builder.getBackground() != null;
        Drawable background = DrawableCompat.wrap(builder.getBackground()).mutate();
        DrawableCompat.setTint(background, surfaceColor);
        if (background instanceof MaterialShapeDrawable) {
            MaterialShapeDrawable shape = (MaterialShapeDrawable) background;
            shape.setStroke(context.getResources().getDisplayMetrics().density,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.metric_card_stroke)));
        }
        builder.setBackground(background);

        AlertDialog dialog = builder
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .create();

        dialog.setOnShowListener(ignored -> applyCardColors(context, dialog));
        dialog.show();
    }

    /**
     * Aplica os tons de cinza dos cartões e o som de clique ao botão de confirmação.
     *
     * @param context contexto usado para resolver as cores do aplicativo.
     * @param dialog  diálogo cujos componentes receberão as cores dos cartões.
     */
    private static void applyCardColors(Context context, AlertDialog dialog) {
        int textColor = ContextCompat.getColor(context, R.color.light_grey);

        TextView title = dialog.findViewById(com.google.android.material.R.id.alertTitle);
        if (title != null) {
            title.setTextColor(textColor);
            title.setTypeface(title.getTypeface(), Typeface.BOLD);
        }

        TextView message = dialog.findViewById(android.R.id.message);
        if (message != null) message.setTextColor(textColor);

        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(textColor);
            ViewUtils.setClickSoundListener(positiveButton, clickedView -> dialog.dismiss());
        }
    }
}
