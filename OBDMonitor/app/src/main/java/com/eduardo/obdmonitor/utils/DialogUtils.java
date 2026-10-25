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
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        int surfaceColor = ViewUtils.getThemeAttrColor(
                context,
                com.google.android.material.R.attr.colorSurface
        );
        assert builder.getBackground() != null;
        Drawable background = DrawableCompat.wrap(builder.getBackground()).mutate();
        DrawableCompat.setTint(background, surfaceColor);
        builder.setBackground(background);

        AlertDialog dialog = builder
                .setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(android.R.string.ok, null)
                .create();

        dialog.setOnShowListener(ignored -> applyThemeColors(context, dialog));
        dialog.show();
    }

    /**
     * Aplica ao título, à mensagem e ao botão de confirmação as cores do tema atual.
     *
     * @param context contexto temático usado para resolver as cores do aplicativo.
     * @param dialog diálogo cujos componentes receberão as cores correspondentes ao tema.
     */
    private static void applyThemeColors(Context context, AlertDialog dialog) {
        int textColor = ViewUtils.getThemeAttrColor(
                context,
                com.google.android.material.R.attr.colorOnSurface
        );
        int actionColor = ViewUtils.getThemeAttrColor(
                context,
                com.google.android.material.R.attr.colorSecondary
        );

        TextView title = dialog.findViewById(com.google.android.material.R.id.alertTitle);
        if (title != null) {
            title.setTextColor(textColor);
            title.setTypeface(title.getTypeface(), Typeface.BOLD);
        }

        TextView message = dialog.findViewById(android.R.id.message);
        if (message != null) message.setTextColor(textColor);

        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) positiveButton.setTextColor(actionColor);
    }
}
