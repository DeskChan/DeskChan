package info.deskchan.weather;

import java.util.Calendar;
import java.util.Locale;

class Temperature{
    private String temp;
    private int itemp;
    public Temperature(int value){
        value=(int)(0.555555*(value-32));
        if(value>0) temp="+"+value;
        else
        if(value<0) temp="-"+value;
        else
            temp=Integer.toString(value);
        itemp=value;
    }
    public int getInt() { return itemp; }
    public String get() { return temp;  }
}

class DayForecast {
    public final Calendar day;
    public final Temperature tempHigh;
    public final Temperature tempLow;
    public final String weather;
    public DayForecast(Calendar day, int low, int high, String weather){
        this.day = day;
        tempHigh = new Temperature(high);
        tempLow = new Temperature(low);
        this.weather = weather;
    }
    public String toString(){
        return day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())+
                ", "+day.get(Calendar.DAY_OF_MONTH)+": "+tempHigh.get()+"/"+tempLow.get()+", "+Main.getString(weather);
    }
}
class TimeForecast {
    public final Temperature temp;
    public final String weather;

    public TimeForecast(int temp, String weather){
        this.temp = new Temperature(temp);
        this.weather = weather;
    }
    public String toString(){
        return temp.get()+", "+Main.getString(weather);
    }
}
