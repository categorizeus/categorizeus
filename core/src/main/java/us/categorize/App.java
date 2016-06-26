package us.categorize;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Hello world!
 *
 */
public class App {
	private static  String clearSql, createSql, dbName, dbUser, dbPass, staticDir, indexSql;
	private static  int port;

	public static void main(String args[]) throws Exception {
		clearSql = System.getProperty("user.home") + "/projects/categorizeus/core/src/main/resources/sql/clear.sql";
		createSql = System.getProperty("user.home") + "/projects/categorizeus/core/src/main/resources/sql/tables.sql";
		indexSql = System.getProperty("user.home") + "/projects/categorizeus/core/src/main/resources/sql/indices.sql";

		dbName = System.getenv("CATEGORIZEUS_DB");
		dbUser = System.getenv("CATEGORIZEUS_DB_USER");
		dbPass = System.getenv("CATEGORIZEUS_DB_PASS");
		port = Integer.parseInt(System.getenv("CATEGORIZEUS_PORT"));
		staticDir = System.getenv("CATEGORIZEUS_STATIC");
		Class.forName("org.postgresql.Driver");

		if (args.length > 0 && "initialize".equals(args[0])){
			initializeDB(args);
		}
		System.out.println("Initialization Complete");
		serverUp(args);
	}

	public static void initializeDB(String args[]) throws ClassNotFoundException, SQLException, IOException {
		Connection conn = DriverManager.getConnection("jdbc:postgresql:" + dbName, dbUser, dbPass);
		Statement st = conn.createStatement();
		executeFile(clearSql, st);
		executeFile(createSql, st);
		executeFile(indexSql, st);
		st.close();
		conn.close();
	}

	private static void executeFile(String filename, Statement st) throws IOException, SQLException {
		SQLReader init = new SQLReader(filename);
		for (String sql : init.getStatements()) {
			System.out.println("Executing " + sql);
			try {
				st.execute(sql);
			} catch (Exception e) {
				System.out.println("Error " + e.getMessage());
			}
		}
	}
	
	public static void serverUp(String args[]) throws Exception{
		System.out.println("Starting Server on Port " + port);
		Server server = new Server(port);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		System.out.println("Preparing to serve static files from " + staticDir);
		context.setResourceBase(staticDir);
		server.setHandler(context);
		MessageServlet messageServlet = new MessageServlet("msg");
		context.addServlet(new ServletHolder(messageServlet), "/msg/*");
		ThreadServlet threadServlet = new ThreadServlet();
		context.addServlet(new ServletHolder(threadServlet), "/thread/*");
		TagServlet tagServlet = new TagServlet();
		context.addServlet(new ServletHolder(tagServlet), "/tag/*");
		context.addServlet(DefaultServlet.class, "/");
		server.start();
		server.join();
	}

	public static void test(String[] args) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode actualObj = mapper.readTree("{\"k1\":\"v1\"}");
		System.out.println("Verifying Jackson Install " + actualObj.get("k1").asText());

		Class.forName("org.postgresql.Driver");
		Connection conn = DriverManager.getConnection("jdbc:postgresql:" + dbName, dbUser, dbPass);

		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("SELECT * FROM testing");
		while (rs.next()) {
			System.out.print("Column 1 returned ");
			System.out.println(rs.getString(1));
		}
		rs.close();
		st.close();

		conn.close();

	}
}
