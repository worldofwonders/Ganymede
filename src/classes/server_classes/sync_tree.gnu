#!/opt/bin/perl5
#
# sync_tree
#
# This perl script is designed to create a tree of files
# suitable for jar'ing into a downloadable archive.  Only
# those files that the server needs are to be included.
#
# Jonathan Abbey 11 March 1998
#
#------------------------------------------------------------

$sourcedir = "/home/broccol/gash2/classes/gnu/regexp";
$targetdir = "/home/broccol/gash2/classes/server_classes/gnu/regexp";

# What directory are we handling?

chdir($targetdir);

# Clean it out

system("rm -rf *");

# We want to link in all classes from the ganymede class directory, not including
# custom, client, loader, and other subordinate package directories

opendir SOURCEDIR, $sourcedir or die "couldn't open source dir";

# First get the list of normal (non-dir) files

@classes = grep -f, map "$sourcedir/$_", readdir SOURCEDIR;

# Now whittle off anything that's not a regular class file

@classes = grep /\.class$/, @classes;

closedir SOURCEDIR;

# And link em.

foreach $file (@classes) {

  # Peel off the sourcedir prefix to get the simple file name

  $file =~ /^$sourcedir\/(.*)/;
  $basename = $1;

  print "Linking " . $basename . "\n";

  $result = link ($file, $basename);
  
  if (!$result){
    print "$! -- $file\n";
  }
}

print "Done linking gnu classes.\n";
