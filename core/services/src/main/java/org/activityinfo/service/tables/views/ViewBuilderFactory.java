package org.activityinfo.service.tables.views;

import org.activityinfo.model.expr.ExprParser;
import org.activityinfo.model.form.FormClassVisitor;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.FieldValue;
import org.activityinfo.model.type.NarrativeType;
import org.activityinfo.model.type.ReferenceType;
import org.activityinfo.model.type.barcode.BarcodeType;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.expr.CalculatedFieldType;
import org.activityinfo.model.type.expr.ExprFieldType;
import org.activityinfo.model.type.geo.GeoPointType;
import org.activityinfo.model.type.image.ImageType;
import org.activityinfo.model.type.number.Quantity;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.model.type.primitive.BooleanType;
import org.activityinfo.model.type.primitive.TextType;
import org.activityinfo.model.type.time.*;

public class ViewBuilderFactory implements FormClassVisitor<ColumnViewBuilder> {

    private static final ViewBuilderFactory INSTANCE = new ViewBuilderFactory();

    private ViewBuilderFactory() {}


    public static ColumnViewBuilder get(FormField field, FieldType type) {
        return type.accept(field, INSTANCE);
    }

    @Override
    public ColumnViewBuilder visitTextField(FormField field, TextType type) {
        return new StringColumnBuilder(field.getId());
    }

    @Override
    public ColumnViewBuilder visitQuantityField(FormField field, QuantityType type) {
        return new DoubleColumnBuilder(field.getId(), new DoubleReader() {
            @Override
            public double read(FieldValue fieldValue) {
                if(fieldValue instanceof Quantity) {
                    Quantity quantity = (Quantity) fieldValue;
                    return quantity.getValue();
                } else {
                    return Double.NaN;
                }
            }
        });
    }

    @Override
    public ColumnViewBuilder visitReferenceField(FormField field, ReferenceType type) {
        return null;
    }

    @Override
    public ColumnViewBuilder visitEnumField(FormField field, EnumType type) {
        return new EnumColumnBuilder(field.getId(), type);
    }

    @Override
    public ColumnViewBuilder visitBarcodeField(FormField field, BarcodeType type) {
        return new StringColumnBuilder(field.getId());
    }

    @Override
    public ColumnViewBuilder visitBooleanField(FormField field, BooleanType type) {
        return new BooleanColumnBuilder(field.getId());
    }

    @Override
    public ColumnViewBuilder visitCalculatedField(FormField field, CalculatedFieldType type) {
        return new CalcColumnBuilder(ExprParser.parse(type.getExpression()));
    }

    @Override
    public ColumnViewBuilder visitGeoPointField(FormField field, GeoPointType type) {
        return null;
    }

    @Override
    public ColumnViewBuilder visitImageField(FormField field, ImageType type) {
        return null;
    }

    @Override
    public ColumnViewBuilder visitLocalDateIntervalField(FormField field, LocalDateIntervalType type) {
        return null;
    }

    @Override
    public ColumnViewBuilder visitExprField(FormField field, ExprFieldType type) {
        return null;
    }

    @Override
    public ColumnViewBuilder visitLocalDateField(FormField field, LocalDateType localDateType) {
        return null;
    }

    @Override
    public ColumnViewBuilder visitNarrativeField(FormField field, NarrativeType narrativeType) {
        return null;
    }

    @Override
    public ColumnViewBuilder visitMonthField(FormField field, MonthType monthType) {
        return null;
    }

    @Override
    public ColumnViewBuilder visitYearField(FormField field, YearType yearType) {
        return new DoubleColumnBuilder(field.getId(), new DoubleReader() {
            @Override
            public double read(FieldValue value) {
                if(value instanceof YearValue) {
                    return ((YearValue)value).getYear();
                } else {
                    return Double.NaN;
                }
            }
        });
    }
}
