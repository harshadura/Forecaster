package org.durapix.forecast.listner;

import dme.forecastiolib.ForecastIO;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Servlet extends HttpServlet{

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException{

        ForecastIO fio = new ForecastIO("f04e0331d57f500de38564058834bb8b");
        fio.setUnits(ForecastIO.UNITS_SI);
        fio.getForecast("38.7252993", "-9.1500364");
        System.out.println("Latitude: "+fio.getLatitude());
        System.out.println("Longitude: "+fio.getLongitude());
        System.out.println("Timezone: "+fio.getTimezone());
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<body>");
        out.println("<h1>Hello Servlet Get"  + fio.getTimezone() + "</h1>");
        out.println("</body>");
        out.println("</html>");
    }
}