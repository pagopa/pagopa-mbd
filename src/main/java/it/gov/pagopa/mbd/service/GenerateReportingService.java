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
import java.nio.file.Paths;
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
import java.util.concurrent.atomic.AtomicLong;

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
	
	@Value("${mbd.rendicontazioni.maxVRecords}")
	private int maxVRecordPerFile; // Maximum record limit per file
	
	// progressivo starts from 1 for each PA and increments both when processing a new PA and when the number of records in a file exceeds the allowed limit, 
	// requiring the creation of an additional file for the same PA.
	private long progressivo = 1L;
	
	public void execute(LocalDate date, String[] organizationsRequest) {
		log.info("Start MBD reporting generation {}", date);
		
		// at each execution the progressive is initialized to one
		progressivo = 1L;

		try {
			// c.timestamp in Cosmos DB biz event is a UNIX timestamp in milliseconds
			// long dateFrom = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
			// long dateTo = date.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC).toEpochMilli();

			// c._ts in Cosmos DB biz event is a UNIX timestamp in seconds
			long dateFrom = date.atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond();
			long dateTo = date.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC).getEpochSecond();

			CacheInstitutionData cacheInstitutionData = loadInstitutionData();

			DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(
					CommonUtility.getConfigKeyValueCache(
							configCacheService.getConfigData().getConfigurations(),
							"rendicontazioni-bollo.dateFormat"
							)
					);

			Map<String, CreditorInstitutionDto> organizations = configCacheService.getConfigData().getCreditorInstitutions();

			// filter and sort the PAs based on idDominio in ascending order
			List<CreditorInstitutionDto> ecs = organizations.values().stream()
					.filter(pa -> organizationsRequest == null || organizationsRequest.length == 0 ||
					Arrays.asList(organizationsRequest).contains(pa.getCreditorInstitutionCode()))
					.sorted(Comparator.comparing(CreditorInstitutionDto::getCreditorInstitutionCode))
					.toList();
			
			for (CreditorInstitutionDto pa : ecs) {
				processData(dateFrom, dateTo, dateFormat, pa, cacheInstitutionData, progressivo);
			}

		} catch (Exception e) {
			log.error("General error while generating MBD reports: {}", e.getMessage(), e);
		}
	}

	private void processData(
			long dateFrom,
			long dateTo,
			DateTimeFormatter dateFormat,
			CreditorInstitutionDto pa,
			CacheInstitutionData cacheInstitutionData,
			long progressivo
			) throws MBDReportingException {
		try {
			List<BizEventEntity> bizEvents = bizEventRepository.getBizEventsByDateFromAndDateToAndEC(
					dateFrom, dateTo, pa.getCreditorInstitutionCode());

			if (!bizEvents.isEmpty()) {
				LocalDateTime now = LocalDateTime.now();
				String dataInvioFlusso = now.format(dateFormat);

				List<String> mbdAttachments = bizEvents.stream()
						.flatMap(b -> b.getTransferList().stream()
								.map(Transfer::getMBDAttachment)
								.filter(StringUtils::isNotBlank))
						.toList();

				List<RecordV> allRecordsV = createRecordVList(mbdAttachments, cacheInstitutionData, pa, dataInvioFlusso, progressivo);

				//Split into files with a maximum of records
				int totalChunks = (int) Math.ceil((double) allRecordsV.size() / maxVRecordPerFile);
				
				AtomicLong vRecordChunkProgressivo = new AtomicLong(progressivo);
				
				for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
					int start = chunkIndex * maxVRecordPerFile;
					int end = Math.min(start + maxVRecordPerFile, allRecordsV.size());
					
					long localProgressivo =  vRecordChunkProgressivo.getAndIncrement();
					
					// recordsV are updated with the new progressive
					List<RecordV> recordsVChunk = allRecordsV.subList(start, end);
					recordsVChunk.forEach(recV -> recV.setProgressivoInvioFlussoMarcheDigitali(localProgressivo));

					// File creation for each chunk
					writeReportFile(pa, cacheInstitutionData, now, localProgressivo, dataInvioFlusso, recordsVChunk);
				}
				
				// Update global progressivo
				this.progressivo = vRecordChunkProgressivo.get();
				
			} else {
				log.info("No events found for PA {} in the date range {} to {} ", pa.getCreditorInstitutionCode(), dateFrom, dateTo);
			}

		} catch (Exception e) {
			log.error("Error generating file for PA {}: {}", pa.getCreditorInstitutionCode(), e.getMessage(), e);
			throw new MBDReportingException("Error generating file for PA " + pa.getCreditorInstitutionCode(), e);
		}
	}

	private CacheInstitutionData loadInstitutionData() {
		return CacheInstitutionData.builder()
				.mittenteCodiceFiscale(CommonUtility.getConfigKeyValueCache(
						configCacheService.getConfigData().getConfigurations(),
						"rendicontazioni-bollo.mittente.codiceFiscale"))
				.intermediarioDenominazione(CommonUtility.getConfigKeyValueCache(
						configCacheService.getConfigData().getConfigurations(),
						"rendicontazioni-bollo.intermediario.denominazione"))
				.intermediarioComune(CommonUtility.getConfigKeyValueCache(
						configCacheService.getConfigData().getConfigurations(),
						"rendicontazioni-bollo.intermediario.comuneDomicilioFiscale"))
				.intermediarioSiglaProvincia(CommonUtility.getConfigKeyValueCache(
						configCacheService.getConfigData().getConfigurations(),
						"rendicontazioni-bollo.intermediario.siglaDellaProvinciaDelDomicilioFiscale"))
				.intermediarioCap(CommonUtility.getConfigKeyValueCache(
						configCacheService.getConfigData().getConfigurations(),
						"rendicontazioni-bollo.intermediario.CAPDelDomicilioFiscale"))
				.intermediarioIndirizzo(CommonUtility.getConfigKeyValueCache(
						configCacheService.getConfigData().getConfigurations(),
						"rendicontazioni-bollo.intermediario.indirizzoFrazioneViaENumeroCivicoDelDomicilioFiscale"))
				.codiceTrasmissivo(CommonUtility.getConfigKeyValueCache(
						configCacheService.getConfigData().getConfigurations(),
						"rendicontazioni-bollo.codiceIdentificativo.1"))
				.build();
	}

	private List<RecordV> createRecordVList(List<String> mbdAttachments, CacheInstitutionData cacheInstitutionData, CreditorInstitutionDto pa, String dataInvioFlusso, long progressivo) {
		return mbdAttachments.stream()
				.map(mbdAttachment -> {
					try {
						TipoMarcaDaBollo tipoMarcaDaBollo = jaxbElementUtil.convertToBean(mbdAttachment, TipoMarcaDaBollo.class);

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
					} catch (Exception e) {
						log.error("Error in JAXB conversion: {}", e.getMessage(), e);
						return null;
					}
				})
				.filter(r -> r != null)
				.toList();
	}

	private void writeReportFile(CreditorInstitutionDto pa, CacheInstitutionData cacheInstitutionData, LocalDateTime now, long progressivo, String dataInvioFlusso, List<RecordV> recordsV) {

		RecordA recordA = createRecordA(cacheInstitutionData, pa, dataInvioFlusso, progressivo);
		RecordM recordM = createRecordM(cacheInstitutionData, pa, dataInvioFlusso, progressivo);
		RecordZ recordZ = createRecordZ(cacheInstitutionData, pa, dataInvioFlusso, progressivo, recordsV.size());

		String dayOfYear = String.valueOf(now.getDayOfYear());
		String paddedDayOfYear = "0" + (DAY_OF_YEAR_LEN - dayOfYear.length()) + dayOfYear;
		String fileName = cacheInstitutionData.getCodiceTrasmissivo() + "AT" + CODICE_FLUSSO_NORMALE +
				".S" + pa.getCreditorInstitutionCode() +
				".D" + now.getYear() + paddedDayOfYear +
				"T" + now.format(formatterHours);

		String filePath = Paths.get(fileSystemPath, fileName).toString();

		StringBuilder fileContent = new StringBuilder();
		fileContent.append(recordA.toLine()).append("\n")
		.append(recordM.toLine()).append("\n");
		recordsV.forEach(recV -> fileContent.append(recV.toLine()).append("\n"));
		fileContent.append(recordZ.toLine());

		writeFile(filePath, fileContent.toString().getBytes(StandardCharsets.UTF_8));
		log.info("File written successfully: {}", filePath);
	}

	private RecordA createRecordA(CacheInstitutionData cacheInstitutionData, CreditorInstitutionDto pa, String dataInvioFlusso, long progressivo) {
		return RecordA.builder()
				.codiceFiscaleMittente(cacheInstitutionData.getMittenteCodiceFiscale())
				.codiceFiscalePa(pa.getCreditorInstitutionCode())
				.dataInvioFlussoMarcheDaBollo(dataInvioFlusso)
				.progressivoInvioFlussoMarcheDigitali(progressivo)
				.build();
	}

	private RecordM createRecordM(CacheInstitutionData cacheInstitutionData, CreditorInstitutionDto pa, String dataInvioFlusso, long progressivo) {
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

		return recordM;
	}

	private RecordZ createRecordZ(CacheInstitutionData cacheInstitutionData, CreditorInstitutionDto pa, String dataInvioFlusso, long progressivo, int totalRecordsV) { 	
		RecordZ recordZ = new RecordZ();
		recordZ.setCodiceFiscaleMittente(cacheInstitutionData.getMittenteCodiceFiscale());
		recordZ.setCodiceFiscalePa(pa.getCreditorInstitutionCode());
		recordZ.setDataInvioFlussoMarcheDigitali(dataInvioFlusso);
		recordZ.setProgressivoInvioFlussoMarcheDigitali(progressivo);
		recordZ.setNumeroRecordDiTipoV((long) totalRecordsV);

		return recordZ;
	}

	@Async
	public void recovery(LocalDate from, LocalDate to, String[] organizations) {
		log.info("MBD reporting recovery from {} to {}", from, to);
		LocalDate date = from;
		do {
			execute(date, organizations);
			date = date.plusDays(1);
		} while (date.isBefore(to));
	}
}

