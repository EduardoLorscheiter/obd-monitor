/*
 **=================================================================================================
 **  @file    VoltageCommandTest.java
 **  @author  Eduardo Lorscheiter
 **  @class   VoltageCommandTest
 **  @date    02/11/2026
 **  @version 1.0
 **  @brief   Verifica fórmula, limites e indisponibilidade da leitura de tensão da ECU.
 **=================================================================================================
 */
package com.eduardo.obdmonitor.obd.commands;

import org.junit.Test;

import static org.junit.Assert.*;

public class VoltageCommandTest {
    /**
     * Converte os dois bytes em milivolts, incluindo os limites do PID.
     */
    @Test
    public void decodesVoltageAndLimits() {
        VoltageCommand command = new VoltageCommand();
        assertEquals(14.0, command.parse("414236B0"), 0.0001);
        assertEquals(0.0, command.parse("41420000"), 0.0001);
        assertEquals(65.535, command.parse("4142FFFF"), 0.0001);
    }

    /**
     * Respostas inválidas não reapresentam a leitura antiga como sucesso e permitem ATRV.
     */
    @Test
    public void invalidResponseDoesNotReusePreviousVoltage() {
        VoltageCommand command = new VoltageCommand();
        for (String response : new String[]{null, "NO DATA", "414236", "410C36B0", "4142GGGG", "4142-100"}) {
            command.parse("414236B0");
            assertTrue(Double.isNaN(command.parse(response)));
        }
    }

    /**
     * O bloco 0140 cobre o PID 42; a máscara básica não pode anunciar esse suporte.
     */
    @Test
    public void usesCorrectSupportBlockForVoltage() {
        SupportedPidsCommand base = new SupportedPidsCommand("00");
        SupportedPidsCommand higher = new SupportedPidsCommand("40");
        assertFalse(base.isPidSupported(0xFFFFFFFFL, "42"));
        assertTrue(higher.isPidSupported(higher.parse("414040000000"), "42"));
        assertFalse(higher.isPidSupported(higher.parse("414000000000"), "42"));
    }
}
