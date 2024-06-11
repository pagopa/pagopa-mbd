package it.gov.pagopa.mbd.service.model.csv;

import it.gov.pagopa.mbd.util.CsvUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecordZ {

    private static final String TIPO_RECORD = "Z";
    private static final String CODICE_FLUSSO = "BDA00";
    private static final String CARATTERE_DI_CONTROLLO_CHIUSURA_RECORD = "F";
    private static final Long NUMERO_DI_RECORD_DI_TIPO_M = 1L;

    private static final Integer TIPO_RECORD_LEN = 1;
    private static final Integer CODICE_FLUSSO_LEN = 6;
    private static final Integer CODICE_FISCALE_MITTENTE_LEN = 16;
    private static final Integer CODICE_FISCALE_PA_LEN = 16;
    private static final Integer DATA_INVIO_FLUSSO_MD_LEN = 10;
    private static final Integer PROG_INVIO_FLUSSO_MD_LEN = 2;
    private static final Integer NUMERO_RECORD_TIPO_V_LEN = 9;
    private static final Integer NUMERO_RECORD_TIPO_M_LEN = 9;
    private static final Integer SPAZIO_DISPONIBILE_LEN = 1430;
    private static final Integer CARATTERE_CONTROLLO_LEN = 1;

    private String codiceFiscaleMittente;
    private String codiceFiscalePa;
    private String dataInvioFlussoMarcheDigitali;
    private Long progressivoInvioFlussoMarcheDigitali;
    private Long numeroRecordDiTipoV;
    private String spazioDisponibile;

    public String toLine() {
        return CsvUtils.toFixedLength(TIPO_RECORD, TIPO_RECORD_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(CODICE_FLUSSO, CODICE_FLUSSO_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(codiceFiscaleMittente, CODICE_FISCALE_MITTENTE_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(codiceFiscalePa, CODICE_FISCALE_PA_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(dataInvioFlussoMarcheDigitali, DATA_INVIO_FLUSSO_MD_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(progressivoInvioFlussoMarcheDigitali, PROG_INVIO_FLUSSO_MD_LEN, RecordAlignEnum.ALIGN_RIGHT, '0') +
                CsvUtils.toFixedLength(numeroRecordDiTipoV, NUMERO_RECORD_TIPO_V_LEN, RecordAlignEnum.ALIGN_RIGHT, '0') +
                CsvUtils.toFixedLength(NUMERO_DI_RECORD_DI_TIPO_M, NUMERO_RECORD_TIPO_M_LEN, RecordAlignEnum.ALIGN_RIGHT, '0') +
                CsvUtils.toFixedLength(spazioDisponibile, SPAZIO_DISPONIBILE_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                CsvUtils.toFixedLength(CARATTERE_DI_CONTROLLO_CHIUSURA_RECORD, CARATTERE_CONTROLLO_LEN, RecordAlignEnum.ALIGN_LEFT, ' ');
    }

}
