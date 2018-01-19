import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/")
public class WebSocket {
    private ClientData client;

    @OnOpen
    public void onOpen(Session session) {
        client = new ClientData(session);
        System.out.println("new connection: " + session.toString());
    }

    @OnClose
    public void onClose(Session session) {
        try {
            Connection c = client.getConnection();
            c.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception ignored) {

        }
    }

    private static String getStacktrace(Throwable t) {
        StringBuilder builder = new StringBuilder(t.toString());
        for (StackTraceElement element : t.getStackTrace()) {
            builder.append("\n    at ")
                    .append(element.toString());
        }
        return builder.toString();
    }

    @OnError
    public void onError(Throwable e) {
        e.printStackTrace();
        try {
            client.getConnection().close();
        } catch (Exception ignored) {

        }
    }

    @OnMessage
    public String onMessage(String message, Session session) {
        if (message.startsWith("uid")) {

            client.setUid(
                    Integer.parseInt(
                            message.substring(message.indexOf('=') + 1)
                    )
            );

            return "+OK";

        }

        if (message.startsWith("mdate")) {

            client.setMdate(
                    message.substring(message.indexOf('=') + 1)
            );

            return "+OK";

        }

        if (message.startsWith("request") && client.getMdate() != null && client.getUid() != 0) {
//            try {
//
//                Class.forName("com.mysql.jdbc.Driver").newInstance();
//                client.setConnection(
//                        DriverManager.getConnection(
//                                "jdbc:mysql://ucloud.sql.regioit.intern:3306/leoapp",
//                                "leo",
//                                "!LeO!2013"
//                        )
//                );
//
//                new ReceiveThread().start();
//
//                return "+OK";
//
//            } catch (IllegalAccessException | InstantiationException | SQLException | ClassNotFoundException e) {
//
//                return "-ERR " + getStacktrace(e);
//
//            }

            return "+OK";
        }

        if (message.startsWith("c+") && client.getUid() != 0) {

            String ctype   = message.charAt(message.indexOf('\'') + 1) == 'G' ? "'group'" : "'private'";
            String cname   = '\'' + message.substring(message.indexOf(';') + 1) + '\'';
            String ccreate = '\'' + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + '\'';
            String query   = "INSERT INTO Chats VALUES(null, " + cname + ", " + ctype + ", " + ccreate + ")";

            try {

                Connection connection = client.getConnection();
                Statement  statement  = connection.createStatement();

                statement.execute(query);

                SQLWarning warning = statement.getWarnings();
                String     warnings;
                if (warning != null)
                    warnings = getStacktrace(warning);
                else
                    warnings = null;

                statement.execute("SELECT cid FROM Chats WHERE ccreate = " + ccreate);
                ResultSet set = statement.getResultSet();
                set.first();
                String cid = set.getString(1);
                set.close();

                query = "INSERT INTO Assoziation VALUES(" + cid + ", " + client.getUid() + ")";
                statement.execute(query);

                if (ctype.equals("'private'")) {
                    query = "INSERT INTO Assoziation VALUES(" + cid + ", " + cname.substring(0, cname.indexOf(' ')) + ")";
                    statement.execute(query);
                }

                statement.close();

                return warnings == null ? "+OK id" + cid : "-ERR " + warnings;

            } catch (Exception e) {
                return "-ERR " + getStacktrace(e);
            }
        }

        if (message.startsWith("a+") && client.getUid() != 0) {

            String cid   = message.substring(message.indexOf(' ') + 1, message.indexOf(';'));
            String uid   = message.substring(message.indexOf(';') + 1);
            String query = "INSERT INTO Assoziation VALUES(" + cid + ", " + uid + ")";

            try {

                Connection connection = client.getConnection();
                Statement  statement  = connection.createStatement();

                statement.execute(query);

                SQLWarning warning = statement.getWarnings();
                String     warnings;
                if (warning != null)
                    warnings = getStacktrace(warning);
                else
                    warnings = null;
                statement.close();

                return warnings == null ? "+OK" : "-ERR " + warnings;

            } catch (Exception e) {
                return "-ERR " + getStacktrace(e);
            }
        }

        if (message.startsWith("a-") && client.getUid() != 0) {

            String cid   = message.substring(message.indexOf(' ') + 1, message.indexOf(';'));
            String uid   = message.substring(message.indexOf(';') + 1);
            String query = "DELETE FROM Assoziation WHERE cid = " + cid + " AND uid = " + uid;

            try {

                Connection connection = client.getConnection();
                Statement  statement  = connection.createStatement();

                statement.execute(query);

                SQLWarning warning = statement.getWarnings();
                String     warnings;
                if (warning != null)
                    warnings = getStacktrace(warning);
                else
                    warnings = null;
                statement.close();

                return warnings == null ? "+OK" : "-ERR " + warnings;

            } catch (Exception e) {
                return "-ERR " + getStacktrace(e);
            }
        }

        if (message.startsWith("m+") && client.getUid() != 0) {

            int i  = message.indexOf(';');
            int i2 = message.indexOf(';', i + 1);
            for (char c : message.substring(i2 + 1).toCharArray()) {
                System.out.println(c);
                System.out.println((int) c);
            }
            System.out.println(message.substring(i2 + 1));

            String cid   = message.substring(message.indexOf(' ') + 1, i);
            String mkey  = Encryption.decryptKey(message.substring(i + 1, i2));
            String mtext = '\'' + Encryption.decrypt(message.substring(i2 + 1), mkey) + '\'';
            String mdate = '\'' + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + '\'';
            String query = "INSERT INTO Messages VALUES (null, " + client.getUid() + ", " + mtext + ", " + cid + ", " + mdate + ")";

            try {

                Connection connection = client.getConnection();
                Statement  statement  = connection.createStatement();

                statement.execute(query);

                SQLWarning warning = statement.getWarnings();
                String     warnings;
                if (warning != null)
                    warnings = getStacktrace(warning);
                else
                    warnings = null;
                statement.close();

                return warnings == null ? "+OK" : "-ERR " + warnings;

            } catch (Exception e) {
                return "-ERR " + getStacktrace(e);
            }

        }

        return "-ERR not part of the protocol";
    }

    private class ReceiveThread extends Thread {
        @Override
        public void run() {
            try {
                while (client.getSession().isOpen()) {

                    Statement statementMessages = client.getConnection().createStatement();
                    String    sqlMessages       = "SELECT mid, mtext, m.cid, m.uid, UNIX_TIMESTAMP(mdate) as mdate FROM Messages m INNER JOIN Assoziation a ON a.cid = m.cid WHERE a.uid = " + client.getUid() + " AND UNIX_TIMESTAMP(mdate) > " + client.getMdate() + " ORDER BY mdate DESC";

                    if (statementMessages.execute(sqlMessages)) {

                        ResultSet resultSet = statementMessages.getResultSet();

                        if (resultSet.first()) {

                            client.setMdate(resultSet.getString(5));

                            for (; !resultSet.isAfterLast(); resultSet.next()) {
                                String text = resultSet.getString(2)
                                        .replace("_ ; _", "_  ;  _");
                                String key = Encryption.createKey(text);
                                text = Encryption.encrypt(text, key);
                                key = Encryption.encryptKey(key);

                                client.getSession().getBasicRemote().sendText(
                                        "m" +
                                                resultSet.getString(1) +
                                                "_ ; _" +
                                                text +
                                                "_ ; _" +
                                                key +
                                                "_ ; _" +
                                                resultSet.getString(5) +
                                                "_ ; _" +
                                                resultSet.getString(3) +
                                                "_ ; _" +
                                                resultSet.getString(4)
                                );
                            }

                        }
                        resultSet.close();

                    }
                    statementMessages.close();

                    Statement statementChats = client.getConnection().createStatement();
                    String    sqlChats       = "SELECT c.cid, c.cname, c.ctype FROM Chats c INNER JOIN Assoziation a ON c.cid = a.cid AND a.uid = " + client.getUid();

                    if (statementChats.execute(sqlChats)) {

                        ResultSet resultSet = statementChats.getResultSet();

                        if (resultSet.first()) {

                            for (; !resultSet.isAfterLast(); resultSet.next()) {

                                String s = "c" +
                                        resultSet.getString(1) +
                                        "_ ; _" +
                                        resultSet.getString(2).replace("_ ; _", "_  ;  _") +
                                        "_ ; _" +
                                        resultSet.getString(3);

                                if (!client.getSent().contains(s)) {
                                    client.getSession().getBasicRemote().sendText(s);
                                    client.getSent().append(s);
                                }

                            }

                        }
                        resultSet.close();

                    }
                    statementChats.close();

                    Statement statementUser = client.getConnection().createStatement();
                    String    sqlUser       = "SELECT uid, uname, uklasse, upermission, udefaultname FROM Users";

                    if (statementUser.execute(sqlUser)) {

                        ResultSet resultSet = statementUser.getResultSet();

                        if (resultSet.first()) {

                            for (; !resultSet.isAfterLast(); resultSet.next()) {
                                String s = "u" +
                                        resultSet.getString(1) +
                                        "_ ; _" +
                                        resultSet.getString(2).replace("_ ; _", "_  ;  _") +
                                        "_ ; _" +
                                        resultSet.getString(3) +
                                        "_ ; _" +
                                        resultSet.getString(4) +
                                        "_ ; _" +
                                        resultSet.getString(5);

                                if (!client.getSent().contains(s)) {
                                    client.getSession().getBasicRemote().sendText(s);
                                    client.getSent().append(s);
                                }

                            }

                        }

                        resultSet.close();

                    }
                    statementUser.close();

                    Statement statementAssoziationCount = client.getConnection().createStatement();
                    String    sqlAssoziationCount       = "SELECT COUNT(*) FROM Assoziation a1 INNER JOIN Assoziation a2 ON a1.cid = a2.cid WHERE a1.uid = " + client.getUid();

                    if (statementAssoziationCount.execute(sqlAssoziationCount)) {

                        ResultSet resultSetCount = statementAssoziationCount.getResultSet();

                        if (!resultSetCount.first()) {
                            client.getSession().getBasicRemote().sendText("-ERR " + statementAssoziationCount.getWarnings());
                        } else if (client.getAcount() != resultSetCount.getInt(1)) {

                            Statement statementAssoziation = client.getConnection().createStatement();
                            String    sqlAssoziation       = "SELECT a2.cid, a2.uid FROM Assoziation a1 INNER JOIN Assoziation a2 ON a1.cid = a2.cid WHERE a1.uid = " + client.getUid();

                            if (statementAssoziation.execute(sqlAssoziation)) {

                                ResultSet     resultSet = statementAssoziation.getResultSet();
                                StringBuilder builder   = new StringBuilder("a");

                                if (resultSet.first()) {

                                    for (; !resultSet.isAfterLast(); resultSet.next()) {

                                        builder.append(resultSet.getString(1))
                                                .append(",")
                                                .append(resultSet.getString(2))
                                                .append(";");

                                    }

                                }
                                resultSet.close();

                                client.getSession().getBasicRemote().sendText(builder.toString());

                            }
                            statementAssoziation.close();

                            client.setAcount(resultSetCount.getInt(1));

                        }
                        resultSetCount.close();

                    }
                    statementAssoziationCount.close();

                }

            } catch (Exception e) {

                e.printStackTrace();

            } finally {

                try {
                    client.getConnection().close();
                } catch (Exception ignored) {

                }

            }
        }
    }
}