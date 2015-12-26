package org.louiswilliams.queueupplayer.queueup.objects;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class SpotifyUser extends SpotifyObject {

    public static final String PRODUCT_FREE = "free";
    public static final String PRODUCT_OPEN = "open";
    public static final String PRODUCT_PREMIUM = "premium";

    public List<String> images;
    public String product;

    public SpotifyUser(JSONObject obj) {
        super(obj);

        name = obj.optString("display_name");
        product = obj.optString("product");
    }
}
