/*
   GASH 2

   DBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision$
   Last Mod Date: $Date$
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

package arlut.csd.ganymede.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.FieldInfo;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.db_field;

/*------------------------------------------------------------------------------
                                                                  abstract class
                                                                         DBField

------------------------------------------------------------------------------*/

/**
 * <P>This abstract base class encapsulates the basic logic for fields in the
 * Ganymede {@link arlut.csd.ganymede.server.DBStore DBStore},
 * including permissions and unique value handling.</P>
 *
 * <P>DBFields are the actual carriers of field value in the Ganymede
 * server.  Each {@link arlut.csd.ganymede.server.DBObject DBObject} holds a
 * set of DBFields in an array.  Each DBField is associated with a {@link
 * arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField} field
 * definition (see {@link arlut.csd.ganymede.server.DBField#getFieldDef()
 * getFieldDef()}) by way of its owner's type and it's own field code,
 * which defines the type of the field as well as various generic and
 * type-specific attributes for the field.  The DBObjectBaseField
 * information is created and edited with the Ganymede schema
 * editor.</P>
 *
 * <P>DBField is an abstract class.  There is a different subclass of DBField
 * for each kind of data that can be held in the Ganymede server, as follows:</P>
 *
 * <UL>
 * <LI>{@link arlut.csd.ganymede.server.StringDBField StringDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.BooleanDBField BooleanDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.NumericDBField NumericDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.FloatDBField FloatDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.DateDBField DateDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.InvidDBField InvidDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.IPDBField IPDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.PasswordDBField PasswordDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.PermissionMatrixDBField PermissionMatrixDBField}</LI>
 * </UL>
 *
 * <P>Each DBField subclass is responsible for writing itself to disk
 * on command with the {@link
 * arlut.csd.ganymede.server.DBField#emit(java.io.DataOutput) emit()} method,
 * and reading its state in with the {@link
 * arlut.csd.ganymede.server.DBField#receive(java.io.DataInput, arlut.csd.ganymede.server.DBObjectBaseField) receive()}
 * method.  Each DBField subclass may also have extensive special
 * logic to handle special operations on fields of the appropriate
 * type.  For instance, the InvidDBField class has lots and lots of
 * logic for handling the bi-directional object linking that the
 * server depends on for its object handling.  Mostly the DBField
 * subclasses provide customization that modifies how things like
 * {@link arlut.csd.ganymede.server.DBField#setValue(java.lang.Object)
 * setValue()} and {@link arlut.csd.ganymede.server.DBField#getValue()
 * getValue()} work, but PasswordDBField and PermissionMatrixDBField
 * don't fit with the standard generic value-container model, and
 * contain their own methods for manipulating and accessing data held
 * in the Ganymede database. Most DBField subclasses only allow a
 * single value to be held, but StringDBField, InvidDBField, and
 * IPDBField support vectors of values.</P>
 *
 * <P>The Ganymede client can directly access fields in RMI-published
 * objects using the {@link arlut.csd.ganymede.rmi.db_field db_field} RMI
 * interface.  Each concrete subclass of DBField has its own special
 * RMI interface which provides special methods for the client.
 * Adding a new data type to the Ganymede server will involve creating
 * a new DBField subclass, as well as a new RMI interface for any
 * special field methods.  All client code would also need to be
 * modified to be aware of the new field type.  DBObjectBaseField,
 * DBEditObject and DBObject would also need to be modified to be
 * aware of the new field type for schema editing, customization, and object loading.
 * The schema editor would have to be modified as well.</P>
 *
 * <P>But you can do it if you absolutely have to.  Just be careful and take a good
 * look around at the code.</P>
 *
 * <P>Note that while DBField was designed to be subclassed, it should only be
 * necessary for adding a new data type to the server.  All other likely 
 * customizations you'd want to do are handled by
 * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} customization methods.  Most
 * DBField methods at some point call methods on the DBObject/DBEditObject
 * that contains it.  All methods that cause changes to fields call out to
 * finalizeXXX() and/or wizardHook() methods in DBEditObject.  Consult the
 * DBEditObject customization guide for details on the field/object interactions.</P>
 *
 * <P>An important note about synchronization: it is possible to encounter a
 * condition called a <b>nested monitor deadlock</b>, where a synchronized
 * method on a field can block trying to enter a synchronized method on
 * a {@link arlut.csd.ganymede.server.DBSession DBSession}, 
 * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}, or 
 * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} object that is itself blocked
 * on another thread trying to call a synchronized method on the same field.</P>
 *
 * <P>To avoid this condition, no field methods that call synchronized methods on
 * other objects should themselves be synchronized in any fashion.</P>
 */

public abstract class DBField implements Remote, db_field {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBField");

  // ---

  /**
   * the object's current value.  May be a Vector for vector fields, in
   * which case getVectVal() may be used to perform the cast.
   */

  Object value = null;
  
  /**
   * The object this field is contained within
   */

  DBObject owner;

  /**
   * <p>The identifying field number for this field within the
   * owning object.  This number is an index into the
   * owning object type's field dictionary.</p>
   */

  short fieldcode;

  /* -- */

  public DBField()
  {
  }

  /**
   * <p>This method is used to return a copy of this field, with the field's owner
   * set to newOwner.</p>
   */

  abstract public DBField getCopy(DBObject newOwner);

  /**
   * <p>This method is designed to handle casting this field's value into
   * a vector as needed.  We don't bother to check whether value is a Vector
   * here, as the code which would have used the old values field should
   * do that for us themselves.</p>
   *
   * <p>This method does no permissions checking at all, and should only
   * be used from within DBField and subclass code.  For other purposes,
   * use getValuesLocal().</p>
   *
   * <p>This method should always return a valid vector if this field
   * is truly a vector field, as we don't keep empty vector fields in
   * non-editable objects, and if this is an editable object we'll
   * have created a vector when this field was initialized for
   * editing.</p>
   */
  
  public final Vector getVectVal()
  {
    return (Vector) value;
  }

  /**
   *
   * Object value of DBField.  Used to represent value in value hashes.
   * Subclasses need to override this method in subclass.
   *
   */

  public Object key()
  {
    if (isVector())
      {
	throw new IllegalArgumentException(ts.l("global.oops_vector", getName(), owner.getLabel()));
      }

    return value;
  }

  /**
   *
   * Object value of a vector DBField.  Used to represent value in value hashes.
   * Subclasses need to override this method in subclass.
   *
   */

  public Object key(int index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    return getVectVal().elementAt(index);
  }

  /**
   * Returns number of elements in vector if this is a vector field.  If
   * this is not a vector field, will return 1. (Should throw exception?)
   */

  public int size()		// returns number of elements in array
  {
    if (!isVector())
      {
	return 1;
      }
    else
      {
	return getVectVal().size();
      }
  }

  /**
   *
   * Returns the maximum length of an array in this field type
   *
   */

  public int getMaxArraySize()
  {
    if (!isVector())
      {
	return 1;		// should throw exception?
      }
    else
      {
	return getFieldDef().getMaxArraySize();
      }     
  }      

  /**
   * <P>This method is responsible for writing out the contents of
   * this field to an binary output stream.  It is used in writing
   * fields to the ganymede.db file and to the journal file.</P>
   *
   * <P>This method only writes out the value contents of this field.
   * The {@link arlut.csd.ganymede.server.DBObject DBObject}
   * {@link arlut.csd.ganymede.server.DBObject#emit(java.io.DataOutput) emit()}
   * method is responsible for writing out the field identifier information
   * ahead of the field's contents.</P>
   */

  abstract void emit(DataOutput out) throws IOException;

  /**
   * <P>This method is responsible for reading in the contents of
   * this field from an binary input stream.  It is used in reading
   * fields from the ganymede.db file and from the journal file.</P>
   *
   * <P>The code that calls receive() on this field is responsible for
   * having read enough of the binary input stream's context to
   * place the read cursor at the point in the file immediately after
   * the field's id and type information has been read.</P>
   */

  abstract void receive(DataInput in, DBObjectBaseField definition) throws IOException;

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().</p>
   */

  abstract void emitXML(XMLDumpContext dump) throws IOException;

  /**
   * <p>This method is used when this field has changed, and its
   * changes need to be written to a Sync Channel.</p>
   *
   * <p>The assumptions of this method are that both this field and
   * the orig field are defined (i.e., non-null, non-empty), and that
   * orig is of the same class as this field.  It is an error to call
   * this method with null dump or orig parameters.</p>
   *
   * <p>It is also an error to call this method when this field is not
   * currently being edited in a DBEditObject, as emitXMLDelta() may
   * depend on context from the editing object.</p>
   *
   * <p>It is the responsibility of the code that calls this method to
   * determine that this field differs from orig.  If this field and
   * orig have no changes between them, the output is undefined.</p>
   */

  abstract void emitXMLDelta(XMLDumpContext dump, DBField orig) throws IOException;

  /**
   * <P>Returns true if obj is a field with the same value(s) as
   * this one.</P>
   *
   * <P>This method is ok to be synchronized because it does not call
   * synchronized methods on any other object that is likely to have
   * another thread trying to call another synchronized method on
   * us.</P> 
   */

  public synchronized boolean equals(Object obj)
  {
    if (!(obj.getClass().equals(this.getClass())))
      {
	return false;
      }

    DBField f = (DBField) obj;

    if (!isVector())
      {
	return f.key().equals(this.key());
      }
    else
      {
	if (f.size() != this.size())
	  {
	    return false;
	  }

	for (int i = 0; i < size(); i++)
	  {
	    if (!f.key(i).equals(this.key(i)))
	      {
		return false;
	      }
	  }

	return true;
      }
  }

  /**
   * <p>This method copies the current value of this DBField
   * to target.  The target DBField must be contained within a
   * checked-out DBEditObject in order to be updated.  Any actions
   * that would normally occur from a user manually setting a value
   * into the field will occur.</p>
   *
   * @param target The DBField to copy this field's contents to.
   * @param local If true, permissions checking is skipped.
   */

  public synchronized ReturnVal copyFieldTo(DBField target, boolean local)
  {
    ReturnVal retVal;

    /* -- */

    if (!local)
      {
	if (!verifyReadPermission())
	  {
	    return Ganymede.createErrorDialog(ts.l("copyFieldTo.copy_error_sub"),
					      ts.l("copyFieldTo.no_read", getName(), owner.getLabel()));
	  }
      }
	
    if (!target.isEditable(local))
      {
	return Ganymede.createErrorDialog(ts.l("copyFieldTo.copy_error_sub"),
					  ts.l("copyFieldTo.no_write",
					       target.getName(), target.owner.getLabel()));
      }

    if (!isVector())
      {
	return target.setValueLocal(getValueLocal(), true); // inhibit wizards..
      }
    else
      {
	Vector valuesToCopy;

	/* -- */

	valuesToCopy = getValuesLocal();

	// we want to inhibit wizards and allow partial failure

	retVal = target.addElementsLocal(valuesToCopy, true, true);

	// the above operation could fail if we don't have write
	// privileges for the target field, so we'll return an
	// error code back to the cloneFromObject() method.

	// this isn't exactly the right thing to do if the failure
	// pertains to a single value that we attempted to add,
	// but if a value was legal in the source object, it
	// should generally be legal in the target object, so
	// undoing the total copy here isn't too horribly
	// inappropriate.

	// if this turns out to be unacceptable, i'll have to add
	// code here to build up a dialog describing the values
	// that could not be copied, which would be a bit of a
	// pain.
	
	if (retVal != null && !retVal.didSucceed())
	  {
	    return retVal;
	  }
      }

    return null;
  }

  /**
   * <p>This method is intended to be called when this field is being checked into
   * the database.  Subclasses of DBField will override this method to clean up
   * data that is cached for speed during editing.</p>
   */

  public void cleanup()
  {
  }

  // ****
  //
  // db_field methods
  // 
  // ****

  /**
   *
   * Returns a handy field description packet for this field,
   * containing the static field elements for this field..
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final FieldTemplate getFieldTemplate()
  {
    return getFieldDef().template;
  }

  /**
   *
   * Returns a handy field description packet for this field.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final FieldInfo getFieldInfo()
  {
    return new FieldInfo(this);
  }

  /**
   *
   * Returns the schema name for this field.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final String getName()
  {
    return getFieldDef().getName();
  }

  /**
   * <P>Returns the name for this field, encoded
   * in a form suitable for use as an XML element
   * name.</P>
   */

  public final String getXMLName()
  {
    return arlut.csd.Util.XMLUtils.XMLEncode(getFieldDef().getName());
  }

  /**
   *
   * Returns the field # for this field.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final short getID()
  {
    return fieldcode;
  }

  /**
   *
   * Returns the object this field is part of.
   * 
   */

  public final DBObject getOwner()
  {
    return owner;
  }

  /**
   *
   * Returns the description of this field from the
   * schema.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final String getComment()
  {
    return getFieldDef().getComment();
  }

  /**
   *
   * Returns the description of this field's type from
   * the schema.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final String getTypeDesc()
  {
    return getFieldDef().getTypeDesc();
  }

  /**
   *
   * Returns the type code for this field from the
   * schema.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   */

  public final short getType()
  {
    return getFieldDef().getType();
  }

  /**
   * <P>This method returns a text encoded value for this DBField
   * without checking permissions.</P>
   *
   * <P>This method avoids checking permissions because it is used on
   * the server side only and because it is involved in the 
   * {@link arlut.csd.ganymede.server.DBObject#getLabel() getLabel()}
   * logic for {@link arlut.csd.ganymede.server.DBObject DBObject}, 
   * which is invoked from {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.server.GanymedeSession#getPerm(arlut.csd.ganymede.server.DBObject) getPerm()} 
   * method.</P>
   *
   * <P>If this method checked permissions and the getPerm() method
   * failed for some reason and tried to report the failure using
   * object.getLabel(), as it does at present, the server could get
   * into an infinite loop.</P>
   */

  abstract public String getValueString();

  /**
   * <P>Returns a String representing a reversible encoding of the
   * value of this field.  Each field type will have its own encoding,
   * suitable for embedding in a {@link arlut.csd.ganymede.common.DumpResult DumpResult}.</P>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  abstract public String getEncodingString();

  /**
   * <P>Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.</P>
   *
   * <P>If there is no change in the field, null will be returned.</P>
   */

  abstract public String getDiffString(DBField orig);

  /**
   * <p>This method returns true if this field differs from the orig.
   * It is intended to do a quick before/after comparison when we are
   * handling a transaction commit.</p>
   */

  public boolean hasChanged(DBField orig)
  {
    if (orig == null)
      {
	return true;
      }

    if (!(orig.getClass().equals(this.getClass())))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    return (!this.equals(orig));
  }

  /**
   *
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   */

  public synchronized boolean isDefined()
  {
    if (isVector())
      {
	Vector values = getVectVal();

	if (values != null && values.size() > 0)
	  {
	    return true;
	  }
	else
	  {
	    return false;
	  }
      }
    else
      {
	if (value != null)
	  {
	    return true;
	  }
	else
	  {
	    return false;
	  }
      }
  }

  /**
   * <p>This method is used to mark a field as undefined when it is
   * checked out for editing.  Different subclasses of {@link
   * arlut.csd.ganymede.server.DBField DBField} may implement this in
   * different ways, if simply setting the field's value member to
   * null is not appropriate.  Any namespace values claimed by the
   * field will be released, and when the transaction is committed,
   * this field will be released.</p>
   *
   * <p>Note that this method is really only intended for those fields
   * which have some significant internal structure to them, such as
   * permission matrix, field option matrix, and password fields.</p>
   *
   * <p>NOTE: There is, at present, no defined DBEditObject callback
   * method that tracks generic field nullification.  This means that
   * if your code uses setUndefined on a PermissionMatrixDBField,
   * FieldOptionDBField, or PasswordDBField, the plugin code is not
   * currently given the opportunity to review and refuse that
   * operation.  Caveat Coder.</p>
   */

  public synchronized ReturnVal setUndefined(boolean local) throws GanyPermissionsException
  {
    if (isVector())
      {
	if (!isEditable(local))	// *sync* GanymedeSession possible
	  {
	    throw new GanyPermissionsException(ts.l("setUndefined.no_perm_vect", getName(), owner.getLabel()));
	  }

	// we have to clone our values Vector in order to use
	// deleteElements().

	Vector currentValues = (Vector) getVectVal().clone();

	if (currentValues.size() != 0)
	  {
	    return deleteElementsLocal(currentValues);
	  }
	else
	  {
	    return null;	// success
	  }
      }
    else
      {
	return setValue(null, local, false);
      }
  }

  /**
   *
   * Returns true if this field is a vector, false
   * otherwise.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final boolean isVector()
  {
    return getFieldDef().isArray();
  }

  /**
   * <P>Returns true if this field is editable, false
   * otherwise.</P>
   *
   * <P>Note that DBField are only editable if they are
   * contained in a subclass of
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}.</P>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final boolean isEditable()
  {
    return isEditable(false);
  }

  /**
   * <P>Returns true if this field is editable, false
   * otherwise.</P>
   *
   * <P>Note that DBField are only editable if they are
   * contained in a subclass of
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}.</P>
   *
   * <P>Server-side method only</P>
   *
   * <P><B>*Deadlock Hazard.*</B></P>
   *
   * @param local If true, skip permissions checking
   *   
   */

  public final boolean isEditable(boolean local)
  {
    DBEditObject eObj;

    /* -- */

    if (!(owner instanceof DBEditObject))
      {
	return false;
      }

    eObj = (DBEditObject) owner;

    // if our owner has already started the commit process, we can't
    // allow any changes, local access or no

    if (eObj.isCommitting())
      {
	return false;
      }

    if (!local && !verifyWritePermission()) // *sync* possible on GanymedeSession
      {
	return false;
      }

    return true;
  }

  /**
   * <p>This method returns true if this field is one of the
   * system fields present in all objects.</p>
   */

  public final boolean isBuiltIn()
  {
    return getFieldDef().isBuiltIn();
  }

  /**
   *
   * Returns true if this field should be displayed in the
   * current client context.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final boolean isVisible()
  {
    return verifyReadPermission() && 
      getFieldDef().base.getObjectHook().canSeeField(null, this);
  }

  /**
   *
   * Returns true if this field is edit in place.
   *
   */

  public final boolean isEditInPlace()
  {
    return getFieldDef().isEditInPlace();
  }

  /**
   *
   * Returns the object type id for the object containing this field.
   *
   */

  public final int getObjTypeID()
  {
    return getFieldDef().base().getTypeID();
  }

  /**
   *
   * Returns the DBNameSpace that this field is associated with, or
   * null if no NameSpace field is associated with this field.
   *
   */

  public final DBNameSpace getNameSpace()
  {
    return getFieldDef().getNameSpace();
  }

  /**
   *
   * Returns the DBObjectBaseField for this field.
   *
   */

  public final DBObjectBaseField getFieldDef()
  {
    return owner.getFieldDef(fieldcode);
  }

  /**
   *
   * This version of getFieldDef() is intended for use by code
   * sections that need to interrogate a field's type definition
   * before it is linked to an owner object.
   *
   */

  public final DBObjectBaseField getFieldDef(short objectType)
  {
    DBObjectBase base = Ganymede.db.getObjectBase(objectType);

    if (base == null)
      {
	return null;
      }

    return (DBObjectBaseField) base.getField(fieldcode);
  }

  /**
   *
   * This version of getFieldDef() is intended for use by code
   * sections that need to interrogate a field's type definition
   * before it is linked to an owner object.
   *
   */

  public final DBObjectBaseField getFieldDef(DBObjectBase base)
  {
    if (base == null)
      {
	return null;
      }

    return (DBObjectBaseField) base.getField(fieldcode);
  }

  /**
   *
   * Returns the value of this field, if a scalar.  An IllegalArgumentException
   * will be thrown if this field is a vector.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   */

  public Object getValue() throws GanyPermissionsException
  {
    if (!verifyReadPermission())
      {
	throw new GanyPermissionsException(ts.l("global.no_read_perms", getName(), owner.getLabel()));
      }

    if (isVector())
      {
	throw new GanyPermissionsException(ts.l("global.oops_vector", getName(), owner.getLabel()));
      }

    return value;
  }

  /**
   * <P>Sets the value of this field, if a scalar.</P>
   *
   * <P>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</P>
   *
   * <P>This method is intended to be called by code that needs to go
   * through the permission checking regime, and that needs to have
   * rescan information passed back.  This includes most wizard
   * setValue calls.</P>
   *
   * @see arlut.csd.ganymede.server.DBSession
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final ReturnVal setValue(Object value) throws GanyPermissionsException
  {
    ReturnVal result;

    /* -- */

    // do the thing, calling into our subclass

    result = setValue(value, false, false);

    return rescanThisField(result);
  }

  /**
   * <P>Sets the value of this field, if a scalar.</P>
   *
   * <P><B>This method is server-side only, and bypasses
   * permissions checking.</B></P>
   *
   * <P>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</P>
   */

  public final ReturnVal setValueLocal(Object value)
  {
    try
      {
	return setValue(value, true, false);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);
      }
  }

  /**
   * <P>Sets the value of this field, if a scalar.</P>
   *
   * <P><B>This method is server-side only, and bypasses permissions
   * checking.</B></P>
   *
   * <P>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.</P>
   *
   * @param value Value to set this field to
   * @param noWizards If true, wizards will be skipped
   */

  public final ReturnVal setValueLocal(Object value, boolean noWizards)
  {
    try
      {
	return setValue(value, true, noWizards);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);
      }
  }

  /**
   * <P>Sets the value of this field, if a scalar.</P>
   *
   * <P><B>This method is server-side only.</B></P>
   *
   * <P>The ReturnVal object returned encodes success or failure, and may
   * optionally pass back a dialog.</P>
   *
   * @param value Value to set this field to
   * @param local If true, permissions checking will be skipped
   */

  public final ReturnVal setValue(Object submittedValue, boolean local) throws GanyPermissionsException
  {
    return setValue(submittedValue, local, false);
  }

  /**
   * <P>Sets the value of this field, if a scalar.</P>
   *
   * <P><B>This method is server-side only.</B></P>
   *
   * <P>The ReturnVal object returned encodes success or failure, and may
   * optionally pass back a dialog.</P>
   *
   * <P>This method will be overridden by DBField subclasses with special
   * needs.</P>
   *
   * @param value Value to set this field to
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   */

  public synchronized ReturnVal setValue(Object submittedValue, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))	// *sync* possible
      {
	throw new GanyPermissionsException(ts.l("global.no_write_perms", getName(), owner.getLabel()));
      }

    if (isVector())
      {
	throw new IllegalArgumentException(ts.l("global.oops_vector", getName(), owner.getLabel()));
      }

    if (this.value == submittedValue || (this.value != null && this.value.equals(submittedValue)))
      {
	return retVal;		// no change (useful for null)
      }

    if (submittedValue instanceof String)
      {
	submittedValue = ((String) submittedValue).intern();
      }
    else if (submittedValue instanceof Invid)
      {
	submittedValue = ((Invid) submittedValue).intern();
      }

    retVal = verifyNewValue(submittedValue);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check
	
	retVal = eObj.wizardHook(this, DBEditObject.SETVAL, submittedValue, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // check to see if we can do the namespace manipulations implied by this
    // operation

    ns = getNameSpace();

    if (ns != null)
      {
	unmark(this.value);

	// if we're not being told to clear this field, try to mark the
	// new value

	if (submittedValue != null)
	  {
	    if (!mark(submittedValue))
	      {
		if (this.value != null)
		  {
		    mark(this.value); // we aren't clearing the old value after all
		  }
		
		return getConflictDialog("DBField.setValue()", submittedValue);
	      }
	  }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    newRetVal = eObj.finalizeSetValue(this, submittedValue);

    if (newRetVal == null || newRetVal.didSucceed())
      {
	if (submittedValue != null)
	  {
	    this.value = submittedValue;
	  }
	else
	  {
	    this.value = null;
	  }

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeSetValue() call.

	if (retVal != null)
	  {
	    return retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    return newRetVal;		// success
	  }
      }
    else
      {
	// our owner disapproved of the operation,
	// undo the namespace manipulations, if any,
	// and finish up.

	if (ns != null)
	  {
	    unmark(submittedValue);
	    mark(this.value);
	  }

	// go ahead and return the dialog that was set by finalizeSetValue().

	return newRetVal;
      }
  }

  /** 
   * <p>Returns a Vector of the values of the elements in this field,
   * if a vector.</p>
   *
   * <p>This is only valid for vectors.  If the field is a scalar, use
   * getValue().</p>
   *
   * <p>This method checks for read permissions.</p>
   *
   * <p><b>Be very careful using this for server-side code, because
   * the Vector returned is not cloned from the field's actual data
   * Vector, for performance reasons.  If this is called by the client,
   * the serialization process will protect us from the client being
   * able to mess with our contents.</b></p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public Vector getValues() throws GanyPermissionsException
  {
    if (!verifyReadPermission())
      {
	throw new GanyPermissionsException("permission denied to read this field " + 
					 getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + 
					   getName());
      }

    return getVectVal();
  }

  /**
   *
   * <p>Returns the value of an element of this field,
   * if a vector.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   */

  public Object getElement(int index) throws GanyPermissionsException
  {
    if (!verifyReadPermission())
      {
	throw new GanyPermissionsException("permission denied to read this field " + getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + getName());
      }

    if (index < 0)
      {
	throw new ArrayIndexOutOfBoundsException("invalid index " + index + " on field " + getName());
      }

    return getVectVal().elementAt(index);
  }

  /**
   * <p>Returns the value of an element of this field,
   * if a vector.</p>
   */

  public Object getElementLocal(int index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + getName());
      }

    if (index < 0)
      {
	throw new ArrayIndexOutOfBoundsException("invalid index " + index + " on field " + getName());
      }

    return getVectVal().elementAt(index);
  }

  /**
   * <p>Sets the value of an element of this field, if a vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.</p>
   *
   * <p>The ReturnVal resulting from a successful setElement will
   * encode an order to rescan this field.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @see arlut.csd.ganymede.server.DBSession
   * @see arlut.csd.ganymede.rmi.db_field
   */
  
  public final ReturnVal setElement(int index, Object value) throws GanyPermissionsException
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + getName());
      }

    if (value == null)
      {
	return Ganymede.createErrorDialog("Field Error",
					  "Null value passed to " + owner.getLabel() + ":" + 
					  getName() + ".setElement()");
      }

    if ((index < 0) || (index > getVectVal().size()))
      {
	throw new ArrayIndexOutOfBoundsException("invalid index " + index);
      }

    return rescanThisField(setElement(index, value, false, false));
  }

  /**
   *
   * <p>Sets the value of an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @see arlut.csd.ganymede.server.DBSession
   *
   */
  
  public final ReturnVal setElementLocal(int index, Object value)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + getName());
      }

    if (value == null)
      {
	return Ganymede.createErrorDialog("Error, bad value",
					  "Null value passed to " + owner.getLabel() + ":" + 
					  getName() + ".setElement()");
      }

    if ((index < 0) || (index > getVectVal().size()))
      {
	throw new ArrayIndexOutOfBoundsException("invalid index " + index);
      }

    try
      {
	return setElement(index, value, true, false);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should not happen
      }
  }

  /**
   * <p>Sets the value of an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  A null result means the
   * operation was carried out successfully and no information
   * needed to be passed back about side-effects.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   */

  public final ReturnVal setElement(int index, Object submittedValue, boolean local) throws GanyPermissionsException
  {
    return setElement(index, submittedValue, local, false);
  }

  /**
   * <p>Sets the value of an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  A null result means the
   * operation was carried out successfully and no information
   * needed to be passed back about side-effects.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   */
  
  public synchronized ReturnVal setElement(int index, Object submittedValue, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + getName());
      }

    if (!isEditable(local))	// *sync* on GanymedeSession possible.
      {
	throw new GanyPermissionsException("don't have permission to change field /  non-editable object, field " +
					 getName());
      }

    Vector values = getVectVal();

    // make sure we're not duplicating an item

    int oldIndex = values.indexOf(submittedValue);

    if (oldIndex == index)
      {
	return null;		// no-op
      }
    else if (oldIndex != -1)
      {
	return getDuplicateValueDialog("setElement", submittedValue); // duplicate
      }

    // make sure that the constraints on this field don't rule out, prima facie, the proposed value

    retVal = verifyNewValue(submittedValue);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // allow the plugin class to review the operation

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.SETELEMENT, new Integer(index), submittedValue);

	// if a wizard intercedes, we are going to let it take the ball.

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // okay, we're going to proceed.. unless there's a namespace
    // violation

    ns = this.getNameSpace();

    if (ns != null)
      {
	unmark(values.elementAt(index));

	if (!mark(submittedValue))
	  {
	    mark(values.elementAt(index)); // we aren't clearing the old value after all

	    return getConflictDialog("DBField.setElement()", submittedValue);
	  }
      }

    // check our owner, do it.  Checking our owner should be the last
    // thing we do.. if it returns true, nothing should stop us from
    // running the change to completion

    newRetVal = eObj.finalizeSetElement(this, index, submittedValue);

    if (newRetVal == null || newRetVal.didSucceed())
      {
	values.setElementAt(submittedValue, index);

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeSetElement() call.

	if (retVal != null)
	  {
	    return retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    return newRetVal;		// success
	  }
      }
    else
      {
	// our owner disapproved of the operation,
	// undo the namespace manipulations, if any,
	// and finish up.

	if (ns != null)
	  {
	    // values is in its final state.. if the submittedValue
	    // isn't in it anywhere, unmark it in the namespace

	    if (!values.contains(submittedValue))
	      {
		unmark(submittedValue);
	      }

	    // mark the old value.. we can always do this safely, even
	    // if the value was already marked

	    mark(values.elementAt(index));
	  }

	// return the error dialog from finalizeSetElement().

	return newRetVal;
      }
  }

  /**
   * <p>Adds an element to the end of this field, if a vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.</p>
   *
   * <p>The ReturnVal resulting from a successful addElement will
   * encode an order to rescan this field.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final ReturnVal addElement(Object value) throws GanyPermissionsException
  {
    return rescanThisField(addElement(value, false, false));
  }

  /**
   * <P>Adds an element to the end of this field, if a vector.</P>
   *
   * <P>Server-side method only</P>
   *
   * <P>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</P>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   */

  public final ReturnVal addElementLocal(Object value)
  {
    try
      {
	return addElement(value, true, false);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should never happen
      }
  }

  /**
   * <P>Adds an element to the end of this field, if a vector.</P>
   *
   * <P>Server-side method only</P>
   *
   * <P>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</P>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @param submittedValue Value to be added
   * @param local If true, permissions checking will be skipped
   */

  public final ReturnVal addElement(Object submittedValue, boolean local) throws GanyPermissionsException
  {
    return addElement(submittedValue, local, false);
  }

  /**
   * <P>Adds an element to the end of this field, if a vector.</P>
   *
   * <P>Server-side method only</P>
   *
   * <P>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</P>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @param submittedValue Value to be added
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   */

  public synchronized ReturnVal addElement(Object submittedValue, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))	// *sync* on GanymedeSession possible
      {
	throw new GanyPermissionsException("don't have permission to change field /  non-editable object " + 
					 getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + 
					   getName());
      }

    if (submittedValue == null)
      {
	throw new IllegalArgumentException("null value passed to addElement.");
      }

    if (submittedValue instanceof String)
      {
	submittedValue = ((String) submittedValue).intern();
      }
    else if (submittedValue instanceof Invid)
      {
	submittedValue = ((Invid) submittedValue).intern();
      }

    // make sure we're not duplicating an item

    if (getVectVal().contains(submittedValue))
      {
	return getDuplicateValueDialog("addElement", submittedValue); // duplicate
      }

    // verifyNewValue should setLastError for us.

    retVal = verifyNewValue(submittedValue);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    if (size() >= getMaxArraySize())
      {
	return Ganymede.createErrorDialog("Server: Error in DBField.addElement()",
					  "Field " + getName() + 
					  " already at or beyond array size limit");
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.ADDELEMENT, submittedValue, null);

	// if a wizard intercedes, we are going to let it take the ball.

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    ns = getNameSpace();

    if (ns != null)
      {
	if (!mark(submittedValue))	// *sync* DBNameSpace
	  {
	    return getConflictDialog("DBField.addElement()", submittedValue);
	  }
      }

    newRetVal = eObj.finalizeAddElement(this, submittedValue);

    if (newRetVal == null || newRetVal.didSucceed()) 
      {
	getVectVal().addElement(submittedValue);

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeAddElement() call.

	if (retVal != null)
	  {
	    return retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    return newRetVal;		// success
	  }
      } 
    else
      {
	if (ns != null)
	  {
	    // if the value that we were going to add is not
	    // left in our vector, unmark the to-be-added
	    // value

	    if (!getVectVal().contains(submittedValue))
	      {
		unmark(submittedValue);	// *sync* DBNameSpace
	      }
	  }

	// return the error dialog created by finalizeAddElement

	return newRetVal;
      }
  }

  /**
   * <p>Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.</p>
   *
   * <p>The ReturnVal resulting from a successful addElements will
   * encode an order to rescan this field.</p> 
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final ReturnVal addElements(Vector values) throws GanyPermissionsException
  {
    return rescanThisField(addElements(values, false, false));
  }

  /**
   * <p>Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.</p>
   *
   * <P>Server-side method only</P>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   */

  public final ReturnVal addElementsLocal(Vector values)
  {
    try
      {
	return addElements(values, true, false);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should never happen
      }
  }


  /**
   * <p>Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.</p>
   *
   * <P>Server-side method only</P>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @param submittedValues Values to be added
   * @param noWizards If true, wizards will be skipped
   * @param copyFieldMode If true, addElements will add any values
   * that it can, even if some values are refused by the server logic.
   * Any values that are skipped will be reported in a dialog passed
   * back in the returned ReturnVal.  This is intended to support
   * vector field cloning, in which we add what values may be cloned,
   * and skip the rest.
   */

  public final ReturnVal addElementsLocal(Vector values, boolean noWizards, boolean copyFieldMode)
  {
    try
      {
	return addElements(values, true, noWizards, copyFieldMode);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should never happen
      }
  }

  /**
   * <p>Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.</p>
   *
   * <P>Server-side method only</P>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @param submittedValues Values to be added
   * @param local If true, permissions checking will be skipped
   */

  public final ReturnVal addElements(Vector submittedValues, boolean local) throws GanyPermissionsException
  {
    return addElements(submittedValues, local, false);
  }

  /**
   * <p>Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.</p>
   *
   * <P>Server-side method only</P>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @param submittedValues Values to be added
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   */

  public final ReturnVal addElements(Vector submittedValues, boolean local,
				     boolean noWizards) throws GanyPermissionsException
  {
    return addElements(submittedValues, local, noWizards, false);
  }

  /**
   * <p>Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.</p>
   *
   * <P>Server-side method only</P>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @param submittedValues Values to be added
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   * @param copyFieldMode If true, addElements will add any values that
   * it can, even if some values are refused by the server logic.  Any
   * values that are skipped will be reported in a dialog passed back
   * in the returned ReturnVal
   */

  public synchronized ReturnVal addElements(Vector submittedValues, boolean local, 
					    boolean noWizards, boolean copyFieldMode) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;
    DBEditSet editset;
    Vector approvedValues = new Vector();

    /* -- */

    if (!isEditable(local))	// *sync* on GanymedeSession possible
      {
	throw new GanyPermissionsException("don't have permission to change field /  non-editable object " + 
					 getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + 
					   getName());
      }

    if (submittedValues == null || submittedValues.size() == 0)
      {
	return Ganymede.createErrorDialog("Server: Error in DBField.addElements()",
					  "Field " + getName() + " can't add a null/empty vector");
      }

    if (submittedValues == getVectVal())
      {
	throw new IllegalArgumentException("can't add field values to itself");
      }

    Vector duplicateValues = VectorUtils.intersection(getVectVal(), submittedValues);

    if (duplicateValues.size() > 0)
      {
	if (!copyFieldMode)
	  {
	    return getDuplicateValuesDialog("addElements", VectorUtils.vectorString(duplicateValues));
	  }
	else
	  {
	    submittedValues = VectorUtils.difference(submittedValues, getVectVal());
	  }
      }

    // can we add this many values?

    if (size() + submittedValues.size() > getMaxArraySize())
      {
	return Ganymede.createErrorDialog("Server: Error in DBField.addElements()",
					  "Field " + getName() + 
					  " can't take " + submittedValues.size() + " new values..\n" +
					  "size():" + size() + ", getMaxArraySize():" + getMaxArraySize());
      }

    // check to see if all of the submitted values are acceptable in
    // type and in identity.  if copyFieldMode, we won't complain
    // unless none of the submitted values are acceptable

    StringBuffer errorBuf = new StringBuffer();

    for (int i = 0; i < submittedValues.size(); i++)
      {
	Object submittedValue = submittedValues.elementAt(i);

	if (submittedValue instanceof String)
	  {
	    submittedValues.set(i, ((String) submittedValue).intern());
	  }
	else if (submittedValue instanceof Invid)
	  {
	    submittedValues.set(i, ((Invid) submittedValue).intern());
	  }
	
	retVal = verifyNewValue(submittedValue);

	if (retVal != null && !retVal.didSucceed())
	  {
	    if (!copyFieldMode)
	      {
		return retVal;
	      }
	    else
	      {
		if (retVal.getDialog() != null)
		  {
		    if (errorBuf.length() != 0)
		      {
			errorBuf.append("\n\n");
		      }

		    errorBuf.append(retVal.getDialog().getText());
		  }
	      }
	  }
	else
	  {
	    approvedValues.addElement(submittedValues.elementAt(i));
	  }
      }

    if (approvedValues.size() == 0)
      {
	return Ganymede.createErrorDialog("AddElements Error",
					  errorBuf.toString());
      }

    // see if our container wants to intercede in the adding operation

    eObj = (DBEditObject) owner;
    editset = eObj.getEditSet();

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.ADDELEMENTS, approvedValues, null);

	// if a wizard intercedes, we are going to let it take the ball.

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // check to see if the all of the values being added are
    // acceptable to a namespace constraint

    ns = getNameSpace();

    if (ns != null)
      {
	synchronized (ns)
	  {
	    for (int i = 0; i < approvedValues.size(); i++)
	      {
		if (!ns.testmark(editset, approvedValues.elementAt(i)))
		  {
		    return getConflictDialog("DBField.addElements()", approvedValues.elementAt(i));
		  }
	      }
	
	    for (int i = 0; i < approvedValues.size(); i++)
	      {
		if (!ns.mark(editset, approvedValues.elementAt(i), this))
		  {
		    throw new RuntimeException("error: testmark / mark inconsistency");
		  }
	      }
	  }
      }

    // okay, see if the DBEditObject is willing to allow all of these
    // elements to be added

    newRetVal = eObj.finalizeAddElements(this, approvedValues);

    if (newRetVal == null || newRetVal.didSucceed()) 
      {
	// okay, we're allowed to do it, so we add them all

	for (int i = 0; i < approvedValues.size(); i++)
	  {
	    getVectVal().addElement(approvedValues.elementAt(i));
	  }

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeAddElement() call.

	if (retVal != null)
	  {
	    newRetVal = retVal.unionRescan(newRetVal);
	  }

	if (newRetVal == null)
	  {
	    newRetVal = new ReturnVal(true, true);
	  }

	// if we were not able to copy some of the values (and we
	// had copyFieldMode set), encode a description of what
	// happened along with the success code
	
	if (errorBuf.length() != 0)
	  {
	    newRetVal.setDialog(new JDialogBuff("Warning",
						errorBuf.toString(),
						"Ok",
						null,
						"ok.gif"));
	  }

	return newRetVal;
      } 
    else
      {
	if (ns != null)
	  {
	    // for each value that we were going to add (and which we
	    // marked in our namespace above), we need to unmark it if
	    // it is not contained in our vector at this point.

	    Vector currentValues = getVectVal();

	    // build up a hashtable of our current values so we can
	    // efficiently do membership checks for our namespace

	    Hashtable valuesLeft = new Hashtable(currentValues.size());

	    for (int i = 0; i < currentValues.size(); i++)
	      {
		valuesLeft.put(currentValues.elementAt(i), currentValues.elementAt(i));
	      }

	    // for each item we were submitted, unmark it in our
	    // namespace if we don't have it left in our vector.

	    for (int i = 0; i < approvedValues.size(); i++)
	      {
		if (!valuesLeft.containsKey(approvedValues.elementAt(i)))
		  {
		    if (!ns.unmark(editset, approvedValues.elementAt(i), this))
		      {
			throw new RuntimeException(ts.l("global.bad_unmark", approvedValues.elementAt(i), this));
		      }
		  }
	      }
	  }

	// return the error dialog created by finalizeAddElements

	return newRetVal;
      }
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, 
   * and may optionally pass back a dialog.</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final ReturnVal deleteElement(int index) throws GanyPermissionsException
  {
    return rescanThisField(deleteElement(index, false, false));
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   */

  public final ReturnVal deleteElementLocal(int index)
  {
    try
      {
	return deleteElement(index, true, false);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should never happen
      }
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   */

  public final ReturnVal deleteElement(int index, boolean local) throws GanyPermissionsException
  {
    return deleteElement(index, local, false);
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   */

  public synchronized ReturnVal deleteElement(int index, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))	// *sync* GanymedeSession possible
      {
	throw new GanyPermissionsException("don't have permission to change field /  non-editable object " + 
					 getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + getName());
      }

    Vector values = getVectVal();

    if ((index < 0) || (index >= values.size()))
      {
	throw new ArrayIndexOutOfBoundsException("invalid index " + index + 
						 " in deleting element in field " + getName());
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.DELELEMENT, new Integer(index), null);

	// if a wizard intercedes, we are going to let it take the ball.

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    newRetVal = eObj.finalizeDeleteElement(this, index);

    if (newRetVal == null || newRetVal.didSucceed())
      {
	Object valueToDelete = values.elementAt(index);
	values.removeElementAt(index);

	// if this field no longer contains the element that
	// we are deleting, we're going to unmark that value
	// in our namespace
	
	if (!values.contains(valueToDelete))
	  {
	    unmark(valueToDelete);
	  }

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeDeleteElement() call.

	if (retVal != null)
	  {
	    return retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    return newRetVal;		// success
	  }
      }
    else
      {
	return newRetVal;
      }
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, 
   * and may optionally pass back a dialog.</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final ReturnVal deleteElement(Object value) throws GanyPermissionsException
  {
    return rescanThisField(deleteElement(value, false, false));
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   */

  public final ReturnVal deleteElementLocal(Object value)
  {
    try
      {
	return deleteElement(value, true, false);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should never happen
      }
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   */

  public final ReturnVal deleteElement(Object value, boolean local) throws GanyPermissionsException
  {
    return deleteElement(value, local, false);
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   */

  public synchronized ReturnVal deleteElement(Object value, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    if (!isEditable(local))	// *sync* GanymedeSession possible
      {
	throw new GanyPermissionsException("don't have permission to change field /  non-editable object " +
					 getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + 
					   getName());
      }

    if (value == null)
      {
	return Ganymede.createErrorDialog("Server: Error in DBField.deleteElement()",
					  "Could not delete null value from field " + getName());
      }

    int index = indexOfValue(value);

    if (index == -1)
      {
	return Ganymede.createErrorDialog("Server: Error in DBField.deleteElement()",
					  "Could not delete value " + value +
					  ", not present in field " + getName());
      }

    return deleteElement(index, local, noWizards);	// *sync* DBNameSpace possible
  }

  /**
   * <p>Removes all elements from this field, if a
   * vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a 
   * failure code is returned, no elements in values were removed.</p>
   *
   * <p>The ReturnVal resulting from a successful deleteAllElements will
   * encode an order to rescan this field.</p> 
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public ReturnVal deleteAllElements() throws GanyPermissionsException
  {
    return this.deleteElements(this.getValues());
  }

  /**
   * <p>Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a 
   * failure code is returned, no elements in values were removed.</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElements will
   * encode an order to rescan this field.</p> 
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final ReturnVal deleteElements(Vector values) throws GanyPermissionsException
  {
    return rescanThisField(deleteElements(values, false, false));
  }

  /**
   * <p>Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a 
   * failure code is returned, no elements in values were removed.</p>
   *
   * <P>Server-side method only</P>
   */

  public final ReturnVal deleteElementsLocal(Vector values)
  {
    try
      {
	return deleteElements(values, true, false);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should never happen
      }
  }

  /**
   * <p>Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a 
   * failure code is returned, no elements in values were removed.</p>
   *
   * <P>Server-side method only</P>
   */

  public final ReturnVal deleteElements(Vector valuesToDelete, boolean local) throws GanyPermissionsException
  {
    return deleteElements(valuesToDelete, local, false);
  }

  /**
   * <p>Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a 
   * failure code is returned, no elements in values were removed.</p>
   *
   * <P>Server-side method only</P>
   */

  public synchronized ReturnVal deleteElements(Vector valuesToDelete, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;
    DBEditSet editset;
    Vector currentValues;

    /* -- */

    if (!isEditable(local))	// *sync* on GanymedeSession possible
      {
	throw new GanyPermissionsException("don't have permission to change field /  non-editable object " + 
					 getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + 
					   getName());
      }

    if (valuesToDelete == null || valuesToDelete.size() == 0)
      {
	return Ganymede.createErrorDialog("Server: Error in DBField.addElements()",
					  "Field " + getName() + " can't remove a null/empty vector");
      }

    // get access to our value vector.

    currentValues = getVectVal();

    // make sure the two vectors we're going to be manipulating aren't
    // actually the same vector

    if (valuesToDelete == currentValues)
      {
	throw new IllegalArgumentException("can't remove field values from itself");
      }

    // see if we are being asked to remove items not in our vector

    Vector notPresent = VectorUtils.minus(valuesToDelete, currentValues);

    if (notPresent.size() != 0)
      {
	return Ganymede.createErrorDialog("Server: Error in DBField.deleteElements()",
					  "Field " + getName() + " can't remove non-present items: " +
					  VectorUtils.vectorString(notPresent));
      }

    // see if our container wants to intercede in the removing operation

    eObj = (DBEditObject) owner;
    editset = eObj.getEditSet();

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.DELELEMENTS, valuesToDelete, null);

	// if a wizard intercedes, we are going to let it take the ball.

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // okay, see if the DBEditObject is willing to allow all of these
    // elements to be removed

    newRetVal = eObj.finalizeDeleteElements(this, valuesToDelete);

    if (newRetVal == null || newRetVal.didSucceed()) 
      {
	// okay, we're allowed to remove, so take the items out

	for (int i = 0; i < valuesToDelete.size(); i++)
	  {
	    currentValues.removeElement(valuesToDelete.elementAt(i));
	  }

	// if this vector is connected to a namespace, clear out what
	// we've left out from the namespace

	ns = getNameSpace();

	if (ns != null)
	  {
	    // build up a hashtable of our current values so we can
	    // efficiently do membership checks for our namespace

	    Hashtable valuesLeft = new Hashtable(currentValues.size());

	    for (int i = 0; i < currentValues.size(); i++)
	      {
		valuesLeft.put(currentValues.elementAt(i), currentValues.elementAt(i));
	      }

	    // for each item we were submitted, unmark it in our
	    // namespace if we don't have it left in our vector.

	    for (int i = 0; i < valuesToDelete.size(); i++)
	      {
		if (!valuesLeft.containsKey(valuesToDelete.elementAt(i)))
		  {
		    if (!ns.unmark(editset, valuesToDelete.elementAt(i), this))
		      {
			throw new RuntimeException(ts.l("global.bad_unmark", valuesToDelete.elementAt(i), this));
		      }
		  }
	      }
	  }

	if (retVal != null)
	  {
	    return retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    return newRetVal;		// success
	  }
      } 
    else
      {
	return newRetVal;
      }
  }

  /**
   * <p>Returns true if this field is a vector field and value is contained
   *  in this field.</p>
   *
   * <p>This method always checks for read privileges.</p>
   *
   * @param value The value to look for in this field
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final boolean containsElement(Object value) throws GanyPermissionsException
  {
    return containsElement(value, false);
  }

  /**
   * <p>This method returns true if this field is a vector
   * field and value is contained in this field.</p>
   *
   * <p>This method is server-side only, and never checks for read
   * privileges.</p>
   *
   * @param value The value to look for in this fieldu
   */

  public final boolean containsElementLocal(Object value)
  {
    try
      {
	return containsElement(value, true);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should never happen
      }
  }

  /**
   * <p>This method returns true if this field is a vector
   * field and value is contained in this field.</p>
   *
   * <p>This method is server-side only.</p>
   *
   * @param value The value to look for in this field
   * @param local If false, read permissin is checked for this field
   */

  public boolean containsElement(Object value, boolean local) throws GanyPermissionsException
  {
    if (!local && !verifyReadPermission())
      {
	throw new GanyPermissionsException("permission denied to read this field " + getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + 
					   getName());
      }

    return (indexOfValue(value) != -1);
  }

  /**
   * Returns a {@link arlut.csd.ganymede.server.fieldDeltaRec fieldDeltaRec} 
   * object listing the changes between this field's state and that
   * of the prior oldField state.
   */

  public fieldDeltaRec getVectorDiff(DBField oldField)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + getName());
      }

    if (oldField == null)
      {
	throw new IllegalArgumentException("can't compare fields.. oldField is null");
      }

    if ((oldField.getID() != getID()) ||
	(oldField.getObjTypeID() != getObjTypeID()))
      {
	throw new IllegalArgumentException("can't compare fields.. incompatible fields");
      }

    /* - */

    fieldDeltaRec deltaRec = new fieldDeltaRec(getID());
    Vector oldValues = oldField.getVectVal();
    Vector newValues = getVectVal();
    Vector addedValues = VectorUtils.difference(newValues, oldValues);
    Vector deletedValues = VectorUtils.difference(oldValues, newValues);

    for (int i = 0; i < addedValues.size(); i++)
      {
	deltaRec.addValue(addedValues.elementAt(i));
      }

    for (int i = 0; i < deletedValues.size(); i++)
      {
	deltaRec.delValue(deletedValues.elementAt(i));
      }

    return deltaRec;
  }

  /**
   * <p>Package-domain method to set the owner of this field.</p>
   *
   * <p>Used by the DBObject copy constructor.</p>
   */

  synchronized void setOwner(DBObject owner)
  {
    this.owner = owner;
  }

  // ****
  //
  // Server-side namespace management functions
  //
  // ****

  /** 
   * <p>unmark() is used to make any and all namespace values in this
   * field as available for use by other objects in the same editset.
   * When the editset is committed, any unmarked values will be
   * flushed from the namespace.</p>
   *
   * <p><b>*Calls synchronized methods on DBNameSpace*</b></p>
   */

  void unmark()
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();
    editset = ((DBEditObject) owner).getEditSet();

    if (namespace == null)
      {
	return;
      }

    if (!isVector())
      {
	if (!namespace.unmark(editset, this.key(), this))
	  {
	    throw new RuntimeException(ts.l("global.bad_unmark", this.key(), this));
	  }
      }
    else
      {
	synchronized (namespace)
	  {
	    for (int i = 0; i < size(); i++)
	      {
		if (!namespace.testunmark(editset, key(i), this))
		  {
		    throw new RuntimeException(ts.l("global.bad_unmark", this.key(), this));
		  }
	      }
	
	    for (int i = 0; i < size(); i++)
	      {
		if (!namespace.unmark(editset, key(i), this))
		  {
		    // "Error: testunmark() / unmark() inconsistency"
		    throw new RuntimeException(ts.l("unmark.testunmark_problem"));
		  }
	      }

	    return;
	  }
      }
  }

  /**
   * <p>Unmark a specific value associated with this field, rather
   * than unmark all values associated with this field.  Note
   * that this method does not check to see if the value is
   * currently associated with this field, it just goes ahead
   * and unmarks it.  This is to be used by the vector
   * modifiers (setElement, addElement, deleteElement, etc.)
   * to keep track of namespace modifications as we go along.</p>
   *
   * <p>If there is no namespace associated with this field, this
   * method will always return true, as a no-op.</p>
   *
   * <p><b>*Calls synchronized methods on DBNameSpace*</b></p>
   */

  boolean unmark(Object value)
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();
    editset = ((DBEditObject) owner).getEditSet();

    if (namespace == null)
      {
	return true;		// do a no-op
      }

    if (value == null)
      {
	return true;		// no previous value
      }

    return namespace.unmark(editset, value, this);
  }

  /** 
   * <p>mark() is used to mark any and all values in this field as taken
   * in the namespace.  When the editset is committed, marked values
   * will be permanently reserved in the namespace.  If the editset is
   * instead aborted, the namespace values will be returned to their
   * pre-editset status.</p>
   *
   * <p>If there is no namespace associated with this field, this
   * method will always return true, as a no-op.</p>
   *  
   * <p><b>*Calls synchronized methods on DBNameSpace*</b></p>
   */

  boolean mark()
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();

    if (namespace == null)
      {
	return true;		// do a no-op
      }

    editset = ((DBEditObject) owner).getEditSet();

    if (!isVector())
      {
	return namespace.mark(editset, this.key(), this);
      }
    else
      {
	synchronized (namespace)
	  {
	    for (int i = 0; i < size(); i++)
	      {
		if (!namespace.testmark(editset, key(i)))
		  {
		    return false;
		  }
	      }
	
	    for (int i = 0; i < size(); i++)
	      {
		if (!namespace.mark(editset, key(i), this))
		  {
		    throw new RuntimeException("error: testmark / mark inconsistency");
		  }
	      }

	    return true;
	  }
      }
  }

  /**
   * <p>Mark a specific value associated with this field, rather than
   * mark all values associated with this field.  Note that this
   * method does not in any way associate this value with this field
   * (add it, set it, etc.), it just marks it.  This is to be used by
   * the vector modifiers (setElement, addElement, etc.)  to keep
   * track of namespace modifications as we go along.</p>
   * 
   * <p><b>*Calls synchronized methods on DBNameSpace*</b></p>
   */

  boolean mark(Object value)
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();
    editset = ((DBEditObject) owner).getEditSet();

    if (namespace == null)
      {
	return false;		// should we throw an exception?
      }

    if (value == null)
      {
	return false;
	//	throw new NullPointerException("null value in mark()");
      }

    if (editset == null)
      {
	throw new NullPointerException("null editset in mark()");
      }

    return namespace.mark(editset, value, this);
  }

  // ****
  //
  // Methods for subclasses to override to implement the
  // behavior for this field.
  //
  // ****

  /**
   * Overridable method to determine whether an
   * Object submitted to this field is of an appropriate
   * type.
   */

  abstract public boolean verifyTypeMatch(Object o);

  /**
   * Overridable method to verify that an object
   * submitted to this field has an appropriate
   * value.
   */

  abstract public ReturnVal verifyNewValue(Object o);

  /** 
   * <P>Overridable method to verify that the current {@link
   * arlut.csd.ganymede.server.DBSession DBSession} / {@link
   * arlut.csd.ganymede.server.DBEditSet DBEditSet} has permission to read
   * values from this field.</P>
   */

   public boolean verifyReadPermission()
   {
     if (owner.getGSession() == null)
       {
	 return true; // we don't know who is looking at us, assume it's a server-local access
       }

     PermEntry pe = owner.getFieldPerm(getID());

     if (pe == null)
       {
	 return false;
       }

     return pe.isVisible();
   }

  /** 
   * <P>Overridable method to verify that the current {@link
   * arlut.csd.ganymede.server.DBSession DBSession} / {@link
   * arlut.csd.ganymede.server.DBEditSet DBEditSet} has permission to read
   * values from this field.</P>
   *
   * <p>This version of verifyReadPermission() is intended to be used
   * in a context in which it would be too expensive to make a
   * read-only duplicate copy of a DBObject from the DBObjectBase's
   * object table, strictly for the purpose of associating a
   * GanymedeSession with the DBObject for permissions
   * verification.</p>
   */

   public boolean verifyReadPermission(GanymedeSession gSession)
   {
     if (gSession == null)
       {
	 return true; // we don't know who is looking at us, assume it's a server-local access
       }

     PermEntry pe = gSession.getPerm(owner, getID());

     // if there is no permission explicitly recorded for the field,
     // inherit from the object as a whole

     if (pe == null)
       {
	 pe = gSession.getPerm(owner);
       }

     if (pe == null)
       {
	 return false;
       }

     return pe.isVisible();
   }

  /**
   * Overridable method to verify that the current
   * DBSession / DBEditSet has permission to write
   * values into this field.
   */

  public boolean verifyWritePermission()
  {
    if (owner instanceof DBEditObject)
      {
	PermEntry pe = owner.getFieldPerm(getID());

	if (pe == null)
	  {
	    return false;
	  }

	return pe.isEditable();
      }
    else
      {
	return false;  // if we're not in a transaction, we certainly can't be edited.
      }
  }

  /**
   * <p>Sub-class hook to support elements for which the default
   * equals() test is inadequate, such as IP addresses (represented
   * as arrays of Byte[] objects.</p>
   *
   * <p>Returns -1 if the value was not found in this field.</p>
   *
   * <p>This method assumes that the calling method has already verified
   * that this is a vector field.</p>
   */

  public int indexOfValue(Object value)
  {
    return getVectVal().indexOf(value);
  }

  /** 
   * <P>Returns a Vector of the values of the elements in this field, if
   * a vector.</P>
   *
   * <P>This is intended to be used within the Ganymede server, it
   * bypasses the permissions checking that getValues() does.</P>
   *
   * <P>The server code <b>*must not*</b> make any modifications to the
   * returned vector as doing such may violate the namespace maintenance
   * logic.  Always, <b>always</b>, use the addElement(), deleteElement(),
   * setElement() methods in this class.</P>
   *
   * <P>Remember, this method gives you <b>*direct access</b> to the vector
   * from this field.  Always always clone the Vector returned if you
   * find you need to modify the results you get back.  I'm trusting you
   * here.  Pay attention.</P>
   */

  public Vector getValuesLocal()
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + 
					   getName());
      }

    return getVectVal();
  }

  /** 
   * <P>Returns an Object carrying the value held in this field.</P>
   *
   * <P>This is intended to be used within the Ganymede server, it bypasses
   * the permissions checking that getValues() does.</P>
   */

  public Object getValueLocal()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector field " + 
					   getName());
      }

    return value;
  }

  // ***
  //
  // The following two methods implement checkpoint and rollback facilities for
  // DBField.  These methods save the field's internal state and restore it
  // on demand at a later time.  The intent is to allow checkpoint/restore
  // without changing the object identity (memory address) of the DBField so
  // that the DBEditSet checkpoint/restore logic can work.
  //
  // ***

  /**
   * <P>This method is used to basically dump state out of this field
   * so that the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}
   * {@link arlut.csd.ganymede.server.DBEditSet#checkpoint(java.lang.String) checkpoint()}
   * code can restore it later if need be.</P>
   *
   * <P>This method is not synchronized because all operations performed
   * by this method are either synchronized at a lower level or are
   * atomic.</P>
   *
   * <P>Called by {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}'s
   * {@link arlut.csd.ganymede.server.DBEditObject#checkpoint() checkpoint()}
   * method.</P>
   */

  public Object checkpoint()
  {
    if (isVector())
      {
	return getVectVal().clone();
      }
    else
      {
	return value;
      }
  }

  /**
   * <P>This method is used to basically force state into this field.</P>
   *
   * <P>It is used to place a value or set of values that were known to
   * be good during the current transaction back into this field,
   * without creating or changing this DBField's object identity, and
   * without doing any of the checking or side effects that calling
   * setValue() will typically do.</P>
   *
   * <P>In particular, it is not necessary to subclass this method for
   * use with {@link arlut.csd.ganymede.server.InvidDBField InvidDBField}, since
   * the {@link arlut.csd.ganymede.server.DBEditSet#rollback(java.lang.String) rollback()}
   * method will always rollback all objects in the transaction at the same
   * time.  It is not necessary to have the InvidDBField subclass handle
   * binding/unbinding during rollback, since all objects which could conceivably 
   * be involved in a link will also have their own states rolled back.</P>
   *
   * <P>Called by {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}'s
   * {@link arlut.csd.ganymede.server.DBEditObject#rollback(java.util.Hashtable) rollback()}
   * method.</P>
   */

  public synchronized void rollback(Object oldval)
  {
    if (!(owner instanceof DBEditObject))
      {
	throw new RuntimeException("Invalid rollback on field " + 
				   getName() + ", not in an editable context");
      }

    if (isVector())
      {
	if (!(oldval instanceof Vector))
	  {
	    throw new RuntimeException("Invalid vector rollback on field " + 
				       getName());
	  }
	else
	  {
	    // in theory we perhaps should iterate through the oldval
	    // Vector to make sure that each element is of the right
	    // type.. in practice, that would be a lot of overhead to
	    // guard against something that should never happen,
	    // anyway.
	    //
	    // i'm just saying this to cover my ass in case it does,
	    // so i'll know that i was deliberately rather than
	    // accidentally stupid.

	    this.value = oldval;
	  }
      }
    else
      {
	if (!verifyTypeMatch(oldval))
	  {
	    throw new RuntimeException("Invalid scalar rollback on field " + 
				       getName());
	  }
	else
	  {
	    this.value = oldval;
	  }
      }
  }

  /**
   * <P>This method takes the result of an operation on this field
   * and wraps it with a {@link arlut.csd.ganymede.common.ReturnVal ReturnVal}
   * that encodes an instruction to the client to rescan
   * this field.  This isn't normally necessary for most client
   * operations, but it is necessary for the case in which wizards
   * call DBField.setValue() on behalf of the client, because in those
   * cases, the client otherwise won't know that the wizard modified
   * the field.</P>
   *
   * <P>This makes for a significant bit of overhead on client calls
   * to the field modifier methods, but this is avoided if code 
   * on the server uses setValueLocal(), setElementLocal(), addElementLocal(),
   * or deleteElementLocal() to make changes to a field.</P>
   *
   * <P>If you are ever in a situation where you want to use the local
   * variants of the modifier methods (to avoid permissions checking
   * overhead), but you <b>do</b> want to have the field's rescan
   * information returned, you can do something like:</P>
   *
   * <pre>
   *
   * return field.rescanThisField(field.setValueLocal(null));
   *
   * </pre> 
   */

  public final ReturnVal rescanThisField(ReturnVal original)
  {
    if (original != null && !original.didSucceed())
      {
        return original;
      }

    if (original == null)
      {
	original = new ReturnVal(true);
      }

    original.addRescanField(getOwner().getInvid(), getID());

    return original;
  }

  /**
   *
   * For debugging
   *
   */

  public String toString()
  {
    return "[" + owner.toString() + ":" + getName() + "]";
  }

  /**
   * <p>Handy utility method for reporting namespace conflict.  This
   * method will work to identify the object and field which is in conflict,
   * and will return an appropriate {@link arlut.csd.ganymede.common.ReturnVal ReturnVal}
   * with an appropriate error dialog.</p>
   */

  public ReturnVal getConflictDialog(String methodName, Object conflictValue)
  {
    DBNameSpace ns = getNameSpace();

    try
      {
	DBField conflictField = ns.lookupPersistent(conflictValue);

	if (conflictField != null)
	  {
	    DBObject conflictObject = conflictField.getOwner();
	    String conflictLabel = conflictObject.getLabel();
	    String conflictClassName = conflictObject.getTypeName();

	    return Ganymede.createErrorDialog("Server: Error in " + methodName,
					      "This action could not be completed" +
					      " because \"" + conflictValue + "\" is already being used.\n\n" +
					      conflictClassName + " \"" + conflictLabel + 
					      "\" contains this value in its " +
					      conflictField.getName() + " field.\n\n" +
					      "You can choose a different value here, or you can try to " +
					      "edit or delete the \"" + conflictLabel + "\" object to remove " +
					      "the conflict.");
	  }
	else
	  {
	    conflictField = ns.lookupShadow(conflictValue);

	    DBObject conflictObject = conflictField.getOwner();
	    String conflictLabel = conflictObject.getLabel();
	    String conflictClassName = conflictObject.getTypeName();

	    return Ganymede.createErrorDialog("Server: Error in " + methodName,
					      "This action could not be completed" +
					      " because \"" + conflictValue + "\" is already being used in a transaction.\n\n" +
					      conflictClassName + " \"" + conflictLabel + 
					      "\" contains this value in its " +
					      conflictField.getName() + " field.\n\n" +
					      "You can choose a different value here, or you can try to " +
					      "edit or delete the \"" + conflictLabel + "\" object to remove " +
					      "the conflict.");
	  }
      }
    catch (NullPointerException ex)
      {
	ex.printStackTrace();

	return Ganymede.createErrorDialog("Server: Error in " + methodName,
					  "value " + conflictValue +
					  " already taken in namespace");
      }
  }

  /**
   * <p>Handy utility method for reporting an attempted duplicate
   * submission to a vector field.</p>
   */

  public ReturnVal getDuplicateValueDialog(String methodName, Object conflictValue)
  {
    return Ganymede.createErrorDialog("Server: Error in " + methodName,
				      "This action could not be duplicated because \"" + String.valueOf(conflictValue) + "\" is already contained in field " + getName());
  }

  /**
   * <p>Handy utility method for reporting an attempted duplicate
   * submission to a vector field.</p>
   */

  public ReturnVal getDuplicateValuesDialog(String methodName, String conflictValues)
  {
    return Ganymede.createErrorDialog("Server: Error in " + methodName,
				      "This action could not be duplicated because \"" + conflictValues + "\" are already contained in field " + getName());
  }

  /**
   * <p>
   * This method is for use primarily within a Jython context and accessed by
   * calling ".val" on a {@link arlut.csd.ganymede.server.DBField DBField} object,
   * but it can theoretically be used in Java code in lieu of calling
   * {@link arlut.csd.ganymede.server.DBField.getValue getValue} or
   * {@link arlut.csd.ganymede.server.DBField.getValues getValues} (but <b>there
   * are some subtle differences </b>!).
   * </p>
   * <p>
   * This method will return this field's value, be it vector or scalar.
   * However, when it encounters an {@link arlut.csd.ganymede.common.Invid Invid}
   * object (either as the value proper or as a member of this fields value
   * vector), it will instead return the
   * {@link arlut.csd.ganymede.server.DBObject DBObject} that the Invid points to.
   * </p>
   * 
   * @return This field's value. This can take the form of scalar types,
   *         {@link arlut.csd.ganymede.server.DBObject DBObjects}, or a
   *         {@link java.util.Vector Vector}containing either.
   */
  public Object getVal()
  {
    if (isVector())
      {
        Vector values = getValuesLocal();
        
        /* Dereference each Invid object in the values vector */
        List returnList = new ArrayList(values.size());
        for (Iterator iter = values.iterator(); iter.hasNext();)
          {
            returnList.add(dereferenceObject(iter.next()));
          }
          
        return returnList;
      }
    else
      {
        /* Return the field value, and dereference it if it is an Invid */
        return dereferenceObject(getValueLocal());
      }
  }
  
  /**
   * If the argument is an Invid, this method will return a reference to the
   * actual DBObject the Invid points to. Otherwise, it returns the same object
   * that was passed in.
   *
   * @param obj 
   * @return a DBObject if <b>obj</b> is an Invid, otherwise return <b>obj</b>
   */
  private Object dereferenceObject(Object obj)
  {
    if (obj instanceof Invid)
      {
        return Ganymede.db.getObject((Invid) obj);
      }
    else
      {
        return obj;
      }
  }
}
