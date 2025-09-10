from azure.storage.file import *
import argparse
import logging
import datetime
from zoneinfo import ZoneInfo

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

    # Parametri per connessione a Azure storage account
    parser.add_argument('--storage_conn_string', required=True,
                        help='Storage account connection string')

    return parser.parse_args()

def get_datetime_one_week():
    return datetime.datetime.now(ZoneInfo("Europe/Rome")) - datetime.timedelta(days=7)

def alert_file_da_predisporre(file_service, share_name, directory_predisporre_name):
    files_predisporre_backup = file_service.list_directories_and_files(share_name, directory_predisporre_name)
    time_one_week = get_datetime_one_week()

    for file_da_predisporre in files_predisporre_backup:
        if isinstance(file_da_predisporre, File):
            file_name = file_da_predisporre.name
            file = file_service.get_file_properties(share_name, directory_predisporre_name, file_name, timeout=None, snapshot=None)
            if file.properties.last_modified > time_one_week:
                break
    else:
        logger.error("ALERT HERE")

def alert_file_da_inviare(file_service, share_name, directory_predisporre_name, directory_inviare_name):
    files_predisporre_backup = file_service.list_directories_and_files(share_name, directory_predisporre_name)
    files_inviare_backup = file_service.list_directories_and_files(share_name, directory_inviare_name)
    time_one_week = get_datetime_one_week()

    file_da_inviare_list_last_week = set()

    for file_da_inviare in files_inviare_backup:
        if isinstance(file_da_inviare, File):
            file_name = file_da_inviare.name
            file = file_service.get_file_properties(share_name, directory_inviare_name, file_name, timeout=None, snapshot=None)
            if file.properties.last_modified > time_one_week:
                file_da_inviare_list_last_week.add(file_name)

    for file_da_predisporre in files_predisporre_backup:
        if isinstance(file_da_predisporre, File):
            if not file_da_predisporre.name in file_da_inviare_list_last_week:
                logger.error("ALERT HERE")

def main():

    args = parse_arguments()
    conn_string_storage = args.storage_conn_string

    file_service = FileService(connection_string=conn_string_storage)
    share_name = "firmatore"
    directory_predisporre_name = "backup/SID-Flussi-e-bollo/SID_cartelle/file_da_predisporre"
    directory_inviare_name = "backup/SID-Flussi-e-bollo/SID_cartelle/file_da_inviare"

    alert_file_da_predisporre(file_service, share_name, directory_predisporre_name)

    alert_file_da_inviare(file_service, share_name, directory_predisporre_name, directory_inviare_name)

    logger.info("Operation completed successfully")

if __name__ == "__main__":
    main()