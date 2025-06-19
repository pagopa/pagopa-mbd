#!/bin/bash

# Inizializzazione ambiente e variabili
sh $BASE_MNT/scripts/init.sh

echo ""
echo "Start runflussi"
cd "$FLUSSI_DIR/prog" || {
  echo "Directory $FLUSSI_DIR/prog non trovata"
  exit 1
}
./runFlussi.sh 
# cd /app || exit 1
echo "End runflussi"

echo ""\
echo "Start backup"
PREDISP_DIR="$FLUSSI_DIR/SID_cartelle/file_da_predisporre"
BACKUP_DIR="$BASE_MNT/backup/SID-Flussi-e-bollo/SID_cartelle/file_da_predisporre"

if [ -z "$(ls -A "$PREDISP_DIR" 2>/dev/null)" ]; then
   echo "Folder $PREDISP_DIR is empty or does not exist"
else
    mkdir -p "$BACKUP_DIR"
    mv -v "$PREDISP_DIR"/* "$BACKUP_DIR"
fi
echo "End backup"