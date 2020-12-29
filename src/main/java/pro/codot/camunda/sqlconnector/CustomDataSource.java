package pro.codot.camunda.sqlconnector;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public class CustomDataSource {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomDataSource.class);
    private static HikariDataSource ds;

    private CustomDataSource() {
    }

    public static Connection getConnection(Properties properties) throws SQLException {
        if (Objects.isNull(ds)) {
            synchronized (CustomDataSource.class) {
                if (Objects.isNull(ds)) {
                    LOGGER.info("Creating datasource ...");
                    createDataSource(properties);
                    LOGGER.info("Creating datasource ... done!");
                }
            }
        }
        return ds.getConnection();
    }

    private static void createDataSource(Properties properties) {
        HikariConfig config = new HikariConfig(properties);
        ds = new HikariDataSource(config);
    }
}
