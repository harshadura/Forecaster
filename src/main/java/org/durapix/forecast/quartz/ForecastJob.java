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
    private String latestUpdate;
    private double longitute, latitude;

    public final static Logger LOGGER = Logger.getLogger(ForecastJob.class.getName());

    public void execute(JobExecutionContext context)
            throws JobExecutionException {

        try{

            dbConnect = new DBConnect();
            con = dbConnect.connect();
            stm = con.createStatement();

            LOGGER.info("\n\nQuartz scheduler starting..");

            try {
                String xy = "";
                String x = "";
                String y = "";

                try {
                    ResultSet results = stm.executeQuery("SELECT * FROM tb_feed_weather_city");
                    while (results.next()) {
                        String city = results.getString("name");

//                        if (city.equals("Singapore")) {
//                            xy = results.getString("feed_url");

//                            if (xy != null){

//                                String arrXY[] = xy.split(",");
//                                x = arrXY[0];
//                                y = arrXY[1];

                                getLatLong(getLocationInfo(city));
                                x = String.valueOf(latitude);
                                y = String.valueOf(longitute);

                                String weatherHtmlTag = generateHTMLforcastByCityXY(x, y);
                                LOGGER.info("^^^^^^^ weatherHtmlTag : " + weatherHtmlTag);

                                String sql1 = "update `tb_feed_weather_city` set `status` = ? where `feed_url` = ?";
                                PreparedStatement preparedStatement = con.prepareStatement(sql1);
                                preparedStatement.setString(2, xy);
                                preparedStatement.setString(1, weatherHtmlTag);
                                preparedStatement.executeUpdate();
//                            }
//                        }
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
        else
            LOGGER.info("\nDaily:\n");
        //Print daily data
        for(int i = 0; i<1; i++){
            String [] h = daily.getDay(i).getFieldsArray();
            LOGGER.info("##### " + "Day #"+(i+1));
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

        String htmlTags  = "<table width='236' border='0' cellpadding='0' cellspacing='0'><tr>" +
                "<td colspan='3'>Today: " + currentDaySummary + "</td></tr><tr><td width='60' rowspan='2' valign='top'>" +
                "<img src='images/weather/animated/14.gif' width='60' height='40'></td><td width='6'>&nbsp;</td>" +
                "<td width='170'> Temp: " + currentDayTemp + "</td></tr><tr>" +
                "<td>&nbsp;</td><td> Wind speed: " + currentDayWind + "</td></tr><tr>" +
                "<td colspan='3'>&nbsp;</td></tr><tr><td colspan='3'>Tommorrow: " + comingDaySummary + "</td></tr><tr>" +
                "<td colspan='3'> Max Temp: " + comingDayTempMax + "</td></tr><tr>" +
                "<td colspan='3'> Min Temp: " + comingDayTempMin + "</td></tr><tr>" +
                "<td colspan='3'><br>Current Time: <span id='timer'>" + currentDayTime + "</span></td></tr></table>";

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

    private boolean getLatLong(JSONObject jsonObject)
    {

        try {

            longitute = ((JSONArray)jsonObject.get("results")).getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getDouble("lng");
            LOGGER.info("Log1" + longitute + "");
            latitude = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                    .getDouble("lat");
            LOGGER.info("lat1" + latitude + "");
        } catch (JSONException e) {

            longitute=0;
            latitude = 0;
            LOGGER.info("getLatLong" + e.toString());
            return false;

        }

        return true;
    }


}
