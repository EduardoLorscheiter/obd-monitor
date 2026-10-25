# Verificação e testes

Este documento é a fonte única dos comandos e limites de verificação. Consulte [desenvolvimento](development.md) para as regras de implementação e [comportamento atual](current-behavior.md) para os fluxos existentes que uma alteração pode afetar.

## Cobertura atual observada

- `app/src/test/.../SupportedPidsCommandTest.java` verifica leitura da máscara básica e identificação do PID `0x20` com bit ligado ou desligado.
- `ExampleUnitTest` é apenas o teste de soma gerado pelo modelo; não cobre comportamento do aplicativo.
- `app/src/androidTest/.../ExampleInstrumentedTest.java` verifica o package name do contexto Android; não valida Bluetooth ou telas.
- Não há cobertura automatizada identificada para as fórmulas dos demais PIDs, `ObdScheduler`, ciclo Bluetooth/ELM327 ou apresentação de estados indisponíveis.

Confirme os testes presentes no checkout antes de assumir que essa lista continua atualizada.

## Comandos no Windows

Com Android SDK configurado:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
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