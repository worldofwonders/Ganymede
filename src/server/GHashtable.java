/*

   GHashtable.java

   A subclass of Hashtable that supports case-insensitive
   hashing/retrieval.
   
   Created: 10 April 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      GHashtable

------------------------------------------------------------------------------*/

/**
 *
 * GHashtable is a Hashtable subclass that can map uppercase/lowercase keys
 * of the same string to identity.  It does this by basically mapping all
 * strings to the lowercase version internally.  The case sensitivity of
 * the hashtable is specified at hash creation time, and may not change
 * thereafter.
 *
 */

public class GHashtable extends Hashtable {

  private boolean caseInsensitive; // we don't allow this to change after creation

  /* -- */

  /**
   *
   * Fully specified constructor.
   *
   * @param initialCapacity as for Hashtable
   * @param loadFactor as for Hashtable
   * @param caseInsensitive if true, lowercase and uppercase string keys will be mapped together
   *
   * @see java.util.Hashtable
   *
   */

  public GHashtable(int initialCapacity, float loadFactor, boolean caseInsensitive)
  {
    super(initialCapacity, loadFactor);
    this.caseInsensitive = caseInsensitive;
  }

  /**
   *
   * Medium specified constructor.
   *
   * @param initialCapacity as for Hashtable
   * @param caseInsensitive if true, lowercase and uppercase string keys will be mapped together
   *
   * @see java.util.Hashtable
   *
   */

  public GHashtable(int initialCapacity, boolean caseInsensitive)
  {
    super(initialCapacity);
    this.caseInsensitive = caseInsensitive;
  }

  /**
   *
   * Least specified constructor.
   *
   * @param caseInsensitive if true, lowercase and uppercase string keys will be mapped together
   *
   * @see java.util.Hashtable
   *
   */

  public GHashtable(boolean caseInsensitive)
  {
    super();
    this.caseInsensitive = caseInsensitive;
  }

  public synchronized Enumeration keys()
  {
    if (caseInsensitive)
      {
	return new GEnum(super.keys());
      }
    else
      {
	return super.keys();
      }
  }

  public synchronized boolean containsKey(Object key)
  {
    if (caseInsensitive)
      {
	return super.containsKey(new GKey(key));
      }
    else
      {
	return super.containsKey(key);
      }
  }

  public synchronized Object get(Object key)
  {
    if (caseInsensitive)
      {
	return super.get(new GKey(key));
      }
    else
      {
	return super.get(key);
      }
  }

  public synchronized Object put(Object key, Object value)
  {
    Object result;

    /* -- */

    if (caseInsensitive)
      {
	result = super.put(new GKey(key), value);
      }
    else
      {
	result = super.put(key, value);
      }

    return result;
  }

  public synchronized Object remove(Object key)
  {
    if (caseInsensitive)
      {
	return super.remove(new GKey(key));
      }
    else
      {
	return super.remove(key);
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                         GHandle

This class provides a mapping to allow keys of differing capitalization to be
treated as identical in a hashtable, while allowing the capitalization-preserved
key value to be retrieved on demand, in support of the Hashtable.keys() method.

------------------------------------------------------------------------------*/

class GKey {

  Object
    key, 
    orig;

  /* -- */

  GKey(Object key)
  {
    if (key instanceof String)
      {
	orig = key;
	this.key = ((String) key).toLowerCase();
      }
    else
      {
	this.key = orig = key;
      }
  }

  public int hashCode()
  {
    return key.hashCode();
  }

  public boolean equals(Object obj)
  {
    if (obj instanceof GKey)
      {
	return key.equals(((GKey) obj).key);
      }
    else
      {
	return key.equals(obj);
      }
  }

  public Object origValue()
  {
    return orig;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                           GEnum

This class is in support of the Hashtable keys() method, to provide an 
enumeration which will 'unwrap' GKey objects to provide access to the original
key submitted to the GHashtable, with capitalization preserved.

------------------------------------------------------------------------------*/

class GEnum implements Enumeration {

  Enumeration source;
  Object t;

  /* -- */

  GEnum(Enumeration enum)
  {
    source = enum;
  }

  public boolean hasMoreElements()
  {
    return source.hasMoreElements();
  }

  public Object nextElement()
  {
    t = source.nextElement();
    
    if (t instanceof GKey)
      {
	return ((GKey) t).origValue();
      }
    else
      {
	return t;
      }
  }
}
