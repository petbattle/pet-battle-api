package app.petbattle;

import app.petbattle.utils.Scalr;
import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@MongoEntity(collection = "cats")
public class Cat extends ReactivePanacheMongoEntity {

    public Integer count;

    public Boolean vote;

    public Boolean issff;

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

    public Boolean getIssff() {
        return issff;
    }

    public void setIssff(Boolean issff) {
        this.issff = issff;
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

    /**
     * convert to jpeg and resize all images to 300px ~15k size for speed
     */
    public void resizeCat() {
        try {
            String p = "^data:image/([^;]*);base64,?";
            String raw = getImage().replaceFirst(p, "");
            byte[] imageData = Base64.getDecoder().decode(raw);
            InputStream is = new ByteArrayInputStream(imageData);
            BufferedImage _tmp = ImageIO.read(is);
            BufferedImage scaledImage = Scalr.resize(_tmp, 300); // Scale image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedImage newImage = new BufferedImage(scaledImage.getWidth(), scaledImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            newImage.createGraphics().drawImage(scaledImage, 0, 0, null);
            ImageIO.write(newImage, "jpeg", baos);
            baos.flush();
            String encodedString = Base64
                    .getEncoder()
                    .encodeToString(baos.toByteArray());
            setImage("data:image/jpeg;base64," + encodedString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String getUrlSafeImage() {
        String p = "^data:image/([^;]*);base64,?";
        String raw = getImage().replaceFirst(p, "");
        byte[] bytes = Base64.getDecoder().decode(raw);
        return '\"' + Base64.getUrlEncoder().encodeToString(bytes) + '\"';
    }

}
