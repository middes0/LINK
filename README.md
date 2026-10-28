# LINK

Aplicativo Android para compartilhar a tela entre dois aparelhos com autorização explícita.

## Estado desta versão

A versão 0.2 adiciona uma primeira implementação de transmissão de vídeo via WebRTC P2P. A sinalização ainda é manual: os dois aparelhos precisam ter o LINK instalado e trocar uma oferta e uma resposta SDP copiando o texto entre eles.

A conexão usa STUN para tentar estabelecer uma ligação direta. **Ainda não há servidor TURN de retransmissão**, então algumas combinações de redes móveis, Wi-Fi e NAT podem impedir a conexão. Isso é uma limitação conhecida, não uma garantia de conexão universal.

## Como testar em dois celulares

1. Compile e instale o mesmo APK nos dois celulares.
2. No celular que vai transmitir, toque em **Transmitir esta tela** e autorize a captura no Android.
3. Quando a oferta aparecer, toque em **Copiar código exibido**.
4. No celular que vai receber, toque em **Receber tela de outro celular**, cole o código recebido e toque em **Gerar resposta**.
5. No receptor, copie a resposta e envie-a de volta ao transmissor.
6. No transmissor, cole a resposta no campo de código recebido e toque em **Conectar usando a resposta**.
7. Aguarde a conexão. Use **Encerrar conexão** para parar a sessão.

Os códigos de conexão são longos e precisam ser copiados completos. Não publique esses códigos em locais públicos.

## Segurança e limitações

- O Android solicita autorização para capturar a tela.
- O LINK mostra uma notificação persistente enquanto a transmissão está ativa.
- O vídeo é enviado por WebRTC com criptografia de transporte DTLS-SRTP; a sinalização manual não passa por um servidor do LINK.
- A conexão direta pode falhar em certas redes, pois TURN ainda não foi configurado.
- Esta versão transmite vídeo da tela; **controle remoto por toque ainda não está implementado**.
- A janela do LINK é marcada como segura para evitar capturar a própria prévia. Ao compartilhar a tela, o conteúdo de janelas seguras pode aparecer preto.

## Gerar APK pelo GitHub

1. Abra [Actions](https://github.com/middes0/LINK/actions).
2. Abra a execução mais recente do workflow **Build LINK Android**.
3. Aguarde o resultado e baixe o artefato `LINK-debug-apk`.
4. Extraia o ZIP e instale o APK nos dois aparelhos.

O APK de depuração não é uma versão final de produção.

## Requisitos

- Android 8.0 (API 26) ou superior.
- GitHub Actions para compilar sem computador local.
