package app.petbattle;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@OpenAPIDefinition(
        tags = {
                @Tag(name = "cats", description = "Cat Operations.")
        },
        info = @Info(
                title = "Cat API",
                version = "0.0.1",
                contact = @Contact(
                        name = "Pet Battle Support",
                        url = "https://petbattle.app/contact",
                        email = "techsupport@petbattle.app"),
                license = @License(
                        name = "Apache 2.0",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"))
)
public class CatApplication {
}
