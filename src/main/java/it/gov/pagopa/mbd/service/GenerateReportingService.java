package it.gov.pagopa.mbd.service;

import static it.gov.pagopa.mbd.util.CsvUtils.writeFile;

import it.gov.agenziaentrate._2014.marcadabollo.TipoMarcaDaBollo;
import it.gov.pagopa.gen.mbd.client.cache.model.CreditorInstitutionDto;
import it.gov.pagopa.mbd.config.RetryConfig;
import it.gov.pagopa.mbd.config.RetryExecutor;
import it.gov.pagopa.mbd.exception.MBDReportingException;
import it.gov.pagopa.mbd.exception.MBDRetryException;
import it.gov.pagopa.mbd.repository.BizEventRepository;
import it.gov.pagopa.mbd.repository.model.BizEventEntity;
import it.gov.pagopa.mbd.repository.model.PaMbdCount;
import it.gov.pagopa.mbd.repository.model.Transfer;
import it.gov.pagopa.mbd.service.model.MarcaDaBolloRaw;
import it.gov.pagopa.mbd.service.model.csv.RecordA;
import it.gov.pagopa.mbd.service.model.csv.RecordM;
import it.gov.pagopa.mbd.service.model.csv.RecordV;
import it.gov.pagopa.mbd.service.model.csv.RecordZ;
import it.gov.pagopa.mbd.util.CacheInstitutionData;
import it.gov.pagopa.mbd.util.CommonUtility;
import it.gov.pagopa.mbd.util.JaxbElementUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

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
  
  private final RetryExecutor retryExecutor;
  private final RetryConfig retryConfig;

  @Value("${mbd.rendicontazioni.filePath}")
  private String fileSystemPath;

  @Value("${mbd.rendicontazioni.maxVRecords}")
  private int maxVRecordPerFile; // Maximum record limit per file (row size)

  @Value("${mbd.rendicontazioni.maxStampsForVRecord}")
  private int maxStampsForVRecord; // Maximum number of digital tax stamps per RecordV (column size)

  // progressivo starts from 1 for each PA and increments both when processing a new PA and when the
  // number of records in a file exceeds the allowed limit,
  // requiring the creation of an additional file for the same PA.
  private long progressivo = 1L;

  public void execute(LocalDate date, String[] organizationsRequest) throws MBDReportingException {
    UUID executionId = UUID.randomUUID();
    log.info("[{}] Start MBD reporting generation for date {}", executionId, date);
    
    File dir = new File(fileSystemPath);
    if (!dir.exists()) {
      log.warn("Mount path {} does not exist. It may indicate the Azure File Share is not mounted.", fileSystemPath);
      throw new MBDReportingException(
		  "Mount path " + fileSystemPath + " does not exist. Please check the Azure File Share configuration.");
    }

    // at each execution the progressive is initialized to one
    progressivo = 1L;

    try {
      // c.timestamp in Cosmos DB biz event is a UNIX timestamp in milliseconds
      long dateFrom = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
      long dateTo = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

      // c._ts in Cosmos DB biz event is a UNIX UTC timestamp in seconds
      //long dateFrom = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
      //long dateTo = date.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

      CacheInstitutionData cacheInstitutionData = loadInstitutionData();

      DateTimeFormatter dateFormat =
          DateTimeFormatter.ofPattern(
              CommonUtility.getConfigKeyValueCache(
                  configCacheService.getConfigData().getConfigurations(),
                  "rendicontazioni-bollo.dateFormat"));

      // 1. Get PA from cache
      Map<String, CreditorInstitutionDto> organizations =
          configCacheService.getConfigData().getCreditorInstitutions();
      int totalPaFromCache = organizations.size();
      
      // 2. Get PA with at least one MBD from the repository
      List<PaMbdCount> paWithMbdList = bizEventRepository.getPaWithMbdAndCount(dateFrom, dateTo);
      int totalPaWithMbd = paWithMbdList.size();
      
      // summary logs
      log.info("[{}] Total PAs retrieved from cache: {}", executionId, totalPaFromCache);
      log.info("[{}] Total PAs with at least one associated mbd: {} (range from {} to {})", executionId, totalPaWithMbd, dateFrom, dateTo);
      log.debug("[{}] List of PAs with at least one associated mbd: {}", executionId, new ObjectMapper().writeValueAsString(paWithMbdList));
   
      // 3. Create the list of PAs actually to be processed
      List<String> paIdWithMbd = paWithMbdList.stream()
              .map(PaMbdCount::getFiscalCodePA)
              .toList();

      // 4. Apply organizationRequest filter if it exists and create sorted list only for PAs with MBD
      List<CreditorInstitutionDto> ecs = organizations.values().stream()
              .filter(pa ->
                      paIdWithMbd.contains(pa.getCreditorInstitutionCode()) &&
                      (organizationsRequest == null
                          || organizationsRequest.length == 0
                          || Arrays.asList(organizationsRequest).contains(pa.getCreditorInstitutionCode()))
              )
              .sorted(Comparator.comparing(CreditorInstitutionDto::getCreditorInstitutionCode))
              .toList();

      // 5. Process only PAs actually with MBD
      for (CreditorInstitutionDto pa : ecs) {
        processData(executionId, dateFrom, dateTo, dateFormat, pa, cacheInstitutionData, progressivo);
      }

    } catch (Exception e) {
        log.error("[{}] General error while generating MBD reports: {}", executionId, e.getMessage(), e);
    }
  }

  private void processData(
      UUID executionId,
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
        
        log.info("[{}] Total biz-events with at least one associated mbd: {} (range from {} to {})", executionId, bizEvents.size(), dateFrom, dateTo);
        
        Map<String, String> uniqueMbdAttachments = new HashMap<>();

        for (BizEventEntity event : bizEvents) {
            String eventId = event.getId();
            for (Transfer t : event.getTransferList()) {
                if (pa.getCreditorInstitutionCode().equals(t.getFiscalCodePA())
                        && StringUtils.isNotBlank(t.getMBDAttachment())) {
                    String uniqueKey = eventId + "|" + t.getIdTransfer();
                    // Put MBD only if it has not already been mapped
                    uniqueMbdAttachments.putIfAbsent(uniqueKey, t.getMBDAttachment());
                }
            }
        }

        List<String> mbdAttachments = new ArrayList<>(uniqueMbdAttachments.values());
        
        List<RecordV> allRecordsV =
            createRecordVList(
                mbdAttachments, cacheInstitutionData, pa, dataInvioFlusso, progressivo);

        // Split into files with a maximum of records
        int totalChunks = (int) Math.ceil((double) allRecordsV.size() / maxVRecordPerFile);

        AtomicLong vRecordChunkProgressivo = new AtomicLong(progressivo);

        for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
          int start = chunkIndex * maxVRecordPerFile;
          int end = Math.min(start + maxVRecordPerFile, allRecordsV.size());

          long localProgressivo = vRecordChunkProgressivo.getAndIncrement();

          // recordsV are updated with the new progressive
          List<RecordV> recordsVChunk = allRecordsV.subList(start, end);
          recordsVChunk.forEach(
              recV -> recV.setProgressivoInvioFlussoMarcheDigitali(localProgressivo));

          // File creation for each chunk
          writeReportFile(
                  executionId, pa, cacheInstitutionData, now, localProgressivo, dataInvioFlusso, recordsVChunk, dateFrom, dateTo);
        }

        // Update global progressivo
        this.progressivo = vRecordChunkProgressivo.get();

      } else {
          log.info("[{}] No events found for PA {} in range {} to {}", executionId, pa.getCreditorInstitutionCode(), dateFrom, dateTo);
      }

    } catch (Exception e) {
        log.error("[{}] Error generating file for PA {}: {}", executionId, pa.getCreditorInstitutionCode(), e.getMessage(), e);
        throw new MBDReportingException(
          "Error generating file for PA " + pa.getCreditorInstitutionCode(), e);
    }
  }

  private CacheInstitutionData loadInstitutionData() {
    return CacheInstitutionData.builder()
        .mittenteCodiceFiscale(
            CommonUtility.getConfigKeyValueCache(
                configCacheService.getConfigData().getConfigurations(),
                "rendicontazioni-bollo.mittente.codiceFiscale"))
        .intermediarioDenominazione(
            CommonUtility.getConfigKeyValueCache(
                configCacheService.getConfigData().getConfigurations(),
                "rendicontazioni-bollo.intermediario.denominazione"))
        .intermediarioComune(
            CommonUtility.getConfigKeyValueCache(
                configCacheService.getConfigData().getConfigurations(),
                "rendicontazioni-bollo.intermediario.comuneDomicilioFiscale"))
        .intermediarioSiglaProvincia(
            CommonUtility.getConfigKeyValueCache(
                configCacheService.getConfigData().getConfigurations(),
                "rendicontazioni-bollo.intermediario.siglaDellaProvinciaDelDomicilioFiscale"))
        .intermediarioCap(
            CommonUtility.getConfigKeyValueCache(
                configCacheService.getConfigData().getConfigurations(),
                "rendicontazioni-bollo.intermediario.CAPDelDomicilioFiscale"))
        .intermediarioIndirizzo(
            CommonUtility.getConfigKeyValueCache(
                configCacheService.getConfigData().getConfigurations(),
                "rendicontazioni-bollo.intermediario.indirizzoFrazioneViaENumeroCivicoDelDomicilioFiscale"))
        .codiceTrasmissivo(
            CommonUtility.getConfigKeyValueCache(
                configCacheService.getConfigData().getConfigurations(),
                "rendicontazioni-bollo.codiceIdentificativo.1"))
        .build();
  }

  private List<RecordV> createRecordVList(
      List<String> mbdAttachments,
      CacheInstitutionData cacheInstitutionData,
      CreditorInstitutionDto pa,
      String dataInvioFlusso,
      long progressivo) {

    List<MarcaDaBolloRaw> rawList =
        mbdAttachments.stream()
            .map(
                mbdAttachment -> {
                  try {
                    TipoMarcaDaBollo tipoMarcaDaBollo =
                        jaxbElementUtil.convertToBean(mbdAttachment, TipoMarcaDaBollo.class);

                    return new MarcaDaBolloRaw(
                        Base64.getEncoder()
                            .encodeToString(
                                tipoMarcaDaBollo.getImprontaDocumento().getDigestValue()),
                        tipoMarcaDaBollo.getIUBD(),
                        tipoMarcaDaBollo.getPSP().getCodiceFiscale(),
                        tipoMarcaDaBollo.getPSP().getDenominazione(),
                        tipoMarcaDaBollo.getOraAcquisto().toString());
                  } catch (Exception e) {
                    log.error("Error in JAXB conversion: {}", e.getMessage(), e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .toList();

    // Group every 'maxStampsForVRecord' stamps
    List<RecordV> recordVList = new ArrayList<>();
    for (int i = 0; i < rawList.size(); i += maxStampsForVRecord) {
      List<MarcaDaBolloRaw> subList =
          rawList.subList(i, Math.min(i + maxStampsForVRecord, rawList.size()));

      RecordV recordV = new RecordV();
      recordV.setMaxStampsForVRecord(maxStampsForVRecord);
      recordV.setCodiceFiscaleMittente(cacheInstitutionData.getMittenteCodiceFiscale());
      recordV.setCodiceFiscalePa(pa.getCreditorInstitutionCode());
      recordV.setDataInvioFlussoMarcheDigitali(dataInvioFlusso);
      recordV.setProgressivoInvioFlussoMarcheDigitali(progressivo);
      recordV.setMarche(subList);

      recordVList.add(recordV);
    }

    return recordVList;
  }

  void writeReportFile(
          UUID executionId,
		  CreditorInstitutionDto pa,
		  CacheInstitutionData cacheInstitutionData,
		  LocalDateTime now,
		  long progressivo,
		  String dataInvioFlusso,
		  List<RecordV> recordsV,
		  long dateFrom,
		  long dateTo) {

	  RecordA recordA = createRecordA(cacheInstitutionData, pa, dataInvioFlusso, progressivo);
	  RecordM recordM = createRecordM(cacheInstitutionData, pa, dataInvioFlusso, progressivo);
	  RecordZ recordZ =
			  createRecordZ(cacheInstitutionData, pa, dataInvioFlusso, progressivo, recordsV.size());
	  
	  int totalMBD = recordsV.stream()
	          .mapToInt(recV -> recV.getMarche() != null ? recV.getMarche().size() : 0)
	          .sum();

	  String dayOfYear = String.valueOf(now.getDayOfYear());
	  String paddedDayOfYear = "0" + (DAY_OF_YEAR_LEN - dayOfYear.length()) + dayOfYear;
	  // Currently, no checks are in place to prevent file overwriting if multiple files are generated within the same second. 
	  // However, the current value of 'maxVRecordPerFile' and 'maxStampsForVRecord' makes this scenario extremely unlikely.
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
			  + LocalDateTime.now().format(formatterHours);

	  String filePath = Paths.get(fileSystemPath, fileName).toString();

	  StringBuilder fileContent = new StringBuilder();
	  fileContent.append(recordA.toLine()).append("\n").append(recordM.toLine()).append("\n");
	  recordsV.forEach(recV -> fileContent.append(recV.toLine()).append("\n"));
	  fileContent.append(recordZ.toLine());

	  retryExecutor.executeWithRetry(context -> {
		  writeFile(filePath, fileContent.toString().getBytes(StandardCharsets.UTF_8));
		  log.info("[{}] File written successfully for PA [{}] (attempt {}): {}. Contains {} MBD.",
		          executionId, pa.getCreditorInstitutionCode(), context.getRetryCount() + 1, filePath, totalMBD);
		  return null;
	  },
	  // Callback
	  exception -> {
		  String errorMessage = String.format(
				"[%s] File for pa %s write failed after retrying %s times for filePath [%s], dateFrom [%s], dateTo [%s]. Error: %s", 
				executionId, pa.getCreditorInstitutionCode(), retryConfig.getMaxAttempts(), filePath, dateFrom, dateTo, exception.getMessage());
		  log.error(errorMessage, exception);
		  throw new MBDRetryException(errorMessage);
    });
  }

  private RecordA createRecordA(
      CacheInstitutionData cacheInstitutionData,
      CreditorInstitutionDto pa,
      String dataInvioFlusso,
      long progressivo) {
    return RecordA.builder()
        .codiceFiscaleMittente(cacheInstitutionData.getMittenteCodiceFiscale())
        .codiceFiscalePa(pa.getCreditorInstitutionCode())
        .dataInvioFlussoMarcheDaBollo(dataInvioFlusso)
        .progressivoInvioFlussoMarcheDigitali(progressivo)
        .build();
  }

  private RecordM createRecordM(
      CacheInstitutionData cacheInstitutionData,
      CreditorInstitutionDto pa,
      String dataInvioFlusso,
      long progressivo) {
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
    recordM.setComuneDomicilioFiscaleIntermediario(cacheInstitutionData.getIntermediarioComune());
    recordM.setSiglaDellaProvinciaDelDomicilioFiscaleIntermediario(
        cacheInstitutionData.getIntermediarioSiglaProvincia());
    recordM.setCapDelDomicilioFiscaleIntermediario(
        Long.parseLong(cacheInstitutionData.getIntermediarioCap()));
    recordM.setIndirizzoFrazioneViaENumeroCivicoDelDomicilioFiscaleIntermediario(
        cacheInstitutionData.getIntermediarioIndirizzo());

    return recordM;
  }

  private RecordZ createRecordZ(
      CacheInstitutionData cacheInstitutionData,
      CreditorInstitutionDto pa,
      String dataInvioFlusso,
      long progressivo,
      int totalRecordsV) {
    RecordZ recordZ = new RecordZ();
    recordZ.setCodiceFiscaleMittente(cacheInstitutionData.getMittenteCodiceFiscale());
    recordZ.setCodiceFiscalePa(pa.getCreditorInstitutionCode());
    recordZ.setDataInvioFlussoMarcheDigitali(dataInvioFlusso);
    recordZ.setProgressivoInvioFlussoMarcheDigitali(progressivo);
    recordZ.setNumeroRecordDiTipoV((long) totalRecordsV);

    return recordZ;
  }

  @Async
  public void recovery(LocalDate from, LocalDate to, String[] organizations) throws MBDReportingException {
    log.info("MBD reporting recovery from {} to {}", from, to);
    LocalDate date = from;
    do {
      execute(date, organizations);
      date = date.plusDays(1);
    } while (date.isBefore(to));
  }
}
