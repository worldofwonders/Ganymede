'''
Totally scammed from the SAMBA guys. Props where props is due.

Stolen from SAMBA trunk/source/include/rpc_secdes.h
http://websvn.samba.org/cgi-bin/viewcvs.cgi/trunk/source/include/rpc_secdes.h?view=markup

Copyright Andrew Tridgell 1992-2000
Copyright Luke Kenneth Casson Leighton 1996-2000
'''

SEC_RIGHTS_QUERY_VALUE		= 0x00000001
SEC_RIGHTS_SET_VALUE		= 0x00000002
SEC_RIGHTS_CREATE_SUBKEY	= 0x00000004
SEC_RIGHTS_ENUM_SUBKEYS		= 0x00000008
SEC_RIGHTS_NOTIFY		= 0x00000010
SEC_RIGHTS_CREATE_LINK		= 0x00000020
SEC_RIGHTS_READ			= 0x00020019
SEC_RIGHTS_FULL_CONTROL		= 0x000f003f
SEC_RIGHTS_MAXIMUM_ALLOWED	= 0x02000000

# for ADS
SEC_RIGHTS_LIST_CONTENTS	= 0x4
SEC_RIGHTS_LIST_OBJECT		= 0x80
SEC_RIGHTS_READ_ALL_PROP	= 0x10
SEC_RIGHTS_READ_PERMS		= 0x20000
SEC_RIGHTS_WRITE_ALL_VALID	= 0x8
SEC_RIGHTS_WRITE_ALL_PROP	= 0x20     
SEC_RIGHTS_MODIFY_OWNER		= 0x80000
SEC_RIGHTS_MODIFY_PERMS		= 0x40000
SEC_RIGHTS_CREATE_CHILD		= 0x1
SEC_RIGHTS_DELETE_CHILD		= 0x2
SEC_RIGHTS_DELETE_SUBTREE	= 0x40
SEC_RIGHTS_DELETE               = 0x10000 # advanced/special/object/delete
SEC_RIGHTS_EXTENDED		= 0x100   # change/reset password, receive/send as
SEC_RIGHTS_CHANGE_PASSWD	= SEC_RIGHTS_EXTENDED
SEC_RIGHTS_RESET_PASSWD		= SEC_RIGHTS_EXTENDED
SEC_RIGHTS_FULL_CTRL		= 0xf01ff

SEC_ACE_OBJECT_PRESENT           = 0x00000001 # thanks for Jim McDonough <jmcd@us.ibm.com>
SEC_ACE_OBJECT_INHERITED_PRESENT = 0x00000002

SEC_ACE_FLAG_OBJECT_INHERIT		= 0x1
SEC_ACE_FLAG_CONTAINER_INHERIT		= 0x2
SEC_ACE_FLAG_NO_PROPAGATE_INHERIT	= 0x4
SEC_ACE_FLAG_INHERIT_ONLY		= 0x8
SEC_ACE_FLAG_INHERITED_ACE		= 0x10 # New for Windows 2000
SEC_ACE_FLAG_VALID_INHERIT		= 0xf
SEC_ACE_FLAG_SUCCESSFUL_ACCESS		= 0x40
SEC_ACE_FLAG_FAILED_ACCESS		= 0x80

SEC_ACE_TYPE_ACCESS_ALLOWED		= 0x0
SEC_ACE_TYPE_ACCESS_DENIED		= 0x1
SEC_ACE_TYPE_SYSTEM_AUDIT		= 0x2
SEC_ACE_TYPE_SYSTEM_ALARM		= 0x3
SEC_ACE_TYPE_ALLOWED_COMPOUND		= 0x4
SEC_ACE_TYPE_ACCESS_ALLOWED_OBJECT	= 0x5
SEC_ACE_TYPE_ACCESS_DENIED_OBJECT       = 0x6
SEC_ACE_TYPE_SYSTEM_AUDIT_OBJECT        = 0x7
SEC_ACE_TYPE_SYSTEM_ALARM_OBJECT	= 0x8

SEC_DESC_OWNER_DEFAULTED	= 0x0001
SEC_DESC_GROUP_DEFAULTED	= 0x0002
SEC_DESC_DACL_PRESENT		= 0x0004
SEC_DESC_DACL_DEFAULTED		= 0x0008
SEC_DESC_SACL_PRESENT		= 0x0010
SEC_DESC_SACL_DEFAULTED		= 0x0020
SEC_DESC_DACL_TRUSTED		= 0x0040
SEC_DESC_SERVER_SECURITY	= 0x0080
 
# New Windows 2000 bits.
 
SE_DESC_DACL_AUTO_INHERIT_REQ	= 0x0100
SE_DESC_SACL_AUTO_INHERIT_REQ	= 0x0200
SE_DESC_DACL_AUTO_INHERITED	= 0x0400
SE_DESC_SACL_AUTO_INHERITED	= 0x0800
SE_DESC_DACL_PROTECTED		= 0x1000
SE_DESC_SACL_PROTECTED		= 0x2000

# Don't know what this means.
SEC_DESC_RM_CONTROL_VALID	= 0x4000

SEC_DESC_SELF_RELATIVE		= 0x8000

# security information
OWNER_SECURITY_INFORMATION	= 0x00000001
GROUP_SECURITY_INFORMATION	= 0x00000002
DACL_SECURITY_INFORMATION	= 0x00000004
SACL_SECURITY_INFORMATION	= 0x00000008

# Extra W2K flags.
UNPROTECTED_SACL_SECURITY_INFORMATION	= 0x10000000
UNPROTECTED_DACL_SECURITY_INFORMATION	= 0x20000000
PROTECTED_SACL_SECURITY_INFORMATION	= 0x40000000
PROTECTED_DACL_SECURITY_INFORMATION	= 0x80000000

ALL_SECURITY_INFORMATION              = OWNER_SECURITY_INFORMATION|\
                                        GROUP_SECURITY_INFORMATION|\
					DACL_SECURITY_INFORMATION|\
                                        SACL_SECURITY_INFORMATION|\
					UNPROTECTED_SACL_SECURITY_INFORMATION|\
					UNPROTECTED_DACL_SECURITY_INFORMATION|\
					PROTECTED_SACL_SECURITY_INFORMATION|\
					PROTECTED_DACL_SECURITY_INFORMATION


# Security Access Masks Rights

SPECIFIC_RIGHTS_MASK	= 0x0000FFFF
STANDARD_RIGHTS_MASK	= 0x00FF0000
GENERIC_RIGHTS_MASK	= 0xF0000000

SEC_RIGHT_SYSTEM_SECURITY	= 0x01000000
SEC_RIGHT_MAXIMUM_ALLOWED	= 0x02000000

# Generic access rights

GENERIC_RIGHT_ALL_ACCESS	= 0x10000000
GENERIC_RIGHT_EXECUTE_ACCESS	= 0x20000000
GENERIC_RIGHT_WRITE_ACCESS	= 0x40000000
GENERIC_RIGHT_READ_ACCESS	= 0x80000000

# Standard access rights.

STD_RIGHT_DELETE_ACCESS		= 0x00010000
STD_RIGHT_READ_CONTROL_ACCESS	= 0x00020000
STD_RIGHT_WRITE_DAC_ACCESS	= 0x00040000
STD_RIGHT_WRITE_OWNER_ACCESS	= 0x00080000
STD_RIGHT_SYNCHRONIZE_ACCESS	= 0x00100000

STD_RIGHT_ALL_ACCESS		= 0x001F0000

# Combinations of standard masks.
STANDARD_RIGHTS_ALL_ACCESS	= STD_RIGHT_ALL_ACCESS # = 0x001f0000
STANDARD_RIGHTS_EXECUTE_ACCESS  = STD_RIGHT_READ_CONTROL_ACCESS # = 0x00020000 
STANDARD_RIGHTS_READ_ACCESS	= STD_RIGHT_READ_CONTROL_ACCESS # = 0x00020000 
STANDARD_RIGHTS_WRITE_ACCESS	= STD_RIGHT_READ_CONTROL_ACCESS # = 0x00020000 
STANDARD_RIGHTS_REQUIRED_ACCESS = STD_RIGHT_DELETE_ACCESS       | \
		                  STD_RIGHT_READ_CONTROL_ACCESS	| \
		                  STD_RIGHT_WRITE_DAC_ACCESS	| \
		                  STD_RIGHT_WRITE_OWNER_ACCESS	# = 0x000f0000 

# File Object specific access rights 

SA_RIGHT_FILE_READ_DATA		= 0x00000001
SA_RIGHT_FILE_WRITE_DATA	= 0x00000002
SA_RIGHT_FILE_APPEND_DATA	= 0x00000004
SA_RIGHT_FILE_READ_EA		= 0x00000008
SA_RIGHT_FILE_WRITE_EA		= 0x00000010
SA_RIGHT_FILE_EXECUTE		= 0x00000020
SA_RIGHT_FILE_DELETE_CHILD	= 0x00000040
SA_RIGHT_FILE_READ_ATTRIBUTES	= 0x00000080
SA_RIGHT_FILE_WRITE_ATTRIBUTES	= 0x00000100

SA_RIGHT_FILE_ALL_ACCESS	= 0x000001FF

GENERIC_RIGHTS_FILE_ALL_ACCESS \
		= STANDARD_RIGHTS_REQUIRED_ACCESS| \
		STD_RIGHT_SYNCHRONIZE_ACCESS	| \
		SA_RIGHT_FILE_ALL_ACCESS

GENERIC_RIGHTS_FILE_READ	\
		= STANDARD_RIGHTS_READ_ACCESS	| \
		STD_RIGHT_SYNCHRONIZE_ACCESS	| \
		SA_RIGHT_FILE_READ_DATA		| \
		SA_RIGHT_FILE_READ_ATTRIBUTES	| \
		SA_RIGHT_FILE_READ_EA

GENERIC_RIGHTS_FILE_WRITE \
		= STANDARD_RIGHTS_WRITE_ACCESS	| \
		STD_RIGHT_SYNCHRONIZE_ACCESS	| \
		SA_RIGHT_FILE_WRITE_DATA	| \
		SA_RIGHT_FILE_WRITE_ATTRIBUTES	| \
		SA_RIGHT_FILE_WRITE_EA		| \
		SA_RIGHT_FILE_APPEND_DATA

GENERIC_RIGHTS_FILE_EXECUTE \
		= STANDARD_RIGHTS_EXECUTE_ACCESS	| \
		SA_RIGHT_FILE_READ_ATTRIBUTES	| \
		SA_RIGHT_FILE_EXECUTE

		
# SAM server specific access rights 

SA_RIGHT_SAM_CONNECT_SERVER	= 0x00000001
SA_RIGHT_SAM_SHUTDOWN_SERVER	= 0x00000002
SA_RIGHT_SAM_INITIALISE_SERVER	= 0x00000004
SA_RIGHT_SAM_CREATE_DOMAIN	= 0x00000008
SA_RIGHT_SAM_ENUM_DOMAINS	= 0x00000010
SA_RIGHT_SAM_OPEN_DOMAIN	= 0x00000020

SA_RIGHT_SAM_ALL_ACCESS		= 0x0000003F

GENERIC_RIGHTS_SAM_ALL_ACCESS \
		= STANDARD_RIGHTS_REQUIRED_ACCESS| \
		SA_RIGHT_SAM_ALL_ACCESS

GENERIC_RIGHTS_SAM_READ	\
		= STANDARD_RIGHTS_READ_ACCESS	| \
		SA_RIGHT_SAM_ENUM_DOMAINS

GENERIC_RIGHTS_SAM_WRITE \
		= STANDARD_RIGHTS_WRITE_ACCESS	| \
		SA_RIGHT_SAM_CREATE_DOMAIN	| \
		SA_RIGHT_SAM_INITIALISE_SERVER	| \
		SA_RIGHT_SAM_SHUTDOWN_SERVER

GENERIC_RIGHTS_SAM_EXECUTE \
		= STANDARD_RIGHTS_EXECUTE_ACCESS	| \
		SA_RIGHT_SAM_OPEN_DOMAIN	| \
		SA_RIGHT_SAM_CONNECT_SERVER


# Domain Object specific access rights 

SA_RIGHT_DOMAIN_LOOKUP_INFO_1		= 0x00000001
SA_RIGHT_DOMAIN_SET_INFO_1		= 0x00000002
SA_RIGHT_DOMAIN_LOOKUP_INFO_2		= 0x00000004
SA_RIGHT_DOMAIN_SET_INFO_2		= 0x00000008
SA_RIGHT_DOMAIN_CREATE_USER		= 0x00000010
SA_RIGHT_DOMAIN_CREATE_GROUP		= 0x00000020
SA_RIGHT_DOMAIN_CREATE_ALIAS		= 0x00000040
SA_RIGHT_DOMAIN_LOOKUP_ALIAS_BY_MEM	= 0x00000080
SA_RIGHT_DOMAIN_ENUM_ACCOUNTS		= 0x00000100
SA_RIGHT_DOMAIN_OPEN_ACCOUNT		= 0x00000200
SA_RIGHT_DOMAIN_SET_INFO_3		= 0x00000400

SA_RIGHT_DOMAIN_ALL_ACCESS		= 0x000007FF

GENERIC_RIGHTS_DOMAIN_ALL_ACCESS \
		= STANDARD_RIGHTS_REQUIRED_ACCESS| \
		SA_RIGHT_DOMAIN_ALL_ACCESS

GENERIC_RIGHTS_DOMAIN_READ \
		= STANDARD_RIGHTS_READ_ACCESS		| \
		SA_RIGHT_DOMAIN_LOOKUP_ALIAS_BY_MEM	| \
		SA_RIGHT_DOMAIN_LOOKUP_INFO_2

GENERIC_RIGHTS_DOMAIN_WRITE \
		= STANDARD_RIGHTS_WRITE_ACCESS	| \
		SA_RIGHT_DOMAIN_SET_INFO_3	| \
		SA_RIGHT_DOMAIN_CREATE_ALIAS	| \
		SA_RIGHT_DOMAIN_CREATE_GROUP	| \
		SA_RIGHT_DOMAIN_CREATE_USER	| \
		SA_RIGHT_DOMAIN_SET_INFO_2	| \
		SA_RIGHT_DOMAIN_SET_INFO_1

GENERIC_RIGHTS_DOMAIN_EXECUTE \
		= STANDARD_RIGHTS_EXECUTE_ACCESS	| \
		SA_RIGHT_DOMAIN_OPEN_ACCOUNT	| \
		SA_RIGHT_DOMAIN_ENUM_ACCOUNTS	| \
		SA_RIGHT_DOMAIN_LOOKUP_INFO_1            


# User Object specific access rights 

SA_RIGHT_USER_GET_NAME_ETC	= 0x00000001
SA_RIGHT_USER_GET_LOCALE	= 0x00000002
SA_RIGHT_USER_SET_LOC_COM	= 0x00000004
SA_RIGHT_USER_GET_LOGONINFO	= 0x00000008
SA_RIGHT_USER_ACCT_FLAGS_EXPIRY	= 0x00000010
SA_RIGHT_USER_SET_ATTRIBUTES	= 0x00000020
SA_RIGHT_USER_CHANGE_PASSWORD	= 0x00000040
SA_RIGHT_USER_SET_PASSWORD	= 0x00000080
SA_RIGHT_USER_GET_GROUPS	= 0x00000100
SA_RIGHT_USER_READ_GROUP_MEM	= 0x00000200
SA_RIGHT_USER_CHANGE_GROUP_MEM	= 0x00000400

SA_RIGHT_USER_ALL_ACCESS	= 0x000007FF

GENERIC_RIGHTS_USER_ALL_ACCESS \
		= STANDARD_RIGHTS_REQUIRED_ACCESS| \
		SA_RIGHT_USER_ALL_ACCESS	# = 0x000f07ff 

GENERIC_RIGHTS_USER_READ \
		= STANDARD_RIGHTS_READ_ACCESS	| \
		SA_RIGHT_USER_READ_GROUP_MEM	| \
		SA_RIGHT_USER_GET_GROUPS	| \
		SA_RIGHT_USER_ACCT_FLAGS_EXPIRY	| \
		SA_RIGHT_USER_GET_LOGONINFO	| \
		SA_RIGHT_USER_GET_LOCALE	# = 0x0002031a 

GENERIC_RIGHTS_USER_WRITE \
		= STANDARD_RIGHTS_WRITE_ACCESS	| \
		SA_RIGHT_USER_CHANGE_PASSWORD	| \
		SA_RIGHT_USER_SET_LOC_COM	| \
		SA_RIGHT_USER_SET_ATTRIBUTES	| \
		SA_RIGHT_USER_SET_PASSWORD	| \
		SA_RIGHT_USER_CHANGE_GROUP_MEM	# = 0x000204e4 

GENERIC_RIGHTS_USER_EXECUTE \
		= STANDARD_RIGHTS_EXECUTE_ACCESS	| \
		SA_RIGHT_USER_CHANGE_PASSWORD	| \
		SA_RIGHT_USER_GET_NAME_ETC 	# = 0x00020041 


# Group Object specific access rights 

SA_RIGHT_GROUP_LOOKUP_INFO	= 0x00000001
SA_RIGHT_GROUP_SET_INFO		= 0x00000002
SA_RIGHT_GROUP_ADD_MEMBER	= 0x00000004
SA_RIGHT_GROUP_REMOVE_MEMBER	= 0x00000008
SA_RIGHT_GROUP_GET_MEMBERS	= 0x00000010

SA_RIGHT_GROUP_ALL_ACCESS	= 0x0000001F

GENERIC_RIGHTS_GROUP_ALL_ACCESS \
		= STANDARD_RIGHTS_REQUIRED_ACCESS| \
		SA_RIGHT_GROUP_ALL_ACCESS	# = 0x000f001f 

GENERIC_RIGHTS_GROUP_READ \
		= STANDARD_RIGHTS_READ_ACCESS	| \
		SA_RIGHT_GROUP_GET_MEMBERS	# = 0x00020010 

GENERIC_RIGHTS_GROUP_WRITE \
		= STANDARD_RIGHTS_WRITE_ACCESS	| \
		SA_RIGHT_GROUP_REMOVE_MEMBER	| \
		SA_RIGHT_GROUP_ADD_MEMBER	| \
		SA_RIGHT_GROUP_SET_INFO 	# = 0x0002000e 

GENERIC_RIGHTS_GROUP_EXECUTE \
		= STANDARD_RIGHTS_EXECUTE_ACCESS	| \
		SA_RIGHT_GROUP_LOOKUP_INFO	# = 0x00020001 


# Alias Object specific access rights 

SA_RIGHT_ALIAS_ADD_MEMBER	= 0x00000001
SA_RIGHT_ALIAS_REMOVE_MEMBER	= 0x00000002
SA_RIGHT_ALIAS_GET_MEMBERS	= 0x00000004
SA_RIGHT_ALIAS_LOOKUP_INFO	= 0x00000008
SA_RIGHT_ALIAS_SET_INFO		= 0x00000010

SA_RIGHT_ALIAS_ALL_ACCESS	= 0x0000001F

GENERIC_RIGHTS_ALIAS_ALL_ACCESS \
		= STANDARD_RIGHTS_REQUIRED_ACCESS| \
		SA_RIGHT_ALIAS_ALL_ACCESS	# = 0x000f001f 

GENERIC_RIGHTS_ALIAS_READ \
		= STANDARD_RIGHTS_READ_ACCESS	| \
		SA_RIGHT_ALIAS_GET_MEMBERS	# = 0x00020004 

GENERIC_RIGHTS_ALIAS_WRITE \
		= STANDARD_RIGHTS_WRITE_ACCESS	| \
		SA_RIGHT_ALIAS_REMOVE_MEMBER	| \
		SA_RIGHT_ALIAS_ADD_MEMBER	| \
		SA_RIGHT_ALIAS_SET_INFO 	# = 0x00020013 

GENERIC_RIGHTS_ALIAS_EXECUTE \
		= STANDARD_RIGHTS_EXECUTE_ACCESS	| \
		SA_RIGHT_ALIAS_LOOKUP_INFO 	# = 0x00020008 


# Constants for certain canonical SID's
#
# http://linux-ntfs.sourceforge.net/ntfs/concepts/sid.html

SID_NULL = "S-1-0-0"
SID_WORLD = "S-1-1-0"
SID_LOCAL = "S-1-2-0"
SID_CREATOR_OWNER = "S-1-3-0"
SID_CREATOR_GROUP = "S-1-3-1"
SID_CREATOR_OWNER_SERVER = "S-1-3-2"
SID_CREATOR_GROUP_SERVER = "S-1-3-3"
SID_NON_UNIQUE = "S-1-4"
SID_NT = "S-1-5"
SID_NT_DIALUP = "S-1-5-1"
SID_NT_NETWORK = "S-1-5-2"
SID_NT_BATCH = "S-1-5-3"
SID_NT_INTERACTIVE = "S-1-5-4"
SID_NT_SERVICE = "S-1-5-6"
SID_NT_ANONYMOUS_LOGON = "S-1-5-7"
SID_NT_PROXY = "S-1-5-8"
SID_NT_ENTERPRISE_CONTROLLERS = "S-1-5-9"
SID_NT_SERVER_LOGON = "S-1-5-9"
SID_NT_PRINCIPAL_SELF = "S-1-5-10"
SID_NT_AUTHENTICATED_USER = "S-1-5-11"
SID_NT_RESTRICTED_CODE = "S-1-5-12"
SID_NT_TERMINAL_SERVER = "S-1-5-13"
SID_NT_LOCAL_SYSTEM = "S-1-5-18"
SID_NT_NON_UNIQUE = "S-1-5-21"
SID_NT_BUILTIN_DOMAIN = "S-1-5-32"
