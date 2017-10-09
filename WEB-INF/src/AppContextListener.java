import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@WebListener
public class AppContextListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            ServletContext servletContext = servletContextEvent.getServletContext();

            Class.forName("com.mysql.jdbc.Driver").newInstance();

            Properties properties = new Properties();
            properties.setProperty("user", "Moritz");
            properties.setProperty("password", "tracy310");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/d02566f2", properties);

            servletContext.setAttribute("DBConnection", connection);

            System.out.println("Database connection initialized for Application.");
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        try {
            ServletContext servletContext = servletContextEvent.getServletContext();

            Connection connection = (Connection) servletContext.getAttribute("DBConnection");
            connection.close();

            System.out.println("Database connection closed for Application.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
