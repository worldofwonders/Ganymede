/*

   QueryDataNode.java

   Created: 10 July 1997
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   QueryDataNode

------------------------------------------------------------------------------*/

public class QueryDataNode extends QueryNode {

  static public final byte FIRST = 1;

  static public final byte EQUALS = 1;
  static public final byte LESS = 2;
  static public final byte LESSEQ = 3;
  static public final byte GREAT = 4;
  static public final byte GREATEQ = 5;
  static public final byte NOCASEEQ = 6; // case insensitive string equals
  static public final byte STARTSWITH = 7;
  static public final byte ENDSWITH = 8;
  static public final byte UNDEFINED = 9;
  static public final byte LAST = 9;

  /* - */

  String fieldname;
  short fieldId;
  byte comparator;
  Object value;

  /* -- */

  /**
   * Field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against a field in an object.
   *
   */

  public QueryDataNode(String fieldname, byte comparator, Object value)
  {
    this.fieldname = fieldname;
    this.fieldId = -1;

    if (comparator < FIRST || comparator > LAST)
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;
  }

  /**
   * Field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against a field in an object.
   *
   * If fieldID == -1, the labels of objects in the database will be
   * taken as the field for comparison's sake.
   *
   * If fieldID == -2, the Invid of objects in the database will be
   * taken as the field for comparison's sake.
   * 
   */

  public QueryDataNode(short fieldId, byte comparator, Object value)
  {
    this.fieldname = null;
    this.fieldId = fieldId;

    if (comparator < FIRST || comparator > LAST)
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;
  }

  /**
   * Default field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against an object's primary label field.
   *
   */

  public QueryDataNode(byte comparator, Object value)
  {
    this.fieldname = null;
    this.fieldId = -1;

    if (comparator < FIRST || comparator > LAST)
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;
  }


  /** 
   * This method dumps the value of the data node to a string in the form of:
   *
   * quote(data value)
   *
   */
  
  public String dumpToString ()
    {

      String operator, returnVal;

      // The following is designed to make the query string
      // more readable by using the operator instad of its
      // mapped numeric value

      if (this.comparator == 1)
	{
	  operator = "=";
	}
      else if (this.comparator == 2)
	{
	  operator = "<"; 
	} 
      else if (this.comparator == 3)
	  {
	    operator = "<=";
	  }
      else if (this.comparator == 4)
	{
	  operator = ">";
	}
      else if (this.comparator == 5)
	{
	  operator = ">=";
	}
      else
	{
	  operator = "Error: Operator undefined";
	}


      returnVal = "(" + operator;

      // add fieldname
      
      if (fieldname != null)
	{
	  returnVal = returnVal + "(fieldname " + fieldname + ")"; 
	}
      else if (fieldId != -1)
	{
	  returnVal = returnVal + "(fieldname #" + fieldId +")"; 
	}
      else
	{
	  returnVal = returnVal + "(fieldname null)";
	}
      
      // add value
      
      if (this.value == null)
	{
	  System.err.println("Error: Value undefined in QueryDataNode");
	  returnVal = returnVal + "(value undefined)";
	}
      else
	{
      returnVal = returnVal + "(value " + this.value + ")";
	}
      
      // close parenthesis

      returnVal = returnVal + ")";
      
      // all done

      return returnVal;
      
    }
}
