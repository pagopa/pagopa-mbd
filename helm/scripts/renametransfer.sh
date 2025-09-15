#!/bin/bash

# Inizializzazione ambiente e variabili
sh $BASE_MNT/scripts/init.sh

echo ""
echo "Start renametransfer"

IN_DIR="$FLUSSI_DIR/SID_cartelle/file_da_inviare"
BACKUP_DIR="$BASE_MNT/backup/SID-Flussi-e-bollo/SID_cartelle/file_da_inviare"

count=$(find "$IN_DIR" -type f -name "PEC*" | wc -l)
if [ "$count" -gt 0 ]; then
    cd "$IN_DIR" || {
      echo "Directory $IN_DIR non trovata"
      exit 1
    }

    for name in PEC*; do
       newname="SIA$(echo "$name" | cut -c4-)"
       mv "$name" "$newname"
    done

    # cd /app || exit 1

    echo ""
    echo "Start upload"
    mkdir -p "$IN_DIR"

    # echo "mput $IN_DIR/PAGPA* /Inbox/" | sftp -o StrictHostKeyChecking=no -P "${SFTP_PORT}" -i /app/certs/firmatore.pem "${SFTP_USERNAME}@${SFTP_HOST}"

    # Salva la chiave in un file temporaneo
    KEY_FILE=$(mktemp)
    echo "$SFTP_PEM_KEY" > "$KEY_FILE"
    chmod 600 "$KEY_FILE"
    # chmod 600 "$FLUSSI_DIR/pagopa_pgsg_rsa"
    # Esegui sftp usando il file temporaneo

    echo "mput $IN_DIR/SIA* /Inbox/" | sftp -o StrictHostKeyChecking=no -P "${SFTP_PORT}" -i "$KEY_FILE" "${SFTP_USERNAME}@${SFTP_HOST}"
    # echo "mput $IN_DIR/PAGPA* /Inbox/" | sftp -o StrictHostKeyChecking=no -P "${SFTP_PORT}" -i "$FLUSSI_DIR/pagopa_pgsg_rsa" "${SFTP_USERNAME}@${SFTP_HOST}"
    # Cancella il file temporaneo
    rm -f "$KEY_FILE"

    # to check 
    # echo "ls /Inbox/" | sftp -o StrictHostKeyChecking=no -P "22" -i "$KEY_FILE" "pagopa_pgsg@sftp1.public.pdnd.pagopa.it"


    echo "End upload"

    echo ""
    echo "Start backupfiles"
    count=$(find "$IN_DIR" -type f -name "SIA*" | wc -l)
    if [ "$count" -gt 0 ]; then
        mkdir -p "$BACKUP_DIR"
        mv -v "$IN_DIR"/SIA* "$BACKUP_DIR"
    else
        echo "Files $IN_DIR/SIA* not found"
    fi
    echo "End backupfiles"
else
    echo "Files $IN_DIR/PEC* not found"
fi

echo "End renametransfer"
