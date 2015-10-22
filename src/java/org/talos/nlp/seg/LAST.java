package org.talos.nlp.seg;

import org.talos.nlp.Chunk;
import org.talos.util.IFn;

public class LAST implements IFn<Chunk[], Chunk[]> {

    @Override
    public Chunk[] call(Chunk[] chunks) {
        return new Chunk[]{chunks[0]};
    }

}
