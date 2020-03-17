package org.acme.mongodb.panache;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;

import javax.ws.rs.*;
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

}
