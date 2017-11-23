#!/usr/bin/env python
# See also:
# https://stuffivelearned.org/doku.php?id=programming:python:python-libmilter
# https://github.com/crustymonkey/python-libmilter
# https://iomarmochtar.wordpress.com/2017/09/13/zimbra-prevent-user-customizing-from-header/

import libmilter as lm
import sys , time
import subprocess

# Create our milter class with the forking mixin and the regular milter
# protocol base classes
class MailingListsMilter(lm.ForkMixin , lm.MilterProtocol):
    def __init__(self , opts=0 , protos=0):
        # We must init our parents here
        lm.MilterProtocol.__init__(self , opts , protos)
        lm.ForkMixin.__init__(self)
        # You can initialize more stuff here
        self.bodyTxt = ''
        self.ListProcessed = 'false'

    def log(self , msg):
        t = time.strftime('%H:%M:%S')
        print '[%s] %s' % (t , msg)
        sys.stdout.flush()

    @lm.noReply
    def rcpt(self , recip , cmdDict):
        self.recip = 'RCPT: %s' % recip
        return lm.CONTINUE

    @lm.noReply
    def mailFrom(self , frAddr , cmdDict):
        self.frAddr = 'MAIL: %s' % frAddr
        return lm.CONTINUE

    @lm.noReply
    def header(self , key , val , cmdDict):
        self.log('%s: %s' % (key , val))
        if key == 'From':
           self.fromHeader = '%s' % val

        #This message is already processed, we do not discard/rewrite it again
        if key == 'X-ZMList-Processed':
           self.ListProcessed = '%s' % val

        #Needed to copy from source to new message
        if key == 'Subject':
           self.subjectHeader = '%s' % val + '\r\n'
        if key == 'Content-Type':
           self.fromContentType = 'Content-Type: %s' % val + '\r\n'
        if key == 'MIME-Version':
           self.MIMEVersionHeader = 'MIME-Version: %s' % val + '\r\n'


        return lm.CONTINUE

    @lm.noReply
    def body(self , chunk , cmdDict):
        self.bodyTxt = self.bodyTxt + '%s' % chunk
        return lm.ACCEPT

    def eob(self , cmdDict):
        if 'testdl@mail.zetalliance.org' in self.recip:
            if 'true' in self.ListProcessed:
                self.log('This is mail is OK')
#                self.chgHeader('Sender','bounces@mail.zetalliance.org', index=1)
#                self.addHeader('X-Barry-Debug','is continued mail')
                return lm.CONTINUE
            else:
                self.log('Discard original email and make a new one')
                p = subprocess.Popen(["/usr/sbin/sendmail", "-t","-f","bounces@mail.zetalliance.org","-F","testdl@mail.zetalliance.org","testdl@mail.zetalliance.org"], stdin=subprocess.PIPE)
                headers = 'To: ' + 'testdl@mail.zetalliance.org' + '\r\n'
                headers += 'From: ' + 'testdl@mail.zetalliance.org' + '\r\n'
                headers += 'Reply-To: ' + 'testdl@mail.zetalliance.org' + '\r\n'
                headers +='Precedence: list\r\n'
                headers +='List-Id: ' + 'testdl@mail.zetalliance.org' + '\r\n'
                headers +='List-Subscribe: ' + '<https://zimbra.example.com/service/extension/mailinglists/>' + '\r\n'
                headers +='List-Unsubscribe: ' + '<https://zimbra.example.com/service/extension/mailinglists/>' + '\r\n'
                headers +='List-Post: ' + '<mailto:testdl@mail.zetalliance.org>' + '\r\n'
                headers +='Errors-To: ' + 'bounces@mail.zetalliance.org' + '\r\n'
                headers +='Sender: ' + 'bounces@mail.zetalliance.org' + '\r\n'
                headers +='X-ZMList-Processed: ' + 'true' + '\r\n'
                p.communicate(headers + 'Subject: [testdl] ' + self.subjectHeader + self.fromContentType + self.MIMEVersionHeader + self.bodyTxt)
                return lm.DISCARD
        else:
            self.log('I continue this one')
        return lm.CONTINUE


def runMailingListsMilter():
    import signal , traceback
    # We can set our milter opts here
    opts = lm.SMFIF_CHGFROM | lm.SMFIF_ADDHDRS | lm.SMFIF_ADDRCPT | lm.SMFIF_QUARANTINE | lm.SMFIF_CHGHDRS 

    # We initialize the factory we want to use (you can choose from an 
    # AsyncFactory, ForkFactory or ThreadFactory.  You must use the
    # appropriate mixin classes for your milter for Thread and Fork)
    f = lm.ForkFactory('inet:127.0.0.1:5000' , MailingListsMilter , opts)
    def sigHandler(num , frame):
        f.close()
        sys.exit(0)
    signal.signal(signal.SIGINT , sigHandler)
    try:
        # run it
        f.run()
    except Exception , e:
        f.close()
        print >> sys.stderr , 'EXCEPTION OCCURED: %s' % e
        traceback.print_tb(sys.exc_traceback)
        sys.exit(3)

if __name__ == '__main__':
    runMailingListsMilter()
