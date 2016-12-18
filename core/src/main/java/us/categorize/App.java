package us.categorize;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.Properties;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import us.categorize.communication.creation.attachment.AttachmentHandler;
import us.categorize.communication.creation.attachment.FileSystemAttachmentHandler;
import us.categorize.communication.query.ThreadCriteria;
import us.categorize.model.MessageThread;
import us.categorize.model.Tag;
import us.categorize.repository.MessageRepository;
import us.categorize.repository.SQLMessageRepository;
import us.categorize.repository.SQLTagRepository;
import us.categorize.repository.SQLUserRepository;
import us.categorize.repository.TagRepository;
import us.categorize.repository.UserRepository;
import us.categorize.server.FilesystemMultipartHandler;
import us.categorize.server.http.AuthFilter;
import us.categorize.server.http.MessageServlet;
import us.categorize.server.http.MultipartHandler;
import us.categorize.server.http.SessionCookieFilter;
import us.categorize.server.http.TagServlet;
import us.categorize.server.http.ThreadServlet;
import us.categorize.server.http.UploadServlet;
import us.categorize.server.http.UserServlet;

public class App {
	private static  String clearSql, createSql, dbName, dbUser, dbPass, staticDir, indexSql, seedSql, fileBase;
	private static long maxUploadSize = -1;
	private static double maxThumbWidth, maxThumbHeight;
	
	private static  int port;

	public static void main(String args[]) throws Exception {


		Properties properties = new Properties();
		properties.load(App.class.getResourceAsStream("/categorizeus.properties"));
		System.out.println("Connecting to " + properties.getProperty("DB_NAME") + " as " + properties.getProperty("DB_USER"));
		
		//TODO put these all into an automapped java bean with defaults
		clearSql = properties.getProperty("SQL_BASE") + "core/src/main/resources/sql/clear.sql";//TODO refactor to load from the jar as above
		createSql = properties.getProperty("SQL_BASE") + "core/src/main/resources/sql/tables.sql";
		indexSql = properties.getProperty("SQL_BASE") + "core/src/main/resources/sql/indices.sql";
		seedSql = properties.getProperty("SQL_BASE") + "core/src/main/resources/sql/seed.sql";		
		dbName = properties.getProperty("DB_NAME");
		dbUser = properties.getProperty("DB_USER");
		dbPass = properties.getProperty("DB_PASS");
		maxUploadSize = Long.parseLong(properties.getProperty("MAX_UPLOAD_SIZE"));
		maxThumbWidth = Double.parseDouble(properties.getProperty("MAX_THUMB_WIDTH"));
		maxThumbHeight = Double.parseDouble(properties.getProperty("MAX_THUMB_HEIGHT"));
		
		port = Integer.parseInt(properties.getProperty("PORT"));
		staticDir = properties.getProperty("STATIC_DIR");
		fileBase = staticDir + "/files";

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
		executeFile(seedSql, st);
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
		Connection conn = DriverManager.getConnection("jdbc:postgresql:" + dbName, dbUser, dbPass);
		UserRepository userRepository = new SQLUserRepository(conn);
		TagRepository tagRepository = new SQLTagRepository(conn);
		MessageRepository messageRepository = new SQLMessageRepository(conn, userRepository);

		Tag repliesTo = tagRepository.tagFor("repliesTo");
		ThreadCriteria criteria = new ThreadCriteria();
		criteria.setSearchTags(new Tag[]{tagRepository.tagFor("tag1")});
		criteria.setTransitiveTags(new Tag[]{repliesTo});
		MessageThread thread = messageRepository.loadThread(criteria);
		System.out.println(thread);
		System.out.println("Starting Server on Port " + port);
		Server server = new Server(port);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);		
		context.setContextPath("/");
		System.out.println("Preparing to serve static files from " + staticDir);
		context.setResourceBase(staticDir);
		
		SessionCookieFilter sessionCookieFilter = new SessionCookieFilter(userRepository);
		FilterHolder sessionCookieFilterHolder = new FilterHolder(sessionCookieFilter);
		context.addFilter(sessionCookieFilterHolder, "/*", EnumSet.of(DispatcherType.REQUEST));
		
		ServletHandler handler = new ServletHandler();
		FilterHolder filterHolder = handler.addFilterWithMapping(AuthFilter.class, "/msg/*", EnumSet.of(DispatcherType.REQUEST));
		
		context.addFilter(filterHolder, "/msg/*", EnumSet.of(DispatcherType.REQUEST));
		
		MultipartHandler multipartHandler = new FilesystemMultipartHandler(messageRepository,maxThumbWidth, maxThumbHeight, tagRepository, fileBase);
		UploadServlet uploadServlet = new UploadServlet(maxUploadSize, messageRepository, tagRepository, multipartHandler);
		context.addServlet(new ServletHolder(uploadServlet), "/msg/upload/*");
		
		MessageServlet messageServlet = new MessageServlet(messageRepository, userRepository, tagRepository, new FileSystemAttachmentHandler(fileBase), maxThumbWidth, maxThumbHeight);
		context.addServlet(new ServletHolder(messageServlet), "/msg/*");
		ThreadServlet threadServlet = new ThreadServlet(tagRepository, messageRepository);
		context.addServlet(new ServletHolder(threadServlet), "/thread/*");
		TagServlet tagServlet = new TagServlet(messageRepository, tagRepository);
		context.addServlet(new ServletHolder(tagServlet), "/tag/*");
		
		UserServlet userServlet = new UserServlet(userRepository);
		context.addServlet(new ServletHolder(userServlet), "/user/*");
		
		context.addServlet(DefaultServlet.class, "/");
		server.setHandler(context);
		server.start();
		server.join();
	}
	
}
