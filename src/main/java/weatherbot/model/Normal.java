package weatherbot.model;

/** The standard normal distribution, which is all the model needs from statistics. */
final class Normal {

    private Normal() {}

    /** P(Z <= z) for a standard normal Z. */
    static double cdf(double z) {
        return 0.5 * (1 + erf(z / Math.sqrt(2)));
    }

    /** Abramowitz and Stegun 7.1.26: max error 1.5e-7, far below anything that matters for a price in cents. */
    static double erf(double x) {
        double sign = Math.signum(x);
        x = Math.abs(x);
        double t = 1 / (1 + 0.3275911 * x);
        double poly = t * (0.254829592 + t * (-0.284496736 + t * (1.421413741 + t * (-1.453152027 + t * 1.061405429))));
        return sign * (1 - poly * Math.exp(-x * x));
    }
}
