package info.deskchan.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class YahooServer implements WeatherServer{

    TimeForecast now;
    DayForecast[] forecasts=new DayForecast[10];

    public DayForecast getByDay(int day) {
        update();
        return forecasts[day];
    }

    public TimeForecast getNow() {
        update();
        return now;
    }

    @Override
    public String checkLocation() {
        JSONObject json = getQuery();
        if(json==null) return "1";
        try{
            json=json.getJSONObject("query");
            if(!json.has("results") || !(json.get("results") instanceof JSONObject)) return "2";
            json=json.getJSONObject("results");
            json=json.getJSONObject("channel");
            json=json.getJSONObject("location");
            return json.getString("city")+", "+json.getString("country");
        } catch (Exception e){
            Main.log("Error while parsing data from weather server");
            Main.log(e);
            return "3";
        }
    }

    private Date lastUpdate;
    public String getLastUpdate(){
        SimpleDateFormat format=new SimpleDateFormat("HH:mm:ss");
        return format.format(lastUpdate);
    }
    private void update(){
        if(lastUpdate!=null && (new Date().getTime()-lastUpdate.getTime())/60000<30) return;

        JSONObject json = getQuery();
        if(json==null) return;
        try{
            json=json.getJSONObject("query");
            if(!json.has("results") || !(json.get("results") instanceof JSONObject)) return;
            json=json.getJSONObject("results");
            json=json.getJSONObject("channel");
            json=json.getJSONObject("item");
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            JSONArray array=json.getJSONArray("forecast");
            for(int i=0;i<10;i++){
                forecasts[i]=new DayForecast((Calendar) cal.clone(),array.getJSONObject(i).getInt("high"),array.getJSONObject(i).getInt("low"),getWeatherString(array.getJSONObject(i).getInt("code")));
                cal.add(Calendar.DATE,1);
            }
            json=json.getJSONObject("condition");
            now=new TimeForecast(json.getInt("temp"),getWeatherString(json.getInt("code")));
            lastUpdate=new Date();
        } catch (Exception e){
            Main.log("Error while parsing data from weather server");
            Main.log(e);
            return;
        }
    }
    private JSONObject getQuery(){
        String query;
        try {
            query=URLEncoder.encode(Main.getCity(),"UTF-8");
        } catch(Exception e){
            return null;
        }
        String url = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22";
        url += query + "%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";

        StringBuilder out = new StringBuilder();
        try {
            URL DATA_URL = new URL(url);
            java.io.InputStream stream = DATA_URL.openStream();

            char[] buffer = new char[1024];

            Reader in = new InputStreamReader(stream, "UTF-8");
            for (; ; ) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0)
                    break;
                out.append(buffer, 0, rsz);
            }
            stream.close();
        } catch(Exception e){
            Main.log("Error while reading data from weather server");
            Main.log(e);
            return null;
        }

        return new JSONObject(out.toString());
    }
    private String getWeatherString(int code){
        String text="unknown";
        switch(code){
            case 0: case 1:
            case 2:  text="storm"; break;
            case 3: case 4: case 37: case 38: case 39: case 45:
            case 47: text="thunderstorm"; break;
            case 5: case 6: case 7: case 16: case 18:
            case 46: text="snow"; break;
            case 8: case 9: case 10:
            case 40: text="rain"; break;
            case 11:
            case 12: text="heavy-rain"; break;
            case 13: case 14: case 15: case 41: case 42:
            case 43: text="heavy-snow"; break;
            case 17:
            case 35: text="hail"; break;
            case 19: case 20: case 21:
            case 22: text="fog"; break;
            case 23: case 24:
            case 25: text="wind"; break;
            case 26: case 27: case 28: case 29: case 30:
            case 44: text="cloudy"; break;
            case 31: case 32: case 33: case 34:
            case 36: text="clear"; break;
        }
        return text;
    }
}
