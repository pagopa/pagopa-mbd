package it.gov.pagopa.mbd.repository.model;

import java.util.List;

import com.azure.spring.data.cosmos.core.mapping.Container;

import lombok.Getter;
import lombok.Setter;

@Container(containerName = "biz-events")
@Getter
@Setter
public class PaMbdCount {
    private String idPA;
    private long mbdCount;
}
