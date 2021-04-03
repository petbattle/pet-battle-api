package app.petbattle.utils;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.io.InputStream;
import java.util.Properties;

@ApplicationScoped
public class ApplicationLifecycle {

    private final Logger log = LoggerFactory.getLogger(ApplicationLifecycle.class);

    void onStart(@Observes StartupEvent ev) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream confFile = loader.getResourceAsStream("git.properties");
            Properties prop = new Properties();
            prop.load(confFile);
            prop.forEach((k, v) -> {
                System.out.println("GITINFO -> " + k + ":" + v);
            });
        } catch (Exception ex) {
            System.out.println("GITINFO -> Unable to get git.properties file " + ex.getMessage());
        }
    }
}
