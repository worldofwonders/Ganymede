/*

   PasswordAgingTask.java

   This task is run periodically to check all user accounts in Ganymede
   for their password expiration time.
   
   Created: 14 June 2001
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
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

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Vector;

import arlut.csd.ddroid.common.Invid;
import arlut.csd.ddroid.common.NotLoggedInException;
import arlut.csd.ddroid.common.Query;
import arlut.csd.ddroid.common.QueryDataNode;
import arlut.csd.ddroid.common.Result;
import arlut.csd.ddroid.common.ReturnVal;
import arlut.csd.ddroid.common.SchemaConstants;
import arlut.csd.ddroid.server.DBObject;
import arlut.csd.ddroid.server.Ganymede;
import arlut.csd.ddroid.server.GanymedeServer;
import arlut.csd.ddroid.server.GanymedeSession;

/*------------------------------------------------------------------------------
                                                                           class
                                                               PasswordAgingTask

------------------------------------------------------------------------------*/

/**
 *
 * This task is run periodically to check all user accounts in Ganymede
 * for their password expiration time.
 *
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public class PasswordAgingTask implements Runnable {

  static final boolean debug = true;

  /* -- */

  GanymedeSession mySession = null;
  Thread currentThread = null;

  /**
   *
   * Just Do It (tm)
   *
   * @see java.lang.Runnable
   *
   */

  public void run()
  {
    boolean transactionOpen = false;

    /* -- */

    currentThread = java.lang.Thread.currentThread();

    Ganymede.debug("Password Aging Task: Starting");

    String error = GanymedeServer.checkEnabled();
	
    if (error != null)
      {
	Ganymede.debug("Deferring password aging task - semaphore disabled: " + error);
	return;
      }

    try
      {
	try
	  {
	    mySession = new GanymedeSession("PasswordAgingTask");
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("PasswordAging Task: Couldn't establish session");
	    return;
	  }

	// we don't want no wizards

	mySession.enableWizards(false);

	// and we don't want forced required fields oversight..  this
	// can leave us with some invalid objects, but we can do a
	// query to scan for them, and if someone edits the objects
	// later, they'll be requested to fix the problem.

	mySession.enableOversight(false);
	
	ReturnVal retVal = mySession.openTransaction("password aging task");

	if (retVal != null && !retVal.didSucceed())
	  {
	    Ganymede.debug("PasswordAging Task: Couldn't open transaction");
	    return;
	  }

	transactionOpen = true;
	
	// do the stuff

	handlePasswords();

	retVal = mySession.commitTransaction();

	if (retVal != null && !retVal.didSucceed())
	  {
	    // if doNormalProcessing is true, the
	    // transaction was not cleared, but was
	    // left open for a re-try.  Abort it.
	    
	    if (retVal.doNormalProcessing)
	      {
		Ganymede.debug("PasswordAging Task: couldn't fully commit, trying to abort.");
		
		mySession.abortTransaction();
	      }

	    Ganymede.debug("PasswordAging Task: Couldn't successfully commit transaction");
	  }
	else
	  {
	    Ganymede.debug("PasswordAging Task: Transaction committed");
	  }

	transactionOpen = false;
      }
    catch (InterruptedException ex)
      {
      }
    catch (NotLoggedInException ex)
      {
      }
    finally
      {
	if (transactionOpen)
	  {
	    Ganymede.debug("PasswordAging Task: Forced to terminate early, aborting transaction");
	  }

	mySession.logout();
      }
  }

  private void handlePasswords() throws InterruptedException, NotLoggedInException
  {
    Query q;
    Vector results;
    Result result;
    Invid invid;
    DBObject object;
    Date currentTime = new Date();
    Calendar lowerBound = new GregorianCalendar();
    Calendar upperBound = new GregorianCalendar();
    Enumeration en;

    QueryDataNode matchNode = new QueryDataNode(userSchema.PASSWORDCHANGETIME,
						QueryDataNode.DEFINED,
						null);
    /* -- */
    
    q = new Query(SchemaConstants.UserBase, matchNode, false);
    
    results = mySession.internalQuery(q);
    
    en = results.elements();
    
    while (en.hasMoreElements())
      {
	if (currentThread.isInterrupted())
	  {
	    throw new InterruptedException("scheduler ordering shutdown");
	  }
	    
	result = (Result) en.nextElement();

	invid = result.getInvid();

	object = mySession.getSession().viewDBObject(invid);

	if (object.isInactivated())
	  {
	    continue;
	  }

	// get the password expiration threshold for this user, if any

	Date passwordTime = (Date) object.getFieldValueLocal(userSchema.PASSWORDCHANGETIME);

	if (passwordTime == null)
	  {
	    continue;
	  }

	// first, check for one month into the future

	// note: we used to try and do month add, but we can't do this
	// cleanly if the next month has fewer days than this month
	// does, i.e., if someone's password expires on February 28th
	// and it is January 29th, Java considers January 29th + 1
	// month to be February 28th.  Ditto January 30th + 1 month,
	// ditto January 31st + 1 month.
	//
	// so, we just go for four weeks of notice

	lowerBound.setTime(currentTime);
	lowerBound.add(Calendar.DATE, 27);

	upperBound.setTime(currentTime);
	upperBound.add(Calendar.DATE, 28);

	if (passwordTime.after(lowerBound.getTime()) && passwordTime.before(upperBound.getTime()))
	  {
	    warnUpcomingPasswordExpire(object);
	    continue;
	  }

	// then two weeks
	//
	// likewise, Java's week calculations for WEEK_OF_MONTH, etc.,
	// are very squirrely, so we'll just stick to simple date
	// incrementing.

	lowerBound.setTime(currentTime);
	lowerBound.add(Calendar.DATE, 13);

	upperBound.setTime(currentTime);
	upperBound.add(Calendar.DATE, 14);

	if (passwordTime.after(lowerBound.getTime()) && passwordTime.before(upperBound.getTime()))
	  {
	    warnUpcomingPasswordExpire(object);
	    continue;
	  }

	// then one week

	lowerBound.setTime(currentTime);
	lowerBound.add(Calendar.DATE, 6);

	upperBound.setTime(currentTime);
	upperBound.add(Calendar.DATE, 7);

	if (passwordTime.after(lowerBound.getTime()) && passwordTime.before(upperBound.getTime()))
	  {
	    warnUpcomingPasswordExpire(object);
	    continue;
	  }

	// then one day before

	lowerBound.setTime(currentTime);

	upperBound.setTime(currentTime);
	upperBound.add(Calendar.DATE, 1);

	if (passwordTime.after(lowerBound.getTime()) && passwordTime.before(upperBound.getTime()))
	  {
	    warnRealSoonNowPasswordExpire(object);
	    continue;
	  }

	// then day one of the grace period

	lowerBound.setTime(currentTime);
	lowerBound.add(Calendar.DATE, -1);

	upperBound.setTime(currentTime);

	if (passwordTime.after(lowerBound.getTime()) && passwordTime.before(upperBound.getTime()))
	  {
	    warnOvertimePassword(object);
	    continue;
	  }

	// then day two of the grace period

	lowerBound.setTime(currentTime);
	lowerBound.add(Calendar.DATE, -2);

	upperBound.setTime(currentTime);
	upperBound.add(Calendar.DATE, -1);

	if (passwordTime.after(lowerBound.getTime()) && passwordTime.before(upperBound.getTime()))
	  {
	    warnOvertimePassword(object);
	    continue;
	  }

	// then day three of the grace period

	lowerBound.setTime(currentTime);
	lowerBound.add(Calendar.DATE, -3);

	upperBound.setTime(currentTime);
	upperBound.add(Calendar.DATE, -2);

	if (passwordTime.after(lowerBound.getTime()) && passwordTime.before(upperBound.getTime()))
	  {
	    warnOvertimePassword(object);
	    continue;
	  }

	// then we blow it away

	upperBound.setTime(currentTime);
	upperBound.add(Calendar.DATE, -3);

	if (passwordTime.before(upperBound.getTime()))
	  {
	    ReturnVal retVal = mySession.inactivate_db_object(invid);

	    if (retVal != null && !retVal.didSucceed())
	      {
		Ganymede.debug("PasswordAging task was not able to inactivate user " + 
			       result.toString());
	      }
	    else
	      {
		Ganymede.debug("PasswordAging task inactivated user " + 
			       result.toString());
	      }
	  }
      }
  }

  /**
   * <p>Send out a warning that the password is going to expire, to the user's
   * email addresses only.</p>
   */

  private void warnUpcomingPasswordExpire(DBObject userObject)
  {
    Date passwordChangeTime = (Date) userObject.getFieldValueLocal(userSchema.PASSWORDCHANGETIME);

    Vector objVect = new Vector();

    objVect.addElement(userObject.getInvid());

    String titleString = "Password Expiring Soon For User " + userObject.toString() + ", at " + passwordChangeTime;

    String messageString = "The password for user account " + userObject.toString() + 
      " will expire soon.  You will need to change your password before " + passwordChangeTime +
      " or else your user account will be inactivated.\n\n" +
      "You can change your password online by visiting http://www.arlut.utexas.edu/password/\n\n" +
      "If you need assistance with this matter, please contact one of your lab unit's Directory Droid administrators.";

    Ganymede.log.sendMail(null, titleString, messageString, true, false, objVect);
  }

  /**
   * <p>Send out a warning that the password is going to expire soon, to the user's
   * and admin groups' email addresses.</p>
   */

  private void warnRealSoonNowPasswordExpire(DBObject userObject)
  {
    Date passwordChangeTime = (Date) userObject.getFieldValueLocal(userSchema.PASSWORDCHANGETIME);

    Vector objVect = new Vector();

    objVect.addElement(userObject.getInvid());

    String titleString = "Password Expiring Very Soon For User " + userObject.toString() + ", at " + passwordChangeTime;

    String messageString = "The password for user account " + userObject.toString() + 
      " will expire very soon.  The password for this user account will need to be changed before " + passwordChangeTime +
      " or else the account will be inactivated.\n\n" +
      "You can change your password online by visiting http://www.arlut.utexas.edu/password/\n\n" +
      "If you need assistance with this matter, please contact one of your lab unit's Directory Droid administrators, " +
      "or CSD.";

    Ganymede.log.sendMail(null, titleString, messageString, true, true, objVect);
  }

  /**
   * <p>Send out a very urgent warning that the password is past its
   * expiration date, to the user's and admin groups' email
   * addresses.</p>
   */

  private void warnOvertimePassword(DBObject userObject)
  {
    Date passwordChangeTime = (Date) userObject.getFieldValueLocal(userSchema.PASSWORDCHANGETIME);

    Vector objVect = new Vector();

    objVect.addElement(userObject.getInvid());

    String titleString = "Password Has Expired For User " + userObject.toString() + "!!!";

    String messageString = "The password for user account " + userObject.toString() + 
      " expired at " + passwordChangeTime + ".  The password for this user account *must* be changed immediately, or else" +
      " the account will be inactivated.  If this account is inactivated, extension of the password" +
      " expiration deadline will be impossible, and a new password will need to be chosen to re-enable" +
      " this account.\n\n" +
      "You can change your password online by visiting http://www.arlut.utexas.edu/password/\n\n" +
      "If you need assistance with this matter, please contact one of your lab unit's Directory Droid administrators, " +
      "or CSD.";

    Ganymede.log.sendMail(null, titleString, messageString, true, true, objVect);
  }
}
