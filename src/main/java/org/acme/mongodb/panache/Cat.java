package org.acme.mongodb.panache;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;

@MongoEntity(collection = "cats")
public class Cat extends PanacheMongoEntity {

    public Integer votes;

    public String image;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }


    public Integer getVotes() {
        return votes;
    }

    public void setVotes(Integer votes) {
        this.votes = votes;
    }

    public void vote(Boolean vote) {
        if (vote) {
            setVotes(getVotes()+1);
        } else {
            setVotes(getVotes()-1);
        }
    }
}
