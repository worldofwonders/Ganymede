/*-----------------------------------------------------------------
  Class 
  Jtaskbar

  This will act like the win95 taskbar and let you go to different
  windows and stuff.

  Override the actionPerformed method to get it to do something.

-----------------------------------------------------------------*/

package arlut.csd.JDataComponent;

import java.awt.Insets;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JPanel;

public class Jtaskbar extends JPanel implements ActionListener, MouseListener {

  Hashtable buttons;
  JPanel panel;
  
  PopupMenu
    popup;

  /* -- */

  public Jtaskbar()
  {
    buttons = new Hashtable();
    panel = new JPanel();
    popup = new PopupMenu();
    add(popup);
    add(panel);
  }

  /**
   * Add a button to the bar.
   *
   * @param label Text to use as label or button
   */

  public JButton addButton(String label)
  {
    //System.out.println("Adding " + label);

    JButton button = new JButton(label);

    //button.addActionListener(this);
    button.addMouseListener(this);
    panel.add(button);
    buttons.put(label, button);
    panel.doLayout();
    button.setMargin(new Insets(5,2,5,2));
    this.validate();
    return button;
  }

  /**
   * Remove a button from the bar
   *
   * @param label Label of button to remove
   */
  public void removeButton(String label)
  {
    JButton button = (JButton)buttons.get(label);

    if (button == null)
      {
	System.err.println("Trying to remove a null button!");
      }
    else
      {
	System.err.println("Button is not null, trying to remove.");
	panel.remove((JButton)buttons.get(label));
	buttons.remove(label);
	this.validate();
      }
  }

  /**
   * Rebuild the bar, adding the buttons to the panel again.
   */
  public void rebuildBar()
  {
    panel.removeAll();
    
    Enumeration en = buttons.keys();
    
    while (en.hasMoreElements())
      {
	panel.add((JButton)en.nextElement());
      }
    
    this.validate();
  }

  /**
   * Change the padding between label and button border
   *
   * @param pad New pad for buttons
   */
  public void setButtonPad(Insets pad)
  {
    Enumeration en = buttons.keys();

    while (en.hasMoreElements())
      {
	((JButton)en.nextElement()).setMargin(pad);
      }
  }

  /**
   * Empty action Performed.  Override this to have the taskbar to what you want.
   */
  public void actionPerformed(ActionEvent e)
  {
    System.out.println("Button clicked in taskbar");
  }

  /**
   * Override these.
   */

  public void mouseClicked(MouseEvent e)
  {
  }

  public void mousePressed(MouseEvent e)
  {
  }

  public void mouseReleased(MouseEvent e)
  {
  }

  public void mouseEntered(MouseEvent e)
  {
  }

  public void mouseExited(MouseEvent e)
  {
  }
}
