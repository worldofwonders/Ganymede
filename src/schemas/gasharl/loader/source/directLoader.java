/*
   directLoader.java

   Ganymede directLoader module

   This module is intended to be bound to the bulk of the Ganymede
   server and automatically create a whole bunch of objects
   to initialize the database from GASH data.

   --

   Created: 20 October 1997
   Release: $Name:  $
   Version: $Revision: 1.41 $
   Last Mod Date: $Date: 1999/10/07 17:38:48 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede.loader;

import java.net.*;

import java.io.*;
import java.util.*;

import java.rmi.*;
import java.rmi.server.*;

import arlut.csd.ganymede.*;
import arlut.csd.ganymede.custom.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    directLoader

------------------------------------------------------------------------------*/

public class directLoader {

  static final boolean debug = false;

  static GanymedeServer my_server;
  static Session my_session;
  static directLoaderClient my_client;

  static Hashtable adminUsers = new Hashtable();
  static Vector ownerGroups = new Vector();
  static Hashtable personaMap = new Hashtable();

  /**
   *
   * maps user category names to invid's.
   *
   */

  static Hashtable userCategories = new Hashtable();
  static Hashtable categoryInvids = new Hashtable();

  static Hashtable users = new Hashtable();
  static Hashtable userInvids = new Hashtable();

  static Vector externalAliases = new Vector();
  static Vector mailGroups = new Vector();
  static Hashtable userMail = new Hashtable();
  static Hashtable mailInvids = new Hashtable();

  /**
   *
   * maps group names to contract numbers
   *
   */

  static Hashtable groupContracts = new Hashtable();

  /**
   *
   * maps contract numbers to Contract objects
   *
   */

  static Hashtable contracts = new Hashtable();

  static Hashtable groups = new Hashtable();
  static Hashtable groupID = new Hashtable();
  static Hashtable groupInvid = new Hashtable();

  static Hashtable userNetgroup = new Hashtable();
  static Hashtable systemNetgroup = new Hashtable();

  static Hashtable systemTypes = new Hashtable();
  static Hashtable systemTypeInvids = new Hashtable();

  static Hashtable rooms = new Hashtable();
  static Hashtable roomInvids = new Hashtable();

  static Hashtable networks = new Hashtable();
  static Hashtable networkInvids = new Hashtable();

  static Hashtable systemInvid = new Hashtable();

  static Hashtable volumes = new Hashtable(); // map volume name -> Volume object
  static Hashtable volumeInvids = new Hashtable(); // map volume name -> invid

  static Vector maps = new Vector();
  static Hashtable mapInvids = new Hashtable();	// map map name -> invid

  static Vector mapEntries = new Vector();

  static Invid gashadminPermInvid;		// the standard GASH admin privileges

  static BufferedReader inReader = null;
  static boolean done = false;

  static Group groupObj;
  static UserNetgroup uNetObj;
  static SystemNetgroup sNetObj;

  static SystemLoader sysLoader;

  // --

  protected boolean connected = false;

  /* -- */

  public static void main(String args[])
  {
    if (args.length < 1)
      {
	System.err.println("Usage error: java arlut.csd.ganymede.loader.directLoader <properties file>");
      }
    
    Ganymede.loadProperties(args[0]);
      
    my_server = Ganymede.directInit(Ganymede.dbFilename);

    // now login

    try
      {
	my_client = new directLoaderClient(my_server);
      }
    catch (RemoteException ex)
      {
	System.err.println("RMI Error: Couldn't log in to server.\n" + ex.getMessage());
	return;
      }
    catch (NullPointerException ex)
      {
	System.err.println("Error: Didn't get server reference.\n\nPlease Quit and Restart.");
	return;
      }
    catch (Exception ex) 
      {
	System.err.println("Error: Unrecognized exception " + ex);
	return;
      }

    try
      {
	System.err.println("Writing schema summary to schema.list");
	FileOutputStream textOutStream = new FileOutputStream("schema.list");
	PrintWriter textOut = new PrintWriter(textOutStream);
	Ganymede.db.printBases(textOut);
	textOut.close();
	textOutStream.close();
	System.err.println("Completed writing schema summary to schema.list");
      }
    catch (IOException ex)
      {
	System.err.println("heck.");
      }

    System.err.println("---------------------------------------- Initiating GASH file scan --------------------");

    scanOwnerGroups();
    scanUserCategories();
    scanUsers();
    scanEmail();
    scanContracts();		// arl
    scanGroups();
    scanUserNetgroups();
    scanSystemTypes();
    scanRooms();
    scanSystems();
    scanSystemNetgroups();
    scanAutomounter();

    System.err.println("\n---------------------------------------- Completed GASH file scan --------------------\n");

    // Okay.. at this point we've scanned the files we need to scan..
    // now we initialize the database module and create the objects

    try
      {
	my_client.session.openTransaction("GASH directLoader");

	String key;
	Invid invid, objInvid;
	Enumeration enum;
	OwnerGroup ogRec;
	DBEditObject current_obj;
	db_field current_field;
	pass_field p_field;
	DBObjectBase base;
	ReturnVal retVal;

	/* -- */

	System.err.println("\nCreating GASH standard permission matrix");

	my_client.session.checkpoint("GASHAdmin");

	current_obj = (DBEditObject) createObject(SchemaConstants.RoleBase);
	gashadminPermInvid = current_obj.getInvid();

	System.err.println("Trying to create a new GASHAdmin perm object: " + gashadminPermInvid.toString());

	retVal = current_obj.setFieldValueLocal(SchemaConstants.RoleName, "GASH Admin");

	if (retVal != null && !retVal.didSucceed())
	  {
	    // the gash privilege matrix object is already in the schema.. rollback

	    my_client.session.rollback("GASHAdmin");

	    // note that QueryDataNode uses the label as the
	    // comparator if you don't provide a field id

	    Query q = new Query(SchemaConstants.RoleBase, 
				new QueryDataNode(QueryDataNode.EQUALS, "GASH Admin"),
				false);

	    QueryResult results = my_client.session.query(q);
	    
	    if (results != null && results.size() != 1)
	      {
		// didn't find it
		System.err.println("Could neither create a new GASH Admin permission matrix, nor find the old one.");
		System.exit(0);
	      }

	    gashadminPermInvid = results.getInvid(0);

	    // We'll get a null pointer exception here if it didn't work out

	    System.err.println("Using pre-existing gash privilege matrix: " + 
			       gashadminPermInvid.toString());
	  }

	current_obj = (DBEditObject) editObject(gashadminPermInvid);

	System.err.println("\n***Default permissions matrix is " + gashadminPermInvid.toString());

	// set permissions for objects owned by the GASH administrator

	perm_field pf = (perm_field) current_obj.getField(SchemaConstants.RoleMatrix);

	// visible, editable, create, delete

	PermEntry defPerm = new PermEntry(true, true, true, true);
	
	pf.setPerm(SchemaConstants.UserBase, defPerm);
	pf.setPerm((short) 257, defPerm); // group privs
	pf.setPerm((short) 263, defPerm); // systems privs
	pf.setPerm((short) 267, defPerm); // I.P. network
	pf.setPerm((short) 265, defPerm); // System Interface
	pf.setPerm((short) 271, defPerm); // Systems Netgroups
	pf.setPerm((short) 270, defPerm); // User Netgroups
	pf.setPerm((short) 269, defPerm); // room
	pf.setPerm((short) 258, defPerm); // shell

	// set default permissions for objects *not* owned by the GASH administrator

	// Here we want to make all rooms, automounter volumes,
	// networks and system types 'editable', but not grant
	// permission to any individual fields.. this will have the
	// effect of allowing rooms, system types, and networks to show
	// up in choice lists, but will not allow a gash admin to
	// directly edit the objects' fields.. the custom code for
	// these objects permit anonymous linking to allow systems and
	// interfaces to be linked in.

	pf = (perm_field) current_obj.getField(SchemaConstants.RoleDefaultMatrix);

	// visible, editable, create, delete

	defPerm = new PermEntry(true, true, true, false);
	PermEntry fieldPerm = new PermEntry(true, false, false, false);

	pf.setPerm((short) 269, defPerm); // room
	pf.setFieldPerms((short) 269, fieldPerm, false);

	pf.setPerm((short) 276, defPerm); // automounter volume
	pf.setFieldPerms((short) 276, fieldPerm, false);

	pf.setPerm((short) 267, defPerm); // network
	pf.setFieldPerms((short) 267, fieldPerm, false);

	pf.setPerm((short) 272, defPerm); // system type
	pf.setFieldPerms((short) 272, fieldPerm, false);

	// now, the ownerGroups Vector has been loaded for us by the
	// scanOwnerGroups() method.  go ahead and register owner
	// groups for the set of differentiable owner prefixes from
	// the GASH admin_info file

	System.err.println("\nCreating ownergroups in server: ");

	enum = ownerGroups.elements();

	while (enum.hasMoreElements())
	  {
	    ogRec = (OwnerGroup) enum.nextElement();

	    // the supergash ownergroup record already exists on the server, we
	    // don't need to create it here.

	    if (ogRec.prefix.equals("super"))
	      {
		continue;
	      }

	    System.out.print("Creating ownergroup:" + ogRec.prefix);

	    // create the owner group on the server

	    current_obj = (DBEditObject) createObject(SchemaConstants.OwnerBase);
	    invid = current_obj.getInvid();

	    // record the invid so that we can refer to this ownerbase later

	    if (invid == null)
	      {
		System.err.println("**** Complain!");
	      }

	    ogRec.setInvid(invid);

	    System.out.println(" [" + invid + "]");

 	    // and set the name of the owner group.. using the prefix mask isn't
	    // great, but it will serve for now.  The administrators will be able to
	    // edit them through the client

	    current_obj.setFieldValueLocal(SchemaConstants.OwnerNameField, ogRec.prefix);

	    // field 256 is the start range for user and group id's
	    // when we're using GASH compatibility..  GASH has separate
	    // ranges for uid's and gid's, but in practice they never
	    // differed..

	    current_obj.setFieldValueLocal((short) 256, new Integer(ogRec.lowuid));

	    if (ogRec.approval_address != null)
	      {
		current_field = current_obj.getField((short) 257); // approval list

		((DBField) current_field).addElementLocal(ogRec.approval_address);
	      }
	  }

	commitTransaction();

	System.out.println("\nRegistering user categories\n");

	my_client.session.openTransaction("GASH directLoader");
	registerUserCategories();
	commitTransaction();

	System.out.println("\nRegistering users\n");

	my_client.session.openTransaction("GASH directLoader");
	registerUsers();
	commitTransaction();

	my_client.session.openTransaction("GASH directLoader");
	registerEmail();
	commitTransaction();

	System.out.println("\nRegistering ARL Contracts\n");

	my_client.session.openTransaction("GASH directLoader");
	registerContracts();
	commitTransaction();

	System.out.println("\nRegistering groups\n");

	my_client.session.openTransaction("GASH directLoader");
	registerGroups();
	commitTransaction();

	System.out.println("\nRegistering user netgroups\n");

	my_client.session.openTransaction("GASH directLoader");
	registerUserNetgroups();
	commitTransaction();

	System.out.println("\nRegistering System Types\n");

	my_client.session.openTransaction("GASH directLoader");
	registerSystemTypes();
	commitTransaction();

	System.out.println("\nRegistering networks and rooms\n");

	my_client.session.openTransaction("GASH directLoader");
	registerNets();
	registerRooms();
	commitTransaction();

	System.out.println("\nRegistering systems\n");

	my_client.session.openTransaction("GASH directLoader");
	registerSystems();
	commitTransaction();

	System.out.println("\nRegistering system netgroups\n");

	my_client.session.openTransaction("GASH directLoader");
	registerSystemNetgroups();
	commitTransaction();

	System.out.println("\nRegistering automounter configuration\n");

	my_client.session.openTransaction("GASH directLoader");
	registerAutomounter();
	commitTransaction();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
    
    // now logout

    System.out.println("Logging out");

    try 
      {
	my_client.disconnect();
      }
    catch (RemoteException ex) 
      {
	System.err.println("Remote exception disconnecting: " + ex);
      }

    System.out.println("Dumping the database");

    my_server.dump();
      
    System.exit(0);
  }

  private static void commitTransaction() throws RemoteException
  {
    ReturnVal retVal = my_client.session.commitTransaction(true);

    if (retVal != null && !retVal.didSucceed())
      {
	throw new RuntimeException("Could not commit transaction, aborting loader");
      }
  }

  /*----------------------------------------------------------------------------*/

  //
  // the following methods handle the parsing of a particular GASH file
  //

  /*----------------------------------------------------------------------------*/


  /**
   *
   * scan the admin_info file, coalesce a group of owner groups
   *
   */

  private static void scanOwnerGroups()
  {
    // create an ownergroup record for supergash-level admins

    OwnerGroup supergash = new OwnerGroup();
    ownerGroups.addElement(supergash);

    try
      {
	inReader = new BufferedReader(new FileReader("input/admin_info"));
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find admin_info");
	done = true;
      }

    Admin adminObj;
    StreamTokenizer tokens = new StreamTokenizer(inReader);

    Admin.initTokenizer(tokens);

    System.out.println("Scanning admin_info");

    try
      {
	while (!done)
	  {
	    try
	      {
		adminObj = new Admin();
		done = adminObj.loadLine(tokens);

		if (adminObj.name != null)
		  {
		    if (adminObj.name.equals(Ganymede.rootname))
		      {
			System.err.println("Skipping over supergash");
			continue;
		      }

		    boolean found = false;
		    OwnerGroup ogRec = null;

		    // go through the existing owner group definitions, see if
		    // there is one that matches the new admin object.. if so,
		    // just remember that that user is a member of that owner
		    // group
		
		    for (int i = 0; !found && i < ownerGroups.size(); i++)
		      {
			ogRec = (OwnerGroup) ownerGroups.elementAt(i);
		    
			if (ogRec.compatible(adminObj))
			  {
			    ogRec.addAdmin(adminObj.name, adminObj.password, adminObj.code);
			    found = true;
			  }
		      }
		
		    if (!found)
		      {
			if (adminObj.mask != null && !adminObj.mask.equals("*"))
			  {
			    // create a new owner group
			
			    ogRec = new OwnerGroup(adminObj);
			    ownerGroups.addElement(ogRec);
			  }
			else	// the admin is a member of supergash
			  {
			    ogRec = supergash;
			  }

			ogRec.addAdmin(adminObj.name, adminObj.password, adminObj.code);
		      }
		
		    // System.out.println("Putting " + adminObj.name + " : " + ogRec);

		    adminUsers.put(adminObj.name, ogRec);

		    if (!done)
		      {
			System.out.print(".");
		      }
		  }
	      }
	    catch (EOFException ex)
	      {
		done = true;
	      }
	    catch (IOException ex)
	      {
		System.err.println("unknown IO exception caught: " + ex);
	      }
	  }
      }
    finally
      {
	try
	  {
	    inReader.close();
	  }
	catch (IOException ex)
	  {
	    System.err.println("unknown IO exception caught: " + ex);
	  }
      }

    System.err.println("\nOwner Groups identified:");
    
    for (int i = 0; i < ownerGroups.size(); i++)
      {
	System.err.println(ownerGroups.elementAt(i));
      }
  }

  /**
   *
   * Scan the GASH user_categories file, load the userCategories
   * hash.
   *
   */

  private static void scanUserCategories()
  {
    UserCategory categoryObj;

    /* -- */

    try
      {
	inReader = new BufferedReader(new FileReader("input/user_categories"));
	done = false;
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find user_categories");
	done = true;
      }

    StreamTokenizer tokens = new StreamTokenizer(inReader);

    UserCategory.initTokenizer(tokens);

    System.out.println("Scanning user_categories");

    try
      {
	while (!done)
	  {
	    try
	      {
		categoryObj = new UserCategory();
		done = categoryObj.loadLine(tokens);

		if (!done && categoryObj.valid)
		  {
		    userCategories.put(categoryObj.name, categoryObj);

		    if (debug)
		      {
			System.out.println("\n\n");
			categoryObj.display();
		      }
		    else
		      {
			System.out.print(".");
		      }
		  }
	      }
	    catch (EOFException ex)
	      {
		done = true;
	      }
	    catch (IOException ex)
	      {
		System.err.println("unknown IO exception caught: " + ex);
	      }
	  }

	System.out.println();
	System.out.println("Done scanning user_categories");
      }
    finally
      {
	try
	  {
	    inReader.close();
	  }
	catch (IOException ex)
	  {
	    System.err.println("unknown IO exception caught: " + ex);
	  }
      }    
  }

  /**
   *
   * This method scans the users from the GASH user_info file
   * and saves them in the users hash.
   *
   */

  private static void scanUsers()
  {
    User userObj;

    /* -- */

    try
      {
	inReader = new BufferedReader(new FileReader("input/user_info"));
	done = false;
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find user_info");
	done = true;
      }

    StreamTokenizer tokens = new StreamTokenizer(inReader);

    User.initTokenizer(tokens);

    System.out.println("Scanning user_info");

    try
      {
	while (!done)
	  {
	    try
	      {
		userObj = new User();
		done = userObj.loadLine(tokens);

		if (!done && userObj.valid)
		  {
		    users.put(userObj.name, userObj);

		    if (debug)
		      {
			System.out.println("\n\n");
			userObj.display();
		      }
		    else
		      {
			System.out.print(".");
		      }
		  }
	      }
	    catch (EOFException ex)
	      {
		done = true;
	      }
	    catch (IOException ex)
	      {
		System.err.println("unknown IO exception caught: " + ex);
	      }
	  }

	System.out.println();
	System.out.println("Done scanning user_info");
      }
    finally
      {
	try
	  {
	    inReader.close();
	  }
	catch (IOException ex)
	  {
	    System.err.println("unknown IO exception caught: " + ex);
	  }
      }
  }

  private static void scanEmail()
  {
    // now process the aliases_info file
    
    BufferedReader in = null;
    String line;

    try
      {
	in = new BufferedReader(new FileReader("input/aliases_info"));
	done = false;
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find aliases_info");
	done = true;
      }

    System.out.println("Scanning aliases_info");

    while (!done)
      {
	try
	  {
	    line = in.readLine();

	    if (line == null)
	      {
		done = true;
	      }
	    else
	      {
		System.err.print(".");

		switch (line.charAt(0))
		  {
		  case '<':
		    ExternMail z = new ExternMail(line);
		    
		    externalAliases.addElement(z);
		    
		    // System.err.println(z);
		    break;
		    
		  case ':':
		    MailGroup y = new MailGroup(line);
		    
		    mailGroups.addElement(y);
		    
		    // System.err.println(y);
		    break;
		    
		  default:
		    UserMail x = new UserMail(line);
		    
		    userMail.put(x.userName, x);
		    
		    // System.err.println(x);
		    break;
		  }
	      }
	  }
	catch (EOFException ex)
	  {
	    done = true;
	  }
	catch (IOException ex)
	  {
	    System.err.println("unknown IO exception caught: " + ex);
	  }
      }

    System.err.println();
    System.out.println("Done scanning aliases_info");

    try
      {
	in.close();
      }
    catch (IOException ex)
      {
	System.err.println("unknown IO exception caught: " + ex);
      }
  }

  /**
   *
   */

  private static void scanContracts()
  {
    String line;

    /* -- */

    // first, load up contract info

    try
      {
	try
	  {
	    inReader = new BufferedReader(new FileReader("input/arlcontracts.txt"));
	  }
	catch (FileNotFoundException ex)
	  {
	    System.err.println("Couldn't find activecontracts.txt");
	    return;
	  }

	for (line = inReader.readLine(); line != null; line = inReader.readLine())
	  {
	    Contract x = new Contract(line);
	    contracts.put(x.number, x);
	  }
      }
    catch (IOException ex)
      {
      }
    finally
      {
	if (inReader != null)
	  {
	    try
	      {
		inReader.close();
	      }
	    catch (IOException ex)
	      {
	      }
	  }
      }

    // second, load up group<->contract mapping info

    try
      {
	try
	  {
	    inReader = new BufferedReader(new FileReader("input/table.in.dat"));
	  }
	catch (FileNotFoundException ex)
	  {
	    System.err.println("Couldn't find table.in.dat");
	    return;
	  }

	for (line = inReader.readLine(); line != null; line = inReader.readLine())
	  {
	    // skip comment lines

	    if (line.startsWith("CC"))
	      {
		continue;
	      }

	    String groupName = line.substring(4,10).toLowerCase();
	    String contractNumber = line.substring(65,72);

	    groupContracts.put(groupName, contractNumber);
	  }
      }
    catch (IOException ex)
      {
      }
    finally
      {
	if (inReader != null)
	  {
	    try
	      {
		inReader.close();
	      }
	    catch (IOException ex)
	      {
	      }
	  }
      }

  }

  /**
   *
   */

  private static void scanGroups()
  {
    // and the group file

    done = false;

    try
      {
	inReader = new BufferedReader(new FileReader("input/group_info"));
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find group_info");
	done = true;
      }

    StreamTokenizer tokens = new StreamTokenizer(inReader);

    Group.initTokenizer(tokens);

    System.out.println("Scanning group_info");

    try
      {
	while (!done)
	  {
	    try
	      {
		groupObj = new Group();
		done = groupObj.loadLine(tokens);

		if (!done && groupObj.valid)
		  {
		    groups.put(groupObj.name, groupObj);
		    groupID.put(new Integer(groupObj.gid), groupObj);

		    if (debug)
		      {
			System.out.println("\n\n");
			groupObj.display();
		      }
		    else
		      {
			System.out.print(".");
		      }
		  }
	      }
	    catch (EOFException ex)
	      {
		done = true;
	      }
	    catch (IOException ex)
	      {
		System.err.println("unknown IO exception caught: " + ex);
	      }
	  }
      }
    finally
      {
	try
	  {
	    inReader.close();
	  }
	catch (IOException ex)
	  {
	    System.err.println("unknown IO exception caught: " + ex);
	  }
      }

    System.out.println();
    System.out.println("Done scanning group_info");
  }

  /**
   *
   */

  private static void scanUserNetgroups()
  {
    done = false;

    try
      {
	inReader = new BufferedReader(new FileReader("input/netgroup"));
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find netgroup");
	done = true;
      }

    StreamTokenizer tokens = new StreamTokenizer(inReader);

    UserNetgroup.initTokenizer(tokens);

    System.out.println("Scanning netgroup for user entries");

    try
      {
	while (!done)
	  {
	    try
	      {
		uNetObj = new UserNetgroup();
		done = uNetObj.loadLine(tokens);

		//	    uNetObj.display();

		if (!done)
		  {
		    System.out.print(".");
		    userNetgroup.put(uNetObj.netgroup_name, uNetObj);
		  }
	      }
	    catch (EOFException ex)
	      {
		done = true;
	      }
	    catch (IOException ex)
	      {
		System.err.println("unknown IO exception caught: " + ex);
	      }
	  }
      }
    finally
      {
	try
	  {
	    inReader.close();
	  }
	catch (IOException ex)
	  {
	    System.err.println("unknown IO exception caught: " + ex);
	  }
      }

    System.out.println();
    System.out.println("Done scanning netgroup for user objects");
  }

  /**
   *
   */

  private static void scanSystemTypes()
  {
    System.out.println("\nScanning System Types\n");

    done = false;

    try
      {
	inReader = new BufferedReader(new FileReader("input/internet_assignment"));
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find system types file");
	done = true;
      }

    StreamTokenizer tokens = new StreamTokenizer(inReader);

    SystemType.initTokenizer(tokens);

    SystemType st;

    try
      {
	while (!done)
	  {
	    try
	      {
		st = new SystemType();
		done = st.loadLine(tokens);

		if (!done)
		  {
		    st.display();
		    systemTypes.put(st.name, st);
		  }
	      }
	    catch (EOFException ex)
	      {
		done = true;
	      }
	    catch (IOException ex)
	      {
		System.err.println("unknown IO exception caught: " + ex);
	      }
	  }
      }
    finally
      {
	try
	  {
	    inReader.close();
	  }
	catch (IOException ex)
	  {
	    System.err.println("unknown IO exception caught: " + ex);
	  }
      }

    System.out.println();
    System.out.println("Done scanning internet_assignment file for system type objects");
  }

  /**
   *
   */

  private static void scanRooms()
  {
    System.out.println("\nScanning Rooms\n");

    done = false;

    try
      {
	inReader = new BufferedReader(new FileReader("input/networks_by_room.cpp"));
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find system types file");
	done = true;
      }

    StreamTokenizer tokens = new StreamTokenizer(inReader);

    Room.initTokenizer(tokens);

    String ipNetName;
    Room room;

    try
      {
	while (!done)
	  {
	    try
	      {
		room = new Room();
		done = room.loadLine(tokens);
	    
		// if we got a room ok, show what it looks like and register
		// the room and the network info for that room in our
		// datastructures.

		if (room.loaded)
		  {
		    if (debug)
		      {
			room.display();
		      }

		    if (rooms.containsKey(room.name))
		      {
			System.err.println("** Error, multiple room definitions for " + room.name);
			continue;
		      }

		    rooms.put(room.name, room);

		    for (int i = 0; i < room.nets.size(); i++)
		      {
			ipNetName = (String) room.nets.elementAt(i);

			if (!networks.containsKey(ipNetName))
			  {
			    networks.put(ipNetName, ipNetName);
			  }
		      }
		  }
	      }
	    catch (EOFException ex)
	      {
		done = true;
	      }
	    catch (IOException ex)
	      {
		System.err.println("unknown IO exception caught: " + ex);
	      }
	  }
      }
    finally
      {
	try
	  {
	    inReader.close();
	  }
	catch (IOException ex)
	  {
	    System.err.println("unknown IO exception caught: " + ex);
	  }
      }

    System.out.println();
    System.out.println("Done scanning rooms for networks");
  }

  /**
   *
   */

  private static void scanSystems()
  {
    System.out.println("\nScanning Systems\n");

    try
      {
	sysLoader = new SystemLoader("input/hosts_info");
      }
    catch (IOException ex)
      {
	throw new RuntimeException("io exception in systemLoader constructor: " + ex);
      }
  }

  /**
   *
   */

  private static void scanSystemNetgroups()
  {
    done = false;

    try
      {
	inReader = new BufferedReader(new FileReader("input/netgroup"));
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find netgroup");
	done = true;
      }

    StreamTokenizer tokens = new StreamTokenizer(inReader);

    SystemNetgroup.initTokenizer(tokens);

    System.out.println("\nScanning netgroup for system entries\n");

    try
      {
	while (!done)
	  {
	    try
	      {
		sNetObj = new SystemNetgroup();
		done = sNetObj.loadLine(tokens);

		//	    sNetObj.display();
		//	    System.out.println();

		if (!done)
		  {
		    System.out.print(".");
		    systemNetgroup.put(sNetObj.netgroup_name, sNetObj);
		  }
	      }
	    catch (EOFException ex)
	      {
		done = true;
	      }
	    catch (IOException ex)
	      {
		System.err.println("unknown IO exception caught: " + ex);
	      }
	  }
      }
    finally
      {
	try
	  {
	    inReader.close();
	  }
	catch (IOException ex)
	  {
	    System.err.println("unknown IO exception caught: " + ex);
	  }
      }

    System.out.println();
    System.out.println("Done scanning netgroup for system objects");
  }

  /**
   *
   */

  private static void scanAutomounter()
  {
    BufferedReader in = null;
    String line;
    Volume v;
    MapEntry m;

    /* -- */

    try
      {
	in = new BufferedReader(new FileReader("input/auto.vol"));
	done = false;
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find input/auto.vol");
	done = true;
      }

    System.out.println("Scanning input/auto.vol");

    while (!done)
      {
	try
	  {
	    line = in.readLine();

	    if (line == null)
	      {
		done = true;
	      }
	    else
	      {
		v = new Volume(line);
		volumes.put(v.volumeName, v);

		System.err.println("Created: " + v);
	      }
	  }
	catch (EOFException ex)
	  {
	    done = true;
	  }
	catch (IOException ex)
	  {
	    System.err.println("unknown IO exception caught: " + ex);
	  }
      }

    System.out.println("\nDone scanning auto.vol");

    System.out.println("\nScanning directory for auto.home.* files\n");

    File dir = new File("input");

    if ((dir == null) || !dir.isDirectory())
      {
	throw new RuntimeException("Couldn't open input directory");
      }

    String[] sAry = dir.list(new FilenameFilter()
			     {
			       public boolean accept(File dir,
						     String name)
				 {
				   return (name.startsWith("auto.home."));
				 }
			     }
			     );

    for (int i = 0; i < sAry.length; i++)
      {
	System.out.println("Scanning " + sAry[i]);

	maps.addElement(sAry[i]);

	try
	  {
	    in = new BufferedReader(new FileReader("input/" + sAry[i]));
	    done = false;
	  }
	catch (FileNotFoundException ex)
	  {
	    System.err.println("Couldn't open input/" + sAry[i]);
	    done = true;
	  }

	while (!done)
	  {
	    try
	      {
		line = in.readLine();

		if (line == null)
		  {
		    done = true;
		  }
		else
		  {
		    m = new MapEntry(sAry[i], line);
		    mapEntries.addElement(m);

		    System.err.println("Created: " + m);
		  }
	      }
	    catch (EOFException ex)
	      {
		done = true;
	      }
	    catch (IOException ex)
	      {
		System.err.println("unknown IO exception caught: " + ex);
	      }
	  }

	System.out.println("\nDone scanning " + sAry[i]);
      }
  }

  /**
   *
   */

  private static void registerUserNetgroups() throws RemoteException
  {
    String key;
    Invid invid, objInvid;
    Enumeration enum;
    OwnerGroup ogRec;
    DBEditObject current_obj;
    db_field current_field;
    Hashtable netHash = new Hashtable();
    ReturnVal retVal;

    /* -- */

    enum = userNetgroup.keys();
    
    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	uNetObj = (UserNetgroup) userNetgroup.get(key);

	System.out.print("Creating " + key);

	current_obj = (DBEditObject) createObject((short) 270);	// base 270 is for user netgroups
	objInvid = current_obj.getInvid();

	System.out.println(" [" + objInvid + "]");

	current_obj.setFieldValueLocal(userNetgroupSchema.NETGROUPNAME, key); // netgroup name
	netHash.put(key, objInvid);

	// now we have to put the users in

	current_field = current_obj.getField(userNetgroupSchema.USERS); // users

	Vector users = uNetObj.users;
	String username;

	for (int i = 0; i < users.size(); i++)
	  {
	    username = (String) users.elementAt(i);

	    if (username.equals("-"))
	      {
		// empty marker

		continue;
	      }

	    if (username != null)
	      {
		invid = (Invid) userInvids.get(username);

		if (invid != null)
		  {
		    System.err.println("Attempting to add " + username + ", [" + invid.toString()+"]");
		    ((DBField) current_field).addElementLocal(invid);
		  }
		else
		  {
		    System.err.println("null invid");
		  }
	      }
	    else
	      {
		System.err.println("null username");
	      }
	  }

	// loop through our owner groups, add this netgroup to the appropriate owner groups

	for (int i = 0; i < ownerGroups.size(); i++)
	  {
	    ogRec = (OwnerGroup) ownerGroups.elementAt(i);

	    if (ogRec.matchMask(key))
	      {
		db_object ownerGroup = editObject(ogRec.getInvid());
		db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		((DBField) f).addElementLocal(objInvid); // add this group
		System.out.print(" " + ogRec.prefix);
	      }
	  }

	System.out.println();
      }

    // register user netgroups in the server, part 2.. the sub netgroups

    enum = userNetgroup.keys();
	
    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	uNetObj = (UserNetgroup) userNetgroup.get(key);

	objInvid = (Invid) netHash.get(key);
	current_obj = (DBEditObject) editObject(objInvid);

	// we have to put the sub netgroups in

	current_field = current_obj.getField(userNetgroupSchema.MEMBERGROUPS); // member netgroups

	Vector subnets = uNetObj.subnetgroups;
	String subName;
	boolean ok = false;

	if (subnets.size() > 0)
	  {
	    System.out.print("Loading subnetgroups for " + key);
	    System.out.println(" [" + objInvid + "]");
	  }

	for (int i = 0; i < subnets.size(); i++)
	  {
	    subName = (String) subnets.elementAt(i);

	    if (subName != null && !subName.equals(""))
	      {
		invid = (Invid) netHash.get(subName);

		if (invid != null)
		  {
		    System.err.println("Attempting to add " + subName + ", [" + invid.toString()+"]");

		    if (!ok)
		      {
			retVal = ((DBField) current_field).addElementLocal(invid);

			ok = (retVal == null || retVal.didSucceed());
		      }
		  }
		else
		  {
		    System.err.println(key + ": *** null invid attempting to add " + subName);
		  }
	      }
	    else
	      {
		System.err.println("null subnet name");
	      }
	  }
	
	if (!ok && subnets.size()>0)
	  {
	    System.err.println("Not successful while processing subnetgroups for " + key);
	  }
      }
  }

  /**
   *
   * This method creates User Category objects in the server, and 
   * records a name-to-invid mapping in the categoryInvids hash.
   *
   */

  private static void registerUserCategories() throws RemoteException
  {
    String key;
    Enumeration enum;
    UserCategory category;
    DBEditObject obj;
    Invid invid;
    ReturnVal result;

    /* -- */

    System.err.println("\nCreating user categories in server: ");

    enum = userCategories.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	category = (UserCategory) userCategories.get(key);

	System.out.print("Creating " + key);

	obj = (DBEditObject) createObject((short) 279);
	invid = obj.getInvid();

	System.out.println(" [" + invid + "] ");

	// save the invid in our categoryInvids hash for later
	// reference

	categoryInvids.put(category.name, invid);

	result = obj.setFieldValueLocal(userCategorySchema.TITLE, category.name);

	if (result != null && !result.didSucceed())
	  {
	    System.err.println("Failure setting category name for " + category.name);
	  }

	result = obj.setFieldValueLocal(userCategorySchema.SUMMARY, category.shortdescrip);

	if (result != null && !result.didSucceed())
	  {
	    System.err.println("Failure setting category summary for " + category.name);
	  }

	result = obj.setFieldValueLocal(userCategorySchema.DESCRIPTION, category.longdescrip);

	if (result != null && !result.didSucceed())
	  {
	    System.err.println("Failure setting category description for " + category.name);
	  }

	if (category.mailvec != null && category.mailvec.size() != 0)
	  {
	    obj.setFieldValueLocal(userCategorySchema.APPROVALREQ, new Boolean(true));

	    DBField temp = (DBField) obj.getField(userCategorySchema.APPROVALLIST);

	    for (int i = 0; i < category.mailvec.size(); i++)
	      {
		result = temp.addElementLocal(category.mailvec.elementAt(i));

		if (result != null && !result.didSucceed())
		  {
		    System.err.println("Failure adding mail address to category maillist for " + category.name);
		  }
	      }
	  }
	
	if (category.days_limit != 0)
	  {
	    obj.setFieldValueLocal(userCategorySchema.EXPIRE, new Boolean(true));
	    result = obj.setFieldValueLocal(userCategorySchema.LIMIT, new Integer(category.days_limit));

	    if (result != null && !result.didSucceed())
	      {
		System.err.println("Failure setting category expiration for " + category.name);
	      }
	  }

	if (category.ssrequired)
	  {
	    obj.setFieldValueLocal(userCategorySchema.SSREQUIRED, new Boolean(true));
	  }
      }

    System.err.println("\nFinished creating user categories in server");
  }

  /**
   *
   */

  private static void registerUsers() throws RemoteException
  {
    String key;
    Invid invid, objInvid;
    Enumeration enum;
    OwnerGroup ogRec;
    DBEditObject current_obj;
    db_field current_field;
    pass_field p_field;
    User userObj;
    Invid categoryInvid;
    Date now = new Date();
    Calendar cal = Calendar.getInstance();

    /* -- */

    System.err.println("\nCreating users in server: ");

    enum = users.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	userObj = (User) users.get(key);

	System.out.print("Creating " + key);

	current_obj = (DBEditObject) createObject(SchemaConstants.UserBase);
	invid = current_obj.getInvid();

	System.out.print(" [" + invid + "] ");

	userInvids.put(userObj.name, invid);

	// set the username
	    
	current_obj.setFieldValueLocal(SchemaConstants.UserUserName, key);

	// set the UID

	current_obj.setFieldValueLocal(userSchema.UID, new Integer(userObj.uid));

	// set the password

	p_field = (pass_field) current_obj.getField(SchemaConstants.UserPassword);
	p_field.setCryptPass(userObj.password);

	// set the fullname

	current_obj.setFieldValueLocal(userSchema.FULLNAME, userObj.fullname);

	// set the division

	current_obj.setFieldValueLocal(userSchema.DIVISION, userObj.division);

	// set the room

	current_obj.setFieldValueLocal(userSchema.ROOM, userObj.room);

	// set the office phone

	current_obj.setFieldValueLocal(userSchema.OFFICEPHONE, userObj.officePhone);

	// set the home phone

	current_obj.setFieldValueLocal(userSchema.HOMEPHONE, userObj.homePhone);

	// set the home directory

	current_obj.setFieldValueLocal(userSchema.HOMEDIR, userObj.directory);

	// set the shell

	current_obj.setFieldValueLocal(userSchema.LOGINSHELL, userObj.shell);

	// set the user category

	if (userObj.category != null && !userObj.category.equals(""))
	  {
	    categoryInvid = (Invid) categoryInvids.get(userObj.category);

	    if (categoryInvid != null)
	      {
		current_obj.setFieldValueLocal(userSchema.CATEGORY, categoryInvid);
	      }
	    else
	      {
		System.err.println("Null category for user " + key);
	      }
	  }
	else
	  {
	    System.err.println("\nNo user category registered for user " + key + ", defaulting to normal.");

	    categoryInvid = (Invid) categoryInvids.get("normal");

	    if (categoryInvid != null)
	      {
		current_obj.setFieldValueLocal(userSchema.CATEGORY, categoryInvid);
	      }
	    else
	      {
		System.err.println("Null category for user " + key);
	      }
	  }

	// if we have an expiration date, set the expiration or
	// removal date, as appropriate.

	if (userObj.expirationDate != null)
	  {
	    if (userObj.expirationDate.before(now))
	      {
		// the expiration time has already passed.. set the
		// removal date (x + 3 months) instead

		cal.setTime(userObj.expirationDate);
		cal.add(Calendar.MONTH, 3);

		current_obj.setFieldValueLocal(SchemaConstants.RemovalField, cal.getTime());
	      }
	    else
	      {
		current_obj.setFieldValueLocal(SchemaConstants.ExpirationField, userObj.expirationDate);
	      }
	  }

	// set the social security #

	current_obj.setFieldValueLocal(userSchema.SOCIALSECURITY, userObj.socialsecurity);

	// register email aliases for this user

	UserMail aliasInfo = (UserMail) userMail.get(key);

	if (aliasInfo != null)
	  {
	    db_field sf = current_obj.getField(userSchema.ALIASES);

	    String tmpStr;

	    for (int i = 0; i < aliasInfo.aliases.size(); i++)
	      {
		tmpStr = (String) aliasInfo.aliases.elementAt(i);

		if (!tmpStr.equals(key))
		  {
		    sf.addElement(aliasInfo.aliases.elementAt(i));
		  }
	      }

	    // set the signature alias

	    current_obj.setFieldValueLocal(userSchema.SIGNATURE, aliasInfo.aliases.elementAt(0));

	    // set the email targets

	    sf = current_obj.getField(userSchema.EMAILTARGET);

	    // the setValue logic for the user object will have created
	    // a default email target for us.. we don't want it.

	    ReturnVal retVal;

	    while (sf.size() != 0)
	      {
		retVal = sf.deleteElement(0);

		if (retVal != null && !retVal.didSucceed())
		  {
		    break;	// oh, well.
		  }
	      }

	    for (int i = 0; i < aliasInfo.targets.size(); i++)
	      {
		sf.addElement(aliasInfo.targets.elementAt(i));
	      }
	  }

	// loop through our owner groups, add this user to the appropriate owner groups

	for (int i = 0; i < ownerGroups.size(); i++)
	  {
	    ogRec = (OwnerGroup) ownerGroups.elementAt(i);

	    if (ogRec.matchUID(userObj.uid))
	      {
		db_object ownerGroup = editObject(ogRec.getInvid());
		db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		((DBField) f).addElementLocal(invid); // add this user
		System.out.print(" " + ogRec.prefix);
	      }
	  }

	System.out.println();

	// now, is this user an admin?

	ogRec = (OwnerGroup) adminUsers.get(key);

	if (ogRec != null)
	  {
	    if (ogRec.prefix.equals("super"))
	      {
		System.out.print("User " + key + " is a SuperGASH admin.. creating persona object ");
	      }
	    else
	      {
		System.out.print("User " + key + " is a GASH admin.. creating persona object ");
	      }

	    DBEditObject newPersona = (DBEditObject) createObject(SchemaConstants.PersonaBase);
	    Invid personaInvid = newPersona.getInvid();

	    System.out.println("[" + personaInvid + "]");

	    newPersona.setFieldValueLocal(SchemaConstants.PersonaAssocUser, invid);

	    newPersona.setFieldValueLocal(SchemaConstants.PersonaNameField, "GASH Admin");

	    pass_field passField = (pass_field) newPersona.getField(SchemaConstants.PersonaPasswordField);
	    passField.setCryptPass(ogRec.password(key));

	    newPersona.setFieldValueLocal(SchemaConstants.PersonaAdminConsole, new Boolean(true));

	    newPersona.setFieldValueLocal(SchemaConstants.PersonaAdminPower, new Boolean(false));

	    // with GASH compatibility mode, we store the admin's 3 char prefix code

	    newPersona.setFieldValueLocal((short) 256, ogRec.code(key));

	    db_field personaField = newPersona.getField(SchemaConstants.PersonaGroupsField);
	    ((DBField) personaField).addElementLocal(ogRec.getInvid());

	    // this is a GASH admininstrator, so give it the GASH perm matrix

	    personaField = newPersona.getField(SchemaConstants.PersonaPrivs);
	    ((DBField)personaField).addElementLocal(gashadminPermInvid);
	  }
      }
  }

  /**
   *
   */

  private static void registerEmail() throws RemoteException
  {
    
    ExternMail mailRec;
    MailGroup mailGroup;
    DBEditObject current_obj;
    db_field current_field;
    Invid objInvid, targetInvid;
    OwnerGroup ogRec;
    String temp;

    /* -- */

    System.err.println("Registering email objects");

    // register external email references

    System.err.println("External References");

    for (int i = 0; i < externalAliases.size(); i++)
      {
	mailRec = (ExternMail) externalAliases.elementAt(i);

	// base 275 is for Email Redirect objects

	current_obj = (DBEditObject) createObject((short) 275);
	objInvid = current_obj.getInvid();

	// remember that we have this external record as a possible target for
	// other things

	mailInvids.put(mailRec.externalName.toLowerCase(), objInvid);

	current_obj.setFieldValueLocal(emailRedirectSchema.NAME, mailRec.externalName);

	// set targets

	current_field = current_obj.getField(emailRedirectSchema.TARGETS);

	for (int j = 0; j < mailRec.targets.size(); j++)
	  {
	    current_field.addElement(mailRec.targets.elementAt(j));
	  }

	// set aliases

	current_field = current_obj.getField(emailRedirectSchema.ALIASES);

	for (int j = 0; j < mailRec.aliases.size(); j++)
	  {
	    String tmp = (String) mailRec.aliases.elementAt(j);

	    // we don't care about signature aliases for
	    // external references, so just leave out the
	    // label for the user in the aliases list
	    
	    if (!tmp.equals(mailRec.externalName))
	      {
		current_field.addElement(tmp);
	      }
	  }

	// loop through our owner groups, add this external mail rec to the appropriate owner groups

	for (int j = 0; j < ownerGroups.size(); j++)
	  {
	    ogRec = (OwnerGroup) ownerGroups.elementAt(j);

	    if (ogRec.matchMask(mailRec.ownerCode))
	      {
		db_object ownerGroup = editObject(ogRec.getInvid());
		db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		((DBField) f).addElementLocal(objInvid); // add this group
		System.out.print(" " + ogRec.prefix);
	      }
	  }
      }

    // register email groups

    // step 1.. create all the groups, hash the names to invids

    System.err.println("Mail Lists.. step 1");

    for (int i = 0; i < mailGroups.size(); i++)
      {
	mailGroup = (MailGroup) mailGroups.elementAt(i);

	// base 274 is for Email List objects

	current_obj = (DBEditObject) createObject((short) 274);
	objInvid = current_obj.getInvid();

	mailInvids.put(mailGroup.listName.toLowerCase(), objInvid);

	current_obj.setFieldValueLocal(emailListSchema.LISTNAME, mailGroup.listName);

	// set external targets

	current_field = current_obj.getField(emailListSchema.EXTERNALTARGETS);

	for (int j = 0; j < mailGroup.targets.size(); j++)
	  {
	    temp = (String) mailGroup.targets.elementAt(j);

	    if ((temp.indexOf('@') != -1) || (temp.indexOf('/') != -1))
	      {
		current_field.addElement(mailGroup.targets.elementAt(j));
	      }
	  }

	// loop through our owner groups, add this mail list to the appropriate owner groups

	for (int j = 0; j < ownerGroups.size(); j++)
	  {
	    ogRec = (OwnerGroup) ownerGroups.elementAt(j);

	    if (ogRec.matchMask(mailGroup.ownerCode))
	      {
		db_object ownerGroup = editObject(ogRec.getInvid());
		db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		((DBField) f).addElementLocal(objInvid); // add this group
		System.out.print(" " + ogRec.prefix);
	      }
	  }
      }

    // okay, now we can loop through and set the internal targets

    System.err.println("Mail Lists.. step 2");

    for (int i = 0; i < mailGroups.size(); i++)
      {
	mailGroup = (MailGroup) mailGroups.elementAt(i);

	current_obj = (DBEditObject) editObject((Invid) mailInvids.get(mailGroup.listName.toLowerCase()));

	// set internal targets

	current_field = current_obj.getField(emailListSchema.MEMBERS);

	for (int j = 0; j < mailGroup.targets.size(); j++)
	  {
	    temp = (String) mailGroup.targets.elementAt(j);

	    if ((temp.indexOf('@') == -1) && (temp.indexOf('/') == -1))
	      {
		targetInvid = (Invid) mailInvids.get(temp.toLowerCase());

		if (targetInvid != null)
		  {
		    ((DBField) current_field).addElementLocal(targetInvid);
		  }
		else
		  {
		    targetInvid = (Invid) userInvids.get(temp.toLowerCase());

		    if (targetInvid != null)
		      {
			((DBField) current_field).addElementLocal(targetInvid);
		      }
		    else
		      {
			System.err.println("Can't find email target reference for " + temp);
		      }
		  }
	      }
	  }
      }

  }

  /**
   *
   */

  private static void registerContracts() throws RemoteException
  {
    String key;
    Contract contract;
    DBEditObject current_obj;
    Enumeration enum;

    /* -- */

    enum = contracts.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	contract = (Contract) contracts.get(key);

	System.out.print("Creating contract " + contract.name);

	// base 280 is for ARL contracts

	current_obj = (DBEditObject) createObject((short) 280);
	contract.invid = current_obj.getInvid();

	System.out.println(" [" + contract.invid + "]");

	current_obj.setFieldValueLocal((short) 256, contract.name);
	current_obj.setFieldValueLocal((short) 257, contract.number);
	current_obj.setFieldValueLocal((short) 258, contract.startDate);
	current_obj.setFieldValueLocal((short) 259, contract.stopDate);
	current_obj.setFieldValueLocal((short) 260, contract.description);
	current_obj.setFieldValueLocal((short) 262, contract.status);
	current_obj.setFieldValueLocal((short) 263, contract.group);
	current_obj.setFieldValueLocal((short) 264, contract.researchUnit);
	current_obj.setFieldValueLocal((short) 265, contract.UTaccount);
      }
  }

  /**
   *
   */

  private static void registerGroups() throws RemoteException
  {
    String key;
    Invid invid, objInvid;
    Enumeration enum;
    OwnerGroup ogRec;
    DBEditObject current_obj;
    db_field current_field, current_field2;
    User userObj;

    /* -- */

    enum = groups.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	groupObj = (Group) groups.get(key);
	
	System.out.print("Creating " + key);

	// base 257 is for groups

	current_obj = (DBEditObject) createObject((short) 257);
	objInvid = current_obj.getInvid();
	
	System.out.println(" [" + objInvid + "]");

	groupInvid.put(groupObj.name, objInvid);

	// set the group name
	    
	current_obj.setFieldValueLocal(groupSchema.GROUPNAME, key);

	// set the GID

	current_obj.setFieldValueLocal(groupSchema.GID, new Integer(groupObj.gid));

	// set the password

	current_obj.setFieldValueLocal(groupSchema.PASSWORD, groupObj.password);

	// set the description

	current_obj.setFieldValueLocal(groupSchema.DESCRIPTION, groupObj.description);

	// set the contract info
	
	try
	  {
	    String contractNumber = (String) groupContracts.get(key);
	    Contract contractObj = (Contract) contracts.get(contractNumber);

	    current_obj.setFieldValueLocal(groupSchema.CONTRACTLINK, contractObj.invid);
	  }
	catch (NullPointerException ex)
	  {
	    System.err.println("Couldn't register contract link for group " + key);

	    // oh well, we don't recognize a contract object.. just provide a string

	    current_obj.setFieldValueLocal(groupSchema.CONTRACT, groupObj.contract);
	  }

	// add users

	current_field = current_obj.getField(groupSchema.USERS);

	String username;

	for (int i = 0; i < groupObj.users.size(); i++)
	  {
	    username = (String) groupObj.users.elementAt(i);

	    if (username != null)
	      {
		invid = (Invid) userInvids.get(username);

		if (invid != null)
		  {
		    System.err.println("\tAdd " + username + ", [" + invid.toString()+"]");
		    ((DBField) current_field).addElementLocal(invid);
		  }
		else
		  {
		    System.err.println("null invid");
		  }

		// does this user have this group specified as his/her home group?
		// if so, add him/her to the home users list as well.

		userObj = (User) users.get(username);

		if (userObj.gid == groupObj.gid)
		  {
		    System.err.println("\t-- home group add " + username);
		    current_field2 = current_obj.getField(groupSchema.HOMEUSERS);
		    ((DBField) current_field2).addElementLocal(invid);
		  }
	      }
	    else
	      {
		System.err.println("null username");
	      }
	  }

	// loop through our owner groups, add this group to the appropriate owner groups

	for (int i = 0; i < ownerGroups.size(); i++)
	  {
	    ogRec = (OwnerGroup) ownerGroups.elementAt(i);

	    if (ogRec.matchGID(groupObj.gid))
	      {
		db_object ownerGroup = editObject(ogRec.getInvid());
		db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		((DBField) f).addElementLocal(objInvid); // add this group
		System.out.print(" " + ogRec.prefix);
	      }
	  }

	System.out.println();
      }
  }

  /**
   *
   */

  private static void registerSystemTypes() throws RemoteException
  {
    String key;
    Invid invid, objInvid;
    Enumeration enum;
    SystemType st;
    DBEditObject current_obj;
    db_field current_field;

    /* -- */

    enum = systemTypes.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	st = (SystemType) systemTypes.get(key);
	
	System.out.print("Creating " + key);
	
	// base 272 is for system types

	current_obj = (DBEditObject) createObject((short) 272);
	objInvid = current_obj.getInvid();
	
	System.out.print(" [" + objInvid + "]");

	systemTypeInvids.put(st.name, objInvid);

	// set the system type name
	    
	current_obj.setFieldValueLocal(systemTypeSchema.SYSTEMTYPE, key);

	// set the start mark

	current_obj.setFieldValueLocal(systemTypeSchema.STARTIP, new Integer(st.start));

	// set the end mark

	current_obj.setFieldValueLocal(systemTypeSchema.STOPIP, new Integer(st.end));

	// set the associated user required flag

	current_obj.setFieldValueLocal(systemTypeSchema.USERREQ, new Boolean(st.requireUser));
      }
  }

  /**
   *
   */

  private static void registerNets() throws RemoteException
  {
    String name;
    Invid invid, objInvid;
    Enumeration enum;
    DBEditObject current_obj;
    db_field current_field;
    ReturnVal retVal;

    /* -- */

    enum = networks.keys();

    while (enum.hasMoreElements())
      {
	name = (String) enum.nextElement();
	
	System.out.print("Creating net: " + name);

	// base 267 is for networks
	
	current_obj = (DBEditObject) createObject((short) 267);
	objInvid = current_obj.getInvid();
	
	System.out.println(" [" + objInvid + "]");

	networkInvids.put(name, objInvid);

	// set the network name.. just use the net number by default
	    
	current_obj.setFieldValueLocal(networkSchema.NAME, name);
	current_obj.setFieldValueLocal(networkSchema.IPV6OK, new Boolean(false));

	IPDBField ipfield = (IPDBField) current_obj.getField(networkSchema.NETNUMBER);

	retVal = ipfield.setValueLocal(name);

	if (retVal != null && !retVal.didSucceed())
	  {
	    System.err.println("*** Error, couldn't register " + name);
	  }
      }
  }

  /**
   *
   */

  private static void registerRooms() throws RemoteException
  {
    String name;
    Room room;
    String netName;
    Invid netInvid, objInvid;
    Enumeration enum;
    DBEditObject current_obj;
    db_field current_field;
    InvidDBField networksField;
    ReturnVal retVal;

    /* -- */

    enum = rooms.keys();

    while (enum.hasMoreElements())
      {
	name = (String) enum.nextElement();
	room = (Room) rooms.get(name);
	
	System.out.print("Creating room: " + name);

	// base 269 is for rooms
	
	current_obj = (DBEditObject) createObject((short) 269);
	objInvid = current_obj.getInvid();
	
	System.out.println(" [" + objInvid + "]");

	roomInvids.put(name, objInvid);

	// set the network name.. just use the net number by default
	    
	current_obj.setFieldValueLocal(roomSchema.ROOMNUMBER, name);

	networksField = (InvidDBField) current_obj.getField(roomSchema.NETWORKS);

	Vector networks = room.nets;

	for (int i = 0; i < networks.size(); i++)
	  {
	    netName = (String) networks.elementAt(i);
	    netInvid = (Invid) networkInvids.get(netName);

	    if (netInvid != null)
	      {
		networksField.addElementLocal(netInvid);
	      }
	  }
      }
  }

  /**
   *
   */

  private static void registerSystems() throws RemoteException
  {
    String key;
    Invid invid, objInvid;
    Enumeration enum;
    OwnerGroup ogRec;
    DBEditObject current_obj;
    db_field current_field;
    Invid roomInvid;

    system sysObj;

    /* -- */

    // create all the systems

    enum = sysLoader.systems.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	sysObj = (system) sysLoader.systems.get(key);
	
	System.out.print("Creating " + key);
	
	// base 263 is for systems

	current_obj = (DBEditObject) createObject((short) 263);
	objInvid = current_obj.getInvid();
	
	System.out.print(" [" + objInvid + "]");

	systemInvid.put(sysObj.systemName, objInvid);

	// set the system name
	    
	current_obj.setFieldValueLocal(systemSchema.SYSTEMNAME,key);

	// set the room

	// this is an invid.. need to see if we've created the room yet,
	// if not, create then set the invid

	roomInvid = (Invid) roomInvids.get(sysObj.room);

	if (roomInvid == null)
	  {
	    System.err.println("\nWarning: registerSystems() creating a room entry not defined in");
	    System.err.println("networks_by_room.cpp file. <" + sysObj.room + ">\n");

	    // base 269 is for rooms

	    DBEditObject room_obj = (DBEditObject) createObject((short) 269);
	    roomInvid = room_obj.getInvid();

	    // label the room with the room number

	    room_obj.setFieldValueLocal(roomSchema.ROOMNUMBER, sysObj.room);

	    // remember the room

	    roomInvids.put(sysObj.room, roomInvid);
	  }

	current_obj.setFieldValueLocal(systemSchema.ROOM, roomInvid);

	// set the type

	Invid typeInvid = (Invid) systemTypeInvids.get(sysObj.type);

	if (typeInvid == null)
	  {
	    System.err.println("\n\n***** No such type found: " + sysObj.type + "\n\n");
	  }

	current_obj.setFieldValueLocal(systemSchema.SYSTEMTYPE, typeInvid);

	// set the manu

	current_obj.setFieldValueLocal(systemSchema.MANUFACTURER, sysObj.manu);

	// set the model

	current_obj.setFieldValueLocal(systemSchema.MODEL, sysObj.model);

	// set the os

	current_obj.setFieldValueLocal(systemSchema.OS, sysObj.os);

	// set the user

	if (sysObj.user != null && !sysObj.user.equals(""))
	  {
	    Invid userInv = (Invid) userInvids.get(sysObj.user);

	    if (userInv != null)
	      {
		current_obj.setFieldValueLocal(systemSchema.PRIMARYUSER, userInv);
	      }
	  }

	// set the aliases

	current_field = current_obj.getField(systemSchema.SYSTEMALIASES);

	for (int i = 0; i < sysObj.aliases.size(); i++)
	  {
	    current_field.addElement(sysObj.aliases.elementAt(i));
	  }

	// set the interfaces

	current_field = current_obj.getField(systemSchema.INTERFACES);

	Invid intInvid;
	Invid netInvid;
	interfaceObj iO;
	DBEditObject interfaceRef;
	db_field interfaceField;
	String netNum;

	for (int i = 0; i < sysObj.interfaces.size(); i++)
	  {
	    iO = (interfaceObj) sysObj.interfaces.elementAt(i);

	    if (debug)
	      {
		System.err.print("Creating interface for system " + sysObj.systemName);
		
		if (iO.interfaceName != null && !iO.interfaceName.equals(""))
		  {
		    System.err.print(" : ");
		    System.err.println(iO.interfaceName);
		  }
		else
		  {
		    System.err.println();
		  }
	      }

	    intInvid = ((invid_field) current_field).createNewEmbedded().getInvid();
	    interfaceRef = (DBEditObject) editObject(intInvid);

	    // set the Ethernet Info for this Interface

	    interfaceRef.setFieldValueLocal(interfaceSchema.ETHERNETINFO, iO.Ether);

	    // set the IP address for this interface

	    interfaceRef.setFieldValueLocal(interfaceSchema.ADDRESS, iO.IP);	// ip address

	    // iO.IP should be a String with four octets in standard IPv4
	    // dotted decimal notation.. we should have an invid for this
	    // interface's network in our networkInvids hash

	    if (iO.IP != null)
	      {
		netNum = iO.IP.substring(0, iO.IP.lastIndexOf("."));
		netInvid = (Invid) networkInvids.get(netNum);

		if (netInvid != null)
		  {
		    interfaceRef.setFieldValueLocal(interfaceSchema.IPNET, netInvid);
		  }
	      }

	    // if the system has a single interface record in GASH, it's
	    // really a single interface system, and we don't need to
	    // have any (redundant) dns information recorded here.

	    if (sysObj.interfaces.size() > 1)
	      {
		// set the name of the interface if this isn't a single-interface
		// systems (single interface systems don't have distinct interface
		// names in GASH)
		
		if (iO.interfaceName != null && !iO.interfaceName.equals(""))
		  {
		    interfaceRef.setFieldValueLocal(interfaceSchema.NAME, iO.interfaceName);
		  }
		
		interfaceField = interfaceRef.getField(interfaceSchema.ALIASES);	// aliases
		
		for (int j = 0; j < iO.aliases.size(); j++)
		  {
		    interfaceField.addElement(iO.aliases.elementAt(j));
		  }
	      }
	  }

	// loop through our owner groups, add this system to the appropriate admin/owner groups

	Hashtable owners = new Hashtable(); // to make sure we don't put this system in groups > once

	for (int i = 0; i < sysObj.admins.size(); i++)
	  {
	    String u = (String) sysObj.admins.elementAt(i);

	    ogRec = (OwnerGroup) adminUsers.get(u);

	    if (ogRec != null)
	      {
		// we don't want to add a system to the supergash owner group

		if (ogRec.prefix.equals("super"))
		  {
		    continue;
		  }

		if (owners.get(ogRec.getInvid()) == null)
		  {
		    // we haven't put the system into this owner group yet

		    db_object ownerGroup = editObject(ogRec.getInvid());
		    db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		    ((DBField) f).addElementLocal(objInvid); // add this group
		    System.out.print(" " + ogRec.prefix);
		    owners.put(ogRec.getInvid(), ogRec);
		  }
	      }
	    else
	      {
		// assume u is a netgroup

		for (int j = 0; j < ownerGroups.size(); j++)
		  {
		    ogRec = (OwnerGroup) ownerGroups.elementAt(j);

		    if (owners.get(ogRec.getInvid()) != null)
		      {
			continue; // don't add redundantly
		      }

		    // if it's a netgroup name, let's see if this ogRec
		    // has edit permission based on the first few chars of the name 
		    
		    if (ogRec.matchMask(u))
		      {
			db_object ownerGroup = editObject(ogRec.getInvid());
			db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
			
			((DBField) f).addElementLocal(objInvid); // add this group.. this is a bi-directional link-up

			System.out.print(" " + ogRec.prefix);
			owners.put(ogRec.getInvid(), ogRec);
		      }
		  }
	      }
	  }

	System.out.println();
      }
  }

  /**
   *
   */

  private static void registerSystemNetgroups() throws RemoteException
  {
    String key;
    Invid invid, objInvid;
    Enumeration enum;
    OwnerGroup ogRec;
    DBEditObject current_obj;
    db_field current_field;
    Hashtable netHash = new Hashtable();

    /* -- */

    enum = systemNetgroup.keys();
    
    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	sNetObj = (SystemNetgroup) systemNetgroup.get(key);

	System.out.print("\nCreating " + key);

	// base 271 is for system netgroups

	current_obj = (DBEditObject) createObject((short) 271);
	objInvid = current_obj.getInvid();

	System.out.println(" [" + objInvid + "]");

	current_obj.setFieldValueLocal(systemNetgroupSchema.NETGROUPNAME, key); // netgroup name

	netHash.put(key, objInvid);

	// now we have to put the systems in

	current_field = current_obj.getField(systemNetgroupSchema.SYSTEMS); // systems

	Vector systems = sNetObj.systems;
	String systemname;

	for (int i = 0; i < systems.size(); i++)
	  {
	    systemname = (String) systems.elementAt(i);

	    if (systemname.equals("-"))
	      {
		// empty marker

		continue;
	      }

	    if (systemname != null)
	      {
		systemname = SystemLoader.stripDomain(systemname);
		invid = (Invid) systemInvid.get(systemname);

		if (invid != null)
		  {
		    System.err.println("Attempting to add " + systemname + ", [" + invid.toString()+"]");
		    ((DBField) current_field).addElementLocal(invid);
		  }
		else
		  {
		    System.err.println("null invid");
		  }
	      }
	    else
	      {
		System.err.println("null systemname");
	      }
	  }

	// loop through our owner groups, add this netgroup to the appropriate owner groups

	for (int i = 0; i < ownerGroups.size(); i++)
	  {
	    ogRec = (OwnerGroup) ownerGroups.elementAt(i);

	    if (ogRec.matchMask(key))
	      {
		db_object ownerGroup = editObject(ogRec.getInvid());
		db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		((DBField) f).addElementLocal(objInvid); // add this group
		System.out.print(" " + ogRec.prefix);
	      }
	  }
      }

    // register system netgroups in the server, part 2.. the sub netgroups

    enum = systemNetgroup.keys();
	
    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	sNetObj = (SystemNetgroup) systemNetgroup.get(key);

	System.out.print("Loading subnetgroups for " + key);

	objInvid = (Invid) netHash.get(key);
	current_obj = (DBEditObject) editObject(objInvid);

	System.out.println(" [" + objInvid + "]");

	// we have to put the sub netgroups in

	current_field = current_obj.getField(systemNetgroupSchema.MEMBERGROUPS); // member netgroups

	Vector subnets = sNetObj.subnetgroups;
	String subName;

	for (int i = 0; i < subnets.size(); i++)
	  {
	    subName = (String) subnets.elementAt(i);

	    if (subName != null)
	      {
		invid = (Invid) netHash.get(subName);

		if (invid != null)
		  {
		    System.err.println("Attempting to add " + subName + ", [" + invid.toString()+"]");
		    ((DBField) current_field).addElementLocal(invid);
		  }
		else
		  {
		    System.err.println("null invid");
		  }
	      }
	    else
	      {
		System.err.println("null subnet name");
	      }
	  }
      }
  }

  /**
   *
   */

  private static void registerAutomounter() throws RemoteException
  {
    Volume v;
    MapEntry m;
    Enumeration enum;
    String key;
    DBEditObject current_obj;
    DBEditObject user_obj, embed_obj;
    db_field current_field;
    invid_field embed_field;
    Invid objInvid, hostInvid, userInvid;

    /* -- */

    // step 1: create the volumes, cache the name->invid mapping

    System.out.println("\nCreating automounter volumes\n");

    enum = volumes.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	v = (Volume) volumes.get(key);

	v.hostName = SystemLoader.stripDomain(v.hostName);

	System.out.print("Creating " + key + " on host: " + v.hostName);

	// base 276 is nfs volume
	
	current_obj = (DBEditObject) createObject((short) 276);
	objInvid = current_obj.getInvid();

	System.out.println(" [" + objInvid + "]");

	volumeInvids.put(key, objInvid); // save the invid in our hashtable

	current_obj.setFieldValueLocal(volumeSchema.LABEL, key); // volume name
	current_obj.setFieldValueLocal(volumeSchema.PATH, v.path);	// path

	hostInvid = (Invid) systemInvid.get(v.hostName);

	current_obj.setFieldValueLocal(volumeSchema.HOST, hostInvid);

	current_obj.setFieldValueLocal(volumeSchema.MOUNTOPTIONS, v.mountOptions);
      }

    // step 2: create all automounter maps

    System.out.println("\nCreating automounter maps\n");

    enum = maps.elements();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();

	// base 277 is automounter map

	current_obj = (DBEditObject) createObject((short) 277);

	current_obj.setFieldValueLocal(mapSchema.MAPNAME, key); // set the name of the map

	objInvid = current_obj.getInvid();

	mapInvids.put(key, objInvid);
      }

    // step 3: create all automounter map entries

    System.out.println("\nCreating automounter map entries\n");

    enum = mapEntries.elements();

    while (enum.hasMoreElements())
      {
	m = (MapEntry) enum.nextElement();

	System.out.println("Creating " + m.toString());

	userInvid = (Invid) userInvids.get(m.userName);

	if (userInvid != null)
	  {
	    // need to edit this user, create an imbedded object

	    user_obj = (DBEditObject) editObject(userInvid);

	    embed_field = (invid_field) user_obj.getField(userSchema.VOLUMES); // Map Entries

	    embed_obj = (DBEditObject) editObject(embed_field.createNewEmbedded().getInvid());

	    // we've got the new map entry, load 'er up
	    
	    embed_obj.setFieldValueLocal(mapEntrySchema.MAP, mapInvids.get(m.mapName));	// map invid
	    
	    embed_obj.setFieldValueLocal(mapEntrySchema.VOLUME, volumeInvids.get(m.volName)); // volume invid
	  }
	else
	  {
	    System.err.println("** Error, user " + m.userName + 
			       " found in map " + m.mapName + 
			       ", but not in user_info");
	    System.err.println("** Skipping.");
	  }
      }
    
    System.out.println("\nFinished creating automounter map entries\n");
  }

  private static db_object createObject(short type)
  {
    return my_client.session.create_db_object(type).getObject();
  }

  private static db_object editObject(Invid invid)
  {
    return my_client.session.edit_db_object(invid).getObject();
  }

}  

/*------------------------------------------------------------------------------
                                                                           class
                                                              directLoaderClient

------------------------------------------------------------------------------*/

/**
 * directLoaderClient does all the heavy lifting to connect the server with the client, and
 * provides callbacks that the server can use to notify the client when something
 * happens.
 */

class directLoaderClient extends UnicastRemoteObject implements Client {

  protected Server server = null;
  protected String username = Ganymede.rootname;
  protected String password = Ganymede.defaultrootpassProperty;
  protected GanymedeSession session = null;

  /* -- */

  public directLoaderClient(Server server) throws RemoteException
  {
    super();    // UnicastRemoteObject can throw RemoteException 

    this.server = server;

    if (server == null)
      {
	System.err.println("Error, null server in directLoaderClient constructor");
      }

    System.err.println("Initializing directLoaderClient object");

    try
      {
	session = (GanymedeSession) server.login(this);

	if (session == null)
	  {
	    System.err.println("Couldn't log in to server.  Bad user/pass?");

	    System.exit(0);
	  }

	System.out.println("logged in");
      }
    catch (RemoteException ex)
      {
	System.err.println("RMI Error: Couldn't log in to server.\n");

	ex.printStackTrace();

	System.exit(0);
      }
    catch (NullPointerException ex)
      {
	System.err.println("Error: Didn't get server reference.  Exiting now.");

	ex.printStackTrace();

	System.exit(0);
      }
    catch (Exception ex)
      {
	System.err.println("Got some other exception: " + ex);
      }

    System.err.println("Got session");

    // turn off oversight whilst we load

    session.enableOversight(false);
  }

  /**
   * Calls the logout() method on the Session object
   */
  public void disconnect() throws RemoteException
  {
    session.logout();
  }

  /**
   * Allows the server to retrieve the username
   */
  public String getName() 
  {
    return username;
  }

  /**
   * Allows the server to retrieve the password
   */
  public String getPassword()
  {
    return password;
  }

  /**
   * Allows the server to force us off when it goes down
   */
  public void forceDisconnect(String reason)
  {
    System.err.println("Server forced disconnect: " + reason);
    System.exit(0);
  }
}
