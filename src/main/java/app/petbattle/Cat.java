package app.petbattle;

import app.petbattle.utils.Scalr;
import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;

@MongoEntity(collection = "cats")
public class Cat extends PanacheMongoEntity {

    public Integer count;

    public Boolean vote;

    public String image;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Boolean getVote() {
        return vote;
    }

    public void setVote(Boolean vote) {
        this.vote = vote;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void vote() {
        if (vote) {
            setCount(getCount() + 1);
        } else {
            setCount(getCount() - 1);
        }
    }

    public void resizeCat() {
        try {
            String id = getImage().replaceFirst("^data:image/[^;]*;base64,?","");
            byte[] imageData = Base64.getDecoder().decode(id);
            InputStream is = new ByteArrayInputStream(imageData);
            BufferedImage _tmp = ImageIO.read(is);
            BufferedImage scaledImage = Scalr.resize(_tmp, 300); // Scale image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(scaledImage, "jpg", baos);
            baos.flush();
            String encodedString = Base64
                    .getEncoder()
                    .encodeToString(baos.toByteArray());
            setImage("data:image/jpeg;base64," + encodedString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
