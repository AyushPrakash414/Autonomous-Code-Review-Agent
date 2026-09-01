package com.autonomousreview;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
class AtlasLiveConnectionTests {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    @DisplayName("Verify live connection to MongoDB Atlas database")
    void testAtlasDatabaseConnection() {
        Document pingCommand = new Document("ping", 1);
        Document result = mongoTemplate.getDb().runCommand(pingCommand);

        assertNotNull(result, "MongoDB ping response should not be null");
        Object okValue = result.get("ok");
        assertNotNull(okValue);
        assertTrue(((Number) okValue).doubleValue() == 1.0, "MongoDB Atlas ping command should return ok: 1.0");

        System.out.println("=================================================");
        System.out.println(">>> MONGO ATLAS CONNECTION VERIFIED SUCCESSFULLY!");
        System.out.println(">>> Ping Response: " + result.toJson());
        System.out.println(">>> Database Name: " + mongoTemplate.getDb().getName());
        System.out.println("=================================================");
    }
}
