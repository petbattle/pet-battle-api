package org.acme.mongodb.panache;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.panache.common.Sort;
import org.bson.types.ObjectId;
import org.jboss.resteasy.annotations.Body;

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
    @Path("/topcats")
    public List<Cat> topcats() {
        return Cat.listAll(Sort.by("count").descending());
    }

    @GET
    @Path("/{id}")
    public Cat get(@PathParam("id") String id) {
        return Cat.findById(new ObjectId(id));
    }

    @POST
    public synchronized Response create(Cat cat) {
        cat.vote();
        cat.persistOrUpdate();
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

}
