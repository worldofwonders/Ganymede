
/*
  GASH 2

  vectorPanel.java

  This module provides for a generic vector of objects, and can be
  used to implement a collection of date fields, i.p. addresses,
  or edit in place (composite) objects.

  Created: 17 Oct 1996
  Version: $Revision: 1.29 $ %D%
  Module By: Navin Manohar, Mike Mulvaney, Jonathan Abbey
  Applied Research Laboratories, The University of Texas at Austin
*/


package arlut.csd.ganymede.client;

//import arlut.csd.ganymede.client.*;
import arlut.csd.ganymede.*;

import java.awt.event.*;
import java.awt.*;

import arlut.csd.JDataComponent.*;
import java.util.*;
import java.rmi.*;
import java.net.*;

import jdj.PackageResources;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     vectorPanel

------------------------------------------------------------------------------*/

/**
 *
 * This module provides for a generic vector of objects, and can be
 * used to implement a collection of date fields, i.p. addresses,
 * or edit in place (composite) objects.
 *
 */

public class vectorPanel extends JPanel implements JsetValueCallback, ActionListener, MouseListener, Runnable {

  boolean debug = false;

  // class variables

  //static JcomponentAttr ca = new JcomponentAttr(null,new Font("Helvetica",Font.PLAIN,12),Color.black,Color.white);

  // --

  // object variables

  Vector
    compVector;

  String 
    name = null;

  Hashtable
    ewHash;

  JScrollPane 
    scrollPane;

  JButton
    addB;
  
  Vector 
    choices = null;

  short 
    type;

  JPanel
    centerPanel;

  boolean 
    editable,
    isEditInPlace,
    centerPanelAdded = false;

  LineBorder
    lineBorder = new LineBorder(Color.black);

  Image
    removeImage;

  private db_field my_field;

  protected windowPanel wp;

  containerPanel
    container;

  gclient
    gc;

  JPopupMenu
    popupMenu;

  JMenuItem
    closeLevelMI,
    expandLevelMI,
    closeAllMI,
    expandAllMI;

  /* -- */
  
  /**
   *
   *
   */

  public vectorPanel(db_field field, windowPanel parent, boolean editable, boolean isEditInPlace, containerPanel container)
  {
    // Took out some checking for null stuff

    my_field = field;
    
    this.editable = editable;
    this.isEditInPlace = isEditInPlace;
    this.wp = parent;
    this.container = container;
    
    gc = container.gc;

    debug = gc.debug;

    if (debug)
      {
	System.out.println("Adding new vectorPanel");
      }

    centerPanel = new JPanel(false);

    //centerPanel.setLayout(new ColumnLayout(Orientation.LEFT,Orientation.TOP));

    centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
    
    try
      {
	type = my_field.getType();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Cannot get field type: " + rx);
      }
    
    setLayout(new BorderLayout());

    EmptyBorder eb = wp.emptyBorder10;
    TitledBorder tb;

    try
      {
	name = field.getName();
	tb = BorderFactory.createTitledBorder(name + ": Vector");
      }
    catch (RemoteException ex)
      {
	tb = BorderFactory.createTitledBorder("Vector -- unknown field name");
      }

    CompoundBorder cb = BorderFactory.createCompoundBorder(tb,eb);
    setBorder(cb);

    addB = new JButton("Add " + name);

    //setBackground(container.frame.getVectorBG());

    // Set up pop up menu
    popupMenu = new JPopupMenu();
    expandLevelMI = new JMenuItem("Expand this level");
    expandLevelMI.addActionListener(this);
    expandAllMI = new JMenuItem("Expand all elements");
    expandAllMI.addActionListener(this);
    popupMenu.add(expandLevelMI);
    popupMenu.add(expandAllMI);
    popupMenu.addSeparator();

    closeLevelMI = new JMenuItem("Close this level");
    closeLevelMI.addActionListener(this);
    closeAllMI = new JMenuItem("Close all elements");
    closeAllMI.addActionListener(this);
    popupMenu.add(closeLevelMI);
    popupMenu.add(closeAllMI);

    addMouseListener(this);

    compVector = new Vector();
    ewHash = new Hashtable();

    createVectorComponents();

    //invalidateRight();
  }

  private void showPopupMenu(int x, int y)
  {
    popupMenu.show(this, x, y);
  }

  private void createVectorComponents()
  {
    // Took out some more redundant checking

    if (my_field instanceof ip_field)
      {
	if (debug)
	  {
	    System.out.println("Adding ip vector field");
	  }

	try
	  {
	    ip_field ipfield = (ip_field) my_field;

	    int size = ipfield.size();
	    
	    for (int i=0;i < size;i++) 
	      {
		JIPField ipf = new JIPField(new JcomponentAttr(null,
							       new Font("Helvetica",Font.PLAIN,12),
							       Color.black,Color.white),
					    editable,
					    ipfield.v6Allowed());
		
		ipf.setValue((Byte[]) ipfield.getElement(i));
		ipf.setCallback(this);
		
		addElement(ipf, false);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't make ip field: " + rx);
	  }
      }
    else if (my_field instanceof invid_field)
      {
	if (debug)
	  {
	    System.out.println("Adding vector invid_field");
	  }

	try
	  {
	    invid_field invidfield = (invid_field) my_field;

	    if (isEditInPlace)
	      {
		if (debug)
		  {
		    System.out.println("Adding edit in place invid vector, size = " + invidfield.size());
		  }

		int size = invidfield.size();

		for (int i=0; i < size ; i++)
		  {
		    if (debug)
		      {
			System.out.println("Adding Invid to edit in place vector panel");
		      }

		    Invid inv = (Invid)(invidfield.getElement(i));
		   
		    db_object object = null;
		    if (editable)
		      {
			object = wp.getgclient().getSession().edit_db_object(inv);
		      }
		    else
		      {
			object = wp.getgclient().getSession().view_db_object(inv);
		      }

		    containerPanel cp = new containerPanel(object,
							   editable,
							   wp.gc,
							   wp, container.frame,
							   null, false);
		    container.frame.containerPanels.addElement(cp);
		    cp.setBorder(wp.lineEmptyBorder);
		    
		    //		    addElement(object.getLabel(), cp);
		    addElement((i+1) + ". " + object.getLabel(), cp, false, false);
		  }
	      }
	    else
	      {
		System.out.println("*** Error - should not handle non edit-in-place Invid's in vector panel ***");
	      }	
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't make invid field: " + rx);
	  }
      }
    else
      {
	System.out.println("\n*** Error - inappropriate field type passed to vectorPanel constructor");
      }

    // do it

    //try
    //{
	//if (editable && my_field.isEditable())

    if (editable)
      {
	if (debug)
	  {
	    System.out.println("Adding add button");
	  }

	JPanel addPanel = new JPanel();
	addPanel.setLayout(new BorderLayout());
	addB.addActionListener(this);

	addPanel.add("East", addB);

	add("South", addPanel);
      }
    else
      {
	if (debug)
	  {
	    System.out.println("Field is not editable, no button added");
	  }
      }

	//   }
	//catch (RemoteException rx)
	// {
	//throw new RuntimeException("Can't check if field is editable: " + rx);
	//}
  } 

  public void addNewElement()
  {
    int size = -1;

    /* -- */

    try
      {
	size = my_field.size();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get field size: " + rx);
      }

    if (compVector.size() > size)
      {
	wp.gc.setStatus("There is already an empty, new field");
      }
    else
      {
	if (debug)
	  {
	    System.out.println("Adding new element");
	  }
	
	if (isEditInPlace)
	  {
	    if (debug)
	      {
		System.out.println("Adding new edit in place element");
	      }

	    try
	      {
		Invid invid = ((invid_field)my_field).createNewEmbedded();
		db_object object = wp.gc.getSession().edit_db_object(invid);

		containerPanel cp = new containerPanel(object,
						       my_field.isEditable() && editable,
						       wp.gc,
						       wp, container.frame);
		
		container.frame.containerPanels.addElement(cp);

		cp.setBorder(wp.lineEmptyBorder);

		addElement("New Element", cp, true);
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not create new containerPanel: " + rx);
	      }
	  }
	else if (my_field instanceof ip_field)
	  {
	    if (debug)
	      {
		System.out.println("Adding new ip vector field");
	      }

	    ip_field ipfield = (ip_field) my_field;
	    
	    try
	      {
		JIPField ipf = new JIPField(new JcomponentAttr(null,
							       new Font("Helvetica",Font.PLAIN,12),
							       Color.black,Color.white),
					    true,
					    ipfield.v6Allowed());
		ipf.setCallback(this);
		addElement(ipf);
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not make new ip field: " + rx);
	      }
	  }
	else
	  {
	    System.out.println("This type is not supported yet.");
	  }
      }
    }

  /*
  public void validate()
  {
    System.out.println("--Validate: vectorP: " + this);
    super.validate();
  }
  public void invalidate()
  {
    System.out.println("--inValidate: vectorP: " + this);
    super.invalidate();
  }

  public void doLayout()
  {
    System.out.println("[[doLayout vect: " + this);
    super.doLayout();
    System.out.println("]]doLayout vect");

  }
  */

  public void addElement(Component c)
  {
    addElement(null, c, false, true);
  }

  public void addElement(Component c, boolean invalidateNow)
  {
    addElement(null, c, false, invalidateNow);
  }
  public void addElement(String title, Component c)
  {
    addElement(title,c,false, true);
  }

  public void addElement(String title, Component c, boolean expand)
  {
    addElement(title, c, expand, true);
  }

  public void addElement(String title, Component c, boolean expand, boolean invalidateNow)
  {
    if (c == null)
      {
	throw new IllegalArgumentException("Component parameter is null");
      }

    setStatus("adding new elementWrapper");
      
    compVector.addElement(c);

    if (debug)
      {
	System.out.println("Index of element: " + compVector.size());
      }

    // Don't add the buttons for an non-editable field

    if (!centerPanelAdded)
      {
	add("Center", centerPanel);
	centerPanelAdded = true;
      }

    elementWrapper ew = new elementWrapper(title, c, this, editable);
    ewHash.put(c, ew);
    centerPanel.add(ew);
    
    if (expand && (c instanceof containerPanel))
      {
	ew.expand();
      }

    if (invalidateNow)
      {
	invalidate();
      }

    setStatus("Done adding elementWrapper");

    //container.frame.validate_general();
  }
  
  public void deleteElement(Component c) 
  {
    if (debug)
      {
	System.out.println("Deleting element");
      }

    if (c == null)
      {
	throw new IllegalArgumentException("Component parameter is null");
      }

    if (my_field == null)
      {
	throw new RuntimeException("Error: vectorPanel.my_field is null ");
      }
      
    try
      {
	if (!my_field.isEditable())
	  {
	    return;
	  }
      }
    catch (RemoteException rx) 
      {
	throw new RuntimeException("Could not check field: " + rx);
      }

    try
      {
	if (debug)
	  {
	    System.out.println("Deleting element number: " + compVector.indexOf(c));
	  }

	my_field.deleteElement(compVector.indexOf(c));
	compVector.removeElement(c);	  
	centerPanel.remove((elementWrapper)ewHash.get(c));

	//invalidateRight();
	invalidate();
	container.frame.validate();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not delete element:" + rx);
      }
  }

  /**
   * Expand all the levels.
   */
  
  public void run()
  {
    Enumeration wrappers = ewHash.keys();

    /* -- */

    while (wrappers.hasMoreElements())
      {
	elementWrapper ew = (elementWrapper)ewHash.get(wrappers.nextElement());
	ew.expand(true);

	Component comp = ew.getComponent();
	
	if (comp instanceof containerPanel)
	  {
	    if (debug)
	      {
		System.out.println("Aye, it's a containerPanel");
	      }

	    containerPanel cp = (containerPanel)comp;
	    
	    for (int i = 0; i < cp.vectorPanelList.size(); i++)
	      {
		((vectorPanel)cp.vectorPanelList.elementAt(i)).expandLevels(true);
	      }
	  }
	else
	  {
	    System.out.println("The likes of this I have never seen: " + comp);
	  }
      }

    invalidate();
    container.frame.validate();
  }
  
  public void expandLevels(boolean recursive)
  {
    if (recursive)
      {
	Thread t = new Thread(this);
	t.start();
      }
    else
      {

	Enumeration wrappers = ewHash.keys();
	
	/* -- */
	
	while (wrappers.hasMoreElements())
	  {
	    elementWrapper ew = (elementWrapper)ewHash.get(wrappers.nextElement());
	    ew.expand(true);
	  }
      }

  }
  public void closeLevels(boolean recursive)
  {
    Enumeration wrappers = ewHash.keys();

    while (wrappers.hasMoreElements())
      {
	elementWrapper ew = (elementWrapper)ewHash.get(wrappers.nextElement());
	ew.expand(false);

	if (recursive)
	  {
	    Component comp = ew.getComponent();

	    if (comp instanceof vectorPanel)
	      {
		((vectorPanel)comp).closeLevels(true);
	      }
	  }
      }
    
    // This is necessary for the closing, but the expanding doesn't need validate stuff.
    // Pretty weird, but thus is swing.
    invalidate();
    container.frame.validate();

  }

  public void actionPerformed(ActionEvent e)
  {
    if ((e.getSource() == addB) && editable)
      {
	addNewElement();
      }
    else if (e.getSource() instanceof JMenuItem)
      {
	if (debug)
	  {
	    System.out.println("JMenuItem: " + e.getActionCommand());
	  }

	if (e.getActionCommand().equals("Expand all elements"))
	  {
	    expandLevels(true);
	  }
	else if (e.getActionCommand().equals("Expand this level"))
	  {
	    expandLevels(false);
	  }
	else if (e.getActionCommand().equals("Close this level"))
	  {
	    closeLevels(false);
	  }
	else if (e.getActionCommand().equals("Close all elements"))
	  {
	    closeLevels(true);
	  }
      }
  }

  public boolean setValuePerformed(JValueObject v)
  {
    boolean returnValue = false;
    
    if (v == null)
      {
	throw new IllegalArgumentException("ValueObject Argument is null");
      }

    if (v.getValue() == null)
      {
	return false;
      }
    else if (v.getValue().equals("remove") )
      {
	if (debug)
	  {
	    System.out.println("You clicked on a minus");
	  }
	if (editable)
	  {
	    deleteElement(v.getSource());
	  }
	else
	  {
	    setStatus("You can't delete elements in a view window.");
	    returnValue = false;
	  }
      }
    else if (v.getSource() instanceof JIPField)
      {
	if (debug)
	  {
	    System.out.println("IP field changed");
	  }
	
	if (editable)
	  {
	    short index = (short)compVector.indexOf(v.getSource());
	
	    if (v.getOperationType() == JValueObject.ERROR)
	      {
		setStatus((String)v.getValue());
		returnValue = false;
	      }
	    else
	      {
		try
		  {
		    returnValue = changeElement((Byte[])v.getValue(), index);
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not set value of date field: " + rx);
		  }
	      }
	  }
	else     //editable == false, so can't make changes
	  {
	    returnValue = false; 
	  }
      }
    else
      {
	System.out.println("Value changed in field that is not yet supported");
      }

    if (returnValue)
      {
	wp.gc.somethingChanged();
      }

    return returnValue;
  }

  public void mousePressed(java.awt.event.MouseEvent e) 
  {
    if (e.isPopupTrigger())
      {
	showPopupMenu(e.getX(), e.getY());
      }
  }

  public void mouseClicked(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
	
  /*
   * This changes an element after a ValueCallback.
   */

  public boolean changeElement(Object obj, short index) throws RemoteException
  {
    ReturnVal retVal;
    boolean succeeded;
    
    /* -- */

    if (index >= my_field.size())
      {
	if (debug)
	  {
	    System.out.println("Adding new element");
	  }

	retVal = my_field.addElement(obj);

	succeeded = (retVal == null) ? true : retVal.didSucceed();

	if (retVal != null)
	  {
	    gc.handleReturnVal(retVal);
	  }

	if (succeeded)
	  {
	    if (debug)
	      {
		System.out.println("Add Element returned true");
		System.out.println("There are now " + my_field.size() + " elements in the field");
	      }

	    return true;
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("Add Element returned false");
	      }

	    return false;
	  }
      }
    else
      {
	if (debug)
	  {
	    System.out.println("Changing element " + index);
	  }

	retVal = my_field.setElement(index, obj);

	succeeded = (retVal == null) ? true : retVal.didSucceed();

	if (retVal != null)
	  {
	    gc.handleReturnVal(retVal);
	  }

	if (succeeded)
	  {
	    if (debug)
	      {
		System.out.println("set Element returned true");
	      }

	    return true;
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("set Element returned false");
	      }

	    return false;
	  }
      }	    
  }

  // What is this for?
  
  public Session getSession() 
  {
    return null;
  }

  /*
   *
   * This method does causes the hierarchy of containers above
   * us to be recalculated from the bottom (us) on up.  Normally
   * the validate process works from the top-most container down,
   * which isn't what we want at all in this context.
   *
   *

  public void invalidateRight()
  {
    Component c;

    c = this;

    while ((c != null) && !(c instanceof JViewport))
      {
	System.out.println("doLayout on " + c);

	c.doLayout();
	c = c.getParent();
      }
  }
  */

  public final void setStatus(String status)
  {
    wp.gc.setStatus(status);
  }

}

