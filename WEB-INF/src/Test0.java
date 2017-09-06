import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import javax.servlet.*;
import javax.servlet.http.*;

public class Test0 extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/plain");

        PrintWriter out = response.getWriter();

        out.append("Hello, world!")
                .append("\n")
                .append("Request URI: ")
                .append(request.getRequestURI())
                .append("\n")
                .append("Protocol: ")
                .append(request.getProtocol())
                .append("\n")
                .append("PathInfo: ")
                .append(request.getPathInfo())
                .append("\n")
                .append("Remote Address: ")
                .append(request.getRemoteAddr())
                .append("\n")
                .append("\n")
                .flush();

        Map<String, String[]> params = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            out.append(entry.getKey())
                    .append('=');
            for (String s : entry.getValue())
                out.append(s);
            out.append("\n");
        }
        out.flush();

        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/d02566f2", "Moritz", "tracy310");
            Statement statement = connection.createStatement();
            statement.execute("SELECT DISTINCT mid, uid, mtext, cid, UNIX_TIMESTAMP(mdate) as mdate FROM Messages WHERE cid = 1 ORDER BY cid, mdate");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        out.close();
    }


}