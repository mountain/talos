package org.talos.voc;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class Vocabulary {

    public TObjectIntMap<String> wd2id;
    public TIntObjectMap<String> id2wd;

    public Vocabulary() {
        wd2id = new TObjectIntHashMap<String>();
        id2wd = new TIntObjectHashMap<String>();
    }

    public String word(int id) {
        return id2wd.get(id);
    }

    public int id(String word) {
        return wd2id.get(word);
    }

    public boolean contains(String word) {
        return wd2id.containsKey(word);
    }

    public boolean contains(int id) {
        return id2wd.containsKey(id);
    }

    public int add(String word) {
        if (contains(word)) {
            return id(word);
        }

        int id = wd2id.size();

        wd2id.put(word, id);
        id2wd.put(id, word);

        return id;
    }
}
