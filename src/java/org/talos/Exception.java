package org.talos;

public class Exception extends RuntimeException {

    private static final long serialVersionUID = -4170061639543762537L;

    public Exception(String msg) {
        super(msg);
    }

    public Exception(Throwable t) {
        super(t);
    }

    public Exception(String msg, Throwable t) {
        super(msg, t);
    }
}
