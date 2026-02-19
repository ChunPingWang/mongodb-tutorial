package com.mongodb.course.m02.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CassandraProductService {

    private final CqlSession cqlSession;

    public CassandraProductService(CqlSession cqlSession) {
        this.cqlSession = cqlSession;
    }

    public void save(String id, String name, String category, BigDecimal price) {
        cqlSession.execute(SimpleStatement.newInstance(
                "INSERT INTO products (category, id, name, price) VALUES (?, ?, ?, ?)",
                category, id, name, price));
    }

    public Row findByCategoryAndId(String category, String id) {
        ResultSet rs = cqlSession.execute(SimpleStatement.newInstance(
                "SELECT * FROM products WHERE category = ? AND id = ?",
                category, id));
        return rs.one();
    }

    public List<Row> findByCategory(String category) {
        ResultSet rs = cqlSession.execute(SimpleStatement.newInstance(
                "SELECT * FROM products WHERE category = ?",
                category));
        return rs.all();
    }
}
