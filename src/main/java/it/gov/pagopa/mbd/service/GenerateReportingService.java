package it.gov.pagopa.mbd.service;

import static it.gov.pagopa.mbd.util.CsvUtils.writeFile;

import it.gov.agenziaentrate._2014.marcadabollo.TipoMarcaDaBollo;
import it.gov.pagopa.gen.mbd.client.cache.model.CreditorInstitutionDto;
import it.gov.pagopa.mbd.exception.MBDReportingException;
import it.gov.pagopa.mbd.repository.BizEventRepository;
import it.gov.pagopa.mbd.repository.model.BizEventEntity;
import it.gov.pagopa.mbd.repository.model.Transfer;
import it.gov.pagopa.mbd.service.model.csv.RecordA;
import it.gov.pagopa.mbd.service.model.csv.RecordM;
import it.gov.pagopa.mbd.service.model.csv.RecordV;
import it.gov.pagopa.mbd.service.model.csv.RecordZ;
import it.gov.pagopa.mbd.util.CacheInstitutionData;
import it.gov.pagopa.mbd.util.CommonUtility;
import it.gov.pagopa.mbd.util.JaxbElementUtil;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "cache")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mbd.rendicontazioni.generate", name = "enabled")
public class GenerateReportingService {

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

    try {
      // c.timestamp in Cosmos DB biz event is a UNIX timestamp in
      // milliseconds
      // long dateFrom =
      // date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
      // long dateTo =
      // date.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC).toEpochMilli();

      // c._ts in Cosmos DB biz event is a UNIX timestamp in seconds
      long dateFrom = date.atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond();
      long dateTo = date.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC).getEpochSecond();

      String rendicontazioneBolloDateFormat =
          CommonUtility.getConfigKeyValueCache(
              configCacheService.getConfigData().getConfigurations(),
              "rendicontazioni-bollo.dateFormat");
      String mittenteCodiceFiscale =
          CommonUtility.getConfigKeyValueCache(
              configCacheService.getConfigData().getConfigurations(),
              "rendicontazioni-bollo.mittente.codiceFiscale");
      String intermediarioDenominazione =
          CommonUtility.getConfigKeyValueCache(
              configCacheService.getConfigData().getConfigurations(),
              "rendicontazioni-bollo.intermediario.denominazione");

      String intermediarioComune =
          CommonUtility.getConfigKeyValueCache(
              configCacheService.getConfigData().getConfigurations(),
              "rendicontazioni-bollo.intermediario.comuneDomicilioFiscale");
      String intermediarioSiglaProvincia =
          CommonUtility.getConfigKeyValueCache(
              configCacheService.getConfigData().getConfigurations(),
              "rendicontazioni-bollo.intermediario.siglaDellaProvinciaDelDomicilioFiscale");
      String intermediarioCap =
          CommonUtility.getConfigKeyValueCache(
              configCacheService.getConfigData().getConfigurations(),
              "rendicontazioni-bollo.intermediario.CAPDelDomicilioFiscale");
      String intermediarioIndirizzo =
          CommonUtility.getConfigKeyValueCache(
              configCacheService.getConfigData().getConfigurations(),
              "rendicontazioni-bollo.intermediario.indirizzoFrazioneViaENumeroCivicoDelDomicilioFiscale");
      String codiceTrasmissivo =
          CommonUtility.getConfigKeyValueCache(
              configCacheService.getConfigData().getConfigurations(),
              "rendicontazioni-bollo.codiceIdentificativo.1");

      CacheInstitutionData cacheInstitutionData =
          CacheInstitutionData.builder()
              .mittenteCodiceFiscale(mittenteCodiceFiscale)
              .intermediarioDenominazione(intermediarioDenominazione)
              .intermediarioComune(intermediarioComune)
              .intermediarioSiglaProvincia(intermediarioSiglaProvincia)
              .intermediarioCap(intermediarioCap)
              .intermediarioIndirizzo(intermediarioIndirizzo)
              .codiceTrasmissivo(codiceTrasmissivo)
              .build();

      DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(rendicontazioneBolloDateFormat);
      Map<String, CreditorInstitutionDto> organizations =
          configCacheService.getConfigData().getCreditorInstitutions();

      /*
       * List<CreditorInstitutionDto> ecs = (organizationsRequest == null ||
       * organizationsRequest.length == 0) ?
       * organizations.values().stream().toList() :
       * organizations.values().stream() .filter( o ->
       * Arrays.asList(organizationsRequest)
       * .contains(o.getCreditorInstitutionCode())) .toList();
       */

      // filter and sort the PAs based on idDominio in ascending order
      List<CreditorInstitutionDto> ecs =
          organizations.values().stream()
              .filter(
                  pa ->
                      organizationsRequest == null
                          || organizationsRequest.length == 0
                          || Arrays.asList(organizationsRequest)
                              .contains(pa.getCreditorInstitutionCode()))
              .sorted(
                  Comparator.comparing(CreditorInstitutionDto::getCreditorInstitutionCode)) // Sort
              .toList();

      // progressivo starts from 1 and increases for each PA
      long progressivo = 1L;
      for (CreditorInstitutionDto pa : ecs) {
        processData(dateFrom, dateTo, dateFormat, pa, cacheInstitutionData, progressivo);
      }
    } catch (Exception e) {
      log.error("Error generating MBD reporting: {}", e.getMessage(), e);
    }
  }

  private void processData(
      long dateFrom,
      long dateTo,
      DateTimeFormatter dateFormat,
      CreditorInstitutionDto pa,
      CacheInstitutionData cacheInstitutionData,
      long progressivo)
      throws MBDReportingException {
    try {
      List<BizEventEntity> bizEvents =
          bizEventRepository.getBizEventsByDateFromAndDateToAndEC(
              dateFrom, dateTo, pa.getCreditorInstitutionCode());

      if (!bizEvents.isEmpty()) {

        LocalDateTime now = LocalDateTime.now();
        String dataInvioFlusso = now.format(dateFormat);

        RecordA recordA =
            RecordA.builder()
                .codiceFiscaleMittente(cacheInstitutionData.getMittenteCodiceFiscale())
                .codiceFiscalePa(pa.getCreditorInstitutionCode())
                .dataInvioFlussoMarcheDaBollo(dataInvioFlusso)
                .progressivoInvioFlussoMarcheDigitali(progressivo)
                .build();

        RecordM recordM = new RecordM();
        recordM.setCodiceFiscaleMittente(cacheInstitutionData.getMittenteCodiceFiscale());
        recordM.setCodiceFiscalePa(pa.getCreditorInstitutionCode());
        recordM.setDataInvioFlussoMarcheDigitali(dataInvioFlusso);
        recordM.setProgressivoInvioFlussoMarcheDigitali(progressivo);
        recordM.setDenominazionePa(pa.getBusinessName());
        if (pa.getAddress() != null) {
          recordM.setComuneDomicilioFiscalePa(pa.getAddress().getCity());
          recordM.setSiglaDellaProvinciaDelDomicilioFiscalePa(pa.getAddress().getLocation());
          recordM.setCapDelDomicilioFiscalePa(pa.getAddress().getZipCode());
          recordM.setIndirizzoFrazioneViaENumeroCivicoDelDomicilioFiscalePa(
              pa.getAddress().getTaxDomicile());
        }
        recordM.setDemoninazioneIntermediario(cacheInstitutionData.getIntermediarioDenominazione());
        recordM.setComuneDomicilioFiscaleIntermediario(
            cacheInstitutionData.getIntermediarioComune());
        recordM.setSiglaDellaProvinciaDelDomicilioFiscaleIntermediario(
            cacheInstitutionData.getIntermediarioSiglaProvincia());
        recordM.setCapDelDomicilioFiscaleIntermediario(
            Long.parseLong(cacheInstitutionData.getIntermediarioCap()));
        recordM.setIndirizzoFrazioneViaENumeroCivicoDelDomicilioFiscaleIntermediario(
            cacheInstitutionData.getIntermediarioIndirizzo());

        List<String> mbdAttachments =
            bizEvents.stream()
                .flatMap(
                    b ->
                        b.getTransferList().stream()
                            .map(Transfer::getMBDAttachment)
                            .filter(StringUtils::isNotBlank))
                .toList();

        List<RecordV> recordsV =
            mbdAttachments.stream()
                .map(
                    b -> {
                      it.gov.agenziaentrate._2014.marcadabollo.TipoMarcaDaBollo tipoMarcaDaBollo =
                          this.jaxbElementUtil.convertToBean(b, TipoMarcaDaBollo.class);

                      String digestValueBase64 =
                          Base64.getEncoder()
                              .encodeToString(
                                  tipoMarcaDaBollo.getImprontaDocumento().getDigestValue());

                      RecordV recordV = new RecordV();
                      recordV.setCodiceFiscaleMittente(
                          cacheInstitutionData.getMittenteCodiceFiscale());
                      recordV.setCodiceFiscalePa(pa.getCreditorInstitutionCode());
                      recordV.setDataInvioFlussoMarcheDigitali(dataInvioFlusso);
                      recordV.setProgressivoInvioFlussoMarcheDigitali(progressivo);
                      recordV.setImprontaDocumentoInformatico(digestValueBase64);
                      recordV.setIubd(tipoMarcaDaBollo.getIUBD());
                      recordV.setCodiceFiscalePsp(tipoMarcaDaBollo.getPSP().getCodiceFiscale());
                      recordV.setDenominazionePsp(tipoMarcaDaBollo.getPSP().getDenominazione());
                      recordV.setDataDiVendita(tipoMarcaDaBollo.getOraAcquisto().toString());
                      return recordV;
                    })
                .toList();

        RecordZ recordZ = new RecordZ();
        recordZ.setCodiceFiscaleMittente(cacheInstitutionData.getMittenteCodiceFiscale());
        recordZ.setCodiceFiscalePa(pa.getCreditorInstitutionCode());
        recordZ.setDataInvioFlussoMarcheDigitali(dataInvioFlusso);
        recordZ.setProgressivoInvioFlussoMarcheDigitali(progressivo);
        recordZ.setNumeroRecordDiTipoV((long) recordsV.size());

        StringBuilder contenutoFile = new StringBuilder();
        contenutoFile.append(recordA.toLine()).append("\n");
        contenutoFile.append(recordM.toLine()).append("\n");
        recordsV.forEach(r -> contenutoFile.append(r.toLine()).append("\n"));
        contenutoFile.append(recordZ.toLine());

        String dayOfYear = String.valueOf(now.getDayOfYear());
        String paddedDayOfYear = "0" + (DAY_OF_YEAR_LEN - dayOfYear.length()) + dayOfYear;

        String fileName =
            cacheInstitutionData.getCodiceTrasmissivo()
                + "AT"
                + CODICE_FLUSSO_NORMALE
                + ".S"
                + pa.getCreditorInstitutionCode()
                + ".D"
                + now.getYear()
                + paddedDayOfYear
                + "T"
                + now.format(formatterHours);

        byte[] contenutoFileBytes = contenutoFile.toString().getBytes(StandardCharsets.UTF_8);
        writeFile(fileSystemPath + "/" + fileName, contenutoFileBytes);
      }
    } catch (Exception e) {
      log.error(
          "Error generating MBD reporting file for PA {}: {}",
          pa.getCreditorInstitutionCode(),
          e.getMessage(),
          e);
      throw new MBDReportingException(
          "Error generating MBD reporting file for PA " + pa.getCreditorInstitutionCode(), e);
    }
  }

  @Async
  public void recovery(LocalDate from, LocalDate to, String[] organizations) {
    log.info("recovery rendicontazioni MBD dal {} al {}", from, to);
    LocalDate date = from;
    do {
      execute(date, organizations);
      date = date.plusDays(1);
    } while (date.isBefore(to));
  }
}
