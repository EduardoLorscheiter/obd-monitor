# Guia de desenvolvimento

Este é o documento normativo único para convenções e regras de implementação do repositório. Consulte [arquitetura](architecture.md) para limites e organização dos componentes, e [comportamento atual](current-behavior.md) para os fluxos que precisam ser preservados ou alterados deliberadamente. Use [verificação](verification.md) para escolher as checagens.

## Linguagem e estrutura

- Preserve Java como linguagem de produção e a compatibilidade Java configurada em `app/build.gradle.kts`, salvo decisão explícita de migração.
- Siga os pacotes existentes: `bluetooth`, `elm327`, `model`, `obd.commands`, `obd.scheduler`, `ui`, `utils` e `view`. Crie outra camada ou dependência somente quando houver necessidade concreta.
- Mantenha transporte, protocolo ELM327, interpretação OBD-II, agendamento e apresentação separados conforme [arquitetura](architecture.md).
- Use o catálogo `gradle/libs.versions.toml` para versões de dependências que possam ser administradas por ele. Explique a necessidade e confira compatibilidade com SDK e Java configurados.
- Não edite arquivos gerados em `build/` ou `app/build/`.

## JavaDoc e cabeçalho de classe

Toda classe Java deve começar com um cabeçalho no formato abaixo. Atualize arquivo, classe, data (`DD/MM/AAAA`) e resumo; mantenha autor e versão inicial conforme o padrão do projeto. Todo método e construtor declarado deve ter JavaDoc, incluindo métodos privados e sobrescritos. Documente comportamento, parâmetros, retorno e exceções quando aplicável.

```java
/*
 **=================================================================================================
 **  @file    FileName.java
 **  @author  Eduardo Lorscheiter
 **  @class   FileName
 **  @date    DD/MM/AAAA
 **  @version 1.0
 **  @brief   Exibe diálogos informativos com o tema visual do aplicativo.
 **=================================================================================================
 */
```

Respeite o estilo e o nível de documentação usado no arquivo e pacote alterados.

## Activities, controles e recursos visuais

- Toda Activity nova deve estender `com.eduardo.obdmonitor.ui.BaseActivity`, para manter os comportamentos comuns e a integração dos gerenciadores.
- Todo botão ou controle que atua como botão precisa reproduzir o som de clique do aplicativo. Em Activities, registre a ação por `setClickSoundListener(view, action)` herdado de `BaseActivity`. A regra inclui `Button`, `MaterialButton`, `ImageButton` e controles customizados clicáveis com função de botão.
- Todo componente novo que use cor deve referenciar um recurso declarado em `app/src/main/res/values/colors.xml`. Se a cor não existir, declare-a ali e use `@color/...` em XML ou `R.color...` em Java; não coloque valores hexadecimais diretamente no componente.
- Texto visível deve vir de `strings.xml`, incluindo mensagens de erro. Mantenha recursos existentes de tema, cor, layout e strings.
- Ao criar ou alterar uma tela, trate estados de carregamento, indisponibilidade e erro, além do estado conectado. Evite manter Activity ou View além do ciclo de vida e remova callbacks/listeners quando necessário.

## Bluetooth e ELM327

Antes de alterar comunicação, consulte o fluxo real em [comportamento atual](current-behavior.md) e os limites de [arquitetura](architecture.md).

- Preserve as responsabilidades distintas de transporte Bluetooth e protocolo ELM327.
- Defina comportamento para falhas de conexão, resposta vazia ou incompleta, timeout, cancelamento, reconexão, permissões negadas e ciclo de vida Android.
- Não faça I/O de Bluetooth nem parsing pesado na thread principal.
- Antes de alterar permissões ou descoberta/conexão, confira os requisitos atuais por versão Android. Alinhe manifesto, verificação de permissão, solicitação em runtime e tratamento de recusa; declarar uma permissão no manifesto não substitui o fluxo em runtime.
- Normalize respostas em um ponto claro, preservando dados suficientes para detectar cabeçalhos e erros inesperados.

## Comandos OBD-II e parsing

1. Inspecione `obd/commands`, `ObdCommand` e os consumidores antes de definir uma API ou semântica nova.
2. Confirme serviço, PID, blocos de suporte, tamanho, codificação, fórmula, unidade e resposta negativa em fonte técnica adequada.
3. Valide resposta e tamanho antes de converter bytes. Resposta incorreta não pode virar uma medição plausível por acidente.
4. Defina explicitamente se erro significa indisponibilidade ou preservação do último valor válido; mantenha a mesma semântica nos comandos, gerenciador, listeners e UI.
5. Ao adicionar PID ao painel, integre suporte, scheduler, listener e view. Escolha prioridade e intervalo conscientemente e documente o comportamento alterado.
6. Cubra entradas válidas, limites, resposta curta ou incorreta e PID não suportado com testes proporcionais; veja [verificação](verification.md).

## Configurações e persistência

Inspecione `SettingsUtils`, as chaves e os chamadores antes de alterar persistência. Valide valores lidos, mantenha padrões explícitos e trate preferências ausentes ou antigas. Documente mudanças nos valores e no fluxo em [comportamento atual](current-behavior.md).

## Atualização da documentação

Se uma mudança alterar fluxo de usuário, comportamento de conexão, PID/fórmula/unidade, permissões, configuração ou arquitetura, atualize o documento que descreve esse assunto. Os demais Markdown devem referenciar a fonte em vez de repetir o conteúdo. Para identificar o dono de cada assunto, use [orientações para agentes](../AGENTS.md) e os links no início dos documentos.
