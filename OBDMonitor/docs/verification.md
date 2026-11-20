# Verificação

Referência dos comandos, da cobertura existente e dos critérios para validar alterações. Antes de usar o wrapper, consulte [desenvolvimento](development.md), tópico **"Preparar o ambiente"**.

## Comandos e resultados

Execute na raiz deste projeto. No Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Em outros sistemas, use `./gradlew` com as mesmas tarefas. A última tarefa requer dispositivo ou emulador conectado e autorizado para depuração.

| Tarefa                      | Finalidade                                  | Saída esperada após execução bem-sucedida                   |
|-----------------------------|---------------------------------------------|-------------------------------------------------------------|
| `testDebugUnitTest`         | Executar testes locais na JVM.              | `app/build/reports/tests/testDebugUnitTest/index.html`      |
| `assembleDebug`             | Compilar e empacotar o aplicativo de debug. | `app/build/outputs/apk/debug/app-debug.apk`                 |
| `lintDebug`                 | Executar a análise estática Android.        | `app/build/reports/lint-results-debug.html`                 |
| `connectedDebugAndroidTest` | Executar testes instrumentados no Android.  | `app/build/reports/androidTests/connected/debug/index.html` |

Execute as verificações pertinentes ao escopo e relate os comandos, resultados e limitações. Um relatório antigo no disco não comprova a execução atual. Uma checagem bloqueada por SDK, rede ou ausência de dispositivo deve ser descrita como não executada ou incompleta, conforme o caso.

## Cobertura e limites

O inventário abaixo descreve os testes presentes; não é um registro de aprovação. Os testes ficam nos diretórios `app/src/test` e `app/src/androidTest`.

| Área                       | Testes                                                                                                                                                                                                  | O que verificam                                                                                                                                                                   |
|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Máscara de suporte         | [SupportedPidsCommandTest](../app/src/test/java/com/eduardo/obdmonitor/obd/commands/SupportedPidsCommandTest.java)                                                                                      | Leitura da máscara básica e bit de continuação para `0x20`, ligado e desligado.                                                                                                   |
| Permissões                 | [BluetoothPermissionsTest](../app/src/test/java/com/eduardo/obdmonitor/bluetooth/BluetoothPermissionsTest.java)                                                                                         | Seleção das permissões exigidas nas APIs 29, 30, 31 e 36.                                                                                                                         |
| Encerramento do transporte | [BluetoothConnectionTest](../app/src/test/java/com/eduardo/obdmonitor/bluetooth/BluetoothConnectionTest.java)                                                                                           | Callbacks de desconexão fora do lock, ordem de locks simulada entre gerenciador e transporte, fechamento único dos fluxos, acúmulo de erros e cancelamento de tentativa pendente. |
| Parsing de tensão          | [VoltageCommandTest](../app/src/test/java/com/eduardo/obdmonitor/obd/commands/VoltageCommandTest.java) e [Elm327VoltageTest](../app/src/test/java/com/eduardo/obdmonitor/elm327/Elm327VoltageTest.java) | Fórmula, limites, bloco de suporte, respostas inválidas sem reutilizar tensão antiga e formato de `ATRV`.                                                                         |
| Integração da tensão       | [VoltageMonitoringTest](../app/src/androidTest/java/com/eduardo/obdmonitor/elm327/VoltageMonitoringTest.java)                                                                                           | Descoberta de suporte, habilitação da tarefa sem PID da ECU e leitura alternativa por `ATRV`, usando transporte em memória e chamadas diretas às etapas.                          |
| Painel e ciclo de vida     | [DashboardActivityTest](../app/src/androidTest/java/com/eduardo/obdmonitor/ui/DashboardActivityTest.java)                                                                                               | Tensão indisponível, pausa/retomada do alerta e limpeza do painel/estado OBD por desconexão Bluetooth, incluindo medições atrasadas.                                              |

[ExampleUnitTest](../app/src/test/java/com/eduardo/obdmonitor/ExampleUnitTest.java) e [ExampleInstrumentedTest](../app/src/androidTest/java/com/eduardo/obdmonitor/ExampleInstrumentedTest.java) são testes do modelo inicial: verificam uma soma e o package name, respectivamente.

Não há testes específicos identificados para as fórmulas das demais medições, o scheduler, o loop periódico completo, a inicialização ELM327 de ponta a ponta ou a preservação de valores/máscaras entre sessões. A integração da tensão não executa o scheduler nem comprova a frequência de leitura. Confira o inventário ao adicionar, remover ou mudar o escopo de um teste.

Os testes de transporte usam recursos em memória. Compilação, testes locais e instrumentação com transporte simulado não comprovam compatibilidade com adaptadores ou ECUs reais. O diálogo de permissões e as operações reais de Bluetooth também exigem validação no Android.

## Escolher as verificações

| Alteração                           | Verificação proporcional                                                                                                                                                |
|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Fórmula, parsing ou máscara de PID  | Testes locais determinísticos para respostas válidas, limites, tamanho, caracteres inválidos, cabeçalho errado e não suporte.                                           |
| Scheduler                           | Testes de suporte, prontidão por intervalo, prioridade e atualização do horário de execução.                                                                            |
| Transporte ou coordenação ELM327    | Testes isolados de estado, falha, fechamento, cancelamento e callbacks; compilação para conferir integração. Use hardware quando a mudança depender do transporte real. |
| Activity, recursos ou ciclo de vida | Compilação, lint e testes instrumentados pertinentes; validação visual e de navegação quando necessária.                                                                |
| Permissões                          | Testes da política por API e os cenários manuais abaixo, em versões Android pertinentes.                                                                                |
| Apenas documentação                 | Revisão factual, estrutural e de links descrita abaixo; sem necessidade de testes de aplicação.                                                                         |

## Validação manual no Android

Para alterações de permissões, use os cenários correspondentes a [comportamento atual](current-behavior.md), tópico **"Permissões Bluetooth"**:

- Nas APIs 29–30, conferir pareados e pedido de ativação do rádio sem solicitar localização.
- Na API 31+, abrir a conexão sem acesso concedido, conferir a solicitação de dispositivos próximos e a lista após a concessão.
- Recusar, tentar novamente e testar a ação que abre as configurações após recusa permanente. Conceder acesso ali e retornar à tela.
- Recriar a Activity durante uma solicitação e verificar ausência de pedido duplicado. Revogar acesso nas configurações do Android e reabrir a tela.

Para mudanças que afetem a sessão ou o painel, conferir os [fluxos e estados documentados](current-behavior.md) em cenários pertinentes: conexão inicial, troca de dispositivo, falha e reconexão, perda do transporte, navegação entre painel/configurações e salvamento de preferências. Valide com adaptador e veículo reais os comportamentos que dependem de resposta da ECU. Registre versão Android, adaptador e cenário quando forem relevantes para interpretar o resultado.

## Revisar alterações documentais

- Conferir afirmações contra código, configuração e testes atuais, especialmente garantias de erro, cache, ciclo de vida e disponibilidade dos dados.
- Verificar os destinos dos links relativos, os títulos dos tópicos citados e as referências de entrada após mover ou remover conteúdo.
- Conferir títulos, tabelas, listas, blocos de código e diagramas, evitando cercas abertas, espaços finais e conteúdo sem propósito próprio.
- Revisar duplicações entre páginas conforme [desenvolvimento](development.md), tópico **"Manter a documentação"**.
- Usar `git diff --check` para detectar problemas de whitespace e confirmar que o diff contém apenas as alterações pretendidas. Essa checagem complementa a revisão de Markdown; não valida links ou afirmações por si só.
