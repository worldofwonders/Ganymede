/*

   dhcpSubnetCustom.java

   This file is a management class for DHCP Network objects in Ganymede.

   Created: 1 August 2013

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.gasharl;

import java.util.Vector;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBEditSet;
import arlut.csd.ganymede.server.DBField;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.DBObjectBase;
import arlut.csd.ganymede.server.DBSession;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.InvidDBField;
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                dhcpSubnetCustom

------------------------------------------------------------------------------*/

public class dhcpSubnetCustom extends DBEditObject implements SchemaConstants, dhcpSubnetSchema {

  /**
   * <p>Customization Constructor</p>
   */

  public dhcpSubnetCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   * <p>Create new object constructor</p>
   */

  public dhcpSubnetCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   * <p>Check-out constructor, used by DBObject.createShadow() to pull
   * out an object for editing.</p>
   */

  public dhcpSubnetCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
  }

  /**
   * <p>Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p>Note that this method will not be called if the controlling
   * GanymedeSession's enableOversight is turned off, as in
   * bulk loading.</p>
   *
   * <p>Note as well that the designated label field for objects are
   * always required, whatever this method returns, and that this
   * requirement holds without regard to the GanymedeSession's
   * enableOversight value.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  @Override public boolean fieldRequired(DBObject object, short fieldid)
  {
    switch (fieldid)
      {
        case NAME:
        case NETWORK_NUMBER:
        case NETWORK_MASK:
          return true;
      }

    if (fieldid == GUEST_RANGE)
      {
        return object.isSet(ALLOW_REGISTERED_GUESTS);
      }

    return false;
  }

  /**
   * <p>Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of
   * {@link arlut.csd.ganymede.server.DBField DBField} will
   * wind up calling up to here to let us override the normal visibility
   * process.</p>
   *
   * <p>Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.</p>
   *
   * <p>If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  @Override public boolean canSeeField(DBSession session, DBField field)
  {
    if (field.getID() == GUEST_RANGE ||
        field.getID() == GUEST_OPTIONS)
      {
        return field.getOwner().isSet(ALLOW_REGISTERED_GUESTS);
      }

    return super.canSeeField(session, field);
  }

  /**
   * <p>Customization method to verify whether a specific field
   * in object should be cloned using the basic field-clone
   * logic.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  @Override public boolean canCloneField(DBSession session, DBObject object, DBField field)
  {
    if (field.getID() == NETWORK_NUMBER)
      {
        return false;
      }

    return super.canCloneField(session, object, field);
  }

  /**
   * <p>Hook to allow the cloning of an object.  If this object type
   * supports cloning (which should be very much customized for this
   * object type.. creation of the ancillary objects, which fields to
   * clone, etc.), this customization method will actually do the
   * work.</p>
   *
   * <p>This method is called on a newly created object, in order to
   * clone the state of origObj into it.  This method does not
   * actually create a new object.. that is handled by {@link
   * arlut.csd.ganymede.server.GanymedeSession#clone_db_object(arlut.csd.ganymede.common.Invid)
   * clone_db_object()} before this method is called on the newly
   * created object.</p>
   *
   * <p>The default (DBEditObject) implementation of this method will
   * only clone fields for which {@link
   * arlut.csd.ganymede.server.DBEditObject#canCloneField(arlut.csd.ganymede.server.DBSession,
   * arlut.csd.ganymede.server.DBObject,
   * arlut.csd.ganymede.server.DBField) canCloneField()} returns true,
   * and which are not connected to a namespace (and thus could not
   * possibly be cloned, because the values are constrained to be
   * unique and non-duplicated).</p>
   *
   * <p>If one or more fields in the original object are unreadable by
   * the cloning session, we will provide a list of fields that could
   * not be cloned due to a lack of read permissions in a dialog in
   * the ReturnVal.  Such a problem will not result in a failure code
   * being returned, however.. the clone will succeed, but an
   * informative dialog will be provided to the user.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses, but
   * this method's default logic will probably do what you need it to
   * do.  If you need to make changes, try to chain your subclassed
   * method to this one via super.cloneFromObject().</p>
   *
   * @param session The DBSession that the new object is to be created in
   * @param origObj The object we are cloning
   * @param local If true, fields that have choice lists will not be checked against
   * those choice lists and read permissions for each field will not be consulted.
   * The canCloneField() method will still be consulted, however.
   *
   * @return A standard ReturnVal status object.  May be null on success, or
   * else may carry a dialog with information on problems and a success flag.
   */

  @Override public ReturnVal cloneFromObject(DBSession session, DBObject origObj, boolean local)
  {
    boolean parentCloneProblem = false;
    ReturnVal retVal = super.cloneFromObject(session, origObj, local);

    if (!ReturnVal.didSucceed(retVal))
      {
        return retVal;
      }

    return ReturnVal.merge(retVal, copyObjects(session, origObj, OPTIONS, local));
  }

  private ReturnVal copyObjects(DBSession session, DBObject origObject, short copyFieldID, boolean local)
  {
    if (!origObject.isDefined(copyFieldID))
      {
        return null;
      }

    InvidDBField origField = (InvidDBField) origObject.getField(copyFieldID);
    InvidDBField newField = (InvidDBField) getField(copyFieldID);

    StringBuilder resultBuf = new StringBuilder();

    try
      {
        for (Invid oldInvid: (Vector<Invid>) origField.getValuesLocal())
          {
            DBObject origSubObject = session.viewDBObject(oldInvid);
            ReturnVal tmpVal;

            try
              {
                tmpVal = newField.createNewEmbedded(local);
              }
            catch (GanyPermissionsException ex)
              {
                tmpVal = Ganymede.createErrorDialog(session.getGSession(),
                                                    "permissions",
                                                    "permissions failure creating embedded object " + ex);
              }

            if (!ReturnVal.didSucceed(tmpVal))
              {
                if (tmpVal.getDialog() != null)
                  {
                    resultBuf.append("\n\n");
                    resultBuf.append(tmpVal.getDialog().getText());
                  }

                continue;
              }

            DBEditObject newSubObject = session.editDBObject(tmpVal.getInvid());
            tmpVal = newSubObject.cloneFromObject(session, origSubObject, local);

            if (!ReturnVal.didSucceed(tmpVal))
              {
                if (tmpVal.getDialog() != null)
                  {
                    resultBuf.append("\n\n");
                    resultBuf.append(tmpVal.getDialog().getText());
                  }
              }
          }
      }
    catch (NotLoggedInException ex)
      {
        return Ganymede.loginError(ex);
      }

    if (resultBuf.length() != 0)
      {
        ReturnVal retVal = new ReturnVal(true, false);

        retVal.setDialog(new JDialogBuff("Possible Clone Problems",
                                         resultBuf.toString(),
                                         "Ok", null, "ok.gif"));
      }

    return null;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval
   * of any scalar set operation, and to take any special actions in
   * reaction to the set.  When a scalar field has its value set, it
   * will call its owners finalizeSetValue() method, passing itself as
   * the &lt;field&gt; parameter, and passing the new value to be
   * approved as the &lt;value&gt; parameter.  A Ganymede customizer
   * who creates custom subclasses of the DBEditObject class can
   * override the finalizeSetValue() method and write his own logic
   * to examine any change and either approve or reject the change.</p>
   *
   * <p>A custom finalizeSetValue() method will typically need to
   * examine the field parameter to see which field is being changed,
   * and then do the appropriate checking based on the value
   * parameter.  The finalizeSetValue() method can call the normal
   * this.getFieldValueLocal() type calls to examine the current state
   * of the object, if such information is necessary to make
   * appropriate decisions.</p>
   *
   * <p>If finalizeSetValue() returns null or a ReturnVal object with
   * a positive success value, the DBField that called us is
   * guaranteed to proceed to make the change to its value.  If this
   * method returns a non-success code in its ReturnVal, as with the
   * result of a call to Ganymede.createErrorDialog(), the DBField
   * that called us will not make the change, and the field will be
   * left unchanged.  Any error dialog returned from finalizeSetValue()
   * will be passed to the user.</p>
   *
   * <p>The DBField that called us will take care of all standard
   * checks on the operation (including a call to our own
   * verifyNewValue() method before calling this method.  Under normal
   * circumstances, we won't need to do anything here.
   * finalizeSetValue() is useful when you need to do unusually
   * involved checks, and for when you want a chance to trigger other
   * changes in response to a particular field's value being
   * changed.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  @Override public synchronized ReturnVal finalizeSetValue(DBField field, Object value)
  {
    // If the allow register guests checkbox is changed, hide/show the field and options next.

    if (field.getID() == ALLOW_REGISTERED_GUESTS)
      {
        ReturnVal result = ReturnVal.success();

        if (value == null || Boolean.FALSE.equals(value))
          {
            getDBSession().checkpoint("clearing guest fields");

            try
              {
                StringDBField guest_range = (StringDBField) getField(GUEST_RANGE);
                result = ReturnVal.merge(result, guest_range.setValueLocal(null));

                DBField guest_options = (DBField) getField(GUEST_OPTIONS);

                try
                  {
                    result = ReturnVal.merge(result, guest_options.deleteAllElements());
                  }
                catch (GanyPermissionsException ex)
                  {
                    return Ganymede.createErrorDialog(this.getGSession(),
                                                      "permissions",
                                                      "permissions error deleting embedded object" + ex);
                  }
              }
            finally
              {
                if (!ReturnVal.didSucceed(result))
                  {
                    getDBSession().rollback("clearing guest fields");
                  }
                else
                  {
                    getDBSession().popCheckpoint("clearing guest fields");
                  }
              }
          }

        result.addRescanField(field.getOwner().getInvid(), GUEST_RANGE);
        result.addRescanField(field.getOwner().getInvid(), GUEST_OPTIONS);

        return result;
      }

    return null;
  }
}
