#!/usr/bin/env bash
set -euo pipefail

SRC="/Users/hyunminlim/Documents/New project/apps/agent-server/"
DST="/Users/hyunminlim/Desktop/opentofu-bakup/confluence-sre/tree-opentofu/fin-tech-sre-platform-portfolio/apps/agent-server/"

rsync -av --delete \
  "$SRC" "$DST" \
  --exclude ".git" \
  --exclude "build" \
  --exclude ".gradle" \
  --exclude ".idea" \
  --exclude "*.iml" \
  --exclude ".DS_Store" \
  --exclude ".classpath" \
  --exclude ".project" \
  --exclude ".settings" \
  --exclude ".gradle-home"

rm -rf "$DST/.gradle" "$DST/.gradle-home" "$DST/build"