package it.gov.pagopa.mbd.exception;

import lombok.Getter;

import java.text.MessageFormat;

@Getter
public class AppException extends RuntimeException {

    private final AppError error;
    private final transient Object[] args;

    public AppException(AppError error, Object... args) {
        super(composeMessage(error, getArgsOrNull(args)));
        this.error = error;
        this.args = getArgsOrNull(args);
    }

    public AppException(Throwable cause, AppError error, Object... args) {
        super(composeMessage(error, getArgsOrNull(args)), cause);
        this.error = error;
        this.args = getArgsOrNull(args);
    }

    private static Object[] getArgsOrNull(Object... args) {
        return args.length > 0 ? args.clone() : null;
    }

    private static String composeMessage(AppError error, Object[] args){
        if(error.getDetails() != null){
            return MessageFormat.format(error.getDetails(), args);
        }
        return null;
    }

}