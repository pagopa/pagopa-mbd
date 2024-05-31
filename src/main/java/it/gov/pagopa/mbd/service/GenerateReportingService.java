package it.gov.pagopa.mbd.service;

import it.gov.pagopa.gen.mbd.client.cache.model.CreditorInstitutionDto;
import it.gov.pagopa.gen.mbd.client.cache.model.PaymentServiceProviderDto;
import it.gov.pagopa.mbd.repository.BizEventRepository;
import it.gov.pagopa.mbd.repository.model.BizEventEntity;
import it.gov.pagopa.mbd.repository.model.Transfer;
import it.gov.pagopa.mbd.service.model.csv.RecordA;
import it.gov.pagopa.mbd.service.model.csv.RecordM;
import it.gov.pagopa.mbd.service.model.csv.RecordV;
import it.gov.pagopa.mbd.service.model.csv.RecordZ;
import it.gov.pagopa.mbd.util.CommonUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@CacheConfig(cacheNames="cache")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mbd.rendicontazioni.generate", name = "enabled")
public class GenerateReportingService {

//    private final FtpClient ftpClient;

    private static final DateTimeFormatter formatterHours = DateTimeFormatter.ofPattern("HHmmss");

    private static final String DEFAULT_CODICE_FISCALE = "0000000000000000";
    private static final int BOLLI_PSP_GROUPED = 10;
    private static final String CODICE_FLUSSO_NORMALE = "A5";
    private static final int DAY_OF_YEAR_LEN = 3;

    private final ConfigCacheService configCacheService;
    private final BizEventRepository bizEventRepository;

    @Value("mbd.rendicontazioni.filePath")
    private String fileSystemPath;

    public void execute(LocalDate date) {
        log.info("generate reporting MBD for {}", date);

        long dateFrom = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long dateTo = date.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC).toEpochMilli();

        String rendicontazioneBolloDateFormat =
                CommonUtility.getConfigKeyValueCache(configCacheService.getConfigData().getConfigurations(), "rendicontazioni-bollo.dateFormat");
        String mittenteCodiceFiscale = CommonUtility.getConfigKeyValueCache(configCacheService.getConfigData().getConfigurations(), "rendicontazioni-bollo.mittente.codiceFiscale");
        String intermediarioDenominazione = CommonUtility.getConfigKeyValueCache(configCacheService.getConfigData().getConfigurations(), "rendicontazioni-bollo.intermediario.denominazione");

        String intermediarioComune = CommonUtility.getConfigKeyValueCache(configCacheService.getConfigData().getConfigurations(), "rendicontazioni-bollo.intermediario.comuneDomicilioFiscale");
        String intermediarioSiglaProvincia = CommonUtility.getConfigKeyValueCache(configCacheService.getConfigData().getConfigurations(), "rendicontazioni-bollo.intermediario.siglaDellaProvinciaDelDomicilioFiscale");
        String intermediarioCap = CommonUtility.getConfigKeyValueCache(configCacheService.getConfigData().getConfigurations(), "rendicontazioni-bollo.intermediario.CAPDelDomicilioFiscale");
        String intermediarioIndirizzo = CommonUtility.getConfigKeyValueCache(configCacheService.getConfigData().getConfigurations(), "rendicontazioni-bollo.intermediario.indirizzoFrazioneViaENumeroCivicoDelDomicilioFiscale");
        String codiceTrasmissivo = CommonUtility.getConfigKeyValueCache(configCacheService.getConfigData().getConfigurations(), "rendicontazioni-bollo.codiceIdentificativo.1");

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(rendicontazioneBolloDateFormat);

        List<BizEventEntity> bizEvents = bizEventRepository.getBizEventsByDateFromAndDateTo(dateFrom, dateTo);

        if( !bizEvents.isEmpty() ) {
            Map<String, CreditorInstitutionDto> organizations = configCacheService.getConfigData().getCreditorInstitutions();
            AtomicInteger progressivo = new AtomicInteger(0);
            Map<String, PaymentServiceProviderDto> psps = configCacheService.getConfigData().getPsps();
            organizations.values().forEach(pa -> {
                LocalDateTime now = LocalDateTime.now();
                String dataInvioFlusso = now.format(dateFormat);
                log.debug("PA:{} - Creazione RecordA", pa.getCreditorInstitutionCode());
                RecordA recordA = RecordA
                        .builder()
                        .codiceFiscaleMittente(mittenteCodiceFiscale)
                        .codiceFiscalePa(pa.getCreditorInstitutionCode())
                        .dataInvioFlussoMarcheDaBollo(dataInvioFlusso)
                        .progressivoInvioFlussoMarcheDigitali(progressivo.longValue())
                        .build();

                log.debug("PA:{} - Creazione RecordM", pa.getCreditorInstitutionCode());
                RecordM recordM = new RecordM();
                recordM.setCodiceFiscaleMittente(mittenteCodiceFiscale);
                recordM.setCodiceFiscalePa(pa.getCreditorInstitutionCode());
                recordM.setDataInvioFlussoMarcheDigitali(dataInvioFlusso);
                recordM.setProgressivoInvioFlussoMarcheDigitali(progressivo.longValue());
                recordM.setDenominazionePa(pa.getBusinessName());
                if( pa.getAddress() != null ) {
                    recordM.setComuneDomicilioFiscalePa(pa.getAddress().getCity());
                    recordM.setSiglaDellaProvinciaDelDomicilioFiscalePa(pa.getAddress().getLocation());
                    recordM.setCAPDelDomicilioFiscalePa(pa.getAddress().getZipCode());
                    recordM.setIndirizzoFrazioneViaENumeroCivicoDelDomicilioFiscalePa(pa.getAddress().getTaxDomicile());
                }
                recordM.setDemoninazioneIntermediario(intermediarioDenominazione);
                recordM.setComuneDomicilioFiscaleIntermediario(intermediarioComune);
                recordM.setSiglaDellaProvinciaDelDomicilioFiscaleIntermediario(intermediarioSiglaProvincia);
                recordM.setCAPDelDomicilioFiscaleIntermediario(Long.getLong(intermediarioCap));
                recordM.setIndirizzoFrazioneViaENumeroCivicoDelDomicilioFiscaleIntermediario(intermediarioIndirizzo);

                log.debug("PA:{} - Creazione Lista RecordV", pa.getCreditorInstitutionCode());
                List<RecordV> recordsV = bizEvents.stream().map(b -> {
                    RecordV recordV = new RecordV();
                    recordV.setCodiceFiscaleMittente(mittenteCodiceFiscale);
                    recordV.setCodiceFiscalePa(pa.getCreditorInstitutionCode());
                    recordV.setDataInvioFlussoMarcheDigitali(dataInvioFlusso);
                    recordV.setProgressivoInvioFlussoMarcheDigitali(progressivo.longValue());
                    recordV.setImprontaDocumentoInformatico(b.getTransferList().stream().map(Transfer::getMBDAttachment).toList());
//                recordV.setIUBD(b.getTransferList().stream().map(Transfer::get));//TODO: da dove lo si recupera?
//                recordV.setCodiceFiscalePsp(psps.get(b.getPsp().getIdPsp()).getTaxCode());
//                recordV.setDenominazionePsp();
//                recordV.setDataDiVendita();
                    return recordV;
                }).toList();

                log.debug("PA:{} - Creazione RecordZ", pa.getCreditorInstitutionCode());
                RecordZ recordZ = new RecordZ();
                recordZ.setCodiceFiscaleMittente(mittenteCodiceFiscale);
                recordZ.setCodiceFiscalePa(pa.getCreditorInstitutionCode());
                recordZ.setDataInvioFlussoMarcheDigitali(dataInvioFlusso);
                recordZ.setProgressivoInvioFlussoMarcheDigitali(progressivo.longValue());
                recordZ.setNumeroRecordDiTipoV((long) recordsV.size());

                String contenutoFile = recordA.toLine() + "\n" + recordM.toLine() + "\n" + recordsV.stream().map(RecordV::toLine) + "\n" + recordZ.toLine();
                String dayOfYear = String.valueOf(now.getDayOfYear());
                String paddedDayOfYear = "0" + (DAY_OF_YEAR_LEN - dayOfYear.length()) + dayOfYear;

                String fileName = codiceTrasmissivo + "AT" + CODICE_FLUSSO_NORMALE + "." + pa.getCreditorInstitutionCode() + ".D" + now.getYear() + paddedDayOfYear + "T" + now.format(formatterHours);
                log.debug("PA:{} - Nome file:{}", pa.getCreditorInstitutionCode(), fileName);
                log.debug("PA:{} - Creazione BinaryFile:{}", pa.getCreditorInstitutionCode(), fileName);
                byte[] contenutoFileBytes = contenutoFile.getBytes(StandardCharsets.UTF_8);
//            contenutoFileBytes = contenutoFile.getBytes(Constant.UTF_8)
//            _ = log.debug(s"PA:$idPa - Creazione RendicontazioneBollo  e BinaryFile - Progressivo:$progressivo")
//            bf = BinaryFile(0, contenutoFile.length, Some(contenutoFileBytes), None, None)
//            rb = RendicontazioneBollo(
//            0,
//            idPa = Some(pa.creditorInstitutionCode),
//            timestampInserimento = now,
//            timestampStartOfTheWeek = startWeek,
//            timestampEndOfTheWeek = endWeek,
//            fileName = filename,
//            progressive = progressivo,
//            rendicontazioneBolloStatus = RendicontazioneBolloStatus.STORED
//            )
//              _ = log.debug(s"PA:$idPa - Progressivo:$progressivo - Salvataggio RendicontazioneBollo e update RTVersamentiBollo")
//              (_, _) <- offlineRepository.saveAllRendicontazioneBollo(bf, rb, bolliPsp.map(_._3.id))
                writeFile(fileSystemPath + "/" + fileName, contenutoFileBytes);
            });
        }

        //check filesystem
//      checkFileSystem(request.key.isEmpty)
    }

    @Async
    public void recovery(LocalDate from, LocalDate to) {
        log.info("recovery rendicontazioni MBD dal {} al {}", from, to);
        LocalDate date = from;
        do {
            execute(date);
            date = date.plusDays(1);
        } while( date.isBefore(to) );

    }

    private void writeFile(String filename, byte[] s) {
        try {
            File file = new File(filename);
            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(file));
            bw.write(s);
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
