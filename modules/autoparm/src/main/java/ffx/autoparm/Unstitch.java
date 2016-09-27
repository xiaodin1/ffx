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
package ffx.autoparm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import ffx.autoparm.fragment.ExhaustiveFragmenter;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.isomorphism.VentoFoggia;
import org.openscience.cdk.modeling.builder3d.ModelBuilder3D;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.AtomTypeManipulator;

/**
 * Splits large molecules into fragments for PolType Maps fragments to full
 * molecule
 *
 * Input: full molecule SDF Output: individual fragment SDFs
 *
 * @author Rae Ann Corrigan
 */
public class Unstitch {

    protected String sdffile;
    protected String ciffile;
    protected String smi;

    private final static Logger logger = Logger.getLogger(Unstitch.class.getName());

    public Unstitch(String sdffile, String ciffile, String smi) {
        this.sdffile = sdffile;
        this.ciffile = ciffile;
        this.smi = smi;
    }

    private static final int SIZE = 30;
    ArrayList<String> uniqueAtomNames = new ArrayList<>();

    //reads in full molecule CIF
    public void readCIF() throws FileNotFoundException, IOException {

        try {
            BufferedReader cread = new BufferedReader(new FileReader(ciffile));
            String line;

            while ((line = cread.readLine()) != null) {
                //test to see if the line read in contains unique atom name info.
                //if there is a space at indice 3 and it's the correct length
                if (line.startsWith(" ", 3) && (line.length() > 50) && line.length() < 100) {

                    String str4 = Character.toString(line.charAt(4));
                    String str5 = Character.toString(line.charAt(5));
                    String str6 = Character.toString(line.charAt(6));
                    String str7 = Character.toString(line.charAt(7));

                    String start = str4.concat(str5);
                    String atomName = start.concat(str6);

                    if (line.charAt(7) != ' ') {
                        atomName = atomName.concat(str7);
                    }

                    System.out.println("Atom Name: " + atomName);
                    uniqueAtomNames.add(atomName);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    } //end "readCIF" cif reader

    //reads in full molecule SDF
    public void readSDF() throws FileNotFoundException, IOException {
        File file = new File(sdffile);
        BufferedReader read = null;

        try {
            FileReader fileReader = new FileReader(file);
            read = new BufferedReader(fileReader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        IteratingSDFReader reader = null;

        try {

            reader = new IteratingSDFReader(read, SilentChemObjectBuilder.getInstance());
            while (reader.hasNext()) {

                IAtomContainer molecule = reader.next();

                try {
                    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
                    CDKHydrogenAdder.getInstance(SilentChemObjectBuilder.getInstance()).addImplicitHydrogens(molecule);

                    //IAtom IDing should go here
                    //setProperty? setID?
                    for (int i = 0; i < molecule.getAtomCount(); i++) {
                        IAtom test = molecule.getAtom(i);
                        test.setID(uniqueAtomNames.get(i));
                    }

                    //test to see if setID worked
                    for (int j = 0; j < molecule.getAtomCount(); j++) {
                        IAtom test2 = molecule.getAtom(j);
                       // System.out.println("atomType: " + test2.getAtomTypeName());
                       // System.out.println("test2ID: " + test2.getID());
                    }

                    //Fragmentation call
                    fragment(molecule);

                } catch (Exception x) {
                    System.err.println("*");
                    System.out.println(x);
                }
            }
        } catch (Exception x) {
            x.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (Exception x) {
            }
        }
    } // end "readSDF" sdf reader

    String[] eArray = new String[]{"For Exh Fragments"};
    String[] eatArray = new String[]{"For testing for eaten fragments"};
    String[] rhArray = new String[]{"For removed-hydrogen SMILES"};
    int numSubstructures = 0;
    List<String> toRemove = new ArrayList<>();
    List<IAtomContainer> rhList = new ArrayList<>();
    List<Integer> indicelist = new ArrayList<>();
    List<String> finallist = new ArrayList<>();
    int[][] map = null;
    int[][] mapfinal = null;

    //fragments full molecule according to exhaustive fragmentation algorithm
    //exhaustive fragments used in further functions
    protected void fragment(IAtomContainer molecule) throws Exception {

        //ExhaustiveFragmenter implimentation
        ExhaustiveFragmenter exh = new ExhaustiveFragmenter();
        exh.setMinimumFragmentSize(20);
        exh.generateFragments(molecule);
        System.out.println("\nEXHAUSTIVE FRAGMENTS");
        eArray = exh.getFragments();
        eatArray = exh.getFragments();
        rhArray = exh.getFragments();
        int orig = eatArray.length;

        System.out.println(Arrays.toString(eArray) + "\n");

        //checking for "eaten" fragments
        //remove hydrogens for more accurate substructure checking
        for (String rhArray1 : rhArray) {
            //convert each array entry (SMILES) to IAtomContainer
            IAtomContainer molec = null;
            try {
                SmilesParser smp = new SmilesParser(SilentChemObjectBuilder.getInstance());
                molec = smp.parseSmiles(rhArray1);
            } catch (InvalidSmilesException ise) {
                System.out.println(ise.toString());
            }
            //remove hydrogens using CDK AtomContainerManipulator.removeHydrogens(IAtomContainer)
            try {
                AtomContainerManipulator.removeHydrogens(molec);
            } catch (Exception e) {
                e.printStackTrace();
            }
            rhList.add(molec);
        }

        //check for substructures and collect indicies to take out entires from full-H array
        for (int t = 0; t < rhList.size(); t++) {
            IAtomContainer query = rhList.get(t);
            Pattern pattern = VentoFoggia.findSubstructure(query);

            for (int u = 0; u < rhList.size(); u++) {
                IAtomContainer tester = rhList.get(u);

                //is "Query" is a substructure of "Tester"
                //makes sure query and tester aren't the same molecule and that
                //     query is smaller than tester (substructures have to be
                //     smaller than the main structure)
                if (pattern.matches(tester) && (tester != query) && (tester.getAtomCount() > query.getAtomCount()) && (tester.getAtomCount() < SIZE)) {
                    indicelist.add(t);
                }
            }
        }

        for (int v = 0; v < rhList.size(); v++) {
            if (!indicelist.contains(v)) {
                finallist.add(eatArray[v]);
            } else {
                numSubstructures++;
            }
        }

        //Make final list of non-substructures into an array to pass on
        String[] finalArray = new String[finallist.size()];

        for (int n = 0; n < finallist.size(); n++) {
            finalArray[n] = finallist.get(n);
        }

        System.out.print("Substructures removed: ");
        System.out.println(numSubstructures);

        System.out.println("Orig length: " + orig);
        System.out.println("Final length: " + finalArray.length + "\n");

        String full = smi;
        System.out.println("fullSmiles: " + full + "\n");

        //write SMILES file
        //pass eArray to smilesToObject to convert Exhaustive fragments to SDF
        //pass finalArray to smilesToObject to convert non-substructure fragments to SDF
        smilesToObject(finalArray, full);

    } //end "fragment" fragmenter

    //Convert SMILES strings to an object to be passed on to converter
    protected void smilesToObject(String[] smilesArr, String fullsmi) throws Exception {

        String fullsm = fullsmi;
        for (int i = 0; i < smilesArr.length; i++) {
            String content = smilesArr[i];
            //entry number in SMILES array to be used later in SDF writing
            int num = i;
            iAtomContainerTo3DModel(content, num, fullsm);
        }

    } //end "smilesToObject" converter

    int fragcounter = 1;

    protected void iAtomContainerTo3DModel(String smi, int num, String fullsmi) throws Exception {
        IAtomContainer mol = null;
        IAtomContainer full = null;
        List<IAtom> toFullTest = new ArrayList<>();
        List<IAtom> toFragTest = new ArrayList<>();
        //entry number in SMILES array to be used later in SDF writing
        int number = num;

        //Parse SMILES for full drug molecule
        try {
            SmilesParser fullp = new SmilesParser(SilentChemObjectBuilder.getInstance());
            full = fullp.parseSmiles(fullsmi);
        } catch (InvalidSmilesException ise) {
            System.out.println(ise.toString());
        }

        //Parse SMILES for fragment
        try {
            SmilesParser sp = new SmilesParser(SilentChemObjectBuilder.getInstance());
            mol = sp.parseSmiles(smi);
        } catch (InvalidSmilesException ise) {
            System.out.println(ise.toString());
        }

        //AtomTypeMatcher for full drug molecule
        try {
            CDKAtomTypeMatcher match = CDKAtomTypeMatcher.getInstance(full.getBuilder());
            for (IAtom fatom : full.atoms()) {
                IAtomType ftype = match.findMatchingAtomType(full, fatom);
                AtomTypeManipulator.configure(fatom, ftype);
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        //AtomTypeMatcher for frag
        try {
            CDKAtomTypeMatcher matcher = CDKAtomTypeMatcher.getInstance(mol.getBuilder());
            for (IAtom atom : mol.atoms()) {
                IAtomType type = matcher.findMatchingAtomType(mol, atom);
                AtomTypeManipulator.configure(atom, type);
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        //CDKHydrogenAdder for full drug molecule
        try {
            CDKHydrogenAdder fha = CDKHydrogenAdder.getInstance(full.getBuilder());
            fha.addImplicitHydrogens(full);
            AtomContainerManipulator.convertImplicitToExplicitHydrogens(full);
        } catch (CDKException e) {
            System.out.println(e);
        }

        //CDKHydrogenAdder for fragment
        try {
            CDKHydrogenAdder ha = CDKHydrogenAdder.getInstance(mol.getBuilder());
            ha.addImplicitHydrogens(mol);
            AtomContainerManipulator.convertImplicitToExplicitHydrogens(mol);
        } catch (CDKException e) {
            System.out.println(e);
        }

        //Builds 3D model of fragment molecule
        ModelBuilder3D mb3d;
        mb3d = ModelBuilder3D.getInstance(SilentChemObjectBuilder.getInstance());
        IAtomContainer molecule = null;
        molecule = mb3d.generate3DCoordinates(mol, false);

        //Fragmenting checks
        //30 atoms or less
        if (molecule.getAtomCount() < SIZE) {

            //"eaten" fragments checked for already
            //writeSDF
            File fragsdf = writeSDF(molecule, number);
            //writeXYZ(molecule, number);
            int fraglen = mol.getAtomCount();
            int fulllen = full.getAtomCount();

            fragcounter++;
        }

    } //end "iAtomContainerTo3DModel" IAtomContainer to 3D model converter

    protected File writeSDF(IAtomContainer iAtomContainer, int n) throws Exception {

        String fileBegin = "fragment";
        String fileEnd = Integer.toString(n);
        String dirName = fileBegin.concat(fileEnd);

        // Make a subdirectory.
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdir();
        }

        for (int j = 0; j < iAtomContainer.getAtomCount(); j++) {
            IAtom test2 = iAtomContainer.getAtom(j);
            System.out.println("atomType in writeSDF: " + test2.getAtomTypeName());
            System.out.println("test2ID in writeSDF: " + test2.getID());
        }

        String fragName = dirName.concat(File.separator).concat(dirName.concat(".sdf"));
        logger.info(String.format(" Writing %s", fragName));

        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;
        SDFWriter sdfWriter = null;
        File sdfFromSMILES = new File(fragName);

        try {
            fileWriter = new FileWriter(sdfFromSMILES.getAbsoluteFile());
            bufferedWriter = new BufferedWriter(fileWriter);
            sdfWriter = new SDFWriter();
            sdfWriter.setWriter(bufferedWriter);
            sdfWriter.write(iAtomContainer);
            bufferedWriter.close();
        } catch (IOException e) {
            logger.warning(e.toString());
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (sdfWriter != null) {
                sdfWriter.close();
            }
        }

        return sdfFromSMILES;
    }

} //end Fragmenter
