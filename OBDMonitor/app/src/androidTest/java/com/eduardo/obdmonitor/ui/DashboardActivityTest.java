/*
 **=================================================================================================
 **  @file    DashboardActivityTest.java
 **  @author  Eduardo Lorscheiter
 **  @class   DashboardActivityTest
 **  @date    02/11/2026
 **  @version 1.0
 **  @brief   Verifica indisponibilidade de tensão e liberação do alerta no ciclo de vida.
 **=================================================================================================
 */
package com.eduardo.obdmonitor.ui;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.eduardo.obdmonitor.R;
import com.eduardo.obdmonitor.utils.SettingsUtils;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.action.ViewActions.click;

@RunWith(AndroidJUnit4.class)
public class DashboardActivityTest {
    /**
     * Confirma cancelamento, callback atrasado sem reinício e retomada somente na tela ativa.
     */
    @Test
    public void blinkIsReleasedOnPauseAndDestroy() {
        AtomicReference<DashboardActivity> reference = new AtomicReference<>();
        try (ActivityScenario<DashboardActivity> scenario = ActivityScenario.launch(DashboardActivity.class)) {
            scenario.onActivity(activity -> {
                reference.set(activity);
                activity.onObdEngineRpmChanged(SettingsUtils.getBlinkStartRpm(activity));
                assertNotNull(animator(activity));
                activity.startActivity(new Intent(activity, SettingsActivity.class));
            });
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                DashboardActivity activity = reference.get();
                assertNull(animator(activity));
                assertEquals(0f, activity.findViewById(R.id.viewLimiter).getAlpha(), 0f);
                activity.onObdEngineRpmChanged(SettingsUtils.getBlinkStartRpm(activity));
                assertNull(animator(activity));
            });
            onView(withId(R.id.btnBack)).perform(click());
            scenario.onActivity(activity -> assertNotNull(animator(activity)));
        }
        assertNull(animator(reference.get()));
    }

    /**
     * Confirma que indisponibilidade de tensão não aparece como bateria com zero volts.
     */
    @Test
    public void invalidVoltageIsDisplayedAsUnavailable() {
        try (ActivityScenario<DashboardActivity> scenario = ActivityScenario.launch(DashboardActivity.class)) {
            scenario.onActivity(activity -> {
                activity.onObdControlModuleVoltageChanged(Double.NaN);
                TextView value = activity.findViewById(R.id.cardBattery).findViewById(R.id.txtValue);
                assertEquals(activity.getString(R.string.__), value.getText().toString());
            });
        }
    }

    /**
     * Consulta o animador para verificar sua liberação efetiva.
     *
     * @param activity Painel sob teste.
     * @return Animador ativo ou null após a liberação.
     */
    private ValueAnimator animator(DashboardActivity activity) {
        try {
            Field field = DashboardActivity.class.getDeclaredField("blinkAnimator");
            field.setAccessible(true);
            return (ValueAnimator) field.get(activity);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
