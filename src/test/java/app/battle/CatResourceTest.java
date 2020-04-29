package app.battle;

import app.petbattle.CatResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;

//@QuarkusTestResource(DatabaseResource.class) - wont work on fedora with podman cgroups v2
@QuarkusTest
public class CatResourceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("CatResourceTest");

    @Inject
    CatResource service;

    @Test
    public void testCatEndpoint() {
        given()
                .when().get("/cats")
                .then()
                .statusCode(200);
        //.body(is("hello"));
    }

    @Test
    public void testCoutEndpoint() {
        Assertions.assertTrue(service.count().equals(3L));
    }

    @Test
    public void testIdEndpoint() {
        given()
                .when().get("/cats/ids")
                .then()
                .statusCode(200);
        //.body(is("hello"));
    }

    @Test
    public void testTopCatEndpoint() {
        given()
                .when().get("/cats/topcats")
                .then()
                .statusCode(200);
        //.body(is("hello"));
    }

}
