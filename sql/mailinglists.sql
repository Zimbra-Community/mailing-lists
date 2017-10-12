CREATE TABLE `list_actions` (
  `email` varchar(100) NOT NULL,
  `list_email` varchar(100) NOT NULL,
  `action` varchar(20) NOT NULL COMMENT 'Subscribe or unsubscribe request',
  `approved` tinyint(1) NOT NULL COMMENT 'When true, approved via admin UI',
  `reject` tinyint(1) NOT NULL COMMENT 'When true, remove request without subscribing',  
  `time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'For housekeeping'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Store in progress (un)subscriptions';

ALTER TABLE `list_actions`
  ADD PRIMARY KEY (`email`,`list_email`);

CREATE TABLE `list_confirmations` (
  `email` varchar(100) NOT NULL,
  `list_email` varchar(100) NOT NULL,
  `confirmation` varchar(100) NOT NULL COMMENT 'Confirmation code for the request or 1 once confirmed'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Stores the users confirmation code and status';

ALTER TABLE `list_confirmations`
  ADD PRIMARY KEY (`email`,`list_email`);

CREATE INDEX `confirmation`
ON `list_confirmations` (`confirmation`);

CREATE TABLE `list_properties` (
  `list_email` varchar(100) NOT NULL,
  `enabled` tinyint(1) NULL DEFAULT '0' COMMENT 'when true, use the distribution list as mailing list',
  `approval` tinyint(1) NULL DEFAULT '0' COMMENT 'when true, require admin approval for new subscriptions',
  `description` varchar(250) DEFAULT NULL COMMENT 'displayed on public page'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Distribution list properties';

ALTER TABLE `list_properties`
  ADD PRIMARY KEY (`list_email`);
COMMIT;

CREATE TABLE `page` (
  `title` varchar(255) DEFAULT NULL,
  `style` text,
  `body` text
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Contents for public subscription management page';

INSERT INTO `page` (`title`, `style`, `body`) VALUES
('Zimbra Mailing Lists', 'a,\r\na:visited {\r\n    color: #71be3f;\r\n}\r\nbody {\r\n    background-color: #efefef;\r\n}\r\nh1,\r\np {\r\n    padding-bottom:25px;\r\n
}\r\nh2 {\r\n    font-family: sans-serif;\r\n    color: #71be3f;\r\n}\r\nh2 {\r\n    font-size: 18px;\r\n    margin:0px;\r\n}\r\nhtml,\r\nbody {\r\n    min-height: 100%;\r\n    font-size: 15px;\r\n    font-family: sans-serif;\r\n    padding:15px;\r\n    min-width:600px;\r\n}\r\n\r\n.main {\r\n    margin: auto;\r\n    padding-left: 20px;\r\n    padding-right: 20px;\r\n    padding-top: 3px;\r\n    padding-bottom: 20px;\r\n    background-color: white;\r\n    box-shadow: 0px 1px 6px rgba(23, 69, 88, .5);\r\n    min-width:800px; \r\n    min-height:400px;\r\n}\r\n.logo {\r\n    background-image: url(\"https://zetalliance.org/wp-content/uploads/2017/04/Zeta-Logo.png\");\r\n    margin-top: -3px;\r\n    float: right;\r\n    padding-bottom: 15px;\r\n    height: 100px;\r\n    width: 250px;\r\n    background-size: auto 100px;\r\n}\r\na img {\r\n    border: 0px;\r\n}\r\na {\r\n    text-decoration: none;\r\n}\r\n.button {\r\n    height: 20px;\r\n    min-width: 100px;\r\n    color: #fff;\r\n    background-color: #78bc51;\r\n    text-shadow: -1px 1px #65a244;\r\n    border: none;\r\n    font-family: sans-serif;\r\n    font-size: 14px;\r\n    font-weight: 700;\r\n}\r\n.button:hover,\r\n.button.hover {\r\n    height: 20px;\r\n    min-width: 100px;\r\n    color: #fff;\r\n    background-color: #8cd065;\r\n    text-shadow: -1px 1px #65a244;\r\n    font-family: sans-serif;\r\n    font-size: 14px;\r\n    font-weight: 700;\r\n}\r\nhr{\r\n    border: none;\r\n    height: 1px;\r\n    color: #cccccc;\r\n    background-color: #cccccc;\r\n}', 'Here you can (un)subscribe to our mailing lists.');

CREATE TABLE `template` (
  `fromEmail` varchar(255) NOT NULL,
  `subject` varchar(255) NOT NULL,
  `body` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Holds the Email Template used for sending confirmation links';

INSERT INTO `template` (`fromEmail`, `subject`, `body`) VALUES
('admin@example.com', 'Mailing list confirmation ', 'Mailing list {unsubscription} confirmation notice for mailing list:\r\n{list-email}\r\n\r\nWe have received a request for {unsubscription} of your email address, \r\n{user-email} to the {list-email} mailing list.  \r\n\r\nTo confirm, visit this web page:\r\n\r\n    https://myzimbra.com/service/extension/mailinglists?confirm={confirmation-link}\r\n\r\nIf you do not to {unsubscription}, please simply disregard this message.  \r\nIf you think you are being maliciously (un)subscribed on the list, \r\nor have any other questions, send them to {from-email}.');

CREATE TABLE `mailer` (
  `email` varchar(255) NOT NULL COMMENT 'Email address that need to receive a confirmation link'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Email addresses that need to receive a confirmation link';

ALTER TABLE `mailer`
  ADD PRIMARY KEY (`email`);

