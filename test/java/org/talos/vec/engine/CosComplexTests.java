package org.talos.vec.engine;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.talos.Config;
import org.talos.vec.Engine;
import org.talos.vec.TestEngine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.talos.vec.TestEngine.*;

public class CosComplexTests {
    public static Engine engine;
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
                vmk("btest", "vtest2"), ok(), //
                rmk("vtest", "vtest"), ok(), //
                rmk("vtest", "vtest2"), ok(), //
                vadd("vtest", 2, 0.9f, 0.09f, 0.01f), ok(), //
                vadd("vtest", 3, 0.89f, 0f, 0.11f), ok(), //
                vadd("vtest", 5, 0.1f, 0.89f, 0.01f), ok(), //
                vadd("vtest", 7, 0.09f, 0f, 0.91f), ok(), //
                vadd("vtest", 11, 0f, 0.89f, 0.11f), ok(), //
                vadd("vtest", 13, 0f, 0.09f, 0.91f), ok(), //
                vadd("vtest2", 2, 0.9f, 0.09f, 0.01f), ok(), //
                vadd("vtest2", 3, 0.89f, 0f, 0.11f), ok(), //
                vadd("vtest2", 5, 0.1f, 0.89f, 0.01f), ok(), //
                vadd("vtest2", 7, 0.09f, 0f, 0.91f), ok(), //
                vadd("vtest2", 11, 0f, 0.89f, 0.11f), ok(), //
                vadd("vtest2", 13, 0f, 0.09f, 0.91f), ok() //
        );
    }

    @After
    public void testDown() throws Throwable {
        execCmd(del("btest"), ok());
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

    /**
     * @throws Throwable
     */
    @Test
    public void testSaveLoad() throws Throwable {
        try {
            execCmd(rrec("vtest", 2, "vtest"), longList(3, 5, 7, 11, 13), //
                    rrec("vtest", 2, "vtest2"), longList(2, 3, 5, 7, 11, 13), //
                    rrec("vtest", 11, "vtest2"), longList(11, 5, 13, 7, 2, 3), //
                    vrem("vtest", 3), ok(), //
                    vrem("vtest", 7), ok(), //
                    vrem("vtest2", 3), ok(), //
                    vrem("vtest2", 7), ok(), //
                    bsave("btest"), ok(), //
                    del("btest"), ok(), //
                    blist(), stringList(), //
                    bload("btest"), ok(),//
                    blist(), stringList("btest"), //
                    vlist("btest"), stringList("vtest", "vtest2"), //
                    rlist("vtest"), stringList("vtest", "vtest2"), //
                    vadd("vtest", 3, 0.89f, 0f, 0.11f), ok(), //
                    vadd("vtest2", 3, 0.89f, 0f, 0.11f), ok(), //
                    vadd("vtest", 7, 0.09f, 0f, 0.91f), ok(), //
                    vadd("vtest2", 7, 0.09f, 0f, 0.91f), ok(), //
                    rrec("vtest", 2, "vtest"), longList(3, 5, 7, 11, 13), //
                    rrec("vtest", 2, "vtest2"), longList(2, 3, 5, 7, 11, 13), //
                    rrec("vtest", 11, "vtest2"), longList(11, 5, 13, 7, 2, 3));
        } catch (Throwable t) {
            throw t;
        } finally {
            File file = new File("./data/btest.dmp");
            file.delete();
        }
    }

    /**
     * @throws Throwable
     */
    @Test
    public void testVacc() throws Throwable {
        execCmd(rrec("vtest", 2, "vtest"), longList(3, 5, 7, 11, 13), //
                rrec("vtest", 2, "vtest2"), longList(2, 3, 5, 7, 11, 13), //
                vacc("vtest", 3, 0.2f, 0f, 0.8f), ok(), //
                vacc("vtest", 3, 0.2f, 0f, 0.8f), ok(), //
                vacc("vtest", 7, 0.4f, 0.41f, 0.19f), ok(), //
                vacc("vtest", 7, 0.4f, 0.41f, 0.19f), ok(), //
                vacc("vtest", 11, 0.6f, 0.11f, 0.29f), ok(), //
                vacc("vtest", 11, 0.6f, 0.11f, 0.29f), ok(), //
                vacc("vtest2", 7, 0.4f, 0.41f, 0.19f), ok(), //
                vacc("vtest2", 7, 0.4f, 0.41f, 0.19f), ok(), //
                rrec("vtest", 2, "vtest"), longList(11, 3, 7, 5, 13), //
                rrec("vtest", 2, "vtest2"), longList(2, 3, 7, 5, 11, 13));
    }
}