# アプリ紹介ページ（site/）

いくメモを紹介する静的ページと、アプリのご意見・ご要望からご意見フォームへ移る転送ページです
（PLAN §7.2・§7.3）。隣のリポジトリ sesami-wear の `site/` を雛形にしています。ビルドは不要で、
`index.html` をブラウザーで開くだけで表示できます。

## 構成

| ファイル | 内容 |
| --- | --- |
| `index.html` | 紹介ページ。文言は `docs/store/STORE_LISTING.md` と PLAN §1.2 から取っている |
| `feedback/index.html` | ご意見フォームへの転送ページ |
| `style.css` | 共通のスタイル（色はアプリの緑） |
| `app.json` | アプリの概要（`schema: app-portfolio.v1`、sesami-wear と同じ形式） |
| `assets/icon.svg` | 仮のアイコン（吹き出しとかご）。正式なアイコンができたら差し替える |
| `.nojekyll` | GitHub Pages に Jekyll の変換をさせないための空ファイル |

## 方針

- HTML と CSS だけで作る。アクセス解析・外部スクリプト・外部フォント・Cookie は使わない（転送ページの数行の JavaScript は同じファイルの中にある）。
- ストアのリンクは公開前のため「準備中」の表示だけにしている。公開したら `index.html` のボタンを Play の URL へ変え、`app.json` の `status` と `links` も直す。
- スクリーンショットはまだ載せていない。ストア用の画像ができたら `assets/` へ置いて載せる。
- リンクは相対パスか GitHub の絶対 URL にし、`site/` の外を相対で参照しない。

## 転送ページ（feedback/）

アプリは `https://filderschoice.github.io/ikumemo/feedback/?info=<URL エンコードした「アプリの版 X ／ Android Y ／ 機種 Z」>` を開きます。
ページは `info` を取り出し、`encodeURIComponent` でエンコードし直して Google フォームの `entry.10635078` に付け、
`location.replace` でフォームへ移ります。`info` が無ければ事前入力なしのフォームへ移ります。
`info` はページに表示せず、URL の組み立てにだけ使います。JavaScript が使えない場合は、ページのリンクからフォームを開けます。

フォームを作り直したとき（アプリの更新は要りません）は、`feedback/index.html` の次を差し替えます。

- `<script>` の先頭の定数 `FORM_URL`（フォームの URL）と `FORM_ENTRY_INFO`（事前入力の欄の `entry.<番号>`）
- JavaScript が無いとき用の、`id="form-link"` のリンクの `href`（フォームの URL と同じ値）

現在の値と調べ方は [docs/FEEDBACK.md](../docs/FEEDBACK.md) の「作成済みのフォーム」にあります。

## GitHub Pages での公開

`site/` の中身だけを `gh-pages` ブランチへ切り出して配信します（sesami-wear と同じ方法）。
`git push` は人が実行します（エージェントは実行しません）。

### 初回の設定（1 回だけ）

1. `site/` の変更を `main` へマージしたあと、`main` を最新にして `gh-pages` ブランチを作って push する。

   ```bash
   git checkout main
   git pull
   git subtree push --prefix site origin gh-pages
   ```

2. GitHub のリポジトリの Settings → Pages で、Source を「Deploy from a branch」、Branch を `gh-pages` の `/ (root)` にして保存する。
3. 数分後に <https://filderschoice.github.io/ikumemo/> と <https://filderschoice.github.io/ikumemo/feedback/?info=TEST> を開く。
   後者はフォームへ移り、「アプリの版・Android の版・機種名」に `TEST` が入っていればよい。

### 更新するとき

`site/` の変更を `main` へマージしたあと、`main` で同じ `git subtree push` を実行します。
`gh-pages` は直接編集しないでください（早送りにならず push が失敗します）。失敗したときは `gh-pages` を `site/` の履歴で作り直します。

```bash
git push origin "$(git subtree split --prefix site main)":refs/heads/gh-pages --force
```
