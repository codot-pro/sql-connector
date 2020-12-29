package pro.codot.camunda.sqlconnector;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@Component
public class ReadDataFromDatabaseMetadata implements JavaDelegate {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReadDataFromDatabaseMetadata.class);

    private final static String OUTPUT_VALUE = "OUTPUT_VALUE";
    private final static String EMPTY_RESULT = "[]";

    private Expression datasourceProperties;
    private Expression query;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        String queryString = (String) query.getValue(delegateExecution);
        String properties = (String) datasourceProperties.getValue(delegateExecution);

        try (Connection connection = CustomDataSource.getConnection(getPropsFromString(properties))) {
            String result = readData(connection, queryString);
            delegateExecution.setVariable(OUTPUT_VALUE, result);
            LOGGER.info(result);
        }
    }

    private String readData(Connection connection, String query) throws SQLException {

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");

            String outObject;
            while (rs.next()) {
                outObject = getEntity(rs);
                stringBuilder.append(outObject).append(",");
            }

            String result = stringBuilder.toString();
            if (result.length() <= 1) {
                return EMPTY_RESULT;
            }
            result = result.substring(0, result.length() - 1) + "]";

            return result;
        }
    }

    private String getEntity(ResultSet rs) throws SQLException {

        int columnCount = rs.getMetaData().getColumnCount();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        String element;
        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
            element = getEntityElement(rs, columnIndex);
            stringBuilder.append(element).append(",");
        }
        String result = stringBuilder.toString();
        result = result.substring(0, result.length() - 1);
        return result + "}";
    }

    private String getEntityElement(ResultSet rs, int columnIndex) throws SQLException {

        ResultSetMetaData metaData = rs.getMetaData();
        String columnClassName = metaData.getColumnClassName(columnIndex);

        String value;

// exception occurs if data from ResultSet won't be readable using getString
// need implement logic like for java.sql.Timestamp
        if (columnClassName.equalsIgnoreCase("java.sql.Timestamp")) {
            Timestamp timestamp = rs.getTimestamp(columnIndex);
            value = timestamp.toInstant().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        } else {
            value = rs.getString(columnIndex);
        }

        return "\"" +
                metaData.getColumnName(columnIndex) +
                "\"" +
                " : " +
                "\"" +
                value +
                "\"";
    }

    private static Properties getPropsFromString(String lines) throws IOException {
        try (InputStream inputStream = new ByteArrayInputStream(lines.getBytes())) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        }
    }
}

