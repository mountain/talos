package org.talos.lda;


public class Model {

    public int[][] docs;         // link to a dataset

    public int M = 0;   // dataset size (i.e., number of
    // docs)
    public int V = 0;   // vocabulary size
    public int K = 2048; // number of topics

    public double alpha = 50.0 / K, beta = 0.1; // LDA
    // hyperparameters
    public int niters = 2000;                // number of Gibbs
    // sampling
    // iteration
    public int liter = 0;                   // the iteration at
    // which the model
    // was saved
    public int savestep;                     // saving period
    public int twords;                       // print out top
    // words per each
    // topic
    public int withrawdata;

    // Estimated/Inferenced parameters
    public double[][] theta;                        // theta: document -
    // topic
    // distributions,
    // size M x
    // K
    public double[][] phi;                          // phi: topic-word
    // distributions,
    // size K x V

    // temp variables while sampling
    public int[][] z;                            // topic assignments
    // for words, size M
    // x
    // doc.size()
    protected int[][] nw;                           // nw[i][j]: number
    // of instances of
    // word/term i
    // assigned to topic
    // j, size V x K
    protected int[][] nd;                           // nd[i][j]: number
    // of words in
    // document i
    // assigned to
    // topic j, size M x
    // K
    protected int[] nwsum;                        // nwsum[j]: total
    // number of words
    // assigned to topic
    // j, size K
    protected int[] ndsum;                        // ndsum[i]: total
    // number of words
    // in document i,
    // size M

    // temp variables for sampling
    protected double[] p;

}
