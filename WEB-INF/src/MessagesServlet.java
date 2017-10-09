import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public class MessagesServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/plain");

        PrintWriter out = response.getWriter();

        Map<String, String[]> params = request.getParameterMap();
        if (!params.containsKey("uid")) {
            out.write("-missing userid");
            out.close();
            return;
        }
        String uid = params.get("uid")[0];
        String date = "0";
        if (params.containsKey("mdate")) {
            date = params.get("mdate")[0];
        } else {
            out.write("-add mdate parameter to increase performance_ next _\n");
        }
        List<String> sent = new List<>();
        int ccount = 0;
        int ucount = 0;
        int acount = 0;

        try {
            Connection connection = (Connection) getServletContext().getAttribute("DBConnection");

            while (true) {
                Statement statementMessages = connection.createStatement();
                if (statementMessages.execute("SELECT mid, mtext, m.cid, m.uid, UNIX_TIMESTAMP(mdate) as mdate FROM Messages m INNER JOIN Assoziation a ON a.cid = m.cid WHERE a.uid = " + uid + " AND UNIX_TIMESTAMP(mdate) > " + date + " ORDER BY mdate DESC")) {
                    ResultSet resultSet = statementMessages.getResultSet();
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
                statementMessages.close();

                Statement statementChatsCount = connection.createStatement();
                if (statementChatsCount.execute("SELECT COUNT(*) FROM Chats c INNER JOIN Assoziation a ON c.cid = a.cid AND a.uid = " + uid)) {
                    ResultSet resultSetCount = statementChatsCount.getResultSet();
                    if (resultSetCount.first() && ccount != resultSetCount.getInt(0)) {
                        ccount = resultSetCount.getInt(0);
                        Statement statementChats = connection.createStatement();
                        if (statementChats.execute("SELECT c.cid, c.cname, c.ctype FROM Chats c INNER JOIN Assoziation a ON c.cid = a.cid AND a.uid = " + uid)) {
                            ResultSet resultSet = statementChats.getResultSet();
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
                        statementChats.close();
                    }
                    resultSetCount.close();
                }
                statementChatsCount.close();

                Statement statementUserCount = connection.createStatement();
                if (statementUserCount.execute("SELECT COUNT(*) FROM Users")) {
                    ResultSet resultSetCount = statementUserCount.getResultSet();
                    if (resultSetCount.first() && ucount != resultSetCount.getInt(0)) {
                        ucount = resultSetCount.getInt(0);
                        Statement statementUser = connection.createStatement();
                        if (statementUser.execute("SELECT uid, uname, uklasse, upermission, udefaultname FROM Users")) {
                            ResultSet resultSet = statementUser.getResultSet();
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
                        statementUser.close();
                    }
                    resultSetCount.close();
                }
                statementUserCount.close();

                Statement statementAssoziationCount = connection.createStatement();
                if (statementAssoziationCount.execute("SELECT COUNT(*) FROM Assoziation a1 INNER JOIN Assoziation a2 ON a1.cid = a2.cid WHERE a1.uid = " + uid)) {
                    ResultSet resultSet = statementAssoziationCount.getResultSet();
                    if (resultSet.first() && resultSet.getInt(1) != acount) {
                        out.append("a_ next _\n")
                                .flush();
                        acount = resultSet.getInt(1);
                    }
                    resultSet.close();
                }
                statementAssoziationCount.close();
            }
        } catch (SQLException | ClassCastException e) {
            out.append('-');
            e.printStackTrace(out);
            out.append("_ next _");
        }

        out.close();
    }
}