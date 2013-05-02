#!/bin/sh
if [ ! -d target ]
  then
    mkdir target
  fi
# Depenendency:  'color-marked' Node package.  Install with:  'npm install color-marked'
(cat github-markdown.css.html && color-marked README.md --gfm --color --tables --breaks) > target/README.html
