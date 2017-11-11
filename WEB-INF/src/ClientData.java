import javax.websocket.Session;
import java.sql.Connection;

class ClientData {
    private final Session session;
    private final List<String> sent;
    private int uid;
    private String mdate;
    private Connection connection;
    private int ccount = 0;
    private int ucount = 0;
    private int acount = 0;

    ClientData(Session session) {
        this.session = session;
        sent = new List<>();
    }

    Session getSession() {
        return session;
    }

    int getUid() {
        return uid;
    }

    void setUid(int uid) {
        this.uid = uid;
    }

    String getMdate() {
        return mdate;
    }

    void setMdate(String mdate) {
        this.mdate = mdate;
    }

    int getCcount() {
        return ccount;
    }

    void setCcount(int ccount) {
        this.ccount = ccount;
    }

    int getUcount() {
        return ucount;
    }

    void setUcount(int ucount) {
        this.ucount = ucount;
    }

    int getAcount() {
        return acount;
    }

    void setAcount(int acount) {
        this.acount = acount;
    }

    List<String> getSent() {
        return sent;
    }

    Connection getConnection() throws Exception {
        if (connection.isClosed())
            throw new Exception("conntection closed!");
        return connection;
    }

    void setConnection(Connection connection) {
        this.connection = connection;
    }
}