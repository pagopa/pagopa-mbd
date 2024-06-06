package it.gov.pagopa.mbd.service.model.csv;

import it.gov.pagopa.mbd.util.CsvUtils;
import lombok.*;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecordM implements Record{

    private static final String TIPO_RECORD = "M";
    private static final String CODICE_FLUSSO = "BDA00";
    private static final String CARATTERE_DI_CONTROLLO_CHIUSURA_RECORD = "F";

    private static final Integer TIPO_RECORD_LEN = 1;
    private static final Integer CODICE_FLUSSO_LEN = 6;
    private static final Integer CODICE_FISCALE_MITTENTE_LEN = 16;
    private static final Integer CODICE_FISCALE_PA_LEN = 16;
    private static final Integer DATA_INVIO_FLUSSO_MD_LEN = 10;
    private static final Integer PROG_INVIO_FLUSSO_MD_LEN = 2;
    private static final Integer DENOMINAZIONE_PA_LEN = 55;
    private static final Integer COMUNE_DOMIC_FISCALE_PA = 40;
    private static final Integer SIGLA_PROV_PA = 2;
    private static final Integer CAP_DOMICILIO_PA_LEN = 5;
    private static final Integer INDIRIZZO_PA_LEN = 35;
    private static final Integer DENOMINAZIONE_INT_LEN = 55;
    private static final Integer COMUNE_INT_LEN = 40;
    private static final Integer PROV_INT_LEN = 2;
    private static final Integer CAP_INT_LEN = 5;
    private static final Integer INDIRIZZO_INT_LEN = 35;
    private static final Integer FILLER_LEN = 1174;
    private static final Integer CARATTERE_CONTROLLO_CHIUSURA_LEN = 1;

    private String codiceFiscaleMittente;
    private String codiceFiscalePa;
    private String dataInvioFlussoMarcheDigitali;
    private Long progressivoInvioFlussoMarcheDigitali;
    private String denominazionePa;
    private String comuneDomicilioFiscalePa;
    private String siglaDellaProvinciaDelDomicilioFiscalePa;
    private String CAPDelDomicilioFiscalePa;
    private String indirizzoFrazioneViaENumeroCivicoDelDomicilioFiscalePa;
    private String demoninazioneIntermediario;
    private String comuneDomicilioFiscaleIntermediario;
    private String siglaDellaProvinciaDelDomicilioFiscaleIntermediario;
    private Long CAPDelDomicilioFiscaleIntermediario;
    private String indirizzoFrazioneViaENumeroCivicoDelDomicilioFiscaleIntermediario;
    private String filler;

    public String toLine() {
        return CsvUtils.toFixedLength(TIPO_RECORD, TIPO_RECORD_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(CODICE_FLUSSO, CODICE_FLUSSO_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(codiceFiscaleMittente, CODICE_FISCALE_MITTENTE_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(codiceFiscalePa, CODICE_FISCALE_PA_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(dataInvioFlussoMarcheDigitali, DATA_INVIO_FLUSSO_MD_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(progressivoInvioFlussoMarcheDigitali, PROG_INVIO_FLUSSO_MD_LEN, RecordAlignEnum.ALIGN_RIGHT, '0') +
                    CsvUtils.toFixedLength(denominazionePa, DENOMINAZIONE_PA_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(comuneDomicilioFiscalePa, COMUNE_DOMIC_FISCALE_PA, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(siglaDellaProvinciaDelDomicilioFiscalePa, SIGLA_PROV_PA, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(CAPDelDomicilioFiscalePa, CAP_DOMICILIO_PA_LEN, RecordAlignEnum.ALIGN_RIGHT, '0') +
                    CsvUtils.toFixedLength(indirizzoFrazioneViaENumeroCivicoDelDomicilioFiscalePa, INDIRIZZO_PA_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(demoninazioneIntermediario, DENOMINAZIONE_INT_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(comuneDomicilioFiscaleIntermediario, COMUNE_INT_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(siglaDellaProvinciaDelDomicilioFiscaleIntermediario, PROV_INT_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(CAPDelDomicilioFiscaleIntermediario, CAP_INT_LEN, RecordAlignEnum.ALIGN_RIGHT, '0') +
                    CsvUtils.toFixedLength(indirizzoFrazioneViaENumeroCivicoDelDomicilioFiscaleIntermediario, INDIRIZZO_INT_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(filler, FILLER_LEN, RecordAlignEnum.ALIGN_LEFT, ' ') +
                    CsvUtils.toFixedLength(CARATTERE_DI_CONTROLLO_CHIUSURA_RECORD, CARATTERE_CONTROLLO_CHIUSURA_LEN, RecordAlignEnum.ALIGN_LEFT, ' ');
    }



}
