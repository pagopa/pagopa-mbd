#!/bin/bash

# Inizializzazione ambiente e variabili
sh $BASE_MNT/scripts/init.sh

echo ""
echo "Start get"

INVIATI_DIR="$FLUSSI_DIR/SID_cartelle/file_da_inviare"
RICEVUTE_DIR="$FLUSSI_DIR/SID_cartelle/ricevute_da_elaborare"
ELABORATE_DIR="$FLUSSI_DIR/SID_cartelle/ricevute_elaborate"
BACKUP_DIR="$BASE_MNT/backup/SID-Flussi-e-bollo/SID_cartelle/ricevute_elaborate"

mkdir -p "$INVIATI_DIR"

# Salva la chiave in un file temporaneo
KEY_FILE=$(mktemp)
echo "$SFTP_PEM_KEY" > "$KEY_FILE"
chmod 600 "$KEY_FILE"

count=$(echo "ls -l /output/" | sftp -o StrictHostKeyChecking=no -P "${SFTP_PORT}" -i "$KEY_FILE" "${SFTP_USERNAME}@${SFTP_HOST}" | grep -v '^sftp>' | grep 'ATSIAA' | wc -l)
# count=$(echo "ls -l /output/" | sftp -o StrictHostKeyChecking=no -P "${SFTP_PORT}" -i /app/certs/firmatore.pem "${SFTP_USERNAME}@${SFTP_HOST}" | grep -v '^sftp>' | grep 'ATSIAA' | wc -l)
if [ "$count" -gt 0 ]; then
    echo "Found $count files"
    echo "mget /output/ATSIAA* $RICEVUTE_DIR/" | sftp -o StrictHostKeyChecking=no -P "${SFTP_PORT}" -i "$KEY_FILE" "${SFTP_USERNAME}@${SFTP_HOST}"
    # echo "mget /output/ATSIAA* $RICEVUTE_DIR/" | sftp -o StrictHostKeyChecking=no -P "${SFTP_PORT}" -i /app/certs/firmatore.pem "${SFTP_USERNAME}@${SFTP_HOST}"
else
    echo "Files /output/ATSIAA* not found"
fi

echo "End get"

# Cancella il file temporaneo
rm -f "$KEY_FILE"


echo ""
echo "Run runRicevute"
cd "$FLUSSI_DIR/prog/" || exit 1
./runRicevute.sh
# cd /app || exit 1
echo "End runRicevute"

echo ""
echo "Start backup"
count=$(find "$ELABORATE_DIR" -type f -name "ATSIAA*" | wc -l)
if [ "$count" -gt 0 ]; then
    mkdir -p "$BACKUP_DIR"
    mv -v "$ELABORATE_DIR"/ATSIAA* "$BACKUP_DIR"/
else
    echo "Files $ELABORATE_DIR/ATSIAA* not found"
fi
echo "End backup"
