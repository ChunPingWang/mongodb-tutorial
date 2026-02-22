package com.mongodb.course.m06.geospatial;

import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoreQueryService {

    private final MongoTemplate mongoTemplate;

    public StoreQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // NearQuery + geoNear() → GeoResults
    public GeoResults<Store> findNearbyStores(double longitude, double latitude, double maxDistanceMeters) {
        NearQuery nearQuery = NearQuery.near(new Point(longitude, latitude))
                .maxDistance(new Distance(maxDistanceMeters / 1000.0, Metrics.KILOMETERS))
                .spherical(true);
        return mongoTemplate.geoNear(nearQuery, Store.class);
    }

    // withinSphere(Circle)
    public List<Store> findStoresWithinRadius(double longitude, double latitude, double radiusKm) {
        Query query = new Query(
                Criteria.where("location").withinSphere(
                        new Circle(new Point(longitude, latitude),
                                new Distance(radiusKm, Metrics.KILOMETERS))
                )
        );
        return mongoTemplate.find(query, Store.class);
    }

    // Geo + boolean filter
    public GeoResults<Store> findNearbyOpenStores(double longitude, double latitude, double maxDistanceKm) {
        NearQuery nearQuery = NearQuery.near(new Point(longitude, latitude))
                .maxDistance(new Distance(maxDistanceKm, Metrics.KILOMETERS))
                .spherical(true)
                .query(new Query(Criteria.where("open").is(true)));
        return mongoTemplate.geoNear(nearQuery, Store.class);
    }

    // Geo + category filter
    public GeoResults<Store> findNearbyByCategory(double longitude, double latitude,
                                                   double maxDistanceKm, String category) {
        NearQuery nearQuery = NearQuery.near(new Point(longitude, latitude))
                .maxDistance(new Distance(maxDistanceKm, Metrics.KILOMETERS))
                .spherical(true)
                .query(new Query(Criteria.where("category").is(category)));
        return mongoTemplate.geoNear(nearQuery, Store.class);
    }

    // within(Box)
    public List<Store> findStoresInBox(Point lowerLeft, Point upperRight) {
        Query query = new Query(
                Criteria.where("location").within(new Box(lowerLeft, upperRight))
        );
        return mongoTemplate.find(query, Store.class);
    }
}
