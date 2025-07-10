package it.gov.pagopa.mbd.repository.model;


import com.azure.spring.data.cosmos.core.mapping.Container;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Container(containerName = "biz-events")
@Getter
@Setter
@Builder
public class PaMbdCount {
    private String fiscalCodePA;
    private long mbdCount;
}
