package org.talos.lda;

import org.talos.Option;

public class Estimator {

    // output model
    protected Model training;
    Option          option;

    public void estimate() {
        System.out.println("Sampling " + training.niters + " iteration!");

        int lastIter = training.liter;
        for (training.liter = lastIter + 1; training.liter < training.niters + lastIter; training.liter++) {
            System.out.println("Iteration " + training.liter + " ...");

            // for all z_i
            for (int m = 0; m < training.M; m++) {
                for (int n = 0; n < training.docs[m].length; n++) {
                    // z_i = z[m][n]
                    // sample from p(z_i|z_-i, w)
                    int topic = sampling(m, n);
                    training.z[m][n] = topic;
                }// end for each word
            }// end for each document

            if (option.savestep > 0) {
                if (training.liter % option.savestep == 0) {
                    System.out.println("Saving the model at iteration " + training.liter + " ...");
                    computeTheta();
                    computePhi();
                }
            }
        }// end iterations

        System.out.println("Gibbs sampling completed!\n");
        System.out.println("Saving the final model!\n");
        computeTheta();
        computePhi();
        training.liter--;
    }

    /**
     * Do sampling
     * 
     * @param m
     *            document number
     * @param n
     *            word number
     * @return topic id
     */
    public int sampling(int m, int n) {
        // remove z_i from the count variable
        int topic = training.z[m][n];
        int w = training.docs[m][n];

        training.nw[w][topic] -= 1;
        training.nd[m][topic] -= 1;
        training.nwsum[topic] -= 1;
        training.ndsum[m] -= 1;

        double Vbeta = training.V * training.beta;
        double Kalpha = training.K * training.alpha;

        // do multinominal sampling via cumulative method
        for (int k = 0; k < training.K; k++) {
            training.p[k] = (training.nw[w][k] + training.beta) / (training.nwsum[k] + Vbeta)
                    * (training.nd[m][k] + training.alpha) / (training.ndsum[m] + Kalpha);
        }

        // cumulate multinomial parameters
        for (int k = 1; k < training.K; k++) {
            training.p[k] += training.p[k - 1];
        }

        // scaled sample because of unnormalized p[]
        double u = Math.random() * training.p[training.K - 1];

        for (topic = 0; topic < training.K; topic++) {
            if (training.p[topic] > u) // sample topic w.r.t distribution p
                break;
        }

        // add newly estimated z_i to count variables
        training.nw[w][topic] += 1;
        training.nd[m][topic] += 1;
        training.nwsum[topic] += 1;
        training.ndsum[m] += 1;

        return topic;
    }

    public void computeTheta() {
        for (int m = 0; m < training.M; m++) {
            for (int k = 0; k < training.K; k++) {
                training.theta[m][k] = (training.nd[m][k] + training.alpha)
                        / (training.ndsum[m] + training.K * training.alpha);
            }
        }
    }

    public void computePhi() {
        for (int k = 0; k < training.K; k++) {
            for (int w = 0; w < training.V; w++) {
                training.phi[k][w] = (training.nw[w][k] + training.beta)
                        / (training.nwsum[k] + training.V * training.beta);
            }
        }
    }
}
