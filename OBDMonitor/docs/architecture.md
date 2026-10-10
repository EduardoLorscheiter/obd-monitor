# Arquitetura e mapa do código

Este documento descreve a estrutura observada no repositório. Ele não pressupõe componentes ainda não implementados; atualize-o quando uma mudança consolidar uma nova decisão arquitetural.

## Plataforma

O repositório contém um módulo Android (`app`), configurado com Android Gradle Plugin por catálogo de versões, Java 11 e recursos Android tradicionais. A configuração de SDK e dependências está em `app/build.gradle.kts` e `gradle/libs.versions.toml`.

## Fluxo funcional

O fluxo conceitual é: seleção/conexão do adaptador Bluetooth, inicialização e troca de comandos com ELM327, interpretação das respostas OBD-II, agendamento de consultas e apresentação no painel. Os detalhes concretos de navegação e coordenação devem ser confirmados no código antes de alterar o fluxo.

## Pacotes

| Pacote          | Responsabilidade observada                                                   |
|-----------------|------------------------------------------------------------------------------|
| `bluetooth`     | Conexão e gerenciamento do transporte Bluetooth, listeners e erros.          |
| `elm327`        | Gerenciamento/protocolo do adaptador, comandos de inicialização e callbacks. |
| `model`         | Modelos de domínio, incluindo dispositivos.                                  |
| `obd.commands`  | Comandos OBD-II tipados por PID e parsing de resposta.                       |
| `obd.scheduler` | Tarefas PID, prioridade e seleção das consultas prontas.                     |
| `ui`            | Atividades Android: splash, conexão, painel, configurações e base comum.     |
| `utils`         | Utilitários de interface e configurações.                                    |
| `view`          | Componentes visuais customizados para métricas do painel.                    |

Recursos como layouts, strings, temas e drawables ficam em `app/src/main/res`; permissões e atividades ficam no manifesto do módulo.

## Limites entre responsabilidades

- A camada Bluetooth deve cuidar do transporte e do estado da conexão, sem interpretar fórmulas de PIDs.
- A camada ELM327 deve lidar com enquadramento/respostas e particularidades do adaptador; não deve assumir que toda resposta é uma medição válida.
- Comandos OBD devem declarar PID, quantidade/formato esperado e conversão conforme a especificação aplicável.
- O agendador deve decidir quando consultar, sem controlar diretamente widgets Android.
- Atividades e views devem apresentar estado e encaminhar interações, evitando concentrar protocolo ou parsing.

Esses limites orientam mudanças futuras; ao descobrir exceções necessárias, documente a razão e mantenha a exceção localizada.

## Android e Bluetooth

O manifesto declara permissões Bluetooth legadas e as permissões `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`, além de permissões de localização para compatibilidade com Android antigo. O `minSdk` atual é 29. Mudanças em descoberta/conexão devem revisar permissões em runtime, versões Android suportadas, recusas e revogações; não basta alterar o manifesto.

## Dados OBD-II

Considere que a ECU pode não suportar um PID e que adaptadores podem devolver respostas negativas, truncadas, atrasadas ou agregadas de vários módulos. Validar cabeçalhos, tamanho e caracteres antes de converter bytes. Fórmulas, unidades, limites e precisão devem vir de referência técnica confiável e ser cobertos por casos de parsing.
