# OBD Monitor

Aplicativo Android nativo para monitoramento de dados atuais do veículo via OBD-II, usando um adaptador ELM327 conectado por Bluetooth clássico.

## Projeto

O código Android fica no módulo `app`. A plataforma e os valores de SDK atuais estão descritos em [arquitetura](docs/architecture.md); `app/build.gradle.kts` e `gradle/libs.versions.toml` são as fontes de configuração.

## Documentação

- [Arquitetura](docs/architecture.md): módulos, dependências entre componentes e mapa do código.
- [Comportamento atual](docs/current-behavior.md): fluxos de tela, conexão, PIDs, configurações e limitações observadas no código.
- [Desenvolvimento](docs/development.md): fonte única das convenções e regras para alterações.
- [Verificação](docs/verification.md): testes existentes, comandos e limites do ambiente.
- [Orientações para agentes](AGENTS.md): ponto de entrada e roteamento para a documentação do projeto.
- [Skill obd-monitor](.agents/skills/obd-monitor/SKILL.md): sequência de trabalho para tarefas deste aplicativo; as regras ficam nos documentos referenciados.

## Abrir e compilar

Abra o projeto no Android Studio e sincronize com Gradle. Em Windows, gere o APK de debug com:

```powershell
.\gradlew.bat :app:assembleDebug
```

É necessário ter um Android SDK compatível com o `compileSdk` configurado. Arquivos locais como `local.properties` não são configuração portátil e não devem ser usados como documentação do ambiente de outra pessoa.

## Escopo funcional

Os fluxos, semântica dos dados e limitações observados estão descritos em [Comportamento atual](docs/current-behavior.md).