
DROP SCHEMA IF EXISTS PizzaDB;
CREATE SCHEMA PizzaDB;
USE PizzaDB;

DROP USER IF EXISTS 'dbtester';
CREATE USER 'dbtester' IDENTIFIED BY 'CPSC4620';
GRANT ALL ON Fact.* TO 'dbtester';


CREATE TABLE topping (
    topping_TopID        INTEGER AUTO_INCREMENT PRIMARY KEY,
    topping_TopName      VARCHAR(30) NOT NULL,
    topping_SmallAMT     DECIMAL(5,2) NOT NULL,
    topping_MedAMT     DECIMAL(5,2) NOT NULL,
    topping_LgAMT     DECIMAL(5,2) NOT NULL,
    topping_XLAMT     DECIMAL(5,2) NOT NULL,
    topping_CustPrice   DECIMAL(5,2) NOT NULL,
    topping_BusPrice    DECIMAL(5,2) NOT NULL,
    topping_MinINVT     INTEGER NOT NULL,
    topping_CurINVT     INTEGER NOT NULL
);

CREATE TABLE discount (
    discount_DiscountID INTEGER AUTO_INCREMENT PRIMARY KEY,
    discount_DiscountNAME VARCHAR(30) NOT NULL,
    discount_Amount DECIMAL(5,2) NOT NULL,
    discount_IsPercent TINYINT NOT NULL
);

CREATE TABLE baseprice (
    baseprice_Size VARCHAR(30) NOT NULL,
    baseprice_CrustType VARCHAR(30) NOT NULL,
    baseprice_CustPrice DECIMAL(5,2) NOT NULL,
    baseprice_BusPrice DECIMAL(5,2) NOT NULL,
    PRIMARY KEY (baseprice_Size, baseprice_CrustType),
    CONSTRAINT BP_UI1 UNIQUE(baseprice_Size, baseprice_CrustType)
);

CREATE TABLE customer (
    customer_CustID INTEGER AUTO_INCREMENT PRIMARY KEY,
    customer_FName VARCHAR(30) NOT NULL,
    customer_LName VARCHAR(30) NOT NULL,
    customer_PhoneNum VARCHAR(30) NOT NULL
);

CREATE TABLE ordertable (
    ordertable_OrderID INTEGER AUTO_INCREMENT PRIMARY KEY NOT NULL,
    customer_CustID INT,
    ordertable_OrderType VARCHAR(30) NOT NULL,
    ordertable_OrderDateTime DATETIME NOT NULL,
    ordertable_CustPrice DECIMAL(5,2) NOT NULL,
    ordertable_BusPrice DECIMAL(5,2) NOT NULL,
    ordertable_isComplete TINYINT(1) DEFAULT 0,
    FOREIGN KEY(customer_CustID) REFERENCES customer(customer_CustID)
);

CREATE TABLE pizza (
    pizza_PizzaID INTEGER AUTO_INCREMENT PRIMARY KEY,
    pizza_Size VARCHAR(30),
    pizza_CrustType VARCHAR(30),
    pizza_PizzaState VARCHAR(30),
    pizza_PizzaDate DATETIME,
    pizza_CustPrice DECIMAL(5,2),
    pizza_BusPrice DECIMAL(5,2),
    ordertable_OrderID INT,
    FOREIGN KEY(pizza_Size, pizza_CrustType) REFERENCES baseprice(baseprice_Size, baseprice_CrustType),
    FOREIGN KEY(ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID)
);

CREATE TABLE pizza_topping (
    pizza_PizzaID INT NOT NULL,
    topping_TopID INT NOT NULL,
    pizza_topping_IsDouble INT NOT NULL,
    CONSTRAINT pizza_topping_PK PRIMARY KEY (pizza_PizzaID, topping_TopID),
    FOREIGN KEY(pizza_PizzaID) REFERENCES pizza(pizza_PizzaID),
    FOREIGN KEY(topping_TopID) REFERENCES topping(topping_TopID)
);

CREATE TABLE pizza_discount (
    pizza_PizzaID INT NOT NULL,
    discount_DiscountID INT NOT NULL,
    CONSTRAINT pizza_discount_PK PRIMARY KEY (pizza_PizzaID, discount_DiscountID),
    FOREIGN KEY(pizza_PizzaID) REFERENCES pizza(pizza_PizzaID),
    FOREIGN KEY(discount_DiscountID) REFERENCES discount(discount_DiscountID)
);

CREATE TABLE order_discount (
    ordertable_OrderID INT,
    discount_DiscountID INT,
    CONSTRAINT ORDER_DISCOUNT_PK PRIMARY KEY (ordertable_OrderID, discount_DiscountID),
    FOREIGN KEY(ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID),
    FOREIGN KEY(discount_DiscountID) REFERENCES discount(discount_DiscountID)
);

CREATE TABLE pickup (
    ordertable_OrderID INTEGER PRIMARY KEY,
    pickup_IsPickedUp TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY(ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID)
);

CREATE TABLE delivery (
    ordertable_OrderID INTEGER PRIMARY KEY,
    delivery_HouseNum INT NOT NULL,
    delivery_Street VARCHAR(30) NOT NULL,
    delivery_City VARCHAR(30) NOT NULL,
    delivery_State VARCHAR(2) NOT NULL,
    delivery_Zip INT NOT NULL,
    delivery_IsDelivered TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY(ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID)
);

CREATE TABLE dinein (
    ordertable_OrderID INTEGER PRIMARY KEY,
    dinein_TableNum INT NOT NULL,
    FOREIGN KEY(ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID)
);

