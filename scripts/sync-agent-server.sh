#!/usr/bin/env bash
set -euo pipefail

SRC="/Users/hyunminlim/Documents/New project/apps/agent-server/"
DST="/Users/hyunminlim/Desktop/opentofu-bakup/confluence-sre/tree-opentofu/fin-tech-sre-platform-portfolio/apps/agent-server/"

rsync -av --delete \
  --include "build/" \
  --include "build/reports/" \
  --include "build/reports/tests/" \
  --include "build/reports/tests/test/" \
  --include "build/reports/tests/test/***" \
  --exclude ".git" \
  --exclude "build/***" \
  --exclude ".gradle" \
  --exclude ".idea" \
  --exclude "*.iml" \
  --exclude ".DS_Store" \
  --exclude ".classpath" \
  --exclude ".project" \
  --exclude ".settings" \
  --exclude ".gradle-home" \
  "$SRC" "$DST" 

rm -rf "$DST/.gradle" "$DST/.gradle-home" 