/*
   GASH 2

   PasswordDBField.java

   The GANYMEDE object storage system.

   Created: 21 July 1997
   Release: $Name:  $
   Version: $Revision: 1.45 $
   Last Mod Date: $Date: 2000/08/25 21:54:14 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;
import MD5;
import MD5Crypt;
import jcrypt;

import arlut.csd.JDialog.*;

import com.jclark.xml.output.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 PasswordDBField

------------------------------------------------------------------------------*/

/**
 * <P>PasswordDBField is a subclass of {@link arlut.csd.ganymede.DBField DBField}
 * for the storage and handling of password
 * fields in the {@link arlut.csd.ganymede.DBStore DBStore} on the Ganymede
 * server.</P>
 *
 * <P>The Ganymede client talks to PasswordDBFields through the
 * {@link arlut.csd.ganymede.pass_field pass_field} RMI interface.</P> 
 *
 * <p>This class differs a bit from most subclasses of {@link
 * arlut.csd.ganymede.DBField DBField} in that the normal setValue()/getValue()
 * methods are non-functional.  Instead, there are special methods used to set or
 * access password information in crypted and non-crypted forms.</p>
 * 
 * <p>Crypted passwords are stored in the UNIX crypt() format.  See the
 * {@link jcrypt jcrypt} class for details on the crypt hashing.</p>
 *
 * <p>There are no methods provided to allow remote access to password
 * information..  server-side code must locally access the {@link
 * arlut.csd.ganymede.PasswordDBField#getUNIXCryptText()
 * getUNIXCryptText()} and {@link
 * arlut.csd.ganymede.PasswordDBField#getPlainText() getPlainText()}
 * methods to get access to the password information.  Generally, even
 * in that case, only crypted password information will be available.
 * If this password field was configured to store encrypted passwords
 * by way of its {@link arlut.csd.ganymede.DBObjectBaseField
 * DBObjectBaseField}, this password field will never emit() the
 * plaintext to disk.  Instead, the crypt()'ed password information
 * will be retained for user authentication.  The plaintext of the
 * password <b>may</b> be retained in memory for the purpose of
 * replicating to systems that do not use the UNIX crypt() format for
 * password hashing, but only on a temporary basis, for those
 * passwords whose plaintext was provided to the server during its
 * operation.  Basically, it's for custom builder tasks that
 * need to be able to provide the plaintext of a stored password
 * for replication to a system with an incompatible hash format.</P>
 *
 * @see arlut.csd.ganymede.BaseField#setCrypted(boolean)
 * @see arlut.csd.ganymede.BaseField#setPlainText(boolean)
 */

public class PasswordDBField extends DBField implements pass_field {

  static final boolean debug = false;

  // ---

  /**
   * <p>Traditional Unix crypt()'ed pass</p>
   */

  private String cryptedPass;

  /**
   * <p>The complex md5crypt()'ed password, as in
   * OpenBSD, FreeBSD, Linux PAM, etc.</p>
   */

  private String md5CryptPass;

  /**
   * <p>Plaintext password.. will never be saved to
   * disk if we have cryptedPass or md5CryptPass.</p>
   */

  private String uncryptedPass;

  /* -- */

  /**
   * <p>Receive constructor.  Used to create a PasswordDBField from a DBStore/DBJournal
   * DataInput stream.</p>
   */

  PasswordDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    value = null;
    this.owner = owner;
    this.definition = definition;
    receive(in);
  }

  /** 
   * <p>No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the {@link
   * arlut.csd.ganymede.DBObjectBase DBObjectBase} definition
   * indicates that a given field may be present, but for which no
   * value has been stored in the {@link arlut.csd.ganymede.DBStore
   * DBStore}.</p>
   *
   * <p>Used to provide the client a template for 'creating' this
   * field if so desired.</p> 
   */

  PasswordDBField(DBObject owner, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.definition = definition;
    
    value = null;
  }

  /**
   * Copy constructor.
   */

  public PasswordDBField(DBObject owner, PasswordDBField field)
  {
    this.owner = owner;
    definition = field.definition;

    cryptedPass = field.cryptedPass;
    md5CryptPass = field.md5CryptPass;
    uncryptedPass = field.uncryptedPass;
  }

  /**
   * <p>Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.</p>
   *
   * @see arlut.csd.ganymede.db_field
   */

  public boolean isDefined()
  {
    return (cryptedPass != null || md5CryptPass != null || uncryptedPass != null);
  }

  /**
   * <p>This method is used to mark a field as undefined when it is
   * checked out for editing.  Different subclasses of DBField will
   * implement this in different ways.  Any namespace values claimed
   * by the field will be released, and when the transaction is
   * committed, this field will be released.</p>
   */

  public synchronized ReturnVal setUndefined(boolean local)
  {
    if (isEditable(local))
      {
	cryptedPass = null;
	md5CryptPass = null;
	uncryptedPass = null;
	return null;
      }

    return Ganymede.createErrorDialog("Permissions Error",
				      "Don't have permission to clear this password field\n" +
				      getName());
  }

  /**
   * <p>Returns true if obj is a field with the same value(s) as
   * this one.</p>
   *
   * <p>This method is ok to be synchronized because it does not
   * call synchronized methods on any other object.</p>
   */

  public synchronized boolean equals(Object obj)
  {
    if (!(obj.getClass().equals(this.getClass())))
      {
	return false;
      }

    PasswordDBField f = (PasswordDBField) obj;

    return f.key().equals(this.key());
  }

  /**
   * <p>This method copies the current value of this DBField
   * to target.  The target DBField must be contained within a
   * checked-out DBEditObject in order to be updated.  Any actions
   * that would normally occur from a user manually setting a value
   * into the field will occur.</p>
   *
   * @param target The DBField to copy this field's contents to.
   * @param local If true, permissions checking is skipped.
   */

  public synchronized ReturnVal copyFieldTo(PasswordDBField target, boolean local)
  {
    if (!local)
      {
	if (!verifyReadPermission())
	  {
	    return Ganymede.createErrorDialog("Copy field error",
					      "Can't copy field " + getName() + ", no read privileges");
	  }
      }
	
    if (!target.isEditable(local))
      {
	return Ganymede.createErrorDialog("Copy field error",
					  "Can't copy field " + getName() + ", no write privileges");
      }

    target.cryptedPass = cryptedPass;
    target.uncryptedPass = uncryptedPass;

    return null;
  }

  /**
   * <p>Object value of DBField.  Used to represent value in value hashes.
   * Subclasses need to override this method in subclass.</p>
   */

  public Object key()
  {
    if (definition.isCrypted())
      {
	return cryptedPass;
      }
    else
      {
	return uncryptedPass;
      }
  }

  public Object clone()
  {
    return new PasswordDBField(owner, this);
  }

  void emit(DataOutput out) throws IOException
  {
    // note that we never save unencrypted passwords if we have
    // a crypted copy.  We'll keep unencrypted passwords around
    // in memory

    if (definition.isCrypted())
      {
	if (cryptedPass == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(cryptedPass);
	  }
      }

    // at file version 1.16 we replaced the old md5pass with md5CryptPass
    // at file version 1.16 we add md5CryptPass

    if (definition.isMD5Crypted())
      {
	if (md5CryptPass == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(md5CryptPass);
	  }
      }

    // only write out plaintext if we have no crypttext

    if (!definition.isCrypted() && !definition.isMD5Crypted())
      {
	if (uncryptedPass == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(uncryptedPass);
	  }
      }
  }

  void receive(DataInput in) throws IOException
  {
    // at file format 1.10, we were keeping both crypted and unecrypted
    // passwords on disk.  Since then, we have decided to only write
    // out encrypted passwords if we are using them.

    if ((Ganymede.db.file_major == 1) && (Ganymede.db.file_minor == 10))
      {
	cryptedPass = in.readUTF();

	if (cryptedPass.equals(""))
	  {
	    cryptedPass = null;
	  }
	
	uncryptedPass = in.readUTF();

	if (uncryptedPass.equals(""))
	  {
	    uncryptedPass = null;
	  }
      }
    else
      {
	if (definition.isCrypted())
	  {
	    cryptedPass = in.readUTF();

	    if (cryptedPass.equals(""))
	      {
		cryptedPass = null;
	      }

	    if ((Ganymede.db.file_major == 1) && (Ganymede.db.file_minor >= 13) &&
		(Ganymede.db.file_minor < 16))
	      {
		in.readUTF();	// skip old-style md5 pass
	      }
	  }
	
	if ((Ganymede.db.file_major >= 1) || (Ganymede.db.file_minor >= 16))
	  {
	    if (definition.isMD5Crypted())
	      {
		md5CryptPass = in.readUTF();
		
		if (md5CryptPass.equals(""))
		  {
		    md5CryptPass = null;
		  }
	      }
	    else
	      {
		md5CryptPass = null;
	      }
	  }

	if (definition.isCrypted() || definition.isMD5Crypted())
	  {
	    uncryptedPass = null;
	  }
	else
	  {
	    uncryptedPass = in.readUTF();

	    if (uncryptedPass.equals(""))
	      {
		uncryptedPass = null;
	      }

	    cryptedPass = null;
	    md5CryptPass = null;
	  }
      }
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().</p>
   */

  synchronized void emitXML(XMLDumpContext dump) throws IOException
  {
    dump.indent();

    dump.startElement(this.getXMLName());
    dump.startElement("password");
    
    if (uncryptedPass != null && 
	(dump.doDumpPlaintext() || (md5CryptPass == null && cryptedPass == null)))
      {
	dump.attribute("plaintext", uncryptedPass);
      }

    if (cryptedPass != null)
      {
	dump.attribute("crypt", cryptedPass);
      }
	
    if (md5CryptPass != null)
      {
	dump.attribute("md5crypt", cryptedPass);
      }

    dump.endElement("password");
    dump.endElement(this.getXMLName());
  }

  /**
   * <p>Standard {@link arlut.csd.ganymede.db_field db_field} method
   * to retrieve the value of this field.  Because we are holding sensitive
   * password information, this method always returns null.. we don't want
   * to make password values available to the client under any circumstances.
   */

  public Object getValue()
  {
    return null;
  }

  /** 
   * <p>Returns an Object carrying the value held in this field.</p>
   *
   * <p>This is intended to be used within the Ganymede server, it bypasses
   * the permissions checking that getValues() does.</p>
   *
   * <p>Note that this method will always return null, as you need to use
   * the special Password-specific value accessors to get access to the
   * password information in crypted or non-crypted form.</p>
   */

  public Object getValueLocal()
  {
    return null;
  }

  // ****
  //
  // type specific value accessors
  //
  // ****

  public synchronized String getValueString()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (cryptedPass != null || md5CryptPass != null || uncryptedPass != null)
      {
	StringBuffer result = new StringBuffer();

	result.append("< ");

	if (cryptedPass != null)
	  {
	    result.append("crypt ");
	  }

	if (md5CryptPass != null)
	  {
	    result.append("md5crypt ");
	  }

	if (uncryptedPass != null)
	  {
	    result.append("text ");
	  }

	result.append(">");

	return result.toString();
      }
    else
      {
	return null;
      }
  }

  /**
   * The default getValueString() encoding is acceptable.
   */

  public String getEncodingString()
  {
    return getValueString();
  }

  /**
   * <p>Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.</p>
   *
   * <p>If there is no change in the field, null will be returned.</p>
   */

  public String getDiffString(DBField orig)
  {
    PasswordDBField origP;

    /* -- */

    if (!(orig instanceof PasswordDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origP = (PasswordDBField) orig;

    if ((cryptedPass != origP.cryptedPass) || (uncryptedPass != origP.uncryptedPass))
      {
	return "\tPassword changed\n";
      }
    else
      {
	return null;
      }
  }

  // ****
  //
  // pass_field methods 
  //
  // ****

  /**
   * <p>Returns the maximum acceptable string length
   * for this field.</p>
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public int maxSize()
  {
    return definition.getMaxLength();
  }

  /**
   * <p>Returns the minimum acceptable string length
   * for this field.</p>
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public int minSize()
  {
    return definition.getMinLength();
  }

  /**
   * <p>Returns a string containing the list of acceptable characters.
   * If the string is null, it should be interpreted as meaning all
   * characters not listed in disallowedChars() are allowable by
   * default.</p>
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public String allowedChars()
  {
    return definition.getOKChars();
  }

  /**
   * <p>Returns a string containing the list of forbidden
   * characters for this field.  If the string is null,
   * it should be interpreted as meaning that no characters
   * are specifically disallowed.</p>
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public String disallowedChars()
  {
    return definition.getBadChars();
  }

  /**
   * <p>Convenience method to identify if a particular
   * character is acceptable in this field.</p>
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public boolean allowed(char c)
  {
    if (allowedChars() != null && (allowedChars().indexOf(c) == -1))
      {
	return false;
      }

    if (disallowedChars() != null && (disallowedChars().indexOf(c) != -1))
      {
	return false;
      }
    
    return true;
  }

  /**
   * <p>Returns true if the password stored in this field is hash-crypted.</p>
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public boolean crypted()
  {
    return (definition.isCrypted());
  }

  /**
   * <p>Verification method for comparing a plaintext entry with a crypted
   * value.</p>
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public synchronized boolean matchPlainText(String text)
  {
    String cryptedText;

    /* -- */
    
    if (text == null || !this.isDefined())
      {
	return false;
      }

    if (uncryptedPass != null)	// easiest
      {
	return text.equals(uncryptedPass);
      }
    else if (cryptedPass != null) // next-easiest
      {
	if (debug)
	  {
	    System.err.println("present crypted text == " + cryptedPass);

	    System.err.println("getSalt() == '" + getSalt() + "'");
	  }

	cryptedText = jcrypt.crypt(getSalt(), text);

	if (debug)
	  {
	    System.err.println("comparison crypted text == " + cryptedText);
	  }

	if (cryptedPass.equals(cryptedText))
	  {
	    // If we're set up to keep plaintext copies or our
	    // encrypted passwords, we're going to go ahead and make a
	    // note of the plaintext password we just matched to the
	    // crypt text.  This is really pretty funky, because this
	    // is being done outside of any transactional context, but
	    // we're really not changing the *content* of this
	    // password field, we're just remembering another thing
	    // about the password we already are keeping.. to wit, the
	    // actual plain text.  By doing this, Ganymede can accumulate
	    // plaintext copies of the passwords whenever anyone logs in
	    // to it (assuming that the schema is set up to have the user's
	    // password field keep a plaintext copy.)

	    if (definition.isPlainText())
	      {
		uncryptedPass = text;
	      }

	    // likewise, remember the MD5 version in case someone is going
	    // to want it

	    if (definition.isMD5Crypted() && (md5CryptPass == null))
	      {
		md5CryptPass = MD5Crypt.crypt(text);
	      }

	    return true;
	  }
	else
	  {
	    return false;
	  }
      }
    else if (md5CryptPass != null) // hardest/most expensive
      {
	if (debug)
	  {
	    System.err.println("present md5 hashed text == " + md5CryptPass);
	  }

	cryptedText = MD5Crypt.crypt(text, getMD5Salt());

	if (debug)
	  {
	    System.err.println("comparison crypted text == " + cryptedText);
	  }

	if (md5CryptPass.equals(cryptedText))
	  {
	    // If we're set up to keep plaintext copies or our
	    // encrypted passwords, we're going to go ahead and make a
	    // note of the plaintext password we just matched to the
	    // crypt text.  This is really pretty funky, because this
	    // is being done outside of any transactional context, but
	    // we're really not changing the *content* of this
	    // password field, we're just remembering another thing
	    // about the password we already are keeping.. to wit, the
	    // actual plain text.  By doing this, Ganymede can accumulate
	    // plaintext copies of the passwords whenever anyone logs in
	    // to it (assuming that the schema is set up to have the user's
	    // password field keep a plaintext copy.)

	    if (definition.isPlainText())
	      {
		uncryptedPass = text;
	      }

	    // likewise, remember the crypt() version in case someone
	    // is going to want it.

	    if (definition.isCrypted())
	      {
		cryptedPass = jcrypt.crypt(text);
	      }

	    return true;
	  }
	else
	  {
	    return false;
	  }
      }

    // should never get here

    return false;
  }

  /**
   * <p>Verification method for comparing a hashed entry with a hashed
   * test value.  If the stored password was hashed with the UNIX crypt()
   * algorithm, The salts for the stored and submitted values must match
   * in order for a comparison to be made, else an illegal argument
   * exception will be thrown.</p>
   *
   * <p>If the stored password was hashed with the MD5 algorithm, there
   * is no SALT to worry about.</p>
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public boolean matchCryptText(String text)
  {
    if (!definition.isCrypted() || cryptedPass == null)
      {
	return false;
      }

    if (!text.startsWith(getSalt()))
      {
	throw new IllegalArgumentException("bad salt");
      }
	
    return text.equals(cryptedPass);
  }

  /** 
   * <p>Verification method for comparing a pre-crypted OpenBSD-style
   * md5crypt()'ed entry with a crypted value.  The salts for the
   * stored and submitted values must match in order for a comparison
   * to be made, else an illegal argument exception will be
   * thrown.</p>
   * 
   * @see arlut.csd.ganymede.pass_field 
   */

  public boolean matchMD5CryptText(String text)
  {
    String salt = text;
    String magic = "$1$";

    if (salt.startsWith(magic))
      {
	salt = salt.substring(magic.length());
      }
    
    /* It stops at the first '$', max 8 chars */
    
    if (salt.indexOf('$') != -1)
      {
	salt = salt.substring(0, salt.indexOf('$'));
      }
    
    if (salt.length() > 8)
      {
	salt = salt.substring(0, 8);
      }

    if (!salt.equals(getMD5Salt()))
      {
	throw new IllegalArgumentException("bad salt");
      }

    if (md5CryptPass != null)
      {
	return text.equals(md5CryptPass);
      }
    else
      {
	return false;
      }
  }

  /**
   * <p>This server-side only method returns the UNIX-encrypted password text.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
   */

  public String getUNIXCryptText()
  {
    if (crypted())
      {
	return cryptedPass;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return jcrypt.crypt(uncryptedPass);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /** 
   * <p>This server-side only method returns the md5crypt()-encrypted
   * hashed password text.</p>
   *
   * <p>This method is never meant to be available remotely.</p> 
   */

  public String getMD5CryptText()
  {
    if (definition.isMD5Crypted() && md5CryptPass != null)
      {
	return md5CryptPass;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return MD5Crypt.crypt(uncryptedPass);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /**
   * <p>This server-side only method returns the plaintext password text,
   * if available.</p>
   */

  public String getPlainText()
  {
    return uncryptedPass;
  }

  /** 
   * <p>Method to obtain the SALT for a stored crypted password.  If
   * the client is going to submit a pre-crypted password for
   * comparison via matchCryptText(), it must be salted by the salt
   * returned by this method.</p>
   * 
   * <p>If the password is not stored in crypt() form, null will be
   * returned.</p> 
   * 
   * @see arlut.csd.ganymede.pass_field 
   */

  public String getSalt()
  {
    if (definition.isCrypted() && cryptedPass != null)
      {
	return cryptedPass.substring(0,2);
      }
    else
      {
	return null;
      }
  }

  /** 
   * <p>Method to obtain the SALT for a stored OpenBSD-style
   * md5crypt()'ed password.  If the client is going to submit a
   * pre-crypted password for comparison via matchMD5CryptText(), it
   * must be salted by the salt returned by this method.</p>
   *
   * <p>If the password is not stored in md5crypt() form,
   * null will be returned.</p>
   * 
   * @see arlut.csd.ganymede.pass_field 
   */

  public String getMD5Salt()
  {
    if (definition.isMD5Crypted() && md5CryptPass != null)
      {
	String salt = md5CryptPass;
	String magic = "$1$";

	if (salt.startsWith(magic))
	  {
	    salt = salt.substring(magic.length());
	  }
	
	/* It stops at the first '$', max 8 chars */
	
	if (salt.indexOf('$') != -1)
	  {
	    salt = salt.substring(0, salt.indexOf('$'));
	  }

	if (salt.length() > 8)
	  {
	    salt = salt.substring(0, 8);
	  }

	return salt;
      }
    else
      {
	return null;
      }
  }

  /**
   * <p>Sets the value of this field, if a scalar.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.</p> 
   */

  public ReturnVal setValue(Object value, boolean local, boolean noWizards)
  {
    throw new IllegalArgumentException("can't directly set the value on a password field");
  }

  /** 
   * <p>This method is used to set the password for this field,
   * crypting it in various ways if this password field is stored
   * crypted.</p>
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public ReturnVal setPlainTextPass(String text)
  {
    ReturnVal retVal;

    /* -- */

    retVal = verifyNewValue(text);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    retVal = ((DBEditObject) owner).finalizeSetValue(this, text);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    if (text == null)
      {
	md5CryptPass = null;
	cryptedPass = null;
	uncryptedPass = null;
	    
	if (debug)
	  {
	    System.err.println("PasswordDBField.setPlainTextPass(): Clearing password");
	  }

	return retVal;
      }

    // store a UNIX-crypted version, and store a naive MD5 hash version
    
    if (definition.isCrypted())
      {
	try
	  {
	    cryptedPass = jcrypt.crypt(text);
	  }
	finally
	  {
	    // see whether the schema editor has us trying to save
	    // plain text
		    
	    if (definition.isPlainText())
	      {
		uncryptedPass = text;
	      }
	    else
	      {
		uncryptedPass = null;
	      }
		    
	    if (debug)
	      {
		System.err.println("PasswordDBField.setPlainTextPass(): Setting plain text pass.. crypted = " + 
				   cryptedPass + ", plain = " + uncryptedPass);
	      }
	  }
      }
    else
      {
	cryptedPass = null;

	if (debug)
	  {
	    System.err.println("PasswordDBField.setPlainTextPass(): Clearing crypt");
	  }
      }

    // if they want an OpenBSD-style md5crypt() password generated/saved, do that

    if (definition.isMD5Crypted())
      {
	try
	  {
	    md5CryptPass = MD5Crypt.crypt(text);
	  }
	finally
	  {
	    // see whether the schema editor has us trying to save
	    // plain text
		    
	    if (definition.isPlainText())
	      {
		uncryptedPass = text;
	      }
	    else
	      {
		uncryptedPass = null;
	      }
		    
	    if (debug)
	      {
		System.err.println("PasswordDBField.setPlainTextPass(): Setting plain text pass.. MD5crypted = " +
				   md5CryptPass);
	      }
	  }
      }
    else
      {
	md5CryptPass = null;
	
	if (debug)
	  {
	    System.err.println("PasswordDBField.setPlainTextPass(): Clearing md5crypt");
	  }
      }

    uncryptedPass = text;

    return retVal;
  }

  /**
   * <p>This method is used to set a pre-crypted password for this field.</p>
   *
   * <p>This method will return an error dialog if this field does not store
   * passwords in UNIX crypted format.</p>
   *
   * <p>Because the UNIX crypt() hashing is not reversible, any MD5 and plain text
   * password information stored in this field will be lost.</p>
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public ReturnVal setCryptPass(String text)
  {
    ReturnVal retVal;

    /* -- */

    if (!isEditable(true))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "Don't have permission to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    if (!definition.isCrypted())
      {
	return Ganymede.createErrorDialog("Server: Error in PasswordDBField.setCryptTextPass()",
					  "Can't set a pre-crypted value into a plaintext-only password field");
      }

    retVal = ((DBEditObject)owner).finalizeSetValue(this, text);

    if (retVal == null || retVal.didSucceed())
      {
	// whenever the crypt password is directly set, we lose 
	// plaintext and alternate hashes

	if ((text == null) || (text.equals("")))
	  {
	    cryptedPass = null;
	  }
	else
	  {
	    cryptedPass = text;
	  }

	md5CryptPass = null;
	uncryptedPass = null;
      }

    return retVal;
  }

  /**
   * <p>This method is used to set a pre-crypted OpenBSD-style
   * MD5Crypt password for this field.  This method will return
   * false if this password field is not stored crypted.</p>
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public ReturnVal setMD5CryptedPass(String text)
  {
    ReturnVal retVal;

    /* -- */

    if (!isEditable(true))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "Don't have permission to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    if (!definition.isMD5Crypted())
      {
	return Ganymede.createErrorDialog("Server: Error in PasswordDBField.setMD5CryptTextPass()",
					  "Can't set a pre-crypted MD5Crypt value into a non-MD5Crypted password field");
      }

    if (text != null && (!text.startsWith("$1$") || (text.indexOf('$', 3) == -1)))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "setMD5CryptedPass() called with an improperly " +
					  "formatted OpenBSD-style password entry.");
      }

    retVal = ((DBEditObject)owner).finalizeSetValue(this, text);

    if (retVal == null || retVal.didSucceed())
      {
	// whenever the md5CryptPass password is directly set, we lose 
	// plaintext and alternate hashes

	if ((text == null) || (text.equals("")))
	  {
	    md5CryptPass = null;
	  }
	else
	  {
	    md5CryptPass = text;
	  }

	// clear alternate forms

	cryptedPass = null;
	uncryptedPass = null;
      }

    return retVal;
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) || (o instanceof String));
  }

  /**
   * Generally only for when we get a plaintext submission..
   */

  public ReturnVal verifyNewValue(Object o)
  {
    DBEditObject eObj;
    String s, s2;
    Vector v;
    boolean ok = true;

    /* -- */

    if (!isEditable(true))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "Don't have permission to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "Submitted value " + o + " is not a string!  Major client error while" +
					  " trying to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    if (o == null)
      {
	return null; // assume we can null out this field
      }

    s = (String) o;

    if (s.length() > maxSize())
      {
	// string too long

	return Ganymede.createErrorDialog("Password Field Error",
					  "Submitted password" +
					  " is too long for field " + 
					  getName() + " in object " +
					  owner.getLabel() +
					  ", which has a length limit of " + 
					  maxSize());
      }

    if (s.length() < minSize())
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "Submitted password" +
					  " is too short for field " + 
					  getName() + " in object " +
					  owner.getLabel() +
					  ", which has a minimum length of " + 
					  minSize());
      }
    
    if (allowedChars() != null)
      {
	String okChars = allowedChars();
	
	for (int i = 0; i < s.length(); i++)
	  {
	    if (okChars.indexOf(s.charAt(i)) == -1)
	      {
		return Ganymede.createErrorDialog("Password Field Error",
						  "Submitted password" +
						  " contains the unacceptable character '" +
						  s.charAt(i) + "' for field " +
						  getName() + " in object " +
						  owner.getLabel() + ".");
	      }
	  }
      }
    
    if (disallowedChars() != null)
      {
	String badChars = disallowedChars();
	
	for (int i = 0; i < s.length(); i++)
	  {
	    if (badChars.indexOf(s.charAt(i)) != -1)
	      {
		return Ganymede.createErrorDialog("Password Field Error",
						  "Submitted password" +
						  " contains the unacceptable character '" +
						  s.charAt(i) + "' for field " +
						  getName() + " in object " +
						  owner.getLabel() + ".");
	      }
	  }
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, s);
  }
}
