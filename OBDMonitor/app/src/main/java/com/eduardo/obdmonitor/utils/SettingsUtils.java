/*
 **=================================================================================================
 **  @file    SettingsUtils.java
 **  @author  Eduardo Lorscheiter
 **  @class   SettingsUtils
 **  @date    28/09/2026
 **  @version 1.0
 **  @brief   Centraliza o gerenciamento das configurações do aplicativo.
 **  @details Responsável por ler, gravar, validar e aplicar as preferências do aplicativo. As
 **           configurações são persistidas por meio de SharedPreferences.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class SettingsUtils {
    /**
     * Nome do arquivo de preferências utilizado pelo aplicativo.
     */
    private static final String APP_PREFERENCES = "app_preferences";
    /**
     * Chave utilizada para armazenar o índice do idioma selecionado.
     */
    private static final String LANGUAGE_INDEX = "language_index";
    /**
     * Chave utilizada para armazenar o limite máximo de RPM do veículo.
     */
    private static final String VEHICLE_MAX_RPM = "vehicle_max_rpm";
    /**
     * Chave utilizada para armazenar o RPM inicial da indicação visual de alerta.
     */
    private static final String RPM_BLINK_START = "rpm_blink_start";

    /**
     * Códigos dos idiomas suportados pelo aplicativo.
     */
    private static final String[] LANGUAGE_CODES = {"pt", "en"};

    /**
     * Valor mínimo permitido para o limite máximo de RPM.
     */
    public static final int MIN_MAX_RPM = 1000;
    /**
     * Valor máximo permitido para o limite máximo de RPM.
     */
    public static final int MAX_MAX_RPM = 20000;
    /**
     * Valor padrão utilizado para o limite máximo de RPM.
     */
    public static final int DEFAULT_MAX_RPM = 7000;
    /**
     * Valor mínimo permitido para o RPM inicial da indicação visual de alerta.
     */
    public static final int MIN_BLINK_START_RPM = 1000;
    /**
     * Aplica o idioma configurado ao contexto informado.
     *
     * <p>O idioma selecionado é obtido das preferências e utilizado para criar um novo contexto
     * com a configuração de idioma e direção de layout correspondentes.</p>
     *
     * @param context Contexto original da aplicação.
     * @return Novo contexto com o idioma configurado.
     */
    public static Context applyLocale(Context context) {
        SharedPreferences prefs = getPreferences(context);
        int languageIndex = prefs.getInt(LANGUAGE_INDEX, 0);
        String selectedLang = LANGUAGE_CODES[
                Math.max(0, Math.min(languageIndex, LANGUAGE_CODES.length - 1))];

        Locale locale = Locale.forLanguageTag(selectedLang);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        return context.createConfigurationContext(config);
    }

    /**
     * Armazena o índice do idioma selecionado.
     *
     * <p>O índice informado é limitado ao intervalo de idiomas suportados pelo aplicativo.</p>
     *
     * @param context       Contexto utilizado para acessar as preferências.
     * @param languageIndex Índice do idioma a ser armazenado.
     */
    public static void setLanguageIndex(Context context, int languageIndex) {
        SharedPreferences prefs = getPreferences(context);
        int validIndex = Math.max(0, Math.min(languageIndex, LANGUAGE_CODES.length - 1));
        prefs.edit().putInt(LANGUAGE_INDEX, validIndex).apply();
    }

    /**
     * Retorna o índice do idioma configurado.
     *
     * @param context Contexto utilizado para acessar as preferências.
     * @return Índice do idioma configurado ou {@code 0} caso nenhuma configuração esteja
     * disponível.
     */
    public static int getLanguageIndex(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getInt(LANGUAGE_INDEX, 0);
    }

    /**
     * Salva o limite máximo de RPM configurado para o veículo.
     *
     * @param context Contexto utilizado para acessar as preferências.
     * @param maxRpm  Limite máximo de RPM.
     */
    public static void setMaxRpm(Context context, int maxRpm) {
        int validatedMaxRpm = Math.max(MIN_MAX_RPM, Math.min(maxRpm, MAX_MAX_RPM));
        SharedPreferences prefs = getPreferences(context);
        prefs.edit().putInt(VEHICLE_MAX_RPM, validatedMaxRpm).apply();
    }

    /**
     * Retorna o limite máximo de RPM configurado para o veículo.
     *
     * @param context Contexto utilizado para acessar as preferências.
     * @return Limite de RPM configurado ou o valor padrão.
     */
    public static int getMaxRpm(Context context) {
        SharedPreferences prefs = getPreferences(context);
        int maxRpm = prefs.getInt(VEHICLE_MAX_RPM, DEFAULT_MAX_RPM);
        return Math.max(MIN_MAX_RPM, Math.min(maxRpm, MAX_MAX_RPM));
    }

    /**
     * Salva o RPM a partir do qual deve ser iniciada a indicação visual de aproximação do limite
     * máximo configurado.
     *
     * <p>O valor informado é limitado entre {@link #MIN_BLINK_START_RPM} e o limite máximo de RPM
     * atualmente configurado.</p>
     *
     * @param context Contexto utilizado para acessar as preferências.
     * @param rpm     RPM inicial da indicação visual de alerta.
     */
    public static void setBlinkStartRpm(Context context, int rpm) {
        int upperBound = getMaxRpm(context);
        int validatedRpm = Math.max(MIN_BLINK_START_RPM, Math.min(rpm, upperBound));
        SharedPreferences prefs = getPreferences(context);
        prefs.edit().putInt(RPM_BLINK_START, validatedRpm).apply();
    }

    /**
     * Retorna o RPM a partir do qual deve ser iniciada a indicação visual de aproximação do limite
     * máximo configurado.
     *
     * <p>Caso nenhum valor tenha sido configurado, o valor padrão corresponde à metade do limite
     * máximo de RPM definido para o veículo.</p>
     * <p>O valor retornado é limitado entre {@link #MIN_BLINK_START_RPM} e o limite máximo de RPM
     * configurado.</p>
     *
     * @param context Contexto utilizado para acessar as preferências.
     * @return RPM inicial da indicação visual de alerta.
     */
    public static int getBlinkStartRpm(Context context) {
        SharedPreferences prefs = getPreferences(context);
        int maxRpm = getMaxRpm(context);
        int defaultRpm = maxRpm / 2;
        int blinkRpm = prefs.getInt(RPM_BLINK_START, defaultRpm);
        return Math.max(MIN_BLINK_START_RPM, Math.min(blinkRpm, maxRpm));
    }

    /**
     * Retorna as preferências utilizadas pelo aplicativo.
     *
     * @param context Contexto utilizado para acessar as preferências.
     * @return Instância de {@link SharedPreferences} utilizada para armazenar as configurações do
     * aplicativo.
     */
    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }
}
