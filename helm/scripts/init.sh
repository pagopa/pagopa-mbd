#!/bin/bash

# Define base directory (override with env var if needed)
# export BASE_MNT=${BASE_MNT:-/mnt}
# export FLUSSI_DIR="$BASE_MNT/SID-Flussi-e-bollo"

# function replace_in_file() {
#     if [ "$OS" = 'Darwin' ]; then
#         # for MacOS
#         sed -i '' -e "$1" "$2"
#     else
#         # for Linux and Windows
#         sed -i'' -e "$1" "$2"
#     fi
#     rm -f "$2-e"
# }

echo ""
# if [ ! -d "$FLUSSI_DIR/prog" ]; then
#     echo "Folder '$FLUSSI_DIR/prog' does not exist"
#     mkdir -p "$BASE_MNT"
#     cd "$BASE_MNT" || exit 1

#     # unzip "$BASE_MNT/firmatore-generico.zip"
#     tar xvf "$BASE_MNT/firmatore-generico.tar"
#     mkdir -p "$FLUSSI_DIR/log"
#     chmod -R 777 "$FLUSSI_DIR"/*
#     rm -rf "$FLUSSI_DIR/filetransfer"

    # replace_in_file "s/SID-Flussi-e-bollo/SID-Flussi-e-bollo/g" "$FLUSSI_DIR/config/predisposizione.cfg"
    # replace_in_file "s/SID-Flussi-e-bollo/SID-Flussi-e-bollo/g" "$FLUSSI_DIR/config/ricevute.cfg"

#     cd / || exit 1
# else
#     echo "Folder '$FLUSSI_DIR/prog' already exists"
# fi

echo ""
echo "Set java symbolic link"
rm -f /usr/bin/java
ln -s "$JAVA_HOME/bin/java" /usr/bin/java
ln -s "$BASE_MNT" "/mnt"