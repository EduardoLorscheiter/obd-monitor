/*
 **=================================================================================================
 **  @file    VoltageMonitoringTest.java
 **  @author  Eduardo Lorscheiter
 **  @class   VoltageMonitoringTest
 **  @date    02/11/2026
 **  @version 1.0
 **  @brief   Verifica descoberta e leitura de tensão com transporte serial simulado.
 **=================================================================================================
 */
package com.eduardo.obdmonitor.elm327;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.eduardo.obdmonitor.bluetooth.BluetoothManager;
import com.eduardo.obdmonitor.obd.scheduler.PidTask;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class VoltageMonitoringTest {
    /**
     * Consulta 0140 quando anunciado e usa 0142 antes de recorrer à tensão do adaptador.
     */
    @Test
    public void discoversVoltageSupportAndFallsBackAfterInvalidPid() throws Exception {
        Session session = new Session();
        session.responses.put("01 00", "410000000001");
        session.responses.put("01 20", "412000000001");
        session.responses.put("01 40", "414040000000");
        assertEquals(true, invoke(session.manager, "discoverSupportedPids"));
        assertTrue((Boolean) get(session.manager, "voltagePidSupported"));
        assertTrue(session.requests.contains("01 40"));
        session.responses.put("01 42", "414236B0");
        assertEquals(14.0, (Double) invoke(session.manager, "readControlModuleVoltage"), 0.0001);
        assertFalse(session.requests.contains("ATRV"));
        session.responses.put("01 42", "NO DATA");
        session.responses.put("ATRV", "12.6V");
        assertEquals(12.6, (Double) invoke(session.manager, "readControlModuleVoltage"), 0.0001);
        assertTrue(session.requests.contains("ATRV"));
    }

    /**
     * Sem blocos estendidos, a tarefa continua agendada e consulta somente ATRV.
     */
    @Test
    public void schedulesAdapterVoltageWithoutEcuPidSupport() throws Exception {
        Session session = new Session();
        session.responses.put("01 00", "410000000000");
        assertEquals(true, invoke(session.manager, "discoverSupportedPids"));
        assertTrue(((PidTask) get(session.manager, "voltageTask")).isSupported());
        assertFalse((Boolean) get(session.manager, "voltagePidSupported"));
        session.responses.put("ATRV", "13.4V");
        assertEquals(13.4, (Double) invoke(session.manager, "readControlModuleVoltage"), 0.0001);
        assertFalse(session.requests.contains("01 42"));
        assertFalse(session.requests.contains("01 40"));
        session.responses.put("ATRV", "?");
        assertTrue(Double.isNaN((Double) invoke(session.manager, "readControlModuleVoltage")));
    }

    /**
     * Transporte em memória, isolado dos singletons usados pelas Activities.
     */
    @SuppressWarnings("JUnitTestClassNamingConvention")
    private static class Session {
        private final Elm327Manager manager;
        private final Map<String, String> responses = new HashMap<>();
        private final List<String> requests = new ArrayList<>();

        /**
         * Monta o gerenciador real com fluxos locais, sem abrir um socket Bluetooth.
         *
         * @throws Exception Caso os campos internos não possam ser preparados.
         */
        private Session() throws Exception {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Constructor<BluetoothManager> bluetoothConstructor = BluetoothManager.class.getDeclaredConstructor(Context.class);
            bluetoothConstructor.setAccessible(true);
            BluetoothManager bluetooth = bluetoothConstructor.newInstance(context);
            Object connection = get(bluetooth, "connection");
            set(connection, "connected", true);
            set(connection, "output", new OutputStream() {
                /**
                 * Aceita bytes isolados; os comandos usam o overload de array.
                 * @param value Byte isolado ignorado.
                 */
                @Override
                public void write(int value) {
                }

                /**
                 * Entrega a resposta configurada ao transporte real.
                 * @param bytes Comando enviado pelo gerenciador.
                 */
                @Override
                public void write(byte[] bytes) {
                    String command = new String(bytes, StandardCharsets.US_ASCII).trim();
                    requests.add(command);
                    try {
                        set(connection, "input", new ByteArrayInputStream(
                                (responses.getOrDefault(command, "NO DATA") + ">").getBytes(StandardCharsets.US_ASCII)));
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                }
            });
            Constructor<Elm327Manager> constructor = Elm327Manager.class.getDeclaredConstructor(Context.class);
            constructor.setAccessible(true);
            manager = constructor.newInstance(context);
            set(manager, "bluetoothManager", bluetooth);
        }
    }

    /**
     * Lê um campo da instância sob teste.
     *
     * @param object Instância sob teste.
     * @param name   Nome do campo.
     * @return Valor do campo.
     * @throws Exception Caso o campo não possa ser acessado.
     */
    private static Object get(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    /**
     * Define um recurso simulado no objeto sob teste.
     *
     * @param object Instância sob teste.
     * @param name   Nome do campo.
     * @param value  Valor simulado.
     * @throws Exception Caso o campo não possa ser acessado.
     */
    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    /**
     * Executa uma etapa real sem iniciar threads de monitoramento.
     *
     * @param object Instância sob teste.
     * @param name   Nome da etapa.
     * @return Resultado da etapa.
     * @throws Exception Caso a etapa falhe.
     */
    private static Object invoke(Object object, String name) throws Exception {
        Method method = object.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(object);
    }
}
