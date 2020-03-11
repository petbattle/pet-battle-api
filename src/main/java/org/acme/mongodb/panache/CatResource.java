package org.acme.mongodb.panache;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import org.bson.types.ObjectId;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/cats")
@Consumes("application/json")
@Produces("application/json")
public class CatResource {

    @GET
    public List<Cat> list() {
        return Cat.listAll();
    }

    @GET
    @Path("/{id}")
    public Cat get(@PathParam("id") String id) {
        return Cat.findById(new ObjectId(id));
    }

    @POST
    public Response create(Cat cat) {
        cat.persist();
        return Response.status(201).entity(cat.id).build();
    }

    @PUT
    @Path("/")
    public void update(Cat cat) {
        cat.update();
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") String id) {
        Cat cat = Cat.findById(new ObjectId(id));
        cat.delete();
    }

    @GET
    @Path("/count")
    public Long count() {
        return Cat.count();
    }

    @PUT
    @Path("/{id}/{vote}")
    public void vote(@PathParam("id") String id, @PathParam("vote") Boolean vote) {
        Cat cat = Cat.findById(new ObjectId(id));
        cat.vote(vote);
        cat.update();
    }
}
