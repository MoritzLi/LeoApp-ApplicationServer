import java.io.*;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import javax.servlet.*;
import javax.servlet.http.*;

public class Test0 extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/plain");

        PrintWriter out = response.getWriter();

        Map<String, String[]> params = request.getParameterMap();
        int uid = Integer.parseInt(params.get("uid")[0]);
        System.out.println(uid);
        out.flush();

        try {
            boolean running = true;
            Properties properties = new Properties();
            properties.setProperty("user", "Moritz");
            properties.setProperty("password", "tracy310");
            properties.setProperty("useSSL", "false");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/d02566f2", properties);
            String date = "0";
            while (running) {
                Statement statement = connection.createStatement();
                if (statement.execute("SELECT mid, mtext, m.cid, m.uid, UNIX_TIMESTAMP(mdate) as mdate FROM Messages m INNER JOIN Assoziation a ON a.cid = m.cid WHERE a.uid = " + uid + " AND UNIX_TIMESTAMP(mdate) > " + date + " ORDER BY mdate DESC")) {
                    ResultSet resultSet = statement.getResultSet();
                    if (resultSet.first()) {
                        date = resultSet.getString(5);
                        for (; !resultSet.isAfterLast(); resultSet.next()) {
                            out.append(resultSet.getString(1))
                                    .append("_;_")
                                    .append(resultSet.getString(2))
                                    .append("_;_")
                                    .append(resultSet.getString(5))
                                    .append("_;_")
                                    .append(resultSet.getString(3))
                                    .append("_;_")
                                    .append(resultSet.getString(4))
                                    .append("_nextMessage_");
                        }
                        out.flush();
                    }
                    resultSet.close();
                }
                statement.close();
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        out.close();
    }
}