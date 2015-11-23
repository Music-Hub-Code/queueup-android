package org.louiswilliams.queueupplayer.queueup.objects;

public class QueueUpApiCredential {
    public String userId;
    public String clientToken;

    public QueueUpApiCredential(String userId, String clientToken) {
        if (clientToken == null || userId == null) {
            throw new UnsupportedOperationException();
        }
        this.userId = userId;
        this.clientToken = clientToken;
    }
}
