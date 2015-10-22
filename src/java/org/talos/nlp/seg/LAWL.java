package org.talos.nlp.seg;

import java.util.ArrayList;

import org.talos.nlp.Chunk;
import org.talos.util.IFn;

public class LAWL implements IFn<Chunk[], Chunk[]> {

    @Override
    public Chunk[] call(Chunk[] chunks) {

        double largetAverage = chunks[0].awl();
        int j;

        // find the largest average word length
        for (j = 1; j < chunks.length; j++) {
            if (chunks[j].awl() > largetAverage)
                largetAverage = chunks[j].awl();
        }

        // get the items that the average word length equals to
        // the max's.
        ArrayList<Chunk> chunkArr = new ArrayList<Chunk>(chunks.length);
        for (j = 0; j < chunks.length; j++) {
            if (chunks[j].awl() == largetAverage)
                chunkArr.add(chunks[j]);
        }

        Chunk[] lchunk = new Chunk[chunkArr.size()];
        chunkArr.toArray(lchunk);
        chunkArr.clear();

        return lchunk;
    }

}
