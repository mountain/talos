package org.talos.vec.event;

import org.talos.vec.store.Basis;

public interface BasisListener {

    public void onBasisRevised(Basis evtSrc, String[] oldSchema, String[] newSchema);

}
