import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        System.out.println(session.getMaxTextMessageBufferSize());
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

        } else if (message.startsWith("mdate")) {

            client.setMdate(
                    message.substring(message.indexOf('=') + 1)
            );

            return "+OK";

        } else if (message.startsWith("request") && client.getMdate() != null && client.getUid() != 0) {
            try {

                Class.forName("com.mysql.jdbc.Driver").newInstance();
                client.setConnection(
                        DriverManager.getConnection(
                                "jdbc:mysql://localhost:3306/leoapp"/*"jdbc:mysql://ucloud.sql.regioit.intern:3306/leoapp" TODO*/,
                                "leo",
                                "!LeO!2013"
                        )
                );

                new ReceiveThread().start();

                return "+OK";

            } catch (IllegalAccessException | InstantiationException | SQLException | ClassNotFoundException e) {

                return "-ERR " + getStacktrace(e);

            }
        } else if (message.startsWith("c+") && client.getUid() != 0) {

            String ctype   = message.charAt(message.indexOf('\'') + 1) == 'G' ? "'group'" : "'private'";
            String cname   = '\'' + message.substring(message.indexOf(';') + 1) + '\'';
            String ccreate = '\'' + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + '\'';
            String query   = "INSERT INTO chats VALUES(null, " + cname + ", " + ctype + ", " + ccreate + ")";
            System.out.println(query);

            try {

                Connection connection = client.getConnection();
                Statement  statement  = connection.createStatement();
                boolean    b          = statement.execute(query);
                statement.close();
                System.out.println(b);

                return b ? "+OK" : "-ERR " + statement.getWarnings().getMessage();

            } catch (Exception e) {
                e.printStackTrace();
                return "-ERR";
            }
        } else if (message.startsWith("a+") && client.getUid() != 0) {

            String cid   = message.substring(message.indexOf(' ') + 1, message.indexOf(';'));
            String uid   = message.substring(message.indexOf(';') + 1);
            String query = "INSERT INTO assoziation VALUES(" + cid + ", " + uid + ")";

            try {

                Connection connection = client.getConnection();
                Statement  statement  = connection.createStatement();
                boolean    b          = statement.execute(query);
                statement.close();
                System.out.println(b);

                return b ? "+OK" : "-ERR " + statement.getWarnings().getMessage();

            } catch (Exception e) {
                e.printStackTrace();
                return "-ERR";
            }
        } else if (message.startsWith("m+") && client.getUid() != 0) {
            //TODO Encryption
            String cid   = message.substring(message.indexOf(' ') + 1, message.indexOf(';'));
            String mtext = message.substring(message.indexOf(';'));
            String mdate = '\'' + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + '\'';
            String query = "INSERT INTO messages VALUES (null, " + client.getUid() + ", " + mtext + ", " + cid + ", " + mdate + ")";

            try {

                Connection connection = client.getConnection();
                Statement  statement  = connection.createStatement();
                boolean    b          = statement.execute(query);
                statement.close();
                System.out.println(b);

                return b ? "+OK" : "-ERR " + statement.getWarnings().getMessage();

            } catch (Exception e) {
                e.printStackTrace();
                return "-ERR";
            }
        }

        return "-not part of the protocol";
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

                            for (; !resultSet.isAfterLast(); resultSet.next()) {
                                String text = resultSet.getString(2)
                                        .replace("_ ; _", "_  ;  _")
                                        .replace("_ next _", "_  next  _");
                                String key = Encryption.createKey(text);
                                text = Encryption.encrypt(text, key);
                                key = Encryption.encryptKey(key);

                                client.getSession().getAsyncRemote().sendText(
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
                                                resultSet.getString(4) +
                                                "_ next _"
                                );
                            }

                            client.setMdate(resultSet.getString(5));

                        }

                        resultSet.close();

                    }

                    statementMessages.close();

                    Statement statementChatsCount = client.getConnection().createStatement();
                    String    sqlChatsCount       = "SELECT COUNT(*) FROM Chats c INNER JOIN Assoziation a ON c.cid = a.cid AND a.uid = " + client.getUid();

                    if (statementChatsCount.execute(sqlChatsCount)) {

                        ResultSet resultSetCount = statementChatsCount.getResultSet();

                        if (resultSetCount.first() && client.getCcount() != resultSetCount.getInt(1)) {

                            Statement statementChats = client.getConnection().createStatement();
                            String    sqlChats       = "SELECT c.cid, c.cname, c.ctype FROM Chats c INNER JOIN Assoziation a ON c.cid = a.cid AND a.uid = " + client.getUid();

                            if (statementChats.execute(sqlChats)) {

                                ResultSet resultSet = statementChats.getResultSet();

                                if (resultSet.first()) {

                                    for (; !resultSet.isAfterLast(); resultSet.next()) {

                                        String s = "c" +
                                                resultSet.getString(1) +
                                                "_ ; _" +
                                                resultSet.getString(2).replace("_ ; _", "_  ;  _").replace("_ next _", "_  next  _") +
                                                "_ ; _" +
                                                resultSet.getString(3) +
                                                "_ next _";

                                        if (!client.getSent().contains(s)) {
                                            client.getSession().getAsyncRemote().sendText(s);
                                            client.getSent().append(s);
                                        }

                                    }

                                }

                                resultSet.close();

                            }

                            statementChats.close();

                            client.setCcount(resultSetCount.getInt(1));

                        }

                        resultSetCount.close();

                    }

                    statementChatsCount.close();

                    Statement statementUserCount = client.getConnection().createStatement();
                    String    sqlUserCount       = "SELECT COUNT(*) FROM Users";

                    if (statementUserCount.execute(sqlUserCount)) {

                        ResultSet resultSetCount = statementUserCount.getResultSet();

                        if (resultSetCount.first() && client.getUcount() != resultSetCount.getInt(1)) {

                            Statement statementUser = client.getConnection().createStatement();
                            String    sqlUser       = "SELECT uid, uname, uklasse, upermission, udefaultname FROM Users";

                            if (statementUser.execute(sqlUser)) {

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
                                                "_ next _";

                                        if (!client.getSent().contains(s)) {
                                            client.getSession().getAsyncRemote().sendText(s);
                                            client.getSent().append(s);
                                        }

                                    }

                                }

                                resultSet.close();

                            }

                            statementUser.close();

                            client.setUcount(resultSetCount.getInt(1));

                        }

                        resultSetCount.close();

                    }

                    statementUserCount.close();

                    Statement statementAssoziationCount = client.getConnection().createStatement();
                    String    sqlAssoziationCount       = "SELECT COUNT(*) FROM Assoziation a1 INNER JOIN Assoziation a2 ON a1.cid = a2.cid WHERE a1.uid = " + client.getUid();

                    if (statementAssoziationCount.execute(sqlAssoziationCount)) {

                        ResultSet resultSetCount = statementAssoziationCount.getResultSet();

                        if (resultSetCount.first() && client.getAcount() != resultSetCount.getInt(1)) {

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

                                client.getSession().getAsyncRemote().sendText(builder.append("_ next _").toString());

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