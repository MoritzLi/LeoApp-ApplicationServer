import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Map;
import java.util.Properties;

@WebServlet("/getMessages")
public class MessagesServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/plain");

        PrintWriter out = response.getWriter();

        Map<String, String[]> params = request.getParameterMap();
        if (!params.containsKey("uid"))
            return;
        String uid = params.get("uid")[0];
        System.out.println(uid);

        List<String> sent = new List<>();
        int acount = 0;

        try {
            boolean running = true;
            Properties properties = new Properties();
            properties.setProperty("user", "Moritz");
            properties.setProperty("password", "tracy310");
            properties.setProperty("useSSL", "false");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/d02566f2", properties);
            String date = "0";
            while (running) {
                Statement statement1 = connection.createStatement();
                if (statement1.execute("SELECT mid, mtext, m.cid, m.uid, UNIX_TIMESTAMP(mdate) as mdate FROM Messages m INNER JOIN Assoziation a ON a.cid = m.cid WHERE a.uid = " + uid + " AND UNIX_TIMESTAMP(mdate) > " + date + " ORDER BY mdate DESC")) {
                    ResultSet resultSet = statement1.getResultSet();
                    if (resultSet.first()) {
                        date = resultSet.getString(5);
                        for (; !resultSet.isAfterLast(); resultSet.next()) {
                            String text = resultSet.getString(2).replace("_ ; _", "_  ;  _").replace("_ next _", "_  next  _");
                            String key = Encryption.createKey(text);
                            text = Encryption.encrypt(text, key);
                            key = Encryption.encryptKey(key);

                            out.append('m')
                                    .append(resultSet.getString(1))
                                    .append("_ ; _")
                                    .append(text)
                                    .append("_ ; _")
                                    .append(key)
                                    .append("_ ; _")
                                    .append(resultSet.getString(5))
                                    .append("_ ; _")
                                    .append(resultSet.getString(3))
                                    .append("_ ; _")
                                    .append(resultSet.getString(4))
                                    .append("_ next _")
                                    .append("\n");
                        }
                        out.flush();
                    }
                    resultSet.close();
                }
                statement1.close();

                Statement statement2 = connection.createStatement();
                if (statement2.execute("SELECT c.cid, c.cname, c.ctype FROM Chats c INNER JOIN Assoziation a ON c.cid = a.cid AND a.uid = " + uid)) {
                    ResultSet resultSet = statement2.getResultSet();
                    if (resultSet.first()) {
                        for (; !resultSet.isAfterLast(); resultSet.next()) {
                            String s = "c" +
                                    resultSet.getString(1) +
                                    "_ ; _" +
                                    resultSet.getString(2).replace("_ ; _", "_  ;  _").replace("_ next _", "_  next  _") +
                                    "_ ; _" +
                                    resultSet.getString(3) +
                                    "_ next _" +
                                    "\n";
                            if (!sent.contains(s)) {
                                out.append(s)
                                        .flush();
                                sent.append(s);
                            }
                        }
                    }
                    resultSet.close();
                }
                statement2.close();

                Statement statement3 = connection.createStatement();
                if (statement3.execute("SELECT uid, uname, uklasse, upermission, udefaultname FROM Users")) {
                    ResultSet resultSet = statement3.getResultSet();
                    if (resultSet.first()) {
                        for (; !resultSet.isAfterLast(); resultSet.next()) {
                            String s = "u" +
                                    resultSet.getString(1) +
                                    "_ ; _" +
                                    resultSet.getString(2).replace("_ ; _", "_  ;  _").replace("_ next _", "_  next  _") +
                                    "_ ; _" +
                                    resultSet.getString(3) +
                                    "_ ; _" +
                                    resultSet.getString(4) +
                                    "_ ; _" +
                                    resultSet.getString(5) +
                                    "_ next _" +
                                    "\n";
                            if (!sent.contains(s)) {
                                out.append(s)
                                        .flush();
                                sent.append(s);
                            }
                        }
                    }
                    resultSet.close();
                }
                statement3.close();

                Statement statement4 = connection.createStatement();
                if (statement4.execute("SELECT COUNT(*) FROM Assoziation a1 INNER JOIN Assoziation a2 ON a1.cid = a2.cid WHERE a1.uid = " + uid)) {
                    ResultSet resultSet = statement4.getResultSet();
                    if (resultSet.first() && resultSet.getInt(1) != acount) {
                        out.append("a_ next _\n")
                                .flush();
                        acount = resultSet.getInt(1);
                    }
                    resultSet.close();
                }
                statement4.close();
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        out.close();
    }
}