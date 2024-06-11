package it.gov.pagopa.mbd.service;

import it.gov.pagopa.gen.mbd.client.cache.model.CacheVersionDto;
import it.gov.pagopa.mbd.exception.AppError;
import it.gov.pagopa.mbd.exception.AppException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@CacheConfig(cacheNames="cache")
@Slf4j
@RequiredArgsConstructor
public class ConfigCacheService {

    private final it.gov.pagopa.gen.mbd.client.cache.invoker.ApiClient configCacheClient;

    @Value("${client.cache.keys}")
    private List<String> cacheKeys;

    @Getter
    private it.gov.pagopa.gen.mbd.client.cache.model.ConfigDataV1Dto configData;

    public void loadCache() {
        log.info("loadCache from cache api");

        try {
            it.gov.pagopa.gen.mbd.client.cache.api.CacheApi apiInstance = new it.gov.pagopa.gen.mbd.client.cache.api.CacheApi(configCacheClient);
            if(configData == null){
                configData = apiInstance.get(cacheKeys);
            }else{
                CacheVersionDto id = apiInstance.id();
                if(!configData.getVersion().equals(id.getVersion())){
                    configData = apiInstance.get(cacheKeys);
                }
            }

        } catch (RestClientException e) {
            throw new AppException(AppError.INTERNAL_SERVER_ERROR,
                    String.format("RestClientException ERROR [%s] - %s", e.getCause().getClass().getCanonicalName(), e.getMessage()));
        }
    }

}
