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
import androidx.lifecycle.Lifecycle;

import com.eduardo.obdmonitor.R;
import com.eduardo.obdmonitor.model.Device;
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
                activity.onElm327EcuStatusChanged(true);
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
            onView(withId(R.id.btnConnection)).check(matches(isDisplayed()));
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> {
                activity.onElm327EcuStatusChanged(true);
                activity.onObdEngineRpmChanged(SettingsUtils.getBlinkStartRpm(activity));
                assertNotNull(animator(activity));
            });
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
                activity.onElm327EcuStatusChanged(true);
                activity.onObdControlModuleVoltageChanged(Double.NaN);
                TextView value = activity.findViewById(R.id.cardBattery).findViewById(R.id.txtValue);
                assertEquals(activity.getString(R.string.__), value.getText().toString());
            });
        }
    }

    /**
     * O evento Bluetooth limpa o gerenciador e o painel e impede o retorno de dados atrasados.
     */
    @Test
    public void bluetoothDisconnectionClearsObdAndDashboard() {
        try (ActivityScenario<DashboardActivity> scenario = ActivityScenario.launch(DashboardActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    // Prepara somente o estado OBD, sem abrir transporte nem iniciar threads de I/O.
                    for (String name : new String[]{"monitoring", "bluetoothConnected", "ecuConnected"}) {
                        Field field = activity.elm327Manager.getClass().getDeclaredField(name);
                        field.setAccessible(true);
                        field.setBoolean(activity.elm327Manager, true);
                    }
                    activity.onElm327BluetoothStatusChanged(true);
                    activity.onElm327EcuStatusChanged(true);
                    activity.onElm327ProtocolChanged("ISO 15765-4 CAN");
                    activity.onObdEngineRpmChanged(SettingsUtils.getBlinkStartRpm(activity));
                    activity.onObdVehicleSpeedChanged(90);
                    activity.onObdEngineCoolantTemperatureChanged(110);
                    activity.onObdControlModuleVoltageChanged(14.0);
                    activity.onObdFuelLevelChanged(75);
                    assertNotNull(animator(activity));

                    Thread pendingData = new Thread(() -> activity.onObdVehicleSpeedChanged(99));
                    pendingData.start();
                    pendingData.join(2000);
                    assertFalse(pendingData.isAlive());
                    activity.onBluetoothManagerDisconnected(new Device("ELM327", "00:11:22:33:44:55"), "");

                    for (String name : new String[]{"monitoring", "bluetoothConnected", "ecuConnected"}) {
                        Field field = activity.elm327Manager.getClass().getDeclaredField(name);
                        field.setAccessible(true);
                        assertFalse(field.getBoolean(activity.elm327Manager));
                    }
                    activity.onObdEngineRpmChanged(SettingsUtils.getMaxRpm(activity));
                    activity.onObdVehicleSpeedChanged(120);
                    assertNull(animator(activity));
                    assertEquals(0f, activity.findViewById(R.id.viewLimiter).getAlpha(), 0f);
                    for (int id : new int[]{R.id.cardSpeed, R.id.cardThrottle, R.id.cardMap,
                            R.id.cardCoolant, R.id.cardIntake, R.id.cardBattery}) {
                        TextView value = activity.findViewById(id).findViewById(R.id.txtValue);
                        assertEquals(activity.getString(R.string.__), value.getText().toString());
                    }
                    TextView protocol = activity.findViewById(R.id.txtProtocol);
                    assertEquals(activity.getString(R.string.__), protocol.getText().toString());
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                TextView value = activity.findViewById(R.id.cardSpeed).findViewById(R.id.txtValue);
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
