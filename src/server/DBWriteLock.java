/*
   GASH 2

   DBWriteLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.7 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     DBWriteLock

------------------------------------------------------------------------------*/

/**
 *
 * <p>A DBWriteLock is established on one or more DBObjectBases to prevent any
 * other threads from reading or writing to the database.  When a DBWriteLock
 * is established on a DBObjectBase, the establishing thread suspends until
 * all readers currently working in the specified DBObjectBases complete.  The
 * write lock is then established, and the thread possessing the DBWriteLock
 * is free to replace objects in the DBStore with modified copies.</p>
 *
 * <p>DBWriteLocks are typically created and managed by the code in the editSet
 * class.  It is very important that any thread that obtains a DBWriteLock be
 * scrupulous about releasing the lock in a timely fashion once the
 * appropriate changes are made in the database. </p>
 *
 * @see arlut.csd.ganymede.DBEditSet
 * @see arlut.csd.ganymede.DBObjectBase
 *
 */

public class DBWriteLock extends DBLock {

  static final boolean debug = true;

  private Object key;
  private DBStore lockManager;
  private Vector baseSet;
  private boolean 
    locked = false,
    abort = false,
    inEstablish = false;

  /* -- */

  /**
   *
   * constructor to get a write lock on all the object bases
   *
   */

  public DBWriteLock(DBStore lockManager)
  {
    Enumeration enum;
    DBObjectBase base;

    /* -- */

    this.key = null;
    this.lockManager = lockManager;
    baseSet = new Vector();

    synchronized (lockManager)
      {
	enum = lockManager.objectBases.elements();
	
	while (enum.hasMoreElements())
	  {
	    base = (DBObjectBase) enum.nextElement();
	    baseSet.addElement(base);
	  }
      }
  }

  /**
   *
   * constructor to get a write lock on a subset of the
   * object bases.
   *
   */

  public DBWriteLock(DBStore lockManager, Vector baseSet)
  {
    this.key = null;
    this.lockManager = lockManager;
    this.baseSet = baseSet;
  }

  /**
   *
   * Establish a dump lock on bases specified in this DBDumpLock's
   * constructor.  Can throw InterruptedException if another thread
   * orders us to abort() while we're waiting for permission to
   * proceed with reads on the specified baseset.
   *
   */

  public void establish(Object key) throws InterruptedException
  {
    boolean done, okay;
    DBObjectBase base;

    /* -- */

    if (debug)
      {
	System.err.println(key + ": DBWriteLock.establish(): enter");
	System.err.println(key + ": DBWriteLock.establish(): baseSet vector size " + baseSet.size());
      }

    synchronized (lockManager)
      {
	if (lockManager.lockHash.containsKey(key))
	  {
	    throw new RuntimeException("Error: lock sought by owner of existing lockset.");
	  }

	lockManager.lockHash.put(key, this);

	if (debug)
	  {
	    System.err.println(key + ": DBWriteLock.establish(): added myself to the DBStore lockHash.");
	  }

	this.key = key;
	inEstablish = true;

	done = false;

	// wait until there are no dumpers 

	do
	  {
	    if (abort)
	      {
		lockManager.lockHash.remove(key);
		key = null;
		inEstablish = false;
		lockManager.notifyAll();
		throw new InterruptedException();
	      }

	    okay = true;

	    if (lockManager.schemaEditInProgress)
	      {
		okay = false;
	      }
	    else
	      {
		for (int i = 0; okay && (i < baseSet.size()); i++)
		  {
		    base = (DBObjectBase) baseSet.elementAt(i);
		    if (!base.isDumperEmpty())
		      {
			if (debug)
			  {
			    System.err.println(key + ": DBWriteLock.establish(): waiting for dumpers on base " + 
					       base.object_name);
			    System.err.println(key + ": DBWriteLock.establish(): dumperList size: " + 
					       base.getDumperSize());
			  }
			okay = false;
		      }
		  }
	      }

	    if (!okay)
	      {
		try
		  {
		    lockManager.wait();
		  }
		catch (InterruptedException ex)
		  {
		    lockManager.lockHash.remove(key);
		    inEstablish = false;
		    lockManager.notifyAll();
		    throw ex;
		  }
	      }

	  } while (!okay);	// waiting for dumpers / schema editing to clear out

	if (debug)
	  {
	    System.err.println(key + ": DBWriteLock.establish(): no dumpers queued.");
	  }

	// add our selves to the ObjectBase write queues

	for (int i = 0; i < baseSet.size(); i++)
	  {
	    base = (DBObjectBase) baseSet.elementAt(i);
 	    base.addWriter(this);
	  }

	if (debug)
	  {
	    System.err.println(key + ": DBWriteLock.establish(): added ourself to the writerList.");
	  }

	// spinwait until we can get into all of the ObjectBases
	// note that since we added ourselves to the writer
	// queues, we know the dumpers are waiting until we
	// finish. 

	while (!done)
	  {
	    if (debug)
	      {
		System.err.println(key + ": DBWriteLock.establish(): spinning.");
	      }

	    if (abort)
	      {
		for (int i = 0; i < baseSet.size(); i++)
		  {
		    base = (DBObjectBase) baseSet.elementAt(i);
		    base.removeWriter(this);
		  }

		lockManager.lockHash.remove(key);
		key = null;
		inEstablish = false;
		lockManager.notifyAll();
		throw new InterruptedException();
	      }

	    okay = true;

	    for (int i = 0; okay && (i < baseSet.size()); i++)
	      {
		base = (DBObjectBase) baseSet.elementAt(i);
		if (base.writeInProgress || !base.isReaderEmpty())
		  {
		    if (debug)
		      {
			if (!base.isReaderEmpty())
			  {
			    System.err.println(key +
					       ": DBWriteLock.establish(): " +
					       "waiting for readers to release.");
			  }
			else if (base.writeInProgress)
			  {
			    System.err.println(key +
					       ": DBWriteLock.establish(): " + 
					       "waiting for writer to release.");
			  }
		      }
		    okay = false;
		  }
	      }

	    // at this point, okay == true only if we were able to
	    // verify that no bases have writeInProgress to be true.
	    // Note that we don't try to insure that writers write in
	    // the order they were put into the writerList, since this
	    // may vary from base to base

	    if (okay)
	      {
		for (int i = 0; i < baseSet.size(); i++)
		  {
		    base = (DBObjectBase) baseSet.elementAt(i);
		    base.writeInProgress = true;
		    base.currentLock = this;
		  }
		
		done = true;
	      }
	    else
	      {
		try
		  {
		    lockManager.wait();
		  }
		catch (InterruptedException ex)
		  {
		    for (int i = 0; i < baseSet.size(); i++)
		      {
			base = (DBObjectBase) baseSet.elementAt(i);
			base.removeWriter(this);
		      }
	
		    lockManager.lockHash.remove(key);
		    key = null;
		    inEstablish = false;
		    lockManager.notifyAll();
		    throw ex;
		  }
	      }
	  } // while (!done)

	locked = true;
	inEstablish = false;
	lockManager.addLock();	// notify consoles
	lockManager.notifyAll();

      } // synchronized(lockManager)

    if (debug)
      {
	System.err.println(key + ": DBWriteLock.establish(): got the lock.");
      }
  }

  /**
   *
   * Release this lock on all bases locked
   *
   */

  public void release()
  {
    DBObjectBase base;

    /* -- */

    synchronized (lockManager)
      {
	while (inEstablish)
	  {
	    try
	      {
		lockManager.wait();
	      } 
	    catch (InterruptedException ex)
	      {
	      }
	  }

	// note that we have to check locked here or else we might accidentally
	// release somebody else's lock below

	if (!locked)
	  {
	    return;
	  }

	for (int i = 0; i < baseSet.size(); i++)
	  {
	    base = (DBObjectBase) baseSet.elementAt(i);
	    base.removeWriter(this);
	    base.writeInProgress = false;
	    base.currentLock = null;
	  }

	locked = false;
	lockManager.lockHash.remove(key);
	key = null;
	lockManager.removeLock();	// notify consoles
	lockManager.notifyAll();	// many readers may want in
      }
  }

  /**
   *
   * Withdraw this lock.  This method can be called by a thread to
   * interrupt a lock establish that is blocked waiting to get
   * access to the appropriate set of DBObjectBase objects.  If
   * this method is called while another thread is blocked in
   * establish(), establish() will throw an InterruptedException.
   *
   * Once abort() is processed, this lock may never be established.
   * Any subsequent calls to estabish() will always throw
   * InterruptedException.
   *
   */
  
  public void abort()
  {
    synchronized (lockManager)
      {
	abort = true;
	lockManager.notifyAll();
	release();
      }
  }

  /**
   *
   * Returns true if <base> is locked by this lock.
   *
   */

  boolean isLocked(DBObjectBase base)
  {
    if (!locked)
      {
	return false;
      }

    for (int i=0; i < baseSet.size(); i++)
      {
	if (baseSet.elementAt(i) == base)
	  {
	    return true;
	  }
      }

    return false;
  }

  /**
   *
   * Returns the key that this lock is established with,
   * or null if the lock has not been established.
   *
   */

  Object getKey()
  {
    if (locked)
      {
	return key;
      }
    else
      {
	return null;
      }
  }
  
}
