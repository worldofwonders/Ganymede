/*

   contractSchema.java

   An interface defining constants to be used by the contract code.
   
   Created: 15 March 1999
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996 - 2004
   The University of Texas at Austin

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ddroid.gasharl;


/*------------------------------------------------------------------------------
                                                                       interface
                                                                  contractSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the contract code.
 *
 */

public interface contractSchema {

  final static short CONTRACTNAME=256;
  final static short CONTRACTNUMBER=257;
  final static short CONTRACTSTART=258;
  final static short CONTRACTSTOP=259;
  final static short CONTRACTDESCRIP=260;
  final static short CONTRACTGROUPS=261;
  final static short CONTRACTSTATUS=262;
  final static short CONTRACTLAB=263;
  final static short CONTRACTRESEARCHUNIT=264;
  final static short CONTRACTUTNUMBER=265;
}
