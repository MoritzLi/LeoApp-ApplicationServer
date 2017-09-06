import java.io.*;
import java.util.Map;
import javax.servlet.*;
import javax.servlet.http.*;

public class Test0 extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Map<String, String[]> params = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            entry.getKey();
        }

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.append("<html>")
                .append("<body>")
                .append("<b>Hello, world!</b>")
                .append("<p>Request URI: ")
                .append(request.getRequestURI())
                .append("</p>")
                .append("<p>Protocol: ")
                .append(request.getProtocol())
                .append("</p>")
                .append("<p>PathInfo: ")
                .append(request.getPathInfo())
                .append("</p>")
                .append("<p>Remote Address: ")
                .append(request.getRemoteAddr())
                .append("</p>")
//                .append("</body>")
//                .append("</html>");
                .flush();

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        out.append("<p>TEST</p>").flush();
        out.close();
    }


}