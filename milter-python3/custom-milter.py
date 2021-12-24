#!/usr/bin/python3
	
## To roll your own milter, create a class that extends Milter.  
#  This is a useless example to show basic features of Milter. 
#  See the pymilter project at https://pymilter.org based 
#  on Sendmail's milter API 
#  This code is open-source on the same terms as Python.

## Milter calls methods of your class at milter events.
## Return REJECT,TEMPFAIL,ACCEPT to short circuit processing for a message.
## You can also add/del recipients, replacebody, add/del headers, etc.

from __future__ import print_function
import Milter
try:
  from StringIO import StringIO as BytesIO
except:
  from io import BytesIO
import time
import email
from email import message_from_binary_file
from email import policy
import mimetypes
import os
import sys
from socket import AF_INET, AF_INET6
from Milter.utils import parse_addr
import subprocess

if True:
  # for logging process - usually not needed
  from multiprocessing import Process as Thread, Queue
else:
  from threading import Thread
  from Queue import Queue

logq = None

class myMilter(Milter.Base):

  def __init__(self):  # A new instance with each new connection.
    self.id = Milter.uniqueID()  # Integer incremented with each call.
    # You can initialize more stuff here
    self.subjectHeader = ''
    self.fromContentType = ''
    self.MIMEVersionHeader = ''
    self.ListProcessed = 'false'
    self.recip = []
  # each connection runs in its own thread and has its own myMilter
  # instance.  Python code must be thread safe.  This is trivial if only stuff
  # in myMilter instances is referenced.
  @Milter.noreply
  def connect(self, IPname, family, hostaddr):
    # (self, 'ip068.subnet71.example.com', AF_INET, ('215.183.71.68', 4720) )
    # (self, 'ip6.mxout.example.com', AF_INET6,
    #	('3ffe:80e8:d8::1', 4720, 1, 0) )
    self.IP = hostaddr[0]
    self.port = hostaddr[1]
    if family == AF_INET6:
      self.flow = hostaddr[2]
      self.scope = hostaddr[3]
    else:
      self.flow = None
      self.scope = None
    self.IPname = IPname  # Name from a reverse IP lookup
    self.H = None
    self.fp = None
    self.receiver = self.getsymval('j')
    self.log("connect from %s at %s" % (IPname, hostaddr) )
    
    return Milter.CONTINUE


  ##  def hello(self,hostname):
  def hello(self, heloname):
    # (self, 'mailout17.dallas.texas.example.com')
    self.H = heloname
    self.log("HELO %s" % heloname)
    if heloname.find('.') < 0:	# illegal helo name
      # NOTE: example only - too many real braindead clients to reject on this
      self.setreply('550','5.7.1','Sheesh people!  Use a proper helo name!')
      return Milter.REJECT
      
    return Milter.CONTINUE

  ##  def envfrom(self,f,*str):
  def envfrom(self, mailfrom, *str):
    self.F = mailfrom
    self.R = []  # list of recipients
    self.fromparms = Milter.dictfromlist(str)	# ESMTP parms
    self.user = self.getsymval('{auth_authen}')	# authenticated user
    self.log("mail from:", mailfrom, *str)
    # NOTE: self.fp is only an *internal* copy of message data.  You
    # must use addheader, chgheader, replacebody to change the message
    # on the MTA.
    self.fp = BytesIO()
    self.canon_from = '@'.join(parse_addr(mailfrom))
    self.fp.write(b'From %s %s\n' % (self.canon_from.encode(),
        time.ctime().encode()))
    return Milter.CONTINUE


  ##  def envrcpt(self, to, *str):
  @Milter.noreply
  def envrcpt(self, to, *str):
    rcptinfo = to,Milter.dictfromlist(str)
    self.recip.append(to)
    self.R.append(rcptinfo)
    
    return Milter.CONTINUE


  @Milter.noreply
  def header(self, name, hval):
    self.fp.write(b'%s: %s\n' % (name.encode(),hval.encode()))	# add header to buffer

    if name == 'From':
      self.fromHeader = '%s' % hval
   
    #This message is already processed, we do not discard/rewrite it again
    if name == 'X-ZMList-Processed':
      self.ListProcessed = '%s' % hval
   
    #Needed to copy from source to new message
    if name == 'Subject':
      self.subjectHeader = '%s' % hval + '\r\n'
    if name == 'Content-Type':
      self.fromContentType = 'Content-Type: %s' % hval + '\r\n'
    if name == 'MIME-Version':
      self.MIMEVersionHeader = 'MIME-Version: %s' % hval + '\r\n'
    return Milter.CONTINUE

  @Milter.noreply
  def eoh(self):
    self.fp.write(b'\n')				# terminate headers
    return Milter.CONTINUE

  @Milter.noreply
  def body(self, chunk):
    self.fp.write(chunk)
    return Milter.CONTINUE

  def eom(self):
    self.fp.seek(0)
    msg = email.message_from_binary_file(self.fp, policy=policy.default)

    self.log("recip %s" %  (self.recip,))
    for x in self.recip:
       print(x)
       if "testdl@zm-zimbra9.barrydegraaff.tk" in x:
        if 'true' in self.ListProcessed:
            self.log('This is mail is OK')
            return Milter.CONTINUE
        else:
            self.log('Discard original email and make a new one')
            p = subprocess.Popen(["/usr/sbin/sendmail", "-t","-f","bounces@zm-zimbra9.barrydegraaff.tk","-F","testdl@zm-zimbra9.barrydegraaff.tk","testdl@zm-zimbra9.barrydegraaff.tk"], stdin=subprocess.PIPE)
            del msg['To']
            del msg['From']
            del msg['Reply-To']
            del msg['Precedence']
            del msg['List-Id']
            del msg['List-Subscribe']
            del msg['List-Unsubscribe']
            del msg['List-Post']
            del msg['Errors-To']
            del msg['Sender']
            del msg['Subject']
            headers = 'To: ' + 'testdl@zm-zimbra9.barrydegraaff.tk' + '\r\n'
            headers += 'From: ' + 'testdl@zm-zimbra9.barrydegraaff.tk' + '\r\n'
            headers += 'Reply-To: ' + 'testdl@zm-zimbra9.barrydegraaff.tk' + '\r\n'
            headers +='Precedence: list\r\n'
            headers +='List-Id: ' + 'testdl@zm-zimbra9.barrydegraaff.tk' + '\r\n'
            headers +='List-Subscribe: ' + '<https://zimbra.example.com/service/extension/mailinglists/>' + '\r\n'
            headers +='List-Unsubscribe: ' + '<https://zimbra.example.com/service/extension/mailinglists/>' + '\r\n'
            headers +='List-Post: ' + '<mailto:testdl@zm-zimbra9.barrydegraaff.tk>' + '\r\n'
            headers +='Errors-To: ' + 'bounces@zm-zimbra9.barrydegraaff.tk' + '\r\n'
            headers +='Sender: ' + 'bounces@zm-zimbra9.barrydegraaff.tk' + '\r\n'
            headers +='X-ZMList-Processed: ' + 'true' + '\r\n'            
            p.communicate((headers + 'Subject: [testdl] ' + self.subjectHeader + msg.as_string()).encode())
            return Milter.DISCARD
    return Milter.ACCEPT

  def close(self):
    # always called, even when abort is called.  Clean up
    # any external resources here.
    return Milter.CONTINUE

  def abort(self):
    # client disconnected prematurely
    return Milter.CONTINUE

  ## === Support Functions ===

  def log(self,*msg):
    t = (msg,self.id,time.time())
    if logq:
      logq.put(t)
    else:
      # logmsg(*t)
      pass

def logmsg(msg,id,ts):
    print("%s [%d]" % (time.strftime('%Y%b%d %H:%M:%S',time.localtime(ts)),id),
        end=None)
    # 2005Oct13 02:34:11 [1] msg1 msg2 msg3 ...
    for i in msg: print(i,end=None)
    print()
    sys.stdout.flush()

def background():
  while True:
    t = logq.get()
    if not t: break
    logmsg(*t)

## ===
    
def main():
  bt = Thread(target=background)
  bt.start()
  # This is NOT a good socket location for production, it is for 
  # playing around.  I suggest /var/run/milter/myappnamesock for production.
#  socketname = os.path.expanduser('~/pythonsock')
  socketname = "inet:8800"
  timeout = 600
  # Register to have the Milter factory create instances of your class:
  Milter.factory = myMilter
  flags = Milter.CHGBODY + Milter.CHGHDRS + Milter.ADDHDRS
  flags += Milter.ADDRCPT
  flags += Milter.DELRCPT
  Milter.set_flags(flags)       # tell Sendmail which features we use
  print("%s milter startup" % time.strftime('%Y%b%d %H:%M:%S'))
  sys.stdout.flush()
  Milter.runmilter("pythonfilter",socketname,timeout)
  logq.put(None)
  bt.join()
  print("%s bms milter shutdown" % time.strftime('%Y%b%d %H:%M:%S'))

if __name__ == "__main__":
  # You probably do not need a logging process, but if you do, this
  # is one way to do it.
  logq = Queue(maxsize=4)
  main()
