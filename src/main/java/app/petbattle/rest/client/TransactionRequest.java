package app.petbattle.rest.client;

public class TransactionRequest {

    private String id;
    private String image;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public TransactionRequest(String id, String image) {
        this.id = id;
        this.image = image;
    }
}
