# Verificação e testes

O módulo declara JUnit para testes locais e AndroidX Test/Espresso para testes instrumentados. A cobertura atual do repositório é limitada; confirme os testes existentes antes de assumir que um fluxo está coberto.

## Comandos

No Windows, com Android SDK configurado:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

O teste instrumentado requer dispositivo/emulador conectado. Execute verificações relacionadas à mudança e informe claramente qualquer comando não executado ou bloqueado por ambiente.

## O que cobrir

- **Parser OBD:** respostas válidas, limites de bytes, entrada nula/curta, caracteres inválidos, cabeçalho inesperado e resposta negativa/não suportada.
- **Agendador:** suporte do PID, prioridade, prontidão por intervalo e atualização do horário de execução.
- **Estado de conexão:** sucesso, falha, encerramento, timeout, cancelamento e reconexão quando aplicável.
- **Interface:** estados de permissão e conexão, ausência de dados e atualização/limpeza de listeners conforme ciclo de vida.

Prefira testes unitários determinísticos para fórmulas e regras de agendamento. Para dependências de Bluetooth, isole o comportamento testável e não dependa de um adaptador físico em testes unitários.

## Critério de relato

Relate o comando executado e seu resultado. Diferencie falha de código de limitação ambiental, como falta de SDK, emulador ou dispositivo. Não trate compilação como prova de correção de parsing ou comportamento Bluetooth.
