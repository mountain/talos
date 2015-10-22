package org.talos.voc;

import java.io.IOException;
import java.lang.reflect.Constructor;

import org.talos.JcsegTaskConfig;

/**
 * Dictionary Factory to create Dictionary instance . <br />
 * a path of the class that has extends the Dictionary class must be given
 * first. <br />
 * 
 * @author chenxin<chenxin619315@gmail.com>
 */
public class DictionaryFactory {

    private DictionaryFactory() {
    }

    /**
     * create a new Dictionary instance . <br />
     * 
     * @param __dicClass
     * @return Dictionary
     */
    public static Dictionary createDictionary(String __dicClass, Class<?>[] paramType, Object[] args) {
        try {
            Class<?> _class = Class.forName(__dicClass);
            Constructor<?> cons = _class.getConstructor(paramType);
            return ((Dictionary) cons.newInstance(args));
        } catch (Exception e) {
            System.err.println("can't create the Dictionary instance " + "with classpath [" + __dicClass + "]");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * create a default Dictionary instance of class
     * com.webssky.jcseg.Dictionary . <br />
     * 
     * @see Dictionary
     * @return Dictionary
     */
    public static Dictionary createDefaultDictionary(JcsegTaskConfig config, boolean sync) {
        Dictionary dic = createDictionary("org.talos.voc.Dictionary", new Class[] { JcsegTaskConfig.class,
                Boolean.class }, new Object[] { config, sync });
        try {
            // load lexicon from more than one path.
            String[] lexpath = config.getLexiconPath();
            if (lexpath == null)
                throw new IOException("Invalid lexicon path, " + "make use the JcsegTaskConfig is initialized.");

            // load word item from all the directories.
            for (String lpath : lexpath)
                dic.loadFromLexiconDirectory(lpath);
            if (dic.getConfig().isAutoload())
                dic.startAutoload();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dic;
    }

    public static Dictionary createDefaultDictionary(JcsegTaskConfig config) {
        Dictionary dic = createDefaultDictionary(config, config.isAutoload());
        dic.outputVocabulary();
        return dic;
    }
}