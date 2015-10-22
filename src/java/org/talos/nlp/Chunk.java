package org.talos.nlp;

public class Chunk {

    private Token[] tokens;

    // average words length
    private double awl = -1D;

    // words variance
    private double wv = -1D;

    // single word morphemic freedom
    private double swmf = -1D;

    private int length = -1;

    public Chunk(Token[] tokens) {
        this.tokens = tokens;
    }

    public Token[] tokens() {
        return tokens;
    }

    public double awl() {
        if (awl == -1D) {
            awl = (double) length() / (double) tokens.length;
        }
        return awl;
    }

    public double wv() {
        if (wv == -1D) {
            double variance = 0D, temp;
            for (int j = 0; j < tokens.length; j++) {
                temp = (double) tokens[j].length() - awl();
                variance = variance + temp * temp;
            }
            // wv = Math.sqrt( variance / (double) tokens.length );
            wv = variance / tokens.length;
        }
        return wv;
    }

    public double swmf() {
        if (swmf == -1D) {
            swmf = 0;
            for (int j = 0; j < tokens.length; j++) {
                // one-character word
                if (tokens[j].length() == 1) {
                    swmf = swmf + Math.log((double) tokens[j].freq());
                }
            }
        }
        return swmf;
    }

    public int length() {
        if (length == -1) {
            length = 0;
            for (int j = 0; j < tokens.length; j++) {
                length = length + tokens[j].length();
            }
        }
        return length;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("chunk: ");
        for (int j = 0; j < tokens.length; j++) {
            sb.append(tokens[j] + "/");
        }
        return sb.toString();
    }

}
