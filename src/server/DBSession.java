/*
   GASH 2

   DBSession.java

   The GANYMEDE object storage system.

   Created: 26 August 1996
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       DBSession

------------------------------------------------------------------------------*/

/**
 * 
 * <p>DBSession is the DBStore session class.  All normal database
 * interactions are performed through a DBSession object.  The
 * DBSession object provides a handle for monitoring the operations on
 * the database.. who holds what lock, what actions are performed
 * during a lock / transaction / session.</p>
 * 
 * */

public class DBSession {

  static boolean debug = true;

  public static void setDebug(boolean val)
  {
    debug = val;
  }
  
  DBStore store;
  DBLock lock;
  DBEditSet editSet;
  String lastError;
  Object key;

  /* -- */

  /**   
   * <p>Constructor for DBSession.</p>
   * 
   * <p>The key passed to the DBSession constructor is intended to be used
   * to allow code to save an identifier in the DBSession.. this might be
   * a thread object or a higher level session object or whatever.  Eventually
   * I expect I'll replace this generic key with some sort of reporting 
   * Interface object.</p>
   *
   * <p>This constructor is intended to be called by the DBStore login() method.</p>
   *
   * @param store The DBStore database this session belongs to.
   * @param key An identifying key with meaning to whatever code is using arlut.csd.ganymede
   *
   */
   
  DBSession(DBStore store, Object key)
  {
    this.store = store;
    this.key = key;

    editSet = null;
    lastError = null;
  }

  public synchronized void logout()
  {
    if (editSet != null)
      {
	abortTransaction();
      }

    this.store = null;
  }

  /**
   * Create a new object in the database.<br><br>
   *
   * This method creates a slot in the object base of the
   * proper object type.  The created object is associated
   * with the current transaction.  When the transaction 
   * is committed, the created object will inserted into
   * the database, and will become visible to other
   * sessions.<br><br>
   *
   * The created object will be given an object id.
   * The DBEditObject can be queried to determine its
   * invid.
   *
   * This method will return null if the object could
   * not be constructed and initialized for some reason.
   *
   * @param object_type Type of the object to be created
   *
   * @see arlut.csd.ganymede.DBStore
   *
   */

  public synchronized DBEditObject createDBObject(short object_type)
  {
    DBObjectBase base;
    DBEditObject e_object;

    /* -- */

    if (editSet == null)
      {
	throw new RuntimeException("createDBObject called outside of a transaction");
      }

    base = (DBObjectBase) store.objectBases.get(new Short(object_type));

    e_object = base.createNewObject(editSet);

    return e_object;
  }

  /**
   *
   * Pull an object out of the database for editing.<br><br>
   *
   * This method is used to check an object out of the database for editing.
   * Only one session can have a particular object checked out for editing at
   * a time.<br><br>
   *
   * The session has to have a readlock established and a transaction 
   * opened before it can pull an object out for editing.
   *
   * @param invid The invariant id of the object to be modified.  The objectBase
   *              referenced in the invid must be locked in lock.
   *
   * @see arlut.csd.ganymede.DBObjectBase
   */

  public synchronized DBEditObject editDBObject(Invid invid)
  {
    return editDBObject(invid.getType(), invid.getNum());
  }

  /**
   *
   * Pull an object out of the database for editing.<br><br>
   *
   * This method is used to check an object out of the database for editing.
   * Only one session can have a particular object checked out for editing at
   * a time.<br><br>
   *
   * The session has to have a readlock established and a transaction 
   * opened before it can pull a new object out for editing.  If the object
   * specified by <baseID, objectID> is part of the current transaction,
   * the transactional copy will be returned, and no readLock is strictly
   * necessary in that case.
   *
   * @param baseID The short id number of the DBObjectBase containing the object to
   *               be edited.
   *
   * @param objectID The int id number of the object to be edited within the specified
   *                 object base.
   *
   * @see arlut.csd.ganymede.DBObjectBase
   *
   */

  public synchronized DBEditObject editDBObject(short baseID, int objectID)
  {
    DBObject obj;

    /* -- */

    if (editSet == null)
      {
	throw new RuntimeException("editDBObject called outside of a transaction");
      }

    obj = viewDBObject(baseID, objectID);

    return obj.createShadow(editSet);
  }

  /**
   *
   * Get a reference to a read-only copy of an object in the DBStore.<br><br>
   *
   * The session has to have a read lock established before calling
   * viewDBObject.  Otherwise a RuntimeException will be thrown.<br><br>
   *
   * If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.
   *
   * @param invid The invariant id of the object to be viewed.  The objectBase
   *              referenced in the invid must be locked in lock.
   *
   * @see arlut.csd.ganymede.DBObjectBase
   * @see arlut.csd.ganymede.DBLock
   *
   */

  public synchronized DBObject viewDBObject(Invid invid)
  {
    return viewDBObject(invid.getType(), invid.getNum());
  }

  /**
   *
   * Get a reference to a read-only copy of an object in the DBStore.<br><br>
   *
   * The session has to have a read lock established before calling
   * viewDBObject.  Otherwise a RuntimeException will be thrown.<br><br>
   *
   * If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.
   *
   * @param baseID The short id number of the DBObjectBase containing the object to
   *               be viewed.
   *
   * @param objectID The int id number of the object to be viewed within the specified
   *                 object base.
   *
   * @see arlut.csd.ganymede.DBObjectBase
   *
   */

  public synchronized DBObject viewDBObject(short baseID, int objectID)
  {
    DBObjectBase base;
    DBObject     obj;
    Short      baseKey;
    Integer      objKey;

    /* -- */

    if (isTransactionOpen())
      {
	obj = editSet.findObject(new Invid(baseID, objectID));
	if (obj != null)
	  {
	    return obj;
	  }
      }

    baseKey = new Short(baseID);
    objKey = new Integer(objectID);

    base = (DBObjectBase) store.objectBases.get(baseKey);

    if (base == null)
      {
	return null;
      }

    // we don't check to see if the lock is a
    // DBReadLock.. viewDBOBject is acceptably atomic relative to any
    // possible write activity that might be occurring if we are
    // trying to read on a DBWriteLock, we won't worry about the
    // DBWriteLock possessor changing things on us.

    // we couldn't be so trusting on an enum establish request, though.

    if ((lock == null) || lock.isLocked(base))
      {
	throw new RuntimeException("viewDBObject: viewDBObject called without lock");
      }

    obj = (DBObject) base.objectHash.get(objKey);

    // do we want to do any kind of logging here?

    return obj;
  }

  /**
   *
   * Remove an object from the database<br><br>
   *
   * This method method can only be called in the context of an open
   * transaction and an open lock on the object's base.  This method
   * will mark an object for deletion.  When the transaction is
   * committed, the object is removed from the database.  If the
   * transaction is aborted, the object remains in the database
   * unchanged.
   *
   * @param invid Invid of the object to be deleted
   * */

  public synchronized void deleteDBObject(Invid invid)
  {
    deleteDBObject(invid.getType(), invid.getNum());
  }

  /**
   *
   * Remove an object from the database<br><br>
   *
   * This method method can only be called in the context of an open
   * transaction and an open lock on the object's base.  This method
   * will check an object out of the DBStore and add it to the
   * editset's deletion list.  When the transaction is committed, the
   * object has its remove() method called to do cleanup, and the
   * editSet nulls the object's slot in the DBStore.  If the
   * transaction is aborted, the object remains in the database
   * unchanged.
   *
   * @param baseID id of the object base containing the object to be deleted
   * @param objectID id of the object to be deleted
   * 
   */

  public synchronized void deleteDBObject(short baseID, int objectID)
  {
    DBObject obj;
    DBEditObject eObj;

    /* -- */

    if (editSet == null)
      {
	throw new RuntimeException("deleteDBObject called outside of a transaction");
      }

    obj = viewDBObject(baseID, objectID);

    if (obj instanceof DBEditObject)
      {
	eObj = (DBEditObject) obj;
      }
    else
      {
	eObj = obj.createShadow(editSet);
      }

    deleteDBObject(eObj);
  }

  /**
   *
   * Remove an object from the database<br><br>
   *
   * This method method can only be called in the context of an open
   * transaction and an open lock on the object's base.  This method
   * will take an object out of the DBStore and add mark it as
   * deleted in the transaction.  When the transaction is committed, the
   * object will have its remove() method called to do cleanup, and the
   * editSet nulls the object's slot in the DBStore.  If the
   * transaction is aborted, the object remains in the database
   * unchanged.
   *
   * @param eObj An object checked out in the current transaction to be deleted
   * 
   */

  public synchronized void deleteDBObject(DBEditObject eObj)
  {
    switch (eObj.getStatus())
      {
      case DBEditObject.CREATING:
	eObj.setStatus(DBEditObject.DROPPING);
	break;

      case DBEditObject.EDITING:
	eObj.setStatus(DBEditObject.DELETING);
	break;

      case DBEditObject.DELETING:
	return;
	break;

      case DBEditObject.DROPPING:
	return;
	break;
      }
  }

  /**
   *
   * openReadLock establishes a read lock for the DBObjectBase's in bases.<br><br>
   *
   * The thread calling this method will block until the read lock 
   * can be established.  If any of the DBObjectBases in bases have transactions
   * currently committing, the establishment of the read lock will be suspended
   * until all such transactions are committed.<br><br>
   *
   * All viewDBObject calls done within the context of an open read lock
   * will be transaction consistent.  Other sessions may pull objects out for
   * editing during the course of the session's read lock, but no visible changes
   * will be made to those ObjectBases until the read lock is released.
   */

  public synchronized void openReadLock(Vector bases)
  {
    lock = new DBReadLock(store, bases);
    lock.establish(this);
  }

  /**
   *
   * openReadLock establishes a read lock for the entire DBStore.<br><br>
   *
   * The thread calling this method will block until the read lock 
   * can be established.  If transactions on the database are
   * currently committing, the establishment of the read lock will be suspended
   * until all such transactions are committed.<br><br>
   *
   * All viewDBObject calls done within the context of an open read lock
   * will be transaction consistent.  Other sessions may pull objects out for
   * editing during the course of the session's read lock, but no visible changes
   * will be made to those ObjectBases until the read lock is released.
   */

  public synchronized void openReadLock()
  {
    lock = new DBReadLock(store);
    lock.establish(this);
  }

  /**
   * releaseReadLock releases the read lock held by this session.  If this
   * session does not hold a read lock, this method is a no-op.
   */

  public synchronized void releaseReadLock()
  {
    if (lock != null && lock instanceof DBReadLock)
      {
	lock.release();
	lock = null;
      }
  }

  /**
   *
   * openWriteLock establishes a write lock for the DBObjectBase's in bases... this
   * is intended to be used only by sessions that have already established an
   * open transaction.<br><br>
   *
   * The thread calling this method will block until the write lock 
   * can be established. 
   *
   * Once a write lock is established, the owner of this DBSession can
   * perform consistency checks in the bases secure in the knowledge 
   * that no other thread can change the database until the current transaction
   * is committed or released.
   *
   * The DBWriteLock will typically be released by commit or abort.
   *
   * ALL THIS NEEDS TO BE WORKED ON MORE ***** JON *****
   */

  public synchronized void openWriteLock(Vector bases)
  {
    if (lock != null && lock instanceof DBWriteLock )
      {
	// already got a writelock.. throw exception

	throw new RuntimeException("already got writelock");
      }
    releaseReadLock();
    lock = new DBWriteLock(store,bases);
    lock.establish(this);    
  }

  /**
   *
   * openReadLock establishes a read lock for the entire DBStore.<br><br>
   *
   * The thread calling this method will block until the read lock 
   * can be established.  If transactions on the database are
   * currently committing, the establishment of the read lock will be suspended
   * until all such transactions are committed.<br><br>
   *
   * All viewDBObject calls done within the context of an open read lock
   * will be transaction consistent.  Other sessions may pull objects out for
   * editing during the course of the session's read lock, but no visible changes
   * will be made to those ObjectBases until the read lock is released.
   */

  public synchronized void openWriteLock()
  {
    if (lock != null && lock instanceof DBWriteLock )
      {
	// already got a writelock.. throw exception

	throw new RuntimeException("already got writelock");
      }
    releaseReadLock();
    lock = new DBWriteLock(store);
    lock.establish(this);
  }

  /**
   * openTransaction establishes a transaction context for this session.
   * When this method returns, the session can call editDBObject and 
   * createDBObject to obtain DBEditObjects.  Methods can then be called
   * on the DBEditObjects to make changes to the database.  These changes
   * are actually performed when and if commitTransaction is called.
   *
   * @see arlut.csd.ganymede.DBEditObject
   *
   */ 

  public synchronized void openTransaction()
  {
    editSet = new DBEditSet(store, this);
  }

  /**
   * commitTransaction causes any changes made during the context of
   * a current transaction to be performed.  Appropriate portions of the
   * database are locked, changes are made to the in-memory image of
   * the database, and a description of the changes is placed in the
   * DBStore journal file.  Event logging / mail notification may
   * take place.<br><br>
   *
   * Note that if the session holds a readlock when commitTransaction
   * is called, the read lock will be released so that a write lock can be
   * acquired.  There is no guarantee that changes won't happen in the
   * database between the relinquishing of the read lock
   * and the acquisition of the write lock, but any objects that were
   * pulled for editing by this transaction may not be touched by
   * any other concurrent sessions.
   *
   * @see arlut.csd.ganymede.DBEditObject
   */

  public synchronized void commitTransaction()
  {
    // we need to release our readlock, if we have one,
    // so that the commit can establish a writelock..

    // should i make it so that a writelock can be established
    // if the possessor of a readlock doesn't give it up? 

    if (debug)
      {
	System.err.println(key + ": entering commitTransaction");
      }

    if (editSet == null)
      {
	throw new RuntimeException(key + ": commitTransaction called outside of a transaction");
      }

    if (lock != null)
      {
	if (debug)
	  {
	    System.err.println(key + ": commitTransaction(): holding a lock");
	  }

	if (lock instanceof DBReadLock)
	  {
	    if (debug)
	      {
		System.err.println(key + ": commitTransaction(): holding a read lock");
	      }
	    lock.release();

	    if (debug)
	      {
		System.err.println(key + ": commitTransaction(): released read lock");
	      }

	    lock = null;

// 	    if (debug)
// 	      {
// 		System.err.println("commitTransaction(): acquiring write lock");
// 	      }

// 	    lock = new DBWriteLock(store);

// 	    if (debug)
// 	      {
// 		System.err.println("commitTransaction(): got write lock");
// 	      }
	  }
      }

    if (debug)
      {
	System.err.println(key + ": commitTransaction(): committing editSet");
      }

    editSet.commit();

    if (debug)
      {
	System.err.println(key + ": commitTransaction(): editset committed");
      }

    editSet = null;

    //    lock.release();
    //
    //    if (debug)
    //      {
    //	System.err.println("commitTransaction(): writeLock released");
    //      }
  }

  /**
   * abortTransaction causes all DBEditObjects that were pulled during
   * the course of the session's transaction to be released without affecting
   * the state of the database.  Any changes made to DBObjects pulled for editing
   * by this session during this transaction are abandoned.  Any objects created
   * or destroyed by this session during this transaction are abandoned / unaffected
   * by the actions during the transaction.<br><br>
   *
   * Calling abortTransaction has no affect on any locks held by this session.
   *
   * @see arlut.csd.ganymede.DBEditObject
   */

  public synchronized void abortTransaction()
  {
    if (editSet == null)
      {
	throw new RuntimeException("abortTransaction called outside of a transaction");
      }

    editSet.release();
    editSet = null;
  }

  /**
   *
   * Returns true if this session has an transaction open
   *
   */

  public boolean isTransactionOpen()
  {
    return (editSet != null);
  }

  /**
   *
   * internal method for setting error messages resulting from session activities.
   *
   * this method may eventually be hooked up to a more general logging
   * mechanism.
   *
   */

  void setLastError(String error)
  {
    this.lastError = error;

    if (debug)
      {
	System.err.println(key + ": DBSession.setLastError(): " + error);
      }
  }
}
