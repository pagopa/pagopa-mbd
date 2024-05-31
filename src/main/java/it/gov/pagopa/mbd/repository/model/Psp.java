package it.gov.pagopa.mbd.repository.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class Psp {

    private String idPsp;

    private String idBrokerPsp;

    private String idChannel;

    private String psp;

    private String pspFiscalCode;

    private String channelDescription;

}
