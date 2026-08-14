/*
 **=================================================================================================
 **  @file    PidTask.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    agosto/2026
 **  @version 1.0
 **  @brief   Representa uma tarefa de leitura de um PID OBD-II, armazenando seu intervalo de
 **           execução e sua prioridade para as leituras.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.scheduler;

public class PidTask {
    // Intervalo mínimo entre duas execuções da tarefa, em milissegundos
    private final int interval;

    // Prioridade utilizada pelo scheduler para ordenar a execução da tarefa
    private final PidPriority priority;

    // Momento da última execução da tarefa, em milissegundos
    private long lastExecutionTime;

    /**
     * Cria uma tarefa de leitura de um PID OBD-II.
     *
     * @param interval Intervalo mínimo entre duas execuções da tarefa, em milissegundos.
     * @param priority Prioridade da tarefa.
     */
    public PidTask(int interval, PidPriority priority) {
        this.interval = interval;
        this.priority = priority;
    }

    /**
     * Verifica se a tarefa está pronta para ser executada.
     *
     * <p>A tarefa é considerada pronta quando o tempo decorrido desde sua última execução é igual
     * ou superior ao intervalo configurado.</p>
     *
     * @param currentTime Momento atual, em milissegundos.
     * @return {@code true} caso a tarefa esteja pronta para execução, {@code false} caso contrário.
     */
    public boolean isReady(long currentTime) {
        return currentTime - lastExecutionTime >= interval;
    }

    /**
     * Atualiza o momento da última execução da tarefa.
     *
     * @param executionTime Momento em que a tarefa foi executada, em milissegundos.
     */
    public void updateExecutionTime(long executionTime) {
        lastExecutionTime = executionTime;
    }

    /**
     * Retorna a prioridade definida para a tarefa.
     *
     * @return Prioridade da tarefa.
     */
    public PidPriority getPriority() {
        return priority;
    }
}