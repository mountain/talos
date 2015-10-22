package org.talos.vec;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.talos.Context;
import org.talos.Exception;
import org.talos.vec.event.BasisListener;
import org.talos.vec.event.ExecutorListener;
import org.talos.vec.event.RecommendationListener;
import org.talos.vec.event.VectorSetListener;
import org.talos.vec.store.Basis;
import org.talos.vec.store.CosineScore;
import org.talos.vec.store.Recommendation;
import org.talos.vec.store.SerializerHelper;
import org.talos.vec.store.VectorSet;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class Executor {

    private Context                       context;
    private Basis                         base;
    private Map<String, VectorSet>        vectorSets      = new HashMap<String, VectorSet>();
    private Map<String, Recommendation>   recommendations = new HashMap<String, Recommendation>();
    private List<ExecutorListener>        listeners       = new ArrayList<ExecutorListener>();

    private ThreadLocal<SerializerHelper> helper          = new ThreadLocal<SerializerHelper>() {
                                                              @Override
                                                              protected SerializerHelper initialValue() {
                                                                  return new SerializerHelper();
                                                              }
                                                          };

    public Executor(Context context, Basis base) {
        this.context = context;
        this.base = base;
    }

    private String rkey(String vkeySource, String vkeyTarget) {
        return new StringBuilder().append(vkeySource).append("_").append(vkeyTarget).toString();
    }

    public String key() {
        return this.base.key();
    }

    public void bsave(String filepath) {
        Output output = null;
        try {
            for (String vkey : vectorSets.keySet()) {
                vectorSets.get(vkey).clean();
            }

            output = new Output(new FileOutputStream(filepath));
            SerializerHelper serializerHelper = helper.get();
            serializerHelper.writeB(output, this.base);
            serializerHelper.writeVectorSets(output, this.vectorSets);
            serializerHelper.writeRecommendations(output, this.recommendations);
        } catch (Throwable e) {
            throw new Exception(e);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    public void bload(String filepath) {
        Input input = null;
        try {
            input = new Input(new FileInputStream(filepath));
            SerializerHelper serializerHelper = helper.get();
            Basis base = serializerHelper.readB(input);
            Map<String, VectorSet> vecSets = serializerHelper.readVectorSets(input, base);
            Map<String, Recommendation> recs = serializerHelper.readRecommendations(input, vecSets);

            this.base = base;
            this.vectorSets = vecSets;
            this.recommendations = recs;

            for (String vkey : vecSets.keySet()) {
                for (ExecutorListener listener : listeners) {
                    listener.onVecSetAdded(key(), vkey);
                }
            }
            for (String key : recs.keySet()) {
                Recommendation rec = recs.get(key);
                String vkeySrc = rec.source.key();
                String vkeyTgt = rec.target.key();
                for (ExecutorListener listener : listeners) {
                    listener.onRecAdded(key(), vkeySrc, vkeyTgt);
                }
            }

        } catch (Throwable e) {
            throw new Exception(e);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    public String[] bget() {
        return this.base.get();
    }

    public void brev(String[] base) {
        this.base.revise(base);
    }

    public void vmk(String vkey) {
        String type = context.getString("vectorSetType");
        Context subcontext = context.getSub(type, vkey);
        float accumuFactor = subcontext.getFloat("accumuFactor");
        int sparseFactor = subcontext.getInt("sparseFactor");

        this.vectorSets.put(vkey, new VectorSet(vkey, this.base, accumuFactor, sparseFactor));
    }

    public void vdel(String vkey) {
        this.vectorSets.remove(vkey);
    }

    public int vlen(String vkey) {
        return this.vectorSets.get(vkey).size();
    }

    public long[] vids(String vkey) {
        return this.vectorSets.get(vkey).ids();
    }

    public float[] vget(String vkey, long vecid) {
        return this.vectorSets.get(vkey).get(vecid);
    }

    public void vadd(String vkey, long vecid, float[] distr) {
        this.vectorSets.get(vkey).add(vecid, distr);
    }

    public void vset(String vkey, long vecid, float[] distr) {
        this.vectorSets.get(vkey).set(vecid, distr);
    }

    public void vacc(String vkey, long vecid, float[] distr) {
        this.vectorSets.get(vkey).accumulate(vecid, distr);
    }

    public void vrem(String vkey, long vecid) {
        this.vectorSets.get(vkey).remove(vecid);
    }

    public int[] iget(String vkey, long vecid) {
        return this.vectorSets.get(vkey)._get(vecid);
    }

    public void iadd(String vkey, long vecid, int[] pairs) {
        this.vectorSets.get(vkey)._add(vecid, pairs);
    }

    public void iset(String vkey, long vecid, int[] pairs) {
        this.vectorSets.get(vkey)._set(vecid, pairs);
    }

    public void iacc(String vkey, long vecid, int[] pairs) {
        this.vectorSets.get(vkey)._accumulate(vecid, pairs);
    }

    public void rmk(String vkeySource, String vkeyTarget) {
        CosineScore scoring = new CosineScore();

        VectorSet source = vectorSets.get(vkeySource);
        VectorSet target = vectorSets.get(vkeyTarget);
        Recommendation rec = new Recommendation(source, target, scoring, context.getInt("maxlimits"));

        String rkey = rkey(vkeySource, vkeyTarget);
        this.recommendations.put(rkey, rec);

        source.addListener(rec);
        if (target != source) {
            target.addListener(rec);
        }

        for (long srcVecId : source.ids()) {
            rec.create(srcVecId);
            int[] vector = source._get(srcVecId);
            target.rescore(source.key(), srcVecId, vector, rec);
        }
    }

    public void rdel(String vkey) {
        this.recommendations.remove(vkey);
    }

    public String[] rget(String vkeySource, long vecid, String vkeyTarget) {
        return this.recommendations.get(rkey(vkeySource, vkeyTarget)).get(vecid);
    }

    public long[] rrec(String vkeySource, long vecid, String vkeyTarget) {
        return this.recommendations.get(rkey(vkeySource, vkeyTarget)).rec(vecid);
    }

    public void addListener(ExecutorListener listener) {
        listeners.add(listener);
    }

    public void addListener(BasisListener listener) {
        base.addListener(listener);
    }

    public void addListener(String vkey, VectorSetListener listener) {
        vectorSets.get(vkey).addListener(listener);
    }

    public void addListener(String srcVkey, String tgtVkey, RecommendationListener listener) {
        recommendations.get(rkey(srcVkey, tgtVkey)).addListener(listener);
    }

}
