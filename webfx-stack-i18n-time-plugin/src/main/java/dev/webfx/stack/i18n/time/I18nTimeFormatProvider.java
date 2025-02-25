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

    private static String getMonthI18nKey(Month month) {
        return month.name().toLowerCase();
    }

    private static String getDayOfWeekI18nKey(DayOfWeek dayOfWeek) {
        return dayOfWeek.name().toLowerCase();
    }

    @Override
    public String getMonthName(Month month) {
        return I18n.getI18nText(getMonthI18nKey(month));
    }

    @Override
    public ObservableStringValue monthNameProperty(Month month) {
        return I18n.i18nTextProperty(getMonthI18nKey(month));
    }

    @Override
    public String getDayOfWeekName(DayOfWeek dayOfWeek) {
        return I18n.getI18nText(getDayOfWeekI18nKey(dayOfWeek));
    }

    @Override
    public ObservableStringValue dayOfWeekNameProperty(DayOfWeek dayOfWeek) {
        return I18n.i18nTextProperty(getDayOfWeekI18nKey(dayOfWeek));
    }

    @Override
    public String getYearMonthName(YearMonth yearMonth) {
        return I18n.getI18nText(TimeI18nKeys.yearMonth2, yearMonth.getYear(), getMonthI18nKey(yearMonth.getMonth()));
    }

    @Override
    public ObservableStringValue yearMonthNameProperty(YearMonth yearMonth) {
        return I18n.i18nTextProperty(TimeI18nKeys.yearMonth2, yearMonth.getYear(), getMonthI18nKey(yearMonth.getMonth()));
    }

    @Override
    public String formatDayAndMonth(int day, Month month) {
        return I18n.getI18nText(TimeI18nKeys.dayAndMonth2, day, getMonthI18nKey(month));
    }

    @Override
    public ObservableStringValue dayAndMonthProperty(int day, Month month) {
        return I18n.i18nTextProperty(TimeI18nKeys.dayAndMonth2, day, getMonthI18nKey(month));
    }
}
