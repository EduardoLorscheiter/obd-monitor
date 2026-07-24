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
 **           conforme o aumento da rotação do motor, alterando sua coloração de verde para amarelo,
 **           laranja e vermelho. Além disso, os LEDs possuem altura crescente, proporcionando um
 **           efeito visual semelhante aos painéis de gerenciamento eletrônico de motores (ECUs),
 **           como FuelTech.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.eduardo.obdmonitor.R;

public class ShiftLightView extends View {
    // Rotação máxima considerada para acendimento completo da barra
    private static final int MAX_RPM = 7000;

    // Quantidade total de LEDs exibidos na barra
    private static final int LED_COUNT = 60;

    // Espaçamento entre os LEDs, em pixels
    private static final float LED_SPACING = 2f;

    // Raio utilizado para arredondar os cantos dos LEDs
    private static final float CORNER_RADIUS = 8f;

    // Proporção do primeiro LED em relação a altura total
    private static final float MIN_LED_HEIGHT_FACTOR = 0.05f;

    // Proporção do último LED em relação a altura total
    private static final float MAX_LED_HEIGHT_FACTOR = 0.95f;

    // Objeto utilizado para desenhar os LEDs da Shift Light
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

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
     * Inicializa os recursos necessários para a View.
     *
     * <p>Pode ser utilizado para configuração de atributos customizados, carregamento de recursos
     * ou inicialização de objetos utilizados pela Shift Light.</p>
     */
    private void init() {

    }

    /**
     * Atualiza o valor da rotação do motor (RPM) exibido pela Shift Light.
     *
     * <p>Após atualizar o RPM, solicita o redesenho da View para refletir imediatamente o novo
     * estado da barra.</p>
     *
     * @param rpm Rotação atual do motor, em RPM.
     */
    public void setRpm(int rpm) {
        this.rpm = rpm;
        invalidate();
    }

    /**
     * Desenha a barra da Shift Light.
     *
     * <p>Os LEDs são desenhados horizontalmente, aumentando sua altura de forma progressiva da
     * esquerda para a direita. Conforme o RPM aumenta, novos LEDs são acesos, alterando sua
     * coloração entre verde, amarelo, laranja e vermelho. LEDs ainda não ativos permanecem na cor
     * cinza escuro.</p>
     *
     * @param canvas Canvas utilizado para renderização da View.
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Dimensões disponíveis para desenho
        float width = getWidth();
        float height = getHeight();

        // Calcula a largura individual de cada LED considerando os espaçamentos
        float totalSpacing = (LED_COUNT - 1) * LED_SPACING;
        float ledWidth = (width - totalSpacing) / LED_COUNT;

        // Encontra a quantidade de LEDs que devem permanecer acesos conforme o RPM atual
        int ledsOn = rpm * LED_COUNT / MAX_RPM;

        // Posição horizontal inicial
        float x = 0;

        // Alturas mínima e máxima dos LEDs
        float minHeight = height * MIN_LED_HEIGHT_FACTOR;
        float maxHeight = height * MAX_LED_HEIGHT_FACTOR;

        for (int led = 0; led < LED_COUNT; led++) {
            // Calcula um fator de crescimento não linear para gerar uma curva suave
            float factor = (float) Math.pow((float) led / (LED_COUNT - 1), 2.4);

            // Suaviza ainda mais a curva de crescimento
            factor = (float) Math.pow(factor, 1.6);

            // Altura atual do LED
            float currentHeight = minHeight + (maxHeight - minHeight) * factor;

            // Define a posição vertical do LED (apoiados na parte superior)
            float top = 0;

            // Define a cor do LED de acordo com sua posição na barra
            if (led < ledsOn) {
                float percent = (float) led / LED_COUNT;

                if (percent < 0.40f) {
                    paint.setColor(getContext().getColor(R.color.led_green));
                } else if (percent < 0.70f) {
                    paint.setColor(getContext().getColor(R.color.led_yellow));
                } else if (percent < 0.90f) {
                    paint.setColor(getContext().getColor(R.color.led_orange));
                } else {
                    paint.setColor(getContext().getColor(R.color.led_red));
                }
            } else {
                // LEDs ainda não atingidos pelo RPM permanecem apagados
                paint.setColor(getContext().getColor(R.color.led_off));
            }

            // Desenha o LED com efeito de gradiente
            drawLed(
                    canvas,
                    x,
                    top,
                    x + ledWidth,
                    currentHeight,
                    paint.getColor()
            );

            x += ledWidth + LED_SPACING;
        }
    }

    /**
     * Desenha um único LED utilizando um gradiente vertical para simular profundidade e efeito de
     * iluminação.
     *
     * <p>O gradiente é composto por uma tonalidade mais clara na parte superior e outra mais escura
     * na parte inferior, proporcionando um aspecto semelhante ao de LEDs encontrados em painéis
     * automotivos.</p>
     *
     * @param canvas Canvas utilizado para renderização.
     * @param left   Coordenada horizontal esquerda do LED.
     * @param top    Coordenada vertical superior do LED.
     * @param right  Coordenada horizontal direita do LED.
     * @param bottom Coordenada vertical inferior do LED.
     * @param color  Cor base utilizada para compor o gradiente.
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
        paint.setShader(gradient);

        // Desenha o corpo principal do LED
        canvas.drawRoundRect(
                left,
                top,
                right,
                bottom,
                CORNER_RADIUS,
                CORNER_RADIUS,
                paint
        );
    }
}