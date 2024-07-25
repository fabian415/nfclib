package com.advantech.nfclib;

/**
 * NFCException
 *
 * @author Fabian Chung
 * @version 1.0.0
 */
public class NFCException extends Exception {
    private NFCExceptionType type;

    public NFCException(NFCExceptionType type) {
        super();
        this.type = type;
    }

    public NFCException() {
        super();
        this.type = NFCExceptionType.NFC_EXCEPTION_TYPE_ERROR;
    }

    public NFCException(String message, NFCExceptionType type) {
        super(message);
        this.type = type;
    }

    public NFCExceptionType getType() {
        return type;
    }
}

