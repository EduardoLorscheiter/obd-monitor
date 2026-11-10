# Verificação e testes

Este documento é a fonte única dos comandos e limites de verificação. Consulte [desenvolvimento](development.md) para as regras de implementação e [comportamento atual](current-behavior.md) para os fluxos existentes que uma alteração pode afetar.

## Cobertura atual observada

- `app/src/test/.../SupportedPidsCommandTest.java` verifica leitura da máscara básica e identificação do PID `0x20` com bit ligado ou desligado.
- `app/src/test/.../BluetoothPermissionsTest.java` verifica as permissões do fluxo de pareados nas APIs 29, 30, 31 e 36.
- `app/src/test/.../BluetoothConnectionTest.java` cobre desconexão fora do lock do transporte, concorrência com o lock do gerenciador, fechamento único dos fluxos, acúmulo de erros sem callbacks e cancelamento de tentativa pendente. Os testes usam recursos em memória, sem adaptador físico.
- `VoltageCommandTest` cobre fórmula, limites, respostas inválidas após leitura válida e o bloco de suporte `0140`. `Elm327VoltageTest` cobre parsing e indisponibilidade de `ATRV`.
- Nos testes instrumentados, `VoltageMonitoringTest` verifica descoberta, agendamento e leitura alternativa com transporte em memória. `DashboardActivityTest` verifica tensão indisponível e liberação/retomada do alerta ao navegar entre painel e configurações.
- `ExampleUnitTest` é apenas o teste de soma gerado pelo modelo; não cobre comportamento do aplicativo.
- `app/src/androidTest/.../ExampleInstrumentedTest.java` verifica o package name do contexto Android; não valida Bluetooth ou telas.
- Não há cobertura automatizada identificada para as fórmulas dos demais PIDs, `ObdScheduler`, inicialização ELM327 completa ou apresentação de estados indisponíveis nas demais métricas. A solicitação de permissões e o funcionamento real do Bluetooth ainda precisam de validação em Android.

Confirme os testes presentes no checkout antes de assumir que essa lista continua atualizada.

## Comandos no Windows

Com Android SDK configurado:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

O último comando requer dispositivo ou emulador conectado. Execute apenas as verificações pertinentes ao escopo solicitado e informe os comandos executados e seus resultados. Se o ambiente bloquear uma checagem, identifique a limitação; não a descreva como sucesso.

## Escolha de verificação

| Área alterada                                   | Verificação adequada                                                                                                                                                    |
|-------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Fórmula, parsing ou máscara de PID              | Testes unitários determinísticos para resposta válida, limites, tamanho, caracteres inválidos, cabeçalho errado e não suporte.                                          |
| Scheduler                                       | Testes para suporte, prontidão por intervalo, ordenação por prioridade e atualização do horário de execução.                                                            |
| Transporte Bluetooth/ELM327                     | Testes isolados para estados, falha, fechamento, cancelamento e callbacks; compilação Android para integração. Testes unitários não devem depender de adaptador físico. |
| Activity, recursos, permissões ou ciclo de vida | Compilação Android e testes instrumentados pertinentes; valide estados de permissão/conexão em dispositivo ou emulador quando necessário.                               |
| Alteração apenas documental                     | Revise os Markdown e seus links; não é necessário executar testes de aplicação.                                                                                         |

Compilação não comprova fórmulas OBD nem o comportamento real de um adaptador. Relate limites de SDK, emulador ou dispositivo que afetarem a checagem.

## Validação manual das permissões Bluetooth

- Nas APIs 29–30, abrir a tela de conexão e verificar a listagem de pareados e o pedido de ativação do rádio, sem solicitar localização.
- Na API 31 ou posterior, instalar sem permissões concedidas e abrir a tela de conexão. Conferir a solicitação de dispositivos próximos e o carregamento dos pareados após conceder acesso.
- Recusar o acesso e conferir a mensagem, a lista oculta e a possibilidade de tentar novamente. Após uma recusa permanente, usar a ação de configurações, conceder acesso e retornar à tela.
- Recriar a Activity durante uma solicitação e verificar que o diálogo não é duplicado. Revogar o acesso nas configurações do Android e reabrir a tela para conferir o estado sem permissão.
