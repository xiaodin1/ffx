/**
 * <p>Title: Force Field X</p>
 * <p>Description: Force Field X is a Molecular Biophysics Environment</p>
 * <p>Copyright: Copyright (c) Michael J. Schnieders 2002-2009</p>
 *
 * @author Michael J. Schnieders
 * @version 0.1
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

package ffx.potential.bonded;

import ffx.potential.bonded.ROLS;
import ffx.potential.bonded.MSNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ffx.potential.bonded.BondedTerm;


/**
 * Unit tests for the MSNode class.
 */
public class MSNodeTest {
	private MSNode dataNode = null;

	@Test(timeout = 500)
	public void MSNode_constructor() {
		String n = "Test";
		assertEquals("MSNode", n, dataNode.getName());
	}

	@Test(timeout = 500)
	public void MSNode_destroy() {
		dataNode.setSelected(true);
		boolean expectedReturn = true;
		boolean actualReturn = dataNode.destroy();
		assertEquals("return value", expectedReturn, actualReturn);
		assertTrue(!dataNode.isSelected());
		assertNull(dataNode.getName());
		assertNull(dataNode.getParent());
		assertNotNull(dataNode.getAtomList());
		assertNotNull(dataNode.getBondList());
		assertNotNull(dataNode
				.getList(BondedTerm.class, new ArrayList<ROLS>()));
		assertNotNull(dataNode.getChildList());
	}

	@Test(timeout = 500)
	public void MSNode_equals() {
		Object object = "";
		boolean expectedReturn = false;
		boolean actualReturn = dataNode.equals(object);
		assertEquals("return value", expectedReturn, actualReturn);
		actualReturn = dataNode.equals(null);
		assertEquals("return value", expectedReturn, actualReturn);
		object = new MSNode("Test");
		expectedReturn = true;
		actualReturn = dataNode.equals(object);
		assertEquals("return value", expectedReturn, actualReturn);
		expectedReturn = true;
		actualReturn = dataNode.equals(dataNode);
		assertEquals("return value", expectedReturn, actualReturn);
	}

	@Before
	public void setUp() {
		dataNode = new MSNode("Test");
	}

	@After
	public void tearDown() {
		dataNode = null;
	}
}