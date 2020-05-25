package app.petbattle;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.openapi.annotations.Operation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;

@Path("/cats")
@Consumes("application/json")
@Produces("application/json")
public class CatResource {

    @GET
    @Operation(operationId = "list",
            summary = "get all cats",
            description = "This operation retrieves all cats from the database",
            deprecated = false,
            hidden = false)
    public Uni<List<Cat>> list() {
        return Cat.listAll();
    }

    @GET
    @Path("/ids")
    @Operation(operationId = "ids",
            summary = "get all cat ids",
            description = "This operation retrieves all cat ids from the database",
            deprecated = false,
            hidden = false)
    public Uni<List<CatId>> catids() {
        ReactivePanacheQuery<CatId> query = Cat.findAll().project(CatId.class);
        return query.list();
    }

    @GET
    @Path("/topcats")
    @Operation(operationId = "topcats",
            summary = "get sorted list of top 3 cats by count descending",
            description = "This operation retrieves top 3 cats from the database sorted by count descending",
            deprecated = false,
            hidden = false)
    public Uni<List<Cat>> topcats() {
        ReactivePanacheQuery<Cat> query = Cat.findAll(Sort.by("count").descending()).page(Page.ofSize(3));
        return query.list();
    }

    @GET
    @Path("/{id}")
    @Operation(operationId = "getById",
            summary = "get cat by id",
            description = "This operation retrieves a cat by id from the database",
            deprecated = false,
            hidden = false)
    public Uni<Cat> get(@PathParam("id") String id) {
        return Cat.findById(new ObjectId(id));
    }

    @POST
    @Operation(operationId = "createOrUpdate",
            summary = "create or update cat",
            description = "This operation creates or updates a cat (if id supplied)",
            deprecated = false,
            hidden = false)
    @Metered(unit = MetricUnits.PER_SECOND, name = "cats-uploaded", description = "Frequency of cats uploaded")
    public synchronized Response create(Cat cat) {
        cat.vote();
        cat.resizeCat();
        cat.persistOrUpdate().await().indefinitely();
        return Response.status(201).entity(cat.id).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(operationId = "delete",
            summary = "delete cat by id",
            description = "This operation deletes a cat by id",
            deprecated = false,
            hidden = false)
    public Uni<Boolean> delete(@PathParam("id") String id) {
        return Cat.deleteById(new ObjectId(id));
    }

    @GET
    @Path("/count")
    @Operation(operationId = "count",
            summary = "count all cats",
            description = "This operation returns a count of all cats in the database",
            deprecated = false,
            hidden = false)
    public Uni<Long> count() {
        return Cat.count();
    }

    @DELETE
    @Path("/kittykiller")
    @Operation(operationId = "kittykiller",
            summary = "⚡ remove all cats ⚡",
            description = "This operation deletes all cats from the database",
            deprecated = false,
            hidden = false)
    public Uni<Long> deleteAll() {
        return Cat.deleteAll();
    }

    @GET
    @Path("/datatable")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "datatable",
            summary = "datatable cats by id",
            description = "This operation returns a datatable of cats - https://www.datatables.net/",
            deprecated = false,
            hidden = false)
    public DataTable datatable(
            @QueryParam(value = "draw") @DefaultValue("1") int draw,
            @QueryParam(value = "start") @DefaultValue("0") int start,
            @QueryParam(value = "length") @DefaultValue("10") int length,
            @QueryParam(value = "search[value]") String searchVal
    ) {
        // Begin result
        DataTable result = new DataTable();
        result.setDraw(draw);

        // Filter based on search
        ReactivePanacheQuery<Cat> filteredCats;

        // FIXME Search busted with ID
        /*if (searchVal != null && !searchVal.isEmpty()) {
            String s = "{\"_id\": ObjectId(\":search\")}";
            filteredCats = Cat.<Cat>find(s, Parameters.with("search", searchVal));
        } else {*/
        filteredCats = Cat.findAll();
        //}
        // Page and return
        int page_number = start / length;
        filteredCats.page(page_number, length);

        result.setData(filteredCats.stream().collectItems().asList().await().atMost(Duration.ofSeconds(1)));
        result.setRecordsFiltered(filteredCats.count().await().atMost(Duration.ofSeconds(1)));
        result.setRecordsTotal(Cat.count().await().atMost(Duration.ofSeconds(1)));

        return result;
    }

    @GET
    @Path("/loadlitter")
    @Operation(operationId = "loadlitter",
            summary = "preload db with cats",
            description = "This operation adds some cats to the database if it is empty",
            deprecated = false,
            hidden = false)
    public static void loadlitter() {
        if (Cat.count().await().indefinitely() > 0)
            return;
        final List<String> catList = Arrays.asList("cat1.jpeg", "cat2.jpeg", "cat3.jpeg", "cat4.jpeg", "cat5.jpeg", "cat6.jpeg", "cat7.jpeg", "cat8.jpeg", "cat9.jpeg", "cat10.jpeg", "cat11.jpeg", "cat12.jpeg", "dog1.jpeg");
        for (String tc : catList) {
            try {
                InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(tc);
                Cat cat = new Cat();
                cat.setCount(new Random().nextInt(5) + 1);
                cat.setVote(false);
                byte[] fileContent = new byte[0];
                fileContent = is.readAllBytes();
                String encodedString = Base64
                        .getEncoder()
                        .encodeToString(fileContent);
                cat.setImage("data:image/jpeg;base64," + encodedString);
                cat.resizeCat();
                cat.persistOrUpdate().await().indefinitely();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
