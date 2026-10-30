# 🎧 Minhas Músicas

App de música estilo Spotify para as suas músicas baixadas. Funciona no celular (Android e iPhone) e no computador, sem internet depois de instalado.

## Instalar no Android (APK)

A cada atualização enviada ao GitHub, o APK é gerado automaticamente e publicado na página **Releases** do repositório.

1. No celular, abra a página **Releases** do repositório no GitHub e baixe o `MinhasMusicas.apk` da versão mais recente (celulares antigos de 32 bits: `MinhasMusicas-32bits.apk`).
2. Abra o arquivo baixado. Se o Android pedir, permita **instalar apps desta fonte**.
3. Pronto: o ícone **Minhas Músicas** aparece na tela inicial.

Versões novas instalam por cima da anterior sem apagar suas músicas. No app Android, a música continua tocando com a tela desligada e os controles aparecem na notificação e na tela de bloqueio.

Para gerar o APK no seu computador: `npm install` e depois `npm run apk` (precisa do Android Studio / SDK do Android e do Java 17).

## Baixar músicas do YouTube (app Android)

- Toque em **+** → **Baixar do YouTube (link)**, cole o link e toque em **Baixar**; ou
- No app do YouTube, toque em **Compartilhar** → **Minhas Músicas**.

O áudio é baixado com o [yt-dlp](https://github.com/yt-dlp/yt-dlp) (via [youtubedl-android](https://github.com/yausername/youtubedl-android)), com título, artista e capa. O yt-dlp se atualiza sozinho uma vez por dia, porque o YouTube muda com frequência. Baixe apenas conteúdo que você tem direito de baixar.

## Instalar como app web (Android ou iPhone)

O app precisa estar publicado num endereço `https://` (GitHub Pages, Vercel, Netlify…). Depois:

- **Android (Chrome):** abra o link e toque no ícone ⬇ no topo do app, ou em ⋮ → **Instalar app**.
- **iPhone (Safari):** abra o link, toque em **Compartilhar** → **Adicionar à Tela de Início**.

O ícone "Músicas" aparece na tela inicial e abre em tela cheia, como um app normal.

## Como usar

1. Toque em **+** e escolha as músicas baixadas no aparelho.
2. Toque numa música para tocar. Toque no mini-player para abrir o player em tela cheia (deslize para baixo para fechar).
3. Use o **⋮** de cada música para curtir, tocar em seguida, adicionar a uma playlist ou excluir.

As músicas ficam guardadas dentro do app, então continuam lá quando você abrir de novo.

## Recursos

- Abas: Início, Buscar, Curtidas e Playlists
- Lê título, artista, álbum e capa das tags do MP3 (ou usa o nome do arquivo `Artista - Música`)
- Aleatório e repetir (tudo / uma) direto na lista, lembrados ao abrir o app de novo
- **Ordenar** por título, artista, mais recentes ou ordem personalizada
- **Mover**: arraste ≡ para mudar a ordem da biblioteca, curtidas e playlists
- **Fila** (botão no player): veja o que toca a seguir, arraste para reordenar ou tire músicas; "tocar em seguida"
- Controles na tela de bloqueio e na barra de notificações
- Formatos: MP3, M4A, AAC, FLAC, OGG, OPUS, WAV

## Arquivos

- `index.html` — o app inteiro
- `manifest.webmanifest` + `icons/` — nome e ícone do app instalado
- `sw.js` — faz o app abrir sem internet
- `android/` — projeto Android (Capacitor) que empacota o `index.html` como APK
- `.github/workflows/android.yml` — gera o APK automaticamente no GitHub
