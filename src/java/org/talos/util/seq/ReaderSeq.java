package org.talos.util.seq;

import java.io.IOException;
import java.io.Reader;

public class ReaderSeq extends Seq<Character> {

    private Reader reader;

    public ReaderSeq(Reader reader) {
        super(null, null);
        this.reader = reader;
    }

    @Override
    public Character head() {
        try {
            int c = reader.read();
            if (c > -1) {
                head = (char) c;
                tail = this;
            } else {
                head = null;
                tail = null;
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return head;
    }

}
