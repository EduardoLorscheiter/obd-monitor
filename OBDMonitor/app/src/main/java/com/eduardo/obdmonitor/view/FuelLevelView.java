/*
 **=================================================================================================
 **  @file    FuelLevelView.java
 **  @author  Eduardo Lorscheiter
 **  @class   FuelLevelView
 **  @date    21/08/2026
 **  @version 1.0
 **  @brief   Componente gráfico responsável por exibir o nível de combustível do veículo.
 **  @details O componente apresenta o nível de combustível por meio de um recipiente vertical
 **           preenchido proporcionalmente ao percentual informado, acompanhado do valor percentual.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.eduardo.obdmonitor.R;

public class FuelLevelView extends View {
    // Ícone utilizado para identificar visualmente o nível de combustível
    private Drawable fuelIcon;

    // Nível atual de combustível, representado em percentual (0 a 100%)
    private float fuelLevel;

    // Objeto responsável pelas propriedades de pintura dos elementos gráficos
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Caminho utilizado para definir a área preenchida que representa o combustível
    private final Path liquidPath = new Path();

    // Área delimitada do recipiente utilizado para representar o nível de combustível
    private final RectF fuelRect = new RectF();

    public FuelLevelView(Context context) {
        super(context);
        init();
    }

    public FuelLevelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FuelLevelView(
            Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Inicializa as propriedades gráficas utilizadas pelo componente.
     *
     * <p>Configura as propriedades de desenho, carrega o ícone de combustível e define o plano de
     * fundo padrão do componente.</p>
     */
    private void init() {
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        fuelIcon = ContextCompat.getDrawable(
                getContext(),
                R.drawable.ic_dashboard_fuel
        );

        setBackgroundResource(R.drawable.metric_card_background);
    }

    /**
     * Atualiza o nível de combustível exibido pelo componente.
     *
     * <p>O valor informado é limitado ao intervalo de 0 a 100%, garantindo que valores fora desse
     * intervalo não sejam utilizados no desenho.</p>
     *
     * @param level Percentual do nível de combustível, entre 0 e 100.
     */
    public void setFuelLevel(float level) {
        fuelLevel = Math.max(0.0f, Math.min(100.0f, level));
        invalidate();
    }

    /**
     * Realiza o desenho dos elementos gráficos do componente.
     *
     * <p>São desenhados o ícone de combustível, o recipiente, o nível de combustível e o respectivo
     * valor percentual.</p>
     *
     * @param canvas Área de desenho utilizada para renderizar o componente.
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        drawFuelIcon(canvas);
        drawFuelContainer(canvas);
        drawFuel(canvas);
        drawPercentage(canvas);
    }

    /**
     * Desenha o ícone utilizado para identificar o nível de combustível.
     *
     * <p>O ícone é centralizado horizontalmente e posicionado na região superior do componente.</p>
     *
     * @param canvas Área de desenho utilizada para renderizar o ícone.
     */
    private void drawFuelIcon(Canvas canvas) {
        if (fuelIcon == null)
            return;

        float iconSize = 28.0f;

        float left = (getWidth() - iconSize) / 2.0f;
        float top = getHeight() * 0.04f;

        fuelIcon.setBounds(
                Math.round(left),
                Math.round(top),
                Math.round(left + iconSize),
                Math.round(top + iconSize)
        );

        fuelIcon.draw(canvas);
    }

    /**
     * Desenha o recipiente utilizado para representar o nível de combustível.
     *
     * <p>O recipiente é dimensionado proporcionalmente ao tamanho disponível no componente e possui
     * bordas arredondadas.</p>
     *
     * @param canvas Área de desenho utilizada para renderizar o recipiente.
     */
    private void drawFuelContainer(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();

        float containerWidth = width * 0.42f;
        float containerHeight = height * 0.62f;

        float left = (width - containerWidth) / 2.0f;
        float top = height * 0.17f;
        float right = left + containerWidth;
        float bottom = top + containerHeight;

        fuelRect.set(left, top, right, bottom);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.0f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.white));

        canvas.drawRoundRect(
                fuelRect,
                8.0f,
                8.0f,
                paint
        );
    }

    /**
     * Desenha o nível de combustível proporcionalmente ao percentual informado.
     *
     * <p>O preenchimento é realizado verticalmente, iniciando na parte inferior do recipiente e
     * avançando proporcionalmente ao nível de combustível atual.</p>
     *
     * @param canvas Área de desenho utilizada para renderizar o nível de combustível.
     */
    private void drawFuel(Canvas canvas) {

        float liquidHeight = fuelRect.height() * (fuelLevel / 100.0f);
        float liquidTop = fuelRect.bottom - liquidHeight;

        liquidPath.reset();

        liquidPath.moveTo(
                fuelRect.left + 2.0f,
                liquidTop
        );

        liquidPath.quadTo(
                fuelRect.centerX(),
                liquidTop - 4.0f,
                fuelRect.right - 2.0f,
                liquidTop
        );

        liquidPath.lineTo(
                fuelRect.right - 2.0f,
                fuelRect.bottom - 2.0f
        );

        liquidPath.lineTo(
                fuelRect.left + 2.0f,
                fuelRect.bottom - 2.0f
        );

        liquidPath.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(
                ContextCompat.getColor(getContext(), R.color.automotive_green)
        );

        canvas.drawPath(liquidPath, paint);
    }

    /**
     * Desenha o percentual atual do nível de combustível.
     *
     * <p>O valor é apresentado abaixo do recipiente e arredondado para o número
     * inteiro mais próximo.</p>
     *
     * @param canvas Área de desenho utilizada para renderizar o percentual.
     */
    private void drawPercentage(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(20.0f);
        paint.setFakeBoldText(true);

        String percentage = Math.round(fuelLevel) + "%";

        canvas.drawText(
                percentage,
                getWidth() / 2.0f,
                getHeight() * 0.91f,
                paint
        );

        paint.setFakeBoldText(false);
    }
}