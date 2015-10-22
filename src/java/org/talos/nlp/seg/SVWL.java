package org.talos.nlp.seg;

import org.talos.nlp.Chunk;
import org.talos.util.IFn;

import java.util.ArrayList;

public class SVWL implements IFn<Chunk[], Chunk[]> {

    @Override
    public Chunk[] call(Chunk[] chunks) {

        double smallestVariance = chunks[0].wv();
        int j;

        // find the smallest variance word length
        for (j = 1; j < chunks.length; j++) {
            if (chunks[j].wv() < smallestVariance)
                smallestVariance = chunks[j].wv();
        }

        // get the items that the variance word length equals to
        // the max's.
        ArrayList<Chunk> chunkArr = new ArrayList<Chunk>(chunks.length);
        for (j = 0; j < chunks.length; j++) {
            if (chunks[j].wv() == smallestVariance)
                chunkArr.add(chunks[j]);
        }

        Chunk[] lchunk = new Chunk[chunkArr.size()];
        chunkArr.toArray(lchunk);
        chunkArr.clear();

        return lchunk;
    }

}
