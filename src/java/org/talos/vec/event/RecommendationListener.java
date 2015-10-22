package org.talos.vec.event;

public interface RecommendationListener {

    public void onItemAdded(long vecid, float score);

    public void onItemRemoved(long vecid, float score);

}
