package it.gov.pagopa.mbd.service;

import it.gov.pagopa.mbd.repository.BizEventRepository;
import it.gov.pagopa.mbd.repository.model.BizEventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@CacheConfig(cacheNames="cache")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mbd.rendicontazioni.generate", name = "enabled")
public class GenerateReportingService {

//    private final FtpClient ftpClient;

    private final BizEventRepository bizEventRepository;

    public void execute(LocalDate date) {
        log.info("generate reporting MBD for {}", date);

        List<BizEventEntity> bizEvents = bizEventRepository.findMBDAttachment();

        bizEvents.forEach(v -> {
            log.debug("{}, {}", v.getId(), v.getTimestamp());
        });

        //check filesystem
//      checkFileSystem(request.key.isEmpty)
    }

    @Async
    public void recovery(LocalDate from, LocalDate to) {
        log.info("recovery rendicontazioni MBD dal {} al {}", from, to);
        LocalDate date = from;
        do {
            execute(date);
            date = date.plusDays(1);
        } while( date.isBefore(to) );

    }

}
