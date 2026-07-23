/*
 **===========================================================================
 **  @file    ViewUtils.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   OBDMonitor
 **===========================================================================
 */

package com.eduardo.obdmonitor.utils;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.View;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.eduardo.obdmonitor.R;

public class ViewUtils {
    public static int getThemeAttrColor(Activity activity, int attrId) {
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(attrId, typedValue, true);
        return typedValue.data;
    }

    /**
     * Atualiza o indicador visual de status.
     *
     * @param view      View utilizada como indicador.
     * @param connected Estado atual do indicador.
     */
    public static void setStatusIndicator(View view, boolean connected) {
        view.setBackgroundResource(connected ? R.drawable.status_on : R.drawable.status_off);
    }

    public static void applySwitchColors(Activity activity, SwitchCompat switchCompat) {
        ColorStateList thumbColor = ContextCompat.getColorStateList(activity, R.color.switch_thumb);
        ColorStateList trackColor = ContextCompat.getColorStateList(activity, R.color.switch_track);

        if (thumbColor != null) {
            switchCompat.setThumbTintList(thumbColor);
        }

        if (trackColor != null) {
            switchCompat.setTrackTintList(trackColor);
        }
    }
}