package it.gov.pagopa.mbd.repository.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Container(containerName = "data")
@Data
@ToString(exclude = "MBDAttachment")
@EqualsAndHashCode(exclude = "MBDAttachment")
@Builder(toBuilder = true)
public class Transfer {

    private String id;

    private String fiscalCodePA;

    private String companyName;

    private String amount;

    private String transferCategory;

    private String remittanceInformation;

    private String MBDAttachment;


}
