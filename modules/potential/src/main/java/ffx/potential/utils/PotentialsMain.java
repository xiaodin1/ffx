/**
 * Title: Force Field X.
 *
 * Description: Force Field X - Software for Molecular Biophysics.
 *
 * Copyright: Copyright (c) Michael J. Schnieders 2001-2018.
 *
 * This file is part of Force Field X.
 *
 * Force Field X is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * Force Field X is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Force Field X; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Linking this library statically or dynamically with other modules is making a
 * combined work based on this library. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting executable under terms of your choice,
 * provided that you also meet, for each linked independent module, the terms
 * and conditions of the license of that module. An independent module is a
 * module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but
 * you are not obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
package ffx.potential.utils;

import java.io.File;
import java.util.logging.Logger;

import ffx.potential.ForceFieldEnergy;
import ffx.potential.MolecularAssembly;

/**
 * @author Stephen LuCore
 */
public class PotentialsMain {

    private static final Logger logger = Logger.getLogger(PotentialsMain.class.getName());

    private File structure;
    private MolecularAssembly molecularAssembly;

    public void setup(String filename) {
        if (filename == null) {
            filename = System.getProperty("testFile");
        }
        if (filename == null) {
            logger.warning("Couldn't find file: " + filename);
        }
        /**
         * Load the test system.
         */
        ClassLoader cl = this.getClass().getClassLoader();
        structure = new File(filename);
        PotentialsFileOpener opener = new PotentialsFileOpener(structure);
        opener.run();
        molecularAssembly = opener.getAssembly();
    }

    public static void main(String args[]) {
        PotentialsMain potentialsMain = new PotentialsMain();
        String filename = null;
        if (args != null && args.length > 0) {
            filename = args[0];
        }
        potentialsMain.setup(filename);
        int nEvals = 10;
        if (args != null && args.length > 1) {
            nEvals = Integer.parseInt(args[1]);
        }
        potentialsMain.timer(nEvals);
    }

    public void timer(int nEvals) {
        logger.info(String.format("\n N-Body Test: "));
        boolean gradient = true;
        boolean print = true;

        ForceFieldEnergy energy = molecularAssembly.getPotentialEnergy();

        long minTime = Long.MAX_VALUE;
        double sumTime2 = 0.0;
        int halfnEvals = (nEvals % 2 == 1) ? (nEvals / 2) : (nEvals / 2) - 1; // Halfway point
        for (int i = 0; i < nEvals; i++) {
            long time = -System.nanoTime();
            energy.energy(gradient, print);
            time += System.nanoTime();
            minTime = time < minTime ? time : minTime;
            if (i >= (int) (nEvals / 2)) {
                double time2 = time * 1.0E-9;
                sumTime2 += (time2 * time2);
            }
        }
        ++halfnEvals;
        double rmsTime = Math.sqrt(sumTime2 / halfnEvals);
        logger.info(String.format(" Minimum time: %14.5f (sec)", minTime * 1.0E-9));
        logger.info(String.format(" RMS time (latter half): %14.5f (sec)", rmsTime));
    }
}
