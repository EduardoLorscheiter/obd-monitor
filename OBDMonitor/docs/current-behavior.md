# Comportamento atual

Referência dos fluxos e parâmetros implementados. Descreve o código do checkout; a disponibilidade das medições depende do veículo e do adaptador. Para localizar componentes, consulte [arquitetura](architecture.md).

## Conectar e usar

1. Conecte ao veículo um adaptador ELM327 compatível com Bluetooth clássico SPP e faça o pareamento nas configurações do Android.
2. Abra o aplicativo. A splash apresenta a marca e a versão, anima o logo por 750 ms e abre o painel após 1.500 ms, saindo da pilha de navegação.
3. No painel, abra a tela de conexão. Conceda as permissões solicitadas e, se necessário, use o controle da tela para pedir ao Android que ligue o Bluetooth.
4. Selecione um dispositivo da lista de **já pareados**. O aplicativo conecta o transporte e inicializa o ELM327 em segundo plano.
5. Após a inicialização OBD, volte ao painel para acompanhar as leituras. A tela de conexão permanece aberta após o sucesso; ela define `RESULT_OK`, e o painel exibe a mensagem de sucesso ao receber esse resultado.

O controle Bluetooth não desliga o rádio: orienta a usar as configurações do Android. Não há descoberta nem pareamento dentro do aplicativo. Adaptadores BLE, Wi-Fi e USB não têm transporte implementado.

As quatro telas usam orientação paisagem. Painel e conexão mantêm a tela ligada enquanto visíveis. O painel também abre as configurações. Voltar do painel desconecta a sessão e encerra a tela; voltar da conexão apenas fecha essa tela. A navegação entre telas pode reutilizar a sessão Bluetooth já inicializada, conforme descrito em [arquitetura](architecture.md), tópico **"Sessão e ciclo de vida"**.

Implementação: [Activities](../app/src/main/java/com/eduardo/obdmonitor/ui) e [manifesto](../app/src/main/AndroidManifest.xml).

## Permissões Bluetooth

| Android                   | Permissões usadas pelo fluxo de pareados                                                                                                        |
|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| 10 e 11 (APIs 29–30)      | `BLUETOOTH` e `BLUETOOTH_ADMIN`, declaradas com `maxSdkVersion="30"`; sem solicitação em runtime.                                               |
| 12 ou posterior (API 31+) | `BLUETOOTH_CONNECT` e `BLUETOOTH_SCAN`, solicitadas em runtime como acesso a dispositivos próximos. `SCAN` tem `neverForLocation` no manifesto. |

Não há permissão de localização. Embora o aplicativo não inicie descoberta, exige `BLUETOOTH_SCAN` para chamar `cancelDiscovery()` antes da conexão. Os requisitos externos estão na [documentação de permissões Bluetooth](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions) e na [referência de `cancelDiscovery()`](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#cancelDiscovery()).

A tela de conexão concentra a solicitação de acesso. Sem permissão, mostra “Permissão necessária”, desabilita o controle do rádio e oculta os dispositivos. Após a concessão, consulta o rádio e carrega os pareados. Uma recusa permite tentar novamente; quando o sistema não permite outro diálogo, a ação abre as configurações do aplicativo. Ao retornar, o acesso é reavaliado. Uma recriação da tela preserva a indicação de solicitação em andamento para evitar pedidos duplicados.

As demais telas não abrem diálogos de permissão. A sincronização inicial evita acesso ao adaptador sem autorização, e o gerenciador trata a revogação durante operações.

Implementação: [BluetoothPermissions](../app/src/main/java/com/eduardo/obdmonitor/bluetooth/BluetoothPermissions.java) e [ConnectionActivity](../app/src/main/java/com/eduardo/obdmonitor/ui/ConnectionActivity.java).

## Conexão e falhas de transporte

O transporte é Bluetooth clássico RFCOMM/SPP. Selecionar outro dispositivo encerra a conexão anterior. Seleções repetidas do dispositivo já conectado ou em conexão são ignoradas; após uma falha final, novas tentativas para o mesmo dispositivo são suprimidas por dois segundos.

A camada RFCOMM faz até duas tentativas, com um segundo entre elas. Substituir uma tentativa cancela a anterior e pode aguardar até 750 ms para liberar recursos.

A leitura termina ao encontrar o prompt `>`, o fim do fluxo ou o prazo configurado de cinco segundos. Esse prazo é verificado entre chamadas bloqueantes de `InputStream.read()`; sem timeout no socket, ele **não garante** retorno em cinco segundos se nenhum byte chegar. Uma resposta vazia retorna falha sem fechar por si só o transporte. Exceções de I/O ou segurança no envio/leitura encerram a conexão.

Bluetooth conectado, ECU pronta e protocolo são estados distintos. As telas exibem os erros encaminhados pelos listeners, mas o callback de erro de transporte no gerenciador só trata tentativas em andamento; não há garantia de mensagem para toda resposta inválida durante uma sessão ativa.

Quando o transporte efetivamente se perde, o gerenciador OBD interrompe o monitoramento e limpa seus estados. O painel descarta medições executadas enquanto a ECU está desconectada, inclusive callbacks enfileirados, e restaura os valores iniciais descritos no tópico **"Painel"** deste documento. Uma inicialização que termina sem dispositivo conectado não publica a ECU como pronta.

Implementação: [BluetoothConnection](../app/src/main/java/com/eduardo/obdmonitor/bluetooth/BluetoothConnection.java), [BluetoothManager](../app/src/main/java/com/eduardo/obdmonitor/bluetooth/BluetoothManager.java) e [Elm327Manager](../app/src/main/java/com/eduardo/obdmonitor/elm327/Elm327Manager.java).

## Inicialização ELM327

A sequência implementada é:

1. `ATZ` para reiniciar o adaptador; espera de 1.000 ms.
2. `ATE0`, `ATL0`, `ATS0` e `ATH0` para desativar eco, quebras de linha, espaços e cabeçalhos.
3. `ATSP0` para seleção automática do protocolo; espera de 5.000 ms.
4. `0100` para obter a máscara dos PIDs `0x01`–`0x20`.
5. `0120`, se a máscara básica anunciar `0x20`, para consultar o bloco `0x21`–`0x40`.
6. `0140`, se o bloco anterior anunciar `0x40`, para verificar o suporte à tensão pelo PID `0x42`.
7. `ATDP` para ler o protocolo; publicação do estado pronto e início do monitoramento.

Os comandos recebem `\r` ao final. As respostas são normalizadas para maiúsculas, sem prompt, com quebras de linha substituídas por espaços e espaços consecutivos reduzidos.

Na configuração AT, a resposta é aceita se contiver `ELM327` ou `OK`. A descoberta básica falha quando o parser de `0100` devolve `null`; uma resposta atual inválida pode, porém, reutilizar a máscara de sessão anterior, conforme descrito no tópico **"Validade e indisponibilidade dos dados"** deste documento. Os blocos adicionais são opcionais. O conteúdo de `ATDP` não é validado como condição de sucesso. Uma falha reconhecida na inicialização encerra o transporte e publica erro.

Implementação: [Elm327Manager](../app/src/main/java/com/eduardo/obdmonitor/elm327/Elm327Manager.java) e [Elm327Commands](../app/src/main/java/com/eduardo/obdmonitor/elm327/Elm327Commands.java). A semântica dos comandos AT é descrita no [datasheet do ELM327](https://www.elmelectronics.com/wp-content/uploads/2017/01/ELM327DS.pdf).

## PIDs e agendamento

As medições OBD usam o Serviço 01. `A` e `B` representam bytes de dados convertidos de hexadecimal. Os valores abaixo são os parâmetros da implementação.

| Medição                                 | PID/comando   | Conversão e unidade                                              | Intervalo configurado | Prioridade |
|-----------------------------------------|---------------|------------------------------------------------------------------|-----------------------|------------|
| Rotação                                 | `0C`          | `((A × 256) + B) / 4` rpm                                        | 100 ms                | HIGH       |
| Velocidade                              | `0D`          | `A` km/h                                                         | 100 ms                | HIGH       |
| Borboleta                               | `11`          | `(A × 100) / 255` %                                              | 300 ms                | MEDIUM     |
| Pressão absoluta do coletor (MAP)       | `0B`          | `A` kPa                                                          | 300 ms                | MEDIUM     |
| Temperatura do líquido de arrefecimento | `05`          | `A - 40` °C                                                      | 1.000 ms              | LOW        |
| Temperatura do ar de admissão           | `0F`          | `A - 40` °C                                                      | 1.000 ms              | LOW        |
| Tensão                                  | `42` / `ATRV` | `((A × 256) + B) / 1000` V pelo PID; decimal em volts por `ATRV` | 2.000 ms              | LOW        |
| Nível de combustível                    | `2F`          | `(A × 100) / 255` %                                              | 2.000 ms              | LOW        |

RPM, borboleta e combustível usam divisão inteira, descartando a parte fracionária. Tensão usa ponto flutuante; no painel, aparece com uma casa decimal.

A máscara de `0100` habilita as tarefas do bloco básico; a de `0120` habilita combustível. Tarefas não suportadas são excluídas das leituras. Tensão é habilitada após a descoberta mesmo sem suporte ao PID, pois pode usar `ATRV`.

O scheduler ordena as tarefas elegíveis por HIGH, MEDIUM e LOW, preservando a ordem de cadastro nos empates. Cada ciclo coleta um lote, executa as consultas em sequência e registra o horário inicial do ciclo para as tarefas executadas; depois espera 10 ms. Os intervalos são critérios de elegibilidade, **não uma frequência garantida**: o tempo das consultas também compõe o ciclo.

Implementação: [comandos OBD](../app/src/main/java/com/eduardo/obdmonitor/obd/commands), [scheduler](../app/src/main/java/com/eduardo/obdmonitor/obd/scheduler) e configuração das tarefas no [Elm327Manager](../app/src/main/java/com/eduardo/obdmonitor/elm327/Elm327Manager.java).

## Validade e indisponibilidade dos dados

`ObdCommand` procura a primeira ocorrência textual do cabeçalho `41` seguido do PID e extrai a quantidade de bytes esperada. Os comandos convertem esses bytes e tratam falhas de conversão. Não há interpretação completa de mensagens negativas ou de respostas de múltiplas ECUs.

### Valores preservados

Os comandos de medição, exceto tensão, começam em zero e preservam o último valor convertido diante de resposta ausente, curta, com cabeçalho incorreto ou hexadecimal inválido. Esses objetos são reutilizados pelo singleton: a desconexão limpa os estados e o suporte das tarefas, mas **não apaga seus valores anteriores**. Em uma nova sessão, uma resposta inválida pode devolver uma medição da sessão anterior.

As máscaras de suporte `0100` e `0120` também preservam a última resposta válida entre sessões. Por isso, uma nova inicialização pode usar suporte antigo quando a consulta atual falha. O comando de `0140` é criado a cada descoberta e não compartilha esse cache entre inicializações.

A interface não sinaliza a idade desses valores nem distingue zero real de zero inicial publicado após resposta inválida. Uma tarefa não suportada não envia medições; seus componentes permanecem no estado inicial até receber atualização.

### Tensão

Se `0140` anunciou suporte a `0x42`, cada leitura tenta primeiro a ECU. Sem suporte ou com resposta inválida, usa `ATRV`. A tarefa permanece habilitada independentemente do suporte ao PID.

O PID informa tensão da ECU; `ATRV` informa alimentação do adaptador. O cartão “Bateria” pode representar qualquer uma dessas fontes e não as diferencia visualmente. O parser de `ATRV` aceita uma ou duas casas antes do ponto, uma casa decimal e sufixo `V`, com espaços opcionais antes da unidade.

Tensão não reutiliza valor antigo em falha. Se nenhuma fonte produzir uma leitura válida, publica `Double.NaN`; o cartão mostra `--` em cor neutra. Zero em resposta válida continua sendo uma medição válida.

Implementação: [ObdCommand](../app/src/main/java/com/eduardo/obdmonitor/obd/commands/ObdCommand.java), [SupportedPidsCommand](../app/src/main/java/com/eduardo/obdmonitor/obd/commands/SupportedPidsCommand.java), [VoltageCommand](../app/src/main/java/com/eduardo/obdmonitor/obd/commands/VoltageCommand.java) e [Elm327Commands](../app/src/main/java/com/eduardo/obdmonitor/elm327/Elm327Commands.java).

## Painel

O painel apresenta as medições da tabela de PIDs, os indicadores Bluetooth/ECU e o protocolo. Inicialmente e após a notificação de ECU desconectada, os cartões e o protocolo mostram `--`, RPM e combustível ficam em zero e as cores dos cartões voltam ao padrão.

Tocar em qualquer uma das oito medições abre uma explicação sobre seu significado e unidade, mesmo sem conexão com o veículo. Os cartões, o indicador de RPM e o nível de combustível apresentam essa ajuda por meio de `DialogUtils`, com textos em português e inglês. Os diálogos de ajuda do painel e das configurações usam a mesma paleta de cinza dos cartões, com texto claro e botão de confirmação.

O indicador de RPM limita o valor numérico e visual entre zero e o máximo configurado. O alerta pisca quando a ECU está conectada, o painel está retomado e o RPM recebido alcança o início do alerta. Ao pausar ou destruir a tela, a animação é cancelada, os listeners são removidos e o overlay volta à transparência zero. Ao retomar, o último RPM recebido é reavaliado.

As faixas abaixo são classificações visuais fixas; não constituem diagnóstico mecânico.

| Medição                                 | Faixas de apresentação                                                                                  |
|-----------------------------------------|---------------------------------------------------------------------------------------------------------|
| Temperatura do líquido de arrefecimento | Frio abaixo de 60 °C; normal de 60 a 95 °C; aviso acima de 95 até 105 °C; quente acima de 105 °C.       |
| Temperatura de admissão                 | Frio abaixo de 20 °C; normal de 20 a 40 °C; aviso acima de 40 até 60 °C; quente acima de 60 °C.         |
| Tensão disponível                       | Baixa abaixo de 11,8 V; aviso de 11,8 a menos de 12,6 V; normal de 12,6 a 14,7 V; alta acima de 14,7 V. |

Implementação: [DashboardActivity](../app/src/main/java/com/eduardo/obdmonitor/ui/DashboardActivity.java), [ShiftLightView](../app/src/main/java/com/eduardo/obdmonitor/view/ShiftLightView.java) e [layout dos cartões](../app/src/main/res/layout/view_metric.xml).

## Configurações

A tela agrupa as opções em Aparência, Veículo e Alertas. Salvar valida todos os campos antes de persistir e fecha a tela. Valores inválidos mantêm a tela aberta, selecionam a seção correspondente e mostram erro no campo. Voltar descarta as alterações não salvas.

| Opção                 | Valores                           | Padrão                                                                                           |
|-----------------------|-----------------------------------|--------------------------------------------------------------------------------------------------|
| Tema                  | Sistema, Claro ou Escuro          | Sistema                                                                                          |
| Idioma                | Português ou Inglês               | Português                                                                                        |
| RPM máximo do veículo | 1.000–20.000 rpm                  | 7.000 rpm                                                                                        |
| Início do alerta      | De 1.000 rpm ao máximo do veículo | Metade do máximo configurado, respeitando o mínimo de 1.000 rpm, quando ainda não há valor salvo |

As preferências ficam no arquivo privado `app_preferences`, gerenciado por `SettingsUtils`. As leituras de RPM limitam os valores à faixa aceita; um alerta salvo não acompanha automaticamente a metade de um novo RPM máximo.

Tema e idioma são aplicados por `BaseActivity.attachBaseContext()` ao criar a Activity. Ao receber o resultado de Salvar, o painel reaplica os limites de RPM; se o tema mudou, chama a aplicação do tema, e, se apenas o idioma mudou, recria a Activity. A aplicação do tema depende da mudança efetiva do modo noturno do AndroidX.

Implementação: [SettingsActivity](../app/src/main/java/com/eduardo/obdmonitor/ui/SettingsActivity.java), [SettingsUtils](../app/src/main/java/com/eduardo/obdmonitor/utils/SettingsUtils.java) e retorno das configurações em [DashboardActivity](../app/src/main/java/com/eduardo/obdmonitor/ui/DashboardActivity.java).

## Limites funcionais

O aplicativo não implementa leitura ou limpeza de códigos de falha, freeze frame, gravação de histórico nem exportação de medições. Os estados de suporte e os valores preservados têm as limitações descritas nesta referência. A compatibilidade com um adaptador ou veículo específico exige validação real; consulte [verificação](verification.md), tópico **"Cobertura e limites"**.
