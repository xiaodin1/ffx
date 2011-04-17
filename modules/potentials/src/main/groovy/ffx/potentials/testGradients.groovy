// TEST GRADIENTS

import ffx.numerics.Potential;
import ffx.potential.bonded.Atom;
import ffx.potential.bonded.MolecularAssembly;

// Name of the file (PDB or XYZ).
String filename = args[0];
if (filename == null) {
   println("Usage: ffx testGradients filname");
   return;
}

// Things below this line normally do not need to be changed.
// ===============================================================================================

println("\n Testing the gradients of " + filename);
open(filename);

Potential energy = active.getPotentialEnergy();
Atom[] atoms = active.getAtomArray();
int n = atoms.length;

double[] x = new double[n*3];
double[] analytic = new double[3*n];
energy.getCoordinates(x);
energy.energyAndGradient(x,analytic);

double[] g = new double[3*n];

double step = 1.0e-5;
double e = 0.0;
double orig = 0.0;
double gradientTolerance = 1.0e-3;
double[] numeric = new double[3];

for (int i=0; i<n; i++) {
   Atom a0 = atoms[i];
   int i3 = i*3;
   int i0 = i3 + 0;
   int i1 = i3 + 1;
   int i2 = i3 + 2;

   // Find numeric dX
   orig = x[i0];
   x[i0] += step;
   e = energy.energyAndGradient(x,g);
   x[i0] -= 2.0 * step;
   e -= energy.energyAndGradient(x,g);
   x[i0] = orig;
   numeric[0] = e / (2.0 * step);

   // Find numeric dY
   orig = x[i1];
   x[i1] += step;
   e = energy.energyAndGradient(x,g);
   x[i1] -= 2.0 * step;
   e -= energy.energyAndGradient(x,g);
   x[i1] = orig;
   numeric[1] = e / (2.0 * step);

   // Find numeric dZ
   orig = x[i2];
   x[i2] += step;
   e = energy.energyAndGradient(x,g);
   x[i2] -= 2.0 * step;
   e -= energy.energyAndGradient(x,g);
   x[i2] = orig;
   numeric[2] = e / (2.0 * step);

   double dx = analytic[i0] - numeric[0];
   double dy = analytic[i1] - numeric[1];
   double dz = analytic[i2] - numeric[2];
   double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
   if (len > gradientTolerance) {
      println(" " + a0.toShortString() + String.format(" failed: %10.6f.", len) + String.format(
              "\n Analytic: (%12.4f, %12.4f, %12.4f)\n", analytic[i0], analytic[i1], analytic[i2]) 
              + String.format(" Numeric:  (%12.4f, %12.4f, %12.4f)\n", numeric[0], numeric[1], numeric[2]));
      return;
   } else {
      println(" " + a0.toShortString() + String.format(" passed: %10.6f.", len) + String.format(
              "\n Analytic: (%12.4f, %12.4f, %12.4f)\n", analytic[i0], analytic[i1], analytic[i2]) 
              + String.format(" Numeric:  (%12.4f, %12.4f, %12.4f)\n", numeric[0], numeric[1], numeric[2]));
   }
}