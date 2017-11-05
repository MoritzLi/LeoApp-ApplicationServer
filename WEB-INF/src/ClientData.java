import javax.websocket.Session;
import java.sql.Connection;

public class ClientData {
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

    public Session getSession() {
        return session;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getMdate() {
        return mdate;
    }

    public void setMdate(String mdate) {
        this.mdate = mdate;
    }

    public int getCcount() {
        return ccount;
    }

    public void setCcount(int ccount) {
        this.ccount = ccount;
    }

    public int getUcount() {
        return ucount;
    }

    public void setUcount(int ucount) {
        this.ucount = ucount;
    }

    public int getAcount() {
        return acount;
    }

    public void setAcount(int acount) {
        this.acount = acount;
    }

    public List<String> getSent() {
        return sent;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
}