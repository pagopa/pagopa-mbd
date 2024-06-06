package it.gov.pagopa.mbd.service.model.csv;

import it.gov.pagopa.mbd.util.CsvUtils;
import lombok.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecordV implements Record{

    private static final String TIPO_RECORD = "V";
    private static final String CODICE_FLUSSO = "BDA00";
    private static final String CARATTERE_DI_CONTROLLO_CHIUSURA_RECORD = "F";
    private static final String TIPO_MODELLO = "1";

    private static final Integer TIPO_RECORD_LEN = 1;
    private static final Integer CODICE_FLUSSO_LEN = 6;
    private static final Integer CODICE_FISCALE_MITTENTE_LEN = 16;
    private static final Integer CODICE_FISCALE_PA_LEN = 16;
    private static final Integer DATA_INVIO_FLUSSO_MD_LEN = 10;
    private static final Integer PROG_INVIO_FLUSSO_MD_LEN = 2;
    private static final Integer TIPO_MODELLO_LEN = 1;
    private static final Integer IMPRONTA_DOCUMENTO_INFORMATICO_LEN = 44;
    private static final Integer IUBD_LEN = 14;
    private static final Integer CF_PSP_LEN = 16;
    private static final Integer DENOMINAZIONE_PSP_LEN = 55;
    private static final Integer DATA_VENDITA_LEN = 10;
    private static final Integer FILLER_LEN = 57;
    private static final Integer CARATTERE_CONTROLLO_CHIUSURA_LEN = 1;

    private String codiceFiscaleMittente;
    private String codiceFiscalePa;
    private String dataInvioFlussoMarcheDigitali;
    private Long progressivoInvioFlussoMarcheDigitali;
    private String improntaDocumentoInformatico;
    private String IUBD;
    private String codiceFiscalePsp;
    private String denominazionePsp;
    private String dataDiVendita;
    private String filler;

    public String toLine() {
        StringBuilder line = new StringBuilder(CsvUtils.toFixedLength(TIPO_RECORD, TIPO_RECORD_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(CODICE_FLUSSO, CODICE_FLUSSO_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(codiceFiscaleMittente, CODICE_FISCALE_MITTENTE_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(codiceFiscalePa, CODICE_FISCALE_PA_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(dataInvioFlussoMarcheDigitali, DATA_INVIO_FLUSSO_MD_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(progressivoInvioFlussoMarcheDigitali, PROG_INVIO_FLUSSO_MD_LEN, RecordAlignEnum.ALIGN_RIGHT, '0') +
                CsvUtils.toFixedLength(TIPO_MODELLO, TIPO_MODELLO_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(improntaDocumentoInformatico, IMPRONTA_DOCUMENTO_INFORMATICO_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(IUBD, IUBD_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(codiceFiscalePsp, CF_PSP_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(denominazionePsp, DENOMINAZIONE_PSP_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(dataDiVendita, DATA_VENDITA_LEN, RecordAlignEnum.ALIGN_LEFT, ' '));
        for (int i = 0; i <= 10; i++) {
            line.append(CsvUtils.toFixedLength("", IMPRONTA_DOCUMENTO_INFORMATICO_LEN + IUBD_LEN + CF_PSP_LEN + DENOMINAZIONE_PSP_LEN + DATA_VENDITA_LEN, RecordAlignEnum.ALIGN_LEFT, ' '));
        }
        line.append(CsvUtils.toFixedLength(filler, FILLER_LEN, RecordAlignEnum.ALIGN_LEFT, ' '));
        line.append(CsvUtils.toFixedLength(CARATTERE_DI_CONTROLLO_CHIUSURA_RECORD, CARATTERE_CONTROLLO_CHIUSURA_LEN, RecordAlignEnum.ALIGN_LEFT, ' '));
        return line.toString();
    }
}