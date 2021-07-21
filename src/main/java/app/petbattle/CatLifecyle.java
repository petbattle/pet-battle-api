package app.petbattle;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

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
