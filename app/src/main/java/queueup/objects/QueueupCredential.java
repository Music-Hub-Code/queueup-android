package queueup.objects;

/**
 * Created by Louis on 6/3/2015.
 */
public class QueueupCredential {
    public String userId;
    public String clientToken;

    public QueueupCredential(String userId, String clientToken) {
        if (clientToken == null || userId == null) {
            throw new UnsupportedOperationException();
        }
        this.userId = userId;
        this.clientToken = clientToken;
    }
}
