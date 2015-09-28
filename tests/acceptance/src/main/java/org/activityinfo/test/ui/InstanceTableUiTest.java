package org.activityinfo.test.ui;
/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.common.base.Predicate;
import org.activityinfo.test.driver.ApiApplicationDriver;
import org.activityinfo.test.driver.FieldValue;
import org.activityinfo.test.driver.UiApplicationDriver;
import org.activityinfo.test.pageobject.bootstrap.BsTable;
import org.activityinfo.test.pageobject.web.entry.TablePage;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import javax.inject.Inject;
import java.util.Arrays;

import static org.activityinfo.test.driver.Property.name;
import static org.activityinfo.test.driver.Property.property;
import static org.junit.Assert.assertTrue;

/**
 * @author yuriyz on 06/18/2015.
 */
public class InstanceTableUiTest {

    private static final String DATABASE = "InstanceTableUiTest";
    private static final String FORM_NAME = "Form";

    public static final int SUBMISSIONS_COUNT = 500 + 1;
    public static final int LOAD_COUNT = 200;

    public InstanceTableUiTest() {
    }

    @Inject
    public UiApplicationDriver driver;

    private void background() throws Exception {
        driver.login();
        ApiApplicationDriver api = (ApiApplicationDriver) driver.setup();
        api.createDatabase(property("name", DATABASE));

        api.addPartner("NRC", DATABASE);

        api.createForm(name(FORM_NAME), property("database", DATABASE));
        api.createField(
                property("form", FORM_NAME),
                property("name", "quantity"),
                property("type", "quantity"),
                property("code", "1")
        );
        api.createField(
                property("form", FORM_NAME),
                property("name", "enum"),
                property("type", "enum"),
                property("items", "item1, item2, item3"),
                property("code", "2")
        );
        api.createField(
                property("form", FORM_NAME),
                property("name", "text"),
                property("type", "text"),
                property("code", "3")
        );
        api.createField(
                property("form", FORM_NAME),
                property("name", "multi-line"),
                property("type", "NARRATIVE"),
                property("code", "4")
        );
        api.createField(
                property("form", FORM_NAME),
                property("name", "barcode"),
                property("type", "BARCODE"),
                property("code", "5")
        );

        // submit with null values
        api.submitForm(FORM_NAME, Arrays.asList(
                new FieldValue("Partner", "NRC"),
                new FieldValue("quantity", -1)
        ));

        for (int j = 0; j < 5; j++) { // chunk on 5 batch commands to avoid SQL timeout
            api.startBatch();
            for (int i = 0; i < 100; i++) {
                api.submitForm(FORM_NAME, Arrays.asList(
                        new FieldValue("Partner", "NRC"),
                        new FieldValue("quantity", i),
                        new FieldValue("enum", i % 2 == 0 ? "item1" : "item1, item2"),
                        new FieldValue("text", "text" + i),
                        new FieldValue("multi-line", "line1\nline2"),
                        new FieldValue("barcode", "barcode")
                ));
            }
            api.submitBatch();
        }
    }

    @Test
    public void infiniteScroll() throws Exception {

        background();

        final TablePage tablePage = driver.getApplicationPage().navigateToTable(
                driver.getAliasTable().getAlias(DATABASE), 
                driver.getAliasTable().getAlias(FORM_NAME));
        
        BsTable table = tablePage.table();

        // start scrolling down and check that rows are loaded during scrolling
        for (int i = 1; i < 11; i++) {
            final int index = i;

            table.scrollToTheBottom();

            table.getContainer().waitUntil(new Predicate<WebDriver>() {
                @Override
                public boolean apply(WebDriver input) {

                    final BsTable table = tablePage.table(); // not clear why but we may get StaleReferenceException here sometimes, refresh reference

                    // we don't really use scroll but just back end forth to emulate it. Need something better here
                    table.scrollToTheTop();
                    table.scrollToTheBottom();
                    int rowCount = table.rowCount();
                    System.out.println("infiniteScroll, rowCount: " + rowCount);
                    return rowCount > index * LOAD_COUNT;
                }
            });

            table = tablePage.table(); // not clear why but we may get StaleReferenceException here sometimes, refresh reference
            int rowCount = table.rowCount();
            assertRowCount(rowCount, i);
            if (rowCount == SUBMISSIONS_COUNT) {
                return;
            }
        }

        throw new AssertionError("Failed to load all rows on infinite scroll. Expected: " + SUBMISSIONS_COUNT +
                ", but got: " + table.rowCount());
    }


    private static void assertRowCount(int rowCount, int iteration) {
        assertTrue("rowCount: " + rowCount + ", expected to be more then: " + LOAD_COUNT * iteration +
                        ", end less/equals then: " + (LOAD_COUNT * (iteration + 1)),
                rowCount >= LOAD_COUNT * iteration /*&& rowCount <= (LOAD_COUNT * (iteration + 1))*/);
    }

}