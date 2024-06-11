package it.gov.pagopa.mbd.util.apiconfigcache;

import it.gov.pagopa.mbd.config.client.RequestResponseLoggingProperties;
import it.gov.pagopa.mbd.util.client.AbstractAppClientLoggingInterceptor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

@Slf4j
public class ApiConfigCacheClientLoggingInterceptor extends AbstractAppClientLoggingInterceptor {

    public ApiConfigCacheClientLoggingInterceptor(RequestResponseLoggingProperties clientLoggingProperties){
        super(clientLoggingProperties);
    }

    @Override
    protected void request(String clientOperationId, String operationId, HttpRequest request, byte[] reqBody) {
        if (log.isDebugEnabled()) {
            log.debug(createRequestMessage(clientOperationId, operationId, request, reqBody));
        }
    }

    @SneakyThrows
    @Override
    protected void response(String clientOperationId, String operationId, String clientExecutionTime, HttpRequest request, ClientHttpResponse response) {
        if (log.isDebugEnabled()) {
            log.debug(createResponseMessage(clientOperationId, operationId, clientExecutionTime, request, response));
        }
    }
}
