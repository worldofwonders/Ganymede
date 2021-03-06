/*
   GASH 2

   DBReadLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2014
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/*------------------------------------------------------------------------------
                                                                           class
                                                                      DBReadLock

------------------------------------------------------------------------------*/

/**
 * <p>DBReadLock is a class used in the Ganymede server to represent a
 * read lock on one or more {@link
 * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects.  A
 * DBReadLock is used in the {@link
 * arlut.csd.ganymede.server.GanymedeSession GanymedeSession} class to
 * guarantee that all query operations go from start to finish without
 * any changes being made along the way.</p>
 *
 * <p>While a DBReadLock is established on a DBObjectBase, no changes
 * may be made to that base.  The {@link
 * arlut.csd.ganymede.server.DBWriteLock DBWriteLock}'s {@link
 * arlut.csd.ganymede.server.DBWriteLock#establish(java.lang.Object)
 * establish()} method will suspend until all read locks on a base are
 * cleared.  As soon as a thread attempts to establish a DBWriteLock
 * on a base, no more DBReadLocks will be established on that base
 * until the DBWriteLock is cleared, but any DBReadLocks already
 * established will persist until released, whereupon the DBWriteLock
 * will establish.</p>
 *
 * <p>{@link arlut.csd.ganymede.server.DBDumpLock DBDumpLocks} can be
 * established while a DBReadLocks is active and vice-versa,
 * though.</p>
 *
 * <p>See {@link arlut.csd.ganymede.server.DBLock DBLock}, {@link
 * arlut.csd.ganymede.server.DBWriteLock DBWriteLock}, and {@link
 * arlut.csd.ganymede.server.DBDumpLock DBDumpLock} for details.</p>
 */

public final class DBReadLock extends DBLock {

  static final boolean debug = false;

  /* -- */

  /**
   * All DBLock's have an identifier key, which is used to identify
   * the lock in the {@link arlut.csd.ganymede.server.DBStore
   * DBStore}'s {@link arlut.csd.ganymede.server.DBLockSync
   * DBLockSync} object.  The establish() methods in the DBLock
   * subclasses consult the DBStore.lockSync to make sure that no
   * {@link arlut.csd.ganymede.server.DBSession DBSession} ever
   * possesses more than one write lock, to prevent deadlocks from
   * occuring in the server.
   */

  private Object key;

  private boolean locked = false;
  private boolean inEstablish = false;
  private boolean abort = false;

  /**
   * In order to prevent deadlocks, each individual lock must be
   * established on all applicable {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBases} at the time
   * the lock is initially established.  baseSet is the List of
   * DBObjectBases that this DBLock is/will be locked on.
   */

  private final List<DBObjectBase> baseSet;

  /**
   * Constructor to get a shared read lock on all of the server's
   * object bases
   */

  public DBReadLock(DBStore store)
  {
    super(store.lockSync);

    this.baseSet = Collections.unmodifiableList(new ArrayList(store.getBases()));
  }

  /**
   * Constructor to get a shared read lock on a subset of the object
   * bases.
   */

  public DBReadLock(DBStore store, List<DBObjectBase> baseSet)
  {
    super(store.lockSync);

    this.baseSet = Collections.unmodifiableList(new ArrayList(baseSet));
  }

  /**
   * Returns true if this lock is locked.
   */

  @Override public final boolean isLocked()
  {
    return this.locked;
  }

  /**
   * Returns true if this lock is waiting in establish()
   */

  @Override public final boolean isEstablishing()
  {
    return this.inEstablish;
  }

  /**
   * Returns true if this lock is aborting
   */

  @Override public final boolean isAborting()
  {
    return this.abort;
  }

  /**
   * Returns immutable list of DBObjectBases that this lock is meant
   * to cover.
   */

  @Override final List<DBObjectBase> getBases()
  {
    return this.baseSet;
  }

  @Override final Object getKey()
  {
    if (isLocked())
      {
        return key;
      }
    else
      {
        return null;
      }
  }

  /**
   * <p>A thread that calls establish() will be suspended (waiting on
   * the server's {@link arlut.csd.ganymede.server.DBLockSync
   * DBLockSync} until all DBObjectBases listed in this DBReadLock's
   * constructor are available to be locked.  At that point, the
   * thread blocking on establish() will wake up possessing a shared
   * read lock on the requested DBObjectBases.</p>
   *
   * <p>It is possible for the establish() to fail completely.. the
   * admin console may reject a client whose thread is blocking on
   * establish(), for instance, or the server may be shut down.  In
   * those cases, another thread may call the DBReadLock's {@link
   * arlut.csd.ganymede.server.DBLock#abort() abort()} method, in
   * which case establish() will throw an InterruptedException, and
   * the lock will not be established.</p>
   *
   * <p>The possessors of DBLocks are identified by a key Object that
   * is provided on the call to {@link
   * arlut.csd.ganymede.server.DBLock#establish(java.lang.Object)}.  A
   * given key may only have one DBWriteLock established at a time,
   * but it may have multiple concurrent DBDumpLocks and DBReadLocks
   * established if there are no DBWriteLocks held by that key or
   * locked with DBObjectBases that overlap this lock request.</p>
   *
   * @param key An object used in the server to uniquely identify the
   * entity internal to Ganymede that is attempting to obtain the
   * lock, typically a unique String.
   */

  @Override public final void establish(Object key) throws InterruptedException
  {
    boolean okay = false;

    /* -- */

    if (debug)
      {
        debug(key, "establish() enter");

        Ganymede.printCallStack();
      }

    synchronized (lockSync)
      {
        if (!lockSync.claimLockKey(key, this))
          {
            throw new RuntimeException("Error: read lock sought by owner of existing write or dump lockset for key: " + key);
          }

        try
          {
            lockSync.incLocksWaitingCount();

            this.inEstablish = true;
            this.key = key;

            while (!okay)
              {
                if (debug) debug("establish() looping to get establish permission for " + getBaseNames(baseSet));

                if (this.abort)
                  {
                    throw new InterruptedException("DBReadLock (" + key + "):  establish aborting before permission granted");
                  }

                okay = true;

                for (DBObjectBase base: baseSet)
                  {
                    // check for writers.  we don't care about
                    // dumpers, since we can read without problems
                    // while a dump lock is held

                    if (base.hasWriter())
                      {
                        if (debug) debug("establish() base " + base.getName() + " has writers queued/locked");

                        okay = false;
                        break;
                      }
                  }

                if (!okay)
                  {
                    if (debug) debug("establish() waiting on lockSync");

                    lockSync.wait(2500);

                    if (debug) debug("establish() done waiting on lockSync");

                    continue;
                  }

                // nothing can stop us now

                for (DBObjectBase base: baseSet)
                  {
                    base.addReader(this);
                  }

                this.locked = true;
                lockSync.incLockCount();

                if (debug) debug("establish() read lock established");
              }
          }
        finally
          {
            lockSync.decLocksWaitingCount();
            this.inEstablish = false;

            if (!this.locked)
              {
                lockSync.unclaimLockKey(key, this);
              }

            lockSync.notifyAll();
          }
      }
  }

  /**
   * <p>Relinquish the lock on bases held by this lock object.</p>
   *
   * <p>Should be called by {@link arlut.csd.ganymede.server.DBSession
   * DBSession}'s {@link
   * arlut.csd.ganymede.server.DBSession#releaseLock(arlut.csd.ganymede.server.DBLock)
   * releaseLock()} method.</p>
   *
   * <p>Note that this method is designed to be able to be called from
   * one thread while another is trying to use and/or establish the
   * lock.  If this.abort is not set to true before calling release(),
   * release() will block until the establish is granted.  That's why
   * abort() sets this.abort to true before calling release().</p>
   *
   * <p>The point of release() is to clear out this lock's connections
   * to the locked object bases and to allow DBLock establish()
   * methods in other threads to proceed.</p>
   */

  @Override public final void release()
  {
    if (debug)
      {
        debug("release() attempting release");
        Ganymede.printCallStack();
      }

    synchronized (lockSync)
      {
        // if this lock is being established in another thread, we
        // need to wait until that thread exits its establish section.
        // if we haven't set abort to true, this won't happen until it
        // gets the lock established, or is interrupted

        while (this.inEstablish)
          {
            if (debug) debug("release() looping waiting on inEstablish");

            try
              {
                lockSync.wait(2500);
              }
            catch (InterruptedException ex)
              {
              }
          }

        if (!locked)
          {
            if (debug) debug("release() not locked, returning");

            return;
          }

        for (DBObjectBase base: baseSet)
          {
            base.removeReader(this);
          }

        locked = false;
        lockSync.unclaimLockKey(key, this);

        if (debug) debug("release() released");

        this.key = null;             // for gc

        lockSync.decLockCount();
        lockSync.notifyAll();
      }
  }

  /**
   * <p>Withdraw this lock.  This method can be called by a thread to
   * interrupt a lock establish that is blocked waiting to get access
   * to the appropriate set of {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects.  If
   * this method is called while another thread is waiting in
   * establish(), establish() will throw an InterruptedException.</p>
   *
   * <p>Once abort() is processed, this lock may never be established.
   * Any subsequent calls to establish() will always throw
   * InterruptedException.</p>
   *
   * <p>Note that calling abort() on a lock that has already
   * established in another thread will remove the lock, but a thread
   * that is using the lock to iterate over a list will explicitly
   * need to check to see if its lock was pulled.</p>
   */

  @Override public final void abort()
  {
    synchronized (lockSync)
      {
        if (debug) debug("abort() aborting");
        this.abort = true;
        release();              // blocks until freed
      }
  }

  private void debug(Object key, String message)
  {
    System.err.println("DBReadLock(" + key + "): " + message);
  }

  private void debug(String message)
  {
    System.err.println("DBReadLock(" + this.key + "): " + message);
  }
}
