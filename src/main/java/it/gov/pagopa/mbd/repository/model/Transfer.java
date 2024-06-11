package it.gov.pagopa.mbd.repository.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {

    private String idTransfer;

    private String fiscalCodePA;

    private String companyName;

    private String amount;

    private String transferCategory;

    private String remittanceInformation;

    @JsonProperty("MBDAttachment")
    private String MBDAttachment;


}
