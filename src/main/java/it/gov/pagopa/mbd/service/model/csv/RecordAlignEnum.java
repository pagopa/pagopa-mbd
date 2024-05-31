package it.gov.pagopa.mbd.service.model.csv;

import lombok.Getter;

@Getter
public enum RecordAlignEnum {

    ALIGN_LEFT("L"),
    ALIGN_RIGHT("R");

    private final String value;

    RecordAlignEnum(String value) {
        this.value = value;
    }

}
