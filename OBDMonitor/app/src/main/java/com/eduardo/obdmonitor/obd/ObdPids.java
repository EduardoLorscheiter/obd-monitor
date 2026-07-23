/*
 **===========================================================================
 **  @file    ObdPids.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Define os Parameter IDs (PIDs) utilizados pelo protocolo OBD-II.
 **  @details Cada PID identifica um parâmetro da ECU que pode ser consultado
 **           através do Modo de Serviço 01 do protocolo OBD-II.
 **===========================================================================
 */

package com.eduardo.obdmonitor.obd;

public final class ObdPids {
    /**
     * Classe utilitária. Não permite instanciação.
     */
    private ObdPids() {
    }

    /**
     * PID {@code 0x00}.
     *
     * <p>Indica os PIDs suportados pela ECU no intervalo de 0x01 a 0x20.</p>
     */
    static final String SUPPORTED_PIDS_01_20 = "00";

    /**
     * PID {@code 0x0C}.
     *
     * <p>Rotação do motor (RPM).</p>
     */
    static final String ENGINE_RPM = "0C";

    /**
     * PID {@code 0x0D}.
     *
     * <p>Velocidade do veículo.</p>
     */
    static final String VEHICLE_SPEED = "0D";

    /**
     * PID {@code 0x05}.
     *
     * <p>Temperatura do líquido de arrefecimento.</p>
     */
    static final String ENGINE_COOLANT_TEMPERATURE = "05";

    /**
     * PID {@code 0x0F}.
     *
     * <p>Temperatura do ar admitido.</p>
     */
    static final String INTAKE_AIR_TEMPERATURE = "0F";

    /**
     * PID {@code 0x0B}.
     *
     * <p>Pressão absoluta do coletor de admissão (MAP).</p>
     */
    static final String INTAKE_MANIFOLD_PRESSURE = "0B";

    /**
     * PID {@code 0x11}.
     *
     * <p>Posição absoluta da borboleta de aceleração.</p>
     */
    static final String THROTTLE_POSITION = "11";

    /**
     * PID {@code 0x42}.
     *
     * <p>Tensão de alimentação do módulo de controle (ECU).</p>
     */
    static final String CONTROL_MODULE_VOLTAGE = "42";
}