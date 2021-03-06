/*

   DBPermissionManager.java

   Contains the permissions management logic for the Ganymede Server.

   Created: 18 April 2012

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT

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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.BaseListTransport;
import arlut.csd.ganymede.common.CategoryTransport;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectHandle;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.PermMatrix;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;

/*------------------------------------------------------------------------------
                                                                           class
                                                             DBPermissionManager

------------------------------------------------------------------------------*/

/**
 * <p>Permissions manager for the Ganymede Server.</p>
 *
 * <p>Each GanymedeSession logged into the Ganymede Server will have
 * its own DBPermissionManager attached, which does permission
 * management for it.</p>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public final class DBPermissionManager {

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts =
    TranslationService.getTranslationService("arlut.csd.ganymede.server.DBPermissionManager");

  /**
   * Invid for the supergash Owner Group Object
   */

  static final Invid SUPERGASH_GROUP_INVID =
    Invid.createInvid(SchemaConstants.OwnerBase,
                      SchemaConstants.OwnerSupergash);

  /**
   * Invid for the supergash Persona Object
   */

  static final Invid SUPERGASH_PERSONA_INVID =
    Invid.createInvid(SchemaConstants.PersonaBase,
                      SchemaConstants.PersonaSupergashObj);

  /**
   * Invid for the default Role Object
   */

  static final Invid DEFAULT_ROLE_INVID =
    Invid.createInvid(SchemaConstants.RoleBase,
                      SchemaConstants.RoleDefaultObj);

  // ---

  /**
   * The GanymedeSession that this DBPermissionManager is connected to.
   */

  final private GanymedeSession gSession;

  /**
   * The DBSession that lays under gSession.
   */

  final private DBSession dbSession;

  /**
   * GanymedeSessions created for internal operations always operate
   * with supergash privileges.  We'll set this flag to true to avoid
   * having to do persona membership checks on initial set-up.
   */

  final private boolean beforeversupergash; // Be Forever Yamamoto

  /**
   * The name that the session is given.  Must be non-null and unique
   * among logged in sessions on the server.
   */

  final private String sessionName;

  /**
   * The object reference identifier for the logged in user, if
   * any. If the client logged in directly to a non user-linked
   * persona account (e.g., supergash, monitor), this will be null.
   * See personaInvid in that case.
   */

  final private Invid userInvid;

  /**
   * <p>The name of the user logged in.</p>
   *
   * <p>May be null if the containing GanymedeSession is created by an
   * internal Ganymede task or process.</p>
   */

  final private String username;

  // --

  /**
   * <p>True if the gSession currently has supergash privileges.</p>
   *
   * <p>May change if {@link
   * arlut.csd.ganymede.server.DBPermissionManager#selectPersona(String,
   * String} is called.</p>
   */

  private boolean supergashMode = false;

  /**
   * False if we've detected that the underlying user or admin persona
   * for this DBPermissionManager has been deleted out from under us.
   */

  private boolean valid = true;

  /**
   * <p>The name of the current persona, of the form
   * '&lt;username&gt;:&lt;description&gt;', for example,
   * 'broccol:GASH Admin'.  If the user is logged in with just
   * end-user privileges, personaName will be null.</p>
   *
   * <p>May change if {@link
   * arlut.csd.ganymede.server.DBPermissionManager#selectPersona(String,
   * String} is called.</p>
   */

  private String personaName;

  /**
   * <p>The object reference identifier for the current persona, if
   * any.</p>
   *
   * <p>May change if {@link
   * arlut.csd.ganymede.server.DBPermissionManager#selectPersona(String,
   * String} is called.</p>
   */

  private Invid personaInvid;

  /**
   * <p>A reference to our current persona object.  We save this so we
   * can look up owner groups and what not more quickly.  An end-user
   * logged in without any extra privileges will have a null
   * personaObj value.</p>
   *
   * <p>May change if {@link
   * arlut.csd.ganymede.server.DBPermissionManager#selectPersona(String,
   * String} is called.</p>
   */

  private DBObject personaObj;

  /**
   * When did we last check our persona permissions?
   */

  private Date personaTimeStamp;

  /**
   * <p>This variable stores the permission bits that are applicable to
   * objects that the current persona has ownership privilege over.
   * This matrix is always a permissive superset of {@link
   * arlut.csd.ganymede.server.DBPermissionManager#unownedObjectPerms
   * unownedObjectPerms}.</p>
   *
   * <p>May change if {@link
   * arlut.csd.ganymede.server.DBPermissionManager#selectPersona(String,
   * String} is called or if the relevant Role Objects are changed in
   * the database.</p>
   *
   * <p>If this DBPermissionManager has supergash privileges, this
   * PermMatrix will be null.</p>
   */

  private PermMatrix ownedObjectPerms;

  /**
   * <p>This variable stores the permission bits that are applicable
   * to generic objects not specifically owned by this persona.</p>
   *
   * <p>May change if {@link
   * arlut.csd.ganymede.server.DBPermissionManager#selectPersona(String,
   * String} is called or if the relevant Role Objects are changed in
   * the database.</p>
   *
   * <p>If this DBPermissionManager has supergash privileges, this
   * PermMatrix will be null.</p>
   */

  private PermMatrix unownedObjectPerms;

  /**
   * <p>This variable stores the permission bits that are applicable
   * to objects that the current persona has ownership privilege over
   * and which the current admin has permission to delegate to
   * subordinate roles.  This matrix is always a permissive superset
   * of {@link
   * arlut.csd.ganymede.server.DBPermissionManager#delegatableUnownedObjectPerms
   * delegatableUnownedObjectPerms}.</p>
   *
   * <p>May change if {@link
   * arlut.csd.ganymede.server.DBPermissionManager#selectPersona(String,
   * String} is called or if the relevant Role Objects are changed in
   * the database.</p>
   *
   * <p>Used by code in {@link
   * arlut.csd.ganymede.server.PermissionMatrixDBField} to control
   * what privileges personae are able to grant to new personae.</p>
   *
   * <p>If this DBPermissionManager has supergash privileges, this
   * PermMatrix will be null.</p>
   */

  private PermMatrix delegatableOwnedObjectPerms;

  /**
   * <p>This variable stores the permission bits that are applicable to
   * generic objects not specifically owned by this persona and which
   * the current admin has permission to delegate to subordinate
   * roles.</p>
   *
   * <p>May change if {@link
   * arlut.csd.ganymede.server.DBPermissionManager#selectPersona(String,
   * String} is called or if the relevant Role Objects are changed in
   * the database.</p>
   *
   * <p>Used by code in {@link
   * arlut.csd.ganymede.server.PermissionMatrixDBField} to control
   * what privileges personae are able to grant to new personae.</p>
   *
   * <p>If this DBPermissionManager has supergash privileges, this
   * PermMatrix will be null.</p>
   */

  private PermMatrix delegatableUnownedObjectPerms;

  /**
   * <p>A reference to the checked-in Ganymede {@link
   * arlut.csd.ganymede.server.DBObject DBObject} storing our default
   * permissions, or the permissions that applies when we are not in
   * supergash mode and we do not have any ownership over the object
   * in question.</p>
   *
   * <p>May change if the relevant Role Object is changed in the
   * database.</p>
   */

  private DBObject defaultRoleObj;

  /**
   * When did we last notice a change in any Role Objects?
   */

  private Date rolesLastCheckedTimeStamp;

  /**
   * When did we last check our User Object?
   */

  private Date userTimeStamp;

  /**
   * <p>This variable is a non-modifiable List of object references
   * ({@link arlut.csd.ganymede.common.Invid Invids}) to the owner
   * groups that the client has requested newly created objects be
   * placed in.  While this List is not-null, any new objects created
   * will be owned by the list of ownergroups held here.</p>
   *
   * <p>May change if {@link
   * arlut.csd.ganymede.server.DBPermissionManager#setDefaultOwner(java.util.List)}
   * is called.</p>
   */

  private List<Invid> newObjectOwnerInvids;

  /**
   * <p>This variable is an unmodifiable List of object references
   * ({@link arlut.csd.ganymede.common.Invid Invids}) to the owner
   * groups that the client has requested the listing of objects be
   * restricted to.  That is, the client has requested that the
   * results of Queries and Dumps only include those objects owned by
   * owner groups in this list.  This feature is used primarily for
   * when a client is logged in with supergash privileges, but the
   * user wants to restrict the visibility of objects for
   * convenience.</p>
   *
   * <p>May change if {@link
   * arlut.csd.ganymede.server.DBPermissionManager#filterQueries(java.util.List)}
   * is called.</p>
   */

  private List<Invid> visibilityFilterInvids;

  /* -- */

  /**
   * Constructor for a privileged internal session
   *
   * @param gSession The GanymedeSession that we are managing
   * permissions for.
   *
   * @param sessionName The name of this session, used for identifying
   * the task or server component that is using our GanymedeSession to
   * perform work in the server.  Must be unique among logged-in
   * sessions on the server and may not be null.
   */

  public DBPermissionManager(GanymedeSession gSession, String sessionName)
  {
    if (gSession == null)
      {
        throw new IllegalArgumentException("gSession must be non-null");
      }

    if (sessionName == null)
      {
        throw new IllegalArgumentException("sessionName may not be null");
      }

    this.gSession = gSession;
    this.dbSession = gSession.getDBSession();
    this.sessionName = sessionName;

    this.beforeversupergash = true;
    this.supergashMode = true;

    this.username = null;
    this.userInvid = null;
    this.personaInvid = null;
    this.personaName = null;
  }

  /**
   * Constructor for a logged-in user
   *
   * @param gSession The GanymedeSession that we are managing
   * permissions for.
   *
   * @param userObject A DBObject describing the user logged in, or
   * null if the user is logging in with a non-user-linked persona
   * object (supergash, monitor, etc.)
   *
   * @param personaObject A DBObject describing the Admin Persona
   * logged in.  May be null if the user is logged in only with his
   * unprivileged end-user account.
   *
   * @param sessionName The name of this session, used for
   * identifying the task or server component that is using our
   * GanymedeSession to perform work in the server.  Must be unique
   * among logged-in sessions in the server and may not be null.
   */

  public DBPermissionManager(GanymedeSession gSession,
                             DBObject userObject,
                             DBObject personaObject,
                             String sessionName)
  {
    if (gSession == null)
      {
        throw new IllegalArgumentException("gSession must be non-null");
      }

    if (sessionName == null)
      {
        throw new IllegalArgumentException("sessionLabel may not be null");
      }

    if (userObject == null && personaObject == null)
      {
        throw new IllegalArgumentException("userObject or personaObject must be non-null");
      }

    this.gSession = gSession;
    this.dbSession = gSession.getDBSession();

    if (personaObject != null &&
        personaObject.getInvid().equals(SUPERGASH_PERSONA_INVID))
      {
        this.beforeversupergash = true;
        this.supergashMode = true;
      }
    else
      {
        this.beforeversupergash = false;
        this.supergashMode = false;
      }

    this.sessionName = sessionName;

    if (userObject != null)
      {
        this.userInvid = userObject.getInvid();
        this.username = userObject.getLabel();

        if (this.userInvid == null || this.username == null)
          {
            throw new NullPointerException();
          }

        if ("".equals(this.username.trim()))
          {
            throw new IllegalArgumentException("empty username");
          }
      }
    else
      {
        this.userInvid = null;
        this.username = personaObject.getLabel();
      }

    if (personaObject != null)
      {
        this.personaInvid = personaObject.getInvid();
        this.personaName = personaObject.getLabel();

        if (this.personaInvid == null || this.personaName == null)
          {
            throw new NullPointerException();
          }

        if ("".equals(this.personaName.trim()))
          {
            throw new IllegalArgumentException("empty personaName");
          }
      }
    else
      {
        this.personaInvid = null;
        this.personaName = null;
      }

    try
      {
        updatePerms();
      }
    catch (NotLoggedInException ex)
      {
        throw new IllegalStateException(ex);
      }
  }

  /**
   * Returns true if the session is operating with unrestricted 'root'
   * level privileges.
   */

  public synchronized boolean isSuperGash()
  {
    return this.supergashMode;
  }

  /**
   * Returns true if the session has any kind of privileges beyond the
   * default end-user privileges.
   */

  public synchronized boolean isPrivileged()
  {
    return personaInvid != null || isSuperGash();
  }

  /**
   * Returns true if the session is either an end-user user or an
   * end-user user using a persona.
   */

  public boolean isUserLinked()
  {
    return userInvid != null;
  }

  /**
   * Returns true if the session is operating solely with unprivileged
   * end-users privileges.
   */

  public boolean isEndUser()
  {
    return !isSuperGash() && userInvid != null && personaInvid == null;
  }

  /**
   * This method returns the name of the user that is logged into this
   * session, or null if this session was created for supergash,
   * monitor, or a Ganymede server task or other internal process.
   */

  public String getUserName()
  {
    return username;
  }

  /**
   * <p>Convenience method to get access to this session's user
   * invid.</p>
   *
   * <p>May be null if supergash, monitor, or a Ganymede server task
   * or internal process is running the session.</p>
   */

  public Invid getUserInvid()
  {
    return userInvid;
  }

  /**
   * <p>Convenience method to get access to this session's User
   * Object.</p>
   *
   * <p>May be null if supergash, monitor, or Ganymede server task or
   * internal process is running the session.</p>
   */

  synchronized DBObject getUser()
  {
    if (userInvid != null)
      {
        // using dbSession to skip perms checking

        DBObject userObject = dbSession.viewDBObject(userInvid);

        if (userObject == null)
          {
            return null;
          }

        return userObject.getOriginal();
      }

    return null;
  }

  /**
   * This method returns the name of the persona who is active.  May
   * be null or empty if we have an end-user who is logged in with no
   * elevated persona privileges.
   */

  public synchronized String getPersonaName()
  {
    return personaName;
  }

  /**
   * Convenience method to get access to this session's persona invid.
   */

  public synchronized Invid getPersonaInvid()
  {
    return personaInvid;
  }

  /**
   * This method gives access to the DBObject for the administrator's
   * persona record, if any.
   */

  synchronized DBObject getPersona()
  {
    return personaObj;
  }

  /**
   * <p>Returns the session name assigned to the GanymedeSession that
   * owns us.  Must be unique among all logged in sessions.</p>
   *
   * <p>getSessionName() will never return a null value.</p>
   */

  public String getSessionName()
  {
    return sessionName;
  }

  /**
   * This method returns the name of the user who is active, or the
   * name of the internal Ganymede task or process that is running the
   * session if no user is attached to this session.
   */

  public synchronized String getBaseIdentity()
  {
    if (username != null)
      {
        return username;
      }

    return sessionName;
  }

  /**
   * This method returns the name of the persona who is active, the
   * raw user name if no persona privileges have been assumed, or the
   * name of the internal Ganymede task or process that is running the
   * session if no user is attached to this session.
   */

  public synchronized String getIdentity()
  {
    if (personaName != null)
      {
        return personaName;
      }

    return getBaseIdentity();
  }

  /**
   * <p>This method returns the Invid of the user who logged in, or
   * the non-user-linked persona (supergash, monitor) if there was no
   * underlying user attached to the persona.</p>
   *
   * <p>May return null if this session is being run by a Ganymede
   * server task or internal process.</p>
   */

  public synchronized Invid getIdentityInvid()
  {
    if (userInvid != null)
      {
        return userInvid;
      }

    return personaInvid;
  }

  /**
   * <p>Returns an unmodifiable List of Invids containing user and
   * persona Invids for the GanymedeSession that this
   * DBPermissionManager is attached to.</p>
   *
   * <p>May return an empty List if this session is being run by a
   * Ganymede server task or internal process.</p>
   */

  public synchronized List<Invid> getIdentityInvids()
  {
    List<Invid> ids = new ArrayList<Invid>();

    if (userInvid != null)
      {
        ids.add(userInvid);
      }

    if (personaInvid != null)
      {
        ids.add(personaInvid);
      }

    return Collections.unmodifiableList(ids);
  }

  /**
   * Returns the email address that should be used in the 'From:'
   * field of mail sent by the GanymedeSession which owns this
   * DBPermissionManager.
   */

  public synchronized String getReturnAddress()
  {
    if (!isUserLinked())
      {
        return Ganymede.returnaddrProperty;
      }

    String mailsuffix = System.getProperty("ganymede.defaultmailsuffix");

    if (mailsuffix != null)
      {
        if (mailsuffix.contains("@"))
          {
            return username + mailsuffix;
          }
        else
          {
            return username + "@" + mailsuffix;
          }
      }

    return username;
  }

  /**
   * Returns the Invid of the admin persona (or user, if running with
   * unelevated privileges) who is responsible for actions taken by
   * the containing GanymedeSession.
   */

  public synchronized Invid getResponsibleInvid()
  {
    if (personaInvid != null)
      {
        return personaInvid;
      }

    return userInvid;
  }

  /**
   * <p>Returns a successful ReturnVal if the user / persona
   * credentials for this session are currently valid.</p>
   *
   * <p>If the user or persona connected to this session have been
   * deleted by another session, we'll return an error dialog
   * explaining that.</p>
   *
   * @return null if the session is valid, else a ReturnVal with an
   * error dialog encoded.
   */

  public synchronized ReturnVal isValidSession()
  {
    if (this.beforeversupergash)
      {
        return null;
      }

    if (isEndUser() && getUser() == null)
      {
        // "Session Invalidated"
        // "User object {0} deleted while logged in with session {1}"
        return Ganymede.createErrorDialog(gSession,
                                          ts.l("isValidSession.error"),
                                          ts.l("isValidSession.user_deleted",
                                               this.username,
                                               this.sessionName));
      }

    if (this.personaInvid != null &&
        dbSession.viewDBObject(this.personaInvid) == null)
      {
        // "Session Invalidated"
        // "Persona object {0} deleted while logged in with session {1}"
        return Ganymede.createErrorDialog(gSession,
                                          ts.l("isValidSession.error"),
                                          ts.l("isValidSession.persona_deleted",
                                               this.personaName,
                                               this.sessionName));
      }

    return null;
  }

  /**
   * Returns a serializable Vector of personae names available to the
   * user logged in, or null if we're logged in as a non user-linked
   * supergash persona.
   */

  public synchronized Vector<String> getAvailablePersonae()
  {
    DBObject u = getUser();

    if (u == null)
      {
        return null;
      }

    Vector<String> results = new Vector<String>();
    Vector personae = u.getFieldValuesLocal(SchemaConstants.UserAdminPersonae);

    for (Invid invid: (List<Invid>) personae)
      {
        try
          {
            results.add(dbSession.getCommittedObjectLabel(invid));
          }
        catch (NullPointerException ex)
          {
          }
      }

    results.add(u.getLabel()); // add their 'end-user' persona

    return results;
  }

  public synchronized PermMatrix getOwnedObjectPerms()
  {
    return ownedObjectPerms;
  }

  public synchronized PermMatrix getDefaultPerms()
  {
    return unownedObjectPerms;
  }

  public synchronized PermMatrix getDelegatableOwnedObjectPerms()
  {
    return delegatableOwnedObjectPerms;
  }

  public synchronized PermMatrix getDelegatableUnownedObjectPerms()
  {
    return delegatableUnownedObjectPerms;
  }

  /**
   * <p>This method is used to select an admin persona linked to the
   * end-user connected to the linked GanymedeSession, or the
   * unprivileged end user itself.</p>
   *
   * <p>If the persona is successfully changed, the linked
   * GanymedeSession's transaction will be aborted and re-started with
   * recalculated permissions.</p>
   *
   * @param label The canonical label of the persona (or end user) to change to
   * @param password The password for the persona to change to.  May
   * be null if label is the user's name.
   *
   * @return true if the persona could be changed
   */

  public synchronized boolean selectPersona(String label, String password)
  {
    DBObject userObject = getUser();

    if (userObject == null)
      {
        return false;
      }

    if (!findPersona(userObject, label, password))
      {
        // "Failed attempt to switch to persona {0} for user: {1}"
        Ganymede.debug(ts.l("selectPersona.no_persona", label, this.username));
        return false;
      }

    // "User {0} switched to persona {1}."
    Ganymede.debug(ts.l("selectPersona.switched", this.username, label));

    gSession.restartTransaction();

    this.visibilityFilterInvids = null;
    this.ownedObjectPerms = null;
    this.unownedObjectPerms = null;
    this.delegatableOwnedObjectPerms = null;
    this.delegatableUnownedObjectPerms = null;
    this.personaTimeStamp = null; // force updatePerms()

    try
      {
        updatePerms();
      }
    catch (NotLoggedInException ex)
      {
        return false;
      }

    gSession.resetAdminEntry();
    gSession.setLastEvent("selectPersona: " + label);

    return true;
  }

  /**
   * Sets this.personaName and this.personaInvid and returns true if a
   * persona object with name matching label and password matching
   * pass linked to the object user can be found, or if the name of
   * the object user matches label.
   *
   * @param user The DBObject containing information about the user
   * looking to change his persona
   * @param label The name of the persona he is attempting to change to
   * @param pass The password that he is using to try to change his
   * persona, or null if the user is attempting to change to his
   * unprivileged end-user privs.
   */

  private boolean findPersona(DBObject user, String label, String pass)
  {
    if (user == null || label == null)
      {
        return false;
      }

    // we don't need to check a password to switch to our end-user
    // privs

    if (user.getLabel().equals(label))
      {
        this.personaInvid = null;
        this.personaName = null;
        this.personaObj = null;

        return true;
      }

    if (pass == null)
      {
        return false;
      }

    List personae = user.getFieldValuesLocal(SchemaConstants.UserAdminPersonae);

    for (Invid invid: (List<Invid>) personae)
      {
        DBObject persona = dbSession.viewDBObject(invid).getOriginal();

        if (!label.equals(persona.getLabel()))
          {
            continue;
          }

        PasswordDBField pdbf =
          persona.getPassField(SchemaConstants.PersonaPasswordField);

        if (pdbf != null && pdbf.matchPlainText(pass))
          {
            if (persona.getLabel() == null || persona.getInvid() == null)
              {
                throw new NullPointerException();
              }

            this.personaName = persona.getLabel();
            this.personaInvid = persona.getInvid();
            this.personaObj = persona;

            return true;
          }
      }

    return false;
  }

  /**
   * This method returns a QueryResult of owner groups that the
   * current persona has access to.  This list is the transitive
   * closure of the list of owner groups in the current persona.  That
   * is, the list includes all the owner groups in the current persona
   * along with all of the owner groups those owner groups own, and so
   * on.
   */

  public synchronized QueryResult getAvailableOwnerGroups()
  {
    QueryResult result = new QueryResult();
    QueryResult fullOwnerList;

    /* -- */

    if (!isPrivileged())
      {
        return result;
      }

    try
      {
        Query q = new Query(SchemaConstants.OwnerBase);
        q.setFiltered(false);

        fullOwnerList = gSession.query(q);
      }
    catch (NotLoggedInException ex)
      {
        throw new RuntimeException(ex);
      }

    // if we're in supergash mode, return a complete list of owner groups

    if (isSuperGash())
      {
        return fullOwnerList;
      }

    // otherwise, we've got to do a very little bit of legwork

    for (ObjectHandle handle: fullOwnerList.getHandles())
      {
        if (isMemberOfOwnerGroup(handle.getInvid()))
          {
            result.addRow(handle);
          }
      }

    return result;
  }

  /**
   * <p>This method may be used to set the owner groups of any objects
   * created hereafter.</p>
   *
   * @param ownerInvids a List of Invid objects pointing to ownergroup
   * objects.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal setDefaultOwner(List<Invid> ownerInvids)
  {
    List<Invid> tmpInvids;

    /* -- */

    if (ownerInvids == null)
      {
        this.newObjectOwnerInvids = null;
        return null;
      }

    tmpInvids = new ArrayList<Invid>();

    for (Invid ownerInvidItem: ownerInvids)
      {
        // this check is actually redundant, as the InvidDBField link
        // logic would catch such for us, but it makes a nice couplet
        // with the getNum() check below, so I'll leave it here.

        if (ownerInvidItem.getType() != SchemaConstants.OwnerBase)
          {
            // "Error in setDefaultOwner()"
            // "Error.. ownerInvids contains an invalid invid"
            return Ganymede.createErrorDialog(gSession,
                                              ts.l("setDefaultOwner.error_title"),
                                              ts.l("setDefaultOwner.error_text"));
          }

        // we don't want to explicitly place the object in
        // supergash.. all objects are implicitly availble to
        // supergash, no sense in making a big deal of it.

        // this is also redundant, since DBSession.createDBObject()
        // will filter this out as well.  Err.. I probably should
        // have faith in DBSession.createDBObject() and take this
        // whole loop out, but I'm gonna leave it for now.

        if (ownerInvidItem.getNum() == SchemaConstants.OwnerSupergash)
          {
            continue;
          }

        tmpInvids.add(ownerInvidItem);
      }

    tmpInvids = Collections.unmodifiableList(tmpInvids);

    if (!this.supergashMode && !isMemberOfAllOwnerGroups(tmpInvids))
      {
        // "Error in setDefaultOwner()"
        // "Error.. ownerInvids contains invid that the persona is not a member of."
        return Ganymede.createErrorDialog(gSession,
                                          ts.l("setDefaultOwner.error_title"),
                                          ts.l("setDefaultOwner.error_text2"));
      }

    this.newObjectOwnerInvids = tmpInvids;
    gSession.setLastEvent("setDefaultOwner");
    return null;
  }

  /**
   * <p>Returns an unmodifiable List of Invids of the owner groups
   * that should be made owners of a newly created object by the
   * GanymedeSession owned by this DBPermissionManager.</p>
   *
   * <p>If an admin has authority over more than one owner group and
   * they have not previously specified the collection of owner groups
   * that they want to assign to new objects, we'll just pick the
   * first one in the list.</p>
   */

  public synchronized List<Invid> getNewOwnerInvids()
  {
    if (this.newObjectOwnerInvids != null)
      {
        return this.newObjectOwnerInvids;
      }

    // supergash is allowed to create objects with no owners, so if
    // they haven't called setDefaultOwner(), provide an empty list

    if (isSuperGash())
      {
        return Collections.unmodifiableList(new ArrayList<Invid>());
      }

    List<Invid> ownerInvids = new ArrayList<Invid>();

    QueryResult ownerList = getAvailableOwnerGroups();

    if (ownerList.size() > 0)
      {
        // If we're interactive, the client really should have
        // helped us out by prompting the user for their
        // preferred default owner list, but if we are talking
        // to a custom client, this might not be the case, in
        // which case we'll just pick the first owner group we
        // can put it into and put it there.
        //
        // The client can always manually set the owner group
        // in a created object after we return it, of course.

        ownerInvids.add(ownerList.getInvid(0));
      }

    return Collections.unmodifiableList(ownerInvids);
  }

  /**
   * <p>This method may be used to cause the server to pre-filter any
   * object listing to only show those objects directly owned by owner
   * groups referenced in the ownerInvids list.  This filtering will
   * not restrict the ability of the client to directly view any
   * object that the client's persona would normally have access to,
   * but will reduce clutter and allow the client to present the world
   * as would be seen by administrator personas with just the listed
   * ownerGroups accessible.</p>
   *
   * <p>This method cannot be used to grant access to objects that are
   * not accessible by the client's adminPersona.</p>
   *
   * <p>Calling this method with ownerInvids set to null will turn off
   * the filtering.</p>
   *
   * @param ownerInvids a List of Invid objects pointing to ownergroup objects.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal filterQueries(List<Invid> ownerInvids)
  {
    if (ownerInvids == null || ownerInvids.size() == 0)
      {
        visibilityFilterInvids = null;
        return null;
      }

    List<Invid> copyList =
      Collections.unmodifiableList(new ArrayList<Invid>(ownerInvids));

    if (this.supergashMode || isMemberOfAllOwnerGroups(copyList))
      {
        this.visibilityFilterInvids = copyList;
        gSession.setLastEvent("filterQueries");
        return null;
      }

    // "Server: Error in filterQueries()"
    // "Error.. ownerInvids contains invid that the persona is not a member of."
    return Ganymede.createErrorDialog(gSession,
                                      ts.l("filterQueries.error"),
                                      ts.l("setDefaultOwner.error_text2"));
  }

  //  Database operations

  /**
   * Returns a serialized representation of the basic category and
   * base structure on the server.
   *
   * @param hideNonEditables If true, the CategoryTransport returned
   * will only include those object types that are editable by the
   * client.
   *
   * @see arlut.csd.ganymede.rmi.Category
   */

  public synchronized CategoryTransport getCategoryTree(boolean hideNonEditables)
  {
    if (this.supergashMode)
      {
        if (Ganymede.catTransport != null)
          {
            return Ganymede.catTransport;
          }

        // hiding noneditables for supergash?  nonsense.
        Ganymede.catTransport = Ganymede.db.rootCategory.getTransport(this.gSession, true);

        return Ganymede.catTransport;
      }

    return Ganymede.db.rootCategory.getTransport(this.gSession, hideNonEditables);
  }

  /**
   * Returns a serialized representation of the object types defined
   * on the server.  This BaseListTransport object will not include
   * field information.  The client is obliged to call
   * getFieldTemplateVector() on any bases that it needs field
   * information for.
   *
   * @see arlut.csd.ganymede.common.BaseListTransport
   */

  public synchronized BaseListTransport getBaseList()
  {
    if (this.supergashMode && Ganymede.baseTransport != null)
      {
        return Ganymede.baseTransport;
      }

    BaseListTransport transport = new BaseListTransport();

    for (DBObjectBase base: Ganymede.db.bases())
      {
        base.addBaseToTransport(transport, this.gSession);
      }

    if (this.supergashMode)
      {
        Ganymede.baseTransport = transport;
      }

    return transport;
  }

  /**
   * <p>This method applies this GanymedeSession's current owner
   * filter to the given QueryResult &lt;qr&gt; and returns a
   * QueryResult with any object handles that are not matched by the
   * filter stripped.</p>
   *
   * <p>If the submitted QueryResult &lt;qr&gt; is null,
   * filterQueryResult() will itself return null.</p>
   */

  public QueryResult filterQueryResult(QueryResult qr)
  {
    if (qr == null)
      {
        return null;
      }

    if (this.visibilityFilterInvids == null ||
        this.visibilityFilterInvids.size() == 0)
      {
        return qr;
      }

    QueryResult result = new QueryResult();

    for (ObjectHandle handle: qr.getHandles())
      {
        if (filterMatch(handle.getInvid()))
          {
            result.addRow(handle);
          }
      }

    return result;
  }

  /**
   * This method returns true if the visibility filter vector allows
   * visibility of the object in question.  The visibility vector
   * works by direct ownership identity (i.e., no recursing up), so
   * it's a simple loop-di-loop.
   */

  public boolean filterMatch(Invid invid)
  {
    if (invid == null)
      {
        return false;
      }

    if (visibilityFilterInvids == null || visibilityFilterInvids.size() == 0)
      {
        return true;
      }

    return filterMatch(dbSession.viewDBObject(invid));
  }

  /**
   * This method returns true if the visibility filter vector allows
   * visibility of the object in question.  The visibility vector
   * works by direct ownership identity (i.e., no recursing up), so
   * it's a simple loop-di-loop.
   */

  public boolean filterMatch(DBObject obj)
  {
    if (obj == null)
      {
        return false;
      }

    if (visibilityFilterInvids == null || visibilityFilterInvids.size() == 0)
      {
        return true;
      }

    List owners = obj.getFieldValuesLocal(SchemaConstants.OwnerListField);

    return VectorUtils.overlaps(visibilityFilterInvids, owners);
  }

  /**
   * Returns the authorized privileges for this DBPermissionManager on
   * object.
   *
   * @return a non-null PermEntry
   */

  public synchronized PermEntry getPerm(DBObject object)
  {
    if (object == null)
      {
        throw new NullPointerException();
      }

    try
      {
        updatePerms();
      }
    catch (NotLoggedInException ex)
      {
        return PermEntry.noPerms;
      }

    if (this.supergashMode)
      {
        return PermEntry.fullPerms;
      }

    return this.getObjectPerm(object, isOwnedByUs(object));
  }

  /**
   * Returns the authorized privileges for this DBPermissionManager on
   * field fieldID in object.
   *
   * @return a non-null PermEntry
   */

  public synchronized PermEntry getPerm(DBObject object, short fieldID)
  {
    if (object == null)
      {
        throw new NullPointerException();
      }

    try
      {
        updatePerms();
      }
    catch (NotLoggedInException ex)
      {
        return PermEntry.noPerms;
      }

    boolean owned = isOwnedByUs(object);
    PermEntry objectPerm = this.getObjectPerm(object, owned);
    PermEntry fieldPerm = this.getFieldPerm(object, fieldID, owned);

    PermEntry result;

    if (fieldPerm == null)
      {
        // it's possible to lack per-field perms, in which case we
        // devolve to the object-level perms

        result = objectPerm;
      }
    else
      {
        // the only perm that we can sensibly have on a field that we
        // don't possess on the object is the create perm

        result = fieldPerm.intersection(objectPerm);

        if (fieldPerm.isCreatable())
          {
            result = result.union(PermEntry.createPerms);
          }
      }

    // the following check we do even for supergash, as we don't want
    // to allow supergash-privileged end users from messing with
    // metadata.
    //
    // DBEditSet.commit_recordModificationDates() bypasses perms, so
    // no problem there

    if ((fieldID == SchemaConstants.OwnerListField &&
         (!owned || this.isEndUser())) ||
        (fieldID == SchemaConstants.CreationDateField ||
         fieldID == SchemaConstants.CreatorField ||
         fieldID == SchemaConstants.ModificationDateField ||
         fieldID == SchemaConstants.ModifierField))
      {
        result = PermEntry.viewPerms.intersection(result);
      }

    return result != null ? result : PermEntry.noPerms;
  }

  /**
   * <p>This method returns the generic permissions for a object type.
   * This is currently used primarily to check to see whether a user
   * has privileges to create an object of a specific type.</p>
   *
   * @param ownedByUs If true, this method will return the permission
   * that the current persona would have for an object that was owned
   * by the current persona.  If false, this method will return the
   * default permissions that apply to objects not owned by the
   * persona.
   *
   * @return a non-null PermEntry
   */

  synchronized PermEntry getPerm(short baseID, boolean ownedByUs)
  {
    try
      {
        updatePerms();
      }
    catch (NotLoggedInException ex)
      {
        return PermEntry.noPerms;
      }

    if (this.supergashMode)
      {
        return PermEntry.fullPerms;
      }

    PermMatrix pm = ownedByUs ? this.ownedObjectPerms : this.unownedObjectPerms;
    PermEntry result = pm.getPerm(baseID);

    return result != null ? result : PermEntry.noPerms;
  }

  /**
   * <p>This method returns the current persona's default permissions
   * for a base and field.  This permission applies generically to
   * objects that are not owned by this persona and to objects that
   * are owned.</p>
   *
   * <p>This is used by the {@link
   * arlut.csd.ganymede.server.GanymedeSession#dump(arlut.csd.ganymede.common.Query)
   * dump()} code to determine whether a field should be added to the
   * set of possible fields to be returned at the time that the dump
   * results are being prepared.</p>
   *
   * @return a non-null PermEntry
   */

  synchronized PermEntry getPerm(short baseID, short fieldID, boolean ownedByUs)
  {
    try
      {
        updatePerms();
      }
    catch (NotLoggedInException ex)
      {
        return PermEntry.noPerms;
      }

    if (this.supergashMode)
      {
        return PermEntry.fullPerms;
      }

    PermMatrix pm = ownedByUs ? ownedObjectPerms : unownedObjectPerms;
    PermEntry result = pm.getPerm(baseID, fieldID);

    if (result == null)
      {
        result = pm.getPerm(baseID);
      }

    return result != null ? result : PermEntry.noPerms;
  }

  /**
   * Returns the permissions for obj.
   *
   * @return a non-null PermEntry
   */

  private PermEntry getObjectPerm(DBObject obj, boolean ownedByUs)
  {
    if (obj == null)
      {
        throw new NullPointerException();
      }

    // getPerm() calls updatePerms() before calling us

    if (this.supergashMode)
      {
        return PermEntry.fullPerms;
      }

    PermEntry customP = obj.getHook().permOverride(gSession, obj);

    if (customP != null)
      {
        return customP;
      }

    PermEntry expandP = obj.getHook().permExpand(gSession, obj);

    if (expandP == null)
      {
        expandP = PermEntry.noPerms;
      }

    PermMatrix pm = ownedByUs ? ownedObjectPerms : unownedObjectPerms;

    // we always union below so that we'll return PermEntry.noPerms
    // rather than null even if the applicable PermMatrix doesn't have
    // an entry for this object type.

    return expandP.union(pm.getPerm(obj.getTypeID()));
  }

  /**
   * Returns the permissions for fieldID in obj, without considering
   * object-level permissions.
   *
   * @return A null PermEntry if no appropriate field-level permission
   * is granted, or a non-null PermEntry if we have an explicit
   * permission recorded for this field type.
   */

  private synchronized PermEntry getFieldPerm(DBObject obj,
                                              short fieldID,
                                              boolean ownedByUs)
  {
    if (obj == null)
      {
        throw new NullPointerException();
      }

    // getPerm() calls updatePerms() before calling us

    if (this.supergashMode)
      {
        return PermEntry.fullPerms;
      }

    PermEntry customP = obj.getHook().permOverride(gSession, obj, fieldID);

    if (customP != null)
      {
        return customP;
      }

    PermMatrix pm = ownedByUs ? ownedObjectPerms: unownedObjectPerms;
    PermEntry expandP = obj.getHook().permExpand(gSession, obj, fieldID);

    if (expandP == null)
      {
        // unlike in the getObjectPerm case, we do want to return null
        // if there is no explicit permission recorded for a specific
        // field

        return pm.getPerm(obj.getTypeID(), fieldID);
      }
    else
      {
        return expandP.union(pm.getPerm(obj.getTypeID(), fieldID));
      }
  }

  /**
   * <p>Sets supergash mode and/or the four PermMatrix objects that
   * DBPermissionManager uses to track permissions.</p>
   *
   * <p>This method is synchronized, and a whole lot of operations in
   * the server need to pass through here to ensure that the effective
   * permissions for this session haven't changed.  This method is
   * designed to return very quickly if permissions have not
   * changed.</p>
   */

  private synchronized void updatePerms() throws NotLoggedInException
  {
    if (beforeversupergash || Ganymede.firstrun)
      {
        this.supergashMode = true;
        return;
      }

    if (!rolesWereChanged() && !personaWasChanged() && !userWasChanged())
      {
        // there's a bit of a race here, as the calling getPerm()
        // method won't check for the currency of the perms we've got
        // configured until the next updatePerms() call, but returning
        // slightly out of date perms won't break consistency.

        return;
      }

    DBReadLock updatePermsLock = null;

    try
      {
        updatePermsLock = dbSession.openReadLock(Ganymede.db.getPermBases());

        updateDefaultRoleObj();
        DBObject persona = updatePersonaObj();

        this.supergashMode = false;

        if (this.isEndUser())
          {
            initializeDefaultPerms();
            configureEndUser();
            return;
          }

        if (persona.containsFieldValueLocal(SchemaConstants.PersonaGroupsField,
                                            SUPERGASH_GROUP_INVID))
          {
            this.supergashMode = true;
            return;
          }

        initializeDefaultPerms();

        // Personae do not get the default 'objects-owned' privileges for
        // the wider range of objects under their ownership.  Any special
        // privileges granted to admins over objects owned by them must be
        // derived from a non-default role.

        List roles = persona.getFieldValuesLocal(SchemaConstants.PersonaPrivs);

        for (Invid role: (List<Invid>) roles)
          {
            DBObject roleObj = dbSession.viewDBObject(role).getOriginal();

            if (roleObj.hasField(SchemaConstants.RoleMatrix))
              {
                PermissionMatrixDBField pmdbf =
                  roleObj.getPermField(SchemaConstants.RoleMatrix);

                PermMatrix m = pmdbf.getMatrix();

                this.ownedObjectPerms = this.ownedObjectPerms.union(m);

                if (roleObj.isSet(SchemaConstants.RoleDelegatable))
                  {
                    this.delegatableOwnedObjectPerms =
                      this.delegatableOwnedObjectPerms.union(m);
                  }
              }

            if (roleObj.hasField(SchemaConstants.RoleDefaultMatrix))
              {
                PermissionMatrixDBField pmdbf =
                  roleObj.getPermField(SchemaConstants.RoleDefaultMatrix);

                PermMatrix m = pmdbf.getMatrix();

                this.ownedObjectPerms = this.ownedObjectPerms.union(m);
                this.unownedObjectPerms = this.unownedObjectPerms.union(m);

                if (roleObj.isSet(SchemaConstants.RoleDelegatable))
                  {
                    this.delegatableOwnedObjectPerms =
                      this.delegatableOwnedObjectPerms.union(m);

                    this.delegatableUnownedObjectPerms =
                      this.delegatableUnownedObjectPerms.union(m);
                  }
              }
          }
      }
    catch (InterruptedException ex)
      {
        throw new RuntimeException(ex);
      }
    catch (Exception ex2)
      {
        if (!this.valid)
          {
            throw new NotLoggedInException(ex2);
          }
      }
    finally
      {
        try
          {
            if (updatePermsLock != null)
              {
                dbSession.releaseLock(updatePermsLock);
              }
          }
        catch (Exception ex3)
          {
            if (this.valid)
              {
                throw new RuntimeException(ex3);
              }
          }
      }
  }

  /**
   * Checks to see if any Role Objects have changed in the server
   * since we last updated our perms.
   *
   * @return true if any changes have been made to Role Objects in the
   * server
   */

  private synchronized boolean rolesWereChanged()
  {
    return (this.rolesLastCheckedTimeStamp == null ||
            Ganymede.db.getObjectBase(SchemaConstants.RoleBase).changedSince(this.rolesLastCheckedTimeStamp));
  }

  /**
   * Updates the defaultRoleObj we reference.  Separated from
   * rolesWereChanged() so that we can do this part in a DBReadLock.
   */

  private synchronized void updateDefaultRoleObj()
  {
    try
      {
        // get the time before we view our object, so if we get a race
        // between the date and the viewDBObject call, that will cause
        // us to check again next time.

        Date roleTime = new Date();

        this.defaultRoleObj =
          dbSession.viewDBObject(DEFAULT_ROLE_INVID).getOriginal();

        this.rolesLastCheckedTimeStamp = roleTime;
      }
    catch (NullPointerException ex)
      {
        // "Serious error!  No default permissions object found in database!"
        throw new IllegalStateException(ts.l("updateDefaultRoleObj.no_default_perms"), ex);
      }
  }

  /**
   * Returns true if this.personaObj may have changed in the database.
   *
   * @return true if this.personaObj may have been changed
   */

  private synchronized boolean personaWasChanged()
  {
    return ((this.personaObj == null && this.personaInvid != null) ||
            (this.personaObj != null && this.personaInvid == null) ||
            this.personaTimeStamp == null ||
            Ganymede.db.getObjectBase(SchemaConstants.PersonaBase).changedSince(this.personaTimeStamp));
  }

  /**
   * Returns true if this session is being run by an end-user and the
   * user linked to this session may have changed in the database.
   *
   * @return true if the user referenced by this.userInvid may have
   * been changed
   */

  private synchronized boolean userWasChanged()
  {
    return this.isEndUser() &&
      (this.userTimeStamp == null ||
       Ganymede.db.getObjectBase(SchemaConstants.UserBase).changedSince(this.userTimeStamp));
  }

  /**
   * Updates the personaObj we reference.  Separated from
   * personaWasChanged() so that we can do this part in a DBReadLock.
   */

  private synchronized DBObject updatePersonaObj()
  {
    // get the time before we view our object, so if we get a race
    // between the date and the viewDBObject call, that will cause
    // us to check again next time.

    Date personaTime = new Date();

    if (this.personaInvid == null)
      {
        this.personaObj = null;
        this.personaTimeStamp = personaTime;
        return null;
      }

    DBObject currentPersonaObj = dbSession.viewDBObject(this.personaInvid);

    this.personaTimeStamp = personaTime;

    if (currentPersonaObj == null)
      {
        this.valid = false;
        this.personaTimeStamp = null;

        // "Persona object {0} deleted while persona {0} logged in with session {1}"
        gSession.forceOff(ts.l("updatePersonaObj.not_logged_in",
                               this.personaName, this.sessionName));

        return null;
      }

    this.personaObj = currentPersonaObj.getOriginal();

    return this.personaObj;
  }

  /**
   * This convenience method resets all privilege matricies from the
   * default unowned permissions in the default Role object.
   */

  private synchronized void initializeDefaultPerms()
  {
    PermissionMatrixDBField pField =
      this.defaultRoleObj.getPermField(SchemaConstants.RoleDefaultMatrix);

    PermMatrix defaultMatrix;

    if (pField != null)
      {
        defaultMatrix = pField.getMatrix();
      }
    else
      {
        defaultMatrix = new PermMatrix();
      }

    this.unownedObjectPerms = defaultMatrix;
    this.delegatableUnownedObjectPerms = defaultMatrix;
    this.ownedObjectPerms = defaultMatrix;
    this.delegatableOwnedObjectPerms = defaultMatrix;
  }

  /**
   * <p>Do the perms configuration needed for an unprivileged end
   * user.</p>
   *
   * <p>This is the only case in which the defaultRoleObj's owned
   * objects matrix (SchemaConstants.RoleMatrix) is consulted.</p>
   */

  private synchronized void configureEndUser()
  {
    // get the time before we view our object, so if we get a race
    // between the date and the viewDBObject call, that will cause
    // us to check again next time.

    Date userTime = new Date();

    if (getUser() == null)
      {
        this.userTimeStamp = null;
        this.valid = false;

        // "User object for user {0} deleted while user {0} logged in with session {1}"
        gSession.forceOff(ts.l("configureEndUser.not_logged_in",
                               this.username, this.sessionName));

        return;
      }

    this.userTimeStamp = userTime;

    PermissionMatrixDBField permField = this.defaultRoleObj.getPermField(SchemaConstants.RoleMatrix);

    if (permField == null)
      {
        return;
      }

    PermMatrix selfPerms = permField.getMatrix();

    this.ownedObjectPerms = this.ownedObjectPerms.union(selfPerms);
    this.delegatableOwnedObjectPerms =
      this.delegatableOwnedObjectPerms.union(selfPerms);
  }

  /**
   * Returns true if the active persona is allowed to exert owned
   * object permissions against obj.  Note that isOwnedByUs() checks
   * the grantOwnership() method in custom plugin code, and must not
   * be called from a grantOwnership call, lest recursion result.
   *
   * @perm obj The DBObject to check ownership privileges on
   */

  private synchronized boolean isOwnedByUs(DBObject obj)
  {
    if (obj == null)
      {
        return false;
      }

    if (this.supergashMode)
      {
        return true;
      }

    // end users are considered to own themselves

    if (!isPrivileged() &&
        this.userInvid != null &&
        this.userInvid.equals(obj.getInvid()))
      {
        return true;
      }

    while (obj.isEmbedded())
      {
        if (obj.getHook().grantOwnership(gSession, obj))
          {
            return true;
          }

        Invid inv = (Invid) obj.getFieldValueLocal(SchemaConstants.ContainerField);

        if (inv == null)
          {
            // "isOwnedByUs couldn''t find owner of embedded object {0}"
            throw new IntegrityConstraintException(ts.l("isOwnedByUs.integrity",
                                                        obj.getLabel()));
          }

        obj = dbSession.viewDBObject(inv);
      }

    if (obj.getHook().grantOwnership(gSession, obj))
      {
        return true;
      }

    if (!isPrivileged())
      {
        return false;
      }

    return personaMatch(obj);
  }

  /**
   * Returns true if the active person has ownership privileges over
   * obj without consulting custom plugin code, solely through owner
   * group membership.
   */

  public boolean personaMatch(DBObject obj)
  {
    if (obj == null)
      {
        return false;
      }

    if (this.supergashMode)
      {
        return true;
      }

    // personaMatch() may be called from custom code without going
    // through isOwnedByUs(), so make sure that we've got the
    // top-level object

    if (obj.isEmbedded())
      {
        obj = dbSession.getContainingObj(obj);
      }

    // end users are considered to own themselves

    if (!isPrivileged())
      {
        return this.userInvid != null && this.userInvid.equals(obj.getInvid());
      }

    List<Invid> owners = (List<Invid>)
      obj.getFieldValuesLocal(SchemaConstants.OwnerListField);

    // All owner group objects are considered to be self-owning.

    if (obj.getTypeID() == SchemaConstants.OwnerBase)
      {
        if (!owners.contains(obj.getInvid()))
          {
            owners.add(obj.getInvid());
          }
      }

    // All admin personae are considered to be owned by the owner groups
    // that they are members of

    if (obj.getTypeID() == SchemaConstants.PersonaBase)
      {
        List<Invid> values = (List<Invid>)
          obj.getFieldValuesLocal(SchemaConstants.PersonaGroupsField);

        owners = arlut.csd.Util.VectorUtils.union(owners, values);
      }

    return isMemberOfAnyOwnerGroups(owners);
  }

  /**
   * Returns true if this.personaInvid is a member of the owner group
   * pointed to by the owner Invid, or in any of the owner groups that
   * own that owner group, transitively.
   *
   * @param owner An Invid pointing to an OwnerBase object
   * @return true if a match is found
   */

  private synchronized boolean isMemberOfOwnerGroup(Invid owner)
  {
    return isMemberOfOwnerGroup(owner, new HashSet<Invid>());
  }

  /**
   * Returns true if this.personaInvid is a member of the owner group
   * pointed to by the owner Invid, or in any of the owner groups that
   * own that owner group, transitively.
   *
   * @param owner An Invid pointing to an OwnerBase object
   * @param alreadySeen A Set of owner group Invid's that have already
   * been checked and which are known. (For infinite loop avoidance).
   *
   * @return true if a match is found
   */

  private synchronized boolean isMemberOfOwnerGroup(Invid owner, Set<Invid> alreadySeen)
  {
    if (owner == null)
      {
        throw new IllegalArgumentException("Null owner");
      }

    if (owner.getType() != SchemaConstants.OwnerBase)
      {
        throw new IllegalArgumentException("bad owner group");
      }

    if (alreadySeen.contains(owner))
      {
        return false;           // cycle
      }

    alreadySeen.add(owner);

    DBObject ownerObj = dbSession.viewDBObject(owner).getOriginal();

    List<Invid> personaeInOwnerGroup = (List<Invid>)
      ownerObj.getFieldValuesLocal(SchemaConstants.OwnerMembersField);

    if (personaeInOwnerGroup.contains(getPersonaInvid()))
      {
        return true;
      }

    // didn't find, recurse up

    List<Invid> ownersOfOwnerGroup = (List<Invid>)
      ownerObj.getFieldValuesLocal(SchemaConstants.OwnerListField);

    return isMemberOfAnyOwnerGroups(ownersOfOwnerGroup, alreadySeen);
  }

  /**
   * Returns true if this.personaInvid is a member of any of the owner
   * group objects whose Invids are included in the owners List, or in
   * any of the owner groups that own those owner groups,
   * transitively.
   *
   * @param owners A List of invids pointing to OwnerBase objects
   *
   * @return true if a match is found
   */

  private synchronized boolean isMemberOfAnyOwnerGroups(List<Invid> owners)
  {
    return isMemberOfAnyOwnerGroups(owners, new HashSet<Invid>());
  }

  /**
   * Returns true if this.personaInvid is a member of any of the owner
   * group objects whose Invids are included in the owners List, or in
   * any of the owner groups that own those owner groups,
   * transitively.
   *
   * @param owners A List of invids pointing to OwnerBase objects
   * @param alreadySeen A Set of owner group Invid's that have already
   * been checked.  (For infinite loop avoidance).
   *
   * @return true if a match is found
   */

  private synchronized boolean isMemberOfAnyOwnerGroups(List<Invid> owners,
                                                        Set<Invid> alreadySeen)
  {
    if (owners == null)
      {
        return false;
      }

    for (Invid owner: owners)
      {
        if (isMemberOfOwnerGroup(owner, alreadySeen))
          {
            return true;
          }
      }

    return false;
  }

  /**
   * This helper method iterates through the owners vector and checks
   * to see if the current personaInvid is a member of all of the
   * groups through either direct membership or through membership of
   * an owning group.  This method depends on isMemberOfOwnerGroup().
   */

  private synchronized boolean isMemberOfAllOwnerGroups(List<Invid> owners)
  {
    if (owners == null)
      {
        return false;
      }

    for (Invid owner: owners)
      {
        if (!isMemberOfOwnerGroup(owner))
          {
            return false;
          }
      }

    return true;
  }
}
