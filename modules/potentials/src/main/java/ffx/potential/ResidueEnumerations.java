/**
 * Title: Force Field X.
 *
 * Description: Force Field X - Software for Molecular Biophysics.
 *
 * Copyright: Copyright (c) Michael J. Schnieders 2001-2013.
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
package ffx.potential;

import java.util.Arrays;
import java.util.List;

/**
 * @author Ava M. Lynn
 */
public class ResidueEnumerations {

    public enum AminoAcid1 {

        G, A, V, L, I, S, T, C, X, c,
        P, F, Y, y, W, H, U, Z, D, d,
        N, E, e, Q, M, K, k, R, O, B,
        J, t, f, a, o, n, m, x
    };

    public enum AminoAcid3 {

        GLY, ALA, VAL, LEU, ILE, SER, THR, CYS, CYX, CYD,
        PRO, PHE, TYR, TYD, TRP, HIS, HID, HIE, ASP, ASH,
        ASN, GLU, GLH, GLN, MET, LYS, LYD, ARG, ORN, AIB,
        PCA, H2N, FOR, ACE, COH, NH2, NME, UNK
    };

    public static final List<AminoAcid3> aminoAcidList = Arrays.asList(AminoAcid3.values());

    public enum NucleicAcid1 {

        A, G, C, U, D, B, I, T, O, W, H, X
    };

    /**
     * Since enumeration values must start with a letter, an 'M' is added to
     * modified bases whose IUPAC name starts with an integer.
     */
    public enum NucleicAcid3 {

        ADE, GUA, CYT, URI, DAD, DGU, DCY, DTY, THY, MP1, DP2, TP3, UNK, M2MG,
        H2U, M2G, OMC, OMG, PSU, M5MC, M7MG, M5MU, M1MA, YYG
    };

    public static final List<NucleicAcid3> nucleicAcidList = Arrays.asList(NucleicAcid3.values());

    public static final int aminoAcidHeavyAtoms[] = {
        4, 5, 7, 8, 8, 6, 7, 6, 6, 6,
        7, 11, 12, 12, 14, 10, 10, 10, 8, 8,
        8, 9, 9, 9, 8, 9, 9, 11, 8, 6,
        8, 0, 0, 0, 0, 0, 0, 0
    };
}
