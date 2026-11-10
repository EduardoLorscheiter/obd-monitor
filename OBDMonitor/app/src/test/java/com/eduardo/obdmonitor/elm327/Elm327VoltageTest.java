/*
 **=================================================================================================
 **  @file    Elm327VoltageTest.java
 **  @author  Eduardo Lorscheiter
 **  @class   Elm327VoltageTest
 **  @date    02/11/2026
 **  @version 1.0
 **  @brief   Verifica o parsing da tensão de alimentação do adaptador ELM327.
 **=================================================================================================
 */
package com.eduardo.obdmonitor.elm327;

import org.junit.Test;

import static org.junit.Assert.*;

public class Elm327VoltageTest {
    /**
     * Aceita o formato decimal com unidade e os limites de apresentação do adaptador.
     */
    @Test
    public void acceptsAdapterVoltage() {
        assertEquals(12.6, Elm327Commands.parseVoltage("12.6V"), 0.0001);
        assertEquals(14.1, Elm327Commands.parseVoltage(" 14.1 V "), 0.0001);
        assertEquals(0.0, Elm327Commands.parseVoltage("0.0V"), 0.0001);
        assertEquals(99.9, Elm327Commands.parseVoltage("99.9V"), 0.0001);
    }

    /**
     * Falhas e valores sem unidade não se transformam em medições plausíveis.
     */
    @Test
    public void rejectsInvalidAdapterVoltage() {
        for (String response : new String[]{null, "", "NO DATA", "?", "12.6", "NaNV", "InfinityV", "-1.0V", "100.0V", "12.6V OK"})
            assertTrue(Double.isNaN(Elm327Commands.parseVoltage(response)));
    }
}
