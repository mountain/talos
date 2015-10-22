package org.talos.nlp.seg;

import org.talos.nlp.Chunk;
import org.talos.util.IFn;

import java.util.ArrayList;

public class LSWMF implements IFn<Chunk[], Chunk[]> {

    @Override
    public Chunk[] call(Chunk[] chunks) {

        double largestFreedom = chunks[0].swmf();
        int j;

        // find the maximum sum of single morphemic freedom
        for (j = 1; j < chunks.length; j++) {
            if (chunks[j].swmf() > largestFreedom)
                largestFreedom = chunks[j].swmf();
        }

        // get the items that the word length equals to
        // the max's length.
        ArrayList<Chunk> chunkArr = new ArrayList<Chunk>(chunks.length);
        for (j = 0; j < chunks.length; j++) {
            if (chunks[j].swmf() == largestFreedom)
                chunkArr.add(chunks[j]);
        }

        Chunk[] lchunk = new Chunk[chunkArr.size()];
        chunkArr.toArray(lchunk);
        chunkArr.clear();

        return lchunk;
    }

}
