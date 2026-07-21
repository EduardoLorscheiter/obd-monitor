/*
 **===========================================================================
 **  @file    ViewUtils.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    Julho/2026
 **  @version 1.0
 **  @brief   OBDMonitor
 **===========================================================================
 */

package com.eduardo.obdmonitor.utils;

import android.app.Activity;
import android.util.TypedValue;

public class ViewUtils {
    public static int getThemeAttrColor(Activity activity, int attrId) {
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(attrId, typedValue, true);
        return typedValue.data;
    }
}