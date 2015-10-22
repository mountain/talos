package org.talos.vec.event;

public interface ExecutorListener {

    public void onVecSetAdded(String bkeySrc, String vkey);

    public void onVecSetDeleted(String bkeySrc, String vkey);

    public void onRecAdded(String bkeySrc, String vkeyFrom, String vkeyTo);

    public void onRecDeleted(String bkeySrc, String vkeyFrom, String vkeyTo);

}
