package it.gov.pagopa.mbd.service;

import it.gov.pagopa.mbd.Application;
import it.gov.pagopa.mbd.exception.MBDRetryException;
import it.gov.pagopa.mbd.repository.BizEventRepository;
import it.gov.pagopa.mbd.repository.model.PaMbdCount;
import it.gov.pagopa.mbd.service.model.csv.RecordV;
import it.gov.pagopa.mbd.util.CsvUtils;
import it.gov.pagopa.mbd.utils.TestUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.gov.pagopa.mbd.utils.TestUtils.getBizEvent;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ActiveProfiles(profiles = "test")
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class GenerateReportingServiceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired private ConfigCacheService configCacheService;
    
    @Autowired private ApplicationContext applicationContext;
    
    @Autowired
    private it.gov.pagopa.mbd.config.RetryExecutor retryExecutor;

    @Autowired
    private it.gov.pagopa.mbd.config.RetryConfig retryConfig;
    
    @Autowired
    private GenerateReportingService generateReportingService;

    @MockBean
    private BizEventRepository bizEventRepository;

    @MockBean
    private it.gov.pagopa.gen.mbd.client.cache.model.ConfigDataV1Dto configDataV1Dto;

    @Test
    void generateForAllEc() throws Exception {
    	
    	org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData", TestUtils.configData());

        when(bizEventRepository.getBizEventsByDateFromAndDateToAndEC(anyLong(), anyLong(), anyString())).thenReturn(getBizEvent());
        when(bizEventRepository.getPaWithMbdAndCount(anyLong(), anyLong())).thenReturn(List.of(PaMbdCount.builder().idPA("66666666666").mbdCount(1).build()));
        
        Path tempDir = Files.createTempDirectory("mbd-test-dir");
        String path = tempDir.toAbsolutePath().toString();
        
        org.springframework.test.util.ReflectionTestUtils.setField(
                applicationContext.getBean(GenerateReportingService.class),
                "fileSystemPath",
                path
        );
        
        mvc.perform(MockMvcRequestBuilders.patch("/recover")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(CollectionUtils.toMultiValueMap(
                                Map.of("from", List.of("2024-01-01"), "to", List.of("2024-01-02"))
                        ))
                )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andDo(
                        result -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });

        verify(bizEventRepository,times(1)).getBizEventsByDateFromAndDateToAndEC(anyLong(), anyLong(), anyString());
        
        Files.walk(tempDir)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
    }

    @Test
    void generateForSpecificEc() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData", TestUtils.configData());

        when(bizEventRepository.getBizEventsByDateFromAndDateToAndEC(anyLong(), anyLong(), anyString())).thenReturn(getBizEvent());

        mvc.perform(MockMvcRequestBuilders.patch("/recover")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(CollectionUtils.toMultiValueMap(
                                Map.of("from", List.of("2024-01-01"), "to", List.of("2024-01-02"), "organizations", List.of("0123456789"))
                        ))
                )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andDo(
                        result -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });
    }
    
    @Test
    void testWriteReportFileWithRetryFailure() {
        // Mock CsvUtils to throw IOException on writeFile
        var csvUtilsMock = mockStatic(CsvUtils.class);
        csvUtilsMock.when(() -> CsvUtils.writeFile(any(), any())).thenThrow(new UncheckedIOException("Write failed!", new IOException()));

        // Prepare test data
        var pa = TestUtils.getCreditorInstitutionDto();
        var cacheInstitutionData = TestUtils.getCacheInstitutionData();
        var now = LocalDateTime.now();
        long progressivo = 1;
        String dataInvioFlusso = "20250610";
        var recordsV = Collections.<RecordV>emptyList();
        long dateFrom = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long dateTo = dateFrom;

        // Set retry configuration
        retryConfig.setMaxAttempts(2);
        retryConfig.setDelayMillis(10);
        retryConfig.setMultiplier(1.0);

        String expectedMsgPart = "failed after retrying 2 times";

        // Execute the method and expect MBDRetryException
        Exception ex = Assertions.assertThrows(
        		MBDRetryException.class,
        		() -> generateReportingService.writeReportFile(UUID.randomUUID(),
        				pa, cacheInstitutionData, now, progressivo, dataInvioFlusso, recordsV,
        				dateFrom,
        				dateTo
        				)
        		);

        Assertions.assertTrue(ex.getMessage().contains(expectedMsgPart));
        Assertions.assertTrue(ex.getMessage().contains("Write failed!"));

        csvUtilsMock.close();
    }
    
    @Test
    void testWriteReportFileSuccess() throws Exception {
        // Mock static CsvUtils.writeFile
        try (MockedStatic<CsvUtils> csvUtilsMock = Mockito.mockStatic(CsvUtils.class)) {
            // Mock writeFile to do nothing (simulate successful write)
            csvUtilsMock.when(() -> CsvUtils.writeFile(any(), any())).then(invocation -> null);

            // Prepare test data
            var pa = TestUtils.getCreditorInstitutionDto();
            var cacheInstitutionData = TestUtils.getCacheInstitutionData();
            var now = LocalDateTime.now();
            long progressivo = 1;
            String dataInvioFlusso = "20250610";
            var recordsV = Collections.<RecordV>emptyList();
            long dateFrom = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long dateTo = dateFrom;

            // Create a temporary directory for the file system path
            Path tempDir = Files.createTempDirectory("mbd-test-dir-success");
            org.springframework.test.util.ReflectionTestUtils.setField(
                generateReportingService, "fileSystemPath", tempDir.toString()
            );

            // Set retry configuration (useless in this case, but included for completeness)
            retryConfig.setMaxAttempts(2);
            retryConfig.setDelayMillis(10);
            retryConfig.setMultiplier(1.0);

            // Execute the method and verify no exception is thrown
            assertDoesNotThrow(() -> 
                generateReportingService.writeReportFile(UUID.randomUUID(), pa, cacheInstitutionData, now, progressivo, dataInvioFlusso, recordsV, dateFrom, dateTo)
            );

            // Verify that CsvUtils.writeFile was called once
            csvUtilsMock.verify(() -> CsvUtils.writeFile(any(), any()), times(1));

            // Cleanup
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
}
