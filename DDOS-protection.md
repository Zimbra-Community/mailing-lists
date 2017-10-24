# How to set up DDOS protection for Zimbra mailing lists
Based on fail2ban you can do:

      yum install -y epel-release
      yum install -y fail2ban
      
      # debug
      # fail2ban-client start 
      # fail2ban-client stop
      # fail2ban-regex /opt/zimbra/log/nginx.access.log /etc/fail2ban/filter.d/nginx-dos-zimbra.conf
      
      cat > /etc/fail2ban/jail.local << EOF
      [nginx-dos]
      # A simple IP check (any IP doing more than [maxretry] requests in [findtime] seconds ->
      # Block for [bantime].
      # @author Barry de Graaff
      enabled = true
      port    = 443
      filter  = nginx-dos-zimbra
      logpath = /opt/zimbra/log/nginx.access.log
      findtime = 60
      bantime  = 60
      maxretry = 5
      EOF
      
      cat > /etc/fail2ban/filter.d/nginx-dos-zimbra.conf  << EOF
      # Fail2Ban configuration file
      #
      # Author: Barry de Graaff
      # debug: fail2ban-regex /opt/zimbra/log/nginx.access.log /etc/fail2ban/filter.d/nginx-dos-zimbra.conf
      #
      # $Revision: 1 $
      #
      
      [Definition]
      # Option:  failregex
      # Notes.:  Regexp to catch a generic call from an IP address.
      # Values:  TEXT
      #
      failregex = ^<HOST>:.*GET /service/extension/mailinglists.*$
      
      # Option:  ignoreregex
      # Notes.:  regex to ignore. If this regex matches, the line is ignored.
      # Values:  TEXT
      #
      ignoreregex =
      EOF
      
      
      systemctl start fail2ban
      systemctl enable fail2ban
      systemctl restart firewalld
      
