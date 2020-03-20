package app.battle;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

public class DatabaseResource implements QuarkusTestResourceLifecycleManager {

    public static final MongoDbContainer DATABASE = new MongoDbContainer();

    @Override
    public Map<String, String> start() {
        DATABASE.start();
        return null;
    }

    @Override
    public void stop() {
        DATABASE.stop();
    }
}
