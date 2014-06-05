/**
 * Title: Force Field X.
 *
 * Description: Force Field X - Software for Molecular Biophysics.
 *
 * Copyright: Copyright (c) Michael J. Schnieders 2001-2012.
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
 */

// ORTHOGONAL SPACE RANDOM WALK

// Apache Imports
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;

// Groovy Imports
import groovy.util.CliBuilder;

// Paralle Java Imports
import edu.rit.pj.Comm;

// Force Field X Imports
import ffx.algorithms.OSRW;
import ffx.potential.ForceFieldEnergy;

// Printing out PMF information.
boolean pmf = false;

// Things below this line normally do not need to be changed.
// ===============================================================================================

// Create the command line parser.
def cli = new CliBuilder(usage:' ffxc osrwHistogram [options] <filename>');
cli.h(longOpt:'help', 'Print this help message.');
cli.p(longOpt:'PMF', args:0, 'Print out potential of mean force information.');

def options = cli.parse(args);
List<String> arguments = options.arguments();
if (options.h || arguments == null || arguments.size() != 1) {
    return cli.usage();
}

// Read in command line file.
String filename = arguments.get(0);

// PMF?
if (options.a) {
    pmf = true;
}

println("\n Evaluating OSRW Histogram for " + filename);

File structureFile = new File(FilenameUtils.normalize(filename));
structureFile = new File(structureFile.getAbsolutePath());
String baseFilename = FilenameUtils.removeExtension(structureFile.getName());
File histogramRestart = new File(baseFilename + ".his");
File lambdaRestart = null;

Comm world = Comm.world();
int size = world.size();

// For a multi-process job, try to get the restart files from rank sub-directories.
if (size > 1) {
    int rank = world.rank();
    File rankDirectory = new File(structureFile.getParent() + File.separator
        + Integer.toString(rank));
    lambdaRestart = new File(rankDirectory.getPath() + File.separator + baseFilename + ".lam");
    structureFile = new File(rankDirectory.getPath() + File.separator + structureFile.getName());
} else {
    // For a single process job, try to get the restart files from the current directory.
    lambdaRestart = new File(baseFilename + ".lam");
}

open(filename);

// Get a reference to the active system's ForceFieldEnergy and atom array.
ForceFieldEnergy energy = active.getPotentialEnergy();

// Print the current energy
energy.energy(true, true);

// Asychronous communication between walkers.
boolean asynchronous = false;

// Time step in femtoseconds.
double timeStep = 1.0;

// Frequency to log thermodynamics information in picoseconds.
double printInterval = 1.0;

// Frequency to write out coordinates in picoseconds.
double saveInterval = 100.0;

// Temperture in degrees Kelvin.
double temperature = 298.15;

// Wrap the potential energy inside an OSRW instance.
OSRW osrw = new OSRW(energy, energy, lambdaRestart, histogramRestart, active.getProperties(),
    temperature, timeStep, printInterval, saveInterval, asynchronous, sh);

if (pmf){
    osrw.evaluatePMF();
}

