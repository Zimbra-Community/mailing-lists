# Set mailing list e-mail headers with Milter
This wiki page shows how to work around the problem of mail going to junk/spam folder for external recipients of Zimbra distribution lists.

By default Zimbra distribution lists do not set appropriate e-mail headers. This wiki page shows how to use Milter (a Python based extension for Postfix) to add/replace some headers to make Zimbra distribution lists act as mailing lists. In addition it is showed how you can enforce posting restrictions.

The following headers will be added/replaced:

From: testdl@mail.zetalliance.org  
Reply-To: testdl@mail.zetalliance.org  
Precedence: list  
List-Id: testdl@mail.zetalliance.org  
List-Post: <mailto:testdl@mail.zetalliance.org>  
Errors-To: bounces@mail.zetalliance.org  
Sender: bounces@mail.zetalliance.org  

On the CLI:

     yum install epel-release
     yum install python-pip supervisor python-pymilter
     pip install --upgrade pip     
     pip install python-libmilter

     mkdir - p /opt/zimbra_mailinglists_milter
     cd /opt/zimbra_mailinglists_milter
     wget https://raw.githubusercontent.com/Zimbra-Community/mailing-lists/master/milter/zimbra_mailinglists_milter.py -O /opt/zimbra_mailinglists_milter/zimbra_mailinglists_milter.py
     wget https://raw.githubusercontent.com/Zimbra-Community/mailing-lists/master/milter/etc/daemon.ini -O /etc/supervisord.d/zimbra_mailinglists_milter.ini

Then use your favorite editor (nano/vim) and open `/opt/zimbra_mailinglists_milter/zimbra_mailinglists_milter.py` look under `def eob(self , cmdDict)` and change the script to fit your needs. Please be aware that the indentation level of the statements is significant to Python.

If you are satisfied, you can enable and start the service.

     chmod +rx /opt/zimbra_mailinglists_milter/zimbra_mailinglists_milter.py
     systemctl start supervisord 
     systemctl enable supervisord
     tail -f /var/log/supervisor/supervisord.log
     netstat -tulpn | grep 5000 #should show the service

If you made any typos, you will see them in the log, and there will be nothing listening on port 5000. Try again and issue `systemctl stop supervisord && systemctl start supervisord`.

If it works, enable it for Zimbra:

     su - zimbra
     zmprov ms `zmhostname` zimbraMtaSmtpdMilters inet:127.0.0.1:5000
     zmprov ms `zmhostname` zimbraMilterBindPort 5000
     zmprov ms `zmhostname` zimbraMilterServerEnabled TRUE
     zmmtactl reload

Try sending some emails and:

     tail -f /var/log/zimbra_mailinglists_milter.log
