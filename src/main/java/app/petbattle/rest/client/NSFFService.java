package app.petbattle.rest.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
public interface NSFFService {

    @POST
    @Path("/v1/models/test_model:predict")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    NSFFResponse nsff(String request);
    // NSFFResponse nsff(NSFFRequest request) should be here, but tensorflow is picky with the formatting
}
