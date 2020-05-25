package app.battle;

import app.petbattle.CatResource;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Mock
@ApplicationScoped
public class MockCatService extends CatResource {

    private static final Logger LOGGER = LoggerFactory.getLogger("MockCatService");

    @Override
    @GET
    @Path("/count")
    public Uni<Long> count() {
        LOGGER.info("<<< MockCatService <<<");
        return Uni.createFrom().nothing();
    }

}
