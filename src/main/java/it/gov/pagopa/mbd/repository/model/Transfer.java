package it.gov.pagopa.mbd.repository.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class Transfer {

    private String idTransfer;

    private String fiscalCodePA;

    private String companyName;

    private String amount;

    private String transferCategory;

    private String remittanceInformation;

    private String MBDAttachment;


}
