package org.talos.nlp;

public class Token {

    private int      freq   = 0;
    private int      type;
    private int      pos;
    private String   value;
    private String   pinyin = null;
    private String[] tags   = null;
    private String[] synm   = null;

    public Token(String value, int type) {
        this.value = value;
        this.type = type;
    }

    public Token(String value, int freq, int type) {
        this.value = value;
        this.freq = freq;
        this.type = type;
    }

    public String value() {
        return value;
    }

    public int length() {
        return value.length();
    }

    public int freq() {
        return freq;
    }

    public int type() {
        return type;
    }

    public int position() {
        return pos;
    }

    public String pinyin() {
        return pinyin;
    }

    public String[] synm() {
        return synm;
    }

    public String[] tags() {
        return tags;
    }

    public void position(int p) {
        pos = p;
    }

    public void synm(String[] s) {
        this.synm = s;
    }

    public void tags(String[] t) {
        this.tags = t;
    }

    public void pinyin(String p) {
        pinyin = p;
    }

    public void addTag(String t) {
        if (tags == null) {
            tags = new String[1];
            tags[0] = t;
        } else {
            String[] bak = tags;
            tags = new String[tags.length + 1];
            int j;
            for (j = 0; j < bak.length; j++)
                tags[j] = bak[j];
            tags[j] = t;
        }
    }

    public void addSynm(String s) {
        if (synm == null) {
            synm = new String[1];
            synm[0] = s;
        } else {
            String[] bak = synm;
            synm = new String[synm.length + 1];
            int j;
            for (j = 0; j < bak.length; j++)
                synm[j] = bak[j];
            synm[j] = s;
        }
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof Token) {
            Token word = (Token) o;
            boolean bool = word.value().equalsIgnoreCase(this.value());
            /*
             * value equals and the type of the word must be equals too, for
             * there is many words in different lexicon with a same value but in
             * different use.
             */
            return (bool && (word.type() == this.type()));
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        sb.append('/');
        // append the cx
        if (tags != null) {
            for (int j = 0; j < tags.length; j++) {
                if (j == 0)
                    sb.append(tags[j]);
                else {
                    sb.append(',');
                    sb.append(tags[j]);
                }
            }
        } else
            sb.append("null");
        sb.append('/');
        sb.append(pinyin);
        sb.append('/');
        // append the tyc
        if (synm != null) {
            for (int j = 0; j < synm.length; j++) {
                if (j == 0)
                    sb.append(synm[j]);
                else {
                    sb.append(',');
                    sb.append(synm[j]);
                }
            }
        } else
            sb.append("null");

        if (value.length() == 1) {
            sb.append('/');
            sb.append(freq);
        }

        return sb.toString();
    }
}
