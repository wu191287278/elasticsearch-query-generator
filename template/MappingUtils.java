package com.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class MappingUtils {

    public static void main(String[] args) throws SQLException {
        String jdbcUrl = "jdbc:mysql://localhost/users";
        String username = "root";
        String password = "root";
        List<String> containsTables = Arrays.asList("users");
        JSONObject json = new JSONObject(true);
        Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
         try (ResultSet resultSet = connection.getMetaData().getTables(null, null, null, null)) {
            while (resultSet.next()) {
                String tableName = resultSet.getString("TABLE_NAME");
                if (!containsTables.isEmpty() && !containsTables.contains(tableName)) continue;
                JSONObject fields = getFields(connection, tableName, false);
                JSONObject mappings = new JSONObject(true);
                JSONObject type = new JSONObject(true);
                JSONObject properties = new JSONObject(true);
                JSONObject settings = new JSONObject(true);
                settings.put("number_of_shards",5);
                settings.put("number_of_replicas",1);
                mappings.put("settings",settings);
                properties.put("properties", fields);
                type.put("_doc", properties);
                mappings.put("mappings", type);

                json.put(tableName, mappings);
            }
        }

        System.err.println(JSON.toJSONString(json, true));

        connection.close();
    }

    private static JSONObject getFields(Connection connection, String tableName, boolean containsRemark) throws SQLException {
        JSONObject fields = new JSONObject(true);
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, null)) {
            while (columns.next()) {
                String name = columns.getString("COLUMN_NAME").toLowerCase();
                String type = columns.getString("TYPE_NAME").toLowerCase();
                String remark = columns.getString("REMARKS");
                JSONObject attributes = new JSONObject(true);
                fields.put(lowerCamel(name), attributes);
                String elasticSearchType = getElasticSearchType(type);
                if (name.contains("title") ||
                        name.contains("tag") ||
                        name.contains("about") ||
                        name.contains("keyword") ||
                        name.contains("description")) {
                    attributes.put("type", "text");
                    attributes.put("analyzer", "smartcn");
                } else if (elasticSearchType.equals("date")) {
                    attributes.put("format", "epoch_millis");
                    attributes.put("type", elasticSearchType);
                } else {
                    attributes.put("type", elasticSearchType);
                }
                if (containsRemark) {
                    attributes.put("remark", "".equals(remark) ? null : remark.replaceAll("[\\n|\\r]", ""));
                }
            }
        }
        return fields;
    }

    public static String lowerCamel(String str) {
        String[] split = str.replaceAll("[^a-zA-Z0-9]", "_").split("_");
        StringBuilder sb = new StringBuilder();
        sb.append(split[0].toLowerCase());
        for (int i = 1; i < split.length; i++) {
            String keyword = split[i].toLowerCase();
            sb.append(keyword.substring(0, 1).toUpperCase())
                    .append(keyword.length() > 1 ? keyword.substring(1) : "");
        }
        return sb.toString();
    }


    private static String getElasticSearchType(String jdbcType) {
        if (jdbcType.equalsIgnoreCase("CHAR") || jdbcType.equalsIgnoreCase("VARCHAR")
                || jdbcType.equalsIgnoreCase("VARCHAR2") || jdbcType.equalsIgnoreCase("set")
                || jdbcType.equalsIgnoreCase("enum")) {
            return "keyword";
        }

        if (jdbcType.equalsIgnoreCase("TEXT") || jdbcType.equalsIgnoreCase("LONGTEXT")
                || jdbcType.equalsIgnoreCase("LONGVARCHAR") || jdbcType.equalsIgnoreCase("json")
                || jdbcType.equalsIgnoreCase("numeric")) {
            return "text";
        }

        if (jdbcType.equalsIgnoreCase("INT") || jdbcType.equalsIgnoreCase("INT UNSIGNED") || jdbcType.equalsIgnoreCase("INTEGER") || jdbcType.equalsIgnoreCase("MEDIUMINT UNSIGNED")) {
            return "integer";
        }

        if (jdbcType.equalsIgnoreCase("SMALLINT") || jdbcType.equalsIgnoreCase("SMALLINT UNSIGNED")) {
            return "short";
        }

        if (jdbcType.equalsIgnoreCase("TINYINT") || jdbcType.equalsIgnoreCase("TINYINT UNSIGNED")) {
            return "byte";
        }

        if (jdbcType.equalsIgnoreCase("DATETIME") || jdbcType.equalsIgnoreCase("TIMESTAMP")
                || jdbcType.equalsIgnoreCase("DATE") || jdbcType.equalsIgnoreCase("year") || jdbcType.equalsIgnoreCase("time")) {
            return "date";
        }

        if (jdbcType.equalsIgnoreCase("BIGINT") || jdbcType.equalsIgnoreCase("BIGINT UNSIGNED")) {
            return "long";
        }

        if (jdbcType.equalsIgnoreCase("DOUBLE") || jdbcType.equalsIgnoreCase("DOUBLE UNSIGNED")) {
            return "double";
        }

        if (jdbcType.equalsIgnoreCase("DECIMAL") || jdbcType.equalsIgnoreCase("DECIMAL UNSIGNED")) {
            return "double";
        }

        if (jdbcType.equalsIgnoreCase("FLOAT") || jdbcType.equalsIgnoreCase("FLOAT UNSIGNED")) {
            return "float";
        }

        if (jdbcType.equalsIgnoreCase("BYTE") || jdbcType.equalsIgnoreCase("BYTE UNSIGNED")) {
            return "byte";
        }

        if (jdbcType.equalsIgnoreCase("binary") || jdbcType.equalsIgnoreCase("geometry")) {
            return "binary";
        }

        return "text";
    }
}
