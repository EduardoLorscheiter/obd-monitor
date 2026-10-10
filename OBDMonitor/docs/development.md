# Guia de implementação

Use este guia ao adicionar comportamento ao OBD Monitor. Comece identificando o ponto de extensão existente e mantenha a mudança alinhada aos limites descritos em [arquitetura](architecture.md).

## Adicionar um comando OBD-II

1. Inspecione `obd/commands`, `ObdCommand` e o caminho que consome os comandos antes de definir uma nova API.
2. Confirme o serviço, PID, tamanho de resposta, codificação, fórmula, unidade e comportamento de resposta negativa usando uma fonte técnica adequada.
3. Implemente o parsing em uma classe coesa. Valide `null`, cabeçalho/serviço e comprimento antes de converter bytes; não deixe uma resposta inválida virar uma medição plausível.
4. Defina explicitamente a semântica para resposta inválida: erro/indisponibilidade ou último valor válido, conforme o contrato existente. Não misture os dois silenciosamente.
5. Se o PID entrar no painel ou agendador, integre-o nos pontos existentes e escolha prioridade/intervalo com base no custo e na frequência útil da leitura.
6. Cubra parsing válido, limites, resposta incorreta, resposta curta e PID não suportado.

## Bluetooth e ELM327

- Mantenha transporte, ciclo de conexão e protocolo ELM327 em seus componentes próprios.
- Considere timeouts, encerramento do socket, cancelamento e reconexão; callbacks podem chegar quando uma tela já não está ativa.
- Não faça trabalho de rede ou parsing pesado na thread principal.
- Trate permissões negadas, Bluetooth desligado, dispositivo indisponível e falha de pareamento como estados distintos quando a interface precisar orientar a pessoa.
- Normalize respostas em um ponto claro, preservando informação suficiente para detectar erros e cabeçalhos inesperados.

## Interface e recursos

- Toda Activity nova deve herdar de `com.eduardo.obdmonitor.ui.BaseActivity`. A classe base gerencia comportamentos comuns do aplicativo, inclusive o som de interação.
- Todo botão deve tocar o som de clique do aplicativo ao ser acionado. Registre sua ação com `setClickSoundListener(view, action)`, fornecido por `BaseActivity`; evite instalar um `setOnClickListener` diretamente em botões. A regra inclui botões de imagem, botões Material e controles customizados com função de botão.
- Use `strings.xml` para texto visível, incluindo mensagens de erro.
- Reutilize estilos, cores e componentes visuais existentes quando adequados.
- Pense em estados de carregamento, indisponibilidade e erro, além do estado conectado com dados.
- Evite manter referências a Activity/View depois do ciclo de vida; cancele atualizações e listeners quando apropriado.

## Configurações e persistência

Inspecione `SettingsUtils` e os chamadores antes de alterar armazenamento ou chaves. Mantenha padrões seguros, valide valores lidos e trate configurações antigas ou ausentes sem falha.

## Mudanças de dependências e plataforma

Use o catálogo `gradle/libs.versions.toml` quando a dependência puder ser gerida por ele. Explique o motivo da dependência e confira compatibilidade com SDK/Java configurados. Alterações de permissões Android precisam refletir manifesto e fluxo em runtime.
