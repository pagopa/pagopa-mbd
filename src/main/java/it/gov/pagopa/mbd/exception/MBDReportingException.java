package it.gov.pagopa.mbd.exception;

public class MBDReportingException extends Exception {

  private static final long serialVersionUID = -2553855687464938614L;

  public MBDReportingException(String message) {
    super(message);
  }

  public MBDReportingException(String message, Throwable cause) {
    super(message, cause);
  }
}
