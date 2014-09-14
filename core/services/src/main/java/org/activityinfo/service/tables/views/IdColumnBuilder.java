package org.activityinfo.service.tables.views;

import com.google.common.collect.Lists;
import org.activityinfo.model.form.FormEvalContext;
import org.activityinfo.model.table.ColumnView;
import org.activityinfo.model.table.views.StringArrayColumnView;

import java.util.List;

public class IdColumnBuilder implements ColumnViewBuilder {

    private List<String> ids = Lists.newArrayList();


    @Override
    public void accept(FormEvalContext instance) {
        ids.add(instance.getId().asString());
    }

    @Override
    public void finalizeView() {

    }

    @Override
    public ColumnView get() {
        return new StringArrayColumnView(ids);
    }
}
