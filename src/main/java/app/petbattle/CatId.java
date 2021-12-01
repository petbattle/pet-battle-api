package app.petbattle;

import io.quarkus.mongodb.panache.common.ProjectionFor;
import org.bson.types.ObjectId;

@ProjectionFor(Cat.class)
public class CatId {
    public ObjectId id;
    public Integer count;
    public Boolean vote;
}
