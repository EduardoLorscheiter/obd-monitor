# Comportamento atual

Registro dos fluxos observados no código de produção. A arquitetura dos componentes está em [arquitetura](architecture.md), e as regras para contribuir estão em [desenvolvimento](development.md).

## Inicialização e navegação

- `SplashActivity` apresenta a marca, anima o logo por 750 ms e abre `DashboardActivity` após 1.500 ms. A splash é removida da pilha.
- As quatro Activities declaradas no manifesto usam orientação paisagem. O painel e a tela de conexão mantêm a tela ligada enquanto estão visíveis.
- O painel abre as telas de conexão e configurações. Voltar do painel chama `Elm327Manager.disconnect()` e encerra o painel. Voltar da tela de conexão apenas fecha essa tela.
- Ao iniciar, `BaseActivity` verifica o estado Bluetooth e, se o `BluetoothManager` ainda tiver um dispositivo conectado, pede ao `Elm327Manager` que retome a sessão. Essa retomada evita reinicialização quando a geração da sessão continua ativa.

## Bluetooth e conexão com o ELM327

1. A tela de conexão solicita as permissões Bluetooth necessárias quando ainda não foram concedidas. Com acesso autorizado, apresenta o estado do Bluetooth. Se o rádio estiver desligado, o controle solicita ao Android que o habilite. O aplicativo não o desliga pela própria tela; o controle exibe uma mensagem orientando a pessoa a usar as configurações do Android.
2. Quando o Bluetooth está ligado, a tela carrega a lista de dispositivos **já pareados** no Android. Não há busca de dispositivos nem pareamento dentro do aplicativo; pareie o adaptador nas configurações do sistema antes de selecioná-lo.
3. Selecionar um dispositivo inicia uma conexão Bluetooth clássico RFCOMM usando o perfil serial SPP. Uma conexão existente com outro dispositivo é encerrada antes da nova tentativa. Tentativas repetidas para o mesmo dispositivo já conectado ou em conexão são ignoradas.
4. Depois da conexão Bluetooth, `BaseActivity` inicia a inicialização ELM327 em segundo plano. A `ConnectionActivity` define `RESULT_OK` quando o `Elm327Manager` conclui a inicialização OBD, mas continua aberta. Ao sair dela com Voltar, o painel recebe esse resultado e exibe uma mensagem de sucesso.
5. Bluetooth, ECU e protocolo são estados distintos no painel. Falhas são encaminhadas por callbacks e exibidas como mensagens breves.

A conexão RFCOMM tenta conectar até duas vezes, com intervalo de um segundo entre tentativas. Uma tentativa substituída é cancelada e pode aguardar 750 ms para liberar recursos. Cada comando termina quando chega o prompt `>` do ELM327 ou quando se esgota o prazo de leitura configurado de cinco segundos. A leitura usa `InputStream.read()` bloqueante sem configurar timeout no socket; se nenhum byte chegar, o prazo não garante que a chamada bloqueada retorne exatamente em cinco segundos. Falhas de comunicação notificam erro e encerram a conexão.

No encerramento de uma sessão, o transporte protege o fechamento e a limpeza dos recursos com seu lock, acumula eventuais erros e só então libera o lock e publica os callbacks. Os eventos conservam o identificador da sessão encerrada. Assim, um callback que aguarda o gerenciador não mantém o lock do transporte e não bloqueia uma desconexão concorrente solicitada pelo gerenciador.

### Permissões Bluetooth observadas

- No Android 10 e 11 (APIs 29–30), o fluxo de dispositivos pareados usa `BLUETOOTH` e `BLUETOOTH_ADMIN`, declaradas com `maxSdkVersion="30"`. Não há solicitação em runtime nem permissão de localização, pois o aplicativo não faz descoberta de dispositivos.
- No Android 12 (API 31) ou posterior, `BluetoothPermissions` centraliza a exigência de `BLUETOOTH_CONNECT` e `BLUETOOTH_SCAN`. A primeira permite acessar os pareados e conectar; a segunda é necessária para chamar `cancelDiscovery()` antes da conexão, mesmo sem iniciar uma busca. O manifesto declara `neverForLocation` para `BLUETOOTH_SCAN`.
- A tela de conexão solicita essas permissões pelo contrato `RequestMultiplePermissions`. Enquanto falta acesso, apresenta “Permissão necessária”, mantém o controle do rádio desabilitado e oculta os dispositivos. Após a concessão, consulta o estado do rádio e carrega os pareados automaticamente.
- Uma recusa mantém a tela disponível e apresenta uma ação para tentar novamente. Se o Android não permitir outro diálogo, a ação abre as configurações do aplicativo, por iniciativa do usuário. Ao retornar, o estado é consultado novamente. Uma recriação da Activity preserva a indicação de solicitação em andamento para evitar diálogos duplicados.
- As demais telas não solicitam permissões. A sincronização inicial evita consultar o adaptador sem acesso, e o gerenciador continua validando as duas permissões antes das operações e tratando revogação durante chamadas ao sistema.

Requisitos conferidos na [documentação de permissões Bluetooth](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions) e na [referência de `cancelDiscovery()`](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#cancelDiscovery()).

## Inicialização ELM327 e OBD

Após conectar, `Elm327Manager` envia esta sequência ao adaptador:

1. `ATZ` para reiniciar; aguarda 1.000 ms.
2. `ATE0`, `ATL0`, `ATS0` e `ATH0` para desativar eco, quebras de linha, espaços e cabeçalhos.
3. `ATSP0` para seleção automática do protocolo; aguarda 5.000 ms.
4. Consulta `0100`, que precisa produzir uma resposta válida para a sessão ser considerada pronta.
5. Se a máscara de `0100` anunciar suporte ao PID `0x20`, consulta `0120` para o bloco `0x21`–`0x40`.
6. Se `0120` anunciar suporte ao PID `0x40`, consulta `0140` para identificar suporte ao PID de tensão `0x42`.
7. Lê o protocolo identificado com `ATDP` e começa o monitoramento.

Uma resposta AT é aceita quando contém a identificação ELM327 ou `OK`. Falha de inicialização ou ausência de resposta válida a `0100` resulta em erro, não em uma sessão OBD pronta. Os comandos terminam com carriage return; a resposta é normalizada para maiúsculas, sem prompt, quebras de linha ou espaços excedentes.

### PIDs, fórmulas e agendamento

As consultas de leitura usam o Serviço 01. Os intervalos são mínimos entre execuções elegíveis; tarefas prontas são ordenadas por prioridade (HIGH antes de MEDIUM, depois LOW). O ciclo de monitoramento volta a verificar tarefas a cada 10 ms. Os intervalos e prioridades são os configurados atualmente em `Elm327Manager`.

| Dado do painel                            |           PID | Conversão do dado `A` (e `B`, quando indicado)                    | Intervalo | Prioridade |
|-------------------------------------------|--------------:|-------------------------------------------------------------------|----------:|------------|
| Rotação                                   |          `0C` | `((A × 256) + B) / 4` RPM                                         |    100 ms | HIGH       |
| Velocidade                                |          `0D` | `A` km/h                                                          |    100 ms | HIGH       |
| Borboleta                                 |          `11` | `(A × 100) / 255` %                                               |    300 ms | MEDIUM     |
| MAP                                       |          `0B` | `A` kPa                                                           |    300 ms | MEDIUM     |
| Temperatura do motor                      |          `05` | `A - 40` °C                                                       |  1.000 ms | LOW        |
| Temperatura de admissão                   |          `0F` | `A - 40` °C                                                       |  1.000 ms | LOW        |
| Tensão da ECU ou alimentação do adaptador | `42` / `ATRV` | `((A × 256) + B) / 1000` V para o PID; decimal em volts para ATRV |  2.000 ms | LOW        |
| Combustível                               |          `2F` | `(A × 100) / 255` %                                               |  2.000 ms | LOW        |

As tarefas de `0x05`, `0x0B`, `0x0C`, `0x0D` e `0x0F`/`0x11` usam a máscara retornada por `0100`. Combustível `0x2F` usa a máscara de `0120`; portanto, só é marcado suportado se `0100` anunciar `0x20` e a consulta seguinte anunciar `0x2F`. Tarefa não suportada não é consultada.

**Comportamento especial da tensão:** a tarefa é habilitada após a descoberta OBD mesmo sem suporte ao PID `0x42`. Quando `0140` anuncia esse PID, a leitura tenta primeiro a ECU; sem suporte ou com resposta inválida, usa `ATRV`. O suporte ao PID e a habilitação da tarefa são estados separados. Respostas inválidas do PID não reutilizam valores anteriores. Se ambas as fontes falharem, o callback recebe `Double.NaN` e o cartão exibe `--` em cor neutra. Zero em resposta válida continua sendo uma medição válida. `ATRV` mede a alimentação do adaptador, enquanto `0142` informa a tensão da ECU. Formatos conferidos no [datasheet ELM327](https://www.elmelectronics.com/wp-content/uploads/2017/01/ELM327DS.pdf) e na [tabela de PIDs da CSS Electronics](https://www.csselectronics.com/pages/obd2-pid-table-on-board-diagnostics-j1979).

### Parsing e significado de valores

`ObdCommand` procura o cabeçalho de resposta `41` seguido do PID e exige o comprimento configurado antes de converter os bytes hexadecimais. Os comandos de medição, exceto tensão, guardam o último valor convertido e retornam esse valor anterior para respostas ausentes, curtas ou com hexadecimal inválido. Esses comandos e seus widgets iniciam em zero. Tensão usa a semântica de indisponibilidade descrita acima e seu cartão inicia em `--`.

Consequentemente, nas demais métricas o estado exibido **não distingue** uma leitura real igual a zero de uma resposta inválida anterior à primeira leitura válida, nem identifica quando um valor preservado ficou antigo. Uma tarefa marcada não suportada simplesmente não atualiza o widget; seu valor inicial permanece. Não trate esses zeros como uma indicação explícita de dado indisponível.

## Painel

O painel apresenta rotação, velocidade, borboleta, MAP, temperaturas do motor e da admissão, tensão e nível de combustível. O indicador de rotação limita o valor visual ao intervalo entre zero e o RPM máximo configurado. O alerta visual pisca quando a leitura alcança ou ultrapassa o RPM inicial do alerta e a Activity está retomada. Ao pausar ou destruir a tela, o animador é cancelado, seus listeners são removidos e o overlay volta à transparência zero. Callbacks atrasados não reiniciam o alerta enquanto a tela estiver inativa; ao retomar, o último RPM recebido é reavaliado.

As cores dos valores são classificações visuais codificadas na `DashboardActivity` quando o callback de cada métrica é recebido:

- Temperatura do motor: abaixo de 60 °C frio; até 95 °C normal; até 105 °C aviso; acima disso quente.
- Temperatura de admissão: abaixo de 20 °C frio; até 40 °C normal; até 60 °C aviso; acima disso quente.
- Tensão exibida: abaixo de 11,8 V baixa; de 11,8 V a menos de 12,6 V aviso; até 14,7 V normal; acima disso alta.

As faixas de cor só mudam a apresentação, não constituem diagnóstico mecânico. A faixa de tensão se aplica à fonte que forneceu a leitura válida; a indicação indisponível usa cor neutra. O painel é mantido ligado enquanto visível.

## Configurações

A tela agrupa as opções em Aparência, Veículo e Alerta. As alterações só são persistidas ao acionar Salvar; valores inválidos mantêm a tela aberta, selecionam a seção correspondente e mostram erro no campo.

| Opção                   | Valores e padrão atuais                                                                |
|-------------------------|----------------------------------------------------------------------------------------|
| Tema                    | Sistema (padrão), Claro ou Escuro                                                      |
| Idioma                  | Português (padrão) ou Inglês                                                           |
| RPM máximo do veículo   | 1.000–20.000 rpm; padrão 7.000 rpm                                                     |
| Início do alerta de RPM | mínimo 1.000 rpm e máximo igual ao RPM do veículo; padrão metade do máximo configurado |

As preferências ficam em `SharedPreferences` privadas por `SettingsUtils`. Ao retornar às telas, `BaseActivity` reaplica tema e idioma ao contexto. O painel reaplica o limite de RPM e trata alterações de tema/idioma recebidas da tela de configurações.

## O que ainda não existe

O código atual não implementa descoberta ou pareamento Bluetooth na própria aplicação, leitura ou limpeza de códigos de falha, freeze frame, nem gravação de histórico de medições. As consultas implementadas são as indicadas na tabela acima, com a particularidade de tensão descrita nesta página.
