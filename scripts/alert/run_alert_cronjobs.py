from azure.storage.file import *
import argparse
import logging
import datetime
from zoneinfo import ZoneInfo
import requests
import sys

# Configurazione del logging
logging.basicConfig(
    level=logging.ERROR,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger("mbd-files-extractor")
logger.setLevel(logging.INFO)

def parse_arguments():
    """Parse command line arguments"""
    parser = argparse.ArgumentParser(description='Extract mbd events from Azure storage account')

    # Parametro per connessione a Azure storage account
    parser.add_argument('--storage_conn_string', required=True,
                        help='Storage account connection string')

    # Parametro webhook per slack
    parser.add_argument('--slack-webhook', required=True,
                        help='Slack channel webhook')

    return parser.parse_args()

def format_bullet_list_from_set(item_set):
    return '\n'.join([f"• *{item}*" for item in item_set])

def send_slack_alert(url, payload):
    response = requests.post(url, json = payload)
    if not response.ok:
        logger.error("Error sending message to slack via webhook")
        sys.exit(1)


def get_datetime_one_week():
    return datetime.datetime.now(ZoneInfo("Europe/Rome")) - datetime.timedelta(days=7)

def alert_file_da_predisporre(file_service, share_name, directory_predisporre_name, slack_webhook):
    try:
        files_predisporre_backup = file_service.list_directories_and_files(share_name, directory_predisporre_name)
    except Exception as e:
        logger.error(f"Error retrieving files in \"file_da_predisporre\" directory")
        sys.exit(1)

    time_one_week = get_datetime_one_week()
    recent_file_found = False

    for file_da_predisporre in files_predisporre_backup:
        if isinstance(file_da_predisporre, File):
            file_name = file_da_predisporre.name
            file = file_service.get_file_properties(share_name, directory_predisporre_name, file_name, timeout=None, snapshot=None)
            if file.properties.last_modified > time_one_week:
                recent_file_found = True
                break

    if not recent_file_found:
        payload = {
            "blocks": [
                    {
                    "type": "section",
                    "text": {
                        "type": "mrkdwn",
                        "text": "*ALERT: Nessun file da predisporre negli ultimi 7 giorni*"
                        }
                    }
                ],
            "username": "RMBD-Alert",
            "icon_emoji": ":sos:"
        }
        send_slack_alert(slack_webhook, payload)
        logger.warning("ALERT FILE PREDISPORRE")

def alert_file_da_inviare(file_service, share_name, directory_predisporre_name, directory_inviare_name, slack_webhook):
    try:
        files_predisporre_backup = file_service.list_directories_and_files(share_name, directory_predisporre_name)
        files_inviare_backup = file_service.list_directories_and_files(share_name, directory_inviare_name)
    except Exception as e:
        logger.error(f"Error retrieving files in \"file_da_predisporre\" and \"file_da_inviare\" directory")
        sys.exit(1)

    time_one_week = get_datetime_one_week()

    file_da_inviare_list_last_week = set()
    file_da_predisporre_non_presenti = set()

    for file_da_inviare in files_inviare_backup:
        if isinstance(file_da_inviare, File):
            file_name = file_da_inviare.name
            file = file_service.get_file_properties(share_name, directory_inviare_name, file_name, timeout=None, snapshot=None)
            if file.properties.last_modified > time_one_week:
                file_da_inviare_list_last_week.add(file_name)

    for file_da_predisporre in files_predisporre_backup:
        if isinstance(file_da_predisporre, File):
            if not file_da_predisporre.name in file_da_inviare_list_last_week:
                file_da_predisporre_non_presenti.add(file_da_predisporre.name)

    if len(file_da_predisporre_non_presenti) > 0:
        payload = {
                    "blocks": [
                            {
                            "type": "section",
                            "text": {
                                "type": "mrkdwn",
                                "text": "*ALERT: Lista dei file da predisporre non inviati nella scorsa settimana:*"
                                }
                            },
                            {
                            "type": "section",
                            "text": {
                                "type": "mrkdwn",
                                "text": f"{format_bullet_list_from_set(file_da_predisporre_non_presenti)}"
                                }
                            }
                        ],
                    "username": "RMBD-Alert",
                    "icon_emoji": ":sos:"
                }
        logger.warning("ALERT FILE INVIARE")
        send_slack_alert(slack_webhook, payload)

def main():

    args = parse_arguments()
    conn_string_storage = args.storage_conn_string
    slack_webhook = args.slack_webhook

    file_service = FileService(connection_string=conn_string_storage)
    share_name = "firmatore"
    directory_predisporre_name = "backup/SID-Flussi-e-bollo/SID_cartelle/file_da_predisporre"
    directory_inviare_name = "backup/SID-Flussi-e-bollo/SID_cartelle/file_da_inviare"

    alert_file_da_predisporre(file_service, share_name, directory_predisporre_name, slack_webhook)

    alert_file_da_inviare(file_service, share_name, directory_predisporre_name, directory_inviare_name, slack_webhook)

    logger.info("Operation completed successfully")

if __name__ == "__main__":
    main()