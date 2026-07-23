/*
 **===========================================================================
 **  @file    ObdCommands.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Define os comandos OBD-II utilizados para consulta de parâmetros
 **           da ECU (Engine Control Unit).
 **  @details Os comandos desta classe utilizam o Modo de Serviço 01,
 **           responsável pela leitura de dados em tempo real do veículo.
 **===========================================================================
 */

package com.eduardo.obdmonitor.obd;

public final class ObdCommands {
    /**
     * Classe utilitária. Não permite instanciação.
     */
    private ObdCommands() {
    }

    /**
     * Solicita os PIDs suportados pela ECU no intervalo de 0x01 a 0x20.
     *
     * <p>Normalmente utilizado para validar a comunicação com a ECU
     * antes da leitura dos demais parâmetros.</p>
     *
     * <p>Resposta: 41 00 AA BB CC DD</p>
     * <p>Cada bit representa um PID suportado.</p>
     */
    public static final String SUPPORTED_PIDS_01_20 = "01 00";

    /**
     * PID 0x0C - Rotação do motor (RPM).
     *
     * <p>Resposta: 41 0C A B</p>
     * <p>Fórmula: RPM = ((A × 256) + B) / 4</p>
     */
    public static final String ENGINE_RPM = "01 0C";

    /**
     * PID 0x0D - Velocidade do veículo.
     *
     * <p>Resposta: 41 0D A</p>
     * <p>Fórmula: Velocidade (km/h) = A</p>
     */
    public static final String VEHICLE_SPEED = "01 0D";

    /**
     * PID 0x05 - Temperatura do líquido de arrefecimento.
     *
     * <p>Resposta: 41 05 A</p>
     * <p>Fórmula: Temperatura (°C) = A - 40</p>
     */
    public static final String ENGINE_COOLANT_TEMPERATURE = "01 05";

    /**
     * PID 0x0F - Temperatura do ar admitido.
     *
     * <p>Resposta: 41 0F A</p>
     * <p>Fórmula: Temperatura (°C) = A - 40</p>
     */
    public static final String INTAKE_AIR_TEMPERATURE = "01 0F";

    /**
     * PID 0x0B - Pressão absoluta do coletor de admissão (MAP).
     *
     * <p>Resposta: 41 0B A</p>
     * <p>Fórmula: Pressão (kPa) = A</p>
     */
    public static final String INTAKE_MANIFOLD_PRESSURE = "01 0B";

    /**
     * PID 0x11 - Posição absoluta da borboleta de aceleração.
     *
     * <p>Resposta: 41 11 A</p>
     * <p>Fórmula: Posição (%) = (A × 100) / 255</p>
     */
    public static final String THROTTLE_POSITION = "01 11";

    /**
     * PID 0x42 - Tensão de alimentação do módulo de controle (ECU).
     *
     * <p>Resposta: 41 42 A B</p>
     * <p>Fórmula: Tensão (V) = ((A × 256) + B) / 1000</p>
     */
    public static final String CONTROL_MODULE_VOLTAGE = "01 42";
}