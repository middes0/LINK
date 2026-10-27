# LINK

Projeto Android nativo para visualizar a tela de outro dispositivo com autorização explícita.

## Estado atual

Este repositório contém a primeira versão do MVP de captura e prévia local da tela. Ele ainda **não conecta dois celulares pela internet** e não oferece controle remoto. O próximo passo é confirmar a compilação no GitHub Actions e testar em um Android real.

## Segurança

- A captura só começa depois da confirmação do sistema Android.
- A sessão pode ser encerrada pelo usuário.
- Não há captura escondida nem envio de tela para servidores nesta etapa.

## Gerar APK pelo GitHub

1. Abra a aba **Actions** deste repositório.
2. Se necessário, habilite GitHub Actions.
3. Execute o workflow **Build LINK Android**.
4. Abra a execução concluída e baixe o artefato `LINK-debug-apk`.

O APK de depuração não é uma versão final de produção.

## Próximas etapas

1. Confirmar a compilação e testar a prévia de captura em um Android real.
2. Implementar conexão WebRTC entre dispositivos pareados.
3. Adicionar sinalização segura e fallback TURN somente quando necessário.
4. Avaliar controle remoto com permissões explícitas.

## Requisitos

- Android Studio / Android SDK para compilação local, ou GitHub Actions para compilação em nuvem.
- Android 8.0 (API 26) ou superior para instalar o protótipo.
