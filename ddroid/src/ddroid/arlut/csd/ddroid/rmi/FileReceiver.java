/*

   FileReceiver.java

   Server-side interface for the Client object.

   Created: 16 September 2000
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ddroid.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import arlut.csd.ddroid.common.ReturnVal;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    FileReceiver

------------------------------------------------------------------------------*/

/**
 * <p>Interface that can be used on the server or client to represent an end-point
 * for transmitting a file across the RMI link..</p>
 */

public interface FileReceiver extends Remote {

  /**
   * <p>This method is used to send chunks of a file, in order, to the
   * FileReceiver.  The FileReceiver can return a non-successful ReturnVal
   * if it wants to stop receiving the file.  A null return value
   * indicates success, keep sending.</p>
   */
  
  public ReturnVal sendBytes(byte[] bytes) throws RemoteException;

  /**
   * <p>This method is used to send chunks of a file, in order, to the
   * FileReceiver.  The FileReceiver can return a non-successful ReturnVal
   * if it wants to stop receiving the file.  A null return value
   * indicates success, keep sending.</p>
   */
  
  public ReturnVal sendBytes(byte[] bytes, int offset, int len) throws RemoteException;

  /**
   * <p>This method is called to notify the FileReceiver that no more
   * of the file will be transmitted.  The boolean parameter will
   * be true if the file was completely sent, or false if the transmission
   * is being aborted by the sender for some reason.</p>
   *
   * @return Returns true if the FileReceiver successfully received
   * the file in its entirety.
   */
  
  public ReturnVal end(boolean completed) throws RemoteException;
}
