package org.activityinfo.migrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activityinfo.model.json.ObjectMapperFactory;
import org.activityinfo.model.resource.Resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

public class MySqlResourceWriter implements ResourceWriter {

    private long version = 1;

    private final ObjectMapper objectMapper = ObjectMapperFactory.get();

    private final Connection connection;
    private int count;
    private PreparedStatement statement;


    public MySqlResourceWriter(Connection connection) throws SQLException {
        this.connection = connection;

        connection.setAutoCommit(false);

        clearTables();

    }

    private void clearTables() throws SQLException {
        try( Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE resource");
            statement.execute("TRUNCATE TABLE user_root_index");

        }
        connection.commit();
    }

    @Override
    public void beginResources() throws Exception {
        count = 0;
        this.statement = connection.prepareStatement(
                "insert INTO resource (id, version, sub_tree_version, owner_id, class_id, label, content) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)");
    }


    @Override
    public void writeResource(int userId, Resource resource, Date dateCreated, Date dateDeleted) throws SQLException, JsonProcessingException {

        if(resource == null) {
            throw new NullPointerException("resource");
        }

        statement.setString(1, resource.getId().asString());
        statement.setLong(2, version);
        statement.setLong(3, version);
        statement.setString(4, resource.getOwnerId().asString());
        statement.setString(5, resource.getValue().getClassId().asString());
        statement.setString(6, ResourceWriters.getLabel(resource));
        statement.setString(7, objectMapper.writeValueAsString(resource));
        statement.addBatch();
        count ++ ;

        if(count % 10000 == 0) {
            statement.executeBatch();
            connection.commit();

            System.out.println("..." + count + " resources committed...");
        }
    }

    @Override
    public void endResources() throws Exception {
        System.out.println("Closing... " + count + " resources written in total.");
        statement.executeBatch();
        statement.close();
        connection.commit();
    }

    public void close() throws SQLException {

        System.out.println(count + " resources written.");
    }
}
