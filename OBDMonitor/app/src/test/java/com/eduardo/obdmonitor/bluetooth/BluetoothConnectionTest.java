/*
 **=================================================================================================
 **  @file    BluetoothConnectionTest.java
 **  @author  Eduardo Lorscheiter
 **  @class   BluetoothConnectionTest
 **  @date    02/11/2026
 **  @version 1.0
 **  @brief   Verifica fechamento, cancelamento e ausência de inversão de locks nos callbacks.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.bluetooth;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BluetoothConnectionTest {
    /**
     * Confirma que o listener recebe a sessão encerrada sem manter o lock do transporte.
     *
     * @throws Exception Caso a preparação dos recursos simulados falhe.
     */
    @Test
    public void publishesDisconnectOutsideTransportLock() throws Exception {
        RecordingListener listener = new RecordingListener();
        BluetoothConnection connection = activeConnection(listener);
        listener.onDisconnect = () -> assertFalse(Thread.holdsLock(connection));

        connection.disconnect();

        assertEquals(1, listener.disconnectCount);
        assertEquals(7L, listener.disconnectedId);
        assertNull(getField(connection, "input"));
        assertNull(getField(connection, "output"));
        assertFalse((Boolean) getField(connection, "connected"));
    }

    /**
     * Reproduz a ordem gerenciador-transporte enquanto outro callback aguarda o gerenciador.
     *
     * @throws Exception Caso alguma thread ou recurso simulado não possa ser preparado.
     */
    @Test(timeout = 10000)
    public void waitingManagerCallbackDoesNotBlockConcurrentDisconnect() throws Exception {
        Object managerLock = new Object();
        CountDownLatch callbackEntered = new CountDownLatch(1);
        RecordingListener listener = new RecordingListener();
        BluetoothConnection connection = activeConnection(listener);
        listener.onDisconnect = () -> {
            callbackEntered.countDown();
            //noinspection EmptySynchronizedStatement
            synchronized (managerLock) {
                // Simula o callback que precisa acessar o estado protegido do gerenciador.
            }
        };
        Thread transportThread = new Thread(connection::disconnect);
        Thread managerThread = new Thread(connection::disconnect);
        transportThread.setDaemon(true);
        managerThread.setDaemon(true);

        try {
            synchronized (managerLock) {
                transportThread.start();
                assertTrue(callbackEntered.await(2, TimeUnit.SECONDS));
                managerThread.start();
                managerThread.join(2000);
                assertFalse("O callback reteve o lock do transporte", managerThread.isAlive());
            }
        } finally {
            transportThread.join(2000);
            managerThread.join(2000);
        }

        assertFalse(transportThread.isAlive());
        assertEquals(1, listener.disconnectCount);
    }

    /**
     * Garante que chamadas repetidas não fechem recursos nem publiquem desconexão novamente.
     *
     * @throws Exception Caso os campos privados não possam ser preparados.
     */
    @Test
    public void closesStreamsOnlyOnce() throws Exception {
        RecordingListener listener = new RecordingListener();
        BluetoothConnection connection = activeConnection(listener);
        AtomicInteger closed = new AtomicInteger();
        setField(connection, "input", new ByteArrayInputStream(new byte[0]) {
            /**
             * Registra o fechamento do fluxo de entrada simulado.
             */
            @Override
            public void close() {
                closed.incrementAndGet();
            }
        });
        setField(connection, "output", new ByteArrayOutputStream() {
            /**
             * Registra o fechamento do fluxo de saída simulado.
             */
            @Override
            public void close() {
                closed.incrementAndGet();
            }
        });

        connection.disconnect();
        connection.disconnect();

        assertEquals(2, closed.get());
        assertEquals(1, listener.disconnectCount);
    }

    /**
     * Falhas no fechamento são acumuladas sem executar callbacks dentro da seção protegida.
     *
     * @throws Exception Caso não possa ser acessado.
     */
    @Test
    public void collectsCloseFailureWithoutCallingListener() throws Exception {
        RecordingListener listener = new RecordingListener();
        BluetoothConnection connection = activeConnection(listener);
        List<BluetoothConnectionError> errors = new ArrayList<>();
        Method closeResource = BluetoothConnection.class.getDeclaredMethod(
                "closeResource", Closeable.class, List.class);
        closeResource.setAccessible(true);
        Closeable failingResource = () -> {
            throw new IOException("simulated close failure");
        };

        synchronized (connection) {
            assertNull(closeResource.invoke(connection, failingResource, errors));
        }

        assertEquals(1, errors.size());
        assertEquals(BluetoothConnectionError.RESOURCE_CLOSE_FAILED, errors.get(0));
        assertEquals(0, listener.errorCount);
        assertEquals(0, listener.disconnectCount);
    }

    /**
     * O cancelamento invalida uma tentativa pendente sem anunciar uma sessão estabelecida.
     *
     * @throws Exception Caso os campos privados não possam ser preparados.
     */
    @Test
    public void cancelsPendingAttemptWithoutDisconnectCallback() throws Exception {
        RecordingListener listener = new RecordingListener();
        BluetoothConnection connection = new BluetoothConnection(null, listener);
        setField(connection, "connecting", true);
        setField(connection, "connectionId", 7L);

        connection.disconnect();

        assertFalse((Boolean) getField(connection, "connecting"));
        assertEquals(8L, getField(connection, "connectionId"));
        assertEquals(0, listener.disconnectCount);
    }

    /**
     * Prepara uma sessão em memória, sem dispositivo ou contexto Android.
     *
     * @param listener Listener usado para observar a desconexão.
     * @return Transporte com fluxos locais e sessão identificada.
     * @throws Exception Caso os campos privados não possam ser preparados.
     */
    private BluetoothConnection activeConnection(RecordingListener listener) throws Exception {
        BluetoothConnection connection = new BluetoothConnection(null, listener);
        setField(connection, "connected", true);
        setField(connection, "connectionId", 7L);
        setField(connection, "input", new ByteArrayInputStream(new byte[0]));
        setField(connection, "output", new ByteArrayOutputStream());
        return connection;
    }

    /**
     * Define somente o estado necessário para simular uma conexão sem hardware.
     *
     * @param connection Transporte sob teste.
     * @param name       Nome do campo privado.
     * @param value      Valor simulado.
     * @throws Exception Caso o campo não possa ser acessado.
     */
    private void setField(BluetoothConnection connection, String name, Object value) throws Exception {
        Field field = BluetoothConnection.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(connection, value);
    }

    /**
     * Consulta o estado interno para confirmar que os recursos foram liberados.
     *
     * @param connection Transporte sob teste.
     * @param name       Nome do campo privado.
     * @return Valor armazenado no campo.
     * @throws Exception Caso o campo não possa ser acessado.
     */
    private Object getField(BluetoothConnection connection, String name) throws Exception {
        Field field = BluetoothConnection.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(connection);
    }

    /**
     * Registra eventos sem acessar recursos Android.
     */
    private static class RecordingListener implements BluetoothConnectionListener {
        private int disconnectCount;
        private int errorCount;
        private long disconnectedId;
        private Runnable onDisconnect = () -> {
        };

        /**
         * Ignora conexões, pois os testes preparam a sessão em memória.
         *
         * @param connectionId Identificador da conexão estabelecida.
         */
        @Override
        public void onBluetoothConnectionConnected(long connectionId) {
        }

        /**
         * Registra a sessão encerrada e executa o callback configurado pelo teste.
         *
         * @param connectionId Identificador da sessão encerrada.
         */
        @Override
        public void onBluetoothConnectionDisconnected(long connectionId) {
            disconnectCount++;
            disconnectedId = connectionId;
            onDisconnect.run();
        }

        /**
         * Registra uma notificação de falha sem interpretar sua mensagem.
         *
         * @param connectionId Identificador da sessão que falhou.
         * @param message      Mensagem de falha.
         */
        @Override
        public void onBluetoothConnectionError(long connectionId, String message) {
            errorCount++;
        }
    }
}
