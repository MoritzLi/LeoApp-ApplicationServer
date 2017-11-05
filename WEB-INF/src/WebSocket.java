import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@ServerEndpoint("/")
public class WebSocket {
    private ClientData client;

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Open Connection ...");
        client = new ClientData(session);
    }

    @OnClose
    public void onClose(Session session) {
        try {
            client.getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Close Connection ...");
    }

    @OnMessage
    public String onMessage(String message, Session session) {
        System.out.println("Message from the client: " + message);
        if (message.startsWith("uid")) {
            client.setUid(Integer.parseInt(message.substring(message.indexOf('=') + 1)));
            return "uid set to: " + client.getUid();
        }
        if (message.startsWith("mdate")) {
            client.setMdate(message.substring(message.indexOf('=') + 1));
            return "mdate set to: " + client.getMdate();
        }
        if (message.startsWith("request") && client.getMdate() != null && client.getUid() != 0) {
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                client.setConnection(DriverManager.getConnection("jdbc:mysql://localhost:3306/leoapp", "leo", "!LeO!2013"));

                new ReceiveThread().start();

                return "+ok";
            } catch (IllegalAccessException | InstantiationException | SQLException | ClassNotFoundException e) {
                return '-' + e.getMessage();
            }
        }
        return "not part of the protocol";
    }

    @OnError
    public void onError(Throwable e) {
        e.printStackTrace();
    }

    private class ReceiveThread extends Thread {
        @Override
        public void run() {
            try {
                sleep(1500);

                while (client.getSession().isOpen()) {
                    Statement statementMessages = client.getConnection().createStatement();
                    if (statementMessages.execute("SELECT mid, mtext, m.cid, m.uid, UNIX_TIMESTAMP(mdate) as mdate FROM Messages m INNER JOIN Assoziation a ON a.cid = m.cid WHERE a.uid = " + client.getUid() + " AND UNIX_TIMESTAMP(mdate) > " + client.getMdate() + " ORDER BY mdate DESC")) {
                        ResultSet resultSet = statementMessages.getResultSet();
                        if (resultSet.first()) {
                            client.setMdate(resultSet.getString(5));
                            for (; !resultSet.isAfterLast(); resultSet.next()) {
                                String text = resultSet.getString(2).replace("_ ; _", "_  ;  _").replace("_ next _", "_  next  _");
                                String key = Encryption.createKey(text);
                                text = Encryption.encrypt(text, key);
                                key = Encryption.encryptKey(key);

                                System.out.println("write");
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
                                                "_ next _" +
                                                "\n");
                            }
                        }
                        resultSet.close();
                    }
                    statementMessages.close();

                    Statement statementChatsCount = client.getConnection().createStatement();
                    if (statementChatsCount.execute("SELECT COUNT(*) FROM Chats c INNER JOIN Assoziation a ON c.cid = a.cid AND a.uid = " + client.getUid())) {
                        ResultSet resultSetCount = statementChatsCount.getResultSet();
                        if (resultSetCount.first() && client.getCcount() != resultSetCount.getInt(1)) {
                            client.setCcount(resultSetCount.getInt(1));
                            Statement statementChats = client.getConnection().createStatement();
                            if (statementChats.execute("SELECT c.cid, c.cname, c.ctype FROM Chats c INNER JOIN Assoziation a ON c.cid = a.cid AND a.uid = " + client.getUid())) {
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
                                        if (!client.getSent().contains(s)) {
                                            System.out.println("write");
                                            client.getSession().getAsyncRemote().sendText(s);
                                            client.getSent().append(s);
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

                    Statement statementUserCount = client.getConnection().createStatement();
                    if (statementUserCount.execute("SELECT COUNT(*) FROM Users")) {
                        ResultSet resultSetCount = statementUserCount.getResultSet();
                        if (resultSetCount.first() && client.getUcount() != resultSetCount.getInt(1)) {
                            client.setUcount(resultSetCount.getInt(1));
                            Statement statementUser = client.getConnection().createStatement();
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
                                        if (!client.getSent().contains(s)) {
                                            System.out.println("write");
                                            client.getSession().getAsyncRemote().sendText(s);
                                            client.getSent().append(s);
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

                    Statement statementAssoziationCount = client.getConnection().createStatement();
                    if (statementAssoziationCount.execute("SELECT COUNT(*) FROM Assoziation a1 INNER JOIN Assoziation a2 ON a1.cid = a2.cid WHERE a1.uid = " + client.getUid())) {
                        ResultSet resultSet = statementAssoziationCount.getResultSet();
                        if (resultSet.first() && client.getAcount() != resultSet.getInt(1)) {
                            System.out.println("write");
                            client.getSession().getAsyncRemote().sendText("a_ next _\n");
                            client.setAcount(resultSet.getInt(1));
                        }
                        resultSet.close();
                    }
                    statementAssoziationCount.close();
                }
            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
                try {
                    client.getConnection().close();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}