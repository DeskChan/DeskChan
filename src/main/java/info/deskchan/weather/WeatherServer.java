package info.deskchan.weather;

public interface WeatherServer {

    void update();

    TimeForecast getNow();

    DayForecast getByDay(int day);

    String checkLocation();

    String getLastUpdate();

    void drop();

    int getDaysLimit();

}
