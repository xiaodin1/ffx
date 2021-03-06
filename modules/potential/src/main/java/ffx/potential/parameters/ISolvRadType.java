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
package ffx.potential.parameters;

import java.util.Comparator;
import java.util.logging.Logger;

import ffx.potential.parameters.ForceField.ForceFieldType;

/**
 * The ISolvRadType class defines one implicit solvent radius scaling factor.
 *
 * @author Michael J. Schnieders
 * @since 1.0
 *
 */
public final class ISolvRadType extends BaseType implements Comparator<String> {

    private static final Logger logger = Logger.getLogger(BondType.class.getName());
    /**
     * Atom classes that form this bond stretch.
     */
    private int atomType;
    /**
     * Multiply this by VdW radius to get base Born radius.
     */
    public final double radiusScale;

    public ISolvRadType(int atomType, double radiusScale) {
        super(ForceFieldType.ISOLVRAD, Integer.toString(atomType));
        this.atomType = atomType;
        this.radiusScale = radiusScale;
    }

    /**
     * <p>
     * incrementClasses</p>
     *
     * @param increment a int.
     */
    public void incrementType(int increment) {
        atomType += increment;
        setKey(Integer.toString(atomType));
    }

    /**
     * {@inheritDoc}
     *
     * Nicely formatted bond stretch string.
     */
    @Override
    public String toString() {
        return String.format("isolvrad  %4d  %6.4f", atomType, radiusScale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(String key1, String key2) {
        int type1 = Integer.parseInt(key1);
        int type2 = Integer.parseInt(key2);
        if (type1 < type2) {
            return -1;
        } else if (type1 > type2) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || !(other instanceof ISolvRadType)) {
            return false;
        }
        ISolvRadType otherType = (ISolvRadType) other;
        return (otherType.atomType == this.atomType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.atomType;
        return hash;
    }
}
