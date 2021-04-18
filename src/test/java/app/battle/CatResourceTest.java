package app.battle;

import app.petbattle.Cat;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusTest
class CatResourceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("CatResourceTest");

    @Test
    void testCat() {
        PanacheMock.mock(Cat.class);

        Mockito.when(Cat.count()).thenReturn(Uni.createFrom().item(23l));
        Assertions.assertEquals(23, Cat.count().await().indefinitely());

        CatInstance catInstance = new CatInstance();
        Cat cat = catInstance.cat;

        Assertions.assertEquals(catInstance.redCatString, cat.getImage());

        cat.setVote(false);
        Assertions.assertEquals(false, cat.getVote());

        cat.setIssff(false);
        Assertions.assertEquals(false, cat.getIssff());
    }

}
