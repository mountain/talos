package org.talos.vec.engine;

import static org.talos.vec.TestEngine.blist;
import static org.talos.vec.TestEngine.bmk;
import static org.talos.vec.TestEngine.brev;
import static org.talos.vec.TestEngine.del;
import static org.talos.vec.TestEngine.execCmd;
import static org.talos.vec.TestEngine.floatList;
import static org.talos.vec.TestEngine.longList;
import static org.talos.vec.TestEngine.ok;
import static org.talos.vec.TestEngine.rlist;
import static org.talos.vec.TestEngine.rmk;
import static org.talos.vec.TestEngine.rrec;
import static org.talos.vec.TestEngine.stringList;
import static org.talos.vec.TestEngine.vacc;
import static org.talos.vec.TestEngine.vadd;
import static org.talos.vec.TestEngine.vget;
import static org.talos.vec.TestEngine.vlist;
import static org.talos.vec.TestEngine.vmk;
import static org.talos.vec.TestEngine.vrem;
import static org.talos.vec.TestEngine.vset;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.talos.Config;
import org.talos.vec.Engine;
import org.talos.vec.TestEngine;

public class GeneralTests {
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
        basis.put("maxlimits", 3);
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
    public void testRec() throws Throwable {
        // TODO: should be
        // islongList(new int[] { 7, 11, 3});
        execCmd(rrec("vtest", 13, "vtest"), longList(7, 11), //
                rrec("vtest", 7, "vtest"), longList(13, 3, 11));
    }

    @Test
    public void testVget() throws Throwable {
        execCmd(vget("vtest", 2), floatList(0.9f, 0.09f, 0.01f), //
                vget("vtest", 3), floatList(0.89f, 0f, 0.11f), //
                vget("vtest", 5), floatList(0.1f, 0.89f, 0.01f), //
                vget("vtest", 7), floatList(0.09f, 0f, 0.91f),//
                vget("vtest", 11), floatList(0f, 0.89f, 0.11f),//
                vget("vtest", 13), floatList(0f, 0.09f, 0.91f)//
        );
    }

    @Test
    public void testRlist() throws Throwable {
        execCmd(rlist("vtest"), stringList("vtest"));
    }

    @Test
    public void testVrem() throws Throwable {
        // TODO: should be
        // longList(11, 3, 2)
        // longList(11, 3, 5)
        execCmd(rrec("vtest", 2, "vtest"), longList(3, 5, 7), //
                vrem("vtest", 5), ok(), //
                vrem("vtest", 7), ok(), //
                rrec("vtest", 13, "vtest"), longList(11),//
                vadd("vtest", 5, 0.1f, 0.89f, 0.01f), ok(), //
                rrec("vtest", 13, "vtest"), longList(11, 5),//
                vadd("vtest", 7, 0.09f, 0f, 0.91f), ok(), //
                rrec("vtest", 2, "vtest"), longList(3, 5, 7)//
        );
    }

    @Test
    public void testVset() throws Throwable {
        // replace 2 with 7 and 7 with 2
        // and then restore the original
        // TODO: should be
        // longList(2, 11, 3)
        // longList(7, 11, 3)
        execCmd(vset("vtest", 2, 0.09f, 0f, 0.91f), ok(), //
                vset("vtest", 7, 0.9f, 0.09f, 0.01f), ok(), //
                rrec("vtest", 13, "vtest"), longList(2, 11, 7), //
                vset("vtest", 2, 0.9f, 0.09f, 0.01f), ok(), //
                vset("vtest", 7, 0.09f, 0f, 0.91f), ok(), //
                rrec("vtest", 13, "vtest"), longList(7, 11, 2) //
        );
    }

    @Test
    public void testBlist() throws Throwable {
        execCmd(blist(), stringList("btest"));
    }

    @Test
    public void testVlist() throws Throwable {
        execCmd(vlist("btest"), stringList("vtest"));
    }

    @Test
    public void testVacc() throws Throwable {
        execCmd(vacc("vtest", 5, 0.1f, 0.9f, 0f), ok(), //
                vget("vtest", 5), floatList(0.2f, 1.79f, 0.01f) //
        );
    }

    @Test
    public void testBrev() throws Throwable {
        execCmd(brev("btest", "B2", "B1", "B0"), ok());
    }
}