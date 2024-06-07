package it.gov.pagopa.mbd;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.mbd.controller.RecoveryController;
import it.gov.pagopa.mbd.repository.BizEventRepository;
import it.gov.pagopa.mbd.service.ConfigCacheService;
import it.gov.pagopa.mbd.service.GenerateReportingService;
import it.gov.pagopa.mbd.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@ActiveProfiles(profiles = "test")
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class GenerateReportingServiceTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private MockMvc mvc;

    @Autowired private ConfigCacheService configCacheService;

    @MockBean
    private RecoveryController recoveryController;

    @MockBean
    private GenerateReportingService generateReportingService;

    @MockBean
    private BizEventRepository bizEventRepository;

    @Test
    void success_positive() throws Exception {
        String station = "mystation";
        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData", TestUtils.configData(station));

//        when(rptRequestRepository.findById(any())).thenReturn(
//                Optional.of(
//                        RPTRequestEntity.builder().primitive("nodoInviaRPT")
//                                .payload(
//                                        TestUtils.zipAndEncode(TestUtils.getRptPayload(false,station,"100.00","datispec"))
//                                ).build()
//                )
//        );
//        when(cacheRepository.read(any(),any())).thenReturn("asdsad");

        GenerateReportingService service = Mockito.mock( GenerateReportingService.class, CALLS_REAL_METHODS );

        mvc.perform(MockMvcRequestBuilders.patch("/recover")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(CollectionUtils.toMultiValueMap(
                                Map.of("from", List.of("2024-01-01"), "to", List.of("2024-01-02"))
                        ))
                )

                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andDo(
                        (result) -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });

//        verify(reEventRepository,times(5)).save(any());
    }

//    @Test
//    void success_negative() throws Exception {
//        String station = "mystation";
//        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData",TestUtils.configData(station));
//
//        when(rptRequestRepository.findById(any())).thenReturn(Optional.of(RPTRequestEntity
//                .builder()
//                        .id(UUID.randomUUID().toString())
//                        .primitive("nodoInviaRPT")
//                        .payload(TestUtils.zipAndEncode(TestUtils.getRptPayload(false,"mystation","10.00","dati")))
//                .build()));
//        when(cacheRepository.read(any(),any())).thenReturn("wisp_nav2iuv_dominio");
//
//        ReceiptDto[] receiptDtos = {
//                new ReceiptDto("token", "dominio", "iuv")
//        };
//        mvc.perform(MockMvcRequestBuilders.post("/receipt/ko")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(new ReceiptRequest(objectMapper.writeValueAsString(receiptDtos)))))
//                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
//                .andDo(
//                        (result) -> {
//                            assertNotNull(result);
//                            assertNotNull(result.getResponse());
//                        });
//
//        verify(reEventRepository,times(5)).save(any());
//    }
//
//    @Test
//    void error_send_rt() throws Exception {
//        String station = "mystation";
//        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData",TestUtils.configData(station));
//
//        when(rptRequestRepository.findById(any())).thenReturn(
//                Optional.of(
//                        RPTRequestEntity.builder().primitive("nodoInviaRPT")
//                                .payload(
//                                        TestUtils.zipAndEncode(TestUtils.getRptPayload(false,station,"100.00","datispec"))
//                                ).build()
//                )
//        );
//        when(cacheRepository.read(any(),any())).thenReturn("asdsad");
//        doThrow(new PaaInviaRTException("PAA_ERRORE_RESPONSE","PAA_ERRORE_RESPONSE","Errore PA")).doNothing().when(paaInviaRTService).send(anyString(), anyString());
//
//        mvc.perform(MockMvcRequestBuilders.post("/receipt/ok")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(new ReceiptRequest(getPaSendRTPayload()))))
//                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
//                .andDo(
//                        (result) -> {
//                            assertNotNull(result);
//                            assertNotNull(result.getResponse());
//                        });
//
//        verify(paaInviaRTService, times(1)).send(anyString(), anyString());
//        verify(reEventRepository,times(6)).save(any());
//    }
//
//    @Test
//    void error_send_rt2() throws Exception {
//        String station = "mystation";
//        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData",TestUtils.configData(station));
//
//        when(rptRequestRepository.findById(any())).thenReturn(
//                Optional.of(
//                        RPTRequestEntity.builder().primitive("nodoInviaRPT")
//                                .payload(
//                                        TestUtils.zipAndEncode(TestUtils.getRptPayload(false,station,"100.00","datispec"))
//                                ).build()
//                )
//        );
//        when(cacheRepository.read(any(),any())).thenReturn("asdsad");
//        doAnswer((i) -> {
//            return new ResponseEntity<>(HttpStatusCode.valueOf(200));
//        }).when(paaInviaRTService).send(anyString(), anyString());
//        mvc.perform(MockMvcRequestBuilders.post("/receipt/ok")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(new ReceiptRequest(getPaSendRTPayload()))))
//                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
//                .andDo(
//                        (result) -> {
//                            assertNotNull(result);
//                            assertNotNull(result.getResponse());
//                        });
//
//        verify(paaInviaRTService, times(1)).send(anyString(), anyString());
//        verify(reEventRepository,times(5)).save(any());
//    }

}
