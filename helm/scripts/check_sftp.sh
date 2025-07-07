export SFTP_PEM_KEY="-----BEGIN OPENSSH PRIVATE KEY-----\n\n << YOUR_SFTP_PEM_KEY >> \n-----END OPENSSH PRIVATE KEY-----\n"

KEY_FILE=$(mktemp)
echo "$SFTP_PEM_KEY" > "$KEY_FILE"
chmod 600 "$KEY_FILE"
#Create Inbox folder and upload the test file using SFTP batch commands

echo "Create /Inbox folder"
sftp -o StrictHostKeyChecking=no -P "22" -i "$KEY_FILE" "pagopa_pgsg@sftp1.public.pdnd.pagopa.it" <<EOF
mkdir /Inbox
mput ./readme-md /Inbox/
EOF

echo "Create /output folder"
sftp -o StrictHostKeyChecking=no -P "22" -i "$KEY_FILE" "pagopa_pgsg@sftp1.public.pdnd.pagopa.it" <<EOF
mkdir /output
mput ./readme-md /output/
EOF


echo ">>>>>>>>>>>>>>>>>>>>>>"
echo ">>> SHOW /Inbox folder"
echo ">>>>>>>>>>>>>>>>>>>>>>"
echo "ls /Inbox" | sftp -o StrictHostKeyChecking=no -P "22" -i "$KEY_FILE" "pagopa_pgsg@sftp1.public.pdnd.pagopa.it"

echo ">>>>>>>>>>>>>>>>>>>>>>"
echo ">>> SHOW /output folder"
echo ">>>>>>>>>>>>>>>>>>>>>>"
echo "ls /output" | sftp -o StrictHostKeyChecking=no -P "22" -i "$KEY_FILE" "pagopa_pgsg@sftp1.public.pdnd.pagopa.it"

rm -f "$KEY_FILE"