package app.battle;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
@Epic("Pet Battle")
@Feature("PetEndpointTest")
class CatEndpointTest {

    private static final Logger log = LoggerFactory.getLogger("CatEndpointTest");

    @Test
    @Story("Test get all pets")
    void testCats() {
        RestAssured.given()
                .log().all()
                .when().get("/cats")
                .then()
                .log().all()
                .statusCode(200)
                .body("id", everyItem(is(notNullValue())))
                .body("image", everyItem(is(notNullValue())))
                .body("vote", everyItem(is(notNullValue())))
                .body("issff", everyItem(is(notNullValue())))
                .body("count", everyItem(is(notNullValue())));
    }

    @Test
    @Story("Test get all pet id's")
    void testCatIds() {
        RestAssured.given()
                .log().all()
                .when().get("/cats/ids")
                .then()
                .log().all()
                .statusCode(200)
                .body("id", everyItem(is(notNullValue())))
                .body("vote", everyItem(is(notNullValue())))
                .body("count", everyItem(is(notNullValue())));
    }

    @Test
    @Story("Test top 3 pets")
    void testTopCats() {
        RestAssured.given()
                .log().all()
                .when().get("/cats/topcats")
                .then()
                .log().all()
                .statusCode(200)
                .body("size()", is(3));
    }

    @Test
    @Story("Test get pet by id")
    void testCatById() {
        Response response = RestAssured.given()
                .log().all()
                .when().get("/cats/ids")
                .then()
                .log().all()
                .statusCode(200)
                .body("[0]", is(notNullValue()))
                .extract()
                .response();

        final String id = response.path("[0].id");

        RestAssured.given()
                .log().all()
                .when().get("/cats/" + id)
                .then()
                .log().all()
                .statusCode(200)
                .body("id", is(id));
    }

    @Test
    @Story("Test pet count")
    void testCatCount() {
        RestAssured.given()
                .log().all()
                .when().delete("/cats/kittykiller")
                .then()
                .log().all()
                .statusCode(200)
                .body(is(notNullValue()));

        RestAssured.given()
                .log().all()
                .when().get("/cats/count")
                .then()
                .log().all()
                .statusCode(200)
                .body(is("0"));

        RestAssured.given()
                .log().all()
                .when().get("/cats/loadlitter")
                .then()
                .log().all()
                .statusCode(204);

        RestAssured.given()
                .log().all()
                .when().get("/cats/count")
                .then()
                .log().all()
                .statusCode(200)
                .body(is("13"));
    }

    @Test
    @Story("Test pet create")
    void testCatCreate() {
        CatInstance catInstance = new CatInstance();

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(catInstance.cat)
                .log().all()
                .when().post("/cats")
                .then()
                .log().all()
                .statusCode(201)
                .body(is(notNullValue()));
    }

    @Test
    @Story("Test pet delete")
    void testCatDelete() {
        Response response = RestAssured.given()
                .log().all()
                .when().get("/cats/ids")
                .then()
                .log().all()
                .statusCode(200)
                .body("[0]", is(notNullValue()))
                .extract()
                .response();

        final String id = response.path("[0].id");

        RestAssured.given()
                .log().all()
                .when().delete("/cats/" + id)
                .then()
                .log().all()
                .statusCode(200)
                .body(is(notNullValue()));
    }

    @Test
    @Story("Test pet datatable")
    void testDatatable() {
        RestAssured.given()
                .log().all()
                .when().get("/cats/datatable")
                .then()
                .log().all()
                .statusCode(200)
                .body("size()", is(4));
    }

}
