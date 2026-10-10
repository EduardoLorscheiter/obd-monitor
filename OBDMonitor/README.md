# OBD Monitor

Aplicativo Android para acompanhar dados de diagnóstico veicular OBD-II usando um adaptador ELM327 conectado por Bluetooth.

## Estado atual

- Aplicativo Android nativo, com código de produção em Java 11.
- Módulo Android principal: `app`.
- Interface com atividades para splash, conexão, painel e configurações.
- Pacotes próprios para Bluetooth, protocolo ELM327, comandos OBD-II, agendamento de PIDs, modelos, utilitários e componentes visuais.
- `minSdk` 29 e `targetSdk` 36; consulte `app/build.gradle.kts` como fonte de verdade.

## Documentação para desenvolvimento

- [Arquitetura e mapa do código](docs/architecture.md)
- [Guia de implementação](docs/development.md)
- [Verificação e testes](docs/verification.md)
- [Instruções para agentes](AGENTS.md)
- [Skill android-obd-monitor](.agents/skills/obd-monitor/SKILL.md)

## Abrir e compilar

Abra o projeto no Android Studio e sincronize com Gradle. Em Windows, a compilação de debug pode ser iniciada com:

```powershell
.\gradlew.bat :app:assembleDebug
```

O projeto requer um Android SDK compatível com o `compileSdk` declarado. Configurações locais, como `local.properties`, não devem ser compartilhadas como configuração portátil.

## Observação de segurança

O aplicativo apresenta dados do veículo; interpretações e novos comandos devem respeitar as especificações OBD-II e tratar erros do adaptador/ECU. Não use valores ilustrativos como se fossem leituras reais.
