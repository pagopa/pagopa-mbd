
from azure.cosmos import CosmosClient
import uuid
import os

url = 'https://pagopa-<env>-weu-bizevents-ds-cosmos-account.documents.azure.com:443/'
key = '<<YOUR_COSMOS_DB_KEY>>'  # Replace with your actual Cosmos DB key
client = CosmosClient(url, key)



database_name = 'db'
database = client.get_database_client(database_name)
container_name = 'biz-events'
container = database.get_container_client(container_name)


for i in range(1, 100001):


    generated_uuid = str(uuid.uuid4())+"-mbd"
    print(f"{i} Inserting item with id: {generated_uuid}")
    container.upsert_item({
        "id": generated_uuid,
        "version": "2",
        "complete": "false",
        "receiptId": "043686905e7a4490afa831a95ac7e9eb",
        "missingInfo": [
            "idPaymentManager",
            "creditor.officeName",
            "psp.pspPartitaIVA",
            "payer",
            "paymentInfo.primaryCiIncurredFee",
            "paymentInfo.idBundle",
            "paymentInfo.idCiBundle"
        ],
        "debtorPosition": {
            "modelType": "2",
            "noticeNumber": "348175074887444364",
            "iuv": "211904306569699",
            "iur": "043686905e7a4490afa831a95ac7e9eb"
        },
        "creditor": {
            "idPA": "15376371009",
            "idBrokerPA": "15376371009",
            "idStation": "15376371009_48",
            "companyName": "PagoPA S.p.A."
        },
        "psp": {
            "idPsp": "BCITITMM",
            "idBrokerPsp": "00799960158",
            "idChannel": "00799960158_07",
            "psp": "Intesa Sanpaolo S.p.A",
            "pspFiscalCode": "00799960158",
            "channelDescription": "app"
        },
        "debtor": {
            "fullName": "Marina Verdi",
            "entityUniqueIdentifierType": "F",
            "entityUniqueIdentifierValue": "VRDMRN92A12H501Z",
            "streetName": "Via della Conciliazione",
            "civicNumber": "1",
            "postalCode": "00100",
            "city": "Roma",
            "stateProvinceRegion": "RM",
            "country": "IT",
            "eMail": "marina.verdi@mail.com"
        },
        "paymentInfo": {
            "paymentDateTime": "2025-06-24T09:08:00.246748",
            "applicationDate": "2025-06-24",
            "transferDate": "2025-06-25",
            "dueDate": "2025-06-25",
            "paymentToken": "043686905e7a4490afa831a95ac7e9eb",
            "amount": "350.3",
            "fee": "1.16",
            "totalNotice": "1",
            "paymentMethod": "creditCard",
            "touchpoint": "app",
            "remittanceInformation": "/RFB/211904306569699/334.30/TXT/DEBITORE/VRDMRN92A12H501Z",
            "description": "/RFB/211904306569699/334.30/TXT/DEBITORE/VRDMRN92A12H501Z",
            "metadata": [
                {
                    "key": "NOTIFICATION_FEE",
                    "value": "0"
                }
            ],
            "IUR": "043686905e7a4490afa831a95ac7e9eb"
        },
        "transferList": [
            {
                "idTransfer": "1",
                "fiscalCodePA": "15376371009",
                "companyName": "PagoPA S.p.A.",
                "amount": "334.3",
                "transferCategory": "9/0301109AP",
                "remittanceInformation": "/RFB/211904306569699/334.30/TXT/DEBITORE/VRDMRN92A12H501Z",
                "IBAN": "IT76P0306909790100000300089",
                "metadata": [
                    {
                        "key": "DatiSpecificiRiscossione",
                        "value": "9/0301109AP"
                    }
                ]
            },
            {
                "idTransfer": "2",
                "fiscalCodePA": "15376371009",
                "companyName": "PagoPA S.p.A.",
                "amount": "16.0",
                "transferCategory": "9/0301116TS/9/24B0060000000017",
                "remittanceInformation": "/RFB/211904306569699/16.00/TXT/DEBITORE/VRDMRN92A12H501Z",
                "MBDAttachment": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><marcaDaBollo xmlns=\"http://www.agenziaentrate.gov.it/2014/MarcaDaBollo\" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"><PSP><CodiceFiscale>00799960158</CodiceFiscale><Denominazione>Intesa Sanpaolo S.p.A.</Denominazione></PSP><IUBD>01240002186093</IUBD><OraAcquisto>2024-07-08T20:18:01+02:00</OraAcquisto><Importo>16.00</Importo><TipoBollo>01</TipoBollo><ImprontaDocumento><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><ns2:DigestValue>cXVlc3RhIMOoIHVuYSBtYXJjYSBkYSBib2xsbw==</ns2:DigestValue></ImprontaDocumento><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"/><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/><Reference URI=\"\"><Transforms><Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><DigestValue>R5J+YPJDj2NL/BGtOi0H6xpIcPQmPeLMXKOQlDwhVxI=</DigestValue></Reference></SignedInfo><SignatureValue>Sx9/pkyMXMmoWW/5QUq8kSyzOtFODrL3UTzQr2HDUIHaAffGPY0tzIAgs0R9TebGGj2XTcE5ZkTCwh+eT0Q4T6FvICglfUNE4ULmzOjLekmgMSX4ZoGJHoxhlBiOiyAFMoPHuPlYbkL0sMggqMRXqhmDg28of8OL58C/WB44Xe52SgNilmgkaQFeXvXU21Id8d0vhmSDRv9pXltaidoXVRtCXpsFrO42dfRyM1v/Kanm/vAVdk2yILhHPEpxiRLXBuW5obwKlnzNW4IzIIt/qVdz+DhGBf1yOtG3n1eO49tvSwPW2FK7kg1p2v4+5V/T2YQm+PnU7iyIJgSY+jDSZg==</SignatureValue><KeyInfo><X509Data><X509Certificate>MIIEtTCCAp2gAwIBAgIIVXapZ1v89YAwDQYJKoZIhvcNAQELBQAwaDELMAkGA1UEBhMCSVQxHjAcBgNVBAoMFUFnZW56aWEgZGVsbGUgRW50cmF0ZTEbMBkGA1UECwwSU2Vydml6aSBUZWxlbWF0aWNpMRwwGgYDVQQDDBNDQSBCb2xsbyBUZWxlbWF0aWNvMB4XDTE5MTAwMjEzNTgxN1oXDTI1MTAwMjEzNTgxN1owZTELMAkGA1UEBhMCSVQxHjAcBgNVBAoMFUFnZW56aWEgZGVsbGUgRW50cmF0ZTEMMAoGA1UECwwDUFNQMSgwJgYDVQQDDB8wMDc5OTk2MDE1OCBJTlRFU0EgU0FOUEFPTE8gU1BBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvKGguCHpJBDUWsRbYzaLxH7swW3q46TgJ+iP0DaiWbI/TBgkCFRrupNNxI6vR4xPiQ+Ca2x4A2qQcbmzy5SnVu0jKx56q9YCDDb5QQvXdyZRV+iApV8EgQbHbjqo2LqQGcVmeI4N7ozCed5pAzhwF++NfE2XqiGZWCh5bXanDk3a/xBy/bxY347MFLkjhNleOoKOcVtmZkLmq+StIqbyV66zOOmwwPx3Fxbt934bjEWRFNfVKC16Ld60wN0qy8gknJtN6h7McFmjGGkjulU3/+8ldN28w2x5cNgwp2hEcPp0Bf6gkSKQFoaN11o9/wJhK2Em6TWzeqiEAXUq/gHrOQIDAQABo2YwZDAfBgNVHSMEGDAWgBQqR7J/IUy/y94IjdL4Oervr95CRDASBgNVHSAECzAJMAcGBStMHQEJMB0GA1UdDgQWBBQqByzS4StvuC8cdWKRsm+eYngEPDAOBgNVHQ8BAf8EBAMCBkAwDQYJKoZIhvcNAQELBQADggIBAAaWtoo0GIIpzBydynec1pShQLduFKPHypuS41sTJwarIXEtc0PsErw6ZazqTi0jORCx15Qbo0Ylq4BWE7ZsSR14fgW9fEMSHGQeaxt1n4PeCPR7AJYwU6Lyk0CasSZYZgCwEBLxBr/hKGMrkikP2BFnLl5TPpAfXWZYPLpaUa8t/RHRm2Yf5xj9+GOFKvBc6e9liEG42Pg8wl7FVpAI/WnDHzkL0B8KkL5M8usviB8GtvQGuM1SZ7gsrcomq48cQSAx/r/WkPx5wsQ9H3CILDFEsyOv+M7xKyb7ARWywJo5AUNa4F0KZb9YBRmCGj7mbMjMLnamYJyDmbJ27K3R+hxrrclcfuR27HY2dTmutTqjrVffa3BjYVvnqBVGhEE6XqSUqvbRbM5uVaaXzpXBO0yFKWn4esCHdmYUZDmcATq5D1m7iO+uCtMS/AvLXO6u9cOrrQXFhqEDGBS71p+5W9IGd7e2z43Iq7MJogObullNHrX5/2LYY3Han+2dSXBlnFCwe+l9sYtfg1VVzGeNXua3qsbKus0SzTgsfRiJcWY3KtXTPze+aap7BxzV45qax9ljSvw8P5sEEC7GbsWkIxliSsL5Ey5h6d3Ji4z+ymZwoChYcIQQG+oNPV34MS3neJLyWb0HjxmnFn6OHCMt8Zs3kUd9+1b72P/EXa3EGggc</X509Certificate><X509CRL>MIIC4zCBzAIBATANBgkqhkiG9w0BAQsFADBoMQswCQYDVQQGEwJJVDEeMBwGA1UECgwVQWdlbnppYSBkZWxsZSBFbnRyYXRlMRswGQYDVQQLDBJTZXJ2aXppIFRlbGVtYXRpY2kxHDAaBgNVBAMME0NBIEJvbGxvIFRlbGVtYXRpY28XDTI1MDYyMzE5NTI1MVoXDTI1MDYyNDE5NTI1MFqgMDAuMB8GA1UdIwQYMBaAFCpHsn8hTL/L3giN0vg56u+v3kJEMAsGA1UdFAQEAgIY+TANBgkqhkiG9w0BAQsFAAOCAgEAWDBrtm8cCMgcabMrAXWuGKqcrWyz2X/LiA8kVUepcwPKqZX6getiazzPQNVcmCCIv61Zsd/22NNMfW8YJduKgOTma+n6Xx3MSRYHsOABmlN6R/Zd0keO6ST91M0vLiw5xDCUNbAuEAnXqmLcb3/Ee5+d5vzS9J8hHWcQdXpdlDFXHAcG71829+hUkkklebyAtPdhcKsIbrjprP4sd8RDxPsOW/Pm6r031uyeOjrfhqvNcahvXp9TFsXTwMwCe61H712zArsy6xeQrGxfpOlO0OVZBKMGDeb+ysMDX/3sDz+5tkIgzGLDgZTlBDLqP6u8xSK9WltRZXXxQ43QLQ9NiaKwUPV4PyHflc0eZwkazwJxW0F/I0iv+ifNCigz8h7c9D6uLTY+vftlKtQEfI3cNVgVi3Qm0asSqxn/yL2wTFTDSy9wLOWLJG1pDN6tmVj8bcfem2oNKPqT6O2hFegmFGKr7pmRoMt8/r5vWTkxC1BbgMGfnR5T4VVP8PZD3LMnEm/BzNdETKh1jOirXSEaUWAAW0nzt1Fd7HTdie2jrWLsLr/+zV9ss5Ro4vEpdxFWYP8FsO+Caqy9rwmk03jZnDD9SI8HYbSODhZlB+MOwsTGerv29373WaEGNSz2CqQ8VxtHk9hexUvpsTfBVBilBdWyM3S1V0QFzzjnnGbb3v4=</X509CRL></X509Data></KeyInfo></Signature></marcaDaBollo>",
                "metadata": [
                    {
                        "key": "DatiSpecificiRiscossione",
                        "value": "9/0301116TS/9/24B0060000000017"
                    }
                ]
            }
        ],
        "transactionDetails": {
            "user": {
                "type": "GUEST"
            },
            "transaction": {
                "transactionId": "Q4yzrybpMjoQKfrKtrFKa3Yu7g3NGCBX",
                "grandTotal": 35146,
                "amount": 35030,
                "fee": 115,
                "transactionStatus": "Confermato",
                "rrn": "633307409489",
                "authorizationCode": "575992",
                "creationDate": "2025-06-24T07:08:05Z",
                "psp": {
                    "idChannel": "00799960158_07",
                    "businessName": "Intesa Sanpaolo S.p.A"
                }
            },
            "info": {
                "brand": "MC",
                "brandLogo": "https://assets.cdn.platform.pagopa.it/creditcard/mastercard.png",
                "clientId": "CHECKOUT",
                "paymentMethodName": "CARDS",
                "type": "CP"
            }
        },
        "timestamp": 1751353486903,
        "properties": {
            "serviceIdentifier": "NDP004UAT"
        },
        "eventStatus": "DONE",
        "eventRetryEnrichmentCount": 0,
        "eventTriggeredBySchedule": "false"
    })