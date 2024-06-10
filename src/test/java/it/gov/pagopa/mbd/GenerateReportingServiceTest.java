package it.gov.pagopa.mbd;

import it.gov.pagopa.mbd.repository.BizEventRepository;
import it.gov.pagopa.mbd.service.ConfigCacheService;
import it.gov.pagopa.mbd.utils.TestUtils;
import org.junit.jupiter.api.Test;
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

import java.util.List;
import java.util.Map;

import static it.gov.pagopa.mbd.utils.TestUtils.getBizEvent;
import static org.junit.Assert.assertNotNull;
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

    @MockBean
    private BizEventRepository bizEventRepository;

    @MockBean
    private it.gov.pagopa.gen.mbd.client.cache.model.ConfigDataV1Dto configDataV1Dto;

    @Test
    void generateForAllEc() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData", TestUtils.configData());

        when(bizEventRepository.getBizEventsByDateFromAndDateToAndEC(anyLong(), anyLong(), anyString())).thenReturn(getBizEvent());

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

        verify(bizEventRepository,times(1)).getBizEventsByDateFromAndDateToAndEC(anyLong(), anyLong(), anyString());
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
                        (result) -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });

        verify(bizEventRepository,times(1)).getBizEventsByDateFromAndDateToAndEC(anyLong(), anyLong(), anyString());
    }
}
