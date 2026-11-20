/*
 **=================================================================================================
 **  @file    SplashActivity.java
 **  @author  Eduardo Lorscheiter
 **  @class   SplashActivity
 **  @date    30/07/2026
 **  @version 1.0
 **  @brief   Interface inicial do aplicativo OBD Monitor.
 **  @details Exibe a Splash Screen do aplicativo, apresenta sua identidade visual, inicializa os
 **           gerenciadores globais necessários ao funcionamento da aplicação e realiza a transição
 **           para a Dashboard principal após um breve intervalo de tempo.
 **=================================================================================================
 */

package com.eduardo.obdmonitor.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eduardo.obdmonitor.R;
import com.eduardo.obdmonitor.BuildConfig;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {
    /**
     * Tempo de exibição da Splash Screen, em milissegundos.
     */
    private static final long SPLASH_DURATION_MS = 1500L;

    /**
     * Componentes da janela da Activity.
     */
    private ImageView imgLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        startLogoAnimation();
        scheduleNavigation();
    }

    /**
     * Inicializa os componentes visuais da Activity.
     *
     * <p>Realiza a associação entre os elementos da interface e seus respectivos componentes
     * declarados no layout.</p>
     */
    private void initViews() {
        imgLogo = findViewById(R.id.imgLogo);
        TextView txtVersion = findViewById(R.id.txtVersion);
        txtVersion.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME));
    }

    /**
     * Inicia a animação de exibição da logo do aplicativo.
     *
     * <p>Aplica um efeito de fade-in combinado com uma leve ampliação da imagem, proporcionando uma
     * transição visual suave durante a apresentação da Splash Screen.</p>
     */
    private void startLogoAnimation() {
        imgLogo.setAlpha(0f);
        imgLogo.setScaleX(0.85f);
        imgLogo.setScaleY(0.85f);

        imgLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(750)
                .start();
    }

    /**
     * Agenda a navegação para a Dashboard principal.
     *
     * <p>Mantém a Splash Screen visível durante o tempo definido por {@link #SPLASH_DURATION_MS} e,
     * ao término desse intervalo, inicia a transição para a {@link DashboardActivity}.</p>
     */
    private void scheduleNavigation() {
        getWindow().getDecorView().postDelayed(this::navigateToDashboard, SPLASH_DURATION_MS);
    }

    /**
     * Realiza a navegação para a Dashboard principal.
     *
     * <p>Inicia a {@link DashboardActivity}, aplica a animação de transição entre as Activities e
     * finaliza a Splash Screen para impedir seu retorno através da pilha de navegação.</p>
     */
    private void navigateToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        applyTransition();
        finish();
    }

    /**
     * Aplica a animação de transição entre a Splash Screen e a Dashboard.
     *
     * <p>Utiliza a API recomendada para transições em dispositivos com Android 14 (API 34) ou
     * superior e, para manter a compatibilidade com versões anteriores do sistema, utiliza o legado
     * {@code overridePendingTransition(int, int)} quando necessário.</p>
     */
    @SuppressWarnings("deprecation")
    private void applyTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );
        } else {
            overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );
        }
    }
}
