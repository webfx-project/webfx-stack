package dev.webfx.stack.i18n.time;

import dev.webfx.extras.time.format.spi.TimeFormatProvider;
import dev.webfx.stack.i18n.I18n;
import javafx.beans.value.ObservableStringValue;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.YearMonth;

/**
 * @author Bruno Salmon
 */
public class I18nTimeFormatProvider implements TimeFormatProvider {

    @Override
    public String getMonthName(Month month) {
        return I18n.getI18nText(month.name().toLowerCase());
    }

    @Override
    public ObservableStringValue monthNameProperty(Month month) {
        return I18n.i18nTextProperty(month.name().toLowerCase());
    }

    @Override
    public String getDayOfWeekName(DayOfWeek dayOfWeek) {
        return I18n.getI18nText(dayOfWeek.name().toLowerCase());
    }

    @Override
    public ObservableStringValue dayOfWeekNameProperty(DayOfWeek dayOfWeek) {
        return I18n.i18nTextProperty(dayOfWeek.name().toLowerCase());
    }

    @Override
    public String getYearMonthName(YearMonth yearMonth) {
        // Commented as [{0}] doesn't work for now because [] is interpreted first (-> produces {0}} and then {0}
        //return I18n.getI18nText("[{0}] {1}", yearMonth.getMonth().name().toUpperCase(), yearMonth.getYear());
        return I18n.getI18nText("[" + yearMonth.getMonth().name().toLowerCase() + "] {0}", yearMonth.getYear());
    }

    @Override
    public ObservableStringValue yearMonthNameProperty(YearMonth yearMonth) {
        // Commented as [{0}] doesn't work for now because [] is interpreted first (-> produces {0}} and then {0}
        //return I18n.i18nTextProperty("[{0}] {1}", yearMonth.getMonth().name().toUpperCase(), yearMonth.getYear());
        return I18n.i18nTextProperty("[" + yearMonth.getMonth().name().toLowerCase() + "] {0}", yearMonth.getYear());
    }

}
