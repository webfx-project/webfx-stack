package dev.webfx.stack.db.query;

import dev.webfx.platform.util.Numbers;

import java.util.Arrays;

/**
 * @author Bruno Salmon
 */
public final class QueryResult {

    /**
     * The number of rows in this result set.
     */
    private final int rowCount;

    /**
     * The number of columns in this result set.
     */
    private final int columnCount;

    /**
     * The values of the result set stored in an inline array (more efficient for compression algorithm).
     * First column, then 2nd column, etc... So inlineIndex = rowIndex + columnIndex * rowCount.
     * (better than 1st row, 2nd row, etc... for compression algorithm)
     */
    private final Object[] values;

    /**
     * Column names of the result set. This information is actually optional and useful only for debugging or when
     * using methods to access values with column names instead of column index (like in the DomainModelLoader)
     */
    private final String[] columnNames;

    /**
     * The version number (optional). This information is mainly designed for the query push service when sending a
     * QueryResultDiff: the receiver should check that the version number expected by the diff matches the last result
     * before applying that diff.
     */
    private int versionNumber;

    public QueryResult(int rowCount, int columnCount, Object[] values, String[] columnNames) {
        if ((values == null ? 0 : values.length) != columnCount * rowCount || columnNames != null && columnNames.length != columnCount)
            throw new IllegalArgumentException("Incoherent sizes in QueryResult initialization");
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.columnNames = columnNames;
        this.values = values;
    }

    public QueryResult(int columnCount, Object[] values) {
        this(columnCount, values, null);
    }

    public QueryResult(int columnCount, Object[] values, String[] columnNames) {
        this(values == null || values.length == 0 ? 0 : values.length / columnCount, columnCount, values, columnNames);
    }

    public QueryResult(Object[] values, String[] columnNames) {
        this(values.length / columnNames.length, columnNames.length, values, columnNames);
    }

    public int getColumnCount() {
        return columnCount;
    }

    public int getRowCount() {
        return rowCount;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public Object[] getValues() {
        return values;
    }

    public <T> T getValue(int rowIndex, int columnIndex) {
        return (T) values[rowIndex + columnIndex * rowCount];
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    /*******************************************************************************************
     * Secondary convenient methods to access values with column names instead of column index *
     *******************************************************************************************/

    public int getColumnIndex(String columnName) {
        int n = getColumnCount();
        String[] columnNames = getColumnNames();
        for (int i = 0; i < n; i++) {
            if (columnName.equalsIgnoreCase(columnNames[i]))
                return i;
        }
        return -1;
    }

    public <T> T getValue(int rowIndex, String columnName) {
        return getValue(rowIndex, getColumnIndex(columnName));
    }

    public <T> T getValue(int rowIndex, String columnName, T defaultValue) {
        T value = getValue(rowIndex, getColumnIndex(columnName));
        return value != null ? value : defaultValue;
    }

    // To be used with int to avoid GWT ClassCastException as Integer coming from Json may actually be Double
    public int getInt(int rowIndex, String columnName, int defaultValue) {
        return getInt(rowIndex, getColumnIndex(columnName), defaultValue);
    }

    public int getInt(int rowIndex, int columnIndex, int defaultValue) {
        Object value = getValue(rowIndex, columnIndex);
        return Numbers.isNumber(value) ? Numbers.intValue(value) : defaultValue;
    }

    // To be used with Codenameone because generic one raise an error when value is a string
    public boolean getBoolean(int rowIndex, String columnName, boolean defaultValue) {
        return getBoolean(rowIndex, getColumnIndex(columnName), defaultValue);
    }

    public boolean getBoolean(int rowIndex, int columnIndex, boolean defaultValue) {
        Object value = getValue(rowIndex, columnIndex);
        return value == null ? defaultValue : value instanceof Boolean ? (Boolean) value : Boolean.valueOf(value.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryResult that = (QueryResult) o;

        if (rowCount != that.rowCount) return false;
        if (columnCount != that.columnCount) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(values, that.values)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(columnNames, that.columnNames);
    }

    @Override
    public int hashCode() {
        int result = rowCount;
        result = 31 * result + columnCount;
        result = 31 * result + Arrays.hashCode(values);
        result = 31 * result + Arrays.hashCode(columnNames);
        return result;
    }

}
