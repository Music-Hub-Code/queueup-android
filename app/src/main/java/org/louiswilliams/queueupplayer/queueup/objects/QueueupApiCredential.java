package org.louiswilliams.queueupplayer.queueup.objects;

/**
 * Created by Louis on 6/3/2015.
 */
public class QueueupApiCredential {
    public String userId;
    public String clientToken;

    public QueueupApiCredential(String userId, String clientToken) {
        if (clientToken == null || userId == null) {
            throw new UnsupportedOperationException();
        }
        this.userId = userId;
        this.clientToken = clientToken;
    }
}
