package it.gov.pagopa.mbd.config;

import it.gov.pagopa.mbd.service.ConfigCacheService;
import it.gov.pagopa.mbd.service.GenerateReportingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Configuration
@Slf4j
@EnableScheduling
@ConditionalOnExpression("'${info.properties.environment}'!='test'")
public class ScheduledJobsConfig {

    private final GenerateReportingService generateReportingService;
    private final ConfigCacheService configCacheService;

    public ScheduledJobsConfig(GenerateReportingService generateReportingService, ConfigCacheService configCacheService) {
        this.generateReportingService = generateReportingService;
        this.configCacheService = configCacheService;
    }


    @Scheduled(cron = "${mbd.rendicontazioni.genera.cron:-}")
    public void generateReporting() {
        LocalDate date = LocalDate.now();
        log.info("[Scheduled] Starting genera rendicontazioni mbd for {}", date);
        generateReportingService.execute(date, new String[0]);
    }

    @Scheduled(cron = "${mbd.cache.refresh.cron:-}")
    @EventListener(ApplicationReadyEvent.class)
    public void refreshCache() {
        log.info("[Scheduled] Starting configuration cache refresh {}", ZonedDateTime.now());
        configCacheService.loadCache();
    }

}
