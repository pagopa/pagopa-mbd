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
        
        Map<String, it.gov.pagopa.gen.mbd.client.cache.model.CreditorInstitutionDto> orgs = getOrganizations();
        orgs.putIfAbsent("66666666666", dummyCi("66666666666", "Dummy PA 66666666666"));
        orgs.putIfAbsent("0123456789", dummyCi("0123456789", "Dummy PA 0123456789"));
        
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
	
	private static it.gov.pagopa.gen.mbd.client.cache.model.CreditorInstitutionDto dummyCi(String cf, String name) {
	    var ci = new it.gov.pagopa.gen.mbd.client.cache.model.CreditorInstitutionDto();
	    ci.setCreditorInstitutionCode(cf);
	    ci.setBusinessName(name);
	    ci.setDescription("test");
	    ci.setEnabled(true);
	    ci.setPspPayment(true);
	    ci.setReportingFtp(true);
	    ci.setReportingZip(true);

	    var addr = new it.gov.pagopa.gen.mbd.client.cache.model.CreditorInstitutionAddressDto();
	    addr.setCity("city");
	    addr.setZipCode("00000");
	    addr.setCountryCode("IT");
	    addr.setLocation("RM");
	    addr.setTaxDomicile("via test 1");
	    ci.setAddress(addr);

	    return ci;
	}

}
