package it.gov.pagopa.mbd.service;

import it.gov.agenziaentrate._2014.marcadabollo.TipoMarcaDaBollo;
import it.gov.pagopa.gen.mbd.client.cache.model.CreditorInstitutionDto;
import it.gov.pagopa.mbd.repository.BizEventRepository;
import it.gov.pagopa.mbd.repository.model.BizEventEntity;
import it.gov.pagopa.mbd.repository.model.Transfer;
import it.gov.pagopa.mbd.service.model.csv.RecordA;
import it.gov.pagopa.mbd.service.model.csv.RecordM;
import it.gov.pagopa.mbd.service.model.csv.RecordV;
import it.gov.pagopa.mbd.service.model.csv.RecordZ;
import it.gov.pagopa.mbd.util.CommonUtility;
import it.gov.pagopa.mbd.util.JaxbElementUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static it.gov.pagopa.mbd.util.CsvUtils.writeFile;

@Service
@CacheConfig(cacheNames="cache")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mbd.rendicontazioni.generate", name = "enabled")
public class GenerateReportingService {

//    private final FtpClient ftpClient;

    private static final DateTimeFormatter formatterHours = DateTimeFormatter.ofPattern("HHmmss");
    private static final String CODICE_FLUSSO_NORMALE = "A5";
    private static final int DAY_OF_YEAR_LEN = 3;

    private final ConfigCacheService configCacheService;
    private final BizEventRepository bizEventRepository;

    private final JaxbElementUtil jaxbElementUtil;

    @Value("${mbd.rendicontazioni.filePath}")
    private String fileSystemPath;

    public void execute(LocalDate date, String[] organizationsRequest) {
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
        Map<String, CreditorInstitutionDto> organizations = configCacheService.getConfigData().getCreditorInstitutions();

        List<CreditorInstitutionDto> ecs;
        if( organizationsRequest == null || organizationsRequest.length == 0 ) {
            ecs = organizations.values().stream().toList();
        } else {
            ecs = organizations.values().stream()
                    .filter(o -> Arrays.stream(organizationsRequest)
                            .anyMatch(or -> or.equals(o.getCreditorInstitutionCode())))
                    .collect(Collectors.toList());
        }


        for (CreditorInstitutionDto pa : ecs) {
            List<BizEventEntity> bizEvents = bizEventRepository.getBizEventsByDateFromAndDateToAndEC(dateFrom, dateTo, pa.getCreditorInstitutionCode());

            if (!bizEvents.isEmpty()) {
                AtomicInteger progressivo = new AtomicInteger(0);

                LocalDateTime now = LocalDateTime.now();
                String dataInvioFlusso = now.format(dateFormat);
                log.debug("PA:{} - Progressivo:{} - Creazione RecordA", pa.getCreditorInstitutionCode(), progressivo);
                RecordA recordA = RecordA
                        .builder()
                        .codiceFiscaleMittente(mittenteCodiceFiscale)
                        .codiceFiscalePa(pa.getCreditorInstitutionCode())
                        .dataInvioFlussoMarcheDaBollo(dataInvioFlusso)
                        .progressivoInvioFlussoMarcheDigitali(progressivo.longValue())
                        .build();

                log.debug("PA:{} - Progressivo:{} - Creazione RecordM", pa.getCreditorInstitutionCode(), progressivo);
                RecordM recordM = new RecordM();
                recordM.setCodiceFiscaleMittente(mittenteCodiceFiscale);
                recordM.setCodiceFiscalePa(pa.getCreditorInstitutionCode());
                recordM.setDataInvioFlussoMarcheDigitali(dataInvioFlusso);
                recordM.setProgressivoInvioFlussoMarcheDigitali(progressivo.longValue());
                recordM.setDenominazionePa(pa.getBusinessName());
                if (pa.getAddress() != null) {
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

                log.debug("PA:{} - Progressivo:{} - Creazione Lista RecordV", pa.getCreditorInstitutionCode(), progressivo);

                List<String> mbdAttachments = bizEvents.stream().flatMap(b -> b.getTransferList().stream().map(Transfer::getMBDAttachment).filter(StringUtils::isNotBlank)).toList();

                List<RecordV> recordsV = mbdAttachments.stream().map(b -> {
                    it.gov.agenziaentrate._2014.marcadabollo.TipoMarcaDaBollo tipoMarcaDaBollo = this.jaxbElementUtil.convertToBean(b, TipoMarcaDaBollo.class);

                    RecordV recordV = new RecordV();
                    recordV.setCodiceFiscaleMittente(mittenteCodiceFiscale);
                    recordV.setCodiceFiscalePa(pa.getCreditorInstitutionCode());
                    recordV.setDataInvioFlussoMarcheDigitali(dataInvioFlusso);
                    recordV.setProgressivoInvioFlussoMarcheDigitali(progressivo.longValue());
                    recordV.setImprontaDocumentoInformatico(new String(tipoMarcaDaBollo.getImprontaDocumento().getDigestValue(), StandardCharsets.UTF_8));
                    recordV.setIUBD(tipoMarcaDaBollo.getIUBD());
                    recordV.setCodiceFiscalePsp(tipoMarcaDaBollo.getPSP().getCodiceFiscale());
                    recordV.setDenominazionePsp(tipoMarcaDaBollo.getPSP().getDenominazione());
                    recordV.setDataDiVendita(tipoMarcaDaBollo.getOraAcquisto().toString());
                    return recordV;
                }).toList();

                log.debug("PA:{} - Progressivo:{} - Creazione RecordZ", pa.getCreditorInstitutionCode(), progressivo);
                RecordZ recordZ = new RecordZ();
                recordZ.setCodiceFiscaleMittente(mittenteCodiceFiscale);
                recordZ.setCodiceFiscalePa(pa.getCreditorInstitutionCode());
                recordZ.setDataInvioFlussoMarcheDigitali(dataInvioFlusso);
                recordZ.setProgressivoInvioFlussoMarcheDigitali(progressivo.longValue());
                recordZ.setNumeroRecordDiTipoV((long) recordsV.size());

                String contenutoFile = recordA.toLine() + "\n" + recordM.toLine() + "\n" + recordsV.stream().map(RecordV::toLine).collect(Collectors.joining("\n")) + "\n" + recordZ.toLine();

                String a = recordA.toLine();
                String m = recordM.toLine();
                String v = recordsV.stream().map(RecordV::toLine).collect(Collectors.joining("\n"));
                String z = recordZ.toLine();

                String dayOfYear = String.valueOf(now.getDayOfYear());
                String paddedDayOfYear = "0" + (DAY_OF_YEAR_LEN - dayOfYear.length()) + dayOfYear;

                String fileName = codiceTrasmissivo + "AT" + CODICE_FLUSSO_NORMALE + "." + pa.getCreditorInstitutionCode() + ".D" + now.getYear() + paddedDayOfYear + "T" + now.format(formatterHours);

                log.debug("PA:{} - Nome file:{}", pa.getCreditorInstitutionCode(), fileName);
                log.debug("PA:{} - Creazione BinaryFile:{}", pa.getCreditorInstitutionCode(), fileName);
                byte[] contenutoFileBytes = contenutoFile.getBytes(StandardCharsets.UTF_8);

                log.debug("PA:{} - Progressivo:{} - Scrittura file:{}", pa.getCreditorInstitutionCode(), progressivo, fileName);
                writeFile(fileSystemPath + "/" + fileName, contenutoFileBytes);
            }
        }
    }

    @Async
    public void recovery(LocalDate from, LocalDate to, String[] organizations) {
        log.info("recovery rendicontazioni MBD dal {} al {}", from, to);
        LocalDate date = from;
        do {
            execute(date, organizations);
            date = date.plusDays(1);
        } while( date.isBefore(to) );

    }

}
