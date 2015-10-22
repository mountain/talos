package org.talos.lda;

public class Inferencer {

    public Model  training;
    private Model model;
    public int    niters = 100;

    // inference new model ~ getting data from a specified dataset
    public Model inference(int[] doc) {
        model = new Model();
        model.M = 1;
        model.docs = new int[][] { doc };

        System.out.println("Sampling " + niters + " iteration for inference!");
        for (model.liter = 1; model.liter <= niters; model.liter++) {
            for (int m = 0; m < model.M; ++m) {
                for (int n = 0; n < model.docs[m].length; n++) {
                    int topic = infSampling(m, n);
                    model.z[m][n] = topic;
                }
            }// end foreach new doc
        }// end iterations

        System.out.println("Gibbs sampling for inference completed!");

        computeNewTheta();
        computeNewPhi();
        model.liter--;
        return model;
    }

    /**
     * do sampling for inference m: document number n: word number?
     */
    protected int infSampling(int m, int n) {
        // remove z_i from the count variables
        int topic = model.z[m][n];
        int w = model.docs[m][n];
        model.nw[w][topic] -= 1;
        model.nd[m][topic] -= 1;
        model.nwsum[topic] -= 1;
        model.ndsum[m] -= 1;

        double Vbeta = training.V * model.beta;
        double Kalpha = training.K * model.alpha;

        // do multinomial sampling via cummulative method
        for (int k = 0; k < model.K; k++) {
            model.p[k] = (training.nw[w][k] + model.nw[w][k] + model.beta)
                    / (training.nwsum[k] + model.nwsum[k] + Vbeta) * (model.nd[m][k] + model.alpha)
                    / (model.ndsum[m] + Kalpha);
        }

        // cummulate multinomial parameters
        for (int k = 1; k < model.K; k++) {
            model.p[k] += model.p[k - 1];
        }

        // scaled sample because of unnormalized p[]
        double u = Math.random() * model.p[model.K - 1];

        for (topic = 0; topic < model.K; topic++) {
            if (model.p[topic] > u)
                break;
        }

        // add newly estimated z_i to count variables
        model.nw[w][topic] += 1;
        model.nd[m][topic] += 1;
        model.nwsum[topic] += 1;
        model.ndsum[m] += 1;

        return topic;
    }

    protected void computeNewTheta() {
        for (int m = 0; m < model.M; m++) {
            for (int k = 0; k < model.K; k++) {
                model.theta[m][k] = (model.nd[m][k] + model.alpha) / (model.ndsum[m] + model.K * model.alpha);
            }// end foreach topic
        }// end foreach new document
    }

    protected void computeNewPhi() {
        for (int k = 0; k < model.K; k++) {
            for (int w = 0; w < model.V; w++) {

                model.phi[k][w] = (training.nw[w][k] + model.nw[w][k] + model.beta)
                        / (model.nwsum[k] + model.nwsum[k] + training.V * model.beta);
            }// end foreach word
        }// end foreach topic
    }

    protected void computeTrnTheta() {
        for (int m = 0; m < training.M; m++) {
            for (int k = 0; k < training.K; k++) {
                training.theta[m][k] = (training.nd[m][k] + training.alpha)
                        / (training.ndsum[m] + training.K * training.alpha);
            }
        }
    }

    protected void computeTrnPhi() {
        for (int k = 0; k < training.K; k++) {
            for (int w = 0; w < training.V; w++) {
                training.phi[k][w] = (training.nw[w][k] + training.beta)
                        / (training.nwsum[k] + training.V * training.beta);
            }
        }
    }
}
