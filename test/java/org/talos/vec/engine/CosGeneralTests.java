package org.talos.vec.engine;

import static org.talos.vec.TestEngine.blist;
import static org.talos.vec.TestEngine.bload;
import static org.talos.vec.TestEngine.bmk;
import static org.talos.vec.TestEngine.brev;
import static org.talos.vec.TestEngine.bsave;
import static org.talos.vec.TestEngine.del;
import static org.talos.vec.TestEngine.execCmd;
import static org.talos.vec.TestEngine.floatList;
import static org.talos.vec.TestEngine.longList;
import static org.talos.vec.TestEngine.ok;
import static org.talos.vec.TestEngine.rlist;
import static org.talos.vec.TestEngine.rmk;
import static org.talos.vec.TestEngine.rrec;
import static org.talos.vec.TestEngine.stringList;
import static org.talos.vec.TestEngine.vadd;
import static org.talos.vec.TestEngine.vget;
import static org.talos.vec.TestEngine.vlist;
import static org.talos.vec.TestEngine.vmk;
import static org.talos.vec.TestEngine.vrem;
import static org.talos.vec.TestEngine.vset;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.talos.Config;
import org.talos.vec.Engine;
import org.talos.vec.TestEngine;

public class CosGeneralTests {
    public static Engine   engine;
    public static String[] components;

    @BeforeClass
    public static void setup() throws Throwable {
        Map<String, Object> settings = new HashMap<String, Object>();
        Map<String, Object> defaults = new HashMap<String, Object>();
        Map<String, Object> basis = new HashMap<String, Object>();
        Map<String, Object> sparse = new HashMap<String, Object>();
        Map<String, Object> econf = new HashMap<String, Object>();
        sparse.put("accumuFactor", 10.0);
        sparse.put("sparseFactor", 2048);
        basis.put("vectorSetType", "sparse");
        basis.put("maxlimits", 20);
        econf.put("savepath", "data");
        econf.put("saveinterval", 7200000);
        econf.put("loadfactor", 0.75);
        econf.put("bycount", 100);
        defaults.put("sparse", sparse);
        defaults.put("basis", basis);
        defaults.put("engine", econf);
        settings.put("defaults", defaults);
        Config config = new Config(settings);

        engine = new Engine(config.getSub("engine"));
        TestEngine.engine = engine;

        components = new String[3];
        for (int i = 0; i < components.length; i++) {
            components[i] = "B" + String.valueOf(i);
        }

    }

    @Before
    public void testUp() throws Throwable {
        execCmd(bmk("btest", components), ok(), //
                vmk("btest", "vtest"), ok(), //
                rmk("vtest", "vtest"), ok(), //
                vadd("vtest", 2, 0.9f, 0.09f, 0.01f), ok(), //
                vadd("vtest", 3, 0.89f, 0f, 0.11f), ok(), //
                vadd("vtest", 5, 0.1f, 0.89f, 0.01f), ok(), //
                vadd("vtest", 7, 0.09f, 0f, 0.91f), ok(), //
                vadd("vtest", 11, 0f, 0.89f, 0.11f), ok(), //
                vadd("vtest", 13, 0f, 0.09f, 0.91f), ok() //
        );
    }

    @After
    public void testDown() throws Throwable {
        execCmd(del("btest"), ok());
        Thread.sleep(10);
    }

    @Test
    public void testBRev() throws Throwable {
        execCmd(brev("btest", "B3", "B1", "B0"), ok(), //
                vset("vtest", 13, 0.1f, 0.2f, 0.3f, 0.4f), ok(), //
                vset("vtest", 17, 0.4f, 0.3f, 0.2f, 0.1f), ok(), //
                vget("vtest", 2), floatList(0.9f, 0.09f, 0.01f, 0f), //
                vget("vtest", 3), floatList(0.89f, 0f, 0.11f, 0f), //
                vget("vtest", 5), floatList(0.1f, 0.89f, 0.01f, 0f), //
                vget("vtest", 7), floatList(0.09f, 0f, 0.91f, 0f), //
                vget("vtest", 11), floatList(0f, 0.89f, 0.11f, 0f), //
                vget("vtest", 13), floatList(0.1f, 0.2f, 0.3f, 0.4f), //
                vget("vtest", 17), floatList(0.4f, 0.3f, 0.2f, 0.1f));
    }

    @Test
    public void testSaveLoad() throws Throwable {
        try {
            execCmd(rrec("vtest", 2, "vtest"), longList(3, 5, 7, 11, 13), //
                    rrec("vtest", 11, "vtest"), longList(5, 13, 7, 2, 3), //
                    vrem("vtest", 3), ok(), //
                    vrem("vtest", 7), ok(), //
                    bsave("btest"), ok(), //
                    del("btest"), ok(), //
                    blist(), stringList(), //
                    bload("btest"), ok(),//
                    blist(), stringList("btest"), //
                    vlist("btest"), stringList("vtest"), //
                    rlist("vtest"), stringList("vtest"), //
                    vadd("vtest", 3, 0.89f, 0f, 0.11f), ok(), //
                    vadd("vtest", 7, 0.09f, 0f, 0.91f), ok(), //
                    rrec("vtest", 2, "vtest"), longList(3, 5, 7, 11, 13), //
                    rrec("vtest", 11, "vtest"), longList(5, 13, 7, 2, 3));
        } catch (Throwable t) {
            throw t;
        } finally {
            File file = new File("./data/btest.dmp");
            file.delete();
        }
    }
}
