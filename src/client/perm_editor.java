/*

   perm_editor.java

   perm_editor is a JTable-based permissions editor for Ganymede.
   
   Created: 18 November 1998
   Version: $Revision: 1.19 $ %D%
   Module By: Brian O'Mara omara@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.rmi.RemoteException;

import jdj.PackageResources; 

import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.JSeparator;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import javax.swing.plaf.*;

/*------------------------------------------------------------------------------
                                                                           class 
                                                                     perm_editor

------------------------------------------------------------------------------*/

class perm_editor extends JDialog implements ActionListener, Runnable {

  boolean debug = false;

  String DialogTitle;
  boolean enabled;
  boolean viewOnly; // = !enabled
  boolean isActive = true;
  Session session;
  perm_field permField;
  PermMatrix matrix, templateMatrix;
  DefaultMutableTreeNode rowRootNode;
  gclient gc;

  JButton OkButton = new JButton ("Ok");
  JButton CancelButton = new JButton("Cancel");
  JButton ExpandButton = new JButton ("Expand All");
  JButton CollapseButton = new JButton("Collapse All");
 
  boolean keepLoading = true;

  JProgressBar progressBar;
  JDialog progressDialog;
  JButton cancelLoadingButton;
  JScrollPane edit_pane;
  JTreeTable treeTable;
  JTree tree;

  // Layout Stuff 
  JPanel 
    Base_Panel,
    Choice_Buttons,
    Expansion_Buttons,
    All_Buttons,
    waitPanel;

  /* -- */

  /**
   *
   * Constructor
   *
   * @param permField Who do we talk to to get the permissions?
   * @param enabled If false, it will not be possible to edit permissions, will just display
   * @param gc The gclient that connects us to the client-side schema caches
   * @param parent The frame we are attaching this dialog to
   * @param DialogTitle The title for this dialog box
   * @param justShowUser If true, this permissions editor will only show the user object.
   * This is used when editing the 'self permissions' object in the database.
   *
   */

  public perm_editor (perm_field permField, 
		       boolean enabled, gclient gc,
		       Frame parent, String DialogTitle)
  {

    super(parent, DialogTitle, true); // the boolean value is to make the dialog modal
    


    // Main constructor for the perm_editor window      

    this.session = session;
    this.permField = permField;
    this.enabled = enabled;
    this.gc = gc;
    this.viewOnly = !enabled;
    this.DialogTitle = DialogTitle;

    if (!debug)
      {
	this.debug = gc.debug;
      }


    addWindowListener(new WindowAdapter()
					  {
					    public void WindowClosing(WindowEvent e)
					      {
						myshow(false);
					      }
					  });

    // Change the tree icons/font to match the gclient. -Better place to put this?
    UIManager.put("Tree.leafIcon", new ImageIcon(PackageResources.getImageResource(this, "i043.gif", getClass())));
    UIManager.put("Tree.openIcon", new ImageIcon(PackageResources.getImageResource(this, "openfolder.gif", getClass())));
    UIManager.put("Tree.closedIcon", new ImageIcon(PackageResources.getImageResource(this, "folder.gif", getClass())));
    UIManager.put("Tree.expandedIcon", new ImageIcon(PackageResources.getImageResource(this, "minus.gif", getClass())));
    UIManager.put("Tree.collapsedIcon", new ImageIcon(PackageResources.getImageResource(this, "plus.gif", getClass())));
    UIManager.put("Tree.font", new Font("SansSerif", Font.BOLD, 12));


    // Set up progress bar stuff

    progressBar = new JProgressBar();
    progressBar.setBorder(gc.emptyBorder10);
    progressBar.setMinimum(0);
    progressBar.setMaximum(29);
    progressBar.setValue(0);

    JPanel progressBarPanel = new JPanel();
    progressBarPanel.add(progressBar);

    waitPanel = new JPanel(new BorderLayout(5, 5));
    waitPanel.add("Center", progressBarPanel);
    waitPanel.add("South", new JSeparator());

    progressDialog = new JDialog(gc, "Loading permission editor", false);    
    progressDialog.getContentPane().setLayout(new BorderLayout(5, 5));
    progressDialog.getContentPane().add("Center", waitPanel);
    
    cancelLoadingButton = new JButton("Cancel");
    cancelLoadingButton.addActionListener(new ActionListener()
					  {
					    public void actionPerformed(ActionEvent e)
					      {
						keepLoading = false;
					      }
					  });

    JPanel cancelButtonPanel = new JPanel();
    cancelButtonPanel.add(cancelLoadingButton);
    progressDialog.getContentPane().add("South", cancelButtonPanel);

    JLabel loadingLabel = new JLabel("Loading permissions editor", SwingConstants.CENTER);
    loadingLabel.setBorder(gc.emptyBorder10);
    progressDialog.getContentPane().add("North", loadingLabel);

    Rectangle b = gc.getBounds();
    progressDialog.setLocation(b.width/2 + b.x - 75, b.height/2 + b.y - 50);
    progressDialog.pack();
    progressDialog.setVisible(true);


    Thread t = new Thread(this);
    t.start();
  }

  public void run() 
  {
    System.out.println("Starting thread");


    // Get a quick dump of the permission field's state

    try
      {
	this.matrix = permField.getMatrix();
	this.templateMatrix = permField.getTemplateMatrix();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't get permission matrix\n" + ex.getMessage());
      }
    
    if (debug)
      {
	System.out.println("Starting Permissions Editor Initialization");
      }

    // OK/Cancel buttons
    
    OkButton.setBackground(Color.lightGray);  
    CancelButton.setBackground(Color.lightGray);
    OkButton.addActionListener(this);
    CancelButton.addActionListener(this);
 
    Choice_Buttons = new JPanel(); 
    Choice_Buttons.setLayout(new GridLayout(1,2));
    Choice_Buttons.setBorder(new EmptyBorder(new Insets(5,5,5,5)));
    Choice_Buttons.add(OkButton);
    Choice_Buttons.add(CancelButton);


    // Expand/Collapse Buttons

    ExpandButton.setBackground(Color.lightGray);
    CollapseButton.setBackground(Color.lightGray);
    ExpandButton.addActionListener(new ActionListener()
					  {
					    public void actionPerformed(ActionEvent e)
					      {
						for (Enumeration enum = (rowRootNode.children()); enum.hasMoreElements();) 
						  {
						    DefaultMutableTreeNode node = (DefaultMutableTreeNode)enum.nextElement();
						    TreePath path = new TreePath(node.getPath());
						    tree.expandPath(path);
						  } 
					      }
					  });
 
    CollapseButton.addActionListener(new ActionListener()
					  {
					    public void actionPerformed(ActionEvent e)
					      {
						for (Enumeration enum = (rowRootNode.children()); enum.hasMoreElements();) 
						  {
						    DefaultMutableTreeNode node = (DefaultMutableTreeNode)enum.nextElement();
						    TreePath path = new TreePath(node.getPath());
						    tree.collapsePath(path);
						  } 
					      }
					  });

    Expansion_Buttons = new JPanel(); 
    Expansion_Buttons.setBorder(new EmptyBorder(new Insets(5,5,5,5)));
    Expansion_Buttons.setLayout(new GridLayout(1,2));
    Expansion_Buttons.add(ExpandButton);
    Expansion_Buttons.add(CollapseButton);


    // Group Expansion buttons and Choice buttons together

    All_Buttons = new JPanel();
    All_Buttons.setLayout(new BorderLayout());
    All_Buttons.add("West", Expansion_Buttons);
    All_Buttons.add("East", Choice_Buttons);


    progressBar.setValue(2);
    

    // Set up the TreeTable

    try 
      {
	// Build the tree structure, returning rowRootNode as root
	rowRootNode = initRowTree();

	if (rowRootNode == null)
	  {
	    this.dispose();
	    return;
	  }
	
	System.out.println("rowVector initialized");

	if (debug)
	  {
	    System.err.println("got it, it " + (rowRootNode == null ? "is " : "isn't ") + "equal to null");
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Caught RemoteException" + ex);
      }
    
    getContentPane().remove(waitPanel);
    getContentPane().setLayout(new BorderLayout());


    // The model for the JTreeTable is a PermEditorModel

    TreeTableModel permEditor = new PermEditorModel(rowRootNode, viewOnly);
    treeTable = new JTreeTable(permEditor);


    // Get the tree part of the JTreeTable- useful later

    tree = treeTable.getTree();


    // Expand all visible base nodes on startup 

    for (Enumeration e = (rowRootNode.children()); e.hasMoreElements();) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
      PermRow myRow = (PermRow)node.getUserObject();
      if (myRow.isVisible()) {
	TreePath path = new TreePath(node.getPath());
	tree.expandPath(path);
      }
    } 


    // Set default column widths
 
     TableColumn column = null;
     for (int i = 0; i < 5; i++) {
       column = treeTable.getColumnModel().getColumn(i);
       if (i == 0) {
 	column.setPreferredWidth(205); 
       } else {
 	column.setPreferredWidth(10);
       }
     }


     // Uses custom renderer for Booleans

     treeTable.setDefaultRenderer(Boolean.class, new BoolRenderer((TreeTableModelAdapter)treeTable.getModel(), viewOnly)); 

    // leave column reordering enabled for now to make sure all is well...
    // Can disable it later- it's not really useful in this 
    // application.

    //    table.getTableHeader().setReorderingAllowed(false); 


    edit_pane = new JScrollPane(treeTable);
    edit_pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    getContentPane().setBackground(Color.white);

    TitledBorder border = new TitledBorder(new EtchedBorder(), this.DialogTitle);
    border.setTitlePosition(TitledBorder.TOP);
    border.setTitleJustification(TitledBorder.LEFT);

    Base_Panel = new JPanel(); 
    Base_Panel.setBorder(border);
    Base_Panel.setLayout(new BorderLayout());
    Base_Panel.setBackground(Color.white);
    Base_Panel.add("Center", edit_pane);
    Base_Panel.add("South", All_Buttons);

    getContentPane().add("Center", Base_Panel);
    gc.setWaitCursor();
    
    progressDialog.setVisible(false);    
    progressDialog.dispose();
    
    this.myshow(true);
    gc.setNormalCursor();
  }
  
  /**
   * This method will create a tree of row info that will be used
   * to store the permissions values for the base and basefields.
   * It returns a DefaultMutableTreeNode as the root of the tree structure
   */
  
  private DefaultMutableTreeNode initRowTree() throws RemoteException 
  {
    PermEntry entry, templateEntry;
    BaseDump base;
    FieldTemplate template;
    boolean create, view, edit, delete;
    boolean createOK, viewOK, editOK, deleteOK;
    boolean baseCreate, baseView, baseEdit, baseDelete;
    Vector fields;
    boolean visibleField;
    Enumeration enum;
    short id;
    String name;
    DefaultMutableTreeNode rootNode, baseNode, fieldNode;

    // Initialize a "blank" node for the root
    boolean[] rootPermAry = {false,false,false,false};
    boolean[] rootPermOKAry = {false,false,false,false};

    rootNode = 
      new DefaultMutableTreeNode(new PermRow(null, null, rootPermAry, rootPermOKAry, false));

    /* -- */

    // get a list of base types from the gclient.. we really just care
    // about their names and id's.

    enum = gc.getBaseList().elements();

    progressBar.setMaximum(gc.getBaseList().size() + 3);
    progressBar.setValue(3);

    int i = 3;

    while (enum.hasMoreElements())
      {
	if (! keepLoading)
	  {
	    // Cancel was pushed.
	    stopLoading();
	    return (DefaultMutableTreeNode)null;
	  }

	progressBar.setValue(i++);

	base = (BaseDump) enum.nextElement();

	id = base.getTypeID();
	name = base.getName();

	if (debug)
	  {
	    System.err.println("init_hash: processing " + name);
	  }


	// retrieve the current permissions for this object type 
	// (The following logic is unchanged from the original perm editor)

	entry = matrix.getPerm(id);
	
	if (entry == null)
	  {
	    baseCreate = baseView = baseEdit = baseDelete = false;
	  }
	else
	  {
	    baseCreate = entry.isCreatable();
	    baseView = entry.isVisible();
	    baseEdit = entry.isEditable();
	    baseDelete = entry.isDeletable();
	  }

	if (templateMatrix != null)
	  {
	    templateEntry = templateMatrix.getPerm(id);

	    // if permissions already exist on an entry, we'll allow
	    // it to be reduced.. it's conceivable that an admin might
	    // have authority to edit a role that he or she wouldn't
	    // normally have the authority to delegate.. of course,
	    // this wouldn't make a lot of sense since in theory the
	    // admin could just click on the delegatable checkbox and
	    // give him/herself the ability to delegate these perms,
	    // but we might conceivably give an admin permission to
	    // edit this perm field but not the delegatable checkbox
	    // in this object.

	    if (templateEntry == null)
	      {
		createOK = baseCreate;
		viewOK = baseView;
		editOK = baseEdit;
		deleteOK = baseDelete;
	      }
	    else
	      {
		createOK = templateEntry.isCreatable() || baseCreate;
		viewOK = templateEntry.isVisible() || baseView;
		editOK = templateEntry.isEditable() || baseEdit;
		deleteOK = templateEntry.isDeletable() || baseDelete;
	      }
	  }
	else
	  {
	    createOK = viewOK = editOK = deleteOK = true;
	  }


	// Initialize a PermRow object for this base and
	// add it to the tree structure 
	boolean[] basePermBitsAry = {baseView, baseCreate, baseEdit, baseDelete};
 	boolean[] basePermBitsOKAry = {viewOK, createOK, editOK, deleteOK};

	PermRow	basePermRow = new PermRow(base, null, basePermBitsAry, basePermBitsOKAry, enabled);

	baseNode= new DefaultMutableTreeNode(basePermRow);
	rootNode.add(baseNode);


	// Now go through the fields

	visibleField = baseView;

	fields = (Vector) gc.getTemplateVector(id);

	for (int j=0; fields != null && (j < fields.size()); j++) 
	  {
	    template = (FieldTemplate) fields.elementAt(j);


	    // we don't want to show built-in fields

	    if (template.isBuiltIn())
	      {
		continue;
	      }


	    // get the permission set for this field
	    // (The following logic is unchanged from the original perm editor)

	    entry = matrix.getPerm(id, template.getID());

	    if (entry == null)
	      {
		// if no permission is explicitly recorded for this
		// field, use the record for the base

		create = baseCreate;
		view = baseView;
		edit = baseEdit;
		delete = baseDelete;
	      }
	    else 
	      {
		create = entry.isCreatable();
		view = entry.isVisible();
		edit = entry.isEditable();
		delete = entry.isDeletable();
	      }

	    if (templateMatrix != null)
	      {
		templateEntry = templateMatrix.getPerm(id, template.getID());

		// if permissions already exist on an entry, we'll allow
		// it to be reduced.. it's conceivable that an admin might
		// have authority to edit a role that he or she wouldn't
		// normally have the authority to delegate.. of course,
		// this wouldn't make a lot of sense since in theory the
		// admin could just click on the delegatable checkbox and
		// give him/herself the ability to delegate these perms,
		// but we might conceivably give an admin permission to
		// edit this perm field but not the delegatable checkbox
		// in this object.
		
		if (templateEntry == null)
		  {
		    createOK = create;
		    viewOK = view;
		    editOK = edit;
		    deleteOK = delete;
		  }
		else
		  {
		    createOK = templateEntry.isCreatable() || create;
		    viewOK = templateEntry.isVisible() || view;
		    editOK = templateEntry.isEditable() || edit;
		    deleteOK = templateEntry.isDeletable() || delete;
		  }
	      }
	    else
	      {
		createOK = viewOK = editOK = deleteOK = true;
	      }


	    // Initialize a PermRow object for this field and
	    // add it to the tree structure 

	    boolean[] fieldPermBitsAry = {view, create, edit, delete};
	    boolean[] fieldPermBitsOKAry = {viewOK, createOK, editOK, deleteOK};

	    PermRow templatePermRow = new PermRow(base, template, fieldPermBitsAry, fieldPermBitsOKAry, visibleField);
	    
	    fieldNode = new DefaultMutableTreeNode(templatePermRow);
	    baseNode.add(fieldNode);
	  }
      }
    
    return rootNode;
  }

  private void stopLoading()
  {
    if (progressDialog != null)
      {
	progressDialog.setVisible(false);
	progressDialog.dispose();
      }

    this.dispose();
  }

 
 
 
  /**
   *
   * Method to pop-up/pop-down the perm_editor
   *
   */
  
  public void myshow(boolean truth_value)
  {
    if (truth_value)
      {
	setSize(550,550);
      } else {
	isActive = false;
      }

    setVisible(truth_value);
  }

  public boolean isActiveEditor() {
    return isActive;
  }
  
  public void actionPerformed(ActionEvent e)
  {
    PermRow ref;
    short baseid;
    BaseDump bd;
    String baseName, templateName;
    FieldTemplate template;
    boolean view, edit, create, delete;
   
    /* -- */

    /* This method will either simply exit the perm_editor [if "ok" is 
     * the source], or will retore the original permission values [if the
     * "cancel" button is pushed]
     */
	
    if (e.getSource() == OkButton) 
      {
	if (debug)
	  {
	    System.out.println("Ok was pushed");
	  }
	
	Enumeration enum = rowRootNode.preorderEnumeration();
	
	while (enum.hasMoreElements()) {
	  DefaultMutableTreeNode node = (DefaultMutableTreeNode)enum.nextElement();
	  ref = (PermRow)node.getUserObject();
	  
	  if (!ref.isChanged()) {
	    continue;
	    
	  } else {
	    
	    gc.somethingChanged();	    
	    
	    if (ref.isBase()) {
	      bd = (BaseDump) ref.getReference();
	      baseid = bd.getTypeID();
	      baseName = bd.getName();
	      
	      view = ref.isVisible();
	      create = ref.isCreatable();
	      edit = ref.isEditable();
	      delete = ref.isDeletable();
	      
	      if (debug)
		{
		  System.err.println("setting base perms for " + baseName+ " ("+baseid+")");
		}
	      
	      
	      try
		{ 
		  // note that PermEntry bit order (V,E,C,D) is different from that used 
		  // everywhere else (V,C,E,D). Should see if PermEntry can be changed for consistency
		  
		  gc.handleReturnVal(permField.setPerm(baseid, new PermEntry (view, edit, create, delete)));
		}
	      catch (RemoteException ex)
		{
		  throw new RuntimeException("Caught RemoteException" + ex);
		}
	      
	    } 
	    else  {
	      template = (FieldTemplate) ref.getReference();
	      templateName = template.getName();
	      
	      view = ref.isVisible();
	      create = ref.isCreatable();
	      edit = ref.isEditable();
	      delete = ref.isDeletable();
	      
	      if (debug)
		{
		  System.err.println("setting basefield perms for field " + templateName);
		}
	      
	      
	      try
		{
		  // note that PermEntry bit order (V,E,C,D) is different from that used 
		  // everywhere else (V,C,E,D). Should see if PermEntry can be changed for consistency
		  
		  gc.handleReturnVal(permField.setPerm(template.getBaseID(), template.getID(), 
						       new PermEntry (view, edit, create, delete)));
		}
	      catch (RemoteException ex)
		{
		  throw new RuntimeException("Caught RemoteException" + ex);
		}
	      
	    }
	  }
	}	      
	
	myshow(false);
	return;
	
      }
    else 
      {
	if (debug)
	  {
	    System.out.println("Cancel was pushed");
	  }
	
	myshow(false);
	return;
      } 
  }
   
} 

/* BoolRenderer

 Provides custom renderer for Boolean class. 
*/

class BoolRenderer extends JCheckBox
    implements TableCellRenderer {
  
  TreeTableModelAdapter tntModel;
  boolean viewOnly;

  // This is the "-" which replaces the checkbox
  ImageIcon noAccess = 
    new ImageIcon(PackageResources.getImageResource(this, "noaccess.gif", getClass()));

  public BoolRenderer(TreeTableModelAdapter tntModel, boolean viewOnly) {
    super();
    this.tntModel = tntModel;
    this.viewOnly = viewOnly;
    setOpaque(true); //do this for background to show up.
  }
  
  public Component getTableCellRendererComponent(
						 JTable table, Object value, 
						 boolean isSelected, boolean hasFocus,
						 int row, int column) {
    
    setBackground(Color.white);

    DefaultMutableTreeNode node = (DefaultMutableTreeNode)tntModel.nodeForRow(row);
    PermRow myRow = (PermRow)node.getUserObject(); 
    Boolean selected = (Boolean)table.getValueAt(row,column);
    boolean enabled = myRow.isEnabled();
    
    String columnName = table.getColumnName(column);

    



    // Check if viewOK, etc. If not, put
    // noAccess icon ("-") instead of checkbox

    // Note that this just shows what to display. Whether you can select the box or not
    // is controlled in the isCellEditable() method of PermEditorModel.

    // Visible Column    
    if (columnName.equals("Visible")) {

      if (myRow.canSeeView()) {
	setIcon(null);
	setEnabled(viewOnly ? false : enabled);
	setSelected(selected.booleanValue());	
      } else {
	setIcon(noAccess);
	setEnabled(true);
      }

    }
    
    // Creatable Column
    else if (columnName.equals("Creatable")) {

      if (myRow.canSeeCreate()) {
	setIcon(null);
	setEnabled((viewOnly || !myRow.isVisible()) ? false : enabled);
	setSelected(selected.booleanValue());	
      } else {
	setIcon(noAccess);
	setEnabled(true);
      }

    }
  
    // Editable Column
    else if (columnName.equals("Editable")) {

      if (myRow.canSeeEdit()) {
	setIcon(null);
	setEnabled((viewOnly || !myRow.isVisible()) ? false : enabled);
	setSelected(selected.booleanValue());	
      } else {
	setIcon(noAccess);
	setEnabled(true);
      }

    }

    // Deletable Column
    else if (columnName.equals("Deletable")){

      if (myRow.canSeeDelete() && myRow.isBase()) {
	setIcon(null);
	setEnabled((viewOnly || !myRow.isVisible()) ? false : enabled);
	setSelected(selected.booleanValue());	
      } else {
	setIcon(noAccess);
	setEnabled(true);
      }

    }
  
    // Center checkbox
    setHorizontalAlignment(CENTER);   

    return this;
  }
}


class PermRow {

  static final int VISIBLE = 0;
  static final int CREATABLE = 1;
  static final int EDITABLE = 2;
  static final int DELETABLE = 3;

  private Object reference;
  private boolean[] permBitsAry;
  private boolean[] permBitsOKAry;
  private boolean enabled;
  private boolean changed;
  private String name;


  //
  // Constructor  
  //

  public PermRow(BaseDump base, FieldTemplate field, boolean[] permBitsAry, boolean[] permBitsOKAry, boolean enabled) {

    this.permBitsAry = permBitsAry;
    this.permBitsOKAry = permBitsOKAry;
    this.enabled = enabled;


    // set up initial values for name and reference type 

    if (base == null) {
      reference = null;
      name = "Root";
    }
    else {
      if (field == null) {   
	reference = base;
	name = base.getName();
      } else {
	reference = field;
	name = field.getName();
      }
    }
    
  }
  
  
  //
  // Methods to check and set if row enabled
  //
  public boolean isEnabled() {
    return enabled;
  }
  
  public void setEnabled(Boolean value) {
    enabled = value.booleanValue();
  }

  public String toString() {
    return name;
  } 

  //
  // Methods to check and set if row has changed
  //
  public boolean isChanged() {
    return changed;
  }
  
  public void setChanged(boolean value) {
    changed = value;
  }
  
  //
  // Methods to check and set values of perm bits
  //
  public boolean isVisible() {
    return permBitsAry[VISIBLE];
  }

  public boolean isCreatable() {
    return permBitsAry[CREATABLE];
  }

  public boolean isEditable() {
    return permBitsAry[EDITABLE];
  }

  public boolean isDeletable() {
    return permBitsAry[DELETABLE];
  }
  

  public void setVisible(Boolean value) {
    permBitsAry[VISIBLE] = value.booleanValue();
  }
  
  public void setCreatable(Boolean value) {
    permBitsAry[CREATABLE] = value.booleanValue();
  }
  
  public void setEditable(Boolean value) {
    permBitsAry[EDITABLE] = value.booleanValue();
  }

  public void setDeletable(Boolean value) {
    permBitsAry[DELETABLE] = value.booleanValue();
  }

  //
  // Methods to determine if you see checkbox or "X"
  //
  public boolean canSeeView() {
    return permBitsOKAry[VISIBLE];
  }

  public boolean canSeeCreate() {
    return permBitsOKAry[CREATABLE];
  }

  public boolean canSeeEdit() {
    return permBitsOKAry[EDITABLE];
  }

  public boolean canSeeDelete() {
    return permBitsOKAry[DELETABLE];
  }

  // Returns the current base or field object
  public Object getReference() {
    return reference;
  }

  // Returns if the current row is dealing w/ base or field
  public boolean isBase() {
    if (reference instanceof BaseDump)    
      return true;
    else
      return false;
  }

}

class PermEditorModel extends AbstractTreeTableModel implements TreeTableModel {
  
  static final int NAME = 0;
  static final int VISIBLE = 1;
  static final int CREATABLE = 2;
  static final int EDITABLE = 3;
  static final int DELETABLE = 4;
  boolean viewOnly;
  
  // Names of the columns.
  static protected String[]  cNames =  {"Name", 
					"Visible",
					"Creatable",
					"Editable",
					"Deletable"};
  
  // Types of the columns.
  static protected Class[]  cTypes = {TreeTableModel.class, Boolean.class, Boolean.class, Boolean.class, Boolean.class};
  
  public PermEditorModel(DefaultMutableTreeNode root, boolean viewOnly) {
    super(root); 
    this.viewOnly = viewOnly;
  }
  
  //
  // The TreeModel interface
  //
  public int getChildCount(Object node) { 
    return ((DefaultMutableTreeNode)node).getChildCount();
  }
  
  public Object getChild(Object node, int i) { 
    return ((DefaultMutableTreeNode)node).getChildAt(i);
  }
  
  public boolean isLeaf(Object node) { 
    return ((DefaultMutableTreeNode)node).isLeaf();
  }
  
  //
  //  The TreeTableNode interface. 
  //
  public int getColumnCount() {
    return cNames.length;
  }
  
  public String getColumnName(int column) {
    return cNames[column];
  }
  
  public Class getColumnClass(int column) {
    return cTypes[column];
  }
  
  public Object getValueAt(Object node, int column) {
    PermRow myRow = (PermRow)((DefaultMutableTreeNode)node).getUserObject(); 
    
    switch(column) {
    case 1:
      return new Boolean(myRow.isVisible());
    case 2:
      return new Boolean(myRow.isCreatable());
    case 3:
      return new Boolean(myRow.isEditable());
    case 4:
      return new Boolean(myRow.isDeletable());
    }
    return null; 
  }
  
  public boolean isCellEditable(Object node, int col) {
    PermRow myRow = (PermRow)((DefaultMutableTreeNode)node).getUserObject(); 
    
    // tree column is always editable (expansion and contraction)
    if (getColumnClass(col) == TreeTableModel.class)  
      return true;
    
    // If we entered from gclient "View Permissions" button, then can't edit
    if (viewOnly) 
      return false;
    
    // disabled checkbox is not editable, also bases if Visible base not checked
    if (!myRow.isEnabled() || ((myRow.isBase()) && (!myRow.isVisible()) && (col != VISIBLE)))  
      return false;
    
    // otherwise, if it's not a "-", it is editable  
    else { 
      
      switch(col) {
	
      case NAME:
	return true;
	
      case VISIBLE:
	if (myRow.canSeeView()) 
	  return true;
	break;
	
      case CREATABLE:
	if (myRow.canSeeCreate()) 
	  return true;
	break;	  
	
      case EDITABLE:
	if (myRow.canSeeEdit()) 
	  return true;
	break;
	
      case DELETABLE:
	if (myRow.canSeeDelete() && myRow.isBase()) 
	  return true;
	break;
      }
      return false;
    }
  }
  
  public void setValueAt(Object value, Object node, int col) {
    PermRow myRow = (PermRow)((DefaultMutableTreeNode)node).getUserObject(); 
    
    switch(col) {
      
    case VISIBLE:
      myRow.setVisible((Boolean)value);

      // Turning off base Visible turns off all other bases too. 
      if (((Boolean)value).booleanValue() == false) {

	// We don't want to programmatically modify a checkbox 
	// that we can't manually modify (noaccess fields). 
	if (myRow.canSeeCreate()) 
	  myRow.setCreatable((Boolean)value);
	if (myRow.canSeeEdit())
	  myRow.setEditable((Boolean)value);
	if (myRow.canSeeDelete())
	  myRow.setDeletable((Boolean)value);
      }
      myRow.setChanged(true);
      
      
      // If making a base selection
      // update children too 
      
      if (myRow.isBase()) {
	setBaseChildren(node, VISIBLE, value);
	
	
	// Take care of visibility of creatable
	// and editable children too
	
	if (myRow.isCreatable()) {
	  setBaseChildren(node, CREATABLE, value);
	}
	
	if (myRow.isEditable()) {
	  setBaseChildren(node, EDITABLE, value);
	}
	
      }
      break;
      
    case CREATABLE:
      myRow.setCreatable((Boolean)value);
      myRow.setChanged(true);
 
      // If base, update children too
      if ((myRow.isBase()) && (myRow.isVisible())) {
	setBaseChildren(node, CREATABLE, value);
      }
      break;
      
    case EDITABLE:
      myRow.setEditable((Boolean)value);
      myRow.setChanged(true);

      // If base, update children too      
      if ((myRow.isBase()) && (myRow.isVisible())) {
	setBaseChildren(node, EDITABLE, value);
      }
      break;
      
    case DELETABLE:
      myRow.setDeletable((Boolean)value);
      myRow.setChanged(true);
      
      // No update of children for deletable
      break;
    }
    
    // Update table to reflect these changes
    
    TreeNode[] path = ((DefaultMutableTreeNode)node).getPath();
    fireTreeNodesChanged(PermEditorModel.this, path, null, null);    
  }
  
  
  public void setBaseChildren(Object node, int col, Object value) {
    
    for (Enumeration e = ((DefaultMutableTreeNode)node).children(); e.hasMoreElements();) {
      
      PermRow myRow = (PermRow)((DefaultMutableTreeNode)e.nextElement()).getUserObject(); 
      
      switch(col) {
	
      case VISIBLE:
	
	// If checkbox not an "X", update it
	if (myRow.canSeeView()) {
	  myRow.setVisible((Boolean)value);	    
	  myRow.setEnabled((Boolean)value);
	  
	  // If View base was toggled to false
	  // then all children in each col are set to false
	  if (!myRow.isEnabled()) {
	    
	    Boolean toFalse = new Boolean(false);
	    
	    myRow.setVisible(toFalse);
	    myRow.setCreatable(toFalse);
	    myRow.setEditable(toFalse);
	    myRow.setDeletable(toFalse);
	  }
	  
	  // Lets us know we need to write this row out
	  // when we're finished
	  myRow.setChanged(true);
	}
	break;
	
      case CREATABLE:
	if (myRow.canSeeCreate()) {
	  myRow.setCreatable((Boolean)value);
	  myRow.setChanged(true);
	}
	break;
	
      case EDITABLE:
	if (myRow.canSeeEdit()) {
	  myRow.setEditable((Boolean)value);
	  myRow.setChanged(true);
	}
	break;
	
      case DELETABLE:
	if (myRow.canSeeDelete()){
	  myRow.setDeletable((Boolean)value);
	  myRow.setChanged(true);
	}
	break;
      }
    }
  }
  
}

