/*
 **=================================================================================================
 **  @file    SupportedPidsCommandTest.java
 **  @author  Eduardo Lorscheiter
 **  @class   SupportedPidsCommandTest
 **  @date    25/10/2026
 **  @version 1.0
 **  @brief   Verifica a interpretação da máscara de PIDs suportados pela ECU.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.obd.commands;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SupportedPidsCommandTest {
    /**
     * Confirma que o PID 0x20 é verificado como identificador, sem o serviço da requisição.
     */
    @Test
    public void reportsPid20AsSupportedWhenItsMaskBitIsSet() {
        SupportedPidsCommand command = new SupportedPidsCommand(
                SupportedPidsCommand.SUPPORTED_PIDS_00_PID
        );
        Long supportedPids = command.parse("410000000001");

        assertNotNull(supportedPids);
        assertTrue(command.isPidSupported(supportedPids, "20"));
    }

    /**
     * Confirma que o PID 0x20 é considerado não suportado quando seu bit está limpo.
     */
    @Test
    public void reportsPid20AsUnsupportedWhenItsMaskBitIsClear() {
        SupportedPidsCommand command = new SupportedPidsCommand(
                SupportedPidsCommand.SUPPORTED_PIDS_00_PID
        );
        Long supportedPids = command.parse("410000000000");

        assertNotNull(supportedPids);
        assertFalse(command.isPidSupported(supportedPids, "20"));
    }
}
