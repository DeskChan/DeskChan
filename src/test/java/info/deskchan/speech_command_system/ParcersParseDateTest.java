package info.deskchan.speech_command_system;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class ParcersParseDateTest {
    private String a;
    private Long b;

    @Parameterized.Parameters(name = "{index}: data({0})={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                //{"сейчас"},
                {"сегодня в 12 30"},
                {"сегодня"},
                {"завтра"},
                {"послезавтра в 5 часов"},
                {"5 часов вечера 30 минут 29 июля"},
                {"за 50 минут до сегодня"},
                {"за три часа до завтрашнего дня"},
                {"7 марта седьмое"},
                {"2012 марта седьмое"},
                {"через месяц"},
                {"в следующий понедельник"},
                {"в следующее вторник"},
                {"вторник"},
                {"прошлый вторник"},
                {"среда"},
                {"следующая среда"},
                {"пятое июня"},
                {"после четверга"},
                {"за 5 дней до четверга"},
                {"за 12 дней до пятого сентября"},
                {"за 2 дна до августа две тысячи шестого"},
                {"Пятое мая"},
                {"Седьмое июня следующего года"},
                {"три утра"},
                {"девять вечера"},
                {"Просто седьмое июня"},
                {"через трое суток"},
                {"через четверть часа"},
                {"полторы недели назад"},
                {"30 февраля"},
                {"29 февраля прошлого года"},
                {"Второе число следующего месяца"},
                {"Девятое прошлого года следующего месяца"},
                {"шестой понедельник ноября"},
                {"Этот день в прошлом году"},
                {"Это число в следующем году"},
                {"через пятницу"},
                {"пятницу назад"},
                {"3 субботы назад"},
                {"через воскресенье"},
                {"Третья суббота июня"},
                {"послезавтра"},
                {"позапозавчера"},
                {"17 декабря этого года"},
                {"за неделю до"},
                {"после послепосле послезавтра"},
                {"15 02 2017"},
                {"2 15 2017"},
                {"15/02-2017 23 34 05 23"},
                {"15/02-2017 23:34:05 23"},
                {"15/02-2017 23/34/05 23"},
                {"23:34:05.23 15.02.2017"},
                {"23:30 15/08"},
                {"15 августа 23:30"},
                {"через 5 минут"},
                {"послезавтра в 20:18"}
        });
    }

    public ParcersParseDateTest(String a) {
        this.a = a;
    }
    
    @Test
    public void test0() {
        ArrayList<String> words = PhraseComparison.toClearWords(a);
        Calendar cal = Calendar.getInstance();
        Number num = Parsers.parseDate(new Words(words));
        assertNotNull(num);
        this.b = num.longValue();
        System.out.println(num);
    }

}