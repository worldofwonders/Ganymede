/*
   GASH 2

   DateDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.5 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     DateDBField

------------------------------------------------------------------------------*/

public class DateDBField extends DBField implements date_field {

  /**
   *
   * Receive constructor.  Used to create a BooleanDBField from a DBStore/DBJournal
   * DataInput stream.
   *
   */

  DateDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    value = values = null;
    this.owner = owner;
    this.definition = definition;
    receive(in);
  }

  /**
   *
   * No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the DBObjectBase
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the DBStore.
   *
   * Used to provide the client a template for 'creating' this
   * field if so desired.
   *
   */

  DateDBField(DBObject owner, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.definition = definition;
    
    defined = false;
    value = null;
    values = null;
  }

  /**
   *
   * Copy constructor.
   *
   */

  public DateDBField(DBObject owner, DateDBField field)
  {
    this.owner = owner;
    definition = field.definition;
    
    if (isVector())
      {
	values = (Vector) field.values.clone();
	value = null;
      }
    else
      {
	value = field.value;
	values = null;
      }

    defined = true;
  }

  /**
   *
   * Scalar value constructor.
   *
   */

  public DateDBField(DBObject owner, Date value, DBObjectBaseField definition)
  {
    if (definition.isArray())
      {
	throw new IllegalArgumentException("scalar constructor called on vector field");
      }

    this.owner = owner;
    this.definition = definition;
    this.value = value;

    if (value != null)
      {
	defined = true;
      }
    else
      {
	defined = false;
      }

    values = null;
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public DateDBField(DBObject owner, Vector values, DBObjectBaseField definition)
  {
    if (!definition.isArray())
      {
	throw new IllegalArgumentException("vector constructor called on scalar field");
      }

    this.owner = owner;
    this.definition = definition;

    if (values == null)
      {
	this.values = new Vector();
	defined = false;
      }
    else
      {
	this.values = (Vector) value.clone();
	defined = true;
      }

    defined = true;
    value = null;
  }

  protected Object clone()
  {
    return new DateDBField(owner, this);
  }

  void emit(DataOutput out) throws IOException
  {
    if (isVector())
      {
	out.writeShort(values.size());
	for (int i = 0; i < values.size(); i++)
	  {
	    out.writeLong(((Date) values.elementAt(i)).getTime());
	  }
      }
    else
      {
	out.writeLong(((Date) value).getTime());
      }
  }

  void receive(DataInput in) throws IOException
  {
    int count;

    /* -- */

    if (isVector())
      {
	count = in.readShort();
	values = new Vector(count);
	for (int i = 0; i < count; i++)
	  {
	    values.addElement(new Date(in.readLong()));
	  }
      }
    else
      {
	value = new Date(in.readLong());
      }

    defined = true;
  }

  // ****
  //
  // type-specific accessor methods
  //
  // ****

  public Date value()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector");
      }

    return (Date) value;
  }

  public Date value(int index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar");
      }

    return (Date) values.elementAt(index);
  }

  // ****
  //
  // date_field methods
  //
  // ****

  /**
   *
   * Returns true if this date field has a minimum and/or maximum date
   * set.
   *
   * We are currently assuming that time limited fields will need to
   * have their limits dynamically calculated, so such fields will
   * need to override this method to provide limit information.
   *
   * @see arlut.csd.ganymede.date_field
   *
   */

  public boolean limited()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) owner;

    return eObj.isDateLimited(this);
  }

  /**
   *
   * Returns the earliest date acceptable for this field
   *
   * We are currently assuming that time limited fields will need to
   * have their limits dynamically calculated, so such fields will
   * need to override this method to provide limit information.
   *
   * @see arlut.csd.ganymede.date_field
   *
   */

  public Date minDate()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) owner;

    return eObj.minDate(this);
  }

  /**
   *
   * Returns the latest date acceptable for this field
   *
   * We are currently assuming that time limited fields will need to
   * have their limits dynamically calculated, so such fields will
   * need to override this method to provide limit information.
   *
   * @see arlut.csd.ganymede.date_field
   *
   */

  public Date maxDate()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) owner;

    return eObj.maxDate(this);
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return (o instanceof Date);
  }

  public boolean verifyNewValue(Object o)
  {
    DBEditObject eObj;
    Date d, d2;
    Vector v;
    boolean ok = true;

    /* -- */

    if (!isEditable())
      {
	return false;
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	setLastError("type mismatch");
	return false;
      }

    d = (Date) o;

    if (limited())
      {
	d2 = minDate();
	if (d2 != null)
	  {
	    if (d.before(d2))
	      {
		setLastError("Date is out of range (under)");
		return false;
	      }
	  }

	d2 = maxDate();
	if (d2 != null)
	  {
	    if (d.after(d2))
	      {
		setLastError("Date is out of range (over)");
		return false;
	      }
	  }
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }
}
