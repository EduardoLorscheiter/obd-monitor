/*
 **=================================================================================================
 **  @file    ShiftLightView.java
 **  @author  Eduardo Lorscheiter
 **  @class   ShiftLightView
 **  @date    24/07/2026
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
    /**
     * Quantidade total de LEDs exibidos na Shift Light.
     */
    private static final int LED_COUNT = 60;

    /**
     * Altura reservada para a escala de RPM em relação à altura total da View.
     */
    private static final float RPM_SCALE_HEIGHT_FACTOR = 0.18f;

    /**
     * Proporção mínima e máxima dos LEDs em relação à altura total disponível para os LEDs.
     */
    private static final float MIN_LED_HEIGHT_FACTOR = 0.05f;
    private static final float MAX_LED_HEIGHT_FACTOR = 0.95f;

    /**
     * Espaçamento entre os LEDs, em píxeis.
     */
    private static final float LED_SPACING = 2f;

    /**
     * Altura reservada para o valor do RPM em relação ao maior LED.
     */
    private static final float RPM_VALUE_HEIGHT_FACTOR = 0.70f;

    /**
     * Altura reservada para a unidade do RPM em relação ao valor do RPM.
     */
    private static final float RPM_UNIT_HEIGHT_FACTOR = 0.45f;

    /**
     * Percentuais de transição das cores dos LEDs.
     */
    private static final float LED_PERCENT_GREEN_LIME = 0.30f;
    private static final float LED_PERCENT_LIME_YELLOW = 0.40f;
    private static final float LED_PERCENT_YELLOW_ORANGE = 0.65f;

    /**
     * Expoente utilizado para calcular a curva de crescimento da altura dos LEDs.
     */
    private static final float LED_CURVE = 3.84f;

    /**
     * Raio utilizado para arredondar os cantos dos LEDs.
     */
    private static final float LED_CORNER_RADIUS = 8f;

    /**
     * Índice do LED utilizado como referência para posicionar o valor do RPM.
     */
    private static final int RPM_REFERENCE_LED = 0;

    /**
     * Tamanho do texto da escala de RPM.
     */
    private static final float RPM_SCALE_TEXT_SIZE = 34f;

    /**
     * Espaçamento entre o valor do RPM e a unidade, em píxeis.
     */
    private static final float RPM_UNIT_SPACING = 8f;

    /**
     * Objetos utilizados para desenhar a Shift Light.
     */
    private final Paint rpmValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rpmUnitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ledsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rpmScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * Interpolador suave para mudanças de cores entre os estágios da Shift Light.
     *
     * <p>Evita transições bruscas entre os LEDs.</p>
     */
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    /**
     * Cores dos LEDs.
     */
    private int ledOff;
    private int ledGreen;
    private int ledLime;
    private int ledYellow;
    private int ledOrange;
    private int ledRed;

    /**
     * Cores pré-calculadas de cada LED da barra.
     */
    private final int[] ledColors = new int[LED_COUNT];
    private final LinearGradient[] ledOnGradients = new LinearGradient[LED_COUNT];
    private final LinearGradient[] ledOffGradients = new LinearGradient[LED_COUNT];

    /**
     * Fatores de altura pré-calculados de cada LED da barra.
     */
    private final float[] ledHeightFactors = new float[LED_COUNT];

    /**
     * Métricas geométricas pré-calculadas dos LEDs da barra.
     */
    private float ledWidth;
    private float ledBottom;
    private final float[] ledTops = new float[LED_COUNT];

    /**
     * Métricas tipográficas do valor do RPM utilizadas para alinhamento vertical do texto.
     */
    private Paint.FontMetrics rpmFontMetrics;

    /**
     * Altura ocupada pelo texto do valor do RPM.
     */
    private float rpmTextHeight;

    /**
     * Rotação máxima considerada para acendimento completo da barra.
     */
    private int maxRpm = 7000;

    /**
     * Rotação do motor (RPM) atual.
     */
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
     * Notificado pelo Android sempre que o tamanho da View é alterado.
     *
     * <p>Responsável por atualizar as métricas geométricas utilizadas na renderização da Shift
     * Light, incluindo dimensões dos LEDs, posições, tamanhos dos textos e gradientes.
     * O recálculo é necessário porque a View pode possuir diferentes dimensões conforme o
     * dispositivo, orientação da tela ou alterações no layout.</p>
     *
     * @param width     Nova largura da View em pixels.
     * @param height    Nova altura da View em pixels.
     * @param oldWidth  Largura anterior da View em pixels.
     * @param oldHeight Altura anterior da View em pixels.
     */
    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        updateLayoutMetrics(width, height);
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
        updateLayoutMetrics(getWidth(), getHeight());
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
     * Inicializa os objetos de pintura, carrega as cores da aplicação e executa os pré-cálculos
     * necessários para a renderização inicial do componente.
     */
    private void init() {
        initPaints();
        loadColors();
        updateLayoutMetrics(getWidth(), getHeight());
    }

    /**
     * Inicializa os objetos Paint utilizados na renderização da Shift Light.
     *
     * <p>Configura cores, alinhamentos e propriedades tipográficas dos elementos desenhados pela
     * View.</p>
     */
    private void initPaints() {
        rpmValuePaint.setColor(getContext().getColor(R.color.automotive_green));
        rpmValuePaint.setFakeBoldText(true);

        int textColor = ViewUtils.getThemeAttrColor(getContext(), com.google.android.material.R.attr.colorOnPrimary);
        rpmUnitPaint.setColor(textColor);

        rpmScalePaint.setColor(textColor);
        rpmScalePaint.setTextSize(RPM_SCALE_TEXT_SIZE);
        rpmScalePaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Carrega as cores dos recursos da aplicação e executa os pré-cálculos dependentes dessas
     * cores.
     */
    private void loadColors() {
        ledOff = getContext().getColor(R.color.led_off);
        ledGreen = getContext().getColor(R.color.led_green);
        ledLime = getContext().getColor(R.color.led_lime);
        ledYellow = getContext().getColor(R.color.led_yellow);
        ledOrange = getContext().getColor(R.color.led_orange);
        ledRed = getContext().getColor(R.color.led_red);
        calculateLedColors();
        calculateLedHeightFactors();
    }

    /**
     * Calcula previamente a cor correspondente de cada LED da barra.
     *
     * <p>Como a cor de cada LED depende apenas de sua posição relativa na barra, este cálculo é
     * realizado apenas quando necessário, evitando interpolações durante a renderização.</p>
     */
    private void calculateLedColors() {
        for (int led = 0; led < LED_COUNT; led++) {
            float percent = (float) led / (LED_COUNT - 1);
            ledColors[led] = getLedColor(percent);
        }
    }

    /**
     * Retorna a cor correspondente à posição de um LED na barra.
     *
     * <p>A cor é obtida por interpolação linear entre cores adjacentes, produzindo uma transição
     * suave entre verde, verde-limão, amarelo, laranja e vermelho. Dessa forma, evitam-se mudanças
     * abruptas de cor, proporcionando um efeito visual semelhante às Shift Lights utilizadas em
     * ECUs automotivas.</p>
     *
     * @param percent Posição do LED na barra, variando entre 0.0 e 1.0.
     * @return Cor interpolada correspondente à posição informada.
     */
    private int getLedColor(float percent) {
        if (percent < LED_PERCENT_GREEN_LIME) {
            // Progressão: Verde → Verde limão
            float t = percent / LED_PERCENT_GREEN_LIME;
            return (int) argbEvaluator.evaluate(t, ledGreen, ledLime);
        } else if (percent < LED_PERCENT_LIME_YELLOW) {
            // Progressão: Verde limão → Amarelo
            float t = (percent - LED_PERCENT_GREEN_LIME) / (LED_PERCENT_LIME_YELLOW - LED_PERCENT_GREEN_LIME);
            return (int) argbEvaluator.evaluate(t, ledLime, ledYellow);
        } else if (percent < LED_PERCENT_YELLOW_ORANGE) {
            // Progressão: Amarelo → Laranja
            float t = (percent - LED_PERCENT_LIME_YELLOW) / (LED_PERCENT_YELLOW_ORANGE - LED_PERCENT_LIME_YELLOW);
            return (int) argbEvaluator.evaluate(t, ledYellow, ledOrange);
        } else {
            // Progressão: Laranja → Vermelho
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
    private void calculateLedHeightFactors() {
        for (int led = 0; led < LED_COUNT; led++) {
            float position = (float) led / (LED_COUNT - 1);
            ledHeightFactors[led] = (float) Math.pow(position, LED_CURVE);
        }
    }

    /**
     * Recalcula todas as métricas utilizadas durante a renderização.
     *
     * <p>Executado durante a inicialização do componente e sempre que o tamanho da View é alterado.
     * São recalculadas as dimensões dos LEDs, suas posições, o tamanho dos textos e demais métricas
     * necessárias para evitar cálculos durante o desenho.</p>
     *
     * @param width  Largura disponível da View.
     * @param height Altura disponível da View.
     */
    private void updateLayoutMetrics(int width, int height) {
        if (width <= 0 || height <= 0)
            return;

        // Calcula a altura disponível para desenhar os LEDs da Shift Light
        // Obs.: Será a altura total da componente reduzida da altura da escala do RPM
        float rpmScaleHeight = height * RPM_SCALE_HEIGHT_FACTOR;
        float ledsHeight = height - rpmScaleHeight;

        // Calcula as alturas mínima e máxima dos LEDs com base na altura disponível
        float minLedHeight = ledsHeight * MIN_LED_HEIGHT_FACTOR;
        float maxLedHeight = ledsHeight * MAX_LED_HEIGHT_FACTOR;

        // Calcula a largura individual de cada LED considerando os espaçamentos
        float totalSpacing = (LED_COUNT - 1) * LED_SPACING;
        ledWidth = (width - totalSpacing) / LED_COUNT;

        // Considera como base dos LEDs a altura disponível
        ledBottom = ledsHeight;

        // Calcula o topo individual de cada LED
        for (int led = 0; led < LED_COUNT; led++) {
            float ledHeight = minLedHeight + (maxLedHeight - minLedHeight) * ledHeightFactors[led];
            ledTops[led] = ledBottom - ledHeight;
        }

        // Define o tamanho visual do valor do RPM
        rpmValuePaint.setTextSize(maxLedHeight * RPM_VALUE_HEIGHT_FACTOR);
        // Define o tamanho visual da unidade do RPM do com base no tamanho visual do valor do RPM
        rpmUnitPaint.setTextSize(rpmValuePaint.getTextSize() * RPM_UNIT_HEIGHT_FACTOR);

        // Calcula a altura ocupada pelo texto do RPM
        rpmFontMetrics = rpmValuePaint.getFontMetrics();
        rpmTextHeight = rpmFontMetrics.descent - rpmFontMetrics.ascent;

        // Cria os gradientes dos LEDs
        for (int led = 0; led < LED_COUNT; led++) {
            ledOnGradients[led] = createGradient(
                    ledColors[led],
                    ledTops[led],
                    ledBottom
            );

            ledOffGradients[led] = createGradient(
                    ledOff,
                    ledTops[led],
                    ledBottom
            );
        }
    }

    /**
     * Cria um gradiente vertical utilizado para renderização de um LED.
     *
     * <p>O gradiente é composto por uma tonalidade mais clara na região superior e uma tonalidade
     * mais escura na região inferior, produzindo um efeito visual de profundidade e iluminação
     * semelhante ao de um LED físico.</p>
     *
     * @param top    Coordenada superior do LED.
     * @param bottom Coordenada inferior do LED.
     * @param color  Cor base utilizada para gerar o gradiente.
     * @return Gradiente vertical correspondente à cor informada.
     */
    private static LinearGradient createGradient(int color, float top, float bottom) {
        return new LinearGradient(
                0,
                top,
                0,
                bottom,
                getLightenColor(color),
                getDarkenColor(color),
                Shader.TileMode.CLAMP
        );
    }

    /**
     * Calcula uma versão mais clara da cor informada.
     *
     * <p>Utilizado para compor a região superior do gradiente dos LEDs, simulando a incidência de
     * luz sobre a superfície.</p>
     *
     * @param color Cor base.
     * @return Cor com luminosidade aumentada.
     */
    private static int getLightenColor(int color) {
        return android.graphics.Color.argb(
                255,
                Math.min(255, (int) (android.graphics.Color.red(color) * 1.35f)),
                Math.min(255, (int) (android.graphics.Color.green(color) * 1.35f)),
                Math.min(255, (int) (android.graphics.Color.blue(color) * 1.35f))
        );
    }

    /**
     * Calcula uma versão mais escura da cor informada.
     *
     * <p>Utilizado para compor a região inferior do gradiente dos LEDs, aumentando a percepção de
     * profundidade durante a renderização.</p>
     *
     * @param color Cor base.
     * @return Cor com luminosidade reduzida.
     */
    private static int getDarkenColor(int color) {
        return android.graphics.Color.argb(
                255,
                (int) (android.graphics.Color.red(color) * 0.65f),
                (int) (android.graphics.Color.green(color) * 0.65f),
                (int) (android.graphics.Color.blue(color) * 0.65f)
        );
    }

    /**
     * Executa o processo de renderização completo da Shift Light.
     *
     * <p>Durante o desenho são utilizados os valores previamente calculados para determinar a
     * quantidade de LEDs ativos, suas cores, além da renderização do valor numérico do RPM e da
     * escala superior.</p>
     *
     * @param canvas Canvas utilizado pelo Android para renderização do componente.
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Encontra a quantidade de LEDs que devem permanecer acesos conforme o RPM atual
        int ledsOn = rpm * LED_COUNT / maxRpm;

        // Posição horizontal inicial para desenhar os LEDs
        float ledPositionX = 0;

        for (int led = 0; led < LED_COUNT; led++) {
            float ledTop = ledTops[led];

            // Se for o LED de referência para desenhar o valor do RPM
            if (led == RPM_REFERENCE_LED) {
                // Centraliza o texto no espaço livre
                float textY = (ledTop - rpmTextHeight) / 2f - rpmFontMetrics.ascent;
                @SuppressWarnings("RedundantLocalVariable")
                float textX = ledPositionX;

                drawRpm(canvas, textX, textY);
            }

            drawLed(
                    canvas,
                    ledPositionX,
                    ledTop,
                    ledPositionX + ledWidth,
                    ledBottom,
                    led < ledsOn ? ledOnGradients[led] : ledOffGradients[led]
            );

            ledPositionX += ledWidth + LED_SPACING;
        }

        drawScale(canvas);
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
                " " + getContext().getString(R.string.rpm),
                startX + reservedWidth + RPM_UNIT_SPACING,
                y,
                rpmUnitPaint
        );
    }

    /**
     * Desenha um LED aplicando um gradiente vertical gerado a partir da cor base.
     *
     * <p>A partir da cor informada são calculadas automaticamente versões mais clara e mais escura,
     * utilizadas para compor um gradiente vertical que simula profundidade e iluminação. O
     * gradiente é aplicado temporariamente ao objeto Paint e removido após o desenho do LED,
     * evitando interferência na renderização dos demais elementos.</p>
     *
     * @param canvas   Canvas utilizado para renderização da View.
     * @param left     Coordenada horizontal esquerda do LED.
     * @param top      Coordenada vertical superior do LED.
     * @param right    Coordenada horizontal direita do LED.
     * @param bottom   Coordenada vertical inferior do LED.
     * @param gradient Gradiente previamente calculado utilizado para renderizar o LED.
     */
    private void drawLed(
            Canvas canvas,
            float left,
            float top,
            float right,
            float bottom,
            LinearGradient gradient
    ) {
        ledsPaint.setShader(gradient);

        canvas.drawRoundRect(
                left,
                top,
                right,
                bottom,
                LED_CORNER_RADIUS,
                LED_CORNER_RADIUS,
                ledsPaint
        );

        ledsPaint.setShader(null);
    }

    /**
     * Desenha a escala fixa de rotação do motor (RPM) acima da barra de LEDs.
     *
     * <p>A posição de cada marcação é calculada proporcionalmente ao valor máximo de RPM
     * configurado ({@code maxRpm}), garantindo que cada marcação permaneça proporcionalmente
     * alinhada à posição correspondente na barra de LEDs da Shift Light, independentemente da
     * quantidade de LEDs ou do RPM máximo utilizado.</p>
     *
     * @param canvas Canvas utilizado para renderização da View.
     */
    private void drawScale(Canvas canvas) {
        // Dimensões disponíveis para desenho
        float width = getWidth();

        int maxScale = maxRpm / 1000;

        // Desenha o zero no início da escala
        canvas.drawText(
                getContext().getString(R.string.zero),
                ledWidth / 2f,
                ledBottom + (RPM_SCALE_TEXT_SIZE * 0.9f),
                rpmScalePaint
        );

        // Desenha as demais marcações da escala
        for (int rpm = 1; rpm <= maxScale; rpm++) {
            // Posição proporcional do RPM
            float percent = (float) (rpm * 1000) / maxRpm;

            // Calcula a posição horizontal do fator da escala com base na proporção
            float x = percent * width - (ledWidth / 2f);

            canvas.drawText(
                    String.valueOf(rpm),
                    x,
                    ledBottom + (RPM_SCALE_TEXT_SIZE * 0.9f),
                    rpmScalePaint
            );
        }
    }
}