/**
 * Title: Force Field X.
 * <p>
 * Description: Force Field X - Software for Molecular Biophysics.
 * <p>
 * Copyright: Copyright (c) Michael J. Schnieders 2001-2018.
 * <p>
 * This file is part of Force Field X.
 * <p>
 * Force Field X is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 * <p>
 * Force Field X is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * Force Field X; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * <p>
 * Linking this library statically or dynamically with other modules is making a
 * combined work based on this library. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * <p>
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
package ffx.potential;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.lang.String.format;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import static org.apache.commons.math3.util.FastMath.abs;
import static org.apache.commons.math3.util.FastMath.sqrt;

import edu.rit.pj.Comm;

import simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaVdwForce_NonbondedMethod;
import simtk.openmm.OpenMMLibrary.OpenMM_Boolean;
import simtk.openmm.OpenMMLibrary.OpenMM_CustomNonbondedForce_NonbondedMethod;
import simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_NonbondedMethod;
import simtk.openmm.OpenMM_Vec3;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_3D_DoubleArray_create;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_3D_DoubleArray_destroy;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_3D_DoubleArray_set;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaAngleForce_addAngle;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaAngleForce_create;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaAngleForce_setAmoebaGlobalAngleCubic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaAngleForce_setAmoebaGlobalAnglePentic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaAngleForce_setAmoebaGlobalAngleQuartic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaAngleForce_setAmoebaGlobalAngleSextic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaBondForce_addBond;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaBondForce_create;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaBondForce_setAmoebaGlobalBondCubic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaBondForce_setAmoebaGlobalBondQuartic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaGeneralizedKirkwoodForce_addParticle;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaGeneralizedKirkwoodForce_create;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaGeneralizedKirkwoodForce_setIncludeCavityTerm;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaGeneralizedKirkwoodForce_setParticleParameters;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaGeneralizedKirkwoodForce_setProbeRadius;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaGeneralizedKirkwoodForce_setSoluteDielectric;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaGeneralizedKirkwoodForce_setSolventDielectric;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaGeneralizedKirkwoodForce_setSurfaceAreaFactor;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaGeneralizedKirkwoodForce_updateParametersInContext;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaInPlaneAngleForce_addAngle;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaInPlaneAngleForce_create;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaInPlaneAngleForce_setAmoebaGlobalInPlaneAngleCubic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaInPlaneAngleForce_setAmoebaGlobalInPlaneAnglePentic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaInPlaneAngleForce_setAmoebaGlobalInPlaneAngleQuartic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaInPlaneAngleForce_setAmoebaGlobalInPlaneAngleSextic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_CovalentType.OpenMM_AmoebaMultipoleForce_Covalent12;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_CovalentType.OpenMM_AmoebaMultipoleForce_Covalent13;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_CovalentType.OpenMM_AmoebaMultipoleForce_Covalent14;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_CovalentType.OpenMM_AmoebaMultipoleForce_Covalent15;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_CovalentType.OpenMM_AmoebaMultipoleForce_PolarizationCovalent11;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_MultipoleAxisTypes.OpenMM_AmoebaMultipoleForce_Bisector;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_MultipoleAxisTypes.OpenMM_AmoebaMultipoleForce_NoAxisType;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_MultipoleAxisTypes.OpenMM_AmoebaMultipoleForce_ThreeFold;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_MultipoleAxisTypes.OpenMM_AmoebaMultipoleForce_ZBisect;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_MultipoleAxisTypes.OpenMM_AmoebaMultipoleForce_ZOnly;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_MultipoleAxisTypes.OpenMM_AmoebaMultipoleForce_ZThenX;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_NonbondedMethod.OpenMM_AmoebaMultipoleForce_NoCutoff;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_NonbondedMethod.OpenMM_AmoebaMultipoleForce_PME;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_PolarizationType.OpenMM_AmoebaMultipoleForce_Direct;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_PolarizationType.OpenMM_AmoebaMultipoleForce_Extrapolated;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_PolarizationType.OpenMM_AmoebaMultipoleForce_Mutual;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_addMultipole;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_create;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_setAEwald;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_setCovalentMap;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_setCutoffDistance;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_setEwaldErrorTolerance;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_setExtrapolationCoefficients;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_setMultipoleParameters;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_setMutualInducedMaxIterations;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_setMutualInducedTargetEpsilon;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_setNonbondedMethod;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_setPmeGridDimensions;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_setPolarizationType;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaMultipoleForce_updateParametersInContext;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaOutOfPlaneBendForce_addOutOfPlaneBend;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaOutOfPlaneBendForce_create;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaOutOfPlaneBendForce_setAmoebaGlobalOutOfPlaneBendCubic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaOutOfPlaneBendForce_setAmoebaGlobalOutOfPlaneBendPentic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaOutOfPlaneBendForce_setAmoebaGlobalOutOfPlaneBendQuartic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaOutOfPlaneBendForce_setAmoebaGlobalOutOfPlaneBendSextic;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaPiTorsionForce_addPiTorsion;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaPiTorsionForce_create;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaStretchBendForce_addStretchBend;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaStretchBendForce_create;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaTorsionTorsionForce_addTorsionTorsion;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaTorsionTorsionForce_create;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaTorsionTorsionForce_setTorsionTorsionGrid;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaVdwForce_addParticle;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaVdwForce_create;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaVdwForce_setCutoffDistance;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaVdwForce_setNonbondedMethod;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaVdwForce_setParticleExclusions;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaVdwForce_setParticleParameters;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaVdwForce_setUseDispersionCorrection;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaVdwForce_updateParametersInContext;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaWcaDispersionForce_addParticle;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaWcaDispersionForce_create;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaWcaDispersionForce_setAwater;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaWcaDispersionForce_setDispoff;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaWcaDispersionForce_setEpsh;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaWcaDispersionForce_setEpso;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaWcaDispersionForce_setParticleParameters;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaWcaDispersionForce_setRminh;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaWcaDispersionForce_setRmino;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaWcaDispersionForce_setShctd;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaWcaDispersionForce_setSlevy;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AmoebaWcaDispersionForce_updateParametersInContext;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_AngstromsPerNm;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_KJPerKcal;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_KcalPerKJ;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_NmPerAngstrom;
import static simtk.openmm.AmoebaOpenMMLibrary.OpenMM_RadiansPerDegree;
import static simtk.openmm.OpenMMLibrary.OpenMM_AndersenThermostat_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_BondArray_append;
import static simtk.openmm.OpenMMLibrary.OpenMM_BondArray_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_BondArray_destroy;
import static simtk.openmm.OpenMMLibrary.OpenMM_Boolean.OpenMM_False;
import static simtk.openmm.OpenMMLibrary.OpenMM_Boolean.OpenMM_True;
import static simtk.openmm.OpenMMLibrary.OpenMM_CMMotionRemover_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_Context_create_2;
import static simtk.openmm.OpenMMLibrary.OpenMM_Context_destroy;
import static simtk.openmm.OpenMMLibrary.OpenMM_Context_getState;
import static simtk.openmm.OpenMMLibrary.OpenMM_Context_setParameter;
import static simtk.openmm.OpenMMLibrary.OpenMM_Context_setPositions;
import static simtk.openmm.OpenMMLibrary.OpenMM_Context_setVelocities;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomExternalForce_addGlobalParameter;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomExternalForce_addParticle;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomExternalForce_addPerParticleParameter;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomExternalForce_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomGBForce_ComputationType.OpenMM_CustomGBForce_ParticlePair;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomGBForce_ComputationType.OpenMM_CustomGBForce_ParticlePairNoExclusions;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomGBForce_ComputationType.OpenMM_CustomGBForce_SingleParticle;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomGBForce_addComputedValue;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomGBForce_addEnergyTerm;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomGBForce_addGlobalParameter;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomGBForce_addParticle;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomGBForce_addPerParticleParameter;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomGBForce_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomGBForce_setCutoffDistance;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomGBForce_setParticleParameters;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomGBForce_updateParametersInContext;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomNonbondedForce_addEnergyParameterDerivative;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomNonbondedForce_addGlobalParameter;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomNonbondedForce_addInteractionGroup;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomNonbondedForce_addParticle;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomNonbondedForce_addPerParticleParameter;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomNonbondedForce_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomNonbondedForce_setCutoffDistance;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomNonbondedForce_setNonbondedMethod;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomNonbondedForce_setSwitchingDistance;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomNonbondedForce_setUseSwitchingFunction;
import static simtk.openmm.OpenMMLibrary.OpenMM_DoubleArray_append;
import static simtk.openmm.OpenMMLibrary.OpenMM_DoubleArray_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_DoubleArray_destroy;
import static simtk.openmm.OpenMMLibrary.OpenMM_DoubleArray_resize;
import static simtk.openmm.OpenMMLibrary.OpenMM_DoubleArray_set;
import static simtk.openmm.OpenMMLibrary.OpenMM_Force_setForceGroup;
import static simtk.openmm.OpenMMLibrary.OpenMM_HarmonicBondForce_addBond;
import static simtk.openmm.OpenMMLibrary.OpenMM_HarmonicBondForce_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_IntArray_append;
import static simtk.openmm.OpenMMLibrary.OpenMM_IntArray_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_IntArray_destroy;
import static simtk.openmm.OpenMMLibrary.OpenMM_IntArray_resize;
import static simtk.openmm.OpenMMLibrary.OpenMM_IntArray_set;
import static simtk.openmm.OpenMMLibrary.OpenMM_IntSet_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_IntSet_destroy;
import static simtk.openmm.OpenMMLibrary.OpenMM_IntSet_insert;
import static simtk.openmm.OpenMMLibrary.OpenMM_Integrator_destroy;
import static simtk.openmm.OpenMMLibrary.OpenMM_LangevinIntegrator_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_addParticle;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_createExceptionsFromBonds;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_getExceptionParameters;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_getNumExceptions;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_getParticleParameters;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_setCutoffDistance;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_setExceptionParameters;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_setNonbondedMethod;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_setPMEParameters;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_setParticleParameters;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_setSwitchingDistance;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_setUseDispersionCorrection;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_setUseSwitchingFunction;
import static simtk.openmm.OpenMMLibrary.OpenMM_NonbondedForce_updateParametersInContext;
import static simtk.openmm.OpenMMLibrary.OpenMM_ParameterArray_get;
import static simtk.openmm.OpenMMLibrary.OpenMM_ParameterArray_getSize;
import static simtk.openmm.OpenMMLibrary.OpenMM_PeriodicTorsionForce_addTorsion;
import static simtk.openmm.OpenMMLibrary.OpenMM_PeriodicTorsionForce_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_Platform_getDefaultPluginsDirectory;
import static simtk.openmm.OpenMMLibrary.OpenMM_Platform_getNumPlatforms;
import static simtk.openmm.OpenMMLibrary.OpenMM_Platform_getOpenMMVersion;
import static simtk.openmm.OpenMMLibrary.OpenMM_Platform_getPlatformByName;
import static simtk.openmm.OpenMMLibrary.OpenMM_Platform_getPluginLoadFailures;
import static simtk.openmm.OpenMMLibrary.OpenMM_Platform_loadPluginsFromDirectory;
import static simtk.openmm.OpenMMLibrary.OpenMM_Platform_setPropertyDefaultValue;
import static simtk.openmm.OpenMMLibrary.OpenMM_State_DataType.OpenMM_State_Energy;
import static simtk.openmm.OpenMMLibrary.OpenMM_State_DataType.OpenMM_State_Forces;
import static simtk.openmm.OpenMMLibrary.OpenMM_State_DataType.OpenMM_State_ParameterDerivatives;
import static simtk.openmm.OpenMMLibrary.OpenMM_State_DataType.OpenMM_State_Positions;
import static simtk.openmm.OpenMMLibrary.OpenMM_State_destroy;
import static simtk.openmm.OpenMMLibrary.OpenMM_State_getEnergyParameterDerivatives;
import static simtk.openmm.OpenMMLibrary.OpenMM_State_getForces;
import static simtk.openmm.OpenMMLibrary.OpenMM_State_getPotentialEnergy;
import static simtk.openmm.OpenMMLibrary.OpenMM_StringArray_destroy;
import static simtk.openmm.OpenMMLibrary.OpenMM_StringArray_get;
import static simtk.openmm.OpenMMLibrary.OpenMM_StringArray_getSize;
import static simtk.openmm.OpenMMLibrary.OpenMM_System_addConstraint;
import static simtk.openmm.OpenMMLibrary.OpenMM_System_addForce;
import static simtk.openmm.OpenMMLibrary.OpenMM_System_addParticle;
import static simtk.openmm.OpenMMLibrary.OpenMM_System_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_System_destroy;
import static simtk.openmm.OpenMMLibrary.OpenMM_System_getNumConstraints;
import static simtk.openmm.OpenMMLibrary.OpenMM_System_setDefaultPeriodicBoxVectors;
import static simtk.openmm.OpenMMLibrary.OpenMM_System_setVirtualSite;
import static simtk.openmm.OpenMMLibrary.OpenMM_TwoParticleAverageSite_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_Vec3Array_append;
import static simtk.openmm.OpenMMLibrary.OpenMM_Vec3Array_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_Vec3Array_get;
import static simtk.openmm.OpenMMLibrary.OpenMM_Vec3Array_resize;
import static simtk.openmm.OpenMMLibrary.OpenMM_VerletIntegrator_create;

import ffx.crystal.Crystal;
import ffx.potential.bonded.Angle;
import ffx.potential.bonded.Atom;
import ffx.potential.bonded.Bond;
import ffx.potential.bonded.ImproperTorsion;
import ffx.potential.bonded.OutOfPlaneBend;
import ffx.potential.bonded.PiOrbitalTorsion;
import ffx.potential.bonded.RestraintBond;
import ffx.potential.bonded.StretchBend;
import ffx.potential.bonded.Torsion;
import ffx.potential.bonded.TorsionTorsion;
import ffx.potential.bonded.UreyBradley;
import ffx.potential.nonbonded.CoordRestraint;
import ffx.potential.nonbonded.GeneralizedKirkwood;
import ffx.potential.nonbonded.GeneralizedKirkwood.NonPolar;
import ffx.potential.nonbonded.NonbondedCutoff;
import ffx.potential.nonbonded.ParticleMeshEwald;
import ffx.potential.nonbonded.ParticleMeshEwald.Polarization;
import ffx.potential.nonbonded.ReciprocalSpace;
import ffx.potential.nonbonded.VanDerWaals;
import ffx.potential.nonbonded.VanDerWaalsForm;
import ffx.potential.parameters.AngleType;
import ffx.potential.parameters.AngleType.AngleFunction;
import ffx.potential.parameters.BondType;
import ffx.potential.parameters.BondType.BondFunction;
import ffx.potential.parameters.ForceField;
import ffx.potential.parameters.ForceField.ForceFieldBoolean;
import ffx.potential.parameters.ForceField.ForceFieldDouble;
import ffx.potential.parameters.ImproperTorsionType;
import ffx.potential.parameters.MultipoleType;
import ffx.potential.parameters.OutOfPlaneBendType;
import ffx.potential.parameters.PiTorsionType;
import ffx.potential.parameters.PolarizeType;
import ffx.potential.parameters.TorsionTorsionType;
import ffx.potential.parameters.TorsionType;
import ffx.potential.parameters.UreyBradleyType;
import ffx.potential.parameters.VDWType;
import ffx.potential.utils.EnergyException;
import ffx.potential.utils.PotentialsFunctions;
import ffx.potential.utils.PotentialsUtils;
import static ffx.potential.nonbonded.VanDerWaalsForm.EPSILON_RULE.GEOMETRIC;
import static ffx.potential.nonbonded.VanDerWaalsForm.RADIUS_RULE.ARITHMETIC;
import static ffx.potential.nonbonded.VanDerWaalsForm.RADIUS_SIZE.RADIUS;
import static ffx.potential.nonbonded.VanDerWaalsForm.RADIUS_TYPE.R_MIN;
import static ffx.potential.nonbonded.VanDerWaalsForm.VDW_TYPE.LENNARD_JONES;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomBondForce_addBond;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomBondForce_addEnergyParameterDerivative;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomBondForce_addGlobalParameter;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomBondForce_addPerBondParameter;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomBondForce_create;
import static simtk.openmm.OpenMMLibrary.OpenMM_CustomNonbondedForce_addExclusion;

/**
 * Compute the potential energy and derivatives using OpenMM.
 *
 * @author Michael J. Schnieders
 * @since 1.0
 */
public class ForceFieldEnergyOpenMM extends ForceFieldEnergy {

    private static final Logger logger = Logger.getLogger(ForceFieldEnergyOpenMM.class.getName());
    /**
     * Whether to enforce periodic boundary conditions when obtaining new
     * States.
     */
    public final int enforcePBC;

    private String integratorString = "VERLET";
    private double timeStep = 1.0;
    private double temperature = 298.15;

    /**
     * OpenMM Platform.
     */
    private PointerByReference platform = null;
    /**
     * OpenMM System.
     */
    private PointerByReference system = null;
    /**
     * OpenMM Integrator.
     */
    private PointerByReference integrator = null;
    /**
     * OpenMM Context.
     */
    private PointerByReference context = null;
    /**
     * OpenMM State.
     */
    private PointerByReference state = null;
    /**
     * OpenMM Forces.
     */
    private PointerByReference forces = null;
    /**
     * OpenMM Positions.
     */
    private PointerByReference positions = null;
    /**
     * OpenMM Velocities.
     */
    private PointerByReference velocities = null;
    /**
     * OpenMM center-of-mass motion remover.
     */
    private PointerByReference commRemover = null;
    /**
     * Number of particles.
     */
    private int numParticles = 0;
    /**
     * OpenMM AMOEBA Bond Force.
     */
    private PointerByReference amoebaBondForce = null;
    /**
     * OpenMM AMOEBA Angle Force.
     */
    private PointerByReference amoebaAngleForce = null;
    /**
     * OpenMM AMOEBA In-Plane Angle Force.
     */
    private PointerByReference amoebaInPlaneAngleForce = null;
    /**
     * OpenMM AMOEBA Urey Bradley Force.
     */
    private PointerByReference amoebaUreyBradleyForce = null;
    /**
     * OpenMM AMOEBA Out-of-Plane Bend Force.
     */
    private PointerByReference amoebaOutOfPlaneBendForce = null;
    /**
     * OpenMM AMOEBA Stretch Bend Force.
     */
    private PointerByReference amoebaStretchBendForce = null;
    /**
     * OpenMM AMOEBA Torsion Force.
     */
    private PointerByReference amoebaTorsionForce = null;
    /**
     * OpenMM Improper Torsion Force.
     */
    private PointerByReference amoebaImproperTorsionForce = null;
    /**
     * OpenMM AMOEBA Pi Torsion Force.
     */
    private PointerByReference amoebaPiTorsionForce = null;
    /**
     * OpenMM AMOEBA Torsion Torsion Force.
     */
    private PointerByReference amoebaTorsionTorsionForce = null;
    /**
     * OpenMM AMOEBA van der Waals Force.
     */
    private PointerByReference amoebaVDWForce = null;
    /**
     * OpenMM AMOEBA Multipole Force.
     */
    private PointerByReference amoebaMultipoleForce = null;
    /**
     * OpenMM Generalized Kirkwood Force.
     */
    private PointerByReference amoebaGeneralizedKirkwoodForce = null;
    /**
     * OpenMM AMOEBA WCA Dispersion Force.
     */
    private PointerByReference amoebaWcaDispersionForce = null;
    /**
     * OpenMM Fixed Charge Non-Bonded Force.
     */
    private PointerByReference fixedChargeNonBondedForce = null;
    /**
     * Fixed charge softcore vdW force.
     */
    boolean softcoreCreated = false;
    private PointerByReference fixedChargeSoftcore = null;

    private PointerByReference alchemicalAlchemicalStericsForce = null;

    private PointerByReference nonAlchemicalAlchemicalStericsForce = null;

    private PointerByReference allStericsForce = null;

    private PointerByReference alchemicalAlchemicalElectrostaticsForce = null;

    private PointerByReference nonAlchemicalAlchemicalElectrostaticsForce = null;

    private PointerByReference allElectrostaticsForce = null;

    private boolean chargeExclusion[];
    private boolean vdWExclusion[];
    private double exceptionChargeProd[];
    private double exceptionEps[];

    /**
     * OpenMM Custom GB Force.
     */
    private PointerByReference customGBForce = null;
    /**
     * OpenMM harmonic bond force, used for harmonic bond restraints.
     */
    private PointerByReference harmonicBondForce = null;
    /**
     * Langevin friction coefficient.
     */
    private double frictionCoeff;
    /**
     * Langevin collision frequency.
     */
    private double collisionFreq;
    /**
     * Value of the state variable Lambda.
     */
    private boolean pmeLambdaTerm = false;
    private boolean vdwLambdaTerm = false;
    private double lambda = 1.0;
    private double vdwdUdL = 0.0;

    /**
     * Lambda step size for finite difference dU/dL.
     */
    private double fdDLambda = 0.001;
    /**
     * Flag to set water molecule bonds as rigid.
     */
    private boolean rigidHydrogen = false;
    /**
     * OpenMM thermostat. Currently an Andersen thermostat is supported.
     */
    private PointerByReference ommThermostat = null;

    private boolean doOpenMMdEdL = false;
    private boolean doFFXdEdL = true;
    private boolean testdEdL = true;

    /**
     * ForceFieldEnergyOpenMM constructor; offloads heavy-duty computation to an
     * OpenMM Platform while keeping track of information locally.
     *
     * @param molecularAssembly Assembly to construct energy for.
     * @param requestedPlatform requested OpenMM platform to be used.
     * @param restraints Harmonic coordinate restraints.
     * @param nThreads Number of threads to use in the super class
     * ForceFieldEnergy instance.
     */
    protected ForceFieldEnergyOpenMM(MolecularAssembly molecularAssembly, Platform requestedPlatform, List<CoordRestraint> restraints, int nThreads) {
        super(molecularAssembly, restraints, nThreads);

        //super.energy(false, true);
        logger.info(" Initializing OpenMM\n");

        loadPlatform(requestedPlatform);

        // Create the OpenMM System
        system = OpenMM_System_create();
        logger.info(" Created OpenMM System");

        ForceField forceField = molecularAssembly.getForceField();

        // Load atoms.
        try {
            addAtoms();
        } catch (Exception e) {
            logger.severe(" Atom without mass encountered.");
        }

        rigidHydrogen = forceField.getBoolean(ForceField.ForceFieldBoolean.RIGID_HYDROGEN, false);

        if (rigidHydrogen) {
            setUpHydrogenConstraints(system);
        }

        // Add Bond Force.
        addBondForce();

        // Add Angle Force.
        addAngleForce();
        addInPlaneAngleForce();

        // Add Stretch-Bend Force.
        addStretchBendForce();

        // Add Urey-Bradley Force.
        addUreyBradleyForce();

        // Out-of Plane Bend Force.
        addOutOfPlaneBendForce();

        // Add Torsion Force.
        addTorsionForce();

        // Add Improper Torsion Force.
        addImproperTorsionForce();

        // Add Pi-Torsion Force.
        addPiTorsionForce();

        // Add Torsion-Torsion Force.
        addTorsionTorsionForce();

        // Add coordinate restraints.
        addHarmonicRestraintForce();

        // Add bond restraints.
        addRestraintBonds();

        VanDerWaals vdW = super.getVdwNode();
        if (vdW != null) {
            VanDerWaalsForm vdwForm = vdW.getVDWForm();
            if (vdwForm.vdwType == LENNARD_JONES) {
                addFixedChargeNonBondedForce();
            } else {
                // Add vdW Force.
                addAmoebaVDWForce();

                // Add Multipole Force.
                addAmoebaMultipoleForce();
            }
        }

        // Set periodic box vectors.
        setDefaultPeriodicBoxVectors();

        frictionCoeff = forceField.getDouble(ForceFieldDouble.FRICTION_COEFF, 91.0);
        collisionFreq = forceField.getDouble(ForceFieldDouble.COLLISION_FREQ, 0.01);

        createContext(integratorString, timeStep, temperature);

        // Set initial positions.
        double x[] = new double[numParticles * 3];
        int index = 0;
        Atom atoms[] = molecularAssembly.getAtomArray();
        for (int i = 0; i < numParticles; i++) {
            Atom atom = atoms[i];
            x[index] = atom.getX();
            x[index + 1] = atom.getY();
            x[index + 2] = atom.getZ();
            index += 3;
        }
        setOpenMMPositions(x, numParticles * 3);

        int infoMask = OpenMM_State_Positions;
        infoMask += OpenMM_State_Forces;
        infoMask += OpenMM_State_Energy;

        boolean aperiodic = super.getCrystal().aperiodic();
        boolean pbcEnforced = forceField.getBoolean(ForceField.ForceFieldBoolean.ENFORCE_PBC, !aperiodic);
        enforcePBC = pbcEnforced ? OpenMM_True : OpenMM_False;

        state = OpenMM_Context_getState(context, infoMask, enforcePBC);
        forces = OpenMM_State_getForces(state);
        double openMMPotentialEnergy = OpenMM_State_getPotentialEnergy(state) / OpenMM_KJPerKcal;

        logger.log(Level.INFO, String.format(" OpenMM Energy: %14.10g", openMMPotentialEnergy));
        fdDLambda = forceField.getDouble(ForceFieldDouble.FD_DLAMBDA, 0.001);

        OpenMM_State_destroy(state);

        pmeLambdaTerm = forceField.getBoolean(ForceFieldBoolean.PME_LAMBDATERM,
                forceField.getBoolean(ForceFieldBoolean.LAMBDATERM, false));

        vdwLambdaTerm = forceField.getBoolean(ForceFieldBoolean.VDW_LAMBDATERM, false);

        /**
         * Currently, electrostatics and vdW cannot undergo alchemical changes
         * simultaneously. Softcore vdW is given priority.
         */
        if (vdwLambdaTerm) {
            pmeLambdaTerm = false;
        }

        if (pmeLambdaTerm || vdwLambdaTerm) {
            lambdaTerm = true;
        }

        logger.info(format(" vdwLambdaTerm %s", vdwLambdaTerm));
        logger.info(format(" pmeLambdaTerm %s", pmeLambdaTerm));
        logger.info(format(" lambdaTerm %s", lambdaTerm));
    }

    /**
     * Load an OpenMM Platform
     */
    private void loadPlatform(Platform requestedPlatform) {
        // Print out the OpenMM Version.
        Pointer version = OpenMM_Platform_getOpenMMVersion();
        logger.log(Level.INFO, " OpenMM Version: {0}", version.getString(0));

        // Print out the OpenMM plugin directory.
        Pointer pluginDir = OpenMM_Platform_getDefaultPluginsDirectory();
        String pluginDirString = pluginDir.getString(0);
        if (SystemUtils.IS_OS_WINDOWS) {
            pluginDirString = pluginDirString + "/plugins";
        }
        logger.log(Level.INFO, " OpenMM Plugin Dir: {0}", pluginDirString);

        /**
         * Load plugins and print out plugins.
         *
         * Call the method twice to avoid a bug where not all platforms are list
         * after the first call.
         */
        PointerByReference plugins = OpenMM_Platform_loadPluginsFromDirectory(pluginDirString);
        OpenMM_StringArray_destroy(plugins);

        plugins = OpenMM_Platform_loadPluginsFromDirectory(pluginDirString);
        int numPlugins = OpenMM_StringArray_getSize(plugins);

        logger.log(Level.INFO, " Number of OpenMM Plugins: {0}", numPlugins);
        boolean cuda = false;
        for (int i = 0; i < numPlugins; i++) {
            String pluginString = stringFromArray(plugins, i);
            logger.log(Level.INFO, "  Plugin: {0}", pluginString);
            if (pluginString.toUpperCase().contains("AMOEBACUDA")) {
                cuda = true;
            }
        }
        OpenMM_StringArray_destroy(plugins);

        /**
         * Extra logging to print out plugins that failed to load.
         */
        if (logger.isLoggable(Level.FINE)) {
            PointerByReference pluginFailers = OpenMM_Platform_getPluginLoadFailures();
            int numFailures = OpenMM_StringArray_getSize(pluginFailers);
            for (int i = 0; i < numFailures; i++) {
                String pluginString = stringFromArray(pluginFailers, i);
                logger.log(Level.FINE, " Plugin load failure: {0}", pluginString);
            }
            OpenMM_StringArray_destroy(pluginFailers);
        }

        int numPlatforms = OpenMM_Platform_getNumPlatforms();
        logger.log(Level.INFO, " Number of OpenMM Platforms: {0}", numPlatforms);

        /**
         * for (int i = 0; i < numPlatforms; i++) { PointerByReference
         * currentPlatform = OpenMM_Platform_getPlatform(i); Pointer
         * platformName = OpenMM_Platform_getName(currentPlatform);
         * logger.log(Level.INFO, " Platform: {0}", platformName.getString(0));
         * OpenMM_Platform_destroy(currentPlatform); }
         */
        String defaultPrecision = "mixed";
        String precision = molecularAssembly.getForceField().getString(ForceField.ForceFieldString.PRECISION, defaultPrecision).toLowerCase();
        precision = precision.replace("-precision", "");
        switch (precision) {
            case "double":
            case "mixed":
            case "single":
                logger.info(String.format(" Using precision level %s", precision));
                break;
            default:
                logger.info(String.format(" Could not interpret precision level %s, defaulting to %s", precision, defaultPrecision));
                precision = defaultPrecision;
                break;
        }

        Comm world = Comm.world();
        int size = world.size();
        int defDeviceIndex = 0;
        if (size > 1) {
            int rank = world.rank();
            CompositeConfiguration props = molecularAssembly.getProperties();
            // 0/no-arg would indicate "just put everything on device specified by CUDA_DEVICE".
            // TODO: Get the number of CUDA devices from the CUDA API as the alternate default.
            int numCudaDevices = props.getInt("numCudaDevices", 0);
            if (numCudaDevices > 0) {
                defDeviceIndex = rank % numCudaDevices;
                logger.info(String.format(" Placing energy from rank %d on device %d", rank, defDeviceIndex));
            }
        }

        int deviceID = molecularAssembly.getForceField().getInteger(ForceField.ForceFieldInteger.CUDA_DEVICE, defDeviceIndex);
        String deviceIDString = Integer.toString(deviceID);

        if (cuda && requestedPlatform != Platform.OMM_REF) {
            platform = OpenMM_Platform_getPlatformByName("CUDA");
            OpenMM_Platform_setPropertyDefaultValue(platform, pointerForString("CudaDeviceIndex"), pointerForString(deviceIDString));
            OpenMM_Platform_setPropertyDefaultValue(platform, pointerForString("Precision"), pointerForString(precision));
            logger.info(String.format(" Selected OpenMM AMOEBA CUDA Platform (Device ID: %d)", deviceID));
        } else {
            platform = OpenMM_Platform_getPlatformByName("Reference");
            logger.info(" Selected OpenMM AMOEBA Reference Platform");
        }
    }

    /**
     * createContext takes in a parameters to determine which integrator the
     * user requested during the start up of the simulation. A switch statement
     * is used with Strings as the variable to determine between Lengevin,
     * Brownian, Custom, Compound and Verlet integrator
     *
     * @param integratorString
     * @param timeStep
     * @param temperature
     */
    public void createContext(String integratorString, double timeStep, double temperature) {

        this.integratorString = integratorString;
        this.timeStep = timeStep;
        this.temperature = temperature;

        OpenMM_Context_destroy(context);

        double dt = timeStep * 1.0e-3;
        switch (integratorString) {
            case "LANGEVIN":
                logger.log(Level.INFO, String.format(" Created Langevin integrator with time step %6.3e (psec).", dt));
                integrator = OpenMM_LangevinIntegrator_create(temperature, frictionCoeff, dt);
                break;
            /*
            case "BROWNIAN":
                integrator = OpenMM_BrownianIntegrator_create(temperature, frictionCoeff, dt);
                break;
            case "CUSTOM":
                integrator = OpenMM_CustomIntegrator_create(dt);
                break;
            case "COMPOUND":
                integrator = OpenMM_CompoundIntegrator_create();
                break;
             */
            case "VERLET":
            default:
                logger.log(Level.INFO, String.format(" Created Verlet integrator with time step %6.3e (psec).", dt));
                integrator = OpenMM_VerletIntegrator_create(dt);
        }

        // Create a context.
        context = OpenMM_Context_create_2(system, integrator, platform);

        // Set initial positions.
        double x[] = new double[numParticles * 3];
        int index = 0;
        Atom atoms[] = molecularAssembly.getAtomArray();
        for (int i = 0; i < numParticles; i++) {
            Atom atom = atoms[i];
            x[index] = atom.getX();
            x[index + 1] = atom.getY();
            x[index + 2] = atom.getZ();
            index += 3;
        }

        setOpenMMPositions(x, numParticles * 3);
    }

    public String getIntegratorString() {
        return integratorString;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getTimeStep() {
        return timeStep;
    }

    /**
     * Create a JNA Pointer to a String.
     *
     * @param string WARNING: assumes ascii-only string
     * @return pointer.
     */
    private Pointer pointerForString(String string) {
        Pointer pointer = new Memory(string.length() + 1);
        pointer.setString(0, string);
        return pointer;
    }

    /**
     * Returns the platform array as a String
     *
     * @param stringArray
     * @param i
     * @return String
     */
    private String stringFromArray(PointerByReference stringArray, int i) {
        Pointer platformPtr = OpenMM_StringArray_get(stringArray, i);
        if (platformPtr == null) {
            return null;
        }
        return platformPtr.getString(0);
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
        freeOpenMM();
        logger.info(" Destroyed the Context, Integrator, and OpenMMSystem.");
    }

    @Override
    public void finalize() throws Throwable {
        // Safer to leave super.finalize() in, even though right now that calls Object.finalize().
        logger.info(" ForceFieldEnergyOpenMM instance is being finalized.");
        super.finalize();
        if (destroyed) {
            logger.info(String.format(" Finalize called on a destroyed OpenMM ForceFieldEnergy %s", this.toString()));
        } else {
            destroy();
        }
    }

    /**
     * Destroys pointer references to Context, Integrator and System to free up
     * memory.
     */
    private void freeOpenMM() {
        if (context != null) {
            OpenMM_Context_destroy(context);
            context = null;
        }
        if (integrator != null) {
            OpenMM_Integrator_destroy(integrator);
            integrator = null;
        }
        if (system != null) {
            OpenMM_System_destroy(system);
            system = null;
        }
    }

    /**
     * Adds atoms from the molecular assembly to the OpenMM System and reports
     * to the user the number of particles added.
     */
    private void addAtoms() throws Exception {
        Atom[] atoms = molecularAssembly.getAtomArray();
        int nAtoms = atoms.length;
        numParticles = 0;
        for (int i = 0; i < nAtoms; i++) {
            Atom atom = atoms[i];
            OpenMM_System_addParticle(system, atom.getMass());
            if (atom.getMass() <= 0.0) {
                throw new Exception(" Atom without mass greater than 0.");
            }
            numParticles++;
        }
        logger.log(Level.INFO, " Added particles ({0})", nAtoms);
    }

    /**
     * Adds a center-of-mass motion remover to the Potential. Not advised for
     * anything not running MD using the OpenMM library (i.e.
     * OpenMMMolecularDynamics). Has caused bugs with the FFX MD class.
     */
    public void addCOMMRemover() {
        addCOMMRemover(false);
    }

    /**
     * Adds a center-of-mass motion remover to the Potential. Not advised for
     * anything not running MD using the OpenMM library (i.e.
     * OpenMMMolecularDynamics). Has caused bugs with the FFX MD class.
     *
     * @param addIfDuplicate Add a CCOM remover even if it already exists
     */
    public void addCOMMRemover(boolean addIfDuplicate) {
        if (commRemover == null || addIfDuplicate) {
            if (commRemover != null) {
                logger.warning(" Adding a second center-of-mass remover; this is probably incorrect!");
            }
            int frequency = 100;
            commRemover = OpenMM_CMMotionRemover_create(frequency);
            OpenMM_System_addForce(system, commRemover);
            logger.log(Level.INFO, " Added center of mass motion remover (frequency: {0})", frequency);
        } else {
            logger.warning(" Attempted to add a second center-of-mass motion remover when one already exists!");
        }
    }

    /**
     * Add an Andersen thermostat to the system.
     *
     * @param targetTemp Target temperature in Kelvins.
     */
    public void addAndersenThermostat(double targetTemp) {
        addAndersenThermostat(targetTemp, collisionFreq);
    }

    /**
     * Add an Andersen thermostat to the system.
     *
     * @param targetTemp Target temperature in Kelvins.
     * @param collisionFreq Collision frequency in 1/psec
     */
    public void addAndersenThermostat(double targetTemp, double collisionFreq) {
        if (ommThermostat == null) {
            ommThermostat = OpenMM_AndersenThermostat_create(targetTemp, collisionFreq);
            OpenMM_System_addForce(system, ommThermostat);
            logger.info(format(" Added an Andersen thermostat at %10.6fK and collison frequency %10.6f.", targetTemp, collisionFreq));
        } else {
            logger.info(" Attempted to add a second thermostat to an OpenMM force field!");
        }
    }

    private void addBondForce() {
        Bond bonds[] = super.getBonds();
        if (bonds == null || bonds.length < 1) {
            return;
        }
        int nBonds = bonds.length;
        amoebaBondForce = OpenMM_AmoebaBondForce_create();
        double kParameterConversion = OpenMM_KJPerKcal / (OpenMM_NmPerAngstrom * OpenMM_NmPerAngstrom);

        for (int i = 0; i < nBonds; i++) {
            Bond bond = bonds[i];
            int i1 = bond.getAtom(0).getXyzIndex() - 1;
            int i2 = bond.getAtom(1).getXyzIndex() - 1;
            BondType bondType = bond.bondType;
            OpenMM_AmoebaBondForce_addBond(amoebaBondForce, i1, i2,
                    bond.bondType.distance * OpenMM_NmPerAngstrom,
                    kParameterConversion * bondType.forceConstant * BondType.units);

        }

        if (bonds[0].bondType.bondFunction == BondFunction.QUARTIC) {
            OpenMM_AmoebaBondForce_setAmoebaGlobalBondCubic(amoebaBondForce,
                    BondType.cubic / OpenMM_NmPerAngstrom);
            OpenMM_AmoebaBondForce_setAmoebaGlobalBondQuartic(amoebaBondForce,
                    BondType.quartic / (OpenMM_NmPerAngstrom * OpenMM_NmPerAngstrom));
        }

        OpenMM_System_addForce(system, amoebaBondForce);
        logger.log(Level.INFO, " Added bonds ({0})", nBonds);
    }

    private void addAngleForce() {
        Angle angles[] = super.getAngles();
        if (angles == null || angles.length < 1) {
            return;
        }
        int nAngles = angles.length;
        List<Angle> normalAngles = new ArrayList<>();
        // Sort all normal angles from in-plane angles
        for (int i = 0; i < nAngles; i++) {
            if (angles[i].getAngleMode() == Angle.AngleMode.NORMAL) {
                normalAngles.add(angles[i]);
            }
        }
        nAngles = normalAngles.size();
        if (nAngles < 1) {
            return;
        }
        amoebaAngleForce = OpenMM_AmoebaAngleForce_create();
        for (int i = 0; i < nAngles; i++) {
            Angle angle = normalAngles.get(i);
            int i1 = angle.getAtom(0).getXyzIndex() - 1;
            int i2 = angle.getAtom(1).getXyzIndex() - 1;
            int i3 = angle.getAtom(2).getXyzIndex() - 1;
            int nh = angle.nh;
            OpenMM_AmoebaAngleForce_addAngle(amoebaAngleForce, i1, i2, i3,
                    angle.angleType.angle[nh], OpenMM_KJPerKcal * AngleType.units * angle.angleType.forceConstant);
        }

        if (angles[0].angleType.angleFunction == AngleFunction.SEXTIC) {
            OpenMM_AmoebaAngleForce_setAmoebaGlobalAngleCubic(amoebaAngleForce, AngleType.cubic);
            OpenMM_AmoebaAngleForce_setAmoebaGlobalAngleQuartic(amoebaAngleForce, AngleType.quartic);
            OpenMM_AmoebaAngleForce_setAmoebaGlobalAnglePentic(amoebaAngleForce, AngleType.quintic);
            OpenMM_AmoebaAngleForce_setAmoebaGlobalAngleSextic(amoebaAngleForce, AngleType.sextic);
        }

        OpenMM_System_addForce(system, amoebaAngleForce);
        logger.log(Level.INFO, " Added angles ({0})", nAngles);
    }

    private void addInPlaneAngleForce() {
        Angle angles[] = super.getAngles();
        if (angles == null || angles.length < 1) {
            return;
        }
        int nAngles = angles.length;
        List<Angle> inPlaneAngles = new ArrayList<>();
        //Sort all in-plane angles from normal angles
        for (int i = 0; i < nAngles; i++) {
            if (angles[i].getAngleMode() == Angle.AngleMode.IN_PLANE) {
                inPlaneAngles.add(angles[i]);
            }
        }
        nAngles = inPlaneAngles.size();
        if (nAngles < 1) {
            return;
        }
        amoebaInPlaneAngleForce = OpenMM_AmoebaInPlaneAngleForce_create();
        for (int i = 0; i < nAngles; i++) {
            Angle angle = inPlaneAngles.get(i);
            int i1 = angle.getAtom(0).getXyzIndex() - 1;
            int i2 = angle.getAtom(1).getXyzIndex() - 1;
            int i3 = angle.getAtom(2).getXyzIndex() - 1;
            int i4 = angle.getAtom4().getXyzIndex() - 1;
            int nh = angle.nh;
            OpenMM_AmoebaInPlaneAngleForce_addAngle(amoebaInPlaneAngleForce, i1, i2, i3, i4,
                    angle.angleType.angle[nh], OpenMM_KJPerKcal * AngleType.units * angle.angleType.forceConstant);
        }
        OpenMM_AmoebaInPlaneAngleForce_setAmoebaGlobalInPlaneAngleCubic(amoebaInPlaneAngleForce, AngleType.cubic);
        OpenMM_AmoebaInPlaneAngleForce_setAmoebaGlobalInPlaneAngleQuartic(amoebaInPlaneAngleForce, AngleType.quartic);
        OpenMM_AmoebaInPlaneAngleForce_setAmoebaGlobalInPlaneAnglePentic(amoebaInPlaneAngleForce, AngleType.quintic);
        OpenMM_AmoebaInPlaneAngleForce_setAmoebaGlobalInPlaneAngleSextic(amoebaInPlaneAngleForce, AngleType.sextic);
        OpenMM_System_addForce(system, amoebaInPlaneAngleForce);
        logger.log(Level.INFO, " Added in-plane angles ({0})", nAngles);
    }

    private void addUreyBradleyForce() {
        UreyBradley ureyBradleys[] = super.getUreyBradleys();
        if (ureyBradleys == null || ureyBradleys.length < 1) {
            return;
        }
        amoebaUreyBradleyForce = OpenMM_AmoebaBondForce_create();
        double kParameterConversion = UreyBradleyType.units * OpenMM_KJPerKcal / (OpenMM_NmPerAngstrom * OpenMM_NmPerAngstrom);
        int nUreys = ureyBradleys.length;
        for (int i = 0; i < nUreys; i++) {
            UreyBradley ureyBradley = ureyBradleys[i];
            int i1 = ureyBradley.getAtom(0).getXyzIndex() - 1;
            int i2 = ureyBradley.getAtom(2).getXyzIndex() - 1;
            UreyBradleyType ureyBradleyType = ureyBradley.ureyBradleyType;
            OpenMM_AmoebaBondForce_addBond(amoebaUreyBradleyForce, i1, i2,
                    ureyBradleyType.distance * OpenMM_NmPerAngstrom,
                    ureyBradleyType.forceConstant * kParameterConversion);
        }

        OpenMM_AmoebaBondForce_setAmoebaGlobalBondCubic(amoebaUreyBradleyForce,
                UreyBradleyType.cubic / OpenMM_NmPerAngstrom);
        OpenMM_AmoebaBondForce_setAmoebaGlobalBondQuartic(amoebaUreyBradleyForce,
                UreyBradleyType.quartic / (OpenMM_NmPerAngstrom * OpenMM_NmPerAngstrom));

        OpenMM_System_addForce(system, amoebaUreyBradleyForce);
        logger.log(Level.INFO, " Added Urey-Bradleys ({0})", nUreys);
    }

    private void addOutOfPlaneBendForce() {
        OutOfPlaneBend outOfPlaneBends[] = super.getOutOfPlaneBends();
        if (outOfPlaneBends == null || outOfPlaneBends.length < 1) {
            return;
        }
        amoebaOutOfPlaneBendForce = OpenMM_AmoebaOutOfPlaneBendForce_create();
        int nOutOfPlaneBends = outOfPlaneBends.length;
        for (int i = 0; i < nOutOfPlaneBends; i++) {
            OutOfPlaneBend outOfPlaneBend = outOfPlaneBends[i];
            int i1 = outOfPlaneBend.getAtom(0).getXyzIndex() - 1;
            int i2 = outOfPlaneBend.getAtom(1).getXyzIndex() - 1;
            int i3 = outOfPlaneBend.getAtom(2).getXyzIndex() - 1;
            int i4 = outOfPlaneBend.getAtom(3).getXyzIndex() - 1;
            OutOfPlaneBendType outOfPlaneBendType = outOfPlaneBend.outOfPlaneBendType;

            OpenMM_AmoebaOutOfPlaneBendForce_addOutOfPlaneBend(amoebaOutOfPlaneBendForce, i1, i2, i3, i4,
                    OpenMM_KJPerKcal * outOfPlaneBendType.forceConstant * OutOfPlaneBendType.units);
        }
        OpenMM_AmoebaOutOfPlaneBendForce_setAmoebaGlobalOutOfPlaneBendCubic(amoebaOutOfPlaneBendForce, OutOfPlaneBendType.cubic);
        OpenMM_AmoebaOutOfPlaneBendForce_setAmoebaGlobalOutOfPlaneBendQuartic(amoebaOutOfPlaneBendForce, OutOfPlaneBendType.quartic);
        OpenMM_AmoebaOutOfPlaneBendForce_setAmoebaGlobalOutOfPlaneBendPentic(amoebaOutOfPlaneBendForce, OutOfPlaneBendType.quintic);
        OpenMM_AmoebaOutOfPlaneBendForce_setAmoebaGlobalOutOfPlaneBendSextic(amoebaOutOfPlaneBendForce, OutOfPlaneBendType.sextic);
        OpenMM_System_addForce(system, amoebaOutOfPlaneBendForce);
        logger.log(Level.INFO, " Added Out of Plane Bends ({0})", nOutOfPlaneBends);
    }

    private void addStretchBendForce() {
        StretchBend stretchBends[] = super.getStretchBends();
        if (stretchBends == null || stretchBends.length < 1) {
            return;
        }
        int nStretchBends = stretchBends.length;
        amoebaStretchBendForce = OpenMM_AmoebaStretchBendForce_create();
        for (int i = 0; i < nStretchBends; i++) {
            StretchBend stretchBend = stretchBends[i];
            int i1 = stretchBend.getAtom(0).getXyzIndex() - 1;
            int i2 = stretchBend.getAtom(1).getXyzIndex() - 1;
            int i3 = stretchBend.getAtom(2).getXyzIndex() - 1;
            double angle = stretchBend.angleEq;
            double beq0 = stretchBend.bond0Eq;
            double beq1 = stretchBend.bond1Eq;
            double fc0 = stretchBend.force0;
            double fc1 = stretchBend.force1;
            OpenMM_AmoebaStretchBendForce_addStretchBend(amoebaStretchBendForce, i1, i2, i3,
                    beq0 * OpenMM_NmPerAngstrom, beq1 * OpenMM_NmPerAngstrom, OpenMM_RadiansPerDegree * angle,
                    (OpenMM_KJPerKcal / OpenMM_NmPerAngstrom) * fc0, (OpenMM_KJPerKcal / OpenMM_NmPerAngstrom) * fc1);

        }
        OpenMM_System_addForce(system, amoebaStretchBendForce);
        logger.log(Level.INFO, " Added Stretch Bends ({0})", nStretchBends);
    }

    private void addTorsionForce() {
        Torsion torsions[] = super.getTorsions();
        if (torsions == null || torsions.length < 1) {
            return;
        }
        int nTorsions = torsions.length;
        amoebaTorsionForce = OpenMM_PeriodicTorsionForce_create();
        for (int i = 0; i < nTorsions; i++) {
            Torsion torsion = torsions[i];
            int a1 = torsion.getAtom(0).getXyzIndex() - 1;
            int a2 = torsion.getAtom(1).getXyzIndex() - 1;
            int a3 = torsion.getAtom(2).getXyzIndex() - 1;
            int a4 = torsion.getAtom(3).getXyzIndex() - 1;
            TorsionType torsionType = torsion.torsionType;
            int nTerms = torsionType.phase.length;
            for (int j = 0; j < nTerms; j++) {
                OpenMM_PeriodicTorsionForce_addTorsion(amoebaTorsionForce,
                        a1, a2, a3, a4, j + 1,
                        torsionType.phase[j] * OpenMM_RadiansPerDegree,
                        OpenMM_KJPerKcal * torsion.units * torsionType.amplitude[j]);
            }
        }

        OpenMM_System_addForce(system, amoebaTorsionForce);
        logger.log(Level.INFO, " Added Torsions ({0})", nTorsions);
    }

    private void addImproperTorsionForce() {
        ImproperTorsion impropers[] = super.getImproperTorsions();
        if (impropers == null || impropers.length < 1) {
            return;
        }
        int nImpropers = impropers.length;
        amoebaImproperTorsionForce = OpenMM_PeriodicTorsionForce_create();

        for (int i = 0; i < nImpropers; i++) {
            ImproperTorsion improperTorsion = impropers[i];
            int a1 = improperTorsion.getAtom(0).getXyzIndex() - 1;
            int a2 = improperTorsion.getAtom(1).getXyzIndex() - 1;
            int a3 = improperTorsion.getAtom(2).getXyzIndex() - 1;
            int a4 = improperTorsion.getAtom(3).getXyzIndex() - 1;
            ImproperTorsionType improperTorsionType = improperTorsion.improperType;
            OpenMM_PeriodicTorsionForce_addTorsion(amoebaImproperTorsionForce,
                    a1, a2, a3, a4, improperTorsionType.periodicity,
                    improperTorsionType.phase * OpenMM_RadiansPerDegree,
                    OpenMM_KJPerKcal * improperTorsion.units
                    * improperTorsion.scaleFactor * improperTorsionType.k);
        }
        OpenMM_System_addForce(system, amoebaImproperTorsionForce);
        logger.log(Level.INFO, " Added improper torsions ({0})", nImpropers);
    }

    private void addPiTorsionForce() {
        PiOrbitalTorsion piOrbitalTorsions[] = super.getPiOrbitalTorsions();
        if (piOrbitalTorsions == null || piOrbitalTorsions.length < 1) {
            return;
        }
        int nPiOrbitalTorsions = piOrbitalTorsions.length;
        amoebaPiTorsionForce = OpenMM_AmoebaPiTorsionForce_create();
        double units = PiTorsionType.units;
        for (int i = 0; i < nPiOrbitalTorsions; i++) {
            PiOrbitalTorsion piOrbitalTorsion = piOrbitalTorsions[i];
            int a1 = piOrbitalTorsion.getAtom(0).getXyzIndex() - 1;
            int a2 = piOrbitalTorsion.getAtom(1).getXyzIndex() - 1;
            int a3 = piOrbitalTorsion.getAtom(2).getXyzIndex() - 1;
            int a4 = piOrbitalTorsion.getAtom(3).getXyzIndex() - 1;
            int a5 = piOrbitalTorsion.getAtom(4).getXyzIndex() - 1;
            int a6 = piOrbitalTorsion.getAtom(5).getXyzIndex() - 1;
            PiTorsionType type = piOrbitalTorsion.piTorsionType;
            OpenMM_AmoebaPiTorsionForce_addPiTorsion(amoebaPiTorsionForce,
                    a1, a2, a3, a4, a5, a6,
                    OpenMM_KJPerKcal * type.forceConstant * units);
        }
        OpenMM_System_addForce(system, amoebaPiTorsionForce);
        logger.log(Level.INFO, " Added Pi-Orbital Torsions ({0})", nPiOrbitalTorsions);
    }

    private void addTorsionTorsionForce() {
        TorsionTorsion torsionTorsions[] = super.getTorsionTorsions();
        if (torsionTorsions == null || torsionTorsions.length < 1) {
            return;
        }
        /**
         * Load the torsion-torsions.
         */

        int nTypes = 0;
        LinkedHashMap<String, TorsionTorsionType> torTorTypes = new LinkedHashMap<>();

        int nTorsionTorsions = torsionTorsions.length;
        amoebaTorsionTorsionForce = OpenMM_AmoebaTorsionTorsionForce_create();
        for (int i = 0; i < nTorsionTorsions; i++) {
            TorsionTorsion torsionTorsion = torsionTorsions[i];
            int ia = torsionTorsion.getAtom(0).getXyzIndex() - 1;
            int ib = torsionTorsion.getAtom(1).getXyzIndex() - 1;
            int ic = torsionTorsion.getAtom(2).getXyzIndex() - 1;
            int id = torsionTorsion.getAtom(3).getXyzIndex() - 1;
            int ie = torsionTorsion.getAtom(4).getXyzIndex() - 1;

            TorsionTorsionType torsionTorsionType = torsionTorsion.torsionTorsionType;
            String key = torsionTorsionType.getKey();
            /**
             * Check if the TorTor parameters have already been added to the
             * Hash.
             */
            int gridIndex = 0;
            if (torTorTypes.containsKey(key)) {
                /**
                 * If the TorTor has been added, get its (ordered) index in the
                 * Hash.
                 */
                int index = 0;
                for (String entry : torTorTypes.keySet()) {
                    if (entry.equalsIgnoreCase(key)) {
                        gridIndex = index;
                        break;
                    } else {
                        index++;
                    }
                }
            } else {
                /**
                 * Add the new TorTor.
                 */
                torTorTypes.put(key, torsionTorsionType);
                gridIndex = nTypes;
                nTypes++;
            }

            Atom atom = torsionTorsion.getChiralAtom();
            int iChiral = -1;
            if (atom != null) {
                iChiral = atom.getXyzIndex() - 1;
            }
            OpenMM_AmoebaTorsionTorsionForce_addTorsionTorsion(amoebaTorsionTorsionForce,
                    ia, ib, ic, id, ie, iChiral, gridIndex);
        }
        /**
         * Load the Torsion-Torsion parameters.
         */
        PointerByReference values = OpenMM_DoubleArray_create(6);
        int gridIndex = 0;
        for (String key : torTorTypes.keySet()) {
            TorsionTorsionType torTorType = torTorTypes.get(key);
            int nx = torTorType.nx;
            int ny = torTorType.ny;
            double tx[] = torTorType.tx;
            double ty[] = torTorType.ty;
            double f[] = torTorType.energy;
            double dx[] = torTorType.dx;
            double dy[] = torTorType.dy;
            double dxy[] = torTorType.dxy;
            /**
             * Create the 3D grid.
             */
            PointerByReference grid3D = OpenMM_3D_DoubleArray_create(nx, ny, 6);
            int xIndex = 0;
            int yIndex = 0;
            for (int j = 0; j < nx * ny; j++) {
                int addIndex = 0;
                OpenMM_DoubleArray_set(values, addIndex++, tx[xIndex]);
                OpenMM_DoubleArray_set(values, addIndex++, ty[yIndex]);
                OpenMM_DoubleArray_set(values, addIndex++, OpenMM_KJPerKcal * f[j]);
                OpenMM_DoubleArray_set(values, addIndex++, OpenMM_KJPerKcal * dx[j]);
                OpenMM_DoubleArray_set(values, addIndex++, OpenMM_KJPerKcal * dy[j]);
                OpenMM_DoubleArray_set(values, addIndex++, OpenMM_KJPerKcal * dxy[j]);
                OpenMM_3D_DoubleArray_set(grid3D, yIndex, xIndex, values);
                xIndex++;
                if (xIndex == nx) {
                    xIndex = 0;
                    yIndex++;
                }
            }
            OpenMM_AmoebaTorsionTorsionForce_setTorsionTorsionGrid(amoebaTorsionTorsionForce, gridIndex++, grid3D);
            OpenMM_3D_DoubleArray_destroy(grid3D);
        }
        OpenMM_DoubleArray_destroy(values);
        OpenMM_System_addForce(system, amoebaTorsionTorsionForce);
        logger.log(Level.INFO, " Added Torsion-Torsions ({0})", nTorsionTorsions);
    }

    /**
     * Uses arithmetic mean to define sigma and geometric mean for epsilon.
     */
    private void addFixedChargeNonBondedForce() {
        VanDerWaals vdW = super.getVdwNode();
        if (vdW == null) {
            return;
        }
        /**
         * Only 6-12 LJ with arithmetic mean to define sigma and geometric mean
         * for epsilon is supported.
         */
        VanDerWaalsForm vdwForm = vdW.getVDWForm();
        if (vdwForm.vdwType != LENNARD_JONES
                || vdwForm.radiusRule != ARITHMETIC
                || vdwForm.epsilonRule != GEOMETRIC) {
            logger.info(format(" VDW Type:         %s", vdwForm.vdwType));
            logger.info(format(" VDW Radius Rule:  %s", vdwForm.radiusRule));
            logger.info(format(" VDW Epsilon Rule: %s", vdwForm.epsilonRule));
            logger.log(Level.SEVERE, String.format(" Unsuppporterd van der Waals functional form."));
            return;
        }

        fixedChargeNonBondedForce = OpenMM_NonbondedForce_create();

        /**
         * OpenMM vdW force requires a diameter (i.e. not radius).
         */
        double radScale = 1.0;
        if (vdwForm.radiusSize == RADIUS) {
            radScale = 2.0;
        }
        /**
         * OpenMM vdw force requires atomic sigma values (i.e. not r-min).
         */
        if (vdwForm.radiusType == R_MIN) {
            radScale /= 1.122462048309372981;
        }

        /**
         * Add particles.
         */
        Atom[] atoms = molecularAssembly.getAtomArray();
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            Atom atom = atoms[i];

            VDWType vdwType = atom.getVDWType();
            double sigma = OpenMM_NmPerAngstrom * vdwType.radius * radScale;
            double eps = OpenMM_KJPerKcal * vdwType.wellDepth;

            double charge = 0.0;
            MultipoleType multipoleType = atom.getMultipoleType();
            if (multipoleType != null && atoms[i].getElectrostatics()) {
                charge = multipoleType.charge;
            }

            OpenMM_NonbondedForce_addParticle(fixedChargeNonBondedForce, charge, sigma, eps);
        }

        /**
         * Define 1-4 scale factors.
         */
        double lj14Scale = vdwForm.getScale14();
        double coulomb14Scale = 1.0 / 1.2;

        ParticleMeshEwald pme = super.getPmeNode();
        Bond bonds[] = super.getBonds();
        if (bonds != null && bonds.length > 0) {
            int nBonds = bonds.length;
            PointerByReference bondArray;
            bondArray = OpenMM_BondArray_create(0);
            for (int i = 0; i < nBonds; i++) {
                Bond bond = bonds[i];
                int i1 = bond.getAtom(0).getXyzIndex() - 1;
                int i2 = bond.getAtom(1).getXyzIndex() - 1;
                OpenMM_BondArray_append(bondArray, i1, i2);
            }
            if (pme != null) {
                coulomb14Scale = pme.getScale14();
            }
            OpenMM_NonbondedForce_createExceptionsFromBonds(fixedChargeNonBondedForce, bondArray, coulomb14Scale, lj14Scale);

            OpenMM_BondArray_destroy(bondArray);

            int num = OpenMM_NonbondedForce_getNumExceptions(fixedChargeNonBondedForce);
            chargeExclusion = new boolean[num];
            vdWExclusion = new boolean[num];
            exceptionChargeProd = new double[num];
            exceptionEps = new double[num];

            IntByReference particle1 = new IntByReference();
            IntByReference particle2 = new IntByReference();
            DoubleByReference chargeProd = new DoubleByReference();
            DoubleByReference sigma = new DoubleByReference();
            DoubleByReference eps = new DoubleByReference();

            for (int i = 0; i < num; i++) {
                OpenMM_NonbondedForce_getExceptionParameters(fixedChargeNonBondedForce, i,
                        particle1, particle2, chargeProd, sigma, eps);
                if (abs(chargeProd.getValue()) > 0.0) {
                    chargeExclusion[i] = false;
                    exceptionChargeProd[i] = chargeProd.getValue();
                } else {
                    exceptionChargeProd[i] = 0.0;
                    chargeExclusion[i] = true;
                }
                if (abs(eps.getValue()) > 0.0) {
                    vdWExclusion[i] = false;
                    exceptionEps[i] = eps.getValue();
                } else {
                    vdWExclusion[i] = true;
                    exceptionEps[i] = 0.0;
                }
            }
        }

        Crystal crystal = super.getCrystal();
        if (crystal.aperiodic()) {
            OpenMM_NonbondedForce_setNonbondedMethod(fixedChargeNonBondedForce,
                    OpenMM_NonbondedForce_NonbondedMethod.OpenMM_NonbondedForce_NoCutoff);
        } else {
            OpenMM_NonbondedForce_setNonbondedMethod(fixedChargeNonBondedForce,
                    OpenMM_NonbondedForce_NonbondedMethod.OpenMM_NonbondedForce_PME);

            if (pme != null) {
                // Units of the Ewald coefficient are A^-1; Multiply by AngstromsPerNM to convert to (Nm^-1).
                double aEwald = OpenMM_AngstromsPerNm * pme.getEwaldCoefficient();
                int nx = pme.getReciprocalSpace().getXDim();
                int ny = pme.getReciprocalSpace().getYDim();
                int nz = pme.getReciprocalSpace().getZDim();
                OpenMM_NonbondedForce_setPMEParameters(fixedChargeNonBondedForce, aEwald, nx, ny, nz);
            }

            NonbondedCutoff nonbondedCutoff = vdW.getNonbondedCutoff();
            double off = nonbondedCutoff.off;
            double cut = nonbondedCutoff.cut;
            OpenMM_NonbondedForce_setCutoffDistance(fixedChargeNonBondedForce, OpenMM_NmPerAngstrom * off);
            OpenMM_NonbondedForce_setUseSwitchingFunction(fixedChargeNonBondedForce, OpenMM_True);
            if (cut == off) {
                logger.warning(" OpenMM does not properly handle cutoffs where cut == off!");
                if (cut == Double.MAX_VALUE || cut == Double.POSITIVE_INFINITY) {
                    logger.info(" Detected infinite or max-value cutoff; setting cut to 1E+40 for OpenMM.");
                    cut = 1E40;
                } else {
                    logger.info(String.format(" Detected cut %8.4g == off %8.4g; scaling cut to 0.99 of off for OpenMM.", cut, off));
                    cut *= 0.99;
                }
            }
            OpenMM_NonbondedForce_setSwitchingDistance(fixedChargeNonBondedForce, OpenMM_NmPerAngstrom * cut);
        }

        OpenMM_NonbondedForce_setUseDispersionCorrection(fixedChargeNonBondedForce, OpenMM_False);

        // OpenMM_Force_setForceGroup(fixedChargeNonBondedForce, 1);
        OpenMM_System_addForce(system, fixedChargeNonBondedForce);
        logger.log(Level.INFO, String.format(" Added fixed charge non-bonded force."));

        GeneralizedKirkwood gk = super.getGK();
        if (gk != null) {
            addCustomGBForce();
        }
    }

    /**
     * 1.) Handle interactions between non-alchemical atoms with our default
     * OpenMM NonBondedForce. Note that alchemical atoms must have eps=0 to turn
     * them off in this force.
     * <p>
     * 2.) Handle interactions between alchemical atoms and mixed non-alchemical
     * <-> alchemical interactions with an OpenMM CustomNonBondedForce.
     */
    private void addCustomNonbondedSoftcoreForce() {

        VanDerWaals vdW = super.getVdwNode();
        if (vdW == null) {
            return;
        }

        /**
         * Only 6-12 LJ with arithmetic mean to define sigma and geometric mean
         * for epsilon is supported.
         */
        VanDerWaalsForm vdwForm = vdW.getVDWForm();
        if (vdwForm.vdwType != LENNARD_JONES
                || vdwForm.radiusRule != ARITHMETIC
                || vdwForm.epsilonRule != GEOMETRIC) {
            logger.info(format(" VDW Type:         %s", vdwForm.vdwType));
            logger.info(format(" VDW Radius Rule:  %s", vdwForm.radiusRule));
            logger.info(format(" VDW Epsilon Rule: %s", vdwForm.epsilonRule));
            logger.log(Level.SEVERE, String.format(" Unsuppporterd van der Waals functional form."));
            return;
        }

        // Sterics mixing rules.
        String stericsMixingRules = " epsilon = sqrt(epsilon1*epsilon2);";
        stericsMixingRules += " rmin = 0.5 * (sigma1 + sigma2) * 1.122462048309372981;";

        // Softcore Lennard-Jones, with a form equivalent to that used in FFX VanDerWaals class.
        String stericsEnergyExpression = "(vdw_lambda^beta)*epsilon*x*(x-2.0);";
        // Effective softcore distance for sterics.
        stericsEnergyExpression += " x = 1.0 / (alpha*(1.0-vdw_lambda)^2.0 + (r/rmin)^6.0);";
        // Define energy expression for sterics.
        String energyExpression = stericsEnergyExpression + stericsMixingRules;

        fixedChargeSoftcore = OpenMM_CustomNonbondedForce_create(energyExpression);

        // Get the Alpha and Beta constants from the VanDerWaals instance.
        double alpha = vdW.getAlpha();
        double beta = vdW.getBeta();

        logger.info(format(" Custom non-bonded force with alpha = %8.6f and beta = %8.6f", alpha, beta));

        OpenMM_CustomNonbondedForce_addGlobalParameter(fixedChargeSoftcore, "vdw_lambda", 1.0);
        OpenMM_CustomNonbondedForce_addGlobalParameter(fixedChargeSoftcore, "alpha", alpha);
        OpenMM_CustomNonbondedForce_addGlobalParameter(fixedChargeSoftcore, "beta", beta);
        OpenMM_CustomNonbondedForce_addPerParticleParameter(fixedChargeSoftcore, "sigma");
        OpenMM_CustomNonbondedForce_addPerParticleParameter(fixedChargeSoftcore, "epsilon");

        /**
         * Add particles.
         */
        PointerByReference alchemicalGroup = OpenMM_IntSet_create();
        PointerByReference nonAlchemicalGroup = OpenMM_IntSet_create();
        DoubleByReference charge = new DoubleByReference();
        DoubleByReference sigma = new DoubleByReference();
        DoubleByReference eps = new DoubleByReference();

        Atom[] atoms = molecularAssembly.getAtomArray();
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            Atom atom = atoms[i];
            OpenMM_NonbondedForce_getParticleParameters(fixedChargeNonBondedForce, i, charge, sigma, eps);
            if (atom.applyLambda()) {
                OpenMM_IntSet_insert(alchemicalGroup, i);
                logger.info(format(" Adding alchemical atom %s.", atom));
            } else {
                OpenMM_IntSet_insert(nonAlchemicalGroup, i);
            }
            PointerByReference particleParameters = OpenMM_DoubleArray_create(0);
            OpenMM_DoubleArray_append(particleParameters, sigma.getValue());
            OpenMM_DoubleArray_append(particleParameters, eps.getValue());
            OpenMM_CustomNonbondedForce_addParticle(fixedChargeSoftcore, particleParameters);
            OpenMM_DoubleArray_destroy(particleParameters);
        }

        OpenMM_CustomNonbondedForce_addInteractionGroup(fixedChargeSoftcore, alchemicalGroup, alchemicalGroup);
        OpenMM_CustomNonbondedForce_addInteractionGroup(fixedChargeSoftcore, alchemicalGroup, nonAlchemicalGroup);
        OpenMM_IntSet_destroy(alchemicalGroup);
        OpenMM_IntSet_destroy(nonAlchemicalGroup);

        Crystal crystal = super.getCrystal();
        if (crystal.aperiodic()) {
            OpenMM_CustomNonbondedForce_setNonbondedMethod(fixedChargeSoftcore,
                    OpenMM_CustomNonbondedForce_NonbondedMethod.OpenMM_CustomNonbondedForce_NoCutoff);
        } else {
            OpenMM_CustomNonbondedForce_setNonbondedMethod(fixedChargeSoftcore,
                    OpenMM_CustomNonbondedForce_NonbondedMethod.OpenMM_CustomNonbondedForce_CutoffPeriodic);
        }

        NonbondedCutoff nonbondedCutoff = vdW.getNonbondedCutoff();
        double off = nonbondedCutoff.off;
        double cut = nonbondedCutoff.cut;

        OpenMM_CustomNonbondedForce_setCutoffDistance(fixedChargeSoftcore, OpenMM_NmPerAngstrom * off);
        OpenMM_CustomNonbondedForce_setUseSwitchingFunction(fixedChargeSoftcore, OpenMM_True);
        OpenMM_CustomNonbondedForce_setSwitchingDistance(fixedChargeSoftcore, OpenMM_NmPerAngstrom * cut);

        if (cut == off) {
            logger.warning(" OpenMM does not properly handle cutoffs where cut == off!");
            if (cut == Double.MAX_VALUE || cut == Double.POSITIVE_INFINITY) {
                logger.info(" Detected infinite or max-value cutoff; setting cut to 1E+40 for OpenMM.");
                cut = 1E40;
            } else {
                logger.info(String.format(" Detected cut %8.4g == off %8.4g; scaling cut to 0.99 of off for OpenMM.", cut, off));
                cut *= 0.99;
            }
        }

        // Add energy parameter derivative
        OpenMM_CustomNonbondedForce_addEnergyParameterDerivative(fixedChargeSoftcore, "vdw_lambda");

        OpenMM_System_addForce(system, fixedChargeSoftcore);
        logger.log(Level.INFO, String.format(" Added fixed charge softcore sterics force."));

        GeneralizedKirkwood gk = super.getGK();
        if (gk != null) {
            logger.severe(" OpenMM alchemical methods are not supported for GB.");
            addCustomGBForce();
        }

        // Not entirely sure how to initialize this portion
        alchemicalAlchemicalStericsForce = OpenMM_CustomBondForce_create(stericsEnergyExpression);
        
        // nonAlchemicalAlchemicalStericsForce = OpenMM_CustomBondForce_create(stericsEnergyExpression);
        // allStericsForce = (alchemicalAlchemicalStericsForce + nonAlchemicalAlchemicalStericsForce);

        // Can be reduced to two lines if I can figure out how to combine the two custom bonded sterics forces
        OpenMM_CustomBondForce_addPerBondParameter(alchemicalAlchemicalStericsForce, "rmin");
        OpenMM_CustomBondForce_addPerBondParameter(alchemicalAlchemicalStericsForce, "epsilon");
        OpenMM_CustomBondForce_addGlobalParameter(alchemicalAlchemicalStericsForce, "vdw_lambda", 1.0);
        OpenMM_CustomBondForce_addGlobalParameter(alchemicalAlchemicalStericsForce, "alpha", alpha);
        OpenMM_CustomBondForce_addGlobalParameter(alchemicalAlchemicalStericsForce, "beta", beta);
        
        //OpenMM_CustomBondForce_addPerBondParameter(nonAlchemicalAlchemicalStericsForce, "sigma");
        //OpenMM_CustomBondForce_addPerBondParameter(nonAlchemicalAlchemicalStericsForce, "epsilon");

        int range = OpenMM_NonbondedForce_getNumExceptions(fixedChargeNonBondedForce);

        IntByReference atomi = new IntByReference();
        IntByReference atomj = new IntByReference();
        int torsionMask[][] = vdW.getTorsionMask();

        for (int i = 0; i < range; i++) {
            
            OpenMM_NonbondedForce_getExceptionParameters(fixedChargeNonBondedForce, i, atomi, atomj, charge, sigma, eps);
            
            OpenMM_CustomNonbondedForce_addExclusion(fixedChargeSoftcore, atomi.getValue(), atomj.getValue());
 
            int maskI[] = torsionMask[atomi.getValue()];

            int jID = atomj.getValue();
            boolean epsException = false;
            for (int j=0; j<maskI.length; j++) {
                if (maskI[j] == jID) {
                    epsException = true;
                    break;
                }
            }
            
            Atom atom1 = atoms[atomi.getValue()];
            Atom atom2 = atoms[atomj.getValue()];

            boolean bothAlchemical = false;
            boolean oneAlchemical = false;

            if (atom1.applyLambda() && atom2.applyLambda()) {
                bothAlchemical = true;
            } else if ((atom1.applyLambda() && !atom2.applyLambda()) || (!atom1.applyLambda() && atom2.applyLambda())) {
                oneAlchemical = true;
            }

            //logger.info(String.format( " about to enter bothAlchemical if statement"));
            
            if (bothAlchemical) {
                if (epsException) {
                    PointerByReference bondParameters = OpenMM_DoubleArray_create(0);
                    //OpenMM_DoubleArray_append(bondParameters, charge.getValue());
                    OpenMM_DoubleArray_append(bondParameters, sigma.getValue() * 1.122462048309372981);
                    OpenMM_DoubleArray_append(bondParameters, eps.getValue());
                    OpenMM_CustomBondForce_addBond(alchemicalAlchemicalStericsForce, atomi.getValue(), atomj.getValue(), bondParameters);
                    OpenMM_DoubleArray_destroy(bondParameters);
                }
            } 
            
            /**  
            else if (oneAlchemical){
                if(epsException){
                    PointerByReference bondParameters = OpenMM_DoubleArray_create(0);
                    OpenMM_DoubleArray_append(bondParameters, sigma.getValue());
                    OpenMM_DoubleArray_append(bondParameters, eps.getValue());
                    OpenMM_CustomBondForce_addBond(nonAlchemicalAlchemicalStericsForce, atomi.getValue(), atomj.getValue(), bondParameters);
                    OpenMM_DoubleArray_destroy(bondParameters);
                }
            } */
        }
        
         OpenMM_CustomBondForce_addEnergyParameterDerivative(alchemicalAlchemicalStericsForce, "vdw_lambda");
        
        OpenMM_System_addForce(system, alchemicalAlchemicalStericsForce);
        

    }

    private void addCustomGBForce() {
        GeneralizedKirkwood gk = super.getGK();
        if (gk == null) {
            return;
        }

        customGBForce = OpenMM_CustomGBForce_create();
        OpenMM_CustomGBForce_addPerParticleParameter(customGBForce, "q");
        OpenMM_CustomGBForce_addPerParticleParameter(customGBForce, "radius");
        OpenMM_CustomGBForce_addPerParticleParameter(customGBForce, "scale");
        OpenMM_CustomGBForce_addGlobalParameter(customGBForce, "solventDielectric", 78.3);
        OpenMM_CustomGBForce_addGlobalParameter(customGBForce, "soluteDielectric", 1.0);
        OpenMM_CustomGBForce_addGlobalParameter(customGBForce, "dOffset", gk.getDielecOffset() * OpenMM_NmPerAngstrom); // Factor of 0.1 for Ang to nm.
        OpenMM_CustomGBForce_addComputedValue(customGBForce, "I",
                // "step(r+sr2-or1)*0.5*(1/L-1/U+0.25*(1/U^2-1/L^2)*(r-sr2*sr2/r)+0.5*log(L/U)/r+C);"
                // "step(r+sr2-or1)*0.5*((1/L^3-1/U^3)/3+(1/U^4-1/L^4)/8*(r-sr2*sr2/r)+0.25*(1/U^2-1/L^2)/r+C);"
                "0.5*((1/L^3-1/U^3)/3.0+(1/U^4-1/L^4)/8.0*(r-sr2*sr2/r)+0.25*(1/U^2-1/L^2)/r+C);"
                + "U=r+sr2;"
                // + "C=2*(1/or1-1/L)*step(sr2-r-or1);"
                + "C=2/3*(1/or1^3-1/L^3)*step(sr2-r-or1);"
                // + "L=step(or1-D)*or1 + (1-step(or1-D))*D;"
                // + "D=step(r-sr2)*(r-sr2) + (1-step(r-sr2))*(sr2-r);"
                + "L = step(sr2 - r1r)*sr2mr + (1 - step(sr2 - r1r))*L;"
                + "sr2mr = sr2 - r;"
                + "r1r = radius1 + r;"
                + "L = step(r1sr2 - r)*radius1 + (1 - step(r1sr2 - r))*L;"
                + "r1sr2 = radius1 + sr2;"
                + "L = r - sr2;"
                + "sr2 = scale2 * radius2;"
                + "or1 = radius1; or2 = radius2",
                OpenMM_CustomGBForce_ParticlePairNoExclusions);

        OpenMM_CustomGBForce_addComputedValue(customGBForce, "B",
                // "1/(1/or-tanh(1*psi-0.8*psi^2+4.85*psi^3)/radius);"
                // "psi=I*or; or=radius-0.009"
                "step(BB-radius)*BB + (1 - step(BB-radius))*radius;"
                + "BB = 1 / ( (3.0*III)^(1.0/3.0) );"
                + "III = step(II)*II + (1 - step(II))*1.0e-9/3.0;"
                + "II = maxI - I;"
                + "maxI = 1/(3.0*radius^3)",
                OpenMM_CustomGBForce_SingleParticle);

        double sTens = gk.getSurfaceTension();
        logger.info(String.format(" FFX surface tension: %9.5g kcal/mol/Ang^2", sTens));
        sTens *= OpenMM_KJPerKcal;
        sTens *= 100.0; // 100 square Angstroms per square nanometer.
        logger.info(String.format(" OpenMM surface tension: %9.5g kJ/mol/nm^2", sTens));
        String surfaceTension = Double.toString(sTens);

        OpenMM_CustomGBForce_addEnergyTerm(customGBForce,
                surfaceTension
                + "*(radius+0.14+dOffset)^2*((radius+dOffset)/B)^6/6-0.5*138.935456*(1/soluteDielectric-1/solventDielectric)*q^2/B",
                OpenMM_CustomGBForce_SingleParticle);

        /**
         * Particle pair term is the generalized Born cross term.
         */
        OpenMM_CustomGBForce_addEnergyTerm(customGBForce,
                "-138.935456*(1/soluteDielectric-1/solventDielectric)*q1*q2/f;"
                + "f=sqrt(r^2+B1*B2*exp(-r^2/(2.455*B1*B2)))",
                OpenMM_CustomGBForce_ParticlePair);

        double baseRadii[] = gk.getBaseRadii();
        double overlapScale[] = gk.getOverlapScale();
        Atom[] atoms = molecularAssembly.getAtomArray();
        int nAtoms = atoms.length;
        PointerByReference doubleArray = OpenMM_DoubleArray_create(0);
        for (int i = 0; i < nAtoms; i++) {
            MultipoleType multipoleType = atoms[i].getMultipoleType();
            OpenMM_DoubleArray_append(doubleArray, multipoleType.charge);
            OpenMM_DoubleArray_append(doubleArray, OpenMM_NmPerAngstrom * baseRadii[i]);
            OpenMM_DoubleArray_append(doubleArray, overlapScale[i]);
            OpenMM_CustomGBForce_addParticle(customGBForce, doubleArray);
            OpenMM_DoubleArray_resize(doubleArray, 0);
        }
        OpenMM_DoubleArray_destroy(doubleArray);

        double cut = gk.getCutoff();
        OpenMM_CustomGBForce_setCutoffDistance(customGBForce, cut);
        OpenMM_Force_setForceGroup(customGBForce, 1);
        OpenMM_System_addForce(system, customGBForce);

        logger.log(Level.INFO, " Added generalized Born force");
    }

    private void addAmoebaVDWForce() {
        VanDerWaals vdW = super.getVdwNode();
        if (vdW == null) {
            return;
        }

        amoebaVDWForce = OpenMM_AmoebaVdwForce_create();
        OpenMM_System_addForce(system, amoebaVDWForce);
        OpenMM_Force_setForceGroup(amoebaVDWForce, 1);

        VanDerWaalsForm vdwForm = vdW.getVDWForm();
        NonbondedCutoff nonbondedCutoff = vdW.getNonbondedCutoff();
        Crystal crystal = super.getCrystal();

        double radScale = 1.0;
        if (vdwForm.radiusSize == VanDerWaalsForm.RADIUS_SIZE.DIAMETER) {
            radScale = 0.5;
        }

        /**
         * Note that the API says it wants a SIGMA value.
         */
        if (vdwForm.radiusType == VanDerWaalsForm.RADIUS_TYPE.R_MIN) {
            //radScale *= 1.122462048309372981;
        }

        int ired[] = vdW.getReductionIndex();
        Atom[] atoms = molecularAssembly.getAtomArray();
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            Atom atom = atoms[i];
            VDWType vdwType = atom.getVDWType();
            OpenMM_AmoebaVdwForce_addParticle(amoebaVDWForce,
                    ired[i], OpenMM_NmPerAngstrom * vdwType.radius * radScale,
                    OpenMM_KJPerKcal * vdwType.wellDepth,
                    vdwType.reductionFactor);
        }

        // OpenMM_AmoebaVdwForce_setSigmaCombiningRule(amoebaVdwForce, toPropertyForm(vdwForm.radiusRule.name()));
        // OpenMM_AmoebaVdwForce_setEpsilonCombiningRule(amoebaVdwForce, toPropertyForm(vdwForm.epsilonRule.name()));
        OpenMM_AmoebaVdwForce_setCutoffDistance(amoebaVDWForce, nonbondedCutoff.off * OpenMM_NmPerAngstrom);
        OpenMM_AmoebaVdwForce_setUseDispersionCorrection(amoebaVDWForce, OpenMM_Boolean.OpenMM_False);

        if (crystal.aperiodic()) {
            OpenMM_AmoebaVdwForce_setNonbondedMethod(amoebaVDWForce,
                    OpenMM_AmoebaVdwForce_NonbondedMethod.OpenMM_AmoebaVdwForce_NoCutoff);
        } else {
            OpenMM_AmoebaVdwForce_setNonbondedMethod(amoebaVDWForce,
                    OpenMM_AmoebaVdwForce_NonbondedMethod.OpenMM_AmoebaVdwForce_CutoffPeriodic);
        }

        /**
         * Create exclusion lists.
         */
        PointerByReference exclusions = OpenMM_IntArray_create(0);
        double mask[] = new double[nAtoms];
        Arrays.fill(mask, 1.0);
        for (int i = 0; i < nAtoms; i++) {
            OpenMM_IntArray_append(exclusions, i);
            vdW.applyMask(mask, i);
            for (int j = 0; j < nAtoms; j++) {
                if (mask[j] == 0.0) {
                    OpenMM_IntArray_append(exclusions, j);
                }
            }
            vdW.removeMask(mask, i);
            OpenMM_AmoebaVdwForce_setParticleExclusions(amoebaVDWForce, i, exclusions);
            OpenMM_IntArray_resize(exclusions, 0);
        }
        OpenMM_IntArray_destroy(exclusions);
        logger.log(Level.INFO, " Added van der Waals force.");
    }

    /**
     * Experimental. Virtual hydrogen sites require creation of new particles,
     * which then need to be handled (ignored?) for the multiple force.
     */
    private void createVirtualHydrogenSites() {

        VanDerWaals vdW = super.getVdwNode();
        if (vdW == null) {
            return;
        }
        int ired[] = vdW.getReductionIndex();

        Atom[] atoms = molecularAssembly.getAtomArray();
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            Atom atom = atoms[i];
            VDWType vdwType = atom.getVDWType();
            if (vdwType.reductionFactor < 1.0) {
                double factor = vdwType.reductionFactor;
                // Create the virtual site.
                PointerByReference virtualSite = OpenMM_TwoParticleAverageSite_create(i, ired[i], factor, 1.0 - factor);
                // Create a massless particle for the hydrogen vdW site.
                int id = OpenMM_System_addParticle(system, 0.0);
                // Denote the massless particle is a virtual site
                OpenMM_System_setVirtualSite(system, id, virtualSite);
            }
        }
    }

    private void addAmoebaMultipoleForce() {
        ParticleMeshEwald pme = super.getPmeNode();
        if (pme == null) {
            return;
        }
        int axisAtom[][] = pme.getAxisAtoms();
        double dipoleConversion = OpenMM_NmPerAngstrom;
        double quadrupoleConversion = OpenMM_NmPerAngstrom * OpenMM_NmPerAngstrom;
        double polarityConversion = OpenMM_NmPerAngstrom * OpenMM_NmPerAngstrom
                * OpenMM_NmPerAngstrom;
        double dampingFactorConversion = sqrt(OpenMM_NmPerAngstrom);

        amoebaMultipoleForce = OpenMM_AmoebaMultipoleForce_create();

        double polarScale = 1.0;
        if (pme.getPolarizationType() != Polarization.MUTUAL) {
            OpenMM_AmoebaMultipoleForce_setPolarizationType(amoebaMultipoleForce, OpenMM_AmoebaMultipoleForce_Direct);
            if (pme.getPolarizationType() == Polarization.NONE) {
                polarScale = 0.0;
            }
        } else {
            ForceField forceField = molecularAssembly.getForceField();
            String algorithm = forceField.getString(ForceField.ForceFieldString.SCF_ALGORITHM, "CG");
            ParticleMeshEwald.SCFAlgorithm scfAlgorithm;

            try {
                algorithm = algorithm.replaceAll("-", "_").toUpperCase();
                scfAlgorithm = ParticleMeshEwald.SCFAlgorithm.valueOf(algorithm);
            } catch (Exception e) {
                scfAlgorithm = ParticleMeshEwald.SCFAlgorithm.CG;
            }

            switch (scfAlgorithm) {
                case EPT:
                    logger.info(" Using extrapolated perturbation theory approximation instead of full SCF calculations. Not supported in FFX reference implementation.");
                    OpenMM_AmoebaMultipoleForce_setPolarizationType(amoebaMultipoleForce, OpenMM_AmoebaMultipoleForce_Extrapolated);
                    PointerByReference exptCoefficients = OpenMM_DoubleArray_create(4);
                    OpenMM_DoubleArray_set(exptCoefficients, 0, -0.154);
                    OpenMM_DoubleArray_set(exptCoefficients, 1, 0.017);
                    OpenMM_DoubleArray_set(exptCoefficients, 2, 0.657);
                    OpenMM_DoubleArray_set(exptCoefficients, 3, 0.475);
                    OpenMM_AmoebaMultipoleForce_setExtrapolationCoefficients(amoebaMultipoleForce, exptCoefficients);
                    OpenMM_DoubleArray_destroy(exptCoefficients);
                    break;
                case CG:
                case SOR:
                default:
                    OpenMM_AmoebaMultipoleForce_setPolarizationType(amoebaMultipoleForce, OpenMM_AmoebaMultipoleForce_Mutual);
                    break;
            }
        }

        PointerByReference dipoles = OpenMM_DoubleArray_create(3);
        PointerByReference quadrupoles = OpenMM_DoubleArray_create(9);

        Atom[] atoms = molecularAssembly.getAtomArray();
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            Atom atom = atoms[i];
            MultipoleType multipoleType = atom.getMultipoleType();
            PolarizeType polarType = atom.getPolarizeType();

            /**
             * Define the frame definition.
             */
            int axisType = OpenMM_AmoebaMultipoleForce_NoAxisType;
            switch (multipoleType.frameDefinition) {
                case ZONLY:
                    axisType = OpenMM_AmoebaMultipoleForce_ZOnly;
                    break;
                case ZTHENX:
                    axisType = OpenMM_AmoebaMultipoleForce_ZThenX;
                    break;
                case BISECTOR:
                    axisType = OpenMM_AmoebaMultipoleForce_Bisector;
                    break;
                case ZTHENBISECTOR:
                    axisType = OpenMM_AmoebaMultipoleForce_ZBisect;
                    break;
                case TRISECTOR:
                    axisType = OpenMM_AmoebaMultipoleForce_ThreeFold;
                    break;
                default:
                    break;
            }

            double useFactor = 1.0;
            if (!atoms[i].getUse() || !atoms[i].getElectrostatics()) {
                //if (!atoms[i].getUse()) {
                useFactor = 0.0;
            }

            double lambdaScale = lambda; // Should be 1.0 at this point.
            if (!atom.applyLambda()) {
                lambdaScale = 1.0;
            }

            useFactor *= lambdaScale;

            /**
             * Load local multipole coefficients.
             */
            for (int j = 0; j < 3; j++) {
                OpenMM_DoubleArray_set(dipoles, j, multipoleType.dipole[j] * dipoleConversion * useFactor);

            }
            int l = 0;
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    OpenMM_DoubleArray_set(quadrupoles, l++, multipoleType.quadrupole[j][k] * quadrupoleConversion * useFactor / 3.0);
                }
            }

            //int zaxis = 0;
            int zaxis = 1;
            //int xaxis = 0;
            int xaxis = 1;
            //int yaxis = 0;
            int yaxis = 1;
            int refAtoms[] = axisAtom[i];
            if (refAtoms != null) {
                zaxis = refAtoms[0];
                if (refAtoms.length > 1) {
                    xaxis = refAtoms[1];
                    if (refAtoms.length > 2) {
                        yaxis = refAtoms[2];
                    }
                }
            } else {

                axisType = OpenMM_AmoebaMultipoleForce_NoAxisType;
                logger.info(String.format(" Atom type %s", atom.getAtomType().toString()));
            }

            double charge = multipoleType.charge * useFactor;

            /**
             * Add the multipole.
             */
            OpenMM_AmoebaMultipoleForce_addMultipole(amoebaMultipoleForce,
                    charge, dipoles, quadrupoles,
                    axisType, zaxis, xaxis, yaxis,
                    polarType.thole,
                    polarType.pdamp * dampingFactorConversion,
                    polarType.polarizability * polarityConversion * polarScale);
        }
        OpenMM_DoubleArray_destroy(dipoles);
        OpenMM_DoubleArray_destroy(quadrupoles);

        Crystal crystal = super.getCrystal();
        if (!crystal.aperiodic()) {
            OpenMM_AmoebaMultipoleForce_setNonbondedMethod(amoebaMultipoleForce, OpenMM_AmoebaMultipoleForce_PME);
            OpenMM_AmoebaMultipoleForce_setCutoffDistance(amoebaMultipoleForce,
                    pme.getEwaldCutoff() * OpenMM_NmPerAngstrom);
            OpenMM_AmoebaMultipoleForce_setAEwald(amoebaMultipoleForce,
                    pme.getEwaldCoefficient() / OpenMM_NmPerAngstrom);

            double ewaldTolerance = 1.0e-04;
            OpenMM_AmoebaMultipoleForce_setEwaldErrorTolerance(amoebaMultipoleForce, ewaldTolerance);

            PointerByReference gridDimensions = OpenMM_IntArray_create(3);
            ReciprocalSpace recip = pme.getReciprocalSpace();
            OpenMM_IntArray_set(gridDimensions, 0, recip.getXDim());
            OpenMM_IntArray_set(gridDimensions, 1, recip.getYDim());
            OpenMM_IntArray_set(gridDimensions, 2, recip.getZDim());
            OpenMM_AmoebaMultipoleForce_setPmeGridDimensions(amoebaMultipoleForce, gridDimensions);
            OpenMM_IntArray_destroy(gridDimensions);
        } else {
            OpenMM_AmoebaMultipoleForce_setNonbondedMethod(amoebaMultipoleForce, OpenMM_AmoebaMultipoleForce_NoCutoff);
        }

        OpenMM_AmoebaMultipoleForce_setMutualInducedMaxIterations(amoebaMultipoleForce, 500);
        OpenMM_AmoebaMultipoleForce_setMutualInducedTargetEpsilon(amoebaMultipoleForce, pme.getPolarEps());

        int ip11[][] = pme.getPolarization11();
        int ip12[][] = pme.getPolarization12();
        int ip13[][] = pme.getPolarization13();

        ArrayList<Integer> list12 = new ArrayList<>();
        ArrayList<Integer> list13 = new ArrayList<>();
        ArrayList<Integer> list14 = new ArrayList<>();

        PointerByReference covalentMap = OpenMM_IntArray_create(0);
        for (int i = 0; i < nAtoms; i++) {
            Atom ai = atoms[i];
            list12.clear();
            list13.clear();
            list14.clear();

            for (Bond bond : ai.getBonds()) {
                int index = bond.get1_2(ai).getIndex() - 1;
                OpenMM_IntArray_append(covalentMap, index);
                list12.add(index);
            }
            OpenMM_AmoebaMultipoleForce_setCovalentMap(amoebaMultipoleForce, i,
                    OpenMM_AmoebaMultipoleForce_Covalent12, covalentMap);
            OpenMM_IntArray_resize(covalentMap, 0);

            for (Angle angle : ai.getAngles()) {
                Atom ak = angle.get1_3(ai);
                if (ak != null) {
                    int index = ak.getIndex() - 1;
                    if (!list12.contains(index)) {
                        list13.add(index);
                        OpenMM_IntArray_append(covalentMap, index);
                    }
                }
            }
            OpenMM_AmoebaMultipoleForce_setCovalentMap(amoebaMultipoleForce, i,
                    OpenMM_AmoebaMultipoleForce_Covalent13, covalentMap);
            OpenMM_IntArray_resize(covalentMap, 0);

            for (Torsion torsion : ai.getTorsions()) {
                Atom ak = torsion.get1_4(ai);
                if (ak != null) {
                    int index = ak.getIndex() - 1;
                    if (!list12.contains(index)
                            && !list13.contains(index)) {
                        list14.add(index);
                        OpenMM_IntArray_append(covalentMap, index);
                    }
                }
            }
            OpenMM_AmoebaMultipoleForce_setCovalentMap(amoebaMultipoleForce, i,
                    OpenMM_AmoebaMultipoleForce_Covalent14, covalentMap);
            OpenMM_IntArray_resize(covalentMap, 0);

            for (Atom ak : ai.get1_5s()) {
                int index = ak.getIndex() - 1;
                if (!list12.contains(index)
                        && !list13.contains(index)
                        && !list14.contains(index)) {
                    OpenMM_IntArray_append(covalentMap, index);
                }
            }
            OpenMM_AmoebaMultipoleForce_setCovalentMap(amoebaMultipoleForce, i,
                    OpenMM_AmoebaMultipoleForce_Covalent15, covalentMap);
            OpenMM_IntArray_resize(covalentMap, 0);

            for (int j = 0; j < ip11[i].length; j++) {
                OpenMM_IntArray_append(covalentMap, ip11[i][j]);
            }
            OpenMM_AmoebaMultipoleForce_setCovalentMap(amoebaMultipoleForce, i,
                    OpenMM_AmoebaMultipoleForce_PolarizationCovalent11, covalentMap);
            OpenMM_IntArray_resize(covalentMap, 0);

//            for (int j = 0; j < ip12[i].length; j++) {
//                OpenMM_IntArray_append(covalentMap, ip12[i][j]);
//            }
//            OpenMM_AmoebaMultipoleForce_setCovalentMap(amoebaMultipoleForce, i,
//                    OpenMM_AmoebaMultipoleForce_PolarizationCovalent12, covalentMap);
//            OpenMM_IntArray_resize(covalentMap, 0);
//
//            for (int j = 0; j < ip13[i].length; j++) {
//                OpenMM_IntArray_append(covalentMap, ip13[i][j]);
//            }
//            OpenMM_AmoebaMultipoleForce_setCovalentMap(amoebaMultipoleForce, i,
//                    OpenMM_AmoebaMultipoleForce_PolarizationCovalent13, covalentMap);
//            OpenMM_IntArray_resize(covalentMap, 0);
//
//            OpenMM_AmoebaMultipoleForce_setCovalentMap(amoebaMultipoleForce, i,
//                    OpenMM_AmoebaMultipoleForce_PolarizationCovalent14, covalentMap);
        }

        OpenMM_IntArray_destroy(covalentMap);

        OpenMM_System_addForce(system, amoebaMultipoleForce);
        OpenMM_Force_setForceGroup(amoebaMultipoleForce, 1);

        logger.log(Level.INFO, " Added polarizable multipole force.");

        GeneralizedKirkwood gk = super.getGK();
        if (gk != null) {
            addGKForce();
        }

    }

    private void addGKForce() {

        GeneralizedKirkwood gk = super.getGK();

        amoebaGeneralizedKirkwoodForce = OpenMM_AmoebaGeneralizedKirkwoodForce_create();
        OpenMM_AmoebaGeneralizedKirkwoodForce_setSolventDielectric(amoebaGeneralizedKirkwoodForce, 78.3);
        OpenMM_AmoebaGeneralizedKirkwoodForce_setSoluteDielectric(amoebaGeneralizedKirkwoodForce, 1.0);

        double overlapScale[] = gk.getOverlapScale();
        double baseRadii[] = gk.getBaseRadii();
        Atom[] atoms = molecularAssembly.getAtomArray();
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            MultipoleType multipoleType = atoms[i].getMultipoleType();
            OpenMM_AmoebaGeneralizedKirkwoodForce_addParticle(amoebaGeneralizedKirkwoodForce,
                    multipoleType.charge, OpenMM_NmPerAngstrom * baseRadii[i], overlapScale[i]);
        }

        OpenMM_AmoebaGeneralizedKirkwoodForce_setProbeRadius(amoebaGeneralizedKirkwoodForce, 1.4 * OpenMM_NmPerAngstrom);

        NonPolar nonpolar = gk.getNonPolarModel();
        switch (nonpolar) {
            case BORN_SOLV:
            case BORN_CAV_DISP:
            default:
                // Configure a Born Radii based surface area term.
                double surfaceTension = gk.getSurfaceTension() * OpenMM_KJPerKcal
                        * OpenMM_AngstromsPerNm * OpenMM_AngstromsPerNm;
                OpenMM_AmoebaGeneralizedKirkwoodForce_setIncludeCavityTerm(amoebaGeneralizedKirkwoodForce, OpenMM_True);
                OpenMM_AmoebaGeneralizedKirkwoodForce_setSurfaceAreaFactor(amoebaGeneralizedKirkwoodForce, -surfaceTension);
                break;
            case CAV:
            case CAV_DISP:
            case HYDROPHOBIC_PMF:
            case NONE:
                // This NonPolar model does not use a Born Radii based surface area term.
                OpenMM_AmoebaGeneralizedKirkwoodForce_setIncludeCavityTerm(amoebaGeneralizedKirkwoodForce, OpenMM_False);
                break;
        }
        OpenMM_System_addForce(system, amoebaGeneralizedKirkwoodForce);

        switch (nonpolar) {
            case CAV_DISP:
            case BORN_CAV_DISP:
                addWCAForce();
                break;
            case CAV:
            case HYDROPHOBIC_PMF:
            case BORN_SOLV:
            case NONE:
            default:
            // WCA force is not being used.
        }

        logger.log(Level.INFO, " Added generalized Kirkwood force.");
    }

    private void addWCAForce() {

        double epso = 0.1100;
        double epsh = 0.0135;
        double rmino = 1.7025;
        double rminh = 1.3275;
        double awater = 0.033428;
        double slevy = 1.0;
        double dispoff = 0.26;
        double shctd = 0.81;

        VanDerWaals vdW = super.getVdwNode();
        VanDerWaalsForm vdwForm = vdW.getVDWForm();
        double radScale = 1.0;
        if (vdwForm.radiusSize == VanDerWaalsForm.RADIUS_SIZE.DIAMETER) {
            radScale = 0.5;
        }

        amoebaWcaDispersionForce = OpenMM_AmoebaWcaDispersionForce_create();

        Atom[] atoms = molecularAssembly.getAtomArray();
        int nAtoms = atoms.length;

        for (int i = 0; i < nAtoms; i++) {
            // cdispTotal += nonpol__.cdisp[ii];
            Atom atom = atoms[i];
            VDWType vdwType = atom.getVDWType();
            double radius = vdwType.radius;
            double eps = vdwType.wellDepth;
            OpenMM_AmoebaWcaDispersionForce_addParticle(amoebaWcaDispersionForce,
                    OpenMM_NmPerAngstrom * radius * radScale,
                    OpenMM_KJPerKcal * eps);
        }

        OpenMM_AmoebaWcaDispersionForce_setEpso(amoebaWcaDispersionForce, epso * OpenMM_KJPerKcal);
        OpenMM_AmoebaWcaDispersionForce_setEpsh(amoebaWcaDispersionForce, epsh * OpenMM_KJPerKcal);
        OpenMM_AmoebaWcaDispersionForce_setRmino(amoebaWcaDispersionForce, rmino * OpenMM_NmPerAngstrom);
        OpenMM_AmoebaWcaDispersionForce_setRminh(amoebaWcaDispersionForce, rminh * OpenMM_NmPerAngstrom);
        OpenMM_AmoebaWcaDispersionForce_setDispoff(amoebaWcaDispersionForce, dispoff * OpenMM_NmPerAngstrom);
        OpenMM_AmoebaWcaDispersionForce_setAwater(amoebaWcaDispersionForce,
                awater / (OpenMM_NmPerAngstrom * OpenMM_NmPerAngstrom * OpenMM_NmPerAngstrom));
        OpenMM_AmoebaWcaDispersionForce_setSlevy(amoebaWcaDispersionForce, slevy);
        OpenMM_AmoebaWcaDispersionForce_setShctd(amoebaWcaDispersionForce, shctd);

        OpenMM_System_addForce(system, amoebaWcaDispersionForce);
        logger.log(Level.INFO, " Added WCA dispersion force.");

    }

    /**
     * Adds harmonic restraints (CoordRestraint objects) to OpenMM as a custom
     * external force.
     */
    private void addHarmonicRestraintForce() {
        for (CoordRestraint restraint : super.getCoordRestraints()) {
            double forceConst = restraint.getForceConstant();
            forceConst *= OpenMM_KJPerKcal;
            forceConst *= (OpenMM_AngstromsPerNm * OpenMM_AngstromsPerNm);
            Atom[] restAtoms = restraint.getAtoms();
            int nRestAts = restraint.getNumAtoms();
            double[][] oCoords = restraint.getOriginalCoordinates();
            for (int i = 0; i < nRestAts; i++) {
                oCoords[i][0] *= OpenMM_NmPerAngstrom;
                oCoords[i][1] *= OpenMM_NmPerAngstrom;
                oCoords[i][2] *= OpenMM_NmPerAngstrom;
            }

            PointerByReference theRestraint = OpenMM_CustomExternalForce_create("k*periodicdistance(x,y,z,x0,y0,z0)^2");
            OpenMM_CustomExternalForce_addGlobalParameter(theRestraint, "k", forceConst);
            OpenMM_CustomExternalForce_addPerParticleParameter(theRestraint, "x0");
            OpenMM_CustomExternalForce_addPerParticleParameter(theRestraint, "y0");
            OpenMM_CustomExternalForce_addPerParticleParameter(theRestraint, "z0");

            PointerByReference xyzOrigArray = OpenMM_DoubleArray_create(3);
            for (int i = 0; i < nRestAts; i++) {
                int ommIndex = restAtoms[i].getXyzIndex() - 1;
                for (int j = 0; j < 3; j++) {
                    OpenMM_DoubleArray_set(xyzOrigArray, j, oCoords[i][j]);
                }
                OpenMM_CustomExternalForce_addParticle(theRestraint, ommIndex, xyzOrigArray);
            }
            OpenMM_DoubleArray_destroy(xyzOrigArray);

            OpenMM_System_addForce(system, theRestraint);
        }
    }

    /**
     * Adds restraint bonds, if any.
     */
    private void addRestraintBonds() {
        List<RestraintBond> restraintBonds = super.getRestraintBonds();

        if (restraintBonds != null && !restraintBonds.isEmpty()) {
            harmonicBondForce = (harmonicBondForce == null) ? OpenMM_HarmonicBondForce_create() : harmonicBondForce;
            // OpenMM's HarmonicBondForce class uses k, not 1/2*k as does FFX.
            double kParameterConversion = BondType.units * 2.0 * OpenMM_KJPerKcal / (OpenMM_NmPerAngstrom * OpenMM_NmPerAngstrom);

            for (RestraintBond rbond : super.getRestraintBonds()) {
                Atom[] ats = rbond.getAtomArray();
                int at0 = ats[0].getXyzIndex() - 1;
                int at1 = ats[1].getXyzIndex() - 1;
                BondType bType = rbond.getBondType();

                double forceConst = bType.forceConstant * kParameterConversion;
                double equilDist = bType.distance * OpenMM_NmPerAngstrom;
                OpenMM_HarmonicBondForce_addBond(harmonicBondForce, at0, at1, equilDist, forceConst);
            }
            OpenMM_System_addForce(system, harmonicBondForce);
        }
    }

    /**
     * Update parameters if the Use flags changed.
     */
    private void updateParameters() {
        Atom[] atoms = molecularAssembly.getAtomArray();

        if (vdwLambdaTerm) {
            if (!softcoreCreated) {
                addCustomNonbondedSoftcoreForce();
                // Reset the context.
                createContext(integratorString, timeStep, temperature);
                OpenMM_Context_setParameter(context, "vdw_lambda", lambda);
                softcoreCreated = true;
                double energy = energy();
                logger.info(format(" OpenMM Energy (L=%6.3f): %16.8f", lambda, energy));
            } else {
                OpenMM_Context_setParameter(context, "vdw_lambda", lambda);
            }
        }

        // Update fixed charge non-bonded parameters.
        if (fixedChargeNonBondedForce != null) {
            updateFixedChargeNonBondedForce(atoms, vdwLambdaTerm);
        }

        // Update fixed charge GB parameters.
        if (customGBForce != null) {
            updateCustomGBForce(atoms);
        }

        // Update AMOEBA vdW parameters.
        if (amoebaVDWForce != null) {
            updateAmoebaVDWForce(atoms);
        }

        // Update AMOEBA polarizable multipole parameters.
        if (amoebaMultipoleForce != null) {
            updateAmoebaMultipoleForce(atoms);
        }

        // Update GK force.
        if (amoebaGeneralizedKirkwoodForce != null) {
            updateAmoebaGeneralizedKirkwoodForce(atoms);
        }

        // Update WCA Force.
        if (amoebaWcaDispersionForce != null) {
            updateWCAForce(atoms);
        }
    }

    /**
     * Updates the AMOEBA van der Waals force for change in Use flags.
     *
     * @param atoms Array of all Atoms in the system
     */
    private void updateAmoebaVDWForce(Atom[] atoms) {
        VanDerWaals vdW = super.getVdwNode();
        VanDerWaalsForm vdwForm = vdW.getVDWForm();

        double radScale = 1.0;
        if (vdwForm.radiusSize == VanDerWaalsForm.RADIUS_SIZE.DIAMETER) {
            radScale = 0.5;
        }

        /**
         * Note that the API says it wants a SIGMA value.
         */
        if (vdwForm.radiusType == VanDerWaalsForm.RADIUS_TYPE.R_MIN) {
            //radScale *= 1.122462048309372981;
        }

        int ired[] = vdW.getReductionIndex();
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            Atom atom = atoms[i];
            VDWType vdwType = atom.getVDWType();
            double useFactor = 1.0;
            if (!atoms[i].getUse()) {
                useFactor = 0.0;
            }
            double eps = OpenMM_KJPerKcal * vdwType.wellDepth * useFactor;
            OpenMM_AmoebaVdwForce_setParticleParameters(amoebaVDWForce,
                    i, ired[i], OpenMM_NmPerAngstrom * vdwType.radius * radScale,
                    eps, vdwType.reductionFactor);
        }
        OpenMM_AmoebaVdwForce_updateParametersInContext(amoebaVDWForce, context);
    }

    /**
     * Updates the fixed-charge non-bonded force for change in Use flags.
     *
     * @param atoms Array of all Atoms in the system
     * @param vdwLambdaTerm If true, set charges and eps values to zero for
     * Lambda atoms
     */
    private void updateFixedChargeNonBondedForce(Atom[] atoms, boolean vdwLambdaTerm) {
        VanDerWaals vdW = super.getVdwNode();
        /**
         * Only 6-12 LJ with arithmetic mean to define sigma and geometric mean
         * for epsilon is supported.
         */
        VanDerWaalsForm vdwForm = vdW.getVDWForm();
        if (vdwForm.vdwType != LENNARD_JONES
                || vdwForm.radiusRule != ARITHMETIC
                || vdwForm.epsilonRule != GEOMETRIC) {
            logger.log(Level.SEVERE, String.format(" Unsuppporterd van der Waals functional form."));
            return;
        }

        /**
         * OpenMM vdW force requires a diameter (i.e. not radius).
         */
        double radScale = 1.0;
        if (vdwForm.radiusSize == RADIUS) {
            radScale = 2.0;
        }
        /**
         * OpenMM vdw force requires atomic sigma values (i.e. not r-min).
         */
        if (vdwForm.radiusType == R_MIN) {
            radScale /= 1.122462048309372981;
        }

        /**
         * Update parameters.
         */
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            Atom atom = atoms[i];
            boolean applyLambda = atom.applyLambda();

            double charge = Double.MIN_VALUE;
            MultipoleType multipoleType = atom.getMultipoleType();
            if (multipoleType != null && atoms[i].getElectrostatics()) {
                charge = multipoleType.charge;
                if (lambdaTerm && applyLambda) {
                    charge *= lambda;
                }
            }

            VDWType vdwType = atom.getVDWType();
            double sigma = OpenMM_NmPerAngstrom * vdwType.radius * radScale;
            double eps = OpenMM_KJPerKcal * vdwType.wellDepth;

            if ((vdwLambdaTerm && applyLambda) || !atoms[i].getUse()) {
                eps = 0.0;
                charge = 0.0;
            }

            OpenMM_NonbondedForce_setParticleParameters(fixedChargeNonBondedForce, i, charge, sigma, eps);
        }

        // OpenMM_NonbondedForce_updateParametersInContext(fixedChargeNonBondedForce, context);
        /**
         * Update Exceptions.
         */
        IntByReference particle1 = new IntByReference();
        IntByReference particle2 = new IntByReference();
        DoubleByReference chargeProd = new DoubleByReference();
        DoubleByReference sigma = new DoubleByReference();
        DoubleByReference eps = new DoubleByReference();

        int numExceptions = OpenMM_NonbondedForce_getNumExceptions(fixedChargeNonBondedForce);

        for (int i = 0; i < numExceptions; i++) {

            /**
             * Only update exceptions.
             */
            if (chargeExclusion[i] && vdWExclusion[i]) {
                continue;
            }

            OpenMM_NonbondedForce_getExceptionParameters(fixedChargeNonBondedForce, i,
                    particle1, particle2, chargeProd, sigma, eps);

            int i1 = particle1.getValue();
            int i2 = particle2.getValue();

            double qq = exceptionChargeProd[i];
            double epsilon = exceptionEps[i];

            Atom atom1 = atoms[i1];
            Atom atom2 = atoms[i2];

            double lambdaValue = lambda;
            if (lambda == 0.0) {
                lambdaValue = 1.0e-6;
            }

            if (atom1.applyLambda()) {
                qq *= lambdaValue;
                if (vdwLambdaTerm) {
                    epsilon = 1.0e-6;
                    qq = 1.0e-6;
                }
            }

            if (atom2.applyLambda()) {
                qq *= lambdaValue;
                if (vdwLambdaTerm) {
                    epsilon = 1.0e-6;
                    qq = 1.0e-6;
                }
            }

            if (!atom1.getUse() || !atom2.getUse()) {
                qq = 1.0e-6;
                epsilon = 1.0e-6;
            }

            OpenMM_NonbondedForce_setExceptionParameters(fixedChargeNonBondedForce, i,
                    i1, i2, qq, sigma.getValue(), epsilon);

            /**
             * logger.info(format(" B Exception %d %d %d q=%10.8f s=%10.8f
             * e=%10.8f.", i, i1, i2, chargeProd.getValue(), sigma.getValue(),
             * eps.getValue()));
             *
             * logger.info(format(" E Exception %d %d %d q=%10.8f s=%10.8f
             * e=%10.8f.", i, i1, i2, qq, sigma.getValue(), epsilon));
             */
        }

        OpenMM_NonbondedForce_updateParametersInContext(fixedChargeNonBondedForce, context);
    }

    /**
     * Updates the custom GB force for change in Use flags.
     *
     * @param atoms Array of all Atoms in the system
     */
    private void updateCustomGBForce(Atom[] atoms) {
        GeneralizedKirkwood gk = super.getGK();
        double baseRadii[] = gk.getBaseRadii();
        double overlapScale[] = gk.getOverlapScale();
        PointerByReference doubleArray = OpenMM_DoubleArray_create(0);

        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            Atom atom = atoms[i];
            double useFactor = 1.0;
            if (!atoms[i].getUse() || !atoms[i].getElectrostatics()) {
                //if (!atoms[i].getUse()) {
                useFactor = 0.0;
            }
            double lambdaScale = lambda;
            if (!atom.applyLambda()) {
                lambdaScale = 1.0;
            }

            useFactor *= lambdaScale;

            MultipoleType multipoleType = atom.getMultipoleType();
            double charge = multipoleType.charge * useFactor;
            double oScale = overlapScale[i] * useFactor;
            OpenMM_DoubleArray_append(doubleArray, charge);
            OpenMM_DoubleArray_append(doubleArray, OpenMM_NmPerAngstrom * baseRadii[i]);
            OpenMM_DoubleArray_append(doubleArray, oScale);
            OpenMM_CustomGBForce_setParticleParameters(customGBForce, i, doubleArray);
            OpenMM_DoubleArray_resize(doubleArray, 0);
        }
        OpenMM_DoubleArray_destroy(doubleArray);
        OpenMM_CustomGBForce_updateParametersInContext(customGBForce, context);
    }

    /**
     * Updates the Amoeba electrostatic multipolar force for change in Use
     * flags.
     *
     * @param atoms Array of all Atoms in the system
     */
    private void updateAmoebaMultipoleForce(Atom[] atoms) {
        ParticleMeshEwald pme = super.getPmeNode();
        int axisAtom[][] = pme.getAxisAtoms();
        double dipoleConversion = OpenMM_NmPerAngstrom;
        double quadrupoleConversion = OpenMM_NmPerAngstrom * OpenMM_NmPerAngstrom;
        double polarityConversion = OpenMM_NmPerAngstrom * OpenMM_NmPerAngstrom
                * OpenMM_NmPerAngstrom;
        double dampingFactorConversion = sqrt(OpenMM_NmPerAngstrom);

        double polarScale = 1.0;
        if (pme.getPolarizationType() == Polarization.NONE) {
            polarScale = 0.0;
        }

        PointerByReference dipoles = OpenMM_DoubleArray_create(3);
        PointerByReference quadrupoles = OpenMM_DoubleArray_create(9);

        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            Atom atom = atoms[i];
            MultipoleType multipoleType = atom.getMultipoleType();
            PolarizeType polarType = atom.getPolarizeType();
            double useFactor = 1.0;

            if (!atoms[i].getUse() || !atoms[i].getElectrostatics()) {
                //if (!atoms[i].getUse()) {
                useFactor = 0.0;
            }

            double lambdaScale = lambda;
            if (!atom.applyLambda()) {
                lambdaScale = 1.0;
            }

            useFactor *= lambdaScale;

            /**
             * Define the frame definition.
             */
            int axisType = OpenMM_AmoebaMultipoleForce_NoAxisType;
            switch (multipoleType.frameDefinition) {
                case ZONLY:
                    axisType = OpenMM_AmoebaMultipoleForce_ZOnly;
                    break;
                case ZTHENX:
                    axisType = OpenMM_AmoebaMultipoleForce_ZThenX;
                    break;
                case BISECTOR:
                    axisType = OpenMM_AmoebaMultipoleForce_Bisector;
                    break;
                case ZTHENBISECTOR:
                    axisType = OpenMM_AmoebaMultipoleForce_ZBisect;
                    break;
                case TRISECTOR:
                    axisType = OpenMM_AmoebaMultipoleForce_ThreeFold;
                    break;
                default:
                    break;
            }

            /**
             * Load local multipole coefficients.
             */
            for (int j = 0; j < 3; j++) {
                OpenMM_DoubleArray_set(dipoles, j, multipoleType.dipole[j] * dipoleConversion * useFactor);

            }
            int l = 0;
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    OpenMM_DoubleArray_set(quadrupoles, l++, multipoleType.quadrupole[j][k]
                            * quadrupoleConversion / 3.0 * useFactor);
                }
            }

            //int zaxis = 0;
            int zaxis = 1;
            //int xaxis = 0;
            int xaxis = 1;
            //int yaxis = 0;
            int yaxis = 1;
            int refAtoms[] = axisAtom[i];
            if (refAtoms != null) {
                zaxis = refAtoms[0];
                if (refAtoms.length > 1) {
                    xaxis = refAtoms[1];
                    if (refAtoms.length > 2) {
                        yaxis = refAtoms[2];
                    }
                }
            } else {
                axisType = OpenMM_AmoebaMultipoleForce_NoAxisType;
            }

            /**
             * Add the multipole.
             */
            OpenMM_AmoebaMultipoleForce_setMultipoleParameters(amoebaMultipoleForce, i,
                    multipoleType.charge * useFactor, dipoles, quadrupoles,
                    axisType, zaxis, xaxis, yaxis,
                    polarType.thole,
                    polarType.pdamp * dampingFactorConversion,
                    polarType.polarizability * polarityConversion * polarScale * useFactor);
        }
        OpenMM_DoubleArray_destroy(dipoles);
        OpenMM_DoubleArray_destroy(quadrupoles);

        OpenMM_AmoebaMultipoleForce_updateParametersInContext(amoebaMultipoleForce, context);
    }

    /**
     * Updates the AMOEBA Generalized Kirkwood force for change in Use flags.
     *
     * @param atoms Array of all Atoms in the system
     */
    private void updateAmoebaGeneralizedKirkwoodForce(Atom[] atoms) {
        GeneralizedKirkwood gk = super.getGK();
        double overlapScale[] = gk.getOverlapScale();
        double baseRadii[] = gk.getBaseRadii();
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            double useFactor = 1.0;
            if (!atoms[i].getUse() || !atoms[i].getElectrostatics()) {
                //if (!atoms[i].getUse()) {
                useFactor = 0.0;
            }

            double lambdaScale = lambda;
            if (!atoms[i].applyLambda()) {
                lambdaScale = 1.0;
            }

            useFactor *= lambdaScale;

            MultipoleType multipoleType = atoms[i].getMultipoleType();
            OpenMM_AmoebaGeneralizedKirkwoodForce_setParticleParameters(amoebaGeneralizedKirkwoodForce, i,
                    multipoleType.charge * useFactor,
                    OpenMM_NmPerAngstrom * baseRadii[i], overlapScale[i] * useFactor);
        }
        OpenMM_AmoebaGeneralizedKirkwoodForce_updateParametersInContext(amoebaGeneralizedKirkwoodForce, context);
    }

    /**
     * Updates the WCA force for change in Use flags.
     *
     * @param atoms Array of all Atoms in the system
     */
    private void updateWCAForce(Atom[] atoms) {
        VanDerWaals vdW = super.getVdwNode();
        VanDerWaalsForm vdwForm = vdW.getVDWForm();
        double radScale = 1.0;
        if (vdwForm.radiusSize == VanDerWaalsForm.RADIUS_SIZE.DIAMETER) {
            radScale = 0.5;
        }
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            double useFactor = 1.0;
            if (!atoms[i].getUse()) {
                useFactor = 0.0;
            }

            double lambdaScale = lambda;
            if (!atoms[i].applyLambda()) {
                lambdaScale = 1.0;
            }
            useFactor *= lambdaScale;

            Atom atom = atoms[i];
            VDWType vdwType = atom.getVDWType();
            double radius = vdwType.radius;
            double eps = vdwType.wellDepth;
            OpenMM_AmoebaWcaDispersionForce_setParticleParameters(amoebaWcaDispersionForce, i,
                    OpenMM_NmPerAngstrom * radius * radScale,
                    OpenMM_KJPerKcal * eps * useFactor);
        }
        OpenMM_AmoebaWcaDispersionForce_updateParametersInContext(amoebaWcaDispersionForce, context);
    }

    @Override
    public void setLambda(double lambda) {
        if (lambdaTerm) {
            if (lambda >= 0.0 && lambda <= 1.0) {
                this.lambda = lambda;
                super.setLambda(lambda);
                updateParameters();
            } else {
                String message = format(" Lambda value %8.3f is not in the range [0..1].", lambda);
                logger.warning(message);
            }
        } else {
            logger.fine(" Attempting to set a lambda value on a ForceFieldEnergyOpenMM with lambdaterm false.");
        }
    }

    /**
     * Evaluates energy both with OpenMM and reference potential, and returns
     * the difference FFX-OpenMM.
     *
     * @param x Coordinate array
     * @param verbose
     * @return Energy discrepancy
     */
    public double energyVsFFX(double[] x, boolean verbose) {
        double ffxE = super.energy(x, verbose);
        double thisE = energy(x, verbose);
        return ffxE - thisE;
    }

    /**
     * Evaluates energy and gradients both with OpenMM and reference potential,
     * and returns the difference FFX-OpenMM.
     *
     * @param x Coordinate array
     * @param gFFX Array for FFX gradients to be stored in
     * @param gOMM Array for OpenMM gradients to be stored in
     * @param verbose
     * @return Energy discrepancy
     */
    public double energyAndGradVsFFX(double[] x, double[] gFFX, double[] gOMM, boolean verbose) {
        double ffxE = super.energyAndGradient(x, gFFX, verbose);
        double thisE = energyAndGradient(x, gOMM, verbose);
        return ffxE - thisE;
    }

    /**
     * Returns the current energy. Preferred is to use the methods with explicit
     * coordinate/gradient arrays.
     *
     * @return Current energy.
     */
    @Override
    public double energy() {
        return energy(false, false);
    }

    @Override
    public double energy(double[] x) {
        return energy(x, false);
    }

    @Override
    public double energy(double[] x, boolean verbose) {

        if (lambdaBondedTerms) {
            return 0.0;
        }

        updateParameters();

        /**
         * Unscale the coordinates.
         */
        if (optimizationScaling != null) {
            int len = x.length;
            for (int i = 0; i < len; i++) {
                x[i] /= optimizationScaling[i];
            }
        }
        setCoordinates(x);
        setOpenMMPositions(x, x.length);

        int infoMask = OpenMM_State_Energy;
        state = OpenMM_Context_getState(context, infoMask, enforcePBC);
        double e = OpenMM_State_getPotentialEnergy(state) / OpenMM_KJPerKcal;

        if (verbose) {
            logger.log(Level.INFO, String.format(" OpenMM Energy: %14.10g", e));
        }

        /**
         * Rescale the coordinates.
         */
        if (optimizationScaling != null) {
            int len = x.length;
            for (int i = 0; i < len; i++) {
                x[i] *= optimizationScaling[i];
            }
        }

        OpenMM_State_destroy(state);
        return e;
    }

    @Override
    public double energyAndGradient(double x[], double g[]) {
        return energyAndGradient(x, g, false);
    }

    @Override
    public double energyAndGradient(double x[], double g[], boolean verbose) {
        if (lambdaBondedTerms) {
            return 0.0;
        }

        /**
         * Un-scale the coordinates.
         */
        if (optimizationScaling != null) {
            int len = x.length;
            for (int i = 0; i < len; i++) {
                x[i] /= optimizationScaling[i];
            }
        }
        setCoordinates(x);
        setOpenMMPositions(x, x.length);

        int infoMask = OpenMM_State_Energy;
        infoMask += OpenMM_State_Forces;

        if (vdwLambdaTerm) {
            infoMask += OpenMM_State_ParameterDerivatives;
        }

        state = OpenMM_Context_getState(context, infoMask, enforcePBC);
        double e = OpenMM_State_getPotentialEnergy(state) / OpenMM_KJPerKcal;

        if (vdwLambdaTerm) {
            PointerByReference parameterArray = OpenMM_State_getEnergyParameterDerivatives(state);
            int numDerives = OpenMM_ParameterArray_getSize(parameterArray);
            if (numDerives > 0) {
                vdwdUdL = OpenMM_ParameterArray_get(parameterArray, pointerForString("vdw_lambda")) / OpenMM_KJPerKcal;
            }
        }

        if (maxDebugGradient < Double.MAX_VALUE) {
            boolean extremeGrad = Arrays.stream(g).anyMatch((double gi) -> {
                return (gi > maxDebugGradient || gi < -maxDebugGradient);
            });
            if (extremeGrad) {
                File origFile = molecularAssembly.getFile();
                String timeString = LocalDateTime.now().format(DateTimeFormatter.
                        ofPattern("yyyy_MM_dd-HH_mm_ss"));

                String filename = String.format("%s-LARGEGRAD-%s.pdb",
                        FilenameUtils.removeExtension(molecularAssembly.getFile().getName()),
                        timeString);
                PotentialsFunctions ef = new PotentialsUtils();
                filename = ef.versionFile(filename);

                logger.warning(String.format(" Excessively large gradients detected; printing snapshot to file %s", filename));
                ef.saveAsPDB(molecularAssembly, new File(filename));
                molecularAssembly.setFile(origFile);
            }
        }

        if (verbose) {
            logger.log(Level.INFO, String.format(" OpenMM Energy: %14.10g", e));
        }

        forces = OpenMM_State_getForces(state);

        fillGradients(g);
        /**
         * Scale the coordinates and gradients.
         */
        if (optimizationScaling != null) {
            int len = x.length;
            for (int i = 0; i < len; i++) {
                x[i] *= optimizationScaling[i];
                g[i] /= optimizationScaling[i];
            }
        }

        OpenMM_State_destroy(state);
        return e;
    }

    @Override
    public void setCrystal(Crystal crystal) {
        super.setCrystal(crystal);
        setDefaultPeriodicBoxVectors();

        //loadFFXPositionToOpenMM();
    }

    /**
     * <p>
     * getGradients</p>
     *
     * @param g an array of double.
     * @return
     */
    @Override
    public double[] getGradients(double g[]) {
        return fillGradients(g);
    }

    /**
     * Private method for internal use, so we don't have subclasses calling
     * super.energy, and this class delegating to the subclass's getGradients
     * method.
     *
     * @param g Gradient array to fill.
     * @return Gradient array.
     */
    public double[] fillGradients(double[] g) {
        assert (g != null);
        int n = getNumberOfVariables();
        if (g.length < n) {
            g = new double[n];
        }
        int index = 0;
        Atom[] atoms = molecularAssembly.getAtomArray();
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            Atom a = atoms[i];
            if (a.isActive()) {
                OpenMM_Vec3 posInNm = OpenMM_Vec3Array_get(forces, i);
                /**
                 * Convert OpenMM Forces in KJ/Nm into an FFX gradient in
                 * Kcal/A.
                 */
                double gx = -posInNm.x * OpenMM_NmPerAngstrom * OpenMM_KcalPerKJ;
                double gy = -posInNm.y * OpenMM_NmPerAngstrom * OpenMM_KcalPerKJ;
                double gz = -posInNm.z * OpenMM_NmPerAngstrom * OpenMM_KcalPerKJ;
                if (Double.isNaN(gx) || Double.isInfinite(gx)
                        || Double.isNaN(gy) || Double.isInfinite(gy)
                        || Double.isNaN(gz) || Double.isInfinite(gz)) {
                    /*String message = format("The gradient of atom %s is (%8.3f,%8.3f,%8.3f).",
                            a.toString(), gx, gy, gz);*/
                    StringBuilder sb = new StringBuilder(format("The gradient of atom %s is (%8.3f,%8.3f,%8.3f).",
                            a.toString(), gx, gy, gz));
                    double[] vals = new double[3];
                    a.getVelocity(vals);
                    sb.append(format("\n Velocities: %8.3g %8.3g %8.3g", vals[0], vals[1], vals[2]));
                    a.getAcceleration(vals);
                    sb.append(format("\n Accelerations: %8.3g %8.3g %8.3g", vals[0], vals[1], vals[2]));
                    a.getPreviousAcceleration(vals);
                    sb.append(format("\n Previous accelerations: %8.3g %8.3g %8.3g", vals[0], vals[1], vals[2]));

                    //logger.severe(message);
                    throw new EnergyException(sb.toString());
                }
                a.setXYZGradient(gx, gy, gz);
                g[index++] = gx;
                g[index++] = gy;
                g[index++] = gz;
            }
        }
        return g;
    }

    /**
     * setOpenMMPositions takes in an array of doubles generated by the DYN
     * reader method and appends these values to a Vec3Array. Finally this
     * method sets the created Vec3Array as the positions of the context.
     *
     * @param x
     * @param numberOfVariables
     */
    public void setOpenMMPositions(double x[], int numberOfVariables) {
        assert numberOfVariables == getNumberOfVariables();
        if (positions == null) {
            positions = OpenMM_Vec3Array_create(0);
        } else {
            OpenMM_Vec3Array_resize(positions, 0);
        }
        OpenMM_Vec3.ByValue pos = new OpenMM_Vec3.ByValue();
        for (int i = 0; i < numberOfVariables; i = i + 3) {
            pos.x = x[i] * OpenMM_NmPerAngstrom;
            pos.y = x[i + 1] * OpenMM_NmPerAngstrom;
            pos.z = x[i + 2] * OpenMM_NmPerAngstrom;
            OpenMM_Vec3Array_append(positions, pos);
        }
        OpenMM_Context_setPositions(context, positions);
    }

    /**
     * setOpenMMVelocities takes in an array of doubles generated by the DYN
     * reader method and appends these values to a Vec3Array. Finally this
     * method sets the created Vec3Arrat as the velocities of the context.
     *
     * @param v
     * @param numberOfVariables
     */
    public void setOpenMMVelocities(double v[], int numberOfVariables) {
        assert numberOfVariables == getNumberOfVariables();
        if (velocities == null) {
            velocities = OpenMM_Vec3Array_create(0);
        } else {
            OpenMM_Vec3Array_resize(velocities, 0);
        }
        OpenMM_Vec3.ByValue vel = new OpenMM_Vec3.ByValue();
        for (int i = 0; i < numberOfVariables; i = i + 3) {
            vel.x = v[i] * OpenMM_NmPerAngstrom;
            vel.y = v[i + 1] * OpenMM_NmPerAngstrom;
            vel.z = v[i + 2] * OpenMM_NmPerAngstrom;
            OpenMM_Vec3Array_append(velocities, vel);
        }
        OpenMM_Context_setVelocities(context, velocities);
    }

    /**
     * getOpenMMPositions takes in a PointerByReference containing the position
     * information of the context. This method creates a Vec3Array that contains
     * the three dimensional information of the positions of the atoms. The
     * method then adds these values to a new double array x and returns it to
     * the method call
     *
     * @param positions
     * @param numberOfVariables
     * @param x
     * @return x
     */
    public double[] getOpenMMPositions(PointerByReference positions, int numberOfVariables, double x[]) {
        assert numberOfVariables == getNumberOfVariables();
        if (x == null || x.length < numberOfVariables) {
            x = new double[numberOfVariables];
        }
        Atom[] atoms = molecularAssembly.getAtomArray();
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            int offset = i * 3;
            OpenMM_Vec3 pos = OpenMM_Vec3Array_get(positions, i);
            x[offset] = pos.x * OpenMM_AngstromsPerNm;
            x[offset + 1] = pos.y * OpenMM_AngstromsPerNm;
            x[offset + 2] = pos.z * OpenMM_AngstromsPerNm;
            Atom atom = atoms[i];
            atom.moveTo(x[offset], x[offset + 1], x[offset + 2]);
        }
        return x;
    }

    /**
     * getOpenMMVelocities takes in a PointerByReference containing the velocity
     * information of the context. This method creates a Vec3Array that contains
     * the three dimensional information of the velocities of the atoms. This
     * method then adds these values to a new double array v and returns it to
     * the method call
     *
     * @param velocities
     * @param numberOfVariables
     * @return
     */
    public double[] getOpenMMVelocities(PointerByReference velocities, int numberOfVariables, double v[]) {
        assert numberOfVariables == getNumberOfVariables();
        if (v == null || v.length < numberOfVariables) {
            v = new double[numberOfVariables];
        }
        Atom[] atoms = molecularAssembly.getAtomArray();
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            int offset = i * 3;
            OpenMM_Vec3 vel = OpenMM_Vec3Array_get(velocities, i);
            v[offset] = vel.x * OpenMM_AngstromsPerNm;
            v[offset + 1] = vel.y * OpenMM_AngstromsPerNm;
            v[offset + 2] = vel.z * OpenMM_AngstromsPerNm;
            Atom atom = atoms[i];
            double velocity[] = {v[offset], v[offset + 1], v[offset + 2]};
            atom.setVelocity(velocity);
        }
        return v;
    }

    /**
     * getOpenMMAccelerations takes in a PointerByReference containing the force
     * information of the context. This method creates a Vec3Array that contains
     * the three dimensional information of the forces on the atoms. This method
     * then adds these values (divided by mass, effectively turning them into
     * accelerations) to a new double array a and returns it to the method call
     *
     * @param forces
     * @param numberOfVariables
     * @param mass
     * @return
     */
    public double[] getOpenMMAccelerations(PointerByReference forces, int numberOfVariables, double[] mass, double[] a) {
        assert numberOfVariables == getNumberOfVariables();
        if (a == null || a.length < numberOfVariables) {
            a = new double[numberOfVariables];
        }
        Atom[] atoms = molecularAssembly.getAtomArray();
        int nAtoms = atoms.length;
        for (int i = 0; i < nAtoms; i++) {
            int offset = i * 3;
            OpenMM_Vec3 acc = OpenMM_Vec3Array_get(forces, i);
            a[offset] = (acc.x * 10.0) / mass[i];
            a[offset + 1] = (acc.y * 10.0) / mass[i + 1];
            a[offset + 2] = (acc.z * 10.0) / mass[i + 2];
            Atom atom = atoms[i];
            double acceleration[] = {a[offset], a[offset + 1], a[offset + 2]};
            atom.setAcceleration(acceleration);
        }
        return a;
    }

    private void setDefaultPeriodicBoxVectors() {

        OpenMM_Vec3 a = new OpenMM_Vec3();
        OpenMM_Vec3 b = new OpenMM_Vec3();
        OpenMM_Vec3 c = new OpenMM_Vec3();

        Crystal crystal = super.getCrystal();

        if (!crystal.aperiodic()) {
            a.x = crystal.a * OpenMM_NmPerAngstrom;
            a.y = 0.0 * OpenMM_NmPerAngstrom;
            a.z = 0.0 * OpenMM_NmPerAngstrom;
            b.x = 0.0 * OpenMM_NmPerAngstrom;
            b.y = crystal.b * OpenMM_NmPerAngstrom;
            b.z = 0.0 * OpenMM_NmPerAngstrom;
            c.x = 0.0 * OpenMM_NmPerAngstrom;
            c.y = 0.0 * OpenMM_NmPerAngstrom;
            c.z = crystal.c * OpenMM_NmPerAngstrom;
            OpenMM_System_setDefaultPeriodicBoxVectors(system, a, b, c);
        }
    }

    /**
     * getIntegrator returns the integrator used for the context
     *
     * @return integrator
     */
    public PointerByReference getIntegrator() {
        return integrator;
    }

    /**
     * Returns the context created for the ForceFieldEnergyOpenMM object
     *
     * @return context
     */
    public PointerByReference getContext() {
        return context;
    }

    /**
     * Sets the finite-difference step size used for getdEdL.
     *
     * @param fdDLambda FD step size.
     */
    public void setFdDLambda(double fdDLambda) {
        this.fdDLambda = fdDLambda;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getdEdL() {
        if (!lambdaTerm) {
            return 0.0;
        }

        if (vdwLambdaTerm) {
            return vdwdUdL;
        }

        if (testdEdL) {
            testLambda();
            testdEdL = false;
        }

        double currentLambda = lambda;
        double width = fdDLambda;
        double ePlus;
        double eMinus;
        double dEdL = 0.0;
        double openMMdEdL = 0.0;
        double ffxdEdL = 0.0;

        // Small optimization to only create the x array once.
        double[] x = new double[getNumberOfVariables()];
        getCoordinates(x);

        if (doOpenMMdEdL) {
            width = fdDLambda;
            if (currentLambda + fdDLambda > 1.0) {
                logger.fine(" Could not test the upper point, as current lambda + fdDL > 1");
                ePlus = energy(x);
                setLambda(currentLambda - fdDLambda);
                eMinus = energy(x);
            } else if (currentLambda - fdDLambda < 0.0) {
                logger.fine(" Could not test the lower point, as current lambda - fdDL < 1");
                eMinus = energy(x);
                setLambda(currentLambda + fdDLambda);
                ePlus = energy(x);
            } else {
                setLambda(currentLambda + fdDLambda);
                ePlus = energy(x);
                setLambda(currentLambda - fdDLambda);
                eMinus = energy(x);
                //logger.info(String.format(" OpenMM %12.8f %12.8f", ePlus, eMinus));
                width *= 2.0;
            }
            // Reset Lambda.
            setLambda(currentLambda);
            openMMdEdL = (ePlus - eMinus) / width;
            //logger.info(String.format(" Step: %16.10f OpenMM: %16.10f", fdDLambda, openMMdEdL));
            dEdL = openMMdEdL;
        }

        if (doFFXdEdL || !doOpenMMdEdL) {
            width = fdDLambda;
            // This section technically not robust to the case that fdDLambda > 0.5.
            // However, that should be an error case checked when fdDLambda is set.
            super.setLambda(1.0);
            if (currentLambda + fdDLambda > 1.0) {
                logger.fine(" Could not test the upper point, as current lambda + fdDL > 1");
                super.setLambdaMultipoleScale(currentLambda);
                ePlus = super.energy(x, false);
                // ePlus = super.getTotalElectrostaticEnergy();
                super.setLambdaMultipoleScale(currentLambda - fdDLambda);
                eMinus = super.energy(x, false);
                // eMinus = super.getTotalElectrostaticEnergy();
            } else if (currentLambda - fdDLambda < 0.0) {
                logger.fine(" Could not test the lower point, as current lambda - fdDL < 1");
                super.setLambdaMultipoleScale(currentLambda);
                eMinus = super.energy(x, false);
                // eMinus = super.getTotalElectrostaticEnergy();
                super.setLambdaMultipoleScale(currentLambda + fdDLambda);
                ePlus = super.energy(x, false);
                // ePlus = super.getTotalElectrostaticEnergy();
            } else {
                super.setLambdaMultipoleScale(currentLambda + fdDLambda);
                ePlus = super.energy(x, false);
                // ePlus = super.getTotalElectrostaticEnergy();
                super.setLambdaMultipoleScale(currentLambda - fdDLambda);
                eMinus = super.energy(x, false);
                // eMinus = super.getTotalElectrostaticEnergy();
                //logger.info(String.format(" FFX    %12.8f %12.8f", ePlus, eMinus));
                width *= 2.0;
            }
            super.setLambdaMultipoleScale(currentLambda);
            ffxdEdL = (ePlus - eMinus) / width;
            //logger.info(String.format(" Step: %16.10f FFX:    %16.10f", fdDLambda, ffxdEdL));
            dEdL = ffxdEdL;
        }

        return dEdL;
    }

    /**
     * Test the OpenMM and FFX energy for Lambda = 0 and Lambda = 1.
     */
    public void testLambda() {
        // Save the current value of Lambda.
        double currentLambda = lambda;

        // Small optimization to only create the x array once.
        double[] x = new double[getNumberOfVariables()];
        getCoordinates(x);

        // Test OpenMM at L=0.5 and L=1.
        setLambda(0.0);
        double openMMEnergyZero = energy(x);
        setLambda(1.0);
        double openMMEnergyOne = energy(x);

        // Test FFX at L=0 and L=1.
        super.setLambda(1.0);
        super.setLambdaMultipoleScale(0.0);
        double ffxEnergyZero = super.energy(x, false);
        super.setLambdaMultipoleScale(1.0);
        double ffxEnergyOne = super.energy(x, false);
        super.setLambdaMultipoleScale(currentLambda);

        setLambda(currentLambda);

        logger.info(format(" OpenMM Energy at L=0.0: %16.8f and L=1: %16.8f", openMMEnergyZero, openMMEnergyOne));
        logger.info(format(" FFX Energy    at L=0.0: %16.8f and L=1: %16.8f", ffxEnergyZero, ffxEnergyOne));
    }

    /**
     * {@inheritDoc}
     *
     * @param gradients
     */
    @Override
    public void getdEdXdL(double gradients[]) {
        // Note for ForceFieldEnergyOpenMM this method is not implemented.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getd2EdL2() {
        return 0.0;
    }

    public double getVDWRadius() {
        VanDerWaals vdW = super.getVdwNode();
        if (vdW == null) {
            return -1.0;
        }

        /**
         * Only 6-12 LJ with arithmetic mean to define sigma and geometric mean
         * for epsilon is supported.
         */
        VanDerWaalsForm vdwForm = vdW.getVDWForm();
        if (vdwForm.vdwType != LENNARD_JONES
                || vdwForm.radiusRule != ARITHMETIC
                || vdwForm.epsilonRule != GEOMETRIC) {
            logger.log(Level.SEVERE, String.format(" Unsuppporterd van der Waals functional form."));
            return -1.0;
        }

        /**
         * OpenMM vdW force requires a diameter (i.e. not radius).
         */
        double radScale = 1.0;
        if (vdwForm.radiusSize == RADIUS) {
            radScale = 2.0;
        }
        /**
         * OpenMM vdw force requires atomic sigma values (i.e. not r-min).
         */
        if (vdwForm.radiusType == R_MIN) {
            radScale /= 1.122462048309372981;
        }

        return radScale;
    }

    public void setUpHydrogenConstraints(PointerByReference system) {
        int i;
        int iAtom1;
        int iAtom2;

        //Atom[] atoms = molecularAssembly.getAtomArray();
        Bond[] bonds = super.getBonds();

        logger.info(String.format(" Setting up Hydrogen constraints"));

        if (bonds == null || bonds.length < 1) {
            return;
        }
        int nBonds = bonds.length;
        Atom atom1;
        Atom atom2;
        Atom parentAtom;
        Bond bondForBondLength;
        BondType bondType;

        for (i = 0; i < nBonds; i++) {
            Bond bond = bonds[i];
            atom1 = bond.getAtom(0);
            atom2 = bond.getAtom(1);
            if (atom1.isHydrogen()) {
                parentAtom = atom1.getBonds().get(0).get1_2(atom1);
                bondForBondLength = atom1.getBonds().get(0);
                bondType = bondForBondLength.bondType;
                iAtom1 = atom1.getXyzIndex() - 1;
                iAtom2 = parentAtom.getXyzIndex() - 1;
                OpenMM_System_addConstraint(system, iAtom1, iAtom2, bondForBondLength.bondType.distance * OpenMM_NmPerAngstrom);
            } else if (atom2.isHydrogen()) {
                parentAtom = atom2.getBonds().get(0).get1_2(atom2);
                bondForBondLength = atom2.getBonds().get(0);
                bondType = bondForBondLength.bondType;
                iAtom1 = atom2.getXyzIndex() - 1;
                iAtom2 = parentAtom.getXyzIndex() - 1;
                OpenMM_System_addConstraint(system, iAtom1, iAtom2, bondForBondLength.bondType.distance * OpenMM_NmPerAngstrom);
            }
        }
    }

    public int calculateDegreesOfFreedom() {
        int dof = numParticles * 3;
        dof = dof - OpenMM_System_getNumConstraints(system);
        if (commRemover != null) {
            dof -= 3;
        }
        return dof;
    }

    public int getNumParticles() {
        return numParticles;
    }

}
