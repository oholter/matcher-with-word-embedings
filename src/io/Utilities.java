/*******************************************************************************
 * Copyright 2012 by the Department of Computer Science (University of Oxford)
 * 
 *    This file is part of LogMap.
 * 
 *    LogMap is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMap is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMap.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package io;

public class Utilities {
	
	
	//LOGMAP VERSION
	public static final int LOGMAP = 0;
	public static final int LOGMAPMENDUM = 1;
	public static final int LOGMAPLITE = 2;
	public static final int LOGMAPINTERACTIVITY = 3;
	
	
	//WEAK MAPPINGS LEVELS
	public static final int WEAK_LEVEL1=1;
	public static final int WEAK_LEVEL2=2;
	public static final int WEAK_LEVEL3=3;
	
	//ORIGIN of AXIOM CLAUSE
	public static final int MAP=0;
	public static final int ONTO1=1;	
	public static final int ONTO2=2;
	
	//DIR IMPLICATION
	public static final int L2R=0; //P->Q
	public static final int R2L=-1; //P<-Q
	public static final int EQ=-2; //P<->Qversion = Utilities.LOGMAPMENDUM;
	public static final int NoMap=-3; 
	public static final int Flagged=-4; //Flagged mappinsg in Largebio 
	
	
	//TYPE OF MAPPING
	public static final int CLASSES=0;
	public static final int DATAPROPERTIES=1;
	public static final int OBJECTPROPERTIES=2;
	public static final int INSTANCES=3;
	public static final int UNKNOWN=4;
	
	public static final String CLASSES_STR="CLS";
	public static final String DATAPROPERTIES_STR="DPROP";
	public static final String OBJECTPROPERTIES_STR="OPROP";
	public static final String INSTANCES_STR="INST";
	
	
	//Reasoner
	public static final int STRUCTURAL_REASONER=0;
	public static final int HERMIT_REASONER=1;
	public static final int CONDOR_INPUT=2;
	
	//Ontos
	public static final int FMA=0;
	public static final int NCI=1;
	public static final int SNOMED=2;
	public static final int Chemo=3;
	public static final int NCIAn=4;
	public static final int Mouse=5;
	public static final int ontoA=6;
	public static final int ontoB=7;
	
	//Pairs
	public static final int FMA2NCI=0;
	public static final int FMA2SNOMED=1;
	public static final int SNOMED2NCI=2;
	public static final int SNOMED2LUCADA=3;
	public static final int OntoA2OntoB=4;
	public static final int MOUSE2HUMAN=5;
	public static final int NCIpeque2FMA=6;
	public static final int NCI2FMApeque=7;
	public static final int NCI2LUCADA=8;
	public static final int FMA2LUCADA=9;
	public static final int LIBRARY=10;
	public static final int CONFERENCE=11;
	public static final int INSTANCE=12;
	public static final int MULTILINGUAL=13;
	
	
	public static final int CONFLICT=0;
	public static final int DANG_EQUIV=1;
	public static final int DANG_SUB=2;
	
	public static final int NOCONFLICT=3;
	
	public static final int DISPARATE=4;
	//public static final int DISP_PATH=5;
	
	public static final int COMPATIBLE=6;

}