/*
 **===========================================================================
 **  @file    ObdCommands.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    Julho/2026
 **  @version 1.0
 **  @brief   Comandos e PIDs (Parameter IDs) do protocolo OBD-II.
 **===========================================================================
 */

package com.eduardo.obdmonitor.obd;

public final class ObdCommands {
    /**
     * Impede a criação de instâncias da classe.
     */
    private ObdCommands() {
    }

    /**
     * Solicita ao veículo os PIDs suportados no intervalo de 0x01 a 0x20.
     *
     * <p>Utilizado para validar a comunicação com a ECU.</p>
     */
    public static final String SUPPORTED_PIDS_01_20 = "0100";

    /**
     * Rotação do motor (RPM).
     */
    public static final String ENGINE_RPM = "010C";

    /**
     * Velocidade do veículo.
     */
    public static final String VEHICLE_SPEED = "010D";

    /**
     * Temperatura do líquido de arrefecimento.
     */
    public static final String ENGINE_COOLANT_TEMPERATURE = "0105";

    /**
     * Temperatura do ar admitido.
     */
    public static final String INTAKE_AIR_TEMPERATURE = "010F";

    /**
     * Pressão absoluta do coletor de admissão (MAP).
     */
    public static final String INTAKE_MANIFOLD_PRESSURE = "010B";

    /**
     * Posição absoluta da borboleta de aceleração.
     */
    public static final String THROTTLE_POSITION = "0111";

    /**
     * Tensão do módulo de controle.
     */
    public static final String CONTROL_MODULE_VOLTAGE = "0142";
}