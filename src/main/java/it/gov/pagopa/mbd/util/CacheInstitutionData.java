package it.gov.pagopa.mbd.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CacheInstitutionData {
  private String mittenteCodiceFiscale;
  private String intermediarioDenominazione;
  private String intermediarioComune;
  private String intermediarioSiglaProvincia;
  private String intermediarioCap;
  private String intermediarioIndirizzo;
  private String codiceTrasmissivo;
}
