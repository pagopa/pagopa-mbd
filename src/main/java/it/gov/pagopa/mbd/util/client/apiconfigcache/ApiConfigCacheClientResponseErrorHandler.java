package it.gov.pagopa.mbd.util.client.apiconfigcache;

import it.gov.pagopa.mbd.exception.AppError;
import it.gov.pagopa.mbd.exception.AppException;
import it.gov.pagopa.mbd.util.client.AbstractResponseErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.io.IOException;
import java.nio.charset.Charset;

@Component
@Slf4j
public class ApiConfigCacheClientResponseErrorHandler extends AbstractResponseErrorHandler implements ResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().is5xxServerError() ||
                response.getStatusCode().is4xxClientError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();
        String statusText = response.getStatusText();
        HttpHeaders headers = response.getHeaders();
        byte[] body = getResponseBody(response);
        Charset charset = getCharset(response);
        String message = getErrorMessage(statusCode.value(), statusText, body, charset);

        RestClientResponseException ex;
        if (statusCode.is4xxClientError()) {
            ex = HttpClientErrorException.create(message, statusCode, statusText, headers, body, charset);
        } else if (statusCode.is5xxServerError()) {
            ex = HttpServerErrorException.create(message, statusCode, statusText, headers, body, charset);
        } else {
            ex = new UnknownHttpStatusCodeException(message, statusCode.value(), statusText, headers, body, charset);
        }

        throw new AppException(ex, AppError.INTERNAL_SERVER_ERROR, message);
    }


}
