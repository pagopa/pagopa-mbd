package it.gov.pagopa.mbd.repository.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.util.List;

@Data
@Container(containerName = "biz-events")
@Builder(toBuilder = true)
public class BizEventEntity {

    @Id
    private String id;

    @Version
    private String version;

    private String receiptId;

    private Psp psp;

    private Long timestamp;

    @Builder.Default
    private List<Transfer> transferList;
}
