# Set mailing list e-mail headers with Milter

This wiki page shows how to work around the problem of mail going to junk/spam folder for external recipients of Zimbra distribution lists.

By default Zimbra distribution lists do not set appropriate e-mail headers. This wiki page shows how to set-up Milter (a Python based extension for Postfix). Basically what Milter is set-up to do is filter the original message and discard it. Before discarding, it takes some headers and the body from the original mail and place them in a new mail and use sendmail to pass it to the DL.

The result is a clean message from the correct domain, and it should be able to pass SPF/DKIM and all that.
 
Its great, because the final message that goes to the recipients is still processed by Zimbra as a normal DL message.

To set up the milter you can find in custom-milter.py, please use the following guide: https://github.com/Zimbra/custom-milter
