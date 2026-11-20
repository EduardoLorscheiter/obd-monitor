# OBD Monitor

<!-- markdownlint-disable MD033 -->

<div align="center">
  <img src="images/OBDMonitorArt.png" alt="Identidade visual do OBD Monitor: veículo, Bluetooth e conector OBD-II" width="400">

  <p><strong>Aplicativo Android para monitoramento de parâmetros automotivos via OBD-II</strong></p>

  <img src="https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&amp;logoColor=white" alt="Android 10 ou posterior">
  <img src="https://img.shields.io/badge/Java-11-ED8B00?logo=openjdk&amp;logoColor=white" alt="Código do aplicativo em Java 11">
  <img src="https://img.shields.io/badge/Bluetooth-Cl%C3%A1ssico-0082FC?logo=bluetooth&amp;logoColor=white" alt="Conexão por Bluetooth clássico">
</div>

<!-- markdownlint-enable MD033 -->

## 🚗 Sobre o OBD Monitor

O **OBD Monitor** transforma um celular ou tablet Android em um painel para acompanhar medições disponibilizadas pelo veículo via OBD-II. A comunicação acontece por um adaptador ELM327 conectado por Bluetooth clássico.

A interface em paisagem reúne as leituras, o estado da conexão e um alerta visual de rotação. O foco é facilitar a consulta dos dados durante a sessão de monitoramento, com ajustes de aparência e de RPM adequados ao veículo.

## ✨ Recursos

- **Medições em um só painel:** rotação, velocidade, abertura da borboleta, pressão absoluta do coletor (MAP), temperaturas de arrefecimento e admissão, tensão e nível de combustível.
- **Alerta visual de rotação:** configure o RPM máximo do veículo e o ponto a partir do qual o painel deve piscar.
- **Estado da comunicação:** acompanhe separadamente a conexão Bluetooth, a comunicação com a ECU e o protocolo identificado pelo adaptador.
- **Interface ajustável:** escolha tema claro, escuro ou conforme o sistema, com textos em português ou inglês.
- **Uso local:** acompanhe as leituras sem cadastro ou conexão com a internet. As preferências ficam salvas no dispositivo.

## 📋 O que você precisa

- Um celular ou tablet Android com Bluetooth clássico e versão compatível com o aplicativo.
- Um adaptador ELM327 que ofereça comunicação serial Bluetooth pelo perfil SPP.
- Um veículo com interface OBD-II que disponibilize as medições consultadas.

## 🔌 Primeira conexão

Com o aplicativo instalado e o adaptador disponível, siga o passo a passo em [comportamento atual](OBDMonitor/docs/current-behavior.md), tópico **"Conectar e usar"**. Esse é o guia de referência para preparar o pareamento, conectar ao adaptador e retornar ao painel.

O mesmo documento explica o acesso ao Bluetooth em **"Permissões Bluetooth"** e as opções de tema, idioma e rotação em **"Configurações"**.

## 📦 Gerar o aplicativo

O projeto pode ser aberto no Android Studio. Para preparar uma cópia local e gerar o APK:

1. Consulte [desenvolvimento](OBDMonitor/docs/development.md), tópico **"Preparar o ambiente"**, para configurar Java, Android SDK e Gradle.
2. Use [verificação](OBDMonitor/docs/verification.md), tópico **"Comandos e resultados"**, para localizar a tarefa de compilação e o APK gerado.

## 🔎 Compatibilidade e alcance

As medições disponíveis dependem dos PIDs oferecidos pela ECU e das respostas do adaptador. A qualidade e o tempo de atualização também variam conforme essa comunicação.

O escopo atual é o acompanhamento de dados do veículo. Consulte [comportamento atual](OBDMonitor/docs/current-behavior.md), tópicos **"Validade e indisponibilidade dos dados"** e **"Limites funcionais"**, para entender o tratamento das leituras e os recursos ainda não implementados.

## 📚 Código e documentação

O aplicativo Android está no diretório [OBDMonitor](OBDMonitor/). A implementação usa Java, layouts XML e Views customizadas.

O [catálogo de documentação](OBDMonitor/README.md), tópico **"Documentação"**, reúne as referências de arquitetura, comportamento, desenvolvimento e verificação. Ele é o ponto de entrada para explorar o código ou contribuir com o projeto.
