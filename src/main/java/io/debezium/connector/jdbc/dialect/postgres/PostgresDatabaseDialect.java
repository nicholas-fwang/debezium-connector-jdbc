/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.jdbc.dialect.postgres;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;

import io.debezium.connector.jdbc.JdbcSinkConnectorConfig;
import io.debezium.connector.jdbc.SinkRecordDescriptor;
import io.debezium.connector.jdbc.dialect.DatabaseDialect;
import io.debezium.connector.jdbc.dialect.DatabaseDialectProvider;
import io.debezium.connector.jdbc.dialect.GeneralDatabaseDialect;
import io.debezium.connector.jdbc.dialect.SqlStatementBuilder;
import io.debezium.connector.jdbc.relational.TableDescriptor;
import io.debezium.connector.jdbc.relational.TableId;

/**
 * A {@link DatabaseDialect} implementation for PostgreSQL.
 *
 * @author Chris Cranford
 */
public class PostgresDatabaseDialect extends GeneralDatabaseDialect {

    public static class PostgresDatabaseDialectProvider implements DatabaseDialectProvider {
        @Override
        public boolean supports(Dialect dialect) {
            return dialect instanceof PostgreSQLDialect;
        }

        @Override
        public Class<?> name() {
            return PostgresDatabaseDialect.class;
        }

        @Override
        public DatabaseDialect instantiate(JdbcSinkConnectorConfig config, SessionFactory sessionFactory) {
            return new PostgresDatabaseDialect(config, sessionFactory);
        }
    }

    private PostgresDatabaseDialect(JdbcSinkConnectorConfig config, SessionFactory sessionFactory) {
        super(config, sessionFactory);
    }

    @Override
    public int getMaxTimestampPrecision() {
        return 6;
    }

    @Override
    public boolean tableExists(Connection connection, TableId tableId) throws SQLException {
        if (!getConfig().isQuoteIdentifiers()) {
            // This means that the table will be stored as lower-case
            tableId = tableId.toLowerCase();
        }
        return super.tableExists(connection, tableId);
    }

    @Override
    public TableDescriptor readTable(Connection connection, TableId tableId) throws SQLException {
        if (!getConfig().isQuoteIdentifiers()) {
            // This means that the table will be stored as lower-case
            tableId = tableId.toLowerCase();
        }
        return super.readTable(connection, tableId);
    }

    @Override
    public String getUpsertStatement(TableDescriptor table, SinkRecordDescriptor record) {
        final SqlStatementBuilder builder = new SqlStatementBuilder();
        builder.append("INSERT INTO ");
        builder.append(getQualifiedTableName(table.getId()));
        builder.append(" (");
        builder.appendLists(",", record.getKeyFieldNames(), record.getNonKeyFieldNames(), (name) -> columnNameFromField(name, record));
        builder.append(") VALUES (");
        builder.appendLists(",", record.getKeyFieldNames(), record.getNonKeyFieldNames(), (name) -> columnQueryBindingFromField(name, record));
        builder.append(") ON CONFLICT (");
        builder.appendList(",", record.getKeyFieldNames(), (name) -> columnNameFromField(name, record));
        if (record.getNonKeyFieldNames().isEmpty()) {
            builder.append(") DO NOTHING");
        }
        else {
            builder.append(") DO UPDATE SET ");
            builder.appendList(",", record.getNonKeyFieldNames(), (name) -> {
                final String columnNme = columnNameFromField(name, record);
                return columnNme + "=EXCLUDED." + columnNme;
            });
        }
        return builder.build();
    }

    @Override
    public String getByteArrayFormat() {
        return "'\\x%s'";
    }

    @Override
    public String getFormattedBoolean(boolean value) {
        // PostgreSQL maps logical TRUE/FALSE for boolean data types
        return value ? "TRUE" : "FALSE";
    }

    @Override
    public String getFormattedDateTimeWithNanos(TemporalAccessor value) {
        return String.format("'%s'", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value));
    }

    @Override
    public String getFormattedTime(TemporalAccessor value) {
        return String.format("'%s'", DateTimeFormatter.ISO_LOCAL_TIME.format(value));
    }

    @Override
    protected Optional<String> getDatabaseTimeZoneQuery() {
        return Optional.of("SELECT CURRENT_SETTING('TIMEZONE')");
    }

    @Override
    protected void registerTypes() {
        super.registerTypes();

        registerType(TimeWithTimezoneType.INSTANCE);
        registerType(IntervalType.INSTANCE);
        registerType(SerialType.INSTANCE);
        registerType(BitType.INSTANCE);
        registerType(JsonType.INSTANCE);
        registerType(UuidType.INSTANCE);
        registerType(EnumType.INSTANCE);
        registerType(PointType.INSTANCE);
        registerType(MoneyType.INSTANCE);
        registerType(XmlType.INSTANCE);
        registerType(LtreeType.INSTANCE);
        registerType(MapToHstoreType.INSTANCE);

        // Allows binding string-based types if column type propagation is enabled
        registerType(RangeType.INSTANCE);
        registerType(CidrType.INSTANCE);
        registerType(MacAddressType.INSTANCE);
        registerType(InetType.INSTANCE);
        registerType(CaseInsensitiveTextType.INSTANCE);
        registerType(OidType.INSTANCE);
    }

    @Override
    public int getMaxVarcharLengthInKey() {
        // Setting to Integer.MAX_VALUE forces PostgreSQL to use TEXT data types in primary keys
        // when no explicit size on the column is specified.
        return Integer.MAX_VALUE;
    }
}
