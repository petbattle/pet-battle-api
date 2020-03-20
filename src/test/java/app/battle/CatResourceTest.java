package app.battle;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

//@QuarkusTestResource(DatabaseResource.class) - wont work on fedora with podman cgroups v2
@QuarkusTest
public class CatResourceTest {

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
        given()
                .when().get("/cats/count")
                .then()
                .statusCode(200);
        //.body(is("hello"));
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
