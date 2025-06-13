package it.gov.pagopa.mbd.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.util.IOUtils;
import it.gov.pagopa.gen.mbd.client.cache.model.CreditorInstitutionAddressDto;
import it.gov.pagopa.gen.mbd.client.cache.model.CreditorInstitutionDto;
import it.gov.pagopa.mbd.repository.model.BizEventEntity;
import it.gov.pagopa.mbd.util.CacheInstitutionData;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TestUtils {

    public static String loadFileContent(String fileName) {
        String content = null;
        InputStream inputStream = TestUtils.class.getResourceAsStream(fileName);
        if (inputStream != null) {
            content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } else {
            System.err.println("File not found: " + fileName);
        }
        return content;
    }

    public static it.gov.pagopa.gen.mbd.client.cache.model.ConfigDataV1Dto configData() throws JsonProcessingException {
        it.gov.pagopa.gen.mbd.client.cache.model.ConfigDataV1Dto configDataV1 = new it.gov.pagopa.gen.mbd.client.cache.model.ConfigDataV1Dto();
        configDataV1.setStations(new HashMap<>());

        configDataV1.setConfigurations(getConfigurations());
        configDataV1.setCreditorInstitutions(getOrganizations());

        return configDataV1;
    }

    public static void setMock(it.gov.pagopa.gen.mbd.client.cache.invoker.ApiClient client,ResponseEntity response){
        when(client.invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(),any(),any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(Arrays.asList());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }

    public static byte[] zip(byte[] uncompressed) throws IOException {
        ByteArrayOutputStream bais = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bais);
        gzipOutputStream.write(uncompressed);
        gzipOutputStream.close();
        bais.close();
        return bais.toByteArray();
    }

    public static String zipAndEncode(String p){
        try {
            return new String(Base64.getEncoder().encode(zip(p.getBytes(StandardCharsets.UTF_8))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<BizEventEntity> getBizEvent() throws JsonProcessingException {
        return List.of(new ObjectMapper().readValue(TestUtils.loadFileContent("/mock/bizevents.json"), BizEventEntity.class));
    }

    public static  Map<String, it.gov.pagopa.gen.mbd.client.cache.model.CreditorInstitutionDto> getOrganizations() throws JsonProcessingException {
        TypeReference<HashMap<String,it.gov.pagopa.gen.mbd.client.cache.model.CreditorInstitutionDto>> typeRef
                = new TypeReference<>() {
        };
        return new ObjectMapper().readValue(TestUtils.loadFileContent("/mock/organizations.json"), typeRef);
    }

    public static Map<String, it.gov.pagopa.gen.mbd.client.cache.model.ConfigurationKeyDto> getConfigurations() throws JsonProcessingException {
        TypeReference<HashMap<String,it.gov.pagopa.gen.mbd.client.cache.model.ConfigurationKeyDto>> typeRef
                = new TypeReference<>() {
        };
        return new ObjectMapper().readValue(TestUtils.loadFileContent("/mock/configurations.json"), typeRef);
    }

	public static CreditorInstitutionDto getCreditorInstitutionDto() {
		CreditorInstitutionDto creditorInstitutionDto = new CreditorInstitutionDto();
		creditorInstitutionDto.setBusinessName("businessName");
		creditorInstitutionDto.setCreditorInstitutionCode("0123456789");
		creditorInstitutionDto.setDescription("description");
		creditorInstitutionDto.setEnabled(true);
		creditorInstitutionDto.setPspPayment(true);
		creditorInstitutionDto.setReportingFtp(true);
		creditorInstitutionDto.setReportingZip(true);
		CreditorInstitutionAddressDto creditorInstitutionAddressDto = new CreditorInstitutionAddressDto();
		creditorInstitutionAddressDto.setCity("city");
		creditorInstitutionAddressDto.setZipCode("postalCode");
		creditorInstitutionAddressDto.setCountryCode("countryCode");
		creditorInstitutionAddressDto.setLocation("location");	
		creditorInstitutionAddressDto.setTaxDomicile("taxDomicile");
		creditorInstitutionDto.setAddress(creditorInstitutionAddressDto);
		
		return creditorInstitutionDto;
	}

	public static CacheInstitutionData getCacheInstitutionData() {
		return CacheInstitutionData.builder()
				.mittenteCodiceFiscale("0123456789")
				.intermediarioDenominazione("Intermediario Denominazione")
				.intermediarioComune("Intermediario Comune")
				.intermediarioSiglaProvincia("IP")
				.intermediarioCap("12345")
				.intermediarioIndirizzo("Intermediario Indirizzo")
				.codiceTrasmissivo("codiceTrasmissivo")
				.build();
	}

}
