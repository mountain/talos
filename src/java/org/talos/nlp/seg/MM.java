package org.talos.nlp.seg;

import java.util.ArrayList;

import org.talos.nlp.Chunk;
import org.talos.util.IFn;

public class MM implements IFn<Chunk[], Chunk[]> {

    @Override
    public Chunk[] call(Chunk[] chunks) {

        int maxLength = chunks[0].length();
        int j;
        // find the maximum word length
        for (j = 1; j < chunks.length; j++) {
            if (chunks[j].length() > maxLength)
                maxLength = chunks[j].length();
        }

        // get the items that the word length equals to
        // the max's length.
        ArrayList<Chunk> chunkArr = new ArrayList<Chunk>(chunks.length);
        for (j = 0; j < chunks.length; j++) {
            if (chunks[j].length() == maxLength)
                chunkArr.add(chunks[j]);
        }

        Chunk[] lchunk = new Chunk[chunkArr.size()];
        chunkArr.toArray(lchunk);
        chunkArr.clear();

        return lchunk;
    }

}
