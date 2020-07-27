create schema orders;

CREATE TABLE orders.`order` (
  `orderId` int(11) NOT NULL,
  `orderAmount` decimal(6,2) NOT NULL,
  `orderStatus` varchar(30) NOT NULL,
  `orderDateTime` varchar(30) NOT NULL,
  `shipToName` varchar(30) NOT NULL,
  `shipToAddress` varchar(30) NOT NULL,
  `shipToCity` varchar(30) NOT NULL,
  `shipToState` varchar(30) NOT NULL,
  `shipToZip` varchar(10) NOT NULL,
  PRIMARY KEY (`orderId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ;

CREATE TABLE orders.`order_detail` (
  `orderId` int(11) NOT NULL,
  `billToName` varchar(45) DEFAULT NULL,
  `billToAddress` varchar(45) DEFAULT NULL,
  `billToCity` varchar(20) DEFAULT NULL,
  `billToState` varchar(15) DEFAULT NULL,
  `billToZip` varchar(10) DEFAULT NULL,
  KEY `orderId_idx` (`orderId`),
  CONSTRAINT `orderId` FOREIGN KEY (`orderId`) REFERENCES `order` (`orderId`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE orders.`order_item` (
  `orderId` int(11) NOT NULL,
  `itemId` int(11) NOT NULL,
  `itemQuanity` int(11) DEFAULT NULL,
  `itemAmount` decimal(6,2) DEFAULT NULL,
  `itemStatus` varchar(15) DEFAULT NULL,
  PRIMARY KEY (`orderId`,`itemId`),
  CONSTRAINT `fk_order_id` FOREIGN KEY (`orderId`) REFERENCES `order` (`orderId`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- setup binlog retention
SET SQL_SAFE_UPDATES = 0;

call mysql.rds_set_configuration('binlog retention hours', 24);



commit;