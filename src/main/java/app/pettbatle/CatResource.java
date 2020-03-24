package app.pettbatle;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.openapi.annotations.Operation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

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
    public List<Cat> list() {
        return Cat.listAll();
    }

    @GET
    @Path("/ids")
    @Operation(operationId = "ids",
            summary = "get all cat ids",
            description = "This operation retrieves all cat ids from the database",
            deprecated = false,
            hidden = false)
    public List<CatId> catids() {
        PanacheQuery<CatId> query = Cat.findAll().project(CatId.class);
        return query.list();
    }

    @GET
    @Path("/topcats")
    @Operation(operationId = "topcats",
            summary = "get sorted list of top 3 cats by count descending",
            description = "This operation retrieves top 3 cats from the database sorted by count descending",
            deprecated = false,
            hidden = false)
    public List<Cat> topcats() {
        PanacheQuery<Cat> query = Cat.findAll(Sort.by("count").descending()).page(Page.ofSize(3));
        return query.list();
    }

    @GET
    @Path("/{id}")
    @Operation(operationId = "getById",
            summary = "get cat by id",
            description = "This operation retrieves a cat by id from the database",
            deprecated = false,
            hidden = false)
    public Cat get(@PathParam("id") String id) {
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
        cat.persistOrUpdate();
        return Response.status(201).entity(cat.id).build();
    }

    @PUT
    @Path("/")
    @Operation(operationId = "update",
            summary = "update cat",
            description = "This operation updates a cat (id supplied) - prefer POST method",
            deprecated = false,
            hidden = false)
    public void update(Cat cat) {
        cat.update();
    }

    @DELETE
    @Path("/{id}")
    @Operation(operationId = "delete",
            summary = "delete cat by id",
            description = "This operation deletes a cat by id",
            deprecated = false,
            hidden = false)
    public void delete(@PathParam("id") String id) {
        Cat cat = Cat.findById(new ObjectId(id));
        cat.delete();
    }

    @GET
    @Path("/count")
    @Operation(operationId = "count",
            summary = "count all cats",
            description = "This operation returns a count of all cats in the database",
            deprecated = false,
            hidden = false)
    public Long count() {
        return Cat.count();
    }

    @DELETE
    @Path("/kittykiller")
    @Operation(operationId = "kittykiller",
            summary = "⚡ remove all cats ⚡",
            description = "This operation deletes all cats from the database",
            deprecated = false,
            hidden = false)
    public void deleteAll() {
        Cat.deleteAll();
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
        PanacheQuery<Cat> filteredCats;

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

        result.setRecordsFiltered(filteredCats.count());
        result.setData(filteredCats.list());
        result.setRecordsTotal(Cat.count());

        return result;
    }

}
