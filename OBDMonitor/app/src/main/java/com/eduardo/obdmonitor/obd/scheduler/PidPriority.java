/*
 **=================================================================================================
 **  @file    PidPriority.java
 **  @author  Eduardo Lorscheiter
 **  @class   PidPriority
 **  @date    09/08/2026
 **  @version 1.0
 **  @brief   Define os níveis de prioridade utilizados no agendamento das leituras dos PIDs OBD-II.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.scheduler;

public enum PidPriority {
    HIGH(0),
    MEDIUM(1),
    LOW(2);

    private final int value;

    PidPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}