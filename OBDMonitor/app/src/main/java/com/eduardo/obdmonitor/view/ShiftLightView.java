/*
 **=================================================================================================
 **  @file    ShiftLightView.java
 **  @author  Eduardo Lorscheiter
 **  @class   Personal
 **  @date    julho/2026
 **  @version 1.0
 **  @brief   Componente gráfico responsável por exibir uma Shift Light dinâmica baseada na rotação
 **           do motor (RPM).
 **  @details A barra é composta por LEDs desenhados em Canvas, que acendem progressivamente
 **           conforme o aumento da rotação do motor. Também exibe o valor da rotação atual,
 **           sincronizada com a barra de LEDs. Além disso, os LEDs possuem altura crescente,
 **           proporcionando um efeito visual semelhante aos painéis de gerenciamento eletrônico de
 **           motores (ECUs).
 **=================================================================================================
 */

package com.eduardo.obdmonitor.view;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.eduardo.obdmonitor.R;
import com.eduardo.obdmonitor.utils.ViewUtils;

public class ShiftLightView extends View {
    // LED de referência para alinhar o RPM atual
    private static final int RPM_REFERENCE_LED = 0;

    // Altura reservada para o valor do RPM em relação ao maior LED
    private static final float RPM_VALUE_HEIGHT_FACTOR = 0.70f;

    // Altura reservada para a unidade do RPM em relação ao valor do RPM
    private static final float RPM_UNIT_HEIGHT_FACTOR = 0.45f;

    // Proporção do primeiro LED em relação a altura total
    private static final float MIN_LED_HEIGHT_FACTOR = 0.05f;

    // Proporção do último LED em relação a altura total
    private static final float MAX_LED_HEIGHT_FACTOR = 0.95f;

    // Espaçamento entre os LEDs, em pixels
    private static final float LED_SPACING = 2f;

    // Percentuais de transição das cores dos LEDs
    final float LED_PERCENT_GREEN_LIME = 0.30f;
    final float LED_PERCENT_LIME_YELLOW = 0.40f;
    final float LED_PERCENT_YELLOW_ORANGE = 0.65f;

    // Curva dos LEDs
    private static final float LED_CURVE = 3.84f;

    // Raio utilizado para arredondar os cantos dos LEDs
    private static final float CORNER_RADIUS = 8f;

    // Altura reservada para a escala de RPM
    private static final float RPM_SCALE_HEIGHT_FACTOR = 0.18f;

    // Tamanho do texto da escala de RPM
    private static final float RPM_SCALE_TEXT_SIZE = 34f;

    // Cores dos LEDs
    private int ledOff;
    private int ledGreen;
    private int ledLime;
    private int ledYellow;
    private int ledOrange;
    private int ledRed;

    // Cores pré-calculadas de cada LED da barra
    private int[] ledColors;

    // Alturas pré-calculadas de cada LED da barra
    private float[] ledHeightsFactors;

    // Objetos utilizados para desenhar os componentes da Shift Light
    private final Paint rpmValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rpmUnitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ledsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rpmScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Interpolador suave para mudanças de cores entre os estágios da Shift Light
    // Obs.: Evita transições bruscas entre os LEDs
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    // Rotação máxima considerada para acendimento completo da barra
    private int maxRpm = 7000;

    // Quantidade total de LEDs exibidos na barra
    private int ledCount = 60;

    // Rotação do motor (RPM) atual
    private int rpm = 0;

    public ShiftLightView(Context context) {
        super(context);
        init();
    }

    public ShiftLightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShiftLightView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Inicializa os recursos gráficos utilizados pela Shift Light.
     *
     * <p>Configura os objetos de pintura, carrega as cores dos LEDs e do texto a partir dos
     * recursos da aplicação e realiza os pré-cálculos necessários para as cores e alturas dos LEDs.
     * </p>
     */
    private void init() {
        rpmValuePaint.setColor(getContext().getColor(R.color.automotive_green));
        rpmValuePaint.setFakeBoldText(true);

        int textColor = ViewUtils.getThemeAttrColor(getContext(), com.google.android.material.R.attr.colorOnPrimary);
        rpmUnitPaint.setColor(textColor);

        rpmScalePaint.setColor(textColor);
        rpmScalePaint.setTextSize(RPM_SCALE_TEXT_SIZE);
        rpmScalePaint.setTextAlign(Paint.Align.CENTER);

        ledOff = getContext().getColor(R.color.led_off);
        ledGreen = getContext().getColor(R.color.led_green);
        ledLime = getContext().getColor(R.color.led_lime);
        ledYellow = getContext().getColor(R.color.led_yellow);
        ledOrange = getContext().getColor(R.color.led_orange);
        ledRed = getContext().getColor(R.color.led_red);
        calculateLedColors();
        calculateLedHeightsFactors();
    }

    /**
     * Atualiza a rotação máxima considerada para acendimento completo da barra.
     *
     * @param maxRpm Rotação máxima considerada para acendimento completo da barra.
     */
    @SuppressWarnings("unused")
    public void setMaxRpm(int maxRpm) {
        if (maxRpm <= 0)
            return;

        this.maxRpm = maxRpm;
        invalidate();
    }

    /**
     * Atualiza a quantidade total de LEDs exibidos na barra.
     *
     * <p>Como a quantidade de LEDs influencia os cálculos estáticos da renderização, as tabelas
     * internas de cores e alturas são recalculadas após a alteração.</p>
     *
     * @param ledCount Quantidade total de LEDs exibidos na barra.
     */
    @SuppressWarnings("unused")
    public void setLedCount(int ledCount) {
        if (ledCount <= 0)
            return;

        this.ledCount = ledCount;
        calculateLedColors();
        calculateLedHeightsFactors();
        invalidate();
    }

    /**
     * Atualiza a rotação atual do motor exibida no componente Shift Light.
     *
     * <p>O novo valor de RPM é utilizado para recalcular a quantidade de LEDs ativos, atualizar a
     * coloração da barra e atualizar o valor numérico exibido no painel. Após a atualização,
     * solicita o redesenho da View.</p>
     *
     * @param rpm Rotação atual do motor, em RPM.
     */
    public void setRpm(int rpm) {
        this.rpm = Math.max(0, Math.min(rpm, maxRpm));
        invalidate();
    }

    /**
     * Calcula previamente os fatores de altura de cada LED da barra.
     *
     * <p>Os fatores são obtidos por meio de uma curva exponencial, produzindo uma variação gradual
     * de altura entre o primeiro e o último LED. Como esse cálculo depende apenas da quantidade de
     * LEDs, ele é executado apenas quando necessário, evitando operações matemáticas durante a
     * renderização.</p>
     */
    private void calculateLedColors() {
        ledColors = new int[ledCount];

        for (int led = 0; led < ledCount; led++) {
            float percent = (float) led / ledCount;
            ledColors[led] = getLedColor(percent);
        }
    }

    /**
     * Retorna a cor correspondente à posição de um LED na barra.
     *
     * <p>A cor é obtida através de interpolação linear entre cores adjacentes, produzindo uma
     * transição suave entre verde, verde-limão, amarelo, laranja e vermelho. Dessa forma, evita-se
     * mudanças abruptas de cor, proporcionando um efeito visual semelhante às Shift Lights
     * utilizadas em ECUs automotivas.</p>
     *
     * @param percent Posição do LED na barra, variando entre 0.0 e 1.0.
     * @return Cor interpolada correspondente à posição informada.
     */
    private int getLedColor(float percent) {
        if (percent < LED_PERCENT_GREEN_LIME) {
            // Progressão: Verde -> Verde limão
            float t = percent / 0.30f;
            return (int) argbEvaluator.evaluate(t, ledGreen, ledLime);
        } else if (percent < LED_PERCENT_LIME_YELLOW) {
            // Progressão: Verde limão -> Amarelo
            float t = (percent - LED_PERCENT_GREEN_LIME) / (LED_PERCENT_LIME_YELLOW - LED_PERCENT_GREEN_LIME);
            return (int) argbEvaluator.evaluate(t, ledLime, ledYellow);
        } else if (percent < LED_PERCENT_YELLOW_ORANGE) {
            // Progressão: Amarelo -> Laranja
            float t = (percent - LED_PERCENT_LIME_YELLOW) / (LED_PERCENT_YELLOW_ORANGE - LED_PERCENT_LIME_YELLOW);
            return (int) argbEvaluator.evaluate(t, ledYellow, ledOrange);
        } else {
            // Progressão: Laranja -> Vermelho
            float t = (percent - LED_PERCENT_YELLOW_ORANGE) / (1f - LED_PERCENT_YELLOW_ORANGE);
            return (int) argbEvaluator.evaluate(t, ledOrange, ledRed);
        }
    }

    /**
     * Calcula previamente os fatores de altura utilizados para criar a curva dos LEDs.
     *
     * <p>A curva exponencial é calculada uma única vez, evitando operações matemáticas pesadas
     * durante o processo de desenho executado pelo Android.</p>
     */
    private void calculateLedHeightsFactors() {
        ledHeightsFactors = new float[ledCount];

        for (int led = 0; led < ledCount; led++) {
            float position = (float) led / (ledCount - 1);
            ledHeightsFactors[led] = (float) Math.pow(position, LED_CURVE);
        }
    }

    /**
     * Executa o processo de renderização completo da Shift Light.
     *
     * <p>Durante o desenho são calculadas as dimensões disponíveis, altura dos LEDs, quantidade de
     * LEDs ativos conforme o RPM atual, cores progressivas e a escala superior de rotação. Também é
     * exibido o valor numérico do RPM alinhado a barra.</p>
     *
     * @param canvas Canvas utilizado pelo Android para renderização do componente.
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Dimensões disponíveis para desenho
        float width = getWidth();
        float height = getHeight();

        // Calcula a altura disponível para desenhar os LEDs da Shift Light
        // Obs.: Será a altura total do componente reduzida da altura da escala do RPM
        float rpmScaleHeight = height * RPM_SCALE_HEIGHT_FACTOR;
        float ledsHeight = height - rpmScaleHeight;

        // Calcula a largura individual de cada LED considerando os espaçamentos
        float totalSpacing = (ledCount - 1) * LED_SPACING;
        float ledWidth = (width - totalSpacing) / ledCount;

        // Calcula as alturas mínima e máxima dos LEDs com base na altura disponível
        float minHeight = ledsHeight * MIN_LED_HEIGHT_FACTOR;
        float maxHeight = ledsHeight * MAX_LED_HEIGHT_FACTOR;

        // Considera como valor da base dos LEDs a altura disponível
        @SuppressWarnings("RedundantLocalVariable")
        float ledsBottom = ledsHeight;

        // Define o tamanho visual do número do RPM
        rpmValuePaint.setTextSize(maxHeight * RPM_VALUE_HEIGHT_FACTOR);

        // Define a unidade com base no tamanho visual do número do RPM
        rpmUnitPaint.setTextSize(rpmValuePaint.getTextSize() * RPM_UNIT_HEIGHT_FACTOR);

        // Encontra a quantidade de LEDs que devem permanecer acesos conforme o RPM atual
        int ledsOn = rpm * ledCount / maxRpm;

        // Posição horizontal inicial para desenhar os LEDs
        float ledPositionX = 0;

        for (int led = 0; led < ledCount; led++) {
            // Altura atual do LED
            float currentHeight = minHeight + (maxHeight - minHeight) * ledHeightsFactors[led];

            // Define a posição vertical do LED (apoiados na parte inferior)
            float ledsTop = ledsBottom - currentHeight;

            // Se for o LED de referência para desenhar o valor do RPM
            if (led == RPM_REFERENCE_LED) {
                Paint.FontMetrics metrics = rpmValuePaint.getFontMetrics();

                // Altura ocupada pelo texto do RPM
                float textHeight = metrics.descent - metrics.ascent;

                // Espaço livre entre o topo da View e o topo do LED
                @SuppressWarnings("RedundantLocalVariable")
                float availableHeight = ledsTop;

                // Centraliza o texto no espaço livre
                float textY = (availableHeight - textHeight) / 2f - metrics.ascent;
                @SuppressWarnings("RedundantLocalVariable")
                float textX = ledPositionX;

                drawRpm(canvas, textX, textY);
            }

            // Define a cor base do LED conforme seu estado (aceso ou apagado)
            if (led < ledsOn) {
                ledsPaint.setColor(ledColors[led]);
            } else {
                ledsPaint.setColor(ledOff);
            }

            // Desenha o LED com efeito de gradiente
            drawLed(
                    canvas,
                    ledPositionX,
                    ledsTop,
                    ledPositionX + ledWidth,
                    ledsBottom,
                    ledsPaint.getColor()
            );

            ledPositionX += ledWidth + LED_SPACING;
        }

        // Desenha a barra de escala
        drawScale(canvas, width, ledsBottom, ledWidth);
    }

    /**
     * Desenha o valor atual de RPM.
     *
     * <p>O campo numérico possui largura reservada baseada na quantidade máxima de dígitos
     * suportada pelo RPM máximo configurado. Dessa forma, valores menores ocupam o mesmo espaço
     * visual sem deslocar a unidade "rpm", mantendo o alinhamento do painel.</p>
     *
     * @param canvas Canvas utilizado para renderização.
     * @param startX Posição horizontal inicial alinhada ao primeiro LED.
     * @param y      Posição vertical da linha de texto.
     */
    private void drawRpm(Canvas canvas, float startX, float y) {
        String value = String.valueOf(rpm);

        // Reserva espaço para o maior RPM possível
        String maxValue = String.valueOf(maxRpm);

        float reservedWidth = rpmValuePaint.measureText(maxValue);
        float currentWidth = rpmValuePaint.measureText(value);

        // Ajusta a posição X para alinhar o valor pela direita dentro da área reservada
        float valueX = startX + (reservedWidth - currentWidth);

        // Desenha o valor do RPM
        canvas.drawText(
                value,
                valueX,
                y,
                rpmValuePaint);

        // Desenha a unidade do RPM
        canvas.drawText(
                " rpm",
                startX + reservedWidth + 8f,
                y,
                rpmUnitPaint
        );
    }

    /**
     * Desenha um LED aplicando um gradiente vertical gerado a partir da cor base.
     *
     * <p>A partir da cor informada são calculadas automaticamente versões mais clara e mais escura,
     * utilizadas para compor um gradiente vertical que simula profundidade e iluminação. Após a
     * criação do gradiente, o LED é desenhado.</p>
     *
     * @param canvas Canvas utilizado para renderização da View.
     * @param left   Coordenada horizontal esquerda do LED.
     * @param top    Coordenada vertical superior do LED.
     * @param right  Coordenada horizontal direita do LED.
     * @param bottom Coordenada vertical inferior do LED.
     * @param color  Cor base utilizada para gerar o gradiente do LED.
     */
    private void drawLed(
            Canvas canvas,
            float left,
            float top,
            float right,
            float bottom,
            int color
    ) {
        // Calcula uma tonalidade mais clara para a parte superior do LED
        int light = android.graphics.Color.argb(
                255,
                Math.min(255, (int) (android.graphics.Color.red(color) * 1.35f)),
                Math.min(255, (int) (android.graphics.Color.green(color) * 1.35f)),
                Math.min(255, (int) (android.graphics.Color.blue(color) * 1.35f))
        );

        // Calcula uma tonalidade mais escura para a parte inferior do LED
        int dark = android.graphics.Color.argb(
                255,
                (int) (android.graphics.Color.red(color) * 0.65f),
                (int) (android.graphics.Color.green(color) * 0.65f),
                (int) (android.graphics.Color.blue(color) * 0.65f)
        );

        // Cria um gradiente vertical entre as duas tonalidades
        LinearGradient gradient = new LinearGradient(
                0,
                top,
                0,
                bottom,
                light,
                dark,
                Shader.TileMode.CLAMP
        );

        // Aplica o gradiente ao objeto de pintura
        ledsPaint.setShader(gradient);

        // Desenha o corpo principal do LED
        canvas.drawRoundRect(
                left,
                top,
                right,
                bottom,
                CORNER_RADIUS,
                CORNER_RADIUS,
                ledsPaint
        );
    }

    /**
     * Desenha a escala fixa de rotação do motor (RPM) acima da barra de LEDs.
     *
     * <p>A posição de cada marcação é calculada proporcionalmente ao valor máximo de RPM
     * configurado ({@code maxRpm}), garantindo que cada marcação permaneça proporcionalmente
     * alinhada à posição correspondente na barra de LEDs da Shift Light,  independentemente da
     * quantidade de LEDs ou do RPM máximo utilizado.</p>
     *
     * @param canvas     Canvas utilizado para renderização da View.
     * @param width      Largura disponível para desenho.
     * @param ledsBottom Altura reservada para a escala superior.
     * @param ledWidth   Largura de um LED, utilizada para centralizar os números.
     */
    private void drawScale(
            Canvas canvas,
            float width,
            float ledsBottom,
            float ledWidth
    ) {
        int maxScale = maxRpm / 1000;

        for (int rpm = 1; rpm <= maxScale; rpm++) {
            // Posição proporcional do RPM
            float percent = (float) (rpm * 1000) / maxRpm;

            // Calcula a posição horizontal do fator da escala com base na proporção
            float x = percent * width - (ledWidth / 2f);

            canvas.drawText(
                    String.valueOf(rpm),
                    x,
                    ledsBottom + (RPM_SCALE_TEXT_SIZE * 0.9f),
                    rpmScalePaint
            );
        }
    }
}