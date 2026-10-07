/*
 **=================================================================================================
 **  @file    ObdScheduler.java
 **  @author  Eduardo Lorscheiter
 **  @class   ObdScheduler
 **  @date    09/08/2026
 **  @version 1.0
 **  @brief   Gerencia o agendamento das leituras dos PIDs OBD-II de acordo com suas prioridades
 **           e respectivos intervalos de atualização.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ObdScheduler {
    /**
     * Lista de tarefas responsáveis pelo agendamento das leituras dos PIDs OBD-II.
     */
    private final List<PidTask> tasks = new ArrayList<>();

    /**
     * Adiciona uma tarefa ao agendador de leituras dos PIDs OBD-II.
     *
     * @param task Tarefa de leitura do PID OBD-II a ser adicionada.
     */
    public void addTask(PidTask task) {
        if (task == null)
            return;

        tasks.add(task);
    }

    /**
     * Retorna as tarefas que estão prontas para execução no momento informado.
     *
     * <p>As tarefas são ordenadas de acordo com sua prioridade, sendo as tarefas de maior
     * prioridade posicionadas primeiro. Tarefas com a mesma prioridade mantêm a ordem em que foram
     * adicionadas ao agendador.</p>
     *
     * @param currentTime Momento atual em milissegundos.
     * @return Lista das tarefas prontas para execução, ordenadas por prioridade.
     */
    public List<PidTask> getReadyTasks(long currentTime) {
        List<PidTask> readyTasks = new ArrayList<>();

        for (PidTask task : tasks) {
            if (task.isSupported() && task.isReady(currentTime))
                readyTasks.add(task);
        }

        readyTasks.sort(Comparator.comparingInt(task -> task.getPriority().getValue()));

        return readyTasks;
    }

    /**
     * Atualiza o momento da última execução de uma tarefa.
     *
     * <p>O momento informado é utilizado pela tarefa para determinar quando ela estará pronta para
     * uma nova execução.</p>
     *
     * @param task          Tarefa que teve sua execução realizada.
     * @param executionTime Momento da execução em milissegundos.
     */
    public void updateTaskExecution(PidTask task, long executionTime) {
        if (task == null)
            return;

        task.updateExecutionTime(executionTime);
    }
}