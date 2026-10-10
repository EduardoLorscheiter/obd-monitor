---
name: obd-monitor
description: Implementar ou revisar funcionalidades do aplicativo Android OBD Monitor, incluindo Java, interface, Bluetooth, ELM327, comandos OBD-II e agendamento de PIDs.
---

# Skill: OBD Monitor

Use esta skill para mudanças neste repositório. Aplique as regras de `AGENTS.md` e consulte os guias do projeto antes de codificar.

## Orientação

1. Leia `README.md`, `docs/architecture.md` e o código diretamente envolvido. Use `docs/development.md` para decisões de implementação e `docs/verification.md` para escolher verificações.
2. Confirme o comportamento atual no código em vez de inferir a partir do nome de classes ou deste documento. A documentação descreve orientação e pode ficar desatualizada.
3. Siga a linguagem, APIs e organização já existentes: Android nativo, Java 11, um módulo `app` e os pacotes do domínio.
4. Toda Activity criada deve estender `com.eduardo.obdmonitor.ui.BaseActivity`.
5. Todo botão ou controle que funcione como botão deve reproduzir o som de clique. Use `setClickSoundListener(view, action)` herdado de `BaseActivity` para registrar a ação, incluindo `Button`, `MaterialButton`, `ImageButton` e controles customizados clicáveis.
6. Para OBD-II, verifique especificação e formato reais; valide resposta antes de conversão e preserve estados de erro/indisponibilidade.
7. Para Bluetooth Android, considere permissões de runtime, versão do sistema, callbacks assíncronos e ciclo de vida.
8. Mantenha UI, transporte, protocolo, parsing e agendamento separados conforme a arquitetura existente.
9. Atualize docs se a mudança alterar uma decisão de arquitetura, um fluxo relevante ou as instruções de desenvolvimento.
10. Verifique somente o necessário para a mudança, não altere saídas geradas e relate com precisão o executado.

## Referências

- `AGENTS.md`
- `docs/architecture.md`
- `docs/development.md`
- `docs/verification.md`
