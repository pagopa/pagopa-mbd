package it.gov.pagopa.mbd.exception;

public class MBDRetryException  extends RuntimeException {

	private static final long serialVersionUID = 3153742091254864806L;

	public MBDRetryException(String message) {
		super(message);
	}

	public MBDRetryException(String message, Throwable cause) {
		super(message, cause);
	}

	public MBDRetryException(Throwable cause) {
		super(cause);
	}

}
