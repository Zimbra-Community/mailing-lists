#!/usr/bin/env python

import libmilter as lm
import sys , time

# See also:
# https://stuffivelearned.org/doku.php?id=programm
# https://github.com/crustymonkey/python-libmilter
# https://iomarmochtar.wordpress.com/2017/09/13/zimbra-prevent-user-customizing-from-header/


# Create our milter class with the forking mixin and the regular milter
# protocol base classes
class MailingListsMilter(lm.ForkMixin , lm.MilterProtocol):
    def __init__(self , opts=0 , protos=0):
        # We must init our parents here
        lm.MilterProtocol.__init__(self , opts , protos)
        lm.ForkMixin.__init__(self)
        # You can initialize more stuff here

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

    def eob(self , cmdDict):
        if 'testdl@mail.zetalliance.org' in self.recip:

        # This shows how to restrict senders
        #    if 'admin@mail.zetalliance.org' not in self.frAddr:
        #        self.setReply('554' , '5.7.1' , 'Rejected sender is not allowed for posting to this list')
        #        self.log('Rejected ' + self.frAddr + ' not allowed for posting to ' + self.recip)
        #        return lm.REJECT

            self.log('Adding headers for ' + self.recip)
            self.chgHeader('From' , 'testdl@mail.zetalliance.org' , index=1)
            self.chgHeader('Reply-To' , 'testdl@mail.zetalliance.org' , index=1)
            self.chgHeader('Precedence','list', index=1)
            self.chgHeader('List-Id','testdl@mail.zetalliance.org', index=1)
            self.chgHeader('List-Post','<mailto:testdl@mail.zetalliance.org>', index=1)
            self.chgHeader('Errors-To','bounces@mail.zetalliance.org', index=1)
            self.chgHeader('Sender','bounces@mail.zetalliance.org', index=1)

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
