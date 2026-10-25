# Comportamento atual

Registro dos fluxos observados no código de produção. A arquitetura dos componentes está em [arquitetura](architecture.md), e as regras para contribuir estão em [desenvolvimento](development.md).

## Inicialização e navegação

- `SplashActivity` apresenta a marca, anima o logo por 750 ms e abre `DashboardActivity` após 1.500 ms. A splash é removida da pilha.
- As quatro Activities declaradas no manifesto usam orientação paisagem. O painel e a tela de conexão mantêm a tela ligada enquanto estão visíveis.
- O painel abre as telas de conexão e configurações. Voltar do painel chama `Elm327Manager.disconnect()` e encerra o painel. Voltar da tela de conexão apenas fecha essa tela.
- Ao iniciar, `BaseActivity` verifica o estado Bluetooth e, se o `BluetoothManager` ainda tiver um dispositivo conectado, pede ao `Elm327Manager` que retome a sessão. Essa retomada evita reinicialização quando a geração da sessão continua ativa.

## Bluetooth e conexão com o ELM327

1. A tela de conexão apresenta o estado do Bluetooth. Se o rádio estiver desligado, o controle solicita ao Android que o habilite. O aplicativo não o desliga pela própria tela; o controle exibe uma mensagem orientando a pessoa a usar as configurações do Android.
2. Quando o Bluetooth está ligado, a tela carrega a lista de dispositivos **já pareados** no Android. Não há busca de dispositivos nem pareamento dentro do aplicativo; pareie o adaptador nas configurações do sistema antes de selecioná-lo.
3. Selecionar um dispositivo inicia uma conexão Bluetooth clássico RFCOMM usando o perfil serial SPP. Uma conexão existente com outro dispositivo é encerrada antes da nova tentativa. Tentativas repetidas para o mesmo dispositivo já conectado ou em conexão são ignoradas.
4. Depois da conexão Bluetooth, `BaseActivity` inicia a inicialização ELM327 em segundo plano. A `ConnectionActivity` define `RESULT_OK` quando o `Elm327Manager` conclui a inicialização OBD, mas continua aberta. Ao sair dela com Voltar, o painel recebe esse resultado e exibe uma mensagem de sucesso.
5. Bluetooth, ECU e protocolo são estados distintos no painel. Falhas são encaminhadas por callbacks e exibidas como mensagens breves.

A conexão RFCOMM tenta conectar até duas vezes, com intervalo de um segundo entre tentativas. Uma tentativa substituída é cancelada e pode aguardar 750 ms para liberar recursos. Cada comando termina quando chega o prompt `>` do ELM327 ou quando se esgota o prazo de leitura configurado de cinco segundos. A leitura usa `InputStream.read()` bloqueante sem configurar timeout no socket; se nenhum byte chegar, o prazo não garante que a chamada bloqueada retorne exatamente em cinco segundos. Falhas de comunicação notificam erro e encerram a conexão.

### Permissões Bluetooth observadas

O manifesto declara `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` e permissões de localização legadas. No Android 12 (API 31) ou posterior, `BluetoothManager` verifica `BLUETOOTH_CONNECT`. O código atual não solicita a permissão em runtime: se ela não estiver concedida, a operação informa erro de permissão. Não há fluxo próprio para pedir permissão de localização ou `BLUETOOTH_SCAN`, e a tela lista pareados em vez de fazer descoberta. Ao alterar esse fluxo, considere separadamente versões Android e os requisitos vigentes.

## Inicialização ELM327 e OBD

Após conectar, `Elm327Manager` envia esta sequência ao adaptador:

1. `ATZ` para reiniciar; aguarda 1.000 ms.
2. `ATE0`, `ATL0`, `ATS0` e `ATH0` para desativar eco, quebras de linha, espaços e cabeçalhos.
3. `ATSP0` para seleção automática do protocolo; aguarda 5.000 ms.
4. Consulta `0100`, que precisa produzir uma resposta válida para a sessão ser considerada pronta.
5. Se a máscara de `0100` anunciar suporte ao PID `0x20`, consulta `0120` para o bloco `0x21`–`0x40`.
6. Lê o protocolo identificado com `ATDP` e começa o monitoramento.

Uma resposta AT é aceita quando contém a identificação ELM327 ou `OK`. Falha de inicialização ou ausência de resposta válida a `0100` resulta em erro, não em uma sessão OBD pronta. Os comandos terminam com carriage return; a resposta é normalizada para maiúsculas, sem prompt, quebras de linha ou espaços excedentes.

### PIDs, fórmulas e agendamento

As consultas de leitura usam o Serviço 01. Os intervalos são mínimos entre execuções elegíveis; tarefas prontas são ordenadas por prioridade (HIGH antes de MEDIUM, depois LOW). O ciclo de monitoramento volta a verificar tarefas a cada 10 ms. Os intervalos e prioridades são os configurados atualmente em `Elm327Manager`.

| Dado do painel                          |  PID | Conversão do dado `A` (e `B`, quando indicado) | Intervalo | Prioridade |
|-----------------------------------------|-----:|------------------------------------------------|----------:|------------|
| Rotação                                 | `0C` | `((A × 256) + B) / 4` RPM                      |    100 ms | HIGH       |
| Velocidade                              | `0D` | `A` km/h                                       |    100 ms | HIGH       |
| Borboleta                               | `11` | `(A × 100) / 255` %                            |    300 ms | MEDIUM     |
| MAP                                     | `0B` | `A` kPa                                        |    300 ms | MEDIUM     |
| Temperatura do motor                    | `05` | `A - 40` °C                                    |  1.000 ms | LOW        |
| Temperatura de admissão                 | `0F` | `A - 40` °C                                    |  1.000 ms | LOW        |
| Tensão (tarefa declarada, hoje inativa) | `42` | `((A × 256) + B) / 1000` V                     |  2.000 ms | LOW        |
| Combustível                             | `2F` | `(A × 100) / 255` %                            |  2.000 ms | LOW        |

As tarefas de `0x05`, `0x0B`, `0x0C`, `0x0D` e `0x0F`/`0x11` usam a máscara retornada por `0100`. Combustível `0x2F` usa a máscara de `0120`; portanto, só é marcado suportado se `0100` anunciar `0x20` e a consulta seguinte anunciar `0x2F`. Tarefa não suportada não é consultada.

**Comportamento especial da tensão:** o código declara o PID `0x42`, mas só obtém máscaras até `0x40` e configura a tarefa `0x42` com a máscara base de `0100`. Assim, o PID `0x42` nunca é marcado suportado e o scheduler não agenda essa tarefa. A função de leitura contém um fallback `ATRV`, mas ele é inalcançável enquanto a tarefa continuar sem suporte. Na implementação atual, o cartão “Bateria” permanece no valor inicial `0.0`; nenhum comando de leitura de tensão é executado. Se o fallback for tornado alcançável, `ATRV` mede a tensão de alimentação do próprio adaptador ELM327, não uma leitura confirmada da ECU. Para habilitar o PID seria necessário consultar e interpretar também o bloco iniciado por `0140` e configurar o suporte com essa máscara.

### Parsing e significado de valores

`ObdCommand` procura o cabeçalho de resposta `41` seguido do PID e exige o comprimento configurado antes de converter os bytes hexadecimais. Cada comando guarda o último valor convertido; as respostas ausentes, curtas ou com hexadecimal inválido retornam esse valor anterior. Os comandos de medição inicializam esse valor em zero. `Elm327Manager` ainda publica o número retornado ao listener, e o layout também inicia as métricas numéricas em zero.

Consequentemente, o estado exibido atualmente **não distingue** uma leitura real igual a zero de uma resposta inválida anterior à primeira leitura válida, nem identifica quando um valor preservado ficou antigo. Uma tarefa marcada não suportada simplesmente não atualiza o widget; seu valor inicial permanece. Se a função de leitura da tensão chegar a executar seu fallback `ATRV`, ele retorna `0.0` se não houver resposta ou se a conversão falhar. No fluxo atual, porém, a tarefa de tensão não chega a ser agendada. Não trate esses zeros como uma indicação explícita de dado indisponível. Se essa semântica mudar, ajuste em conjunto comandos, callbacks, estado de suporte e apresentação.

## Painel

O painel apresenta rotação, velocidade, borboleta, MAP, temperaturas do motor e da admissão, tensão e nível de combustível. O indicador de rotação limita o valor visual ao intervalo entre zero e o RPM máximo configurado. O alerta visual pisca quando a leitura alcança ou ultrapassa o RPM inicial do alerta.

As cores dos valores são classificações visuais codificadas na `DashboardActivity` quando o callback de cada métrica é recebido:

- Temperatura do motor: abaixo de 60 °C frio; até 95 °C normal; até 105 °C aviso; acima disso quente.
- Temperatura de admissão: abaixo de 20 °C frio; até 40 °C normal; até 60 °C aviso; acima disso quente.
- Tensão exibida: abaixo de 11,8 V baixa; de 11,8 V a menos de 12,6 V aviso; até 14,7 V normal; acima disso alta.

No fluxo atual, o callback de tensão não chega a ser recebido porque a tarefa `0x42` não é agendada; por isso, o cartão de bateria mantém o valor inicial e sua faixa de cor não é aplicada. As demais faixas só mudam a apresentação, não constituem diagnóstico mecânico. O painel é mantido ligado enquanto visível.

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