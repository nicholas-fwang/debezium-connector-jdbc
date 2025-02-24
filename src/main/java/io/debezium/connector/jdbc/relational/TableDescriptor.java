/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.jdbc.relational;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.debezium.annotation.Immutable;

/**
 * Describes a relational table.
 *
 * @author Chris Cranford
 */
@Immutable
public class TableDescriptor {

    private final TableId id;
    private final String tableType;
    private final Map<String, ColumnDescriptor> columns = new LinkedHashMap<>();
    private final List<String> primaryKeyColumnNames;

    private TableDescriptor(TableId id, String tableType, List<ColumnDescriptor> columns, List<String> primaryKeyColumnNames) {
        this.id = id;
        this.tableType = tableType;
        this.primaryKeyColumnNames = primaryKeyColumnNames;

        columns.forEach(c -> this.columns.put(c.getColumnName(), c));
    }

    public TableId getId() {
        return id;
    }

    public String getTableType() {
        return tableType;
    }

    public Collection<ColumnDescriptor> getColumns() {
        return columns.values();
    }

    public ColumnDescriptor getColumnByName(String columnName) {
        return columns.get(columnName);
    }

    public boolean hasColumn(String columnName) {
        return columns.containsKey(columnName);
    }

    public List<String> getPrimaryKeyColumnNames() {
        return primaryKeyColumnNames;
    }

    public Collection<ColumnDescriptor> getPrimaryKeyColumns() {
        return columns.values().stream().filter(c -> primaryKeyColumnNames.contains(c.getColumnName())).collect(Collectors.toList());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String catalogName;
        private String schemaName;
        private String tableName;
        private String tableType;
        private List<ColumnDescriptor> columns = new ArrayList<>();
        private List<String> primaryKeyColumnNames = new ArrayList<>();

        private Builder() {
        }

        public Builder catalogName(String catalogName) {
            this.catalogName = catalogName;
            return this;
        }

        public Builder schemaName(String schemaName) {
            this.schemaName = schemaName;
            return this;
        }

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder type(String tableType) {
            this.tableType = tableType;
            return this;
        }

        public Builder column(ColumnDescriptor column) {
            this.columns.add(column);
            return this;
        }

        public Builder columns(List<ColumnDescriptor> columns) {
            this.columns.addAll(columns);
            return this;
        }

        public Builder keyColumn(ColumnDescriptor column) {
            return keyColumn(column.getColumnName());
        }

        public Builder keyColumn(String columnName) {
            this.primaryKeyColumnNames.add(columnName);
            return this;
        }

        public TableDescriptor build() {
            final TableId id = new TableId(catalogName, schemaName, tableName);
            return new TableDescriptor(id, tableType, columns, primaryKeyColumnNames);
        }
    }
}
