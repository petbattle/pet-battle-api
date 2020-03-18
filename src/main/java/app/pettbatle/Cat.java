package app.pettbatle;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;

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
}
