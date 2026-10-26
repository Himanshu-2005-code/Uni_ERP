package edu.univ.erp.ui.anim;

/** Easing curves mapping linear time t in [0,1] to eased progress. */
@FunctionalInterface
public interface Easing {

    double apply(double t);

    Easing LINEAR = t -> t;
    Easing OUT_CUBIC = t -> 1 - Math.pow(1 - t, 3);
    Easing IN_OUT_CUBIC = t -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    Easing OUT_QUART = t -> 1 - Math.pow(1 - t, 4);
    Easing OUT_EXPO = t -> t >= 1 ? 1 : 1 - Math.pow(2, -10 * t);
    Easing IN_OUT_QUINT = t -> t < 0.5 ? 16 * Math.pow(t, 5) : 1 - Math.pow(-2 * t + 2, 5) / 2;

    /** Overshoots slightly then settles, for pop-in effects. */
    Easing OUT_BACK = t -> {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    };

    /** A damped spring. */
    Easing SPRING = t -> t >= 1 ? 1 : 1 - Math.exp(-6.5 * t) * Math.cos(9.5 * t);
}
