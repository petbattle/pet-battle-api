package app.petbattle.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
public interface NSFFService {

    @POST
    @Path("/v1/models/test_model:predict")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    NSFFResponse nsff(String request);
    // NSFFResponse nsff(NSFFRequest request) should be here, but tensorflow is picky with the formatting
}
