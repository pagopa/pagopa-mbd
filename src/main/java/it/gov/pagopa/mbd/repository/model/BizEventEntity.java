package it.gov.pagopa.mbd.repository.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.Id;

import java.util.List;

@Container(containerName = "biz-events")
@Data
@Builder(toBuilder = true)
public class BizEventEntity {

    @Id
    private String id;

    @PartitionKey
    private String partitionKey;

    private String version;

    private String receiptId;

    private Psp psp;

    private Long timestamp;

    private List<Transfer> transferList;
}
