package org.activityinfo.store.query.output;

import com.google.gson.stream.JsonWriter;
import org.activityinfo.model.query.ColumnSet;
import org.activityinfo.model.query.ColumnType;
import org.activityinfo.model.query.ColumnView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Writes a {@code ColumnSet} to an array of JSON Objects
 */
public class ColumnSetJsonWriter {

    private final JsonWriter writer;

    public ColumnSetJsonWriter(JsonWriter writer) {
        this.writer = writer;
    }

    public void write(ColumnSet columnSet) throws IOException {

        int numRows = columnSet.getNumRows();
        int numCols = columnSet.getColumns().size();
        FieldWriter[] writers = createWriters(columnSet);

        writer.beginArray();
        for(int rowIndex=0;rowIndex!=numRows;++rowIndex) {

            writer.beginObject();
            for(int colIndex=0;colIndex!=numCols;++colIndex) {
                writers[rowIndex].write(rowIndex);
            }
            writer.endObject();
        }
        writer.endArray();
    }

    private interface FieldWriter {
        void write(int rowIndex) throws IOException;
    }


    private FieldWriter[] createWriters(ColumnSet columnSet) {
        FieldWriter[] writers = new FieldWriter[columnSet.getColumns().size()];
        int index = 0;
        for (Map.Entry<String, ColumnView> column : columnSet.getColumns().entrySet()) {
            writers[index] = createWriter(column.getKey(), column.getValue());
        }
        return writers;
    }

    public FieldWriter createWriter(final String id, final ColumnView view) {

        switch(view.getType()) {
            case STRING:
                return new FieldWriter() {
                    @Override
                    public void write(int rowIndex) throws IOException {
                        String value = view.getString(rowIndex);
                        if(value != null) {
                            writer.name(id);
                            writer.value(view.getString(rowIndex));
                        }
                    }
                };

            case NUMBER:
                return new FieldWriter() {
                    @Override
                    public void write(int rowIndex) throws IOException {
                        double value = view.getDouble(rowIndex);
                        if(!Double.isNaN(value)) {
                            writer.name(id);
                            writer.value(value);
                        }
                    }
                };
            case BOOLEAN:
                return new FieldWriter() {
                    @Override
                    public void write(int rowIndex) throws IOException {
                        int value = view.getBoolean(rowIndex);
                        if(value != ColumnView.NA) {
                            writer.name(id);
                            writer.value(value != 0);
                        }
                    }
                };

            case DATE:
                final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

                return new FieldWriter() {
                    @Override
                    public void write(int rowIndex) throws IOException {
                        Date date = view.getDate(rowIndex);
                        if(date != null) {
                            writer.name(id);
                            writer.value(dateFormat.format(date));
                        }
                    }
                };
        }
        throw new IllegalArgumentException("type: " + view.getType());
    }
}