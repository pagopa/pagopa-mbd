package it.gov.pagopa.mbd.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public enum AppError {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Something was wrong"),
    PARSING_GENERIC_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Generic parsing error", "Error while parsing payload. {0}"),
    PARSING_INVALID_HEADER(HttpStatus.INTERNAL_SERVER_ERROR,"SOAP Header parsing error", "Error while parsing payload. The SOAP header in payload is invalid: {0}"),
    PARSING_INVALID_BODY(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Body parsing error", "Error while parsing payload. The SOAP body in payload is invalid: {0}"),
    PARSING_INVALID_XML_NODES(HttpStatus.INTERNAL_SERVER_ERROR, "XML parsing error", "Error while parsing payload. The list of nodes extracted from document must be greater than zero, but currently it is zero."),
    PARSING_INVALID_ZIPPED_PAYLOAD(HttpStatus.INTERNAL_SERVER_ERROR, "ZIP extraction error", "Error while parsing payload. Cannot unzip payload correctly."),
    PARSING_PRIMITIVE_NOT_VALID(HttpStatus.INTERNAL_SERVER_ERROR, "Primitive not valid", "Error while checking primitive. Primitive [{0}] not valid."),

    CLIENT_APICONFIGCACHE(HttpStatus.INTERNAL_SERVER_ERROR, "APIConfig cache client error", "Error while communicating with APIConfig cache service. {0}"),

    BAD_REQUEST(HttpStatus.INTERNAL_SERVER_ERROR, "Bad Request", "%s"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized", "Error during authentication"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden", "This method is forbidden"),
    RESPONSE_NOT_READABLE(HttpStatus.BAD_GATEWAY, "Response Not Readable", "The response body is not readable"),

    UNKNOWN(null, null, null);


    public final HttpStatus httpStatus;
    public final String title;
    public final String details;


    AppError(HttpStatus httpStatus, String title, String details) {
        this.httpStatus = httpStatus;
        this.title = title;
        this.details = details;
    }
}


