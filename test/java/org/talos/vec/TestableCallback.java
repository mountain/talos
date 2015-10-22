package org.talos.vec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public abstract class TestableCallback extends Callback {

    protected int realStatus = 0;
    protected String realMessage = null;
    protected String realType = "";
    protected Object realPayload = null;

    public static TestableCallback noop() {
        return new TestableCallback() {
            @Override
            public void excepted() {
            }
        };
    }

    public abstract void excepted();

    @Override
    public void response() {
        excepted();
    }

    public void isOk() {
        realStatus = 0;
        realMessage = null;
        realType = "";
        realPayload = null;
    }

    public void isOk(String msg) {
        realStatus = 0;
        realMessage = msg;
        realType = "";
        realPayload = null;
    }

    public void isError(String msg) {
        realStatus = -1;
        realMessage = msg;
        realType = "";
        realPayload = null;
    }

    public void isStatus(int code) {
        realStatus = code;
        realMessage = null;
        realType = "";
        realPayload = null;
    }

    public void isIntegerValue(int val) {
        realStatus = 0;
        realMessage = null;
        realType = "i";
        realPayload = val;
    }

    public void isIntegerList(int[] list) {
        realStatus = 0;
        realMessage = null;
        realType = "I";
        realPayload = list;
    }

    public void isLongValue(long val) {
        realStatus = 0;
        realMessage = null;
        realType = "l";
        realPayload = val;
    }

    public void isLongList(long[] list) {
        realStatus = 0;
        realMessage = null;
        realType = "L";
        realPayload = list;
    }

    public void isFloatValue(float val) {
        realStatus = 0;
        realMessage = null;
        realType = "f";
        realPayload = val;
    }

    public void isFloatList(float[] list) {
        realStatus = 0;
        realMessage = null;
        realType = "F";
        realPayload = list;
    }

    public void isStringValue(String val) {
        realStatus = 0;
        realMessage = null;
        realType = "s";
        realPayload = val;
    }

    public void isStringList(String[] list) {
        realStatus = 0;
        realMessage = null;
        realType = "S";
        realPayload = list;
    }

    public void waitForFinish() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void validate() {
        assertEquals("assert status equals", status, realStatus);
        assertEquals("assert message equals", message, realMessage);
        assertEquals("assert type equals", type, realType);
        if (type.equals("I")) {
            assertArrayEquals("assert payload equals", (int[]) payload, (int[]) realPayload);
        } else if (type.equals("L")) {
            assertArrayEquals("assert payload equals", (long[]) payload, (long[]) realPayload);
        } else if (type.equals("F")) {
            assertArrayEquals((float[]) payload, (float[]) realPayload, (float) 0.00001);
        } else if (type.equals("S")) {
            assertArrayEquals("assert payload equals", (String[]) payload, (String[]) realPayload);
        } else {
            assertEquals("assert payload equals", payload, realPayload);
        }
    }

}
