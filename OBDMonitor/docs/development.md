# Guia de desenvolvimento

Referência normativa do ambiente e das regras de implementação e documentação. Consulte [arquitetura](architecture.md) para localizar componentes e [comportamento atual](current-behavior.md) para conferir os fluxos afetados.

## Preparar o ambiente

Use o Gradle Wrapper incluído no repositório. A configuração versionada define:

| Item                                  | Configuração                                            | Fonte                                                                    |
|---------------------------------------|---------------------------------------------------------|--------------------------------------------------------------------------|
| Gradle                                | 9.9.2                                                   | [gradle-wrapper.properties](../gradle/wrapper/gradle-wrapper.properties) |
| JVM do daemon Gradle                  | Java 21                                                 | [gradle-daemon-jvm.properties](../gradle/gradle-daemon-jvm.properties)   |
| Compatibilidade do código Java do app | Java 11 (`sourceCompatibility` e `targetCompatibility`) | [app/build.gradle.kts](../app/build.gradle.kts)                          |
| Android SDK                           | `minSdk` 29, `targetSdk` 36, `compileSdk` 36.1          | [app/build.gradle.kts](../app/build.gradle.kts)                          |
| Plugins e bibliotecas                 | Versões centralizadas no catálogo                       | [libs.versions.toml](../gradle/libs.versions.toml)                       |

1. Configure um JDK 21 no ambiente e no Gradle JDK do Android Studio. No terminal, o wrapper precisa encontrar `java` por `JAVA_HOME` ou pelo `PATH`; o arquivo de critérios seleciona a JVM do daemon. A compatibilidade Java do aplicativo é uma configuração distinta.
2. Instale o Android SDK correspondente ao `compileSdk` pelo SDK Manager. Use uma versão do Android Studio compatível com o plugin Android definido no catálogo.
3. Abra a raiz deste projeto no Android Studio e sincronize o Gradle. Configure o caminho local do SDK pelo IDE ou por `sdk.dir` em `local.properties`.
4. Consulte [verificação](verification.md), tópico **"Comandos e resultados"**, para compilar e conferir o projeto.

A primeira sincronização pode baixar Gradle, a JVM selecionada e dependências. `local.properties`, caminhos do SDK e artefatos gerados são específicos da máquina; não os versione nem os use como requisitos de outra instalação.

## Linguagem e estrutura

- Preserve Java como linguagem de produção e a compatibilidade Java configurada em `app/build.gradle.kts`, salvo decisão explícita de migração.
- Siga [arquitetura](architecture.md), tópico **"Estrutura"**, preservando os limites entre transporte, protocolo, interpretação, agendamento e apresentação. Crie outra camada ou dependência somente quando houver necessidade concreta.
- Use o catálogo `gradle/libs.versions.toml` para versões de dependências que possam ser administradas por ele. Explique a necessidade e confira compatibilidade com SDK e Java configurados.
- Não edite arquivos gerados em `build/` ou `app/build/`.

## JavaDoc e cabeçalho de classe

Toda classe Java deve começar com um cabeçalho no formato abaixo. Atualize arquivo, classe, data (`DD/MM/AAAA`) e resumo; se já houver data, mantenha ela, não altere; mantenha autor e versão inicial conforme o padrão do projeto. Todo método e construtor declarado deve ter JavaDoc, incluindo métodos privados e sobrescritos. Documente comportamento, parâmetros, retorno e exceções quando aplicável.

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
- Texto visível deve vir de `strings.xml`, incluindo mensagens de erro. Mantenha as traduções em `values/strings.xml` e `values-en/strings.xml` consistentes e preserve os recursos de tema, cor e layout.
- Ao criar ou alterar uma tela, trate estados de carregamento, indisponibilidade e erro, além do estado conectado. Evite manter Activity ou View além do ciclo de vida e remova callbacks/listeners quando necessário.

## Bluetooth e ELM327

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

## Manter a documentação

O código é a fonte de verdade sobre o comportamento executado. Antes de afirmar um fluxo, parâmetro ou garantia, confira a implementação e os consumidores; comentários, documentação anterior e nomes de testes não bastam como evidência. Separe comportamento existente de regras para mudanças futuras e registre limitações confirmadas sem apresentar melhorias pretendidas como recursos prontos.

- Use o [README.md](../README.md), tópico **"Documentação"**, para localizar a página responsável pelo assunto e atualize-a junto da alteração de código. Ambiente, comandos, fórmulas e convenções devem ter uma única referência documental; use links nas demais páginas.
- Organize o texto pela necessidade de quem lê: orientação inicial, procedimento, explicação ou referência. A separação segue o princípio do [Diátaxis](https://diataxis.fr/), sem exigir novas páginas para categorias sem conteúdo útil.
- Prefira informação duradoura: contratos entre componentes, passos executáveis e limitações relevantes. Deixe detalhes locais de métodos no código e o histórico de correções no Git.
- Use português, títulos descritivos, links relativos para arquivos do repositório, exemplos curtos e tabelas para dados comparáveis. Evite índices repetidos, caminhos pessoais, resultados antigos de testes e promessas universais de compatibilidade.
- Consulte fontes técnicas primárias ao descrever requisitos externos, identificando o que vem da plataforma e o que é uma escolha da implementação.
- Ao mover ou remover conteúdo, ajuste os links e confira os critérios em [verificação](verification.md), tópico **"Revisar alterações documentais"**. Registre na entrega somente as verificações realmente executadas e seus limites.
