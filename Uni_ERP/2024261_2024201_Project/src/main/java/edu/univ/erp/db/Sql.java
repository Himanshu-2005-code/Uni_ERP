package edu.univ.erp.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Small helpers over JDBC prepared statements. */
public final class Sql {

    private Sql() {
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public static <T> List<T> list(Connection c, String sql, RowMapper<T> mapper, Object... params)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out;
            }
        }
    }

    public static <T> Optional<T> one(Connection c, String sql, RowMapper<T> mapper, Object... params)
            throws SQLException {
        List<T> l = list(c, sql, mapper, params);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public static int count(Connection c, String sql, Object... params) throws SQLException {
        return one(c, sql, rs -> rs.getInt(1), params).orElse(0);
    }

    public static double number(Connection c, String sql, Object... params) throws SQLException {
        return one(c, sql, rs -> rs.getDouble(1), params).orElse(0.0);
    }

    public static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Executes an INSERT and returns the generated key. */
    public static int insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p == null) {
                ps.setNull(i + 1, Types.NULL);
            } else if (p instanceof LocalDateTime t) {
                ps.setTimestamp(i + 1, Timestamp.valueOf(t));
            } else if (p instanceof Enum<?> e) {
                ps.setString(i + 1, e.name());
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }

    public static LocalDateTime time(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    public static Integer intOrNull(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    public static Double doubleOrNull(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }
}
