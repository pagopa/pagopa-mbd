package it.gov.pagopa.mbd;

import it.gov.pagopa.mbd.config.RetryConfig;
import it.gov.pagopa.mbd.config.RetryExecutor;
import it.gov.pagopa.mbd.exception.MBDRetryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryExecutorTest {

    private RetryExecutor retryExecutor;

    @BeforeEach
    void setUp() {
        RetryConfig config = new RetryConfig();
        config.setMaxAttempts(3);
        config.setDelay(100); // millis
        config.setMultiplier(1.0); // no backoff increase

        retryExecutor = new RetryExecutor(config);
    }

    @Test
    void testRetryLogicFailsAfterMaxAttempts() {
    	AtomicInteger executionCounter = new AtomicInteger(0);
    	AtomicBoolean failureCallbackExecuted = new AtomicBoolean(false);
        MBDRetryException thrown = assertThrows(
            MBDRetryException.class,
            () -> retryExecutor.executeWithRetry(context -> {
                executionCounter.incrementAndGet();
                throw new IOException("Simulated failure");
            }, exception -> failureCallbackExecuted.set(true))
        );

        assertEquals(3, executionCounter.get(), "Should retry exactly maxAttempts times");
        assertTrue(failureCallbackExecuted.get(), "Failure callback should be executed");
        assertTrue(thrown.getCause() instanceof IOException);
        assertEquals("Retry failed after 3 attempts", thrown.getMessage());
    }

    @Test
    void testRetryLogicSucceedsOnSecondAttempt()  {
        AtomicInteger count = new AtomicInteger(0);

        String result = retryExecutor.executeWithRetry(context -> {
            if (count.incrementAndGet() < 2) {
                throw new IOException("Simulated transient error");
            }
            return "Success";
        }, exception -> {
            fail("Callback should not be invoked on success");
        });

        assertEquals("Success", result);
        assertEquals(2, count.get());
    }
}
