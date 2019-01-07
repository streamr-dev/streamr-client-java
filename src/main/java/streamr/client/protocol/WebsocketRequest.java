package streamr.client.protocol;

public abstract class WebsocketRequest {
    protected String type;

    public WebsocketRequest(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
