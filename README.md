# LINK

Projeto Android nativo para visualizar a tela de outro dispositivo com autorização explícita.

## Estado atual

Este repositório inicia o MVP de captura de tela local. Ele ainda **não conecta dois celulares pela internet** e não oferece controle remoto. A primeira etapa é compilar e validar a captura autorizada em um Android real.

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

1. Compilar e testar a captura local.
2. Exibir os frames capturados na interface.
3. Implementar conexão WebRTC entre dispositivos pareados.
4. Adicionar sinalização segura e fallback TURN somente quando necessário.
5. Avaliar controle remoto com permissões explícitas.

## Requisitos

- Android Studio / Android SDK para compilação local, ou GitHub Actions para compilação em nuvem.
- Android 8.0 (API 26) ou superior para instalar o protótipo.
