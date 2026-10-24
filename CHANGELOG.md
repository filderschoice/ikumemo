# 変更履歴

このリポジトリの主要な変更は本ファイルに記録します。

## 2026-10-24（PLAN.md のタスク分解）

`PLAN.md`（開発計画書）のマイルストーン M0〜M10 を `docs/records/managed/BACKLOG.md` のタスクへ分解しました。

- M0（準備）〜M8（仕上げ）の実装タスクと、実機確認・Play Console・GitHub 設定などオーナーが行う作業
  （`区分: 人手検証`）を登録
- オーナー指示により sesami-wear に合わせ、CI（GitHub Actions）と Dependabot は導入しない。PLAN M0 の完了条件
  「CI でビルドされる」はローカルの品質ゲートで判定する
- LICENSE の配置は PLAN D-6 によりオーナーが行う（BL-007）
- `PLAN.md` の冒頭へ `<!-- markdownlint-disable-file -->` を追加（オーナー指示）。PLAN は D-16 により開発開始後は
  本文を更新しない資料のため、書式の指摘は直さず Markdown の静的解析の対象から外す
