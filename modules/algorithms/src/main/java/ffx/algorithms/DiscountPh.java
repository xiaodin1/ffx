/**
 * Title: Force Field X.
 *
 * Description: Force Field X - Software for Molecular Biophysics.
 *
 * Copyright: Copyright (c) Michael J. Schnieders 2001-2016.
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
package ffx.algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.lang.String.format;

import org.apache.commons.io.FilenameUtils;

import static org.apache.commons.math3.util.FastMath.exp;
import static org.apache.commons.math3.util.FastMath.random;

import ffx.algorithms.DiscountPh.Mode;
import ffx.algorithms.mc.RosenbluthCBMC;
import ffx.potential.AssemblyState;
import ffx.potential.ForceFieldEnergy;
import ffx.potential.MolecularAssembly;
import ffx.potential.bonded.Atom;
import ffx.potential.bonded.BondedUtils;
import ffx.potential.bonded.MultiResidue;
import ffx.potential.bonded.MultiTerminus;
import ffx.potential.bonded.Polymer;
import ffx.potential.bonded.Residue;
import ffx.potential.bonded.Residue.ResidueType;
import ffx.potential.bonded.ResidueEnumerations.AminoAcid3;
import ffx.potential.bonded.ResidueState;
import ffx.potential.bonded.Rotamer;
import ffx.potential.bonded.RotamerLibrary;
import ffx.potential.extended.ExtendedSystem;
import ffx.potential.extended.ExtendedVariable;
import ffx.potential.extended.ThermoConstants;
import ffx.potential.extended.TitrationESV;
import ffx.potential.extended.TitrationESV.TitrationUtils;
import ffx.potential.parameters.ForceField;
import ffx.potential.parsers.PDBFilter;
import ffx.potential.parsers.SystemFilter;

/**
 * @author S. LuCore
 */
public class DiscountPh {
    
    // System handles
    private static final Logger logger = Logger.getLogger(DiscountPh.class.getName());
    private static final double NS_TO_SEC = 0.000000001;
    private StringBuilder discountLogger;
    private final MolecularAssembly mola;
    private final ForceField ff;
    private final ForceFieldEnergy ffe;
    private final MolecularDynamics molDyn;
    private final String originalFilename;
    private long startTime;
    
    // Titrating
    private List<Residue> chosenResidues = new ArrayList<>();
    private List<MultiResidue> titratingMultiResidues = new ArrayList<>();
    private List<MultiTerminus> titratingTermini = new ArrayList<>();
    private List<ExtendedVariable> titratingESVs = new ArrayList<>();
    private boolean finalized = false;
    
    // Extended System and Monte Carlo
    private double pH;
    private final ExtendedSystem esvSystem;
    private double targetTemperature = 298.15;
    private final Random rng = new Random();
    private int movesAccepted;
    private int snapshotIndex = 0;
    
    // Molecular Dynamics parameters
    private final double dt;
    private final double printInterval;
    private final double saveInterval;
    private final boolean initVelocities;
    private final String fileType;
    private final double writeRestartInterval;
    private final File dynLoader;
    
    // Advanced Options
    private final Mode mode = prop(Mode.class, "cphmd-mode", Mode.USE_CURRENT);
    private final MCOverride mcOverride = prop(MCOverride.class, "cphmd-override", MCOverride.NONE);
    private final Snapshots snapshotType = prop(Snapshots.class, "cphmd-snapshotType", Snapshots.NONE);
    private final Histidine histidineMode = prop(Histidine.class, "cphmd-histidineMode", Histidine.HIE_ONLY);
    private static int debugLogLevel = prop("cphmd-debugLog", 0);
    private final OptionalDouble referenceOverride = prop("cphmd-referenceOverride", null);
    private final double tempMonitor = prop("cphmd-tempMonitor", 6000.0);
    private final boolean logTimings = prop("cphmd-logTimings");
    private final boolean titrateTermini = prop("cphmd-termini");
    private final boolean zeroReferences = prop("cphmd-zeroReferences");
    private final int snapshotVersioning = prop("cphmd-snapshotVers", 0);
    
    /**
     * Construct a "Discrete-Continuous" Monte-Carlo titration engine.
     * For traditional discrete titration, use Protonate.
     * For traditional continuous titration, run mdesv for populations.
     * 
     * @param mola the molecular assembly
     * @param mcStepFrequency number of MD steps between switch attempts
     * @param pH the simulation pH
     * @param thermostat the MD thermostat
     */
    // java.lang.Double, java.lang.Integer, java.lang.Integer, java.lang.Boolean, java.lang.String, java.lang.Integer, null)
    DiscountPh(MolecularAssembly mola, MolecularDynamics molDyn, Mode mode, 
            double timeStep, double printInterval, double saveInterval, 
            boolean initVelocities, String fileType, double writeRestartInterval, File dyn) {
        this.mola = mola;
        this.molDyn = molDyn;
        this.dt = timeStep;
        this.printInterval = printInterval;
        this.saveInterval = saveInterval;
        this.initVelocities = initVelocities;
        this.fileType = fileType;
        this.writeRestartInterval = writeRestartInterval;
        this.dynLoader = dyn;
        
        this.ff = mola.getForceField();
        this.ffe = mola.getPotentialEnergy();
        this.esvSystem = new ExtendedSystem(mola, pH);
        this.originalFilename = FilenameUtils.removeExtension(mola.getFile().getAbsolutePath()) + "_dyn.pdb";
        SystemFilter.setVersioning(SystemFilter.Versioning.PREFIX_ABSOLUTE);
        
        // Print system props.
        logger.info(" Advanced option flags:");
        System.getProperties().keySet().stream()
                .filter(k -> {
                    String key = k.toString().toLowerCase();
                    return key.startsWith("cphmd") || key.startsWith("md");
                })
                .forEach(key -> logger.info(format(" #%s=%s\n", 
                        key.toString(), System.getProperty(key.toString()))));
        
        // Set the rotamer library in case we do rotamer MC moves.
        RotamerLibrary.setLibrary(RotamerLibrary.ProteinLibrary.Richardson);
        RotamerLibrary.setUseOrigCoordsRotamer(false);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(" Running DISCOuNT-pH dynamics @ system pH %.2f\n", pH));
        logger.info(sb.toString());

        ffe.reInit();
    }
    
    /**dynamic(totalSteps, pH, temperature, titrationFrequency, titrationDuration, rotamerMoveRatio);
     * Prepend an MD object and totalSteps to the arguments for MD's dynamic().
     * (java.lang.Integer, java.lang.Integer, java.lang.Double, java.lang.Double, java.lang.Double, java.lang.Double, java.lang.Boolean, java.lang.String, java.lang.Double)
     */
    public void dynamic(int totalSteps, double pH, double temperature, 
            int titrationFrequency, int titrationDuration, int rotamerMoveRatio) {
        movesAccepted = 0;
        molDyn.dynamic(titrationFrequency, dt, printInterval, saveInterval, 
                temperature, initVelocities, fileType, writeRestartInterval, dynLoader);
        int stepsTaken = titrationFrequency;
        
        while (stepsTaken < totalSteps) {
            tryContinuousTitrationMove(titrationDuration, temperature);
            if (stepsTaken + titrationFrequency < totalSteps) {
                logger.info(format(" Re-launching DISCOuNT-pH MD for %d steps.", titrationFrequency));
                molDyn.redynamic(titrationFrequency, temperature);
                stepsTaken += titrationFrequency;
            } else {
                logger.info(format(" Launching final run of DISCOuNT-pH MD for %d steps.", totalSteps - stepsTaken));
                molDyn.redynamic(totalSteps - stepsTaken, temperature);
                stepsTaken = totalSteps;
                break;
            }
        }
        logger.info(format(" DISCOuNT-pH completed %d steps and %d moves, of which %d were accepted.", 
                totalSteps, totalSteps / titrationFrequency, movesAccepted));
    }
    
    private Stream<Residue> parallelResidueStream(MolecularAssembly mola) {
        return Arrays.asList(mola.getChains()).parallelStream()
                .flatMap(poly -> ((Polymer) poly).getResidues().parallelStream());
    }
    
    /**
     * Identify all titratable residues.
     */
    private List<Residue> findTitrations() {
        List<Residue> chosen = new ArrayList<>();
        parallelResidueStream(mola)
                .filter(res -> mapTitrations(res).size() > 0)
                .forEach(chosen::add);
        return chosen;
    }

    /**
     * Choose titratables with intrinsic pKa inside (pH-window,pH+window).
     *
     * @param pH
     * @param window
     */
    private List<Residue> findTitrations(double pH, double window) {
        List<Residue> chosen = new ArrayList<>();
        parallelResidueStream(mola)
            .filter(res -> mapTitrations(res).parallelStream()
                .anyMatch(titr -> (titr.pKa >= pH - window && titr.pKa <= pH + window)))
                .forEach(chosen::add);
        return chosen;
    }
    
    private List<ExtendedVariable> createESVs(List<Residue> chosen) {
        for (Residue res : chosen) {
            MultiResidue titr = TitrationUtils.titrationFactory(mola, res);
            TitrationESV esv = new TitrationESV(pH, titr, targetTemperature, 1.0);
            esv.readyup();
            esvSystem.addVariable(esv);
            titratingESVs.add(esv);
        }
        return titratingESVs;
    }
    
    /**
     * Selecting titrating residues by a list of names, i.e. "LYS,TYR,HIS" will get all K/k/Y/y/H/U/D.
     * @param names 
     */
    public List<Residue> findTitrations(String names) {
        List<Residue> chosen = new ArrayList<>();
        String tok[] = names.split(",");
        for (int k = 0; k < tok.length; k++) {
            String name = tok[k];
            AminoAcid3 aa3 = AminoAcid3.valueOf(name);
            Polymer polymers[] = mola.getChains();
            for (int i = 0; i < polymers.length; i++) {
                ArrayList<Residue> residues = polymers[i].getResidues();
                for (int j = 0; j < residues.size(); j++) {
                    Residue res = residues.get(j);
                    List<Titration> avail = mapTitrations(res);
                    for (Titration titration : avail) {
                        AminoAcid3 from = titration.source;
                        AminoAcid3 to = titration.target;
                        if (aa3 == from || aa3 == to) {
                            chosen.add(res);
                        }                        
                    }
                }
            }
        }
        return chosen;
    }

    /**
     * Select titrations by crID, eg: {A4,A12,B7,H199}.
     */
    public List<Residue> findTitrations(ArrayList<String> crIDs) {
        List<Residue> chosen = new ArrayList<>();
        Polymer[] polymers = mola.getChains();
        int n = 0;
        for (String s : crIDs) {
            Character chainID = s.charAt(0);
            int i = Integer.parseInt(s.substring(1));
            for (Polymer p : polymers) {
                if (p.getChainID() == chainID) {
                    List<Residue> rs = p.getResidues();
                    for (Residue r : rs) {
                        if (r.getResidueNumber() == i) {
                            chosen.add(r);
                        }
                    }
                }
            }
        }
        return chosen;
    }

    /**
     * Select one titration by chain and residue ID.
     */
    public List<Residue> findTitrations(char chain, int resID) {
        List<Residue> chosen = new ArrayList<>();
        Polymer polymers[] = mola.getChains();
        for (Polymer polymer : polymers) {
            if (polymer.getChainID() == chain) {
                ArrayList<Residue> residues = polymer.getResidues();
                for (Residue residue : residues) {
                    if (residue.getResidueNumber() == resID) {
                        chosen.add(residue);
                    }
                }
            }
        }
        return chosen;
    }

    /**
     * Must be called after all titratable residues have been chosen, but before
     * beginning MD.
     */
    public void readyup() {
        // Create MultiTerminus objects to wrap termini.
        if (titrateTermini) {
            for (Residue res : mola.getResidueList()) {
                if (res.getPreviousResidue() == null || res.getNextResidue() == null) {
                    MultiTerminus multiTerminus = new MultiTerminus(res, ff, ffe, mola);
                    Polymer polymer = findResiduePolymer(res, mola);
                    polymer.addMultiTerminus(res, multiTerminus);
                    ffe.reInit();
                    titratingTermini.add(multiTerminus);
                    logger.info(String.format(" Titrating: %s", multiTerminus));
                }
            }
        }
        // Create containers (MR or ESV) for titratables.
        for (Residue res : chosenResidues) {
            // Then some form of DISCOUNT.
            double dt = (System.getProperty("cphmd-dt") == null) ? 1.0 
                    : Double.parseDouble(System.getProperty("cphmd-dt"));
            TitrationESV esv = new TitrationESV(pH, TitrationUtils.titrationFactory(mola, res), dt);
            esv.readyup();
            titratingESVs.add(esv);
            titratingMultiResidues.add(esv.getMultiRes());
        }
        
        // Hook everything up to the ExtendedSystem.
        titratingESVs.forEach(esvSystem::addVariable);
        mola.getPotentialEnergy().attachExtendedSystem(esvSystem);
        
        finalized = true;
    }

    /**
     * Recursively maps Titration events and adds target Residues to a
     * MultiResidue object.
     *
     * @param member
     * @param multiRes
     */
    private void recursiveMap(Residue member, MultiResidue multiRes) {
        if (finalized) {
            logger.severe("Programming error: improper function call.");
        }
        // Map titrations for this member.
        List<Titration> titrs = mapTitrations(member);

        // For each titration, check whether it needs added as a MultiResidue option.
        for (Titration titr : titrs) {
            // Allow manual override of Histidine treatment.
            if ((titr.target == AminoAcid3.HID && histidineMode == Histidine.HIE_ONLY)
                    || (titr.target == AminoAcid3.HIE && histidineMode == Histidine.HID_ONLY)) {
                continue;
            }
            // Find all the choices currently available to this MultiResidue.
            List<String> choices = new ArrayList<>();
            for (Residue choice : multiRes.getConsideredResidues()) {
                choices.add(choice.getName());
            }
            // If this Titration target is not a choice for the MultiResidue, then add it.
            String targetName = titr.target.toString();
            if (!choices.contains(targetName)) {
                int resNumber = member.getResidueNumber();
                ResidueType resType = member.getResidueType();
                Residue newChoice = new Residue(targetName, resNumber, resType);
                multiRes.addResidue(newChoice);
                // Recursively call this method on each added choice.
                recursiveMap(newChoice, multiRes);
            }
        }
    }

    /**
     * Maps available Titration enums to a given Residue; used to fill the
     * titrationMap field.
     *
     * @param res
     * @param store add identified Titrations to the HashMap
     * @return list of Titrations available for the given residue
     */
    private List<Titration> mapTitrations(Residue res) {
        if (finalized) {
            logger.severe("Programming error: improper function call.");
        }
        AminoAcid3 source = AminoAcid3.valueOf(res.getName());
        List<Titration> avail = new ArrayList<>();
        for (Titration titr : Titration.values()) {
            // Allow manual override of Histidine treatment.
            if ((titr.target == AminoAcid3.HID && histidineMode == Histidine.HIE_ONLY)
                    || (titr.target == AminoAcid3.HIE && histidineMode == Histidine.HID_ONLY)) {
                continue;
            }
            if (titr.source == source) {
                avail.add(titr);
            }
        }
        return avail;
    }
    
    /**
     * Provides titration info as a utility.
     */
    public static List<Titration> titrationLookup(Residue res) {
        AminoAcid3 source = AminoAcid3.valueOf(res.getName());
        List<Titration> avail = new ArrayList<>();
        for (Titration titr : Titration.values()) {
            if (titr.source.equals(source)) {   // relies on the weird dual-direction enum
                avail.add(titr);
            }
        }
        return avail;
    }
    
/*
    private void meltdown() {
        writeSnapshot(".meltdown-");
        ffe.energy(false, true);
        List<BondedTerm> problems = new ArrayList<>();
        esvSystem.getESVList().parallelStream().forEach(esv -> {
            esv.getAtoms().parallelStream()
                .flatMap(atom -> {
                    true
                });
        });
        mola.getChildList(BondedTerm.class,problems).parallelStream()
                .filter(term -> {
                    ((BondedTerm) term).getAtomList().parallelStream().anyMatch(atom -> {
                        esvSystem.getESVList().parallelStream().filter(esv -> {
                            if (esv.containsAtom((Atom) atom)) {
                                return true;
                            } else {
                                return false;
                            }
                        });
                    });
                })
                .forEach(term -> {
                    try { ((Bond) term).log(); } catch (Exception ex) {}
                    try { ((Angle) term).log(); } catch (Exception ex) {}
                    try { ((Torsion) term).log(); } catch (Exception ex) {}
                });
        if (ffe.getVanDerWaalsEnergy() > 1000) {
            for (ExtendedVariable esv1 : esvSystem.getESVList()) {
                for (Atom a1 : esv1.getAtoms()) {
                    for (ExtendedVariable esv2 : esvSystem.getESVList()) {
                        for (Atom a2 : esv2.getAtoms()) {
                        if (a1 == a2 || a1.getBond(a2) != null) {
                            continue;
                        }
                        double dist = FastMath.sqrt(
                                FastMath.pow((a1.getX()-a2.getX()),2) +
                                FastMath.pow((a1.getY()-a2.getY()),2) +
                                FastMath.pow((a1.getZ()-a2.getZ()),2));
                        if (dist < 0.8*(a1.getVDWR() + a2.getVDWR())) {
                            logger.warning(String.format("Close vdW contact for atoms: \n   %s\n   %s", a1, a2));
                        }
                        }
                    }
                }
            }
        }
        logger.severe(String.format("Temperature above critical threshold: %f", thermostat.getCurrentTemperature()));
    }   */
    
    private double currentTemp() {
        return molDyn.getThermostat().getCurrentTemperature();
    }
    
    /**
     * Run continuous titration MD in implicit solvent as a Monte Carlo move.
     */
    private boolean tryContinuousTitrationMove(int titrationDuration, double targetTemperature) {
        startTime = System.nanoTime();
        if (!finalized) {
            logger.severe("Monte-Carlo protonation engine was not finalized!");
        }
        if (currentTemp() > tempMonitor) {
            throw new SystemTemperatureException();
//            meltdown();
        }
        propagateInactiveResidues(titratingMultiResidues);

        // If rotamer moves have been requested, do that first (separately).
        if (mode == Mode.RANDOM) {  // TODO Mode.RANDOM -> Two Modes
            int random = rng.nextInt(titratingMultiResidues.size());
            MultiResidue targetMulti = titratingMultiResidues.get(random);

            // Check whether rotamer moves are possible for the selected residue.
            Residue targetMultiActive = targetMulti.getActive();
            Rotamer[] targetMultiRotamers = targetMultiActive.getRotamers();
            if (targetMultiRotamers != null && targetMultiRotamers.length > 1) {
                // forceFieldEnergy.checkAtoms();
                // boolean accepted = tryRotamerStep(targetMulti);
                boolean accepted = tryCbmcMove(targetMulti);
                snapshotIndex++;
            }
        }   // end of rotamer step

        discountLogger = new StringBuilder();
        discountLogger.append(format(" Move duration (%d steps): \n", titrationDuration));

        // Save the current state of the molecularAssembly. Specifically,
        //      Atom coordinates and MultiResidue states : AssemblyState
        //      Position, Velocity, Acceleration, etc    : DynamicsState
        AssemblyState assemblyState = new AssemblyState(mola);
//            DynamicsState dynamicsState = new DynamicsState();
        molDyn.storeState();
        writeSnapshot(".pre-store");
        
        // Assign starting titration lambdas.
        if (mode == Mode.HALF_LAMBDA) {
            discountLogger.append(format("   Setting all ESVs to one-half...\n"));
            for (ExtendedVariable esv : esvSystem.getESVList()) {
                esv.setLambda(0.5);
            }
        } else if (mode == Mode.RANDOM) {
            discountLogger.append(format("   Setting all ESVs to [random]...\n"));
            for (ExtendedVariable esv : esvSystem.getESVList()) {
                esv.setLambda(rng.nextDouble());
            }
        } else {
            // Intentionally empty.
            // This is the preferred strategy: use existing values for lambda.
        }

        /*
         * (1) Take current energy for criterion.
         * (2) Hook the ExtendedSystem up to MolecularDynamics.
         * (3) Terminate the thread currently running MolDyn... if possible.
         *          (if this execution dies, try chopping up nSteps from the setup)
         * (3) Run these (now continuous-titration) dynamics for discountSteps steps, 
         *          WITHOUT callbacks to mcUpdate().
         * (4) Round continuous titratables to zero/unity.
         * (5) Take energy and test criterion.
         */

        final double eo = currentTotalEnergy();
        discountLogger.append(format("    Attaching extended system to molecular dynamics.\n"));
        logger.info(discountLogger.toString());
        ffe.attachExtendedSystem(esvSystem);
        molDyn.attachExtendedSystem(esvSystem);
        molDyn.setNotifyMonteCarlo(false);
        
        logger.info(format(" Trying continuous titration move:"));
        logger.info(format("   Starting energy: %10.4g", eo));        
        molDyn.redynamic(titrationDuration, targetTemperature);
        logger.info(" Move finished; detaching extended system.");
        molDyn.detachExtendedSystem();
        ffe.detachExtendedSystem();
        final double en = currentTotalEnergy();
        final double dGmc = en - eo;
                
        final double temperature = molDyn.getThermostat().getCurrentTemperature();
        double kT = ThermoConstants.BOLTZMANN * temperature;
        final double crit = exp(-dGmc / kT);
        final double rand = rng.nextDouble();
        logger.info(format("   Final energy:    %10.4g", en));
        logger.info(format("   dG_mc,crit,rng:  %10.4g, %.4f, %.4f", dGmc, crit, rand));
        long took = System.nanoTime() - startTime;
        if (dGmc <= crit) {
            logger.info(" Move accepted!");
            writeSnapshot(".post-acc");
            movesAccepted++;
            return true;
        } else {
            logger.info(" Move denied; reverting state.");
            writeSnapshot(".post-deny");
            assemblyState.revertState();
            molDyn.revertState();
            ffe.reInit();
            writeSnapshot(".post-revert");
            return false;
        }
    }
    
    private void log() {
        logger.info(discountLogger.toString());
        discountLogger = new StringBuilder();
    }
    
    private boolean tryCbmcMove(MultiResidue targetMulti) {
        List<Residue> targets = new ArrayList<>();
        targets.add(targetMulti.getActive());
        int trialSetSize = 5;   // cost still scales with this, unfortunately
        int mcFrequency = 1;    // irrelevant for manual step call
        boolean writeSnapshots = false;
        System.setProperty("cbmc-type", "CHEAP");
        RosenbluthCBMC cbmc = new RosenbluthCBMC(mola, mola.getPotentialEnergy(), null,
            targets, mcFrequency, trialSetSize, writeSnapshots);
        boolean accepted = cbmc.cbmcStep();
        if (logTimings) {
            long took = System.nanoTime() - startTime;
            logger.info(String.format(" CBMC time: %1.3f", took * NS_TO_SEC));
        }
        return accepted;
    }

    /**
     * Attempt a rotamer MC move.
     *
     * @param targetMulti
     * @return accept/reject
     */
    private boolean tryRotamerMove(MultiResidue targetMulti) {
        // Record the pre-change total energy.
        double previousTotalEnergy = currentTotalEnergy();

        // Write the before-step snapshot.
//        writeSnapshot(true, StepType.ROTAMER, snapshotsType);

        // Save coordinates so we can return to them if move is rejected.
        Residue residue = targetMulti.getActive();
        ArrayList<Atom> atoms = residue.getAtomList();
        ResidueState origState = residue.storeState();
        double chi[] = new double[4];
        RotamerLibrary.measureAARotamer(residue, chi, false);
        AminoAcid3 aa = AminoAcid3.valueOf(residue.getName());
        Rotamer origCoordsRotamer = new Rotamer(aa, origState, chi[0], 0, chi[1], 0, chi[2], 0, chi[3], 0);
        // Select a new rotamer and swap to it.
        //Rotamer rotamers[] = residue.getRotamers();
        Rotamer[] rotamers = residue.getRotamers();
        int rotaRand = rng.nextInt(rotamers.length);
        RotamerLibrary.applyRotamer(residue, rotamers[rotaRand]);

        // Write the post-rotamer change snapshot.
//        writeSnapshot(false, StepType.ROTAMER, snapshotsType);

        // Check the MC criterion.
        double temperature = currentTemp();
        double kT = ThermoConstants.BOLTZMANN * temperature;
        double postTotalEnergy = currentTotalEnergy();
        double dG_tot = postTotalEnergy - previousTotalEnergy;
        double criterion = exp(-dG_tot / kT);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(" Assessing possible MC rotamer step:\n"));
        sb.append(String.format("     prev:   %16.8f\n", previousTotalEnergy));
        sb.append(String.format("     post:   %16.8f\n", postTotalEnergy));
        sb.append(String.format("     dG_tot: %16.8f\n", dG_tot));
        sb.append(String.format("     -----\n"));

        // Automatic acceptance if energy change is favorable.
        if (dG_tot < 0) {
            sb.append(String.format("     Accepted!"));
            logger.info(sb.toString());
            propagateInactiveResidues(titratingMultiResidues);
            return true;
        } else {
            // Conditional acceptance if energy change is positive.
            double metropolis = random();
            sb.append(String.format("     criterion:  %9.4f\n", criterion));
            sb.append(String.format("     rng:        %9.4f\n", metropolis));
            if (metropolis < criterion) {
                sb.append(String.format("     Accepted!"));
                logger.info(sb.toString());
                propagateInactiveResidues(titratingMultiResidues);
                return true;
            } else {
                // Move was denied.
                sb.append(String.format("     Denied."));
                logger.info(sb.toString());

                // Undo the rejected move.
                RotamerLibrary.applyRotamer(residue, origCoordsRotamer);
                return false;
            }
        }
    }

    /**
     * Perform the requested titration on the given MultiResidue and
     * reinitialize the FF.
     *
     * @param multiRes
     * @param titration
     */
    private void performTitration(MultiResidue multiRes, Titration titration) {
        if (titration.source != AminoAcid3.valueOf(multiRes.getActive().getName())) {
            logger.severe(String.format("Requested titration source didn't match target MultiResidue: %s", multiRes.toString()));
        }

        List<Atom> oldAtoms = multiRes.getActive().getAtomList();
        boolean success = multiRes.requestSetActiveResidue(titration.target);
        if (!success) {
            logger.severe(String.format("Couldn't perform requested titration for MultiRes: %s", multiRes.toString()));
        }
        List<Atom> newAtoms = multiRes.getActive().getAtomList();

        // identify which atoms were actually inserted/removed
        List<Atom> removedAtoms = new ArrayList<>();
        List<Atom> insertedAtoms = new ArrayList<>();
        for (Atom oldAtom : oldAtoms) {
            boolean found = false;
            for (Atom newAtom : newAtoms) {
                if (newAtom == oldAtom || newAtom.toNameNumberString().equals(oldAtom.toNameNumberString())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                removedAtoms.add(oldAtom);
            }
        }
        for (Atom newAtom : newAtoms) {
            boolean found = false;
            for (Atom oldAtom : oldAtoms) {
                if (newAtom == oldAtom || newAtom.toNameNumberString().equals(oldAtom.toNameNumberString())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                insertedAtoms.add(newAtom);
            }
        }
        if (insertedAtoms.size() + removedAtoms.size() > 1) {
            logger.warning("Protonate: removed + inserted atom count > 1.");
        }

        ffe.reInit();
        molDyn.reInit();

        StringBuilder sb = new StringBuilder();
        sb.append("Active:\n");
        for (Atom a : multiRes.getActive().getAtomList()) {
            sb.append(String.format("  %s\n", a));
        }
        sb.append("Inactive:\n");
        for (Atom a : multiRes.getInactive().get(0).getAtomList()) {
            sb.append(String.format("  %s\n", a));
        }
        debug(1, sb.toString());

        return;
    }

    /**
     * Copies atomic coordinates from each active residue to its inactive
     * counterparts. Assumes that these residues differ by only a hydrogen. If
     * said hydrogen is in an inactive form, its coordinates are updated by
     * geometry with the propagated heavies.
     *
     * @param multiResidues
     */
    private void propagateInactiveResidues(List<MultiResidue> multiResidues) {
        long startTime = System.nanoTime();
//        debug(3, " Begin multiResidue atomic coordinate propagation:");
//        debug(3, String.format(" multiResidues.size() = %d", multiResidues.size()));
        // Propagate all atom coordinates from active residues to their inactive counterparts.
        for (MultiResidue multiRes : multiResidues) {
            Residue active = multiRes.getActive();
//            debug(3, String.format(" active = %s", active.toString()));
            String activeResName = active.getName();
            List<Residue> inactives = multiRes.getInactive();
            for (Atom activeAtom : active.getAtomList()) {
//                debug(3, String.format(" activeAtom = %s", activeAtom.toString()));
                String activeName = activeAtom.getName();
                for (Residue inactive : inactives) {
//                    debug(3, String.format(" inactive = %s", inactive.toString()));
//                    StringBuilder sb = new StringBuilder();
//                    sb.append("    inactiveAtomList: ");
//                    for (Atom test : inactive.getAtomList()) {
//                        sb.append(String.format("%s, ", test.getName()));
//                    }
//                    sb.append("\n");
//                    debug(3, sb.toString());
                    Atom inactiveAtom = (Atom) inactive.getAtomNode(activeName);
//                    debug(3, String.format(" inactiveAtom = %s", inactiveAtom));
                    if (inactiveAtom != null) {
                        debug(4, String.format(" Propagating %s\n          to %s.", activeAtom, inactiveAtom));
                        // Propagate position and gradient.
                        double activeXYZ[] = activeAtom.getXYZ(null);
                        inactiveAtom.setXYZ(activeXYZ);
                        double grad[] = new double[3];
                        activeAtom.getXYZGradient(grad);
                        inactiveAtom.setXYZGradient(grad[0], grad[1], grad[2]);
                        // Propagate velocity, acceleration, and previous acceleration.
                        double activeVelocity[] = new double[3];
                        activeAtom.getVelocity(activeVelocity);
                        inactiveAtom.setVelocity(activeVelocity);
                        double activeAccel[] = new double[3];
                        activeAtom.getAcceleration(activeAccel);
                        inactiveAtom.setAcceleration(activeAccel);
                        double activePrevAcc[] = new double[3];
                        activeAtom.getPreviousAcceleration(activePrevAcc);
                        inactiveAtom.setPreviousAcceleration(activePrevAcc);
                        debug(4, String.format("\n          to %s.", activeAtom, inactiveAtom));
                    } else {
                        if (activeName.equals("C") || activeName.equals("O") || activeName.equals("N") || activeName.equals("CA")
                                || activeName.equals("H") || activeName.equals("HA")) {
                            // Backbone atoms aren't supposed to exist in inactive multiResidue components; so no problem.
                        } else if ((activeResName.equals("LYS") && activeName.equals("HZ3"))
                                || (activeResName.equals("TYR") && activeName.equals("HH"))
                                || (activeResName.equals("CYS") && activeName.equals("HG"))
                                || (activeResName.equals("HIS") && (activeName.equals("HD1") || activeName.equals("HE2")))
                                || (activeResName.equals("HID") && activeName.equals("HD1"))
                                || (activeResName.equals("HIE") && activeName.equals("HE2"))
                                || (activeResName.equals("ASH") && activeName.equals("HD2"))
                                || (activeResName.equals("GLH") && activeName.equals("HE2"))) {
                            // These titratable protons are handled below; so no problem.
                        } else {
                            // Now we have a problem.
                            logger.warning(String.format("Couldn't copy atom_xyz: %s: %s, %s",
                                    multiRes, activeName, activeAtom.toString()));
                        }
                    }
                }
            }
        }

        // If inactive residue is a protonated form, move the stranded hydrogen to new coords (based on propagated heavies).
        // Also give the stranded hydrogen a maxwell velocity and remove its accelerations.
        for (MultiResidue multiRes : multiResidues) {
            Residue active = multiRes.getActive();
            List<Residue> inactives = multiRes.getInactive();
            for (Residue inactive : inactives) {
                Atom resetMe = null;
                switch (inactive.getName()) {
                    case "LYS": {
                        Atom HZ3 = (Atom) inactive.getAtomNode("HZ3");
                        Atom NZ = (Atom) inactive.getAtomNode("NZ");
                        Atom CE = (Atom) inactive.getAtomNode("CE");
                        Atom HZ1 = (Atom) inactive.getAtomNode("HZ1");
                        BondedUtils.intxyz(HZ3, NZ, 1.02, CE, 109.5, HZ1, 109.5, -1);
                        resetMe = HZ3;
                        debug(4, String.format(" Moved 'stranded' hydrogen %s.", HZ3));
                        // Parameters from AminoAcidUtils, line:
                        // Atom HZ3 = buildHydrogen(inactive, "HZ3", NZ, 1.02, CE, 109.5, HZ1, 109.5, -1, k + 9, forceField, null);
                        break;
                    }
                    case "ASH": {
                        Atom HD2 = (Atom) inactive.getAtomNode("HD2");
                        Atom OD2 = (Atom) inactive.getAtomNode("OD2");
                        Atom CG = (Atom) inactive.getAtomNode("CG");
                        Atom OD1 = (Atom) inactive.getAtomNode("OD1");
                        BondedUtils.intxyz(HD2, OD2, 0.98, CG, 108.7, OD1, 0.0, 0);
                        resetMe = HD2;
                        debug(4, String.format(" Moved 'stranded' hydrogen %s.", HD2));
                        // Parameters from AminoAcidUtils, line:
                        // Atom HD2 = buildHydrogen(residue, "HD2", OD2, 0.98, CG, 108.7, OD1, 0.0, 0, k + 5, forceField, bondList);
                        break;
                    }
                    case "GLH": {
                        Atom HE2 = (Atom) inactive.getAtomNode("HE2");
                        Atom OE2 = (Atom) inactive.getAtomNode("OE2");
                        Atom CD = (Atom) inactive.getAtomNode("CD");
                        Atom OE1 = (Atom) inactive.getAtomNode("OE1");
                        BondedUtils.intxyz(HE2, OE2, 0.98, CD, 108.7, OE1, 0.0, 0);
                        resetMe = HE2;
                        debug(4, String.format(" Moved 'stranded' hydrogen %s.", HE2));
                        // Parameters from AminoAcidUtils, line:
                        // Atom HE2 = buildHydrogen(residue, "HE2", OE2, 0.98, CD, 108.7, OE1, 0.0, 0, k + 7, forceField, bondList);
                        break;
                    }
                    case "HIS": {
                        Atom HE2 = (Atom) inactive.getAtomNode("HE2");
                        Atom NE2 = (Atom) inactive.getAtomNode("NE2");
                        Atom CD2 = (Atom) inactive.getAtomNode("CD2");
                        Atom CE1 = (Atom) inactive.getAtomNode("CE1");
                        Atom HD1 = (Atom) inactive.getAtomNode("HD1");
                        Atom ND1 = (Atom) inactive.getAtomNode("ND1");
                        Atom CG = (Atom) inactive.getAtomNode("CG");
                        Atom CB = (Atom) inactive.getAtomNode("CB");
                        BondedUtils.intxyz(HE2, NE2, 1.02, CD2, 126.0, CE1, 126.0, 1);
                        BondedUtils.intxyz(HD1, ND1, 1.02, CG, 126.0, CB, 0.0, 0);
                        // Manual reset since we gotta reset two of 'em.
                        HE2.setXYZGradient(0, 0, 0);
                        HE2.setVelocity(molDyn.getThermostat().maxwellIndividual(HE2.getMass()));
                        HE2.setAcceleration(new double[]{0, 0, 0});
                        HE2.setPreviousAcceleration(new double[]{0, 0, 0});
                        HD1.setXYZGradient(0, 0, 0);
                        HD1.setVelocity(molDyn.getThermostat().maxwellIndividual(HD1.getMass()));
                        HD1.setAcceleration(new double[]{0, 0, 0});
                        HD1.setPreviousAcceleration(new double[]{0, 0, 0});
                        debug(4, String.format(" Moved 'stranded' hydrogen %s.", HE2));
                        debug(4, String.format(" Moved 'stranded' hydrogen %s.", HD1));
                        // Parameters from AminoAcidUtils, line:
                        // Atom HE2 = buildHydrogen(residue, "HE2", NE2, 1.02, CD2, 126.0, CE1, 126.0, 1, k + 10, forceField, bondList);
                        // Atom HD1 = buildHydrogen(residue, "HD1", ND1, 1.02, CG, 126.0, CB, 0.0, 0, k + 4, forceField, bondList);
                        break;
                    }
                    case "HID": {
                        Atom HD1 = (Atom) inactive.getAtomNode("HD1");
                        Atom ND1 = (Atom) inactive.getAtomNode("ND1");
                        Atom CG = (Atom) inactive.getAtomNode("CG");
                        Atom CB = (Atom) inactive.getAtomNode("CB");
                        BondedUtils.intxyz(HD1, ND1, 1.02, CG, 126.0, CB, 0.0, 0);
                        resetMe = HD1;
                        // Parameters from AminoAcidUtils, line:
                        // Atom HD1 = buildHydrogen(residue, "HD1", ND1, 1.02, CG, 126.0, CB, 0.0, 0, k + 4, forceField, bondList);
                        break;
                    }
                    case "HIE": {
                        Atom HE2 = (Atom) inactive.getAtomNode("HE2");
                        Atom NE2 = (Atom) inactive.getAtomNode("NE2");
                        Atom CD2 = (Atom) inactive.getAtomNode("CD2");
                        Atom CE1 = (Atom) inactive.getAtomNode("CE1");
                        BondedUtils.intxyz(HE2, NE2, 1.02, CD2, 126.0, CE1, 126.0, 1);
                        resetMe = HE2;
                        // Parameters from AminoAcidUtils, line:
                        // Atom HE2 = buildHydrogen(residue, "HE2", NE2, 1.02, CD2, 126.0, CE1, 126.0, 1, k + 9, forceField, bondList);
                        break;
                    }
                    case "CYS": {
                        Atom HG = (Atom) inactive.getAtomNode("HG");
                        Atom SG = (Atom) inactive.getAtomNode("SG");
                        Atom CB = (Atom) inactive.getAtomNode("CB");
                        Atom CA = (Atom) inactive.getAtomNode("CA");
                        BondedUtils.intxyz(HG, SG, 1.34, CB, 96.0, CA, 180.0, 0);
                        resetMe = HG;
                        debug(4, String.format(" Moved 'stranded' hydrogen %s.", HG));
                        // Parameters from AminoAcidUtils, line:
                        // Atom HG = buildHydrogen(residue, "HG", SG, 1.34, CB, 96.0, CA, 180.0, 0, k + 3, forceField, bondList);
                        break;
                    }
                    case "TYR": {
                        Atom HH = (Atom) inactive.getAtomNode("HH");
                        Atom OH = (Atom) inactive.getAtomNode("OH");
                        Atom CZ = (Atom) inactive.getAtomNode("CZ");
                        Atom CE2 = (Atom) inactive.getAtomNode("CE2");
                        BondedUtils.intxyz(HH, OH, 0.97, CZ, 108.0, CE2, 0.0, 0);
                        resetMe = HH;
                        debug(4, String.format(" Moved 'stranded' hydrogen %s.", HH));
                        // Parameters from AminoAcidUtils, line:
                        // Atom HH = buildHydrogen(residue, "HH", OH, 0.97, CZ, 108.0, CE2, 0.0, 0, k + 9, forceField, bondList);
                        break;
                    }
                    default:
                }
                if (resetMe != null) {
                    resetMe.setXYZGradient(0, 0, 0);
                    resetMe.setVelocity(molDyn.getThermostat().maxwellIndividual(resetMe.getMass()));
                    resetMe.setAcceleration(new double[]{0, 0, 0});
                    resetMe.setPreviousAcceleration(new double[]{0, 0, 0});
                }
            }
        }

        // Print out atomic comparisons.
        if (debugLogLevel >= 4) {
            for (MultiResidue multiRes : multiResidues) {
                Residue active = multiRes.getActive();
                List<Residue> inactives = multiRes.getInactive();
                for (Atom activeAtom : active.getAtomList()) {
                    for (Residue inactive : inactives) {
                        Atom inactiveAtom = (Atom) inactive.getAtomNode(activeAtom.getName());
                        StringBuilder sb = new StringBuilder();
                        sb.append(String.format(" %s\n %s\n", activeAtom, inactiveAtom));
                        debug(4, sb.toString());
                    }
                }
            }
        }

        long took = System.nanoTime() - startTime;
//        logger.info(String.format(" Propagating inactive residues took: %d ms", (long) (took * 1e-6)));
    }

    /**
     * Locate to which Polymer in the MolecularAssembly a given Residue belongs.
     *
     * @param residue
     *
     * @param molecularAssembly
     *
     * @return the Polymer where the passed Residue is located.
     */
    private Polymer findResiduePolymer(Residue residue,
            MolecularAssembly molecularAssembly) {
        if (residue.getChainID() == null) {
            logger.severe("No chain ID for residue " + residue);
        }
        Polymer polymers[] = molecularAssembly.getChains();
        Polymer location = null;
        for (Polymer p : polymers) {
            if (p.getChainID() == residue.getChainID()) {
                location = p;
            }
        }
        if (location == null) {
            logger.severe("Couldn't find polymer for residue " + residue);
        }
        return location;
    }

    /**
     * Calculates the electrostatic energy at the current state.
     *
     * @return Energy of the current state.
     */
    private double currentElectrostaticEnergy() {
        double x[] = new double[ffe.getNumberOfVariables() * 3];
        ffe.getCoordinates(x);
        ffe.energy(x);
        return ffe.getTotalElectrostaticEnergy();
    }

    /**
     * Calculates the total energy at the current state.
     *
     * @return Energy of the current state.
     */
    private double currentTotalEnergy() {
        double x[] = new double[ffe.getNumberOfVariables() * 3];
        ffe.getCoordinates(x);
        ffe.energy(x);
        return ffe.getTotalEnergy();
    }

    private void writeSnapshot(String extension) {
        String filename = FilenameUtils.removeExtension(originalFilename);
        if (snapshotType == Snapshots.INTERLEAVED) {
            if (filename.contains("_dyn")) {
                filename = filename.replace("_dyn",format("_dyn_%d.pdb",++snapshotIndex));
            } else {
                filename = FilenameUtils.removeExtension(filename) 
                        + format("_dyn_%d.pdb",++snapshotIndex);
            }
        } else {
            if (!extension.startsWith(".")) {
                extension = "." + extension;
            }
            filename = filename + format("_%d",++snapshotIndex) + extension;
        }
        File file = new File(filename);
        PDBFilter writer = new PDBFilter(file, mola, null, null);
        writer.writeFile(file, false);
    }

    /**
     * Enumerated titration reactions for source/target amino acid pairs.
     */
    public enum Titration {

        Ctoc(8.18, -60.168, TitrationType.DEP, AminoAcid3.CYS, AminoAcid3.CYD),
        ctoC(8.18, +60.168, TitrationType.PROT, AminoAcid3.CYD, AminoAcid3.CYS),
        Dtod(3.90, +53.188, TitrationType.PROT, AminoAcid3.ASP, AminoAcid3.ASH),
        dtoD(3.90, -53.188, TitrationType.DEP, AminoAcid3.ASH, AminoAcid3.ASP),
        Etoe(4.25, +59.390, TitrationType.PROT, AminoAcid3.GLU, AminoAcid3.GLH),
        etoE(4.25, -59.390, TitrationType.DEP, AminoAcid3.GLH, AminoAcid3.GLU),
        Ktok(10.53, +50.440, TitrationType.DEP, AminoAcid3.LYS, AminoAcid3.LYD),    // new dG_elec: 48.6928
        ktoK(10.53, -50.440, TitrationType.PROT, AminoAcid3.LYD, AminoAcid3.LYS),
        Ytoy(10.07, -34.961, TitrationType.DEP, AminoAcid3.TYR, AminoAcid3.TYD),
        ytoY(10.07, +34.961, TitrationType.PROT, AminoAcid3.TYD, AminoAcid3.TYR),
        HtoU(6.00, +42.923, TitrationType.DEP, AminoAcid3.HIS, AminoAcid3.HID),
        UtoH(6.00, -42.923, TitrationType.PROT, AminoAcid3.HID, AminoAcid3.HIS),
        HtoZ(6.00, +00.000, TitrationType.DEP, AminoAcid3.HIS, AminoAcid3.HIE),
        ZtoH(6.00, +00.000, TitrationType.PROT, AminoAcid3.HIE, AminoAcid3.HIS),
        
        TerminusNH3toNH2 (8.23, +00.00, TitrationType.DEP, AminoAcid3.UNK, AminoAcid3.UNK),
        TerminusCOOtoCOOH(3.55, +00.00, TitrationType.PROT, AminoAcid3.UNK, AminoAcid3.UNK);

        public final double pKa, refEnergy;
        public final TitrationType type;
        public final AminoAcid3 source, target;

        Titration(double pKa, double refEnergy, TitrationType type,
                AminoAcid3 source, AminoAcid3 target) {
            this.pKa = pKa;
            this.refEnergy = refEnergy;
            this.type = type;
            this.source = source;
            this.target = target;
        }
    }
    
    private enum MCOverride {
        ACCEPT, REJECT, NONE;
    }

    private enum Snapshots {
        NONE, SEPARATE, INTERLEAVED;
    }

    private enum TitrationType {
        PROT, DEP;
    }

    public enum Histidine {
        HID_ONLY, HIE_ONLY, SINGLE, DOUBLE;
    }
    
    /**
     * Discount flavors differ in the way that they seed titration lambdas at the start of a move.
     */
    public enum Mode {
        HALF_LAMBDA, RANDOM, USE_CURRENT;
    }
    
    public boolean prop(String key) {
        return (System.getProperty(key) != null);
    }
    public static <T> T prop(String key, T defaultVal) {
        if (defaultVal instanceof Integer) {
            return (System.getProperty(key) != null) 
                    ? (T) Integer.valueOf(System.getProperty(key)) : defaultVal;
        } else if (defaultVal instanceof Double) {
            return (System.getProperty(key) != null) 
                    ? (T) Double.valueOf(System.getProperty(key)) : defaultVal;
        } else {
            return (System.getProperty(key) != null) 
                    ? (T) OptionalDouble.of(Double.valueOf(System.getProperty(key))) : (T) OptionalDouble.empty();
        }
    }
    public <T extends Enum<T>> T prop(Class<T> type, String key, T def) {
        return (System.getProperty(key) != null) ? T.valueOf(type, System.getProperty(key)) : def;
    }
    
    private static void debug(int level, String message) {
        if (debugLogLevel >= level) {
            logger.info(message);
        }
    }
    
    public class SystemTemperatureException extends RuntimeException {
        // It's getting hot in here.
    }
}