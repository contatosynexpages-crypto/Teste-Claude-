# 🎧 Minhas Músicas

App de música estilo Spotify para as suas músicas baixadas. Funciona no celular (Android e iPhone) e no computador, sem internet depois de instalado.

## Instalar no celular

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
- Aleatório, repetir (tudo / uma), "tocar em seguida"
- Controles na tela de bloqueio e na barra de notificações
- Formatos: MP3, M4A, AAC, FLAC, OGG, OPUS, WAV

## Arquivos

- `index.html` — o app inteiro
- `manifest.webmanifest` + `icons/` — nome e ícone do app instalado
- `sw.js` — faz o app abrir sem internet
