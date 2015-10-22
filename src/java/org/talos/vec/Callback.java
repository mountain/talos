package org.talos.vec;

public abstract class Callback {

    protected int status = 0;
    protected String message = null;
    protected String type = "";
    protected Object payload = null;

    public void ok() {
        status = 0;
        message = null;
        type = "";
        payload = null;
    }

    public void ok(String msg) {
        status = 0;
        message = msg;
        type = "";
        payload = null;
    }

    public void error(String msg) {
        status = -1;
        message = msg;
        type = "";
        payload = null;
    }

    public void status(int code) {
        status = code;
        message = null;
        type = "";
        payload = null;
    }

    public void integerValue(int val) {
        status = 0;
        message = null;
        type = "i";
        payload = val;
    }

    public void integerList(int[] list) {
        status = 0;
        message = null;
        type = "I";
        payload = list;
    }

    public void longValue(int val) {
        status = 0;
        message = null;
        type = "l";
        payload = val;
    }

    public void longList(long[] list) {
        status = 0;
        message = null;
        type = "L";
        payload = list;
    }

    public void floatValue(float val) {
        status = 0;
        message = null;
        type = "f";
        payload = val;
    }

    public void floatList(float[] list) {
        status = 0;
        message = null;
        type = "F";
        payload = list;
    }

    public void stringValue(String val) {
        status = 0;
        message = null;
        type = "s";
        payload = val;
    }

    public void stringList(String[] list) {
        status = 0;
        message = null;
        type = "S";
        payload = list;
    }

    public abstract void response();

}
