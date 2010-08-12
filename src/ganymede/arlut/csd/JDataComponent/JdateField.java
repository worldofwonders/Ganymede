
/*
   JdateField.java

   This class defines a date input field object.

   Created: 31 Jul 1996

   Module By: Navin Manohar

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.JDataComponent;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.rmi.RemoteException;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import arlut.csd.JDialog.StandardDialog;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.TranslationService;

// James ADDS
import javax.swing.UIManager;

import org.jdesktop.swingx.JXDatePicker; 
import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.plaf.basic.SpinningCalendarHeaderHandler;
import org.jdesktop.swingx.plaf.basic.CalendarHeaderHandler;

//test
import java.awt.event.FocusListener;
import javax.swing.JFormattedTextField;

import java.text.ParseException;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      JdateField

------------------------------------------------------------------------------*/

/**
 *
 * This class defines a date input field object.
 *
 */

public class JdateField extends JPanel implements ActionListener, FocusListener
{
  //public class JdateField extends JPanel implements ActionListener {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.JDataComponent.JdateField");

  // ---

  private JXDatePicker datePicker;       

  private boolean
    allowCallback = false,
    changed = false, 
    limited,
    unset,
    iseditable;

  private JsetValueCallback callback = null;

  protected Date
    original_date,
    my_date,
    old_date;

  private Date 
    maxDate,
    minDate;

  private JButton _calendarButton;


  //////////////////
  // Constructors //
  //////////////////

  /**
   * Minimal Constructor for JdateField.  This will construct a JdateField
   * with no value.
   *  
   */
  
  public JdateField()
  {
    this(null,true,false,null,null);
  }

  /**
   * Contructor that creates a JdateField based on the Date object it is given.  This
   * constructor can be used if the JdateField will be making callbacks to pass its data
   * to the appropriate container.
   *
   * @param parent the container which implements the callback function for this JdateField
   * @param date the Date object to use
   * @param iseditable true if the datefield can be edited by the user
   * @param islimited true if there is to be a restriction on the range of dates
   * @param minDate the oldest possible date that can be entered into this JdateField
   * @param maxDate the newest possible date that can be entered into this JdateField
   */

  public JdateField(Date date,
		    boolean iseditable,
		    boolean islimited,
		    Date minDate,
		    Date maxDate,
		    JsetValueCallback parent)
  {
    this(date,iseditable,islimited,minDate,maxDate);

    setCallback(parent);
  }
  
  /**
   * Contructor that creates a JdateField based on the date it is given.  It is also
   * possible to set restrictions on the range of dates for this JdateField when
   * using this constructor
   *
   * @param date the Date object to use
   * @param islimited true if there is to be a restriction on the range of dates
   * @param minDate the oldest possible date that can be entered into this JdateField
   * @param maxDate the newest possible date that can be entered into this JdateField
   */

  public JdateField(Date date,
		    boolean iseditable,
		    boolean islimited,
		    Date minDate,
		    Date maxDate)
  { 
    if (debug)
      {
	System.err.println("JdateField(): date = " + date);
      }

    if (date == null)
      {
	my_date = original_date = null; 
      }
    else
      {
	my_date = original_date = new Date(date.getTime());
      }

    // Check if the date is limited.
    if (islimited)
      {
	if (minDate == null)
	  {
	    throw new IllegalArgumentException("Invalid Parameter: minDate cannot be null");
	  }

	if (maxDate == null)
	  {
	    throw new IllegalArgumentException("Invalid Parameter: maxDate canot be null");
	  }

	limited = true;
	this.minDate = minDate;
	this.maxDate = maxDate;
      }       
   
    setLayout(new BorderLayout());
    

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BorderLayout());

    // Adds a year spinner to the MonthView object.
    UIManager.put(CalendarHeaderHandler.uiControllerID, 
		  "org.jdesktop.swingx.plaf.basic.SpinningCalendarHeaderHandler");
    // Moves the year spinner after month arrows.
    UIManager.put(SpinningCalendarHeaderHandler.ARROWS_SURROUND_MONTH, Boolean.TRUE);
    
    // Creates a new picker and sets the current date to today 
    datePicker = new JXDatePicker(date); 
    datePicker.setName("datePicker"); 
    datePicker.addActionListener(this);
    
    JXMonthView monthView = datePicker.getMonthView();
    if (minDate != null) monthView.setLowerBound(minDate);
    if (maxDate != null) monthView.setUpperBound(maxDate);
    monthView.setZoomable(true);    
    buttonPanel.add(datePicker, "West");


    // Add a calendar icon to popup the widget.
    // TODO MAKE CAL BUTTON TO PULLUP NEW POPUP.
    Image img = PackageResources.getImageResource(this, "calendar.gif", getClass());
    Image img_dn = PackageResources.getImageResource(this, "calendar_dn.gif", getClass());
    _calendarButton = new JButton(new ImageIcon(img));
    _calendarButton.setPressedIcon(new ImageIcon(img_dn));
    _calendarButton.setFocusPainted(false);
    _calendarButton.addActionListener(this);
    if (iseditable) buttonPanel.add(_calendarButton,"Center");


    add(buttonPanel, "East");
    


    // This will make the text field and popup uneditable.
    datePicker.setEditable(iseditable);


    // todo, may need to add in a time field...


    // just doesnt work.
    // add an actionlistener now.
    //    datePicker.addPropertyChangeListener(new FocusManagerListener());    


    // this doesnt appear to get the change correctly.
    JFormattedTextField textf = datePicker.getEditor();
    textf.addFocusListener(this);

    unset = true;

    setDate(my_date);

    invalidate();
    validate();
  }

  /*
  public FocusManagerListener() implements PropertyChangeListener {
  public void propertyChangeListener(PropertyChangeEvent e) 
  {
    if ("date".equals(e.getPropertyName()))
      {
	System.out.println("date curr:");
	System.out.println(d1);
	
	saveDate(picker.getDate());
      }
  }
  }
  */

  
  public void focusLost(FocusEvent e) 
  {
    System.out.println("Focus lost");

    try 
      {
	datePicker.getEditor().commitEdit();
      } 
    catch ( ParseException pe ) 
      {
      }  

    String d1 = datePicker.getDate().toString();
    System.out.println("date2 curr:");
    System.out.println(d1);

    datePickerFocusLost();
  }


  public void focusGained(FocusEvent e) 
  {
    // nothing.
  }

  // Called from the addActionListeners above.
  public void actionPerformed(ActionEvent e) 
  {
    Object c = e.getSource();

    /* -- */

    if (c == datePicker) 
      {
	datePickerFocusLost();
      }    
    // TODO UNUSED CURRENTLY
    // Open up the calendar widget when clicked.
    else if (c == _calendarButton)
      {
	System.err.println("We have clicked the cal button");

	/*
	*/
      }
  }


  public void datePickerFocusLost()
  {
    // The user has pressed Tab or clicked elsewhere which has caused the
    // datePicker component to lose focus.  This means that we need to update
    // the date value (using the value in datePicker) and then propagate that value
    // up to the server object.
    
    Date d1 = datePicker.getDate();
    setDate(d1);
    //System.err.println(d1.toString());

    
    // Now, the date value needs to be propagated up to the server	
    if (allowCallback) 
      {
	boolean retval = false;

	try 
	  {
	    System.err.println("calling back to the server now!!!");
	    
	    retval = callback.setValuePerformed(new JSetValueObject(this, d1));

	    System.out.println("retval was ");
	    System.out.println(retval);
	  }
	catch (RemoteException ex)
	  {
	    // throw up an information dialog here
	    
	    // "Date Field Error"
	    // "There was an error communicating with the server!\n{0}"
	    new JErrorDialog(new JFrame(),
			     ts.l("global.error_subj"),
			     ts.l("global.error_text", ex.getMessage()), 
			     StandardDialog.ModalityType.DOCUMENT_MODAL);
	  }
	
	// if setValuePerformed() didn't work, revert the date,
	// otherwise do nothing and we'll let ourselves show a
	    // refreshed value if the server requested us to refresh
	    // ourselves during the processing of setValuePerformed().
	if (!retval)
	  {
	    setDate(old_date, false);
	  }
      }
  }  
  
  /**
   * May this field be edited?
   */

  /*
  public void setEditable(boolean editable)
  {
    //_date1.setEditable(editable);  TODO add this to new var.
    this.iseditable = editable;
  }
  */

  /**
   * returns the date associated with this JdateField
   */

  public Date getDate()
  {
    if (unset)
      {
	return null;
      }

    return my_date;
  }

  /**
   * sets the date value of this JdateField
   *
   * @param d the date to use
   */

  public void setDate(Date d)
  {
    this.setDate(d, true);
  }

  /**
   * sets the date value of this JdateField
   *
   * @param d the date to use
   */

  public void setDate(Date d, boolean checkLimits)
  {
    if (debug)
      {
	System.err.println("setDate() called: " + d);
      }

    if (d == null)
      {
	unset = true;
      }

    datePicker.setDate(d);  
    my_date = d;

    if (original_date == null)
      {
        original_date = d;
      }

    unset = false;
    changed = true;
  }

  /**
   *  sets the parent of this component for callback purposes
   *
   */

  public void setCallback(JsetValueCallback callback)
  {
    System.err.println("inside set callback to true now, debug");

    if (callback == null)
      {
	return;
      }
    
    this.callback = callback;    
    allowCallback = true;
  }


  /**
   *
   * Tie into the focus event handling.
   *
   */

  public void processFocusEvent(FocusEvent e)
  {
    super.processFocusEvent(e);

    switch (e.getID())
      {
      case FocusEvent.FOCUS_LOST:
	// When the JdateField looses focus, any changes
	// made to the value in the JdateField must be
	// propagated to the db_field on the server.
	
	// But first, if nothing in the JstringField has changed
	// then there is no reason to do anything.
	
	if (!changed)
	  {
	    break;
	  }
	
	if (!unset && allowCallback)
	  {
	    // Do a callback to talk to the server
	    
	    try 
	      {
		callback.setValuePerformed(new JSetValueObject(this, my_date));
	      }
	    catch (java.rmi.RemoteException re) 
	      {
		// throw up an information dialog here

		// "Date Field Error"
		// "There was an error communicating with the server!\n{0}"
		new JErrorDialog(new JFrame(),
				 ts.l("global.error_subj"),
				 ts.l("global.error_text", re.getMessage()), 
				 StandardDialog.ModalityType.DOCUMENT_MODAL);
	      }
	  }
	
	// Now, the new value has propagated to the server, so we set
	// changed to false, so that the next time we loose focus from
	// this widget, we won't unnecessarily update the server value
	// if nothing has changed locally.
	
	changed = false;

	break;

      case FocusEvent.FOCUS_GAINED:
	break;
      }
  }

}

