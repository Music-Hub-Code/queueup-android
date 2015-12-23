package org.louiswilliams.queueupplayer.fragment;

public interface BackButtonListener {
    /**
     * Interface to allow fragments to intercept back button actions
     *
     * @return True of consumed, false to continue default action
     */
    boolean onBackButtonPressed();
}
