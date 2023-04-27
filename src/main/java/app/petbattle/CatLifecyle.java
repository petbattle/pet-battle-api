package app.petbattle;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class CatLifecyle {

    void onStart(@Observes StartupEvent ev) {
        CatResource catResource = new CatResource();
        catResource.loadlitter();
    }

    void onStop(@Observes ShutdownEvent ev) {
        // empty
    }
}
