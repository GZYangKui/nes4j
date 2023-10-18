#!/usr/bin/env bash
if [ "$1" = "" ]; then
  printf "Please specify 6502 assembly source file"
  exit;
fi
IDX=$(echo "$1" | awk -F"." '{print length($1)}')
FILE_NAME=$(echo "$1" | cut -c 1-$IDX)
OBJ_FILE="$FILE_NAME.o"
ca65 -o "$OBJ_FILE" "$1"
ld65 -C nes.conf --obj "$OBJ_FILE" -o "$FILE_NAME.nes"
if [ -e "$OBJ_FILE" ]; then
  rm -rf "$OBJ_FILE"
fi