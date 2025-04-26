package cpsc4620;

import com.mysql.cj.x.protobuf.MysqlxPrepare;

import java.io.IOException;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.*;

/*
 * This file is where you will implement the methods needed to support this application.
 * You will write the code to retrieve and save information to the database and use that
 * information to build the various objects required by the applicaiton.
 * 
 * The class has several hard coded static variables used for the connection, you will need to
 * change those to your connection information
 * 
 * This class also has static string variables for pickup, delivery and dine-in. 
 * DO NOT change these constant values.
 * 
 * You can add any helper methods you need, but you must implement all the methods
 * in this class and use them to complete the project.  The autograder will rely on
 * these methods being implemented, so do not delete them or alter their method
 * signatures.
 * 
 * Make sure you properly open and close your DB connections in any method that
 * requires access to the DB.
 * Use the connect_to_db below to open your connection in DBConnector.
 * What is opened must be closed!
 */

/*
 * A utility class to help add and retrieve information from the database
 */

public final class DBNinja {
	private static Connection conn;

	// DO NOT change these variables!
	public final static String pickup = "pickup";
	public final static String delivery = "delivery";
	public final static String dine_in = "dinein";

	public final static String size_s = "Small";
	public final static String size_m = "Medium";
	public final static String size_l = "Large";
	public final static String size_xl = "XLarge";

	public final static String crust_thin = "Thin";
	public final static String crust_orig = "Original";
	public final static String crust_pan = "Pan";
	public final static String crust_gf = "Gluten-Free";

	public enum order_state {
		PREPARED,
		DELIVERED,
		PICKEDUP
	}


	private static boolean connect_to_db() throws SQLException, IOException 
	{

		try {
			conn = DBConnector.make_connection();
			return true;
		} catch (SQLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

	}

	public static void addOrder(Order o) throws SQLException, IOException 
	{
		/*
		 * add code to add the order to the DB. Remember that we're not just
		 * adding the order to the order DB table, but we're also recording
		 * the necessary data for the delivery, dinein, pickup, pizzas, toppings
		 * on pizzas, order discounts and pizza discounts.
		 * 
		 * This is a KEY method as it must store all the data in the Order object
		 * in the database and make sure all the tables are correctly linked.
		 * 
		 * Remember, if the order is for Dine In, there is no customer...
		 * so the cusomter id coming from the Order object will be -1.
		 * 
		 */
		/*INSERT INTO ordertable VALUES(1, Null, "dinein", "2025-03-05 12:03:00", 19.75, 3.68, True);*/
		connect_to_db();

		try {

			String findLastOrderID = "SELECT ordertable_OrderID FROM ordertable ORDER BY ordertable_OrderID DESC LIMIT 1;";
			PreparedStatement stmtLastOrderID = conn.prepareStatement(findLastOrderID);
			ResultSet rsetID = stmtLastOrderID.executeQuery();
			rsetID.next();
			int orderID = rsetID.getInt("ordertable_OrderID");
			// Add one to the orderID to get the correct next ID
			orderID += 1;

			String insertOrder = "INSERT INTO ordertable VALUES (?, ?, ?, DATE(?), ?, ?, ?);";
			PreparedStatement stmtOrder = conn.prepareStatement(insertOrder);
			stmtOrder.setInt(1, orderID);
			// ---------------------------------------------------------------------------------
			// Excluding orderID so that it auto increments and assigns the next value
			// ---------------------------------------------------------------------------------
			if (Objects.equals(o.getOrderType(), "dinein")) {
				stmtOrder.setNull(2, Types.INTEGER);
			} else {
				stmtOrder.setInt(2, o.getCustID());
			}
			stmtOrder.setString(3, o.getOrderType());
			stmtOrder.setString(4, o.getDate());
			stmtOrder.setDouble(5, o.getCustPrice());
			stmtOrder.setDouble(6, o.getBusPrice());
			stmtOrder.setBoolean(7, o.getIsComplete());
			stmtOrder.executeUpdate();

			// ----------------------------------------------------------------------------------
			// Must add switch statement for different order types: dine-in, pickup, and delivery
			// ----------------------------------------------------------------------------------
			switch (o.getOrderType()) {
				case dine_in:
					DineinOrder tempDi = (DineinOrder) o;
					String insertDinein = "INSERT INTO dinein VALUES (?, ?);";
					PreparedStatement stmtDinein = conn.prepareStatement(insertDinein);
					stmtDinein.setInt(1, orderID);
					stmtDinein.setInt(2, tempDi.getTableNum());
					stmtDinein.executeUpdate();
					break;
				case pickup:
					PickupOrder tempP = (PickupOrder) o;
					String insertPickup = "INSERT INTO pickup VALUES (?, ?);";
					PreparedStatement stmtPickup = conn.prepareStatement(insertPickup);
					stmtPickup.setInt(1, orderID);
					stmtPickup.setBoolean(2, tempP.getIsPickedUp());
					stmtPickup.executeUpdate();
					break;
				case delivery:
					DeliveryOrder tempDe = (DeliveryOrder) o;
					String insertDelivery = "INSERT INTO delivery VALUES (?, ?, ?, ?, ?, ?, ?);";
					PreparedStatement stmtDelivery = conn.prepareStatement(insertDelivery);
					stmtDelivery.setInt(1, orderID);
					String addr = tempDe.getAddress();
					// housenum, street, city, state, zip
					String[] words = addr.split("\\s+");


					stmtDelivery.setInt(2, Integer.parseInt(words[0]));
					stmtDelivery.setString(3, words[1]);
					stmtDelivery.setString(4, words[2]);
					stmtDelivery.setString(5, words[3]);
					stmtDelivery.setInt(6, Integer.parseInt(words[4]));
					stmtDelivery.setBoolean(7, false);
					stmtDelivery.executeUpdate();
					break;
			}



			for (Pizza pizza : o.getPizzaList()) {
				java.util.Date orderDate = new Date(getYear(o.getDate()), getMonth(o.getDate()), getDay(o.getDate()));
				addPizza(orderDate, orderID, pizza);
			}

			for (Discount discount : o.getDiscountList()) {
				String insertDiscounts = "INSERT INTO order_discount VALUES (?, ?);";
				PreparedStatement stmtDiscounts = conn.prepareStatement(insertDiscounts);
				stmtDiscounts.setInt(1, orderID);
				stmtDiscounts.setInt(2, discount.getDiscountID());
				stmtDiscounts.executeUpdate();
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		conn.close();
	}
	
	public static int addPizza(java.util.Date d, int orderID, Pizza p) throws SQLException, IOException
	{
		/*
		 * Add the code needed to insert the pizza into into the database.
		 * Keep in mind you must also add the pizza discounts and toppings 
		 * associated with the pizza.
		 * 
		 * NOTE: there is a Date object passed into this method so that the Order
		 * and ALL its Pizzas can be assigned the same DTS.
		 * 
		 * This method returns the id of the pizza just added.
		 * 
		 */
		/*INSERT INTO pizza VALUES(1001, "Large", "Thin", "completed",
			"2025-03-05 12:03:00", 19.75, 3.68, 1);*/
		connect_to_db();
		int pizzaID = 0;
		try {
			String findLastOrderID = "SELECT pizza_PizzaID FROM pizza ORDER BY pizza_PizzaID DESC LIMIT 1;";
			PreparedStatement stmtLastOrderID = conn.prepareStatement(findLastOrderID);
			ResultSet rsetID = stmtLastOrderID.executeQuery();
			rsetID.next();
			pizzaID = rsetID.getInt("pizza_PizzaID");
			// Add one to the pizzaID for the correct next ID
			pizzaID += 1;

			String insertPizza = "INSERT INTO pizza VALUES(?, ?, ?, ?, ?, ?, ?, ?);";
			PreparedStatement stmtPizza = conn.prepareStatement(insertPizza);
			// ---------------------------------------------------------------------------------
			// Excluding orderID so that it auto increments and assigns the next value
			// ---------------------------------------------------------------------------------
			stmtPizza.setInt(1, pizzaID);
			stmtPizza.setString(2, p.getSize());
			stmtPizza.setString(3, p.getCrustType());
			stmtPizza.setString(4, p.getPizzaState());
			stmtPizza.setDate(5, (Date) d);
			stmtPizza.setDouble(6, p.getCustPrice());
			stmtPizza.setDouble(7, p.getBusPrice());
			stmtPizza.setInt(8, orderID);
			stmtPizza.executeUpdate();

			for (Topping topping : p.getToppings()) {
				String insertToppings = "INSERT INTO pizza_topping VALUES (?, ?, ?);";
				PreparedStatement stmtToppings = conn.prepareStatement(insertToppings);
				stmtToppings.setInt(1, pizzaID);
				stmtToppings.setInt(2, topping.getTopID());
				stmtToppings.setInt(3, (topping.getDoubled()) ? 1 : 0);
				stmtToppings.executeUpdate();
			}

			for (Discount discount : p.getDiscounts()) {
				String insertDiscounts = "INSERT INTO pizza_discount VALUES (?, ?);";
				PreparedStatement stmtDiscounts = conn.prepareStatement(insertDiscounts);
				stmtDiscounts.setInt(1, pizzaID);
				stmtDiscounts.setInt(2, discount.getDiscountID());
				stmtDiscounts.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return pizzaID;
	}
	
	public static int addCustomer(Customer c) throws SQLException, IOException
	 {
		/*
		 * This method adds a new customer to the database.
		 * 
		 */
		 connect_to_db();

		 String findLastCustID = "SELECT customer_CustID FROM customer ORDER BY customer_CustID DESC LIMIT 1;";
		 PreparedStatement stmtLastOrderID = conn.prepareStatement(findLastCustID);
		 ResultSet rsetID = stmtLastOrderID.executeQuery();
		 rsetID.next();
		 c.setCustID(rsetID.getInt("customer_CustID")+1);

		 String insertCust = "INSERT INTO customer VALUES(?, ?, ?, ?);";
		 PreparedStatement stmtCust = conn.prepareStatement(insertCust);
		 // ---------------------------------------------------------------------------------
		 // Excluding orderID so that it auto increments and assigns the next value
		 // ---------------------------------------------------------------------------------

		 stmtCust.setInt(1, c.getCustID());
		 stmtCust.setString(2, c.getFName());
		 stmtCust.setString(3, c.getLName());
		 stmtCust.setString(4, c.getPhone());
		 stmtCust.executeUpdate();
		 conn.close();

		 return c.getCustID();
	}

	public static void completeOrder(int OrderID, order_state newState ) throws SQLException, IOException
	{
		/*
		 * Mark that order as complete in the database.
		 * Note: if an order is complete, this means all the pizzas are complete as well.
		 * However, it does not mean that the order has been delivered or picked up!
		 *
		 * For newState = PREPARED: mark the order and all associated pizza's as completed
		 * For newState = DELIVERED: mark the delivery status
		 * FOR newState = PICKEDUP: mark the pickup status
		 * 
		 */

	}


	// *****************************************************************************************************************
	// REWORKED
	// *****************************************************************************************************************
	public static ArrayList<Order> getOrders(int status) throws SQLException, IOException
	 {
	/*
	 * Return an ArrayList of orders.
	 * 	status   == 1 => return a list of open (ie order is not completed)
	 *           == 2 => return a list of completed orders (ie order is complete)
	 *           == 3 => return a list of all the orders
	 * Remember that in Java, we account for supertypes and subtypes
	 * which means that when we create an arrayList of orders, that really
	 * means we have an arrayList of dineinOrders, deliveryOrders, and pickupOrders.
	 *
	 * You must fully populate the Order object, this includes order discounts,
	 * and pizzas along with the toppings and discounts associated with them.
	 * 
	 * Don't forget to order the data coming from the database appropriately.
	 *
	 */
		 connect_to_db();

		 ArrayList<Order> orderList = new ArrayList<>();
		 try {
			 PreparedStatement os;
			 ResultSet rset;
			 String query;
			 switch (status) {
				 case 1: query = "Select * From ordertable Where ordertable_isComplete = False;";
				 	break;
				 case 2: query = "Select * From ordertable Where ordertable_isComplete = True;";
				 	break;
				 case 3: query = "Select * From ordertable;";
				 	break;
				 default: throw new IllegalStateException("Unexpected value: " + status);
			 };
			 os = conn.prepareStatement(query);
			 rset = os.executeQuery();
			 while (rset.next()) {
				 Order nextOrder = null;
				 int orderID = rset.getInt("ordertable_OrderID");
				 /* ==============================================================
				  * SWITCH statement for Dine-in, Pickup, and Delivery
				  * =============================================================== */
				 switch (rset.getString("ordertable_OrderType")) {
					 case dine_in:
						 // Retrieving the extra values that are included in a Dine-in order
						 PreparedStatement dine_os;
						 ResultSet rsetDine;
						 String dineQuery = "Select * From dinein Where ordertable_OrderID = ?;";
						 dine_os = conn.prepareStatement(dineQuery);
						 dine_os.setInt(1, orderID);
						 rsetDine = dine_os.executeQuery();
						 rsetDine.next();
						 nextOrder = new DineinOrder(orderID, rset.getInt("customer_CustID"),
								 rset.getString("ordertable_OrderDateTime"), rset.getDouble("ordertable_CustPrice"),
								 rset.getDouble("ordertable_BusPrice"), rset.getBoolean("ordertable_isComplete"),
								 rsetDine.getInt("dinein_TableNum"));
						 break;
					 case pickup:
						 // Retrieving the extra values that are included in a Pickup order
						 PreparedStatement pickup_os;
						 ResultSet rsetPick;
						 String pickQuery = "Select * From pickup Where ordertable_OrderID = ?;";
						 pickup_os = conn.prepareStatement(pickQuery);
						 pickup_os.setInt(1, orderID);
						 rsetPick = pickup_os.executeQuery();
						 rsetPick.next();
						 nextOrder = new PickupOrder(orderID, rset.getInt("customer_CustID"),
								 rset.getString("ordertable_OrderDateTime"), rset.getDouble("ordertable_CustPrice"),
								 rset.getDouble("ordertable_BusPrice"), rsetPick.getBoolean("pickup_IsPickedUp"),
								 rset.getBoolean("ordertable_isComplete"));
					 	 break;
					 case delivery:
						 // Retrieving the extra values that are included in a Delivery order
						 PreparedStatement deliver_os;
						 ResultSet rsetDeliv;
						 String delivQuery = "Select * From delivery Where ordertable_OrderID = ?;";
						 deliver_os = conn.prepareStatement(delivQuery);
						 deliver_os.setInt(1, orderID);
						 rsetDeliv = deliver_os.executeQuery();
						 rsetDeliv.next();
						 String address = rsetDeliv.getString("delivery_HouseNum") + "\t" + rsetDeliv.getString("delivery_Street") +
								 "\t" + rsetDeliv.getString("delivery_City") + "\t" + rsetDeliv.getString("delivery_State") + "\t" +
								 rsetDeliv.getString("delivery_Zip");
						 nextOrder = new DeliveryOrder(orderID, rset.getInt("customer_CustID"),
								 rset.getString("ordertable_OrderDateTime"), rset.getDouble("ordertable_CustPrice"),
								 rset.getDouble("ordertable_BusPrice"), rset.getBoolean("ordertable_isComplete"),
								 rsetDeliv.getBoolean("delivery_IsDelivered"), address);
						 break;
				 }

				 /* ==============================================================
				  * Retrieve and add all the pizzas for the order
				  * =============================================================== */
				 nextOrder.setPizzaList(getPizzas(nextOrder));

				 /* ==============================================================
				  * Getting all the discounts for the order into a list
				  * =============================================================== */
				 nextOrder.setDiscountList(getDiscounts(nextOrder));

				 /* ==============================================================
				  * Add the order to the order list
				  * =============================================================== */
				 orderList.add(nextOrder);
			 }

		 } catch (SQLException e) {
			 e.printStackTrace();
		 }

		 conn.close();
		 return orderList;
	}

	// *****************************************************************************************************************
	// REWORKED
	// *****************************************************************************************************************
	public static Order getLastOrder() throws SQLException, IOException
	{
		/*
		 * Query the database for the LAST order added
		 * then return an Order object for that order.
		 * NOTE...there will ALWAYS be a "last order"!
		 */

		connect_to_db();
		Order lastOrder = null;

		try {
			PreparedStatement os;
			ResultSet rset;
			String query = "SELECT * FROM ordertable ORDER BY ordertable_OrderDateTime DESC LIMIT 1;";
			os = conn.prepareStatement(query);
			rset = os.executeQuery();
			rset.next();
			int orderID = rset.getInt("ordertable_OrderID");
			/* ==============================================================
			 * SWITCH statement for Dine-in, Pickup, and Delivery
			 * =============================================================== */
			switch (rset.getString("ordertable_OrderType")) {
				case dine_in:
					// Retrieving the extra values that are included in a Dine-in order
					PreparedStatement dine_os;
					ResultSet rsetDine;
					String dineQuery = "Select * From dinein Where ordertable_OrderID = ?;";
					dine_os = conn.prepareStatement(dineQuery);
					dine_os.setInt(1, orderID);
					rsetDine = dine_os.executeQuery();
					rsetDine.next();
					lastOrder = new DineinOrder(orderID, rset.getInt("customer_CustID"),
							rset.getString("ordertable_OrderDateTime"), rset.getDouble("ordertable_CustPrice"),
							rset.getDouble("ordertable_BusPrice"), rset.getBoolean("ordertable_isComplete"),
							rsetDine.getInt("dinein_TableNum"));
					break;
				case pickup:
					// Retrieving the extra values that are included in a Pickup order
					PreparedStatement pickup_os;
					ResultSet rsetPick;
					String pickQuery = "Select * From pickup Where ordertable_OrderID = ?;";
					pickup_os = conn.prepareStatement(pickQuery);
					pickup_os.setInt(1, orderID);
					rsetPick = pickup_os.executeQuery();
					rsetPick.next();
					lastOrder = new PickupOrder(orderID, rset.getInt("customer_CustID"),
							rset.getString("ordertable_OrderDateTime"), rset.getDouble("ordertable_CustPrice"),
							rset.getDouble("ordertable_BusPrice"), rsetPick.getBoolean("pickup_IsPickedUp"),
							rset.getBoolean("ordertable_isComplete"));
					break;
				case delivery:
					// Retrieving the extra values that are included in a Delivery order
					PreparedStatement deliver_os;
					ResultSet rsetDeliv;
					String delivQuery = "Select * From delivery Where ordertable_OrderID = ?;";
					deliver_os = conn.prepareStatement(delivQuery);
					deliver_os.setInt(1, orderID);
					rsetDeliv = deliver_os.executeQuery();
					rsetDeliv.next();
					String address = rsetDeliv.getString("delivery_HouseNum") + "\t" + rsetDeliv.getString("delivery_Street") +
							"\t" + rsetDeliv.getString("delivery_City") + "\t" + rsetDeliv.getString("delivery_State") + "\t" +
							rsetDeliv.getString("delivery_Zip");
					lastOrder = new DeliveryOrder(orderID, rset.getInt("customer_CustID"),
							rset.getString("ordertable_OrderDateTime"), rset.getDouble("ordertable_CustPrice"),
							rset.getDouble("ordertable_BusPrice"), rset.getBoolean("ordertable_isComplete"),
							rsetDeliv.getBoolean("delivery_IsDelivered"), address);
					break;
			}

			/* ==============================================================
			 * Retrieve and add all the pizzas for the order
			 * =============================================================== */
			lastOrder.setPizzaList(getPizzas(lastOrder));

			/* ==============================================================
			 * Retrieve and add all the discounts for the order
			 * =============================================================== */
			lastOrder.setDiscountList(getDiscounts(lastOrder));

			// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		} catch (SQLException e) {
			e.printStackTrace();
			// process the error or re-raise the exception to a higher level
		}
		conn.close();
		return lastOrder;
	}

	// *****************************************************************************************************************
	// REWORKED
	// *****************************************************************************************************************
	public static ArrayList<Order> getOrdersByDate(String date) throws SQLException, IOException
	 {
		/*
		 * Query the database for ALL the orders placed on a specific date
		 * and return a list of those orders.
		 *  
		 */
		 connect_to_db();

		 ArrayList<Order> orderList = new ArrayList<>();
		 try {
			 PreparedStatement os;
			 ResultSet rset;
			 String query = "SELECT * FROM ordertable WHERE CAST(ordertable_OrderDateTime AS DATE) = DATE(?);";
			 os = conn.prepareStatement(query);
			 os.setString(1, date);
			 rset = os.executeQuery();

			 while (rset.next()) {
				 Order nextOrder = null;
				 int orderID = rset.getInt("ordertable_OrderID");
				 /* ==============================================================
				  * SWITCH statement for Dine-in, Pickup, and Delivery
				  * =============================================================== */
				 switch (rset.getString("ordertable_OrderType")) {
					 case dine_in:
						 // Retrieving the extra values that are included in a Dine-in order
						 PreparedStatement dine_os;
						 ResultSet rsetDine;
						 String dineQuery = "Select * From dinein Where ordertable_OrderID = ?;";
						 dine_os = conn.prepareStatement(dineQuery);
						 dine_os.setInt(1, orderID);
						 rsetDine = dine_os.executeQuery();
						 rsetDine.next();
						 nextOrder = new DineinOrder(orderID, rset.getInt("customer_CustID"),
								 rset.getString("ordertable_OrderDateTime"), rset.getDouble("ordertable_CustPrice"),
								 rset.getDouble("ordertable_BusPrice"), rset.getBoolean("ordertable_isComplete"),
								 rsetDine.getInt("dinein_TableNum"));
					 	 break;
					 case pickup:
						 // Retrieving the extra values that are included in a Pickup order
						 PreparedStatement pickup_os;
						 ResultSet rsetPick;
						 String pickQuery = "Select * From pickup Where ordertable_OrderID = ?;";
						 pickup_os = conn.prepareStatement(pickQuery);
						 pickup_os.setInt(1, orderID);
						 rsetPick = pickup_os.executeQuery();
						 rsetPick.next();
						 nextOrder = new PickupOrder(orderID, rset.getInt("customer_CustID"),
								 rset.getString("ordertable_OrderDateTime"), rset.getDouble("ordertable_CustPrice"),
								 rset.getDouble("ordertable_BusPrice"), rsetPick.getBoolean("pickup_IsPickedUp"),
								 rset.getBoolean("ordertable_isComplete"));
						 break;
					 case delivery:
						 // Retrieving the extra values that are included in a Delivery order
						 PreparedStatement deliver_os;
						 ResultSet rsetDeliv;
						 String delivQuery = "Select * From delivery Where ordertable_OrderID = ?;";
						 deliver_os = conn.prepareStatement(delivQuery);
						 deliver_os.setInt(1, orderID);
						 rsetDeliv = deliver_os.executeQuery();
						 rsetDeliv.next();
						 String address = rsetDeliv.getString("delivery_HouseNum") + "\t" + rsetDeliv.getString("delivery_Street") +
								 "\t" + rsetDeliv.getString("delivery_City") + "\t" + rsetDeliv.getString("delivery_State") + "\t" +
								 rsetDeliv.getString("delivery_Zip");
						 nextOrder = new DeliveryOrder(orderID, rset.getInt("customer_CustID"),
								 rset.getString("ordertable_OrderDateTime"), rset.getDouble("ordertable_CustPrice"),
								 rset.getDouble("ordertable_BusPrice"), rset.getBoolean("ordertable_isComplete"),
								 rsetDeliv.getBoolean("delivery_IsDelivered"), address);
					 	 break;
				 }

				 // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				 // ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
				 // Getting all the pizzas for the order into a list
				 // ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
				 // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

				 PreparedStatement osPizzas;
				 ResultSet rsetPizzas;
				 String pizzasQuery;
				 pizzasQuery = "Select * From pizza Where ordertable_OrderID = ?;";
				 osPizzas = conn.prepareStatement(pizzasQuery);
				 osPizzas.setInt(1, orderID);
				 rsetPizzas = osPizzas.executeQuery();
				 ArrayList<Pizza> pizzaList = new ArrayList<>();
				 while (rsetPizzas.next()) {
					 // New pizza object
					 Pizza nextPizza = new Pizza(rsetPizzas.getInt("pizza_PizzaID"), rsetPizzas.getString("pizza_Size"),
							 rsetPizzas.getString("pizza_CrustType"), rsetPizzas.getInt("ordertable_OrderID"),
							 rsetPizzas.getString("pizza_PizzaState"), rsetPizzas.getString("pizza_PizzaDate"),
							 rsetPizzas.getDouble("pizza_CustPrice"), rsetPizzas.getDouble("pizza_BusPrice"));

					 /* ==============================================================
					  * Getting toppings for each pizza
					  * =============================================================== */
					 int pizzaID = rsetPizzas.getInt("pizza_PizzaID");
					 PreparedStatement osTops;
					 ResultSet rsetTops;
					 String topsQuery;
					 topsQuery = "SELECT * FROM TOPPING JOIN PIZZA_TOPPING ON TOPPING.topping_TopID = PIZZA_TOPPING.topping_TopID\n" +
							 "    JOIN PIZZA ON PIZZA_TOPPING.pizza_PizzaID = PIZZA.pizza_PizzaID WHERE PIZZA_TOPPING.pizza_PizzaID=?;";
					 osTops = conn.prepareStatement(topsQuery);
					 osTops.setInt(1, pizzaID);
					 rsetTops = osTops.executeQuery();
					 ArrayList<Topping> topsList = new ArrayList<>();
					 while (rsetTops.next()) {
						 Topping nextTopping = new Topping(rsetTops.getInt("topping_TopID"), rsetTops.getString("topping_TopName"), rsetTops.getDouble("topping_SmallAMT"),
								 rsetTops.getDouble("topping_MedAMT"), rsetTops.getDouble("topping_LgAMT"), rsetTops.getDouble("topping_XLAMT"), rsetTops.getDouble("topping_CustPrice"),
								 rsetTops.getDouble("topping_BusPrice"), rsetTops.getInt("topping_MinINVT"), rsetTops.getInt("topping_CurINVT"));
						 // nextTopping.setDoubled(rsetTops.getBoolean("pizza_topping_IsDouble"));
						 nextTopping.setDoubled(rsetTops.getBoolean("pizza_topping_IsDouble"));
						 // System.out.println(rsetTops.getBoolean("pizza_topping_IsDouble"));
						 topsList.add(nextTopping);
					 }
					 nextPizza.setToppings(topsList);

				 /* ==============================================================
				  * Retrieve and add all the pizzas for the order
				  * =============================================================== */
				 nextOrder.setPizzaList(getPizzas(nextOrder));

				 /* ==============================================================
				  * Retrieve and add all the discounts for the order
				  * =============================================================== */
				 nextOrder.setDiscountList(getDiscounts(nextOrder));

				 /* ==============================================================
				  * Add the order to the order list
				  * =============================================================== */
				 orderList.add(nextOrder);
				 // ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
				 // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			 }
		 } catch (SQLException e) {
			 e.printStackTrace();
			 // process the error or re-raise the exception to a higher level
		 }

		 conn.close();
		 return orderList;
	}

	// *****************************************************************************************************************
	// COMPLETE
	// *****************************************************************************************************************
	public static ArrayList<Discount> getDiscountList() throws SQLException, IOException
	{
		/* 
		 * Query the database for all the available discounts and 
		 * return them in an arrayList of discounts ordered by discount name.
		 * 
		*/
		connect_to_db();

		ArrayList<Discount> discountList = new ArrayList<>();
		try {
			PreparedStatement os;
			ResultSet rset;
			String query;
			query = "Select discount_DiscountID, discount_DiscountName, discount_Amount, discount_IsPercent From discount;";
			os = conn.prepareStatement(query);
			rset = os.executeQuery();
			while (rset.next()) {
				Discount nextDiscount = new Discount(rset.getInt("discount_DiscountID"), rset.getString("discount_Name"), rset.getDouble("discount_Amount"), rset.getBoolean("discount_IsPercent"));
				discountList.add(nextDiscount);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			// process the error or re-raise the exception to a higher level
		}

		conn.close();
		return discountList;
	}

	public static Discount findDiscountByName(String name) throws SQLException, IOException 
	{
		/*
		 * Query the database for a discount using it's name.
		 * If found, then return an OrderDiscount object for the discount.
		 * If it's not found....then return null
		 *  
		 */
		 return null;
	}


	// *****************************************************************************************************************
	// COMPLETE
	// *****************************************************************************************************************
	public static ArrayList<Customer> getCustomerList() throws SQLException, IOException
	{
        /*
         * Query the data for all the customers and return an arrayList of all the customers.
         * Don't forget to order the data coming from the database appropriately.
         *
         */
		connect_to_db();

		ArrayList<Customer> customerList = new ArrayList<>();
		try {
			PreparedStatement os;
			ResultSet rset;
			String query;
			query = "Select customer_CustID, customer_FName, customer_LName, customer_PhoneNum From customer;";
			os = conn.prepareStatement(query);
			rset = os.executeQuery();
			while (rset.next()) {
				Customer nextCust = new Customer(rset.getInt("customer_CustID"), rset.getString("customer_FName"), rset.getString("customer_LName"), rset.getString("customer_PhoneNum"));
				customerList.add(nextCust);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			// process the error or re-raise the exception to a higher level
		}

		conn.close();
		return customerList;
    }

	public static Customer findCustomerByPhone(String phoneNumber)  throws SQLException, IOException 
	{
		/*
		 * Query the database for a customer using a phone number.
		 * If found, then return a Customer object for the customer.
		 * If it's not found....then return null
		 *  
		 */
		 return null;
	}

	// *****************************************************************************************************************
	// COMPLETE
	// *****************************************************************************************************************
	public static String getCustomerName(int CustID) throws SQLException, IOException
	{
		/*
		 * COMPLETED...WORKING Example!
		 * 
		 * This is a helper method to fetch and format the name of a customer
		 * based on a customer ID. This is an example of how to interact with
		 * your database from Java.  
		 * 
		 * Notice how the connection to the DB made at the start of the 
		 *
		 */

		 connect_to_db();

		/* 
		 * an example query using a constructed string...
		 * remember, this style of query construction could be subject to sql injection attacks!
		 * 
		 */
		String cname1 = "";
		String cname2 = "";
		String query = "Select customer_FName, customer_LName From customer WHERE customer_CustID=" + CustID + ";";
		Statement stmt = conn.createStatement();
		ResultSet rset = stmt.executeQuery(query);
		
		while(rset.next())
		{
			cname1 = rset.getString(1) + " " + rset.getString(2); 
		}

		/* 
		* an BETTER example of the same query using a prepared statement...
		* with exception handling
		* 
		*/
		try {
			PreparedStatement os;
			ResultSet rset2;
			String query2;
			query2 = "Select customer_FName, customer_LName From customer WHERE customer_CustID=?;";
			os = conn.prepareStatement(query2);
			os.setInt(1, CustID);
			rset2 = os.executeQuery();
			while(rset2.next())
			{
				cname2 = rset2.getString("customer_FName") + " " + rset2.getString("customer_LName"); // note the use of field names in the getSting methods
			}
		} catch (SQLException e) {
			e.printStackTrace();
			// process the error or re-raise the exception to a higher level
		}

		conn.close();

		return cname1;
		// OR
		// return cname2;

	}


	public static ArrayList<Topping> getToppingList() throws SQLException, IOException 
	{
		/*
		 * Query the database for the aviable toppings and 
		 * return an arrayList of all the available toppings. 
		 * Don't forget to order the data coming from the database appropriately.
		 * 
		 */
		return null;
	}

	public static Topping findToppingByName(String name) throws SQLException, IOException 
	{
		/*
		 * Query the database for the topping using it's name.
		 * If found, then return a Topping object for the topping.
		 * If it's not found....then return null
		 *  
		 */
		 return null;
	}

	public static ArrayList<Topping> getToppingsOnPizza(Pizza p) throws SQLException, IOException 
	{
		/* 
		 * This method builds an ArrayList of the toppings ON a pizza.
		 * The list can then be added to the Pizza object elsewhere in the
		 */

		return null;	
	}

	public static void addToInventory(int toppingID, double quantity) throws SQLException, IOException 
	{
		/*
		 * Updates the quantity of the topping in the database by the amount specified.
		 * 
		 * */
	}


	// *****************************************************************************************************************
	// COMPLETE
	// *****************************************************************************************************************
	public static ArrayList<Pizza> getPizzas(Order o) throws SQLException, IOException
	{
		/*
		 * Build an ArrayList of all the Pizzas associated with the Order.
		 * 
		 */
		int orderID = o.getOrderID();
		PreparedStatement osPizzas;
		ResultSet rsetPizzas;
		String pizzasQuery;
		pizzasQuery = "Select * From pizza Where ordertable_OrderID = ?;";
		osPizzas = conn.prepareStatement(pizzasQuery);
		osPizzas.setInt(1, orderID);
		rsetPizzas = osPizzas.executeQuery();
		ArrayList<Pizza> pizzaList = new ArrayList<>();
		while (rsetPizzas.next()) {
			// New pizza object
			Pizza nextPizza = new Pizza(rsetPizzas.getInt("pizza_PizzaID"), rsetPizzas.getString("pizza_Size"),
					rsetPizzas.getString("pizza_CrustType"), rsetPizzas.getInt("ordertable_OrderID"),
					rsetPizzas.getString("pizza_PizzaState"), rsetPizzas.getString("pizza_PizzaDate"),
					rsetPizzas.getDouble("pizza_CustPrice"), rsetPizzas.getDouble("pizza_BusPrice"));

			/* ==============================================================
			 * Getting toppings for each pizza
			 * =============================================================== */
			nextPizza.setToppings(getToppingsOnPizza(nextPizza));

			/* ==============================================================
			 * Getting discount(s) for each pizza
			 * =============================================================== */
			nextPizza.setDiscounts(getDiscounts(nextPizza));

			/* ==============================================================
			 * Add pizza to the current list of pizzas
			 * =============================================================== */
			pizzaList.add(nextPizza);
		}

		return pizzaList;
	}

	// *****************************************************************************************************************
	// COMPLETE
	// *****************************************************************************************************************
	public static ArrayList<Discount> getDiscounts(Order o) throws SQLException, IOException
	{
		/* 
		 * Build an array list of all the Discounts associted with the Order.
		 * 
		 */
		int orderID = o.getOrderID();
		PreparedStatement osDiscs;
		ResultSet rsetDiscs;
		String discsQuery;
		discsQuery = "SELECT * FROM discount JOIN order_discount ON discount.discount_DiscountID = order_discount.discount_DiscountID \n" +
				"JOIN ordertable ON order_discount.ordertable_OrderID = ordertable.ordertable_OrderID Where ordertable.ordertable_OrderID = ?";
		osDiscs = conn.prepareStatement(discsQuery);
		osDiscs.setInt(1, orderID);
		rsetDiscs = osDiscs.executeQuery();
		ArrayList<Discount> discsList = new ArrayList<>();
		// Check if the rsetDiscs object is empty (i.e. there are NO discounts for the order)
		if (rsetDiscs.isBeforeFirst() ) {
			while (rsetDiscs.next()) {
				Discount nextDiscount = new Discount(rsetDiscs.getInt("discount_DiscountID"), rsetDiscs.getString("discount_DiscountName"),
						rsetDiscs.getDouble("discount_Amount"), rsetDiscs.getBoolean("discount_IsPercent"));
				discsList.add(nextDiscount);
			}
		}
		return discsList;
	}

	// *****************************************************************************************************************
	// COMPLETE
	// *****************************************************************************************************************
	public static ArrayList<Discount> getDiscounts(Pizza p) throws SQLException, IOException
	{
		/* 
		 * Build an array list of all the Discounts associted with the Pizza.
		 * 
		 */
		int pizzaID = p.getPizzaID();
		PreparedStatement osDiscs;
		ResultSet rsetDiscs;
		String discsQuery;
		discsQuery = "SELECT * FROM discount JOIN pizza_discount ON discount.discount_DiscountID = pizza_discount.discount_DiscountID\n" +
				"    JOIN pizza ON pizza_discount.pizza_PizzaID = pizza.pizza_PizzaID WHERE pizza_discount.pizza_PizzaID=?;";
		osDiscs = conn.prepareStatement(discsQuery);
		osDiscs.setInt(1, pizzaID);
		rsetDiscs = osDiscs.executeQuery();
		ArrayList<Discount> discsList = new ArrayList<>();
		// Check if the rsetDiscs object is empty (i.e. there are NO discounts for the pizza)
		if (rsetDiscs.isBeforeFirst()) {
			while (rsetDiscs.next()) {
				Discount nextDiscount = new Discount(rsetDiscs.getInt("discount_DiscountID"), rsetDiscs.getString("discount_DiscountName"), rsetDiscs.getDouble("discount_Amount"), rsetDiscs.getBoolean("discount_IsPercent"));
				discsList.add(nextDiscount);
			}
		}
		return discsList;
	}

	// *****************************************************************************************************************
	// COMPLETE
	// *****************************************************************************************************************
	public static double getBaseCustPrice(String size, String crust) throws SQLException, IOException
	{
		/* 
		 * Query the database fro the base customer price for that size and crust pizza.
		 * 
		*/
		connect_to_db();

		double baseCustPrice = 0.0;
		try {
			PreparedStatement osBCP;
			ResultSet rsetBCP;
			String BCPQuery = "SELECT baseprice_CustPrice FROM baseprice WHERE baseprice_Size = ? AND baseprice_CrustType = ?;";
			osBCP = conn.prepareStatement(BCPQuery);
			osBCP.setString(1, size);
			osBCP.setString(2, crust);
			rsetBCP = osBCP.executeQuery();
			rsetBCP.next();
			baseCustPrice = rsetBCP.getDouble("baseprice_CustPrice");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		conn.close();
		return baseCustPrice;
	}

	// *****************************************************************************************************************
	// COMPLETE
	// *****************************************************************************************************************
	public static double getBaseBusPrice(String size, String crust) throws SQLException, IOException
	{
		/* 
		 * Query the database fro the base business price for that size and crust pizza.
		 * 
		*/
		connect_to_db();

		double baseBusPrice = 0.0;
		try {
			PreparedStatement osBCP;
			ResultSet rsetBCP;
			String BCPQuery = "SELECT baseprice_BusPrice FROM baseprice WHERE baseprice_Size = ? AND baseprice_CrustType = ?;";
			osBCP = conn.prepareStatement(BCPQuery);
			osBCP.setString(1, size);
			osBCP.setString(2, crust);
			rsetBCP = osBCP.executeQuery();
			rsetBCP.next();
			baseBusPrice = rsetBCP.getDouble("baseprice_BusPrice");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		conn.close();
		return baseBusPrice;
	}

	
	public static void printToppingReport() throws SQLException, IOException
	{
		/*
		 * Prints the ToppingPopularity view. Remember that this view
		 * needs to exist in your DB, so be sure you've run your createViews.sql
		 * files on your testing DB if you haven't already.
		 * 
		 * The result should be readable and sorted as indicated in the prompt.
		 * 
		 * HINT: You need to match the expected output EXACTLY....I would suggest
		 * you look at the printf method (rather that the simple print of println).
		 * It operates the same in Java as it does in C and will make your code
		 * better.
		 * 
		 */
	}
	
	public static void printProfitByPizzaReport() throws SQLException, IOException 
	{
		/*
		 * Prints the ProfitByPizza view. Remember that this view
		 * needs to exist in your DB, so be sure you've run your createViews.sql
		 * files on your testing DB if you haven't already.
		 * 
		 * The result should be readable and sorted as indicated in the prompt.
		 * 
		 * HINT: You need to match the expected output EXACTLY....I would suggest
		 * you look at the printf method (rather that the simple print of println).
		 * It operates the same in Java as it does in C and will make your code
		 * better.
		 * 
		 */
	}
	
	public static void printProfitByOrderTypeReport() throws SQLException, IOException
	{
		/*
		 * Prints the ProfitByOrderType view. Remember that this view
		 * needs to exist in your DB, so be sure you've run your createViews.sql
		 * files on your testing DB if you haven't already.
		 * 
		 * The result should be readable and sorted as indicated in the prompt.
		 *
		 * HINT: You need to match the expected output EXACTLY....I would suggest
		 * you look at the printf method (rather that the simple print of println).
		 * It operates the same in Java as it does in C and will make your code
		 * better.
		 * 
		 */
	}
	
	
	
	/*
	 * These private methods help get the individual components of an SQL datetime object. 
	 * You're welcome to keep them or remove them....but they are usefull!
	 */
	private static int getYear(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(0,4));
	}
	private static int getMonth(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(5, 7));
	}
	private static int getDay(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(8, 10));
	}

	public static boolean checkDate(int year, int month, int day, String dateOfOrder)
	{
		if(getYear(dateOfOrder) > year)
			return true;
		else if(getYear(dateOfOrder) < year)
			return false;
		else
		{
			if(getMonth(dateOfOrder) > month)
				return true;
			else if(getMonth(dateOfOrder) < month)
				return false;
			else
			{
				if(getDay(dateOfOrder) >= day)
					return true;
				else
					return false;
			}
		}
	}


}