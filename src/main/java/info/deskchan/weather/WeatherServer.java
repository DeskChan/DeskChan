package info.deskchan.weather;

public interface WeatherServer {
    TimeForecast getNow();
    DayForecast getByDay(int day);
    String checkLocation();
    String getLastUpdate();
}
