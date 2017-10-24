#!/bin/bash

# Copyright (C) 2017 Barry de Graaff
# 
# Bugs and feedback: https://github.com/Zimbra-Community/mailing-lists/issues
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2 of the License, or
# (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.

set -e
# if you want to trace your script uncomment the following line
# set -x


echo "This is a development script, do not run it in prod. Hit enter, if you want to continue running this script, or CTRL+C  to abort";
read dum;

# Make sure only root can run our script
if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

MAILINGLIST_PWD=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c${1:-10};echo;)

# creating a user, just to make sure we have one (for mysql on CentOS 6, so we can execute the next mysql queries w/o errors)
MAILINGLISTDBCREATE="$(mktemp /tmp/mailinglists-dbcreate.XXXXXXXX.sql)"
cat <<EOF > "${MAILINGLISTDBCREATE}"
CREATE DATABASE mailinglists CHARACTER SET 'UTF8'; 
CREATE USER 'mailinglists'@'127.0.0.1' IDENTIFIED BY '${MAILINGLIST_PWD}'; 
GRANT ALL PRIVILEGES ON mailinglists . * TO 'mailinglists'@'127.0.0.1' WITH GRANT OPTION; 
FLUSH PRIVILEGES ; 
EOF

/opt/zimbra/bin/mysql --force < "${MAILINGLISTDBCREATE}" > /dev/null 2>&1

cat <<EOF > "${MAILINGLISTDBCREATE}"
DROP USER 'mailinglists'@'127.0.0.1';
DROP DATABASE mailinglists;
CREATE DATABASE mailinglists CHARACTER SET 'UTF8'; 
CREATE USER 'mailinglists'@'127.0.0.1' IDENTIFIED BY '${MAILINGLIST_PWD}'; 
GRANT ALL PRIVILEGES ON mailinglists . * TO 'mailinglists'@'127.0.0.1' WITH GRANT OPTION; 
FLUSH PRIVILEGES ; 
EOF

echo "Creating database and user"
/opt/zimbra/bin/mysql < "${MAILINGLISTDBCREATE}"

rm -Rf /opt/zimbra/lib/ext/mailinglists
mkdir -p /opt/zimbra/lib/ext/mailinglists

#here one could optionally support mysql by using jdbc:mysql://, ssl is disabled as this is a local connection
echo "db_connect_string=jdbc:mariadb://127.0.0.1:7306/mailinglists?user=mailinglists&password=$MAILINGLIST_PWD" > /opt/zimbra/lib/ext/mailinglists/db.properties


echo "Populating database please wait..."
/opt/zimbra/bin/mysql mailinglists < sql/mailinglists.sql
bin/add_distributionlists

echo "Deploy Java server extension for public management page"
cp -v extension/out/artifacts/mailinglists/mailinglists.jar /opt/zimbra/lib/ext/mailinglists/

echo "Deploy Java server extension for admin UI"
rm -Rf /opt/zimbra/lib/ext/zamailinglists
mkdir -p /opt/zimbra/lib/ext/zamailinglists
cp -v adminExtension/out/artifacts/zamailinglists/zamailinglists.jar /opt/zimbra/lib/ext/zamailinglists/

echo "Installing CLI commands"
rm -f /usr/local/sbin/add_distributionlists
cp bin/add_distributionlists /usr/local/sbin/add_distributionlists
chmod +rx /usr/local/sbin/add_distributionlists

rm -f /usr/local/sbin/process_mailinglists
cp bin/process_mailinglists /usr/local/sbin/process_mailinglists
chmod +rx /usr/local/sbin/process_mailinglists

echo "Deploy admin Zimlet"
su - zimbra -c "zmzimletctl undeploy tk_barrydegraaff_mailinglists_admin"
rm -f /tmp/tk_barrydegraaff_mailinglists_admin.zip
cd zimlet/tk_barrydegraaff_mailinglists_admin
zip -r /tmp/tk_barrydegraaff_mailinglists_admin.zip *
cd ..
cd ..
su - zimbra -c "zmzimletctl deploy /tmp/tk_barrydegraaff_mailinglists_admin.zip"

rm -f /usr/local/sbin/processor.jar
cp processor/out/artifacts/processor_jar/processor.jar /usr/local/sbin/processor.jar

echo "Setting up cron"
echo "0,10,20,30,40,50 * * * * root bash -l -c '/usr/local/sbin/process_mailinglists' >/var/log/process_mailinglists_cron.log" > /etc/cron.d/process_mailinglists

echo "--------------------------------------------------------------------------------------------------------------
Mailinglists installed successful, the following is installed:
--

For your reference:
- Database mailinglists and user have been created using: 
  ${MAILINGLISTDBCREATE}

To activate your configuration, run as zimbra user:
su - zimbra -c \"zmmailboxdctl restart\"
--------------------------------------------------------------------------------------------------------------
"
