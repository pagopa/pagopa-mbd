package it.gov.pagopa.mbd.repository.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.util.List;

@Container(containerName = "biz-events")
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BizEventEntity {

    @Id
    private String id;

    @Version
    private String version;

    private String receiptId;

    private Psp psp;

    private Long timestamp;

    private List<Transfer> transferList;
}
