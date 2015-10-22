package org.talos.vec;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talos.Context;
import org.talos.Exception;
import org.talos.util.PrefixThreadFactory;
import org.talos.vec.event.BasisListener;
import org.talos.vec.event.ExecutorListener;
import org.talos.vec.event.RecommendationListener;
import org.talos.vec.event.VectorSetListener;
import org.talos.vec.store.Basis;

public class Engine implements ExecutorListener {

    private static final String version = "0.1.0";

    enum Kind {
        BASIS, VECTORS, RECOMM
    };

    private static final Logger logger         = LoggerFactory.getLogger(Engine.class);

    private final AtomicInteger commandCounter = new AtomicInteger();

    public abstract class AsyncSafeRunner implements Runnable {
        String scope;

        public AsyncSafeRunner(String scope) {
            this.scope = scope;
        }

        public abstract void invoke();

        public void run() {
            try {
                invoke();
                commandCounter.incrementAndGet();
            } catch (Throwable ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    public abstract class SafeRunner implements Runnable {
        Callback callback;
        String   scope;

        public SafeRunner(String scope, Callback callback) {
            this.scope = scope;
            this.callback = callback;
        }

        public abstract void invoke();

        public void run() {
            try {
                invoke();
                commandCounter.incrementAndGet();
            } catch (Throwable ex) {
                String errMsg = ex.getMessage();
                logger.error(errMsg, ex);
                callback.error(errMsg);
            } finally {
                callback.response();
            }
        }
    }

    public class RejectedHandler implements RejectedExecutionHandler {

        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            logger.error("server reject request");
        }

    }

    private Context                            context;

    private final ExecutorService              mngmExec    = Executors.newSingleThreadExecutor();
    private final Map<String, Kind>            kindOf      = new HashMap<String, Kind>();
    private final Map<String, String>          basisOf     = new HashMap<String, String>();
    private final Map<String, List<String>>    vectorsOf   = new HashMap<String, List<String>>();
    private final Map<String, Set<String>>     rtargetsOf  = new HashMap<String, Set<String>>();
    private final Map<String, Executor>        bases       = new HashMap<String, Executor>();

    private final Map<String, ExecutorService> writerExecs = new HashMap<String, ExecutorService>();
    private final ThreadPoolExecutor           readerPool  = new ThreadPoolExecutor(53, 83, 37, TimeUnit.SECONDS,
                                                                   new ArrayBlockingQueue<Runnable>(100),
                                                                   new PrefixThreadFactory("reader"),
                                                                   new RejectedHandler());

    private final Map<String, Integer>         counters    = new HashMap<String, Integer>();
    private final int                          bycount;
    private final String                       savePath;
    private final long                         startTime   = new Date().getTime();

    public Engine(Context simContext) {
        String separator = System.getProperty("file.separator");
        this.context = simContext;
        this.bycount = simContext.getInt("bycount");
        this.savePath = new StringBuilder(System.getProperty("user.dir")).append(separator)
                .append(context.getString("savepath")).append(separator).toString();
        this.load(null);
        this.startCron();
    }

    private void validatePath(String filePath) throws Exception {
        if (!new File(filePath).exists()) {
            throw new Exception(String.format("Dmp file '%s' not exists", filePath));
        }
    }

    private void validateKeyFormat(String key) throws Exception {
        if (key.indexOf('_') > -1) {
            throw new Exception(String.format("Invalid key format '%s'", key));
        }
    }

    private void validateExistence(String toCheck) throws Exception {
        if (!basisOf.containsKey(toCheck)) {
            throw new Exception(String.format("Unknown data entry '%s'", toCheck));
        }
    }

    private void validateNotExistence(String toCheck) throws Exception {
        if (basisOf.containsKey(toCheck)) {
            throw new Exception(String.format("Data entry '%s' already exists", toCheck));
        }
    }

    private void validateKind(String op, String toCheck, Kind kindShouldBe) throws Exception {
        if (!kindOf.containsKey(toCheck) || !kindShouldBe.equals(kindOf.get(toCheck))) {
            throw new Exception(String.format("Operation '%s' against a non-%s type '%s'", op, kindShouldBe, toCheck));
        }
    }

    private void validateId(long toCheck) throws Exception {
        if (toCheck < 1) {
            throw new Exception(String.format("Inviad id '%d', should be positive integer", toCheck));
        }
    }

    private void validatePairs(int maxIndex, int[] toCheck) throws Exception {
        int len = toCheck.length;
        if (len % 2 != 0) {
            throw new Exception("Sparse vector should be paired");
        }
        for (int offset = 0; offset < len; offset += 2) {
            if (toCheck[offset] < 0 || toCheck[offset] > maxIndex) {
                throw new Exception(String.format("Sparse matrix index '%d' out of bound", toCheck[offset]));
            }
            if (toCheck[offset + 1] < 0) {
                throw new Exception(String.format("Sparse matrix value '%d' should be non-negative",
                        toCheck[offset + 1]));
            }
        }
    }

    private void validateSameBasis(String vkeySource, String vkeyTarget) {
        if (!basisOf.get(vkeySource).equals(basisOf.get(vkeyTarget))) {
            throw new Exception(String.format("Recommedation[%s_%s] must be between two vector set with same basis",
                    vkeySource, vkeyTarget));
        }
    }

    private String rkey(String vkeySource, String vkeyTarget) {
        return new StringBuilder().append(vkeySource).append("_").append(vkeyTarget).toString();
    }

    public void startCron() {
        final int saveInterval = this.context.getInt("saveinterval");

        Timer cron = new Timer();

        TimerTask savetask = new TimerTask() {
            public void run() {
                save(null);
            }
        };
        cron.schedule(savetask, saveInterval, saveInterval);
    }

    public void info(final Callback callback) {
        long curTime = new Date().getTime();
        final long disTime = curTime - startTime;

        Runtime runtime = Runtime.getRuntime();
        long allocatedMemory = runtime.totalMemory();
        final String usedMemory = String.valueOf(allocatedMemory);

        List<String> bkeys = new ArrayList<String>(bases.keySet());
        int tempCount = 0;
        for (String bkey : bkeys) {

            List<String> vkeys = vectorsOf.get(bkey);
            if (vkeys != null) {
                for (String vkey : vkeys) {
                    tempCount += bases.get(bkey).vlen(vkey);
                }
            }
        }
        final int keyCount = tempCount;

        mngmExec.execute(new SafeRunner("info", callback) {

            public void invoke() {
                String infos = String.format(
                        "version:%s\nuptime:%s\nused_memory:%s\nvectorKeys:%s\ntotal_command_processed:%s\n", version,
                        disTime / 1000, usedMemory, keyCount, commandCounter.get());
                callback.stringValue(infos);
            }
        });
    }

    public void load(final Callback callback) {
        File[] files = new File(savePath).listFiles();
        if (files != null) {
            for (File file : files) {
                String filename = file.getName();
                if (file.isFile() && filename.endsWith("dmp")) {
                    bload(null, filename.replaceFirst("[.][^.]+$", ""));
                }
            }
        }

        if (callback != null) {
            callback.ok();
            callback.response();
        }
    }

    public void save(final Callback callback) {
        for (String bkey : bases.keySet()) {
            bsave(null, bkey);
        }

        if (callback != null) {
            callback.ok();
            callback.response();
        }
    }

    public void del(final Callback callback, final String key) {
        validateExistence(key);
        writerExecs.get(basisOf.get(key)).execute(new AsyncSafeRunner("del") {

            public void invoke() {
                delete(key);
            }

            public void delete(String toDel) {
                Kind kind = kindOf.get(toDel);
                switch (kind) {
                case BASIS:
                    if (vectorsOf.containsKey(toDel)) {
                        for (String vec : vectorsOf.get(toDel)) {
                            for (String source : rtargetsOf.keySet()) {
                                if (source.equals(vec)) {
                                    for (String target : rtargetsOf.get(vec)) {
                                        String recKey = rkey(vec, target);
                                        basisOf.remove(recKey);
                                        kindOf.remove(recKey);
                                    }
                                    rtargetsOf.remove(vec);
                                } else {
                                    rtargetsOf.get(source).remove(vec);
                                }
                            }
                            kindOf.remove(vec);
                            basisOf.remove(vec);
                        }
                    }
                    vectorsOf.remove(toDel);
                    kindOf.remove(toDel);
                    basisOf.remove(toDel);
                    bases.remove(toDel);
                    writerExecs.remove(toDel);
                    logger.info(String.format("basis[%s] deleted", toDel));
                    break;
                case VECTORS:
                    String bkey = basisOf.get(toDel);
                    for (String source : rtargetsOf.keySet()) {
                        if (source.equals(toDel)) {
                            for (String target : rtargetsOf.get(toDel)) {
                                delete(rkey(toDel, target));
                            }
                            rtargetsOf.remove(toDel);
                        } else {
                            rtargetsOf.get(source).remove(toDel);
                        }
                    }
                    vectorsOf.get(bkey).remove(toDel);
                    basisOf.remove(toDel);
                    kindOf.remove(toDel);
                    bases.get(bkey).vdel(toDel);
                    logger.info(String.format("vectorset[%s] deleted", toDel));
                    break;
                case RECOMM:
                    bases.get(basisOf.get(toDel)).rdel(toDel);
                    basisOf.remove(toDel);
                    kindOf.remove(toDel);
                    logger.info(String.format("recommendation[%s] deleted", toDel));
                    break;
                }
            }
        });

        if (callback != null) {
            callback.ok();
            callback.response();
        }
    }

    public void bload(final Callback callback, final String bkey) {
        mngmExec.execute(new AsyncSafeRunner("bload") {

            public void invoke() {
                validateKeyFormat(bkey);
                String filePath = new StringBuilder(savePath).append(bkey).append(".dmp").toString();
                validatePath(filePath);
                if (basisOf.containsKey(bkey)) {
                    del(null, bkey);
                }

                logger.info(String.format("loading basis[%s]", bkey));

                Basis basis = new Basis(bkey);
                Executor simBasis = new Executor(context.getSub("basis", bkey), basis);
                bases.put(bkey, simBasis);
                basisOf.put(bkey, bkey);
                kindOf.put(bkey, Kind.BASIS);
                simBasis.addListener(Engine.this);

                simBasis.bload(filePath);

                writerExecs.put(bkey, Executors.newSingleThreadExecutor());

                logger.info(String.format("basis[%s] loaded", bkey));
            }
        });

        if (callback != null) {
            callback.ok();
            callback.response();
        }
    }

    public void bsave(final Callback callback, final String bkey) {
        writerExecs.get(bkey).execute(new AsyncSafeRunner("bsave") {

            public void invoke() {
                validateKind("bsave", bkey, Kind.BASIS);

                logger.info(String.format("saving basis[%s]", bkey));

                bases.get(bkey).bsave(new StringBuilder(savePath).append(bkey).append(".dmp").toString());

                logger.info(String.format("basis[%s] saved", bkey));
            }
        });

        if (callback != null) {
            callback.ok();
            callback.response();
        }
    }

    public void blist(final Callback callback) {
        mngmExec.execute(new SafeRunner("blist", callback) {

            public void invoke() {
                List<String> bkeys = new ArrayList<String>(bases.keySet());
                Collections.sort(bkeys);
                callback.stringList((String[]) bkeys.toArray(new String[bkeys.size()]));
            }
        });
    }

    public void bmk(final Callback callback, final String bkey, final String[] base) {
        mngmExec.execute(new SafeRunner("bmk", callback) {

            public void invoke() {
                validateKeyFormat(bkey);
                validateNotExistence(bkey);
                Basis basis = new Basis(bkey, base);
                Executor simbasis = new Executor(context.getSub("basis", bkey), basis);
                bases.put(bkey, simbasis);
                basisOf.put(bkey, bkey);
                kindOf.put(bkey, Kind.BASIS);
                simbasis.addListener(Engine.this);

                writerExecs.put(bkey, Executors.newSingleThreadExecutor());

                logger.info(String.format("basis[%s] created", bkey));

                callback.ok();
            }
        });
    }

    public void brev(final Callback callback, final String bkey, final String[] base) {
        writerExecs.get(bkey).execute(new SafeRunner("brev", callback) {

            public void invoke() {
                validateKind("brev", bkey, Kind.BASIS);
                validateKeyFormat(bkey);
                bases.get(bkey).brev(base);

                logger.info(String.format("basis[%s] revised", bkey));

                callback.ok();
            }
        });
    }

    public void bget(final Callback callback, final String bkey) {
        validateKind("bget", bkey, Kind.BASIS);
        readerPool.submit(new SafeRunner("bget", callback) {

            public void invoke() {
                callback.stringList(bases.get(bkey).bget());
            }
        });
    }

    public void vlist(final Callback callback, final String bkey) {
        mngmExec.execute(new SafeRunner("vlist", callback) {

            public void invoke() {
                validateKind("vlist", bkey, Kind.BASIS);
                List<String> vkeys = vectorsOf.get(bkey);
                if (vkeys == null) {
                    vkeys = new ArrayList<String>();
                } else {
                    Collections.sort(vkeys);
                }
                int i = 0;
                String[] result = new String[vkeys.size()];
                for (String key : vkeys) {
                    result[i++] = key;
                }
                callback.stringList(result);
            }
        });
    }

    public void vmk(final Callback callback, final String bkey, final String vkey) {
        mngmExec.execute(new SafeRunner("vmk", callback) {

            public void invoke() {
                validateKind("vmk", bkey, Kind.BASIS);
                validateKeyFormat(vkey);
                validateNotExistence(vkey);
                bases.get(bkey).vmk(vkey);

                kindOf.put(vkey, Kind.VECTORS);
                basisOf.put(vkey, bkey);
                List<String> vkeys = vectorsOf.get(bkey);
                if (vkeys == null) {
                    vkeys = new ArrayList<String>();
                    vectorsOf.put(bkey, vkeys);
                }
                vkeys.add(vkey);

                logger.info(String.format("vectorset[%s] created under basis[%s]", vkey, bkey));

                callback.ok();
            }
        });
    }

    public void vlen(final Callback callback, final String vkey) {
        validateKind("vget", vkey, Kind.VECTORS);
        final String bkey = basisOf.get(vkey);
        readerPool.submit(new SafeRunner("vids", callback) {

            public void invoke() {
                callback.integerValue(bases.get(bkey).vlen(vkey));
            }
        });
    }

    public void vids(final Callback callback, final String vkey) {
        validateKind("vget", vkey, Kind.VECTORS);
        final String bkey = basisOf.get(vkey);
        readerPool.submit(new SafeRunner("vids", callback) {

            public void invoke() {
                callback.longList(bases.get(bkey).vids(vkey));
            }
        });
    }

    // CURD operations for one vector in vector-set

    public void vget(final Callback callback, final String vkey, final long vecid) {
        validateKind("vget", vkey, Kind.VECTORS);
        final String bkey = basisOf.get(vkey);
        readerPool.submit(new SafeRunner("vget", callback) {

            public void invoke() {
                callback.floatList(bases.get(bkey).vget(vkey, vecid));
            }
        });
    }

    public void vadd(final Callback callback, final String vkey, final long vecid, final float[] vector) {
        validateKind("vadd", vkey, Kind.VECTORS);
        validateId(vecid);
        final String bkey = basisOf.get(vkey);
        writerExecs.get(bkey).execute(new AsyncSafeRunner("vadd") {

            public void invoke() {
                bases.get(bkey).vadd(vkey, vecid, vector);

                if (!counters.containsKey(vkey)) {
                    counters.put(vkey, 0);
                }
                int counter = counters.get(vkey) + 1;
                counters.put(vkey, counter);
                if (counter % bycount == 0) {
                    logger.info(String.format("adding dense vectors %d to %s", counter, vkey));
                }
                logger.info(String.format("vector[%s] added to vectorset[%s]", vecid, vkey));
            }
        });

        callback.ok();
        callback.response();
    }

    public void vset(final Callback callback, final String vkey, final long vecid, final float[] vector) {
        validateKind("vset", vkey, Kind.VECTORS);
        validateId(vecid);
        final String bkey = basisOf.get(vkey);
        writerExecs.get(bkey).execute(new AsyncSafeRunner("vset") {

            public void invoke() {
                bases.get(bkey).vset(vkey, vecid, vector);

                if (!counters.containsKey(vkey)) {
                    counters.put(vkey, 0);
                }
                int counter = counters.get(vkey) + 1;
                counters.put(vkey, counter);
                if (counter % bycount == 0) {
                    logger.info(String.format("setting dense vectors %d to %s", counter, vkey));
                }
                logger.info(String.format("vector[%s] setted to vectorset[%s]", vecid, vkey));
            }
        });

        callback.ok();
        callback.response();
    }

    public void vacc(final Callback callback, final String vkey, final long vecid, final float[] vector) {
        validateKind("vacc", vkey, Kind.VECTORS);
        validateId(vecid);
        final String bkey = basisOf.get(vkey);
        writerExecs.get(bkey).execute(new AsyncSafeRunner("vacc") {

            public void invoke() {
                bases.get(bkey).vacc(vkey, vecid, vector);

                if (!counters.containsKey(vkey)) {
                    counters.put(vkey, 0);
                }
                int counter = counters.get(vkey) + 1;
                counters.put(vkey, counter);
                if (counter % bycount == 0) {
                    logger.info(String.format("acculmulating dense vectors %d to %s", counter, vkey));
                }
                logger.info(String.format("vector[%s] accumulated to vectorset[%s]", vecid, vkey));
            }
        });

        callback.ok();
        callback.response();
    }

    public void vrem(final Callback callback, final String vkey, final long vecid) {
        this.validateKind("vrem", vkey, Kind.VECTORS);
        final String bkey = basisOf.get(vkey);
        writerExecs.get(bkey).execute(new AsyncSafeRunner("vrem") {

            public void invoke() {
                bases.get(bkey).vrem(vkey, vecid);

                logger.info(String.format("vector[%s] removed from vectorset[%s]", vecid, vkey));
            }
        });

        callback.ok();
        callback.response();
    }

    // Internal use for client-side sparsification

    public void iget(final Callback callback, final String vkey, final long vecid) {
        validateExistence(vkey);
        final String bkey = basisOf.get(vkey);
        readerPool.submit(new SafeRunner("iget", callback) {

            public void invoke() {
                callback.integerList(bases.get(bkey).iget(vkey, vecid));
            }
        });
    }

    public void iadd(Callback callback, final String vkey, final long vecid, final int[] pairs) {
        validateKind("iadd", vkey, Kind.VECTORS);
        validateId(vecid);
        final String bkey = basisOf.get(vkey);
        int maxIndex = bases.get(bkey).bget().length;
        validatePairs(maxIndex, pairs);
        writerExecs.get(bkey).execute(new AsyncSafeRunner("iadd") {

            public void invoke() {
                bases.get(bkey).iadd(vkey, vecid, pairs);

                if (!counters.containsKey(vkey)) {
                    counters.put(vkey, 0);
                }
                int counter = counters.get(vkey) + 1;
                counters.put(vkey, counter);
                if (counter % bycount == 0) {
                    logger.info(String.format("adding sparse vectors %d to %s", counter, vkey));
                }
                logger.info(String.format("sparse vector[%s] added to vectorset[%s]", vecid, vkey));
            }
        });

        callback.ok();
        callback.response();
    }

    public void iset(final Callback callback, final String vkey, final long vecid, final int[] pairs) {
        validateKind("iset", vkey, Kind.VECTORS);
        validateId(vecid);
        final String bkey = basisOf.get(vkey);
        int maxIndex = bases.get(bkey).bget().length;
        validatePairs(maxIndex, pairs);
        writerExecs.get(bkey).execute(new AsyncSafeRunner("iset") {

            public void invoke() {
                bases.get(bkey).iset(vkey, vecid, pairs);

                if (!counters.containsKey(vkey)) {
                    counters.put(vkey, 0);
                }
                int counter = counters.get(vkey) + 1;
                counters.put(vkey, counter);
                if (counter % bycount == 0) {
                    logger.info(String.format("setting sparse vectors %d to %s", counter, vkey));
                }
                logger.info(String.format("sparse vector[%s] setted to vectorset[%s]", vecid, vkey));
            }
        });

        callback.ok();
        callback.response();
    }

    public void iacc(final Callback callback, final String vkey, final long vecid, final int[] pairs) {
        this.validateKind("iacc", vkey, Kind.VECTORS);
        validateId(vecid);
        final String bkey = basisOf.get(vkey);
        int maxIndex = bases.get(bkey).bget().length;
        validatePairs(maxIndex, pairs);
        writerExecs.get(bkey).execute(new AsyncSafeRunner("iacc") {

            public void invoke() {
                bases.get(bkey).iacc(vkey, vecid, pairs);

                if (!counters.containsKey(vkey)) {
                    counters.put(vkey, 0);
                }
                int counter = counters.get(vkey) + 1;
                counters.put(vkey, counter);
                if (counter % bycount == 0) {
                    logger.info(String.format("accumulating sparse vectors %d to %s", counter, vkey));
                }
                logger.info(String.format("sparse vector[%s] accumulated to vectorset[%s]", vecid, vkey));
            }
        });

        callback.ok();
        callback.response();
    }

    public void rlist(final Callback callback, final String vkey) {
        mngmExec.execute(new SafeRunner("rlist", callback) {

            public void invoke() {
                validateKind("rlist", vkey, Kind.VECTORS);
                List<String> targets = new ArrayList<String>();
                Set<String> tgtSet = rtargetsOf.get(vkey);
                if (tgtSet != null) {
                    targets.addAll(tgtSet);
                    Collections.sort(targets);
                }
                callback.stringList((String[]) targets.toArray(new String[targets.size()]));
            }
        });
    }

    public void rmk(final Callback callback, final String vkeySource, final String vkeyTarget) {
        validateSameBasis(vkeySource, vkeyTarget);
        final String bkey = basisOf.get(vkeySource);
        writerExecs.get(bkey).execute(new SafeRunner("rmk", callback) {

            public void invoke() {
                validateKind("rmk", vkeySource, Kind.VECTORS);
                validateKind("rmk", vkeyTarget, Kind.VECTORS);
                validateSameBasis(vkeyTarget, vkeySource);
                String rkey = rkey(vkeySource, vkeyTarget);
                validateNotExistence(rkey);

                logger.info(String.format("creating recommendation[%s_%s]", vkeySource, vkeyTarget));

                final String bkey = basisOf.get(vkeySource);
                bases.get(bkey).rmk(vkeySource, vkeyTarget);
                basisOf.put(rkey, basisOf.get(vkeySource));
                kindOf.put(rkey, Kind.RECOMM);

                if (rtargetsOf.get(vkeySource) == null) {
                    rtargetsOf.put(vkeySource, new HashSet<String>());
                }
                rtargetsOf.get(vkeySource).add(vkeyTarget);

                logger.info(String.format("recommendation[%s_%s] created", vkeySource, vkeyTarget));

                callback.ok();
            }
        });
    }

    public void rget(final Callback callback, final String vkeySource, final long vecid, final String vkeyTarget) {
        validateKind("rget", vkeySource, Kind.VECTORS);
        validateKind("rget", vkeyTarget, Kind.VECTORS);
        String rkey = rkey(vkeySource, vkeyTarget);
        validateExistence(rkey);
        final String bkey = basisOf.get(vkeySource);
        readerPool.submit(new SafeRunner("rget", callback) {

            public void invoke() {
                callback.stringList(bases.get(bkey).rget(vkeySource, vecid, vkeyTarget));
            }
        });
    }

    public void rrec(final Callback callback, final String vkeySource, final long vecid, final String vkeyTarget) {
        validateKind("rget", vkeySource, Kind.VECTORS);
        validateKind("rget", vkeyTarget, Kind.VECTORS);
        String rkey = rkey(vkeySource, vkeyTarget);
        validateExistence(rkey);
        final String bkey = basisOf.get(vkeySource);
        readerPool.submit(new SafeRunner("rrec", callback) {

            public void invoke() {
                callback.longList(bases.get(bkey).rrec(vkeySource, vecid, vkeyTarget));
            }
        });
    }

    public void xacc(Callback callback, final String vkeyTarget, final long vecidTarget, final String vkeyOperand,
            final long vecidOperand) {
        validateKind("xacc", vkeyTarget, Kind.VECTORS);
        validateId(vecidTarget);
        validateKind("xacc", vkeyOperand, Kind.VECTORS);
        validateId(vecidOperand);
        final String bkey = basisOf.get(vkeyTarget);
        writerExecs.get(bkey).execute(new AsyncSafeRunner("xacc") {

            public void invoke() {
                Executor base = bases.get(bkey);
                float[] vector = base.vget(vkeyOperand, vecidOperand);
                base.vacc(vkeyTarget, vecidTarget, vector);

                if (!counters.containsKey(vkeyTarget)) {
                    counters.put(vkeyTarget, 0);
                }
                int counter = counters.get(vkeyTarget) + 1;
                counters.put(vkeyTarget, counter);
                if (counter % bycount == 0) {
                    logger.info(String.format("acculmulating dense vectors %d to %s", counter, vkeyTarget));
                }
            }
        });

        callback.ok();
        callback.response();
    }

    public void xprd(final Callback callback, final String vkeyTarget, final long vecidTarget,
            final String vkeyOperand, final long[] vecidOperands) {
        validateKind("xprd", vkeyTarget, Kind.VECTORS);
        validateId(vecidTarget);
        validateKind("xprd", vkeyOperand, Kind.VECTORS);
        for (long vecidOperand : vecidOperands) {
            validateId(vecidOperand);
        }
        final String bkey = basisOf.get(vkeyTarget);
        writerExecs.get(bkey).execute(new AsyncSafeRunner("xprd") {

            public void invoke() {
                Executor base = bases.get(bkey);

                int size = vecidOperands.length;
                float[] scores = new float[size];
                float[] target = base.vget(vkeyTarget, vecidTarget);
                for (int i = 0; i < size; i++) {
                    long vecidOperand = vecidOperands[i];
                    float[] operand = base.vget(vkeyOperand, vecidOperand);

                    int len = target.length;
                    float score = 0f, lensq1 = 0f, lensq2 = 0f;
                    for (int j = 0; j < len; j++) {
                        score += target[j] * operand[j];
                    }
                    for (int j = 0; j < len; j++) {
                        lensq1 += target[j] * target[j];
                    }
                    for (int j = 0; j < len; j++) {
                        lensq2 += operand[j] * operand[j];
                    }

                    if (lensq1 > 0 && lensq2 > 0) {
                        scores[i] = (float) (score / Math.sqrt(lensq1) / Math.sqrt(lensq2));
                    } else {
                        scores[i] = 0f;
                    }
                }

                callback.floatList(scores);
                callback.response();
            }
        });
    }

    public void listen(final String bkey, final BasisListener listener) {
        writerExecs.get(bkey).execute(new Runnable() {

            public void run() {
                bases.get(bkey).addListener(listener);
            }
        });
    }

    public void listen(final String vkey, final VectorSetListener listener) {
        final String bkey = basisOf.get(vkey);
        writerExecs.get(bkey).execute(new Runnable() {

            public void run() {
                bases.get(bkey).addListener(vkey, listener);
            }
        });
    }

    public void listen(final String srcVkey, final String tgtVkey, final RecommendationListener listener) {
        final String bkey = basisOf.get(srcVkey);
        writerExecs.get(bkey).execute(new Runnable() {

            public void run() {
                bases.get(bkey).addListener(srcVkey, tgtVkey, listener);
            }
        });
    }

    public void onVecSetAdded(String bkeySrc, String vkey) {
        basisOf.put(vkey, bkeySrc);

        List<String> vecs = vectorsOf.get(bkeySrc);
        if (vecs == null) {
            vecs = new ArrayList<String>();
            vectorsOf.put(bkeySrc, vecs);
        }
        vecs.add(vkey);

        kindOf.put(vkey, Kind.VECTORS);
    }

    public void onVecSetDeleted(String bkeySrc, String vkey) {
        basisOf.remove(vkey);
        vectorsOf.get(bkeySrc).remove(vkey);
        kindOf.remove(vkey);
    }

    public void onRecAdded(String bkeySrc, String vkeyFrom, String vkeyTo) {
        String rkey = rkey(vkeyFrom, vkeyTo);

        basisOf.put(rkey, bkeySrc);

        Set<String> tgt = rtargetsOf.get(vkeyFrom);
        if (tgt == null) {
            tgt = new HashSet<String>();
            rtargetsOf.put(vkeyFrom, tgt);
        }
        tgt.add(vkeyTo);

        kindOf.put(rkey, Kind.RECOMM);
    }

    public void onRecDeleted(String bkeySrc, String vkeyFrom, String vkeyTo) {
        String rkey = rkey(vkeyFrom, vkeyTo);
        basisOf.remove(rkey);
        rtargetsOf.get(vkeyFrom).remove(vkeyTo);
        kindOf.remove(rkey);
    }

}
