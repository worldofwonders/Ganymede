/*

   db_object.java

   This interface defines the methods that the client can remotely
   call on a object held in the Ganymede server.
   
   Created: 11 April 1996
   Release: $Name:  $
   Version: $Revision: 1.23 $
   Last Mod Date: $Date: 2000/03/08 22:44:00 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
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

package arlut.csd.ganymede;

import java.util.Date;
import java.util.Vector;
import java.rmi.RemoteException;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                       db_object

------------------------------------------------------------------------------*/

/**
 * <p>Remote reference to a Ganymede {@link arlut.csd.ganymede.DBObject DBObject}
 * or {@link arlut.csd.ganymede.DBEditObject DBEditObject}, the db_object is used by the
 * client to get information about and/or make changes to a object held
 * in the Ganymede server.</p>
 *
 * @version $Revision: 1.23 $ $Date: 2000/03/08 22:44:00 $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public interface db_object extends java.rmi.Remote {

  /**
   *
   * Returns the numeric id of the object in the objectBase
   *
   */

  public int getID() throws RemoteException;

  /**
   *
   * Returns the base id of the object's type
   *
   */

  public short getTypeID() throws RemoteException;

  /**
   *
   * Returns the name of the object's type
   *
   */

  public String getTypeName() throws RemoteException;

  /**
   *
   * Returns the unique object id (invid) of this object
   *
   */

  public Invid getInvid() throws RemoteException;

  /**
   *
   * Returns a vector of custom field information records, in display order.
   *
   * @see arlut.csd.ganymede.FieldInfo
   *
   */

  public Vector getFieldInfoVector() throws RemoteException;

  /**
   *
   * <p>Get access to a field from this object.</p>
   *
   * @param id The field code for the desired field of this object.
   *
   */

  public db_field getField(short id) throws RemoteException;

  /**
   *
   * <p>Get access to a field from this object, by name.</p>
   *
   * @param fieldname The fieldname for the desired field of this object
   *
   */

  public db_field getField(String fieldname) throws RemoteException;

  /**
   * <P>Returns the name of a field from this object.</P>
   *
   * @param id The field code for the desired field of this object.
   */

  public String getFieldName(short id) throws RemoteException;

  /**
   *
   * <p>Get access to the field that serves as this object's label</p>
   *
   * <p>Not all objects use simple field values as their labels.  If an
   * object has a calculated label, this method will return null.</p>
   *
   */

  public db_field getLabelField() throws RemoteException;

  /**
   *
   * <p>Get list of all db_fields contained in this object,
   * in unsorted order.</p>
   *
   */

  public db_field[] listFields() throws RemoteException;

  /**
   *
   * Returns the primary label of this object.
   *
   */

  public String getLabel() throws RemoteException;

  /**
   *
   * Returns true if this object is an embedded type
   *
   */

  public boolean isEmbedded() throws RemoteException;


  /**
   *
   * <p>Returns true if inactivate() is a valid operation on
   * checked-out objects of this type.</p>
   *
   */

  public boolean canInactivate() throws RemoteException;

  /**
   * <p>Returns true if this object has been inactivated and is
   * pending deletion.</p>
   */

  public boolean isInactivated() throws RemoteException;

  /**
   * <p>Returns true if this object has all its required fields defined</p>
   */

  public boolean isValid() throws RemoteException;

  /**
   * <p>Returns the date that this object is to go through final removal
   * if it has been inactivated.</p>
   */

  public Date getRemovalDate() throws RemoteException;

  /**
   *
   * <p>Returns true if this object has an expiration date set.</p>
   *
   */

  public boolean willExpire() throws RemoteException;

  /**
   *
   * <p>Returns the date that this object is to be automatically
   * inactivated if it has an expiration date set.</p>
   *
   */

  public Date getExpirationDate() throws RemoteException;

  /**
   *
   * Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.
   *
   */

  public ReturnVal setFieldValue(short fieldID, Object value) throws RemoteException;

  /**
   *
   * Shortcut method to get a scalar field's value.  Using this
   * method saves a roundtrip to the server.
   *
   */

  public Object getFieldValue(short fieldID) throws RemoteException;

  /**
   *
   * Shortcut method to get a vector field's values.  Using this
   * method saves a roundtrip to the server.
   *
   */

  public Vector getFieldValues(short fieldID) throws RemoteException;

  /**
   * <p>This method is used to provide a summary description of
   * this object, including a listing of all non-null fields and
   * their contents.  This method is remotely callable by the client,
   * and so will only reveal fields that the user has permission
   * to view.</p>
   */

  public StringBuffer getSummaryDescription() throws RemoteException;
}
