package org.durapix.forecast.quartz;

import dme.forecastiolib.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.durapix.forecast.db.DBConnect;
import org.durapix.forecast.db.PropertyLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

public class ForecastJob implements Job
{
    private PropertyLoader propertyLoader = new PropertyLoader();
    private DBConnect dbConnect;
    private Connection con;
    private Statement stm;

    public final static Logger LOGGER = Logger.getLogger(ForecastJob.class.getName());

    public void execute(JobExecutionContext context)
            throws JobExecutionException {

        try{

            dbConnect = new DBConnect();
            con = dbConnect.connect();
            stm = con.createStatement();

            LOGGER.info("\n\nQuartz scheduler starting..");

            try {
                String x = "";
                String y = "";
                String xy = "";

                try {
                    ResultSet results = stm.executeQuery("SELECT * FROM tb_feed_weather_city");
                    while (results.next()) {

                        String city = results.getString("name");

                        /**
                         * this method isnt used before sometime gets : OVER_QUERY_LIMIT problem
                         */
//                            double arrXXYY[] = getLatLong(getLocationInfo(city));
//                            x = String.valueOf(arrXXYY[0]);
//                            y = String.valueOf(arrXXYY[1]);

                        ///////////////////////////////////////////////

                        xy = results.getString("feed_url");
                        if (xy != null){
                            String arrXY[] = xy.split(",");
                            x = arrXY[0];
                            y = arrXY[1];

                            LOGGER.info(">>>>>>>>>>>> Data for : " + city + " coordinates: " + x + "," + y);

                            String weatherHtmlTag = generateHTMLforcastByCityXY(x, y);
                            LOGGER.info("^^^^^^^ weatherHtmlTag : " + weatherHtmlTag);

                            String sql1 = "update `tb_feed_weather_city` set `status` = ? where `name` = ?";
                            PreparedStatement preparedStatement = con.prepareStatement(sql1);
                            preparedStatement.setString(2, city);
                            preparedStatement.setString(1, weatherHtmlTag);
                            preparedStatement.executeUpdate();
                            LOGGER.info("<<<<<<<<<<< Data saved : " + city + " coordinates: " + x + "," + y);

                        }
                    }
                } catch (Exception ee) {
                    ee.getMessage();
                }
            } catch (Exception ee) {
                LOGGER.info("Error : " + ee.getMessage());
                ee.printStackTrace();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public String generateHTMLforcastByCityXY(String x, String y){

        // instantiate forecast api
        ForecastIO fio = new ForecastIO("f04e0331d57f500de38564058834bb8b"); //instantiate the class with the API key.
        fio.setUnits(ForecastIO.UNITS_SI);             //sets the units as SI - optional
        fio.setExcludeURL("hourly,minutely");             //excluded the minutely and hourly reports from the reply
        fio.getForecast(x, y); //("38.7252993", "-9.1500364");


        String currentDaySummary = "", currentDayWind = "", currentDayTime = "", currentDayTemp = "", currentDayIcon = "";

        // current data
        FIOCurrently currently = new FIOCurrently(fio);
        //Print currently data
        LOGGER.info("\nCurrently\n");
        String [] f  = currently.get().getFieldsArray();
        for(int i = 0; i<f.length;i++)
        {

            LOGGER.info("***** " + f[i] + ": " + currently.get().getByKey(f[i]));
            if(f[i].equals("summary")){
                currentDaySummary = currently.get().getByKey(f[i]);
                currentDaySummary = currentDaySummary.replace("\"", "");
            }
            if(f[i].equals("time")){
                currentDayTime = currently.get().getByKey(f[i]);
            }
            if(f[i].equals("temperature")){
                currentDayTemp = currently.get().getByKey(f[i]);
            }
            if(f[i].equals("icon")){
                currentDayIcon = currently.get().getByKey(f[i]);
            }
            if(f[i].equals("windSpeed")){
                currentDayWind = currently.get().getByKey(f[i]);
            }
        }

        /// coming day forecast

        String comingDaySummary = "", comingDayTempMax = "", comingDayTempMin = "", comingDayIcon = "";

        FIODaily daily = new FIODaily(fio);
        //In case there is no daily data available
        if(daily.days()<0)
            System.out.println("No daily data.");
        else {
            LOGGER.info("\nDaily:\n");

        // next-day forecast
        int i = 1;
        String [] h = daily.getDay(i).getFieldsArray();
        LOGGER.info("##### " + "next-day forecast -- day#"+(i+1));
        for(int j=0; j<h.length; j++)  {

            LOGGER.info("***** " + h[j]+": "+daily.getDay(i).getByKey(h[j]));

            if(h[j].equals("summary")){
                comingDaySummary = daily.getDay(i).getByKey(h[j]);
                comingDaySummary = comingDaySummary.replace("\"", "");
            }
            if(h[j].equals("temperatureMax")){
                comingDayTempMax = daily.getDay(i).getByKey(h[j]);
            }
            if(h[j].equals("temperatureMin")){
                comingDayTempMin = daily.getDay(i).getByKey(h[j]);
            }
            if(h[j].equals("icon")){
                comingDayIcon = daily.getDay(i).getByKey(h[j]);
            }
        }
        }

        String htmlTags  = "<table class='weather_dis' width='300' border='0' cellpadding='0' cellspacing='0'><tr>" +
                "<td colspan='3'><b>Today</b> - " + currentDaySummary + "</td></tr><tr><td width='60' rowspan='2' valign='top'>" +
                "<img src='images/weather/weather_gif/"+ getWeatherStateIcon(currentDayIcon) + "' width='60' height='40'></td><td width='6'>&nbsp;</td>" +
                "<td width='170'> Temp: " + currentDayTemp + "</td></tr><tr>" +
                "<td>&nbsp;</td><td> Wind speed: " + currentDayWind + "</td></tr><tr>" +
                "<td colspan='3'><hr/></td></tr><tr>" +
                "<td colspan='3'><b>Tomorrow</b> - " + comingDaySummary + "</td></tr><tr><td width='60' rowspan='2' valign='top'>" +
                "<img src='images/weather/weather_gif/"+ getWeatherStateIcon(comingDayIcon) + "' width='60' height='40'></td><td width='6'>&nbsp;</td>" +
                "<td width='170'> Max Temp: " + comingDayTempMax + "</td></tr><tr>" +
                "<td>&nbsp;</td><td> Min Temp: " + comingDayTempMin + "</td></tr><tr>" +
                "<td colspan='3'><hr/></td></tr><tr>" +
                "<td colspan='3'>Current Time: <span id='timer'>" + currentDayTime + "</span></td></tr></table>";

//                "<tr><td colspan='3'>Tommorrow: " + comingDaySummary + "</td></tr><tr>" +
//                "<td colspan='3'> Max Temp: " + comingDayTempMax + "</td></tr><tr>" +
//                "<td colspan='3'> Min Temp: " + comingDayTempMin + "</td></tr><tr>" +
//                "<td colspan='3'><br>Current Time: <span id='timer'>" + currentDayTime + "</span></td></tr></table>";

        return htmlTags;
    }


    /////////////

    private JSONObject getLocationInfo(String address)
    {

        StringBuilder stringBuilder = new StringBuilder();
        try {

            address = address.replaceAll(" ","%20");

            HttpPost httppost = new HttpPost("http://maps.google.com/maps/api/geocode/json?address=" + address + "&sensor=false");
            HttpClient client = new DefaultHttpClient();
            HttpResponse response;
            stringBuilder = new StringBuilder();

            response = client.execute(httppost);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            int b;
            while ((b = stream.read()) != -1) {
                stringBuilder.append((char) b);
            }
        } catch (ClientProtocolException e) {
            LOGGER.info("getLocationInfo ClientProtocolException" + e.toString());
        } catch (IOException e) {

            LOGGER.info("getLocationInfo IOException" + e.toString());
        }


        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(stringBuilder.toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            LOGGER.info("getLocationInfo JSONException" + e.toString());
        }

        return jsonObject;
    }

    private double[] getLatLong(JSONObject jsonObject)
    {
        double arrXY[] = new double[2];
        try {

            arrXY[0] = ((JSONArray)jsonObject.get("results")).getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getDouble("lng");
            arrXY[1] = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                    .getDouble("lat");
        } catch (JSONException e) {
            arrXY[0] = 0;
            arrXY[1] = 0;
            LOGGER.info("getLatLong" + e.toString());
        }

        return arrXY;
    }

    private String getWeatherStateIcon(String state)
    {
        String icon = "";
        try {
            //wind fog cloudy rain clear partly-cloudy-night

            if(state.contains("clear")){
                icon = "1";
            }
            else if(state.contains("rain")){
                icon = "14";
            }
            else if(state.contains("wind")){
                icon = "14_";
            }
            else if(state.contains("cloudy")){
                icon = "3";
            }
            else if(state.contains("fog")){
                icon = "6";
            }
            else {
                icon = "8";
            }

        } catch (Exception e) {
            LOGGER.info("getLatLong" + e.toString());
        }

        return icon + ".png";
    }



}
