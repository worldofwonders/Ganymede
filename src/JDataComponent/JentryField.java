/*
   JentryField.java
   
   Created: 12 Jul 1996
   Version: 1.2 97/07/22
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin
*/

package arlut.csd.JDataComponent;

import java.awt.*;
import java.awt.event.*;

import com.sun.java.swing.*;
import com.sun.java.swing.text.*;

//---------------------------------------------------------------------
//                                                 class JentryField
//---------------------------------------------------------------------

/**
 *  JentryField serves as an abstract base class for all Fields that use textfields.
 *  The subclasses of this class should be used.
 */

abstract public class JentryField extends JTextField implements FocusListener{
  final boolean debug = false;

  public boolean allowCallback = false;
  protected boolean changed = false; 

  protected JsetValueCallback my_parent = null;

  //////////////////
  // Constructors //
  //////////////////

  public JentryField(int columns) 
  {
    super(columns);
    setEditable(true);

    addFocusListener(this);

    enableEvents(AWTEvent.KEY_EVENT_MASK); 

  }

  ///////////////////
  // Class Methods //
  ///////////////////

  /**
   *  returns true if the value in the JentryField has 
   *  been modified.
   */

  public boolean getChanged()
  {
    return changed;
  }

  
  /**
   *  sets the parent of this component for callback purposes
   *
   */

  public void setCallback(JsetValueCallback parent)
  {
    if (parent == null)
      {
	throw new IllegalArgumentException("Invalid Parameter: parent cannot be null");
      }
    
    my_parent = parent;

    allowCallback = true;
  }

  /**
   * sendCallback is called when focus is lost.
   */
  public abstract void sendCallback();
    

  /**
   *  Stub function that is overriden in subclasses of JentryField
   *
   */

  private boolean isAllowed(char ch)
  {
    return true;
  }
  
  /**
   *
   * We only want certain keystrokes to be registered by the field.
   *
   * This method overrides the processKeyEvent() method in JComponent and
   * gives us a way of intercepting characters that we don't want in our
   * string field.
   *
   */

  protected void processKeyEvent(KeyEvent e)
  {
    // always pass through useful editing keystrokes.. this seems to be
    // necessary because backspace and delete apparently have defined
    // Unicode representations, so they don't match CHAR_UNDEFINED below

    if ((e.getKeyCode() == KeyEvent.VK_BACK_SPACE) ||
	(e.getKeyCode() == KeyEvent.VK_DELETE) ||
	(e.getKeyCode() == KeyEvent.VK_END) ||
	(e.getKeyCode() == KeyEvent.VK_HOME))
      {
	System.out.println("It's a backspace, delte, end , or home.");
	super.processKeyEvent(e);
      }

    // We check against KeyEvent.CHAR_UNDEFINED so that we pass
    // through things like backspace, arrow keys, etc.

    if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED)
      {
	System.out.println("Key is undefined, allowing it.");
	super.processKeyEvent(e);
      }
    else if (isAllowed(e.getKeyChar()))
      {
	System.out.println("isAllowed returned ok.");
	super.processKeyEvent(e);
      }
    else
      {
	System.out.println("isAllowed returned false.");
      }

    // otherwise, we ignore it
  }

  public void focusLost(FocusEvent e)
  {
    if (debug)
      {
	System.out.println("JentryField: focusLost");
      }

    sendCallback();
  }

  public void focusGained(FocusEvent e)
  {
    if (debug)
      {
	System.out.println("focusGained");
      }
  }
}






