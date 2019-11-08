package MyServer;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.security.MessageDigest;
import java.sql.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;

@RestController
public class UserController {
	static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost/DormDash?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
	static final String USER = "root";
//	static final String PASSWORD = "D0rmdash!";
	static final String PASSWORD = "";
	static final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
	static Connection conn = null;
	static PreparedStatement ps = null;
	static PreparedStatement ps1 = null;
	static PreparedStatement ps2 = null;

	@RequestMapping(value = "/register", method = RequestMethod.POST) // <-- setup the endpoint URL at /hello with the HTTP POST method
	public ResponseEntity<String> register(@RequestBody String body, HttpServletRequest request) {
		String username = request.getParameter("username"); //Grabbing name and age parameters
		String password = request.getParameter("password");
		String selectTableSql = "SELECT password FROM users WHERE username = ?;";
		String insertTableSql = "INSERT INTO users(username, password) VALUES(?, ?)";

		/*Creating http headers object to place into response entity the server will return.
		This is what allows us to set the content-type to application/json or any other content-type
		we would want to return */
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Content-Type", "application/json");

		//Initializing a MessageDigest object which will allow us to digest a String with SHA-256
		MessageDigest digest = null;
		String hashedKey = null;
		try {
			digest = MessageDigest.getInstance("SHA-256"); //digest algorithm set to SHA-256
			//Converts the password to SHA-256 bytes. Then the bytes are converted to hexadecimal with the helper method written below
			hashedKey = bytesToHex(digest.digest(password.getBytes("UTF-8")));
			System.out.println(hashedKey);


		}catch(Exception e) {

		}
		try {
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);

			ps = conn.prepareStatement(selectTableSql);
			ps.setString(1, username);
			System.out.println(selectTableSql);
			ResultSet rs = ps.executeQuery();

			//Checking if the hashmap contains the username trying to register and returns a BAD_REQUEST if username is taken
			if (!rs.next() ) {

				System.out.println("Insert Into the User Table");
				ps = conn.prepareStatement(insertTableSql);
				ps.setString(1, username);
				ps.setString(2, hashedKey);
				ps.executeUpdate();


			}
			else {
				return new ResponseEntity("{\"message\":\"username taken\"}", responseHeaders, HttpStatus.BAD_REQUEST);
			}
		}
		catch(ClassNotFoundException ce){
			ce.printStackTrace();
			System.out.println("Class Not found");
		}
		catch(SQLException se){
			System.out.println("SQL Error");
			se.printStackTrace();

		}

		//Returns the response with a String, headers, and HTTP status
		return new ResponseEntity(hashedKey, responseHeaders, HttpStatus.OK);
	}
	@RequestMapping(value = "/login", method = RequestMethod.POST) // <-- setup the endpoint URL at /hello with the HTTP POST method
	public ResponseEntity<String> login(HttpServletRequest request) {
		String username = request.getParameter("username"); //Grabbing name and age parameters from URL
		String password = request.getParameter("password");
		String selectTableSql = "SELECT password FROM users WHERE username = ?;";
		String storedHashedKey;



		/*Creating http headers object to place into response entity the server will return.
		This is what allows us to set the content-type to application/json or any other content-type
		we would want to return */
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Content-Type", "application/json");

		//check is person is already logged in
//		if (MyServer.ActiveUsers.contains(username)){
//			return new ResponseEntity("{\"message\":\"Already logged in.\"}", responseHeaders, HttpStatus.BAD_REQUEST);
//
//		}

		MessageDigest digest = null;
		String hashedKey = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			//Hashing the input password so that we have something to compare with the stored hashed password
			hashedKey = bytesToHex(digest.digest(password.getBytes("UTF-8")));
		}catch(Exception e) {

		}
		try{
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
			ps = conn.prepareStatement(selectTableSql);
			ps.setString(1, username);
			ResultSet rs = ps.executeQuery();
			//Check if the hashmap contains the username trying to login

			if (!rs.next()) {
				return new ResponseEntity("{\"message\":\"username not registered\"}", responseHeaders, HttpStatus.BAD_REQUEST);
			}
			else {
				try {
					storedHashedKey = rs.getString("password");
					//Compare the stored hashed key with the input hashedKey generated from the password parameter to validate the login

					if (storedHashedKey.equals(hashedKey)) {

						//We will sign our JWT with our ApiKey secret
						String jws = Jwts.builder().setHeaderParam("typ", "JWT").
                                setExpiration(new Date(System.currentTimeMillis() + 86400000)).
                                setSubject(sessionGen.randomAlphaNumeric(10)).signWith(key).compact();
						responseHeaders.set("Authorization", jws);
//						MyServer.users.put(username, jws);
						return new ResponseEntity("{\"message\":\"user logged in\"}", responseHeaders, HttpStatus.OK);
					}
				}
				catch(SQLException se) {}

			}
		}
		catch(ClassNotFoundException ce){
			ce.printStackTrace();
			System.out.println("Class Not found");
		}
		catch(SQLException se){
			System.out.println("SQL Error");
			se.printStackTrace();

		}
		return new ResponseEntity("{\"message\":\"username/password combination is incorrect\"}", responseHeaders, HttpStatus.BAD_REQUEST);

	}
	@RequestMapping(value = "/order", method = RequestMethod.POST) // <-- setup the endpoint URL at /order with the HTTP POST method
	public ResponseEntity<String> order(@RequestBody String body, HttpServletRequest request) {

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Content-Type", "application/json");
		System.out.println("made it to here");

		//section to verify authorization. If it fails jump to the catch clause

		String token = null;
        String order_id = null;
		int status = 0;

		System.out.println("This is the Token" + request.getHeader("Authorization"));


        try {
			final Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(request.getHeader("Authorization")).getBody();

			token = request.getHeader("Authorization");
			responseHeaders.set("Authorization", token);

			System.out.println(claims.getSubject());
		} catch (final SignatureException e) {
			return new ResponseEntity("{\"message\":\"Invalid Session\"}", responseHeaders, HttpStatus.FORBIDDEN);
		}

		String username = request.getParameter("username");
		String foodOrder = request.getParameter("foodOrder");
		String orderPickupLocation = request.getParameter("orderPickupLocation");
		String orderDropoffLocation = request.getParameter("orderDropoffLocation");
		String selectUsername = "SELECT username FROM users WHERE username = ?;";
		String insertSql = "INSERT INTO orders(username,foodOrder, orderPickupLocation, orderDropoffLocation) " +
				"VALUES (?, ?, ?, ?)";
        String grabOrder = "Select orderID From orders Where username = ?;";
		//get prices for orders
		String workstatus = "Select is_working from users where username = ?;";



		float price = 0;
		float price2 = 0;
		float totalPrice = 0;



		//section for ordercheck and hashmap placement

		if (foodOrder.isEmpty()) {
			return new ResponseEntity("{\"message\":\"You need to have an order\"}", responseHeaders, HttpStatus.BAD_REQUEST);

		}


		//section for SQL stuff that will save in case of server failuer
		try {

            Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);

			//get correct username
			ps = conn.prepareStatement(selectUsername);
			ps.setString(1, username);
			System.out.println(selectUsername);
			ResultSet rs = ps.executeQuery();

			//put on database
			ps = conn.prepareStatement(insertSql);
			ps.setString(1, username);
			ps.setString(2, foodOrder);
			ps.setString(3, orderPickupLocation);
			ps.setString(4, orderDropoffLocation);
			ps.executeUpdate();

            ps = conn.prepareStatement(grabOrder);
			ps.setString(1, username);
            ResultSet order_rs = ps.executeQuery();
            System.out.println(order_rs);

            while (order_rs.next()) {

                order_id = order_rs.getString("orderID");
            }

			//workstatus
			ps = conn.prepareStatement(workstatus);
			ps.setString(1, username);
			System.out.println(workstatus);

			ResultSet ds = ps.executeQuery();
			System.out.println(ds);
			while (ds.next()) {
				status = Integer.parseInt(ds.getString("is_working"));
				System.out.println(status);
			}

            //Katy Price
			String getLocationSQL = "SELECT buildingCharge FROM locations WHERE buildingName=?";

			ps1 = conn.prepareStatement(getLocationSQL);
			ps1.setString(1, String.valueOf(orderPickupLocation));
			ResultSet rs1 = ps1.executeQuery();

			if(rs1.next()) {
				price = rs1.getFloat("buildingCharge");
			}


			ps2 = conn.prepareStatement(getLocationSQL);
			ps2.setString(1, String.valueOf(orderDropoffLocation));
			ResultSet rs2 = ps2.executeQuery();

			if(rs2.next()) {
				price2 = rs2.getFloat("buildingCharge");
			}

			totalPrice = price + price2;


		} catch(ClassNotFoundException e) {
			System.out.println("Oops there was an error.");
			e.printStackTrace();
			return new ResponseEntity("{\"message\":\"Something went wrong :(\"}", responseHeaders, HttpStatus.BAD_REQUEST);
		}
		catch (SQLException se) {
            se.printStackTrace();
            return new ResponseEntity("{\"message\":\"Somehow your order already exists.(\"}", responseHeaders, HttpStatus.BAD_REQUEST);
        }

        if (MyServer.CustomerOrder.containsKey(order_id)){
            return new ResponseEntity("{\"message\":\"Order already exists.\"}", responseHeaders, HttpStatus.BAD_REQUEST);

        }

		//check if working
		if (status == 1) {
			return new ResponseEntity("{\"message\":\"You are currently working." +
					" Change your status to client.\"}", responseHeaders, HttpStatus.BAD_REQUEST);
		}


        //Create a JSONObject containing the jar file with details
		JSONObject values = new JSONObject();
		values.put("orderPickupLocation", orderPickupLocation);
		values.put("foodOrder", foodOrder);
		values.put("price", Float.toString(totalPrice));
		values.put("orderDropoffLocation", orderDropoffLocation);



		MyServer.CustomerOrder.put(order_id, username);
        MyServer.OpenOrders.put(order_id, values.toString());


		return new ResponseEntity("{\"message\":\"order placed\"}", responseHeaders, HttpStatus.OK);
	}
	//if the consumer has recieved the order
	@RequestMapping(value = "/recievedorder", method = RequestMethod.POST) // <-- setup the endpoint URL at /order with the HTTP POST method
	public ResponseEntity<String> recievedorder(@RequestBody String body, HttpServletRequest request) {

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Content-Type", "application/json");
		System.out.println("made it to here");

		//section to verify authorization. If it fails jump to the catch clause
		String token;
		try {
			final Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(request.getHeader("Authorization")).getBody();

			token = request.getHeader("Authorization");
			responseHeaders.set("Authorization", token);

			System.out.println(claims.getSubject());
		} catch (final SignatureException e) {
			return new ResponseEntity("{\"message\":\"Invalid Session\"}", responseHeaders, HttpStatus.FORBIDDEN);
		}

		String username = request.getParameter("username");
        //begin different sections that affect the user and the delivery person

        //method 1: complete order for user
        //method 2: complete order for deliveryperson

        if (MyServer.CustomerOrder.containsKey(username) || MyServer.DeliveredBy.containsKey(username)){
            if (MyServer.CustomerOrder.get(username).equals(username)){

            }
            if (MyServer.DeliveredBy.get(username).equals(username)){

            }
        }

		//section for ordercheck and hashmap placement
		if (!MyServer.CustomerOrder.containsKey(username)){
			return new ResponseEntity("{\"message\":\"You don't have an order?\"}", responseHeaders, HttpStatus.BAD_REQUEST);
		}

		MyServer.CustomerOrder.remove(username);

		return new ResponseEntity("{\"message\":\"Error\"}", responseHeaders, HttpStatus.BAD_REQUEST);

	}
	@RequestMapping(value = "/cancelorder", method = RequestMethod.POST)
	public ResponseEntity<String> cancelorder(@RequestBody String body, HttpServletRequest request) {

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Content-Type", "application/json");
        String username = request.getParameter("username");
		String orderID = request.getParameter("orderID");
		String deleteSql = "DELETE FROM orders WHERE username = ?" + " AND orderID = ?;";
		String token;

		try {
			final Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(request.getHeader("Authorization")).getBody();

			token = request.getHeader("Authorization");
			responseHeaders.set("Authorization", token);

			System.out.println(claims.getSubject());
		} catch (final SignatureException e) {
			return new ResponseEntity("{\"message\":\"Invalid Session\"}", responseHeaders, HttpStatus.FORBIDDEN);
		}

		//section for ordercheck and hashmap placement
        System.out.println("This is the keyset" + MyServer.CustomerOrder.keySet());


        if (!MyServer.CustomerOrder.containsKey(orderID)){
			return new ResponseEntity("{\"message\":\"You don't have an order?\"}", responseHeaders, HttpStatus.BAD_REQUEST);
		}
        MyServer.CustomerOrder.remove(orderID);
        MyServer.OpenOrders.remove(orderID);
        //if someone is currently delivering this order...
        if (MyServer.DeliveredBy.containsValue(orderID)){
            String key = sessionGen.getKey(MyServer.DeliveredBy, orderID);
            System.out.println(key);
            MyServer.DeliveredBy.remove(key);
            /**FIXME need to update in the future to send a message to the deliverer **/
        }

		try {

			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
			//database
			ps = conn.prepareStatement(deleteSql);
            ps.setString(1, username);
            ps.setString(2, orderID);
			ps.executeUpdate();
		} catch(ClassNotFoundException cne) {
			System.out.println("Class Not Found Exception");
            return new ResponseEntity("{\"message\":\"Class Error.\"}", responseHeaders, HttpStatus.OK);

        } catch (SQLException se) {
			System.out.println("SQL Exception");
            return new ResponseEntity("{\"message\":\"SQL Error\"}", responseHeaders, HttpStatus.OK);


        } catch(Exception e) {
			System.out.println("Oops there was an error");
            return new ResponseEntity("{\"message\":\"Some other Exception Error.\"}", responseHeaders, HttpStatus.OK);


        }
		return new ResponseEntity("{\"message\":\"Order was canceled!\"}", responseHeaders, HttpStatus.OK);

	}

	@RequestMapping(value = "/acceptorder", method = RequestMethod.POST)
	public ResponseEntity<String> acceptorder(@RequestBody String body, HttpServletRequest request) {

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Content-Type", "application/json");

		String username = request.getParameter("username");
//        String recipient = request.getParameter("recipient");
		String orderID = request.getParameter("orderID");
        String workstatus = "Select is_working from users where username = ?;";
//		String selectUsername = "SELECT username FROM orders WHERE orderID = ?;";
		int status = 0;
		String token;
		try {
			final Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(request.getHeader("Authorization")).getBody();

			token = request.getHeader("Authorization");
			responseHeaders.set("Authorization", token);

			System.out.println(claims.getSubject());
		} catch (final SignatureException e) {
			return new ResponseEntity("{\"message\":\"Invalid Session\"}", responseHeaders, HttpStatus.FORBIDDEN);
		}

		try {
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);

			//put on database
			ps = conn.prepareStatement(workstatus);
			ps.setString(1, username);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				status = Integer.parseInt(rs.getString("is_working"));
				System.out.println(status);
			}

		} catch(Exception e) {
			System.out.println("Oops there was an error");
			e.printStackTrace();
			return new ResponseEntity("{\"message\":\"Something went wrong :(\"}", responseHeaders, HttpStatus.BAD_REQUEST);
		}

		if (orderID.isEmpty()){
			return new ResponseEntity("{\"message\":\"You need to supply and Order Id.\"}", responseHeaders, HttpStatus.BAD_REQUEST);

		}


		//check if working
		if (status == 0) {
			return new ResponseEntity("{\"message\":\"You aren't working at the moment" +
					". Activate your work status.\"}", responseHeaders, HttpStatus.BAD_REQUEST);
		}

		//check if you already grabbed this particular order
		if (MyServer.DeliveredBy.containsKey(orderID) && MyServer.DeliveredBy.get(orderID).equals(username)){
			return new ResponseEntity("{\"message\":\"You already grabbed this order.\"}", responseHeaders, HttpStatus.BAD_REQUEST);
		}

		if (MyServer.OpenOrders.get(orderID) == username) {
			return new ResponseEntity("{\"message\":\"You can't grab your own order.\"}", responseHeaders, HttpStatus.BAD_REQUEST);

		}

		MyServer.DeliveredBy.put(orderID, username);
        //need to mark the master list of orders that it is taken
        MyServer.OpenOrders.remove(orderID);


		return new ResponseEntity("{\"message\":\"order accepted\"}", responseHeaders, HttpStatus.OK);



	}

	@RequestMapping(value = "/worktime", method = RequestMethod.POST) // <-- setup the endpoint URL at /hello with the HTTP POST method
	public ResponseEntity<String> worktime(HttpServletRequest request) {
		String workStatus = request.getParameter("working");
        String username = request.getParameter("username");


        HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Content-Type", "application/json");

		String updateSql = "UPDATE Users SET is_working = ? " + "WHERE username = ?;";


		String token;
		try {
			final Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(request.getHeader("Authorization")).getBody();

			token = request.getHeader("Authorization");
			responseHeaders.set("Authorization", token);

			System.out.println(claims.getSubject());
		} catch (final SignatureException e) {
			return new ResponseEntity("{\"message\":\"Invalid Session\"}", responseHeaders, HttpStatus.FORBIDDEN);
		}
		try {
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);

			//put on database
			ps = conn.prepareStatement(updateSql);
			ps.setString(1, workStatus.toString());
			ps.setString(2, username);


			ps.executeUpdate();

		} catch(Exception e) {
			System.out.println("Oops there was an error");
			e.printStackTrace();
			return new ResponseEntity("{\"message\":\"Something went wrong :(\"}", responseHeaders, HttpStatus.BAD_REQUEST);
		}
		if (Integer.parseInt(workStatus) == 0){
			return new ResponseEntity("{\"message\":\"You are not working anymore.\"}", responseHeaders, HttpStatus.OK);}
		return new ResponseEntity("{\"message\":\"You are working now.\"}", responseHeaders, HttpStatus.OK);

	}

    @RequestMapping(value = "/feed", method = RequestMethod.GET) // <-- setup the endpoint URL at /hello with the HTTP POST method
    public ResponseEntity<String> feed(HttpServletRequest request) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        JSONObject responseObj = new JSONObject(MyServer.OpenOrders);
        return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/status", method = RequestMethod.POST) // <-- setup the endpoint URL at /hello with the HTTP POST method
    public ResponseEntity<String> status(HttpServletRequest request) {


        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        int status = 0;
        String username = request.getParameter("username");
        String sqlStatement = "SELECT is_working FROM users WHERE username = ?;";
        String token;

        try {
            final Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(request.getHeader("Authorization")).getBody();

            token = request.getHeader("Authorization");
            responseHeaders.set("Authorization", token);

            System.out.println(claims.getSubject());
        } catch (final SignatureException e) {
            return new ResponseEntity("{\"message\":\"Invalid Session\"}", responseHeaders, HttpStatus.FORBIDDEN);
        }
        //SQL STUFF
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);

            //put on database
            ps = conn.prepareStatement(sqlStatement);
            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) status = Integer.parseInt(rs.getString("is_working"));

        } catch(Exception e) {
            System.out.println("Oops there was an error");
            e.printStackTrace();
            return new ResponseEntity("{\"message\":\"Something went wrong :(\"}", responseHeaders, HttpStatus.BAD_REQUEST);
        }

      //  JSONObject responseObj = new JSONObject(MyServer.OpenOrders);
      //  return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.OK);

        //select the isWorking from users
        //see worktime similar code, change sql statement
        if (status == 1) return new ResponseEntity("y", responseHeaders, HttpStatus.OK);
        return new ResponseEntity("n", responseHeaders, HttpStatus.OK);


    }
//    @RequestMapping(value = "/logout", method = RequestMethod.POST) // <-- setup the endpoint URL at /hello with the HTTP POST method
//    public ResponseEntity<String> logout(HttpServletRequest request) {
//		MyServer.ActiveUsers.remove(request.getParameter("username"));
//        HttpHeaders responseHeaders = new HttpHeaders();
//        responseHeaders.set("Content-Type", "application/json");
//        JSONObject responseObj = new JSONObject(MyServer.OpenOrders);
//        return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.OK);
//    }




	//Helper method to convert bytes into hexadecimal
	public static String bytesToHex(byte[] in) {
		StringBuilder builder = new StringBuilder();
		for(byte b: in) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
	}
}
