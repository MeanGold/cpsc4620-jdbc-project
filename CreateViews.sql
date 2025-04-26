USE PIZZADB;

# -------------------------------------------------------------------------------------------------
# Topping popularity
# -------------------------------------------------------------------------------------------------
CREATE VIEW ToppingPopularity AS
    SELECT v_table.TopName_v AS Topping, SUM(ToppingCount_v) AS ToppingCount FROM
    ((SELECT topping.topping_TopName AS TopName_v, COUNT(pizza_topping.topping_TopID) AS ToppingCount_v
        FROM topping LEFT JOIN pizza_topping ON topping.topping_TopID = pizza_topping.topping_TopID
        GROUP BY TopName_v
        ORDER BY ToppingCount_v DESC, TopName_v)
    UNION ALL
    (SELECT topping.topping_TopName AS TopName_v, COUNT(topping.topping_TopID) AS ToppingCount_v
     FROM pizza_topping JOIN topping ON topping.topping_TopID = pizza_topping.topping_TopID
        WHERE pizza_topping_IsDouble = True
        GROUP BY TopName_v)) as v_table
        GROUP BY Topping
        ORDER BY ToppingCount DESC, Topping;

-- DROP VIEW ToppingPopularity;

# -------------------------------------------------------------------------------------------------
# Profit by Pizza
# -------------------------------------------------------------------------------------------------
CREATE VIEW ProfitByPizza AS
    SELECT pizza_Size as Size, pizza_CrustType as Crust, SUM(pizza_CustPrice-pizza_BusPrice) AS Profit,
           DATE_FORMAT(pizza_PizzaDate, "%c/%Y") AS OrderMonth FROM pizza
        GROUP BY pizza_Size, pizza_CrustType, OrderMonth
        ORDER BY Profit;

-- DROP VIEW ProfitByPizza;

# -------------------------------------------------------------------------------------------------
# Profit by Order Type
# -------------------------------------------------------------------------------------------------
CREATE VIEW ProfitByOrderType AS
(SELECT ordertable_OrderType AS customerType, DATE_FORMAT(ordertable_OrderDateTime, "%c/%Y") as OrderMonth,
        SUM(ordertable_CustPrice) AS TotalOrderPrice, SUM(ordertable_BusPrice) AS TotalOrderCost,
        (SUM(ordertable_CustPrice)-SUM(ordertable_BusPrice)) AS Profit FROM ordertable
        GROUP BY ordertable_OrderType, DATE_FORMAT(ordertable_OrderDateTime, "%c/%Y")
        ORDER BY customerType, Profit DESC)
UNION ALL
(SELECT NULL, 'Grand Total', SUM(ordertable_CustPrice) AS TotalOrderPrice, SUM(ordertable_BusPrice) AS TotalOrderCost,
        (SUM(ordertable_CustPrice)-SUM(ordertable_BusPrice)) AS Profit FROM ordertable);

# SELECT customerType, OrderMonth, TotalOrderPrice, TotalOrderCost, Profit FROM
#     (SELECT ORDERTABLE_ORDERTYPE AS customerType, DATE_FORMAT(ORDERTABLE_ORDERDATETIME, "%c/%Y") as OrderMonth,
#         SUM(ORDERTABLE_CUSTPRICE) AS TotalOrderPrice, SUM(ORDERTABLE_BUSPRICE) AS TotalOrderCost,
#         (SUM(ORDERTABLE_CUSTPRICE)-SUM(ORDERTABLE_BUSPRICE)) AS Profit FROM ORDERTABLE
#         GROUP BY ORDERTABLE_ORDERTYPE, DATE_FORMAT(ORDERTABLE_ORDERDATETIME, "%c/%Y")
#         ORDER BY customerType, Profit DESC) AS ProfitTable
#     GROUP BY customerType, OrderMonth WITH ROLLUP;






