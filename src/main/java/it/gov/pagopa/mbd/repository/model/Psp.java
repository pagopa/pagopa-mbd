package it.gov.pagopa.mbd.repository.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Psp {

    private String idPsp;

    private String idBrokerPsp;

    private String idChannel;

    private String psp;

    private String pspFiscalCode;

    private String channelDescription;

}
