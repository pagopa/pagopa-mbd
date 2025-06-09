package it.gov.pagopa.mbd.config;

import java.util.function.Consumer;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import it.gov.pagopa.mbd.exception.MBDRetryException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RetryExecutor {

    private final RetryConfig retryConfig;

    // Retry and fire callback on final failure
    public <T> T executeWithRetry(RetryCallback<T, Exception> retryCallback, 
                                  Consumer<Exception> onFailureCallback) {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(retryConfig.getMaxAttempts());

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryConfig.getDelayMillis());
        backOffPolicy.setMultiplier(retryConfig.getMultiplier());

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        try {
            return retryTemplate.execute(retryCallback);
        } catch (Exception e) {
            // If the failure persists, execute the callback
            onFailureCallback.accept(e);
            throw new MBDRetryException("Retry failed after "
                + retryConfig.getMaxAttempts() + " attempts", e);
        }
    }
}

