/*
   glogin.java

   Ganymede client login module

   This client has been developed so that it can run as both an applet,
   as well as an application.

   --

   Created: 22 Jan 1997
   Version: $Revision: 1.9 $ %D%
   Module By: Navin Manohar and Mike Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/
package arlut.csd.ganymede.client;

import com.sun.java.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;

import gjt.ImageCanvas;
import jdj.*;

import arlut.csd.DataComponent.*;
import oreilly.Dialog.InfoDialog;
import arlut.csd.ganymede.*;

/**
 *
 *
 *
 */

public class glogin extends java.applet.Applet implements Runnable {

  protected Image ganymede_logo;
  protected TextField username;
  protected TextField passwd;
  protected JButton connector;
  protected JButton _quitButton;
  protected Label _connectStatus = new Label();
  protected JPanel bPanel;
  protected static Frame my_frame = null;

  protected static Server  my_server;

  protected static iClient my_client;

  protected static Session my_session;
  protected static String my_username,my_passwd;

  protected static glogin my_glogin;
  protected InfoDialog _infoD;

  private LoginHandler _loginHandler;

  protected Thread my_thread = new Thread(this);

  protected boolean connected = false;

  private static boolean WeAreApplet = true;

  /**
   *  This main() function will allow this applet to run as an application
   *  when it is not executed in the context of a browser.
   */

  public static void main (String args[])
  {
    WeAreApplet = false;

    my_glogin = new glogin();

    my_glogin.setLayout(new BorderLayout());

    my_frame = new Frame("Ganymede Client");
    my_frame.setLayout(new BorderLayout());
   
    my_frame.add(my_glogin,"Center");

    my_frame.pack();
    my_frame.setSize(265,380);    
    my_frame.show();
 
    my_glogin.init();
    my_glogin.start();
    my_glogin.getLayout().layoutContainer(my_glogin);
  }

  /**
   *
   *
   */

  public void init() 
  {
    
    try
      {
	my_glogin = this;
      
	// Dowload the ganymede logo using the appropriate method

	if (!WeAreApplet)
	  {
	    //	    ganymede_logo = Toolkit.getDefaultToolkit().getImage(new URL(gConfig._GANYMEDE_LOGO_URL));
	    ganymede_logo = PackageResources.getImageResource(this, "ganymede.jpg", getClass());
	  }
	else
	  {
	    ganymede_logo = getImage(new URL(gConfig._GANYMEDE_LOGO_URL));

	    my_frame = new Frame();
	  }
      }
    catch (java.net.MalformedURLException e) 
      {
	System.out.println("The URL was malformed");
      }
   
     _infoD = new InfoDialog(my_frame,true,"","");
 
    setLayout(new BorderLayout());

    add(new ImageCanvas(ganymede_logo), "Center");
    
    Panel p = new Panel();

    p.setLayout(new GridLayout(4,1));

    p.add(new Label("Ganymede Network Management System"));

    // the username and passwd fields here won't have their
    // callback set with addTextListener().. instead, we'll
    // trap the login/quit buttons, and query these
    // fields when we process the buttons.
    
    username = new TextField("",20);

    p.add(new FieldWrapper("Username:",username));

    passwd = new TextField("",20);
    passwd.setEchoChar('*');

    p.add(new FieldWrapper("Password:",passwd));
    
    username.setEnabled(false);
    username.setText("supergash");
    passwd.setEnabled(false);

    bPanel = new JPanel();
    bPanel.setLayout(new BorderLayout());

    _quitButton = new JButton("Quit");

    _loginHandler = new LoginHandler(this);

    connector = new JButton("Login to Server");
    connector.addActionListener(_loginHandler);
    
    _quitButton.addActionListener(_loginHandler);

    bPanel.add(_connectStatus,"Center");
    bPanel.add(_quitButton,"East");

    p.add(bPanel);

    add(p,"South");

    // frames like to be packed

    if (!WeAreApplet)
      {
	my_frame.pack();
      }

    // The Login GUI has been set up.  Now the server connection needs
    // to be properly established.
    
    /* RMI initialization stuff. We do this for our iClient object. */
      
    System.setSecurityManager(new RMISecurityManager());
      
    /* Get a reference to the server */

    my_thread.start();
  }

  /**
   * This will be executed in the thread that tries to connect to the
   * server.  The thread will terminate after a connection to the
   * server has been made.
   */

  public void run() 
  {
    if (connected)
      {
	return;
      }

    int state = 0;
      
    do {

      try
	{
	  connected = true;

	  Remote obj = Naming.lookup(gConfig._GANYMEDE_SERVER_URL);
	  
	  if (obj instanceof Server)
	    {
	      my_server = (Server) obj;
	    }
	}
      catch (NotBoundException ex)
	{
	  connected = false;

	  //System.err.println("RMI: Couldn't bind to server object\n" + ex );
	}
      catch (java.rmi.UnknownHostException ex)
	{
	  connected = false;

	  //System.err.println("RMI: Couldn't find server\n" + gConfig._GANYMEDE_SERVER_URL );
	}
      catch (RemoteException ex)
	{
	  connected = false;
	  //ex.printStackTrace();

	  //	  System.err.println("RMI: RemoteException during lookup.\n" + ex);
	}
      catch (java.net.MalformedURLException ex)
	{
	  connected = false;
	  	  
	  //System.err.println("RMI: Malformed URL " + gConfig._GANYMEDE_SERVER_URL );
	}

      switch (state) 
	{
        case 0: 
	  _connectStatus.setText("Connecting... |");
	  state++;
	  break;

	case 1:
	  _connectStatus.setText("Connecting...  /");
	  state++;
	  break;

	case 2:
	  _connectStatus.setText("Connecting...  -");
	  state++;
	  break;

	case 3: 
	  _connectStatus.setText("Connecting... \\");
	  state = 0;
	  break;
	}

      try 
	{
	  // Wait for 1 sec before retrying to connect to server
	  Thread.sleep(1000);
	}
      catch (InterruptedException e) 
	{
	}

    } while (!connected);

    // At this point, a connection to the server has been established,
    // So we allow the "Login to Server" button to be visible.

    bPanel.remove(_connectStatus);
    bPanel.add(connector,"Center");
    bPanel.doLayout();

    username.setEnabled(true);
    passwd.setEnabled(true);
  }

  public void stop() 
  {
    // If the applet is no longer visible on the page, we exit.

    try 
      {
	if (my_glogin.my_session != null)
	  {
	    my_glogin.my_session.logout();
	  }
      }
    catch (RemoteException ex) 
      {
      }

    System.exit(1);
  }

  public void enableButtons(boolean enabled)
    {
      connector.setEnabled(enabled);
      _quitButton.setEnabled(enabled);

    }
}  


/**
 *
 *
 *
 */
class LoginHandler implements ActionListener {

  protected glogin my_glogin;
  
  /**
   * Constructor
   */
  public LoginHandler(glogin _glogin) 
  {
    super();

    if (_glogin == null)
      {
	throw new IllegalArgumentException("LoginHandler Constructor: _glogin is null");
      }

    my_glogin = _glogin;
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == my_glogin.connector)
      {
	String uname = my_glogin.username.getText();
	String pword = my_glogin.passwd.getText();

	my_glogin.my_username = uname;
	my_glogin.my_passwd = pword;
	
	try
	  {
	    my_glogin.my_client = new iClient(my_glogin, my_glogin.my_server, uname, pword);
	  }
	catch (RemoteException ex)
	  {
	    //    System.err.println("RMI Error: Couldn't log in to server.\n" + ex.getMessage());
	    
	    if (my_glogin._infoD == null)
	      {
		my_glogin._infoD = new InfoDialog(my_glogin.my_frame,true,"","");
	      }
	    
	    my_glogin._infoD.setInfo("RMI Error: Couldn't log in to server.\n" + ex.getMessage());
	    Dimension d = my_glogin._infoD.getPreferredSize();
	    
	    my_glogin._infoD.setSize(d.width,d.height);
	    
	    my_glogin._infoD.show();
	    
	    my_glogin.connector.setEnabled(true);
	    my_glogin._quitButton.setEnabled(true);


	    return;
	  }
	catch (NullPointerException ex)
	  {
	    // System.err.println("Error: Didn't get server reference.\n\nPlease Quit and Restart.");
	    
	    if (my_glogin._infoD == null)
	      {
		my_glogin._infoD = new InfoDialog(my_glogin.my_frame,true,"","");
	      }
	    
	    my_glogin._infoD.setInfo("Error: Didn't get server reference.  Please Quit and Restart");
	    Dimension d = my_glogin._infoD.getPreferredSize();
	    
	    my_glogin._infoD.setSize(d.width,d.height);
	    
	    my_glogin._infoD.show();

	    my_glogin.connector.setEnabled(true);
	    my_glogin._quitButton.setEnabled(true);
	    
	    return;
	  }
	catch (Exception ex) 
	  {
	    if (my_glogin._infoD == null)
	      {
		my_glogin._infoD = new InfoDialog(my_glogin.my_frame,true,"","");
	      }
	    
	    my_glogin._infoD.setInfo("Error: "+ex.getMessage());
	    Dimension d = my_glogin._infoD.getPreferredSize();
	    
	    my_glogin._infoD.setSize(d.width,d.height);
	    
	    my_glogin._infoD.show();
	    
	    my_glogin.connector.setEnabled(true);
	    my_glogin._quitButton.setEnabled(true);

	    return;
	  }

	my_glogin.my_session = my_glogin.my_client.session;

	my_glogin.connector.setEnabled(false);
	my_glogin._quitButton.setEnabled(false);

	if (my_glogin.my_session != null) 
	  {
	    startSession(my_glogin.my_session);
	  }
	else 
	  {
	    //This means that the user was not able to log into the server properly.
	    
	    // We re-enable the "Login to server" button so that the user can try again.
	    my_glogin.connector.setEnabled(true);
	    my_glogin._quitButton.setEnabled(true);
	    // Why is this line here? I'm commenting it out.
	    //my_glogin.connector.setEnabled(false);
	  }
      }
    else if (e.getSource() == my_glogin._quitButton)
      {
	System.exit(1);
      }
  }

  public void startSession(Session s) 
  {
    if (s == null)
      {
	throw new IllegalArgumentException("Ganymede Error: Parameter for Session s is null");;
      }

    Session _session = s;

    gclient _client = new gclient(_session,my_glogin);

    /* At this point, all the login matters have been handled and we have
       a Session object in our hands.  We now instantiate the main client
       that will be used to interact with the Ganymede server.*/

    try 
      {
	// This will get the ball rolling.

	_client.start();

      }
    catch (Exception e)
      {
	// Any exception thrown by glclient will be handled here.
	System.err.println("Error starting client: " + e);
      }
  }
}

/**
 * iClient does all the heavy lifting to connect the server with the client, and
 * provides callbacks that the server can use to notify the client when something
 * happens.
 */

class iClient extends UnicastRemoteObject implements Client {

  protected glogin applet = null;
  protected Server server = null;
  protected String username = null;
  protected String password = null;
  protected Session session = null;

  /* -- */

  public iClient(glogin applet, Server server, String username, String password) throws RemoteException
  {
    super();

    // UnicastRemoteObject can throw RemoteException 

    this.applet = applet;
    this.server = server;
    this.username = username;
    this.password = password;

    System.err.println("Initializing iClient object");

    try
      {
	session = server.login(this);

	if (session == null)
	  {
	    System.err.println("Couldn't log in to server... bad username/password?");

	    if (applet._infoD == null)
	      {
		applet._infoD = new InfoDialog(applet.my_frame,true,"","");
	      }
	    
	    applet._infoD.setInfo("Couldn't log in to server... bad username/password?");
	    Dimension d = applet._infoD.getPreferredSize();

	    applet._infoD.setSize(d.width,d.height);
	
	    applet._infoD.show();
	  }
	else
	  {
	    System.out.println("logged in");
	  }
      }
    catch (RemoteException ex)
      {
	System.err.println("RMI Error: Couldn't log in to server.\n" + ex.getMessage());

	if (applet._infoD == null)
	  {
	    applet._infoD = new InfoDialog(applet.my_frame,true,"","");
	  }

	applet._infoD.setInfo("RMI Error: Couldn't log in to server.\n" + ex.getMessage());
	Dimension d = applet._infoD.getPreferredSize();

	applet._infoD.setSize(d.width,d.height);
	
	applet._infoD.show();

	//System.exit(0);
      }
    catch (NullPointerException ex)
      {
	System.err.println("Error: Didn't get server reference.  Exiting now.");
	//System.exit(0);

	if (applet._infoD == null)
	  {
	    applet._infoD = new InfoDialog(applet.my_frame,true,"","");
	  }

	applet._infoD.setInfo("Error: Didn't get server reference.  Exiting now.");
	Dimension d = applet._infoD.getPreferredSize();
	
	applet._infoD.setSize(d.width,d.height);
	
	applet._infoD.show();
	
      }
    catch (Exception ex)
      {
	System.err.println("Got some other exception: " + ex);
      }

    //    System.err.println("Got session");

/*    try
      {
	Type typeList[] = session.types();
	
	for (int i=0; i < typeList.length; i++)
	  {
	    System.err.println("Type: " + typeList[i]);
	  }
      }
    catch (Exception ex)
      {
	System.err.println("typecatch: " + ex);
      }
*/	
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
