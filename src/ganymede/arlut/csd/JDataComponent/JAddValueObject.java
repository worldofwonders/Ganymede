
/*
   JAddValueObject.java

   Subclass of JValueObject that represents the addition of a specific
   value to a list, or selection in JstringListBox or StringSelector.

   Created: 25 October 2004

   Last Revision Changed: $Rev$
   Last Changed By: $Author: broccol $
   Last Mod Date: $Date$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2004
   The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.JDataComponent;

import java.awt.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 JAddValueObject

------------------------------------------------------------------------------*/

/**
 * <p>Subclass of JValueObject that represents the addition of a specific
 * value to a list, or selection in JstringListBox or StringSelector.</p>
 *
 * @version $Revision$ $Date$ $Name:  $
 * @author Jonathan Abbey
 */

public class JAddValueObject extends JValueObject {

  private Component source;
  private Object value = null;
  private int index = -1;

  /* -- */

  public JAddValueObject(Component source, Object value)
  {
    this.source = source;
    this.value = value;
  }

  public JAddValueObject(Component source, int index)
  {
    this.source = source;
    this.index = index;
  }

  public JAddValueObject(Component source, int index, Object value)
  {
    this.source = source;
    this.value = value;
    this.index = index;
  }

  /**
   * Returns the arlut.csd.JDataComponent GUI component that originated this message.
   */

  public Component getSource()
  {
    return source;
  }

  /**
   * Returns an auxiliary value.  Used for passing information about pop-up menu items, but may
   * be used for different purposes if needed.
   */

  public Object getParameter() 
  {
    return null;
  }

  /**
   * Returns the index of an item operated on in a vector component.
   */

  public int getIndex() 
  {
    return index;
  }

  /**
   * Returns the index of an item operated on in a vector component.
   */

  public int getIndex2() 
  {
    return -1;
  }

  /**
   * Returns the value of the object being affected by this message.
   */

  public Object getValue() 
  {
    return value;
  }

  /**
   *
   * Method to get a human-readable description of the event carried
   * by this object
   * 
   */

  public String toString()
  {
    if (index != -1)
      {
	return source.toString() +  " add(" + String.valueOf(value) + ")";
      }
    else
      {
	return source.toString() +  " add(" + index + ", " + String.valueOf(value) + ")";
      }
  }
}
