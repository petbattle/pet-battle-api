package app.petbattle.rest.client;

public class TransactionResponse {

    private String id;
    private boolean issfw;
    private Double nsfw;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isIssfw() {
        return issfw;
    }

    public void setIssfw(boolean issfw) {
        this.issfw = issfw;
    }

    public Double getNsfw() {
        return nsfw;
    }

    public void setNsfw(Double nsfw) {
        this.nsfw = nsfw;
    }

    public Double getSfw() {
        return sfw;
    }

    public void setSfw(Double sfw) {
        this.sfw = sfw;
    }

    private Double sfw;

    @Override
    public String toString() {
        return "TransactionResponse{" +
                "id='" + id + '\'' +
                ", issfw=" + issfw +
                ", nsfw=" + nsfw +
                ", sfw=" + sfw +
                '}';
    }

}
