from azure.storage.fileshare import ShareDirectoryClient
import datetime
import argparse
import logging
import sys
import csv
from io import StringIO
from slack_sdk import WebClient
from slack_sdk.errors import SlackApiError

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

def get_today_date():
    today = datetime.datetime.today()
    return today.strftime('%Y-%m-%d')

def create_csv(output_file, list_to_insert):
    with open(output_file, 'w') as out_write:
        writer = csv.writer(out_write)
        for line in list_to_insert:
            writer.writerow(line)

def update_csv(resoconto_jobs_file_content, output_file, file_da_predisporre, file_da_inviare):
    reader = csv.reader(StringIO(resoconto_jobs_file_content.decode()), delimiter=",")
    reader_list = list(reader)
    for row in reader_list:
        if get_today_date() == row[0]:
            logger.error(f"Row with today date already present: {get_today_date()}")
            sys.exit(1)
    reader_list.append([get_today_date(), len(file_da_predisporre), len(file_da_inviare)])
    if len(reader_list) > 28:
        reader_list.pop(1)
    return reader_list

def upload_csv(args, file_da_predisporre, file_da_inviare):
    firmatore = ShareDirectoryClient.from_connection_string(conn_str=args.storage_conn_string,share_name="firmatore",directory_path="")
    csv_folder = firmatore.get_subdirectory_client(directory_name="resoconto_jobs")
    output_file = "resoconto_jobs.csv"
    if csv_folder.exists():
        resoconto_jobs_file = csv_folder.get_file_client(output_file)
        resoconto_jobs_file_content = resoconto_jobs_file.download_file().readall() #eccezione qua
        reader_list = update_csv(resoconto_jobs_file_content, output_file, file_da_predisporre, file_da_inviare)
        create_csv(output_file, reader_list)
    else:
        list_to_insert = [["Data", "File da predisporre", "File da inviare"], [get_today_date(), len(file_da_predisporre), len(file_da_inviare)]]
        create_csv(output_file, list_to_insert)
        csv_folder = firmatore.create_subdirectory(directory_name="resoconto_jobs")

    with open(output_file, 'rb') as file:
        csv_bytes = file.read()
        csv_folder.upload_file(file_name="resoconto_jobs.csv",data=csv_bytes)

    return output_file

def main():

    args = parse_arguments()
    conn_string_storage = args.storage_conn_string

    cartella_predisporre = ShareDirectoryClient.from_connection_string(conn_str=conn_string_storage,share_name="firmatore",directory_path="./SID-Flussi-e-bollo/SID_cartelle/file_da_predisporre")
    cartella_inviare = ShareDirectoryClient.from_connection_string(conn_str=conn_string_storage,share_name="firmatore",directory_path="./SID-Flussi-e-bollo/SID_cartelle/file_da_inviare")

    lista_predisporre = list(cartella_predisporre.list_directories_and_files())
    lista_inviare = list(cartella_inviare.list_directories_and_files())
    output_file = upload_csv(args, lista_predisporre, lista_inviare)

    bot_token = args.slack_webapi_token
    channel_id = args.slack_channel_id

    client = WebClient(token = bot_token)
    try:
        result = client.files_upload_v2(
            channel = channel_id,
            initial_comment = f"Generazione del report cumulativo per i file mbd [{today_date}]",
            file = output_file,
        )
        logger.info(f"Response from Slack. Is OK? [{result['ok']}]")

    except SlackApiError as e:
        logger.error("Error uploading file: {}".format(e))

    logger.info("Operation completed successfully")

if __name__ == "__main__":
    main()