/**
 * Title: Force Field X
 * Description: Force Field X - Software for Molecular Biophysics.
 * Copyright: Copyright (c) Michael J. Schnieders 2001-2009
 *
 * This file is part of Force Field X.
 *
 * Force Field X is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation.
 *
 * Force Field X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Force Field X; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */
package ffx.xray;

import static java.lang.Math.abs;
import static java.lang.Math.PI;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.util.logging.Logger;

import ffx.crystal.Crystal;
import ffx.crystal.HKL;
import ffx.crystal.ReflectionList;
import ffx.crystal.ReflectionSpline;
import ffx.numerics.ComplexNumber;
import ffx.numerics.Optimizable;

/**
 *
 * Fit structure factors using spline coefficients
 *
 * @author Tim Fenn<br>
 *
 * @see <a href="http://dx.doi.org/10.1107/S0021889802013420" target="_blank">
 * K. Cowtan, J. Appl. Cryst. (2002). 35, 655-663
 *
 */
public class SplineOptimizer implements Optimizable {

    public interface Type {

        public static final int FOFC = 1;
        public static final int FCTOESQ = 2;
        public static final int FOTOESQ = 3;
    }
    private static final Logger logger = Logger.getLogger(SplineOptimizer.class.getName());
    private static final double twopi2 = 2.0 * PI * PI;
    private final ReflectionList reflectionlist;
    private final ReflectionSpline spline;
    private final int nparams;
    private final int type;
    private final Crystal crystal;
    private final RefinementData refinementdata;
    private final double fc[][];
    private final double fs[][];
    private final double fctot[][];
    private final double fo[][];
    private final int freer[];
    protected double[] optimizationScaling = null;

    public SplineOptimizer(ReflectionList reflectionlist,
            RefinementData refinementdata, int nparams, int type) {
        this.reflectionlist = reflectionlist;
        this.crystal = reflectionlist.crystal;
        this.refinementdata = refinementdata;
        this.type = type;
        this.fc = refinementdata.fc;
        this.fs = refinementdata.fs;
        this.fctot = refinementdata.fctot;
        this.fo = refinementdata.fsigf;
        this.freer = refinementdata.freer;

        // initialize params
        this.spline = new ReflectionSpline(reflectionlist, nparams);
        this.nparams = nparams;
    }

    public double target(double x[], double g[],
            boolean gradient, boolean print) {
        double sum, sumfo;

        // zero out the gradient
        if (gradient) {
            for (int i = 0; i < g.length; i++) {
                g[i] = 0.0;
            }
        }

        sum = sumfo = 0.0;
        for (HKL ih : reflectionlist.hkllist) {
            int i = ih.index();
            if (ih.allowed() == 0.0
                    || Double.isNaN(fc[i][0])
                    || Double.isNaN(fo[i][0])) {
                continue;
            }

            if (type == Type.FOTOESQ
                    && fo[i][0] <= 0.0) {
                continue;
            }

            double eps = ih.epsilon();
            double s = Crystal.invressq(crystal, ih);

            // spline setup
            double fh = spline.f(s, x);

            ComplexNumber fct = new ComplexNumber(fctot[i][0], fctot[i][1]);

            double f1, f2, d, d2, dr, w;
            f1 = f2 = d = d2 = dr = w = 0.0;
            switch (type) {
                case Type.FCTOESQ:
                    w = 2.0 / ih.epsilonc();
                    f1 = pow(fct.abs() / sqrt(eps), 2.0);
                    d = f1 * fh - 1.0;
                    d2 = d * d / f1;
                    dr = 2.0 * d;
                    sumfo = 1.0;
                    break;
                case Type.FOTOESQ:
                    w = 2.0 / ih.epsilonc();
                    f1 = pow(fo[i][0] / sqrt(eps), 2.0);
                    d = f1 * fh - 1.0;
                    d2 = d * d / f1;
                    dr = 2.0 * d;
                    sumfo = 1.0;
                    break;
                case Type.FOFC:
                    w = 1.0;
                    f1 = fo[i][0];
                    f2 = fct.abs();
                    d = f1 - fh * f2;
                    d2 = d * d;
                    dr = -2.0 * fh * d;
                    sumfo += f1 * f1;
                    break;
            }

            sum += w * d2;

            // scaling F1 to F2
            /*
            double f1 = pow(fct.abs(), 2.0) / eps;
            double f2 = pow(fo[i][0], 2.0) / eps;
            double d = fh * f1 - f2;
            double d2 = d * d / f1;
            double dr = 2.0 * d;
             */


            if (gradient) {
                int i0 = spline.i0();
                int i1 = spline.i1();
                int i2 = spline.i2();
                double g0 = spline.dfi0();
                double g1 = spline.dfi1();
                double g2 = spline.dfi2();

                g[i0] += w * dr * g0;
                g[i1] += w * dr * g1;
                g[i2] += w * dr * g2;
            }
        }

        if (gradient) {
            for (int i = 0; i < g.length; i++) {
                g[i] /= sumfo;
            }
        }

        if (print) {
            StringBuffer sb = new StringBuffer("\n");
            sb.append(" Computed Potential Energy\n");
            sb.append(String.format("   residual:  %8.3f\n",
                    sum / sumfo));
            logger.info(sb.toString());
        }

        return sum / sumfo;
    }

    @Override
    public double energyAndGradient(double x[], double g[]) {
        if (optimizationScaling != null) {
            int len = x.length;
            for (int i = 0; i < len; i++) {
                x[i] /= optimizationScaling[i];
            }
        }

        double sum = target(x, g, true, false);

        if (optimizationScaling != null) {
            int len = x.length;
            for (int i = 0; i < len; i++) {
                x[i] *= optimizationScaling[i];
                g[i] /= optimizationScaling[i];
            }
        }

        return sum;
    }

    @Override
    public void setOptimizationScaling(double[] scaling) {
        if (scaling != null && scaling.length == nparams) {
            optimizationScaling = scaling;
        } else {
            optimizationScaling = null;
        }
    }

    @Override
    public double[] getOptimizationScaling() {
        return optimizationScaling;
    }
}