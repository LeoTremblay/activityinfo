package org.activityinfo.model.table.columns;

import org.activityinfo.model.table.ColumnType;
import org.activityinfo.model.table.ColumnView;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Simple Array of String values
 */
public class StringArrayColumnView implements ColumnView {

    private String[] values;

    public StringArrayColumnView(String[] values) {
        this.values = values;
    }

    public StringArrayColumnView(List<String> values) {
        this.values = values.toArray(new String[values.size()]);
    }

    @Override
    public ColumnType getType() {
        return ColumnType.STRING;
    }

    @Override
    public int numRows() {
        return values.length;
    }

    @Override
    public Object get(int row) {
        return values[row];
    }

    @Override
    public double getDouble(int row) {
        return Double.NaN;
    }

    @Override
    public String getString(int row) {
        return values[row];
    }

    @Override
    public Date getDate(int row) {
        return null;
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }
}
