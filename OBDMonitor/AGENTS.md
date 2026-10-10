# Orientações para agentes

## Contexto do projeto

OBD Monitor é um aplicativo Android nativo para leitura de dados veiculares por OBD-II, com adaptador ELM327 via Bluetooth. O código de produção está em `app/src/main/java/com/eduardo/obdmonitor`; recursos Android ficam em `app/src/main/res`.

Antes de implementar uma mudança, leia [a visão geral](docs/architecture.md) e a skill [obd-monitor](.agents/skills/obd-monitor/SKILL.md). Consulte também os guias específicos de implementação e verificação quando forem pertinentes.

## Regras de implementação

- Preserve Java como linguagem de produção e compatibilidade Java 11, salvo decisão explícita de migração.
- Siga os pacotes existentes: `bluetooth`, `elm327`, `model`, `obd.commands`, `obd.scheduler`, `ui`, `utils` e `view`. Evite criar camadas ou dependências sem necessidade concreta.
- Toda nova `Activity` deve estender `BaseActivity` (`com.eduardo.obdmonitor.ui.BaseActivity`), nunca diretamente `AppCompatActivity` ou outra classe base, para preservar os comportamentos comuns do aplicativo.
- Todo botão e controle que funcione como botão deve reproduzir o som de clique configurado. Em Activities, conecte a ação por `setClickSoundListener(view, action)` herdado de `BaseActivity`; não substitua esse listener por `setOnClickListener` sem som. Isso inclui `Button`, `MaterialButton`, `ImageButton` e elementos customizados clicáveis que atuem como botões.
- Mantenha responsabilidades separadas: transporte Bluetooth, protocolo ELM327, interpretação OBD-II, agendamento e apresentação não devem ser acoplados sem motivo.
- Trate falhas de conexão, respostas incompletas/malformadas, permissões e ciclo de vida Android como situações normais que precisam de comportamento definido.
- Preserve compatibilidade com o `minSdk` e `targetSdk` configurados em `app/build.gradle.kts`; confira requisitos atuais de permissões Android antes de alterar fluxos Bluetooth.
- Não registre dados sensíveis nem esconda erros com valores que pareçam medições válidas. Preserve o último valor válido somente quando essa for a semântica apropriada e deixe estados indisponíveis explícitos.
- Atualize documentação com mudanças de arquitetura, comandos/PIDs, permissões ou fluxos de usuário.

## Convenções

- Use nomes claros e consistentes com o domínio automotivo e com os pacotes atuais.
- Recursos visuais e textos devem seguir os recursos Android existentes; evite strings de interface hardcoded no Java.
- Ao criar ou alterar telas, confira cada controle acionável e garanta que todos os botões usem o comportamento sonoro comum.
- Respeite o estilo local do arquivo alterado, incluindo documentação Java já usada nas classes do domínio.
- Mantenha mudanças focadas e não edite arquivos gerados em `build/` ou `app/build/`.

## Verificação

Escolha verificações proporcionais ao código alterado. Para lógica Java, priorize testes unitários; para integração com Android, valide compilação e os testes relevantes. Não afirme que algo foi verificado sem executar a verificação e observar o resultado. Consulte [verificação e testes](docs/verification.md) para comandos e limites conhecidos.
