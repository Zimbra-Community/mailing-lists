yum install epel-release
yum install python-pymilter python-ldap supervisor git-core

cp /opt/zimbra_mailinglists_milter/etc/daemon.ini /etc/supervisord.d/zimbra_mailinglists_milter.ini
su - zimbra
zmprov ms `zmhostname` zimbraMtaSmtpdMilters inet:127.0.0.1:5000
#zmprov ms `zmhostname` zimbraMilterBindPort 5000
zmprov ms `zmhostname` zimbraMilterServerEnabled TRUE
zmmtactl reload
