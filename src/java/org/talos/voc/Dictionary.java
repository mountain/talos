package org.talos.voc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.talos.JcsegTaskConfig;
import org.talos.nlp.Segmenter;
import org.talos.nlp.Token;
import org.talos.nlp.lex.EnSCMix;

/**
 * Dictionary abstract class. <br />
 * 
 * @author chenxin<chenxin619315@gmail.com>
 */
public class Dictionary {

    public static final String     AL_TODO_FILE   = "lex-autoload.todo";

    protected JcsegTaskConfig      config;
    protected boolean              sync;

    /** autoload thread */
    private Thread                 autoloadThread = null;

    /** hash table for the words */
    protected Map<String, Token>[] dics           = null;

    public Dictionary(JcsegTaskConfig config, Boolean sync) {
        this.config = config;
        this.sync = sync.booleanValue();
        this.dics = new Map[ILexicon.T_LEN];
        if (this.sync) {
            for (int j = 0; j < ILexicon.T_LEN; j++)
                this.dics[j] = new ConcurrentHashMap<String, Token>(16, 0.80F);
        } else {
            for (int j = 0; j < ILexicon.T_LEN; j++)
                this.dics[j] = new HashMap<String, Token>(16, 0.80F);
        }
    }

    public JcsegTaskConfig getConfig() {
        return config;
    }

    public void setConfig(JcsegTaskConfig config) {
        this.config = config;
    }

    public void outputVocabulary() {
        try {
            SortedSet<String> set = new TreeSet<String>();
            for (int j = 0; j < ILexicon.T_LEN; j++)
                for (String key : dics[j].keySet())
                    set.add(String.format("%02d\t%s\n", key.length(), key));
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("voc.voc"),
                    Charset.forName("UTF-8")));
            for(String key : set)
                writer.write(key);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFromLexiconFile(File file) {
        loadWordsFromFile(config, this, file, "UTF-8");
    }

    public void loadFromLexiconFile(String file) {
        loadWordsFromFile(config, this, new File(file), "UTF-8");
    }

    /**
     * load the all the words form all the files under a specified lexicon
     * directionry . <br />
     * 
     * @param lexDir
     * 
     * @throws IOException
     */
    public void loadFromLexiconDirectory(String lexDir) throws IOException {

        File[] files = getLexiconFiles(lexDir, config.getLexiconFilePrefix(), config.getLexiconFileSuffix());
        if (files == null)
            return;

        for (int j = 0; j < files.length; j++) {
            loadWordsFromFile(config, this, files[j], "UTF-8");
        }
    }

    /** start the lexicon autoload thread . */
    public void startAutoload() {
        if (autoloadThread != null)
            return;
        autoloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                File todo = new File(config.getLexiconPath() + "/" + AL_TODO_FILE);
                long lastModified = todo.lastModified();
                while (true) {
                    // sleep for some time (seconds)
                    try {
                        Thread.sleep(config.getPollTime() * 1000);
                    } catch (InterruptedException e) {
                        break;
                    }

                    // check the update of the lex-autoload.todo
                    if (todo.lastModified() <= lastModified)
                        continue;

                    // load words form the lexicon files
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(todo));
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.indexOf('#') != -1)
                                continue;
                            if ("".equals(line))
                                continue;
                            loadFromLexiconFile(config.getLexiconPath() + "/" + line);
                        }
                        reader.close();
                        FileWriter fw = new FileWriter(todo);
                        fw.write("");
                        fw.close();

                        lastModified = todo.lastModified();
                        // System.out.println("newly added words loaded.");
                    } catch (IOException e) {
                        break;
                    }
                }
                autoloadThread = null;
            }
        });
        autoloadThread.setDaemon(true);
        autoloadThread.start();
        // System.out.println("lexicon autoload thread started!!!");
    }

    public void stopAutoload() {
        if (autoloadThread != null) {
            autoloadThread.interrupt();
            autoloadThread = null;
        }
    }

    public boolean isSync() {
        return sync;
    }

    public boolean match(int t, String key) {
        if (t < 0 || t >= ILexicon.T_LEN)
            return false;
        return dics[t].containsKey(key);
    }

    public void add(int t, String key, int type) {
        if (t < 0 || t >= ILexicon.T_LEN)
            return;
        if (dics[t].get(key) == null)
            dics[t].put(key, new Token(key, type));
    }

    public void add(int t, String key, int fre, int type) {
        if (t < 0 || t >= ILexicon.T_LEN)
            return;
        if (dics[t].get(key) == null)
            dics[t].put(key, new Token(key, fre, type));
    }

    public Token get(int t, String key) {
        if (t < 0 || t >= ILexicon.T_LEN)
            return null;
        return dics[t].get(key);
    }

    public void remove(int t, String key) {
        if (t < 0 || t >= ILexicon.T_LEN)
            return;
        dics[t].remove(key);
    }

    public int size(int t) {
        if (t < 0 || t >= ILexicon.T_LEN)
            return 0;
        return dics[t].size();
    }

    public static int getIndex(String key) {
        if (key == null)
            return -1;
        key = key.toUpperCase();

        if (key.equals("CJK_WORDS"))
            return ILexicon.CJK_WORD;
        else if (key.equals("CJK_UNITS"))
            return ILexicon.CJK_UNITS;
        else if (key.equals("EC_MIXED_WORD"))
            return ILexicon.EC_MIXED_WORD;
        else if (key.equals("CE_MIXED_WORD"))
            return ILexicon.CE_MIXED_WORD;
        else if (key.equals("CN_LNAME"))
            return ILexicon.CN_LNAME;
        else if (key.equals("CN_SNAME"))
            return ILexicon.CN_SNAME;
        else if (key.equals("CN_DNAME_1"))
            return ILexicon.CN_DNAME_1;
        else if (key.equals("CN_DNAME_2"))
            return ILexicon.CN_DNAME_2;
        else if (key.equals("CN_LNAME_ADORN"))
            return ILexicon.CN_LNAME_ADORN;
        else if (key.equals("EN_PUN_WORDS"))
            return ILexicon.EN_PUN_WORD;
        else if (key.equals("STOP_WORDS"))
            return ILexicon.STOP_WORD;
        else if (key.equals("EN_WORD"))
            return ILexicon.EN_WORD;

        return ILexicon.CJK_WORD;
    }

    /**
     * get all the lexicon file under the specified path and meet the specified
     * conditions . <br />
     * 
     * @throws IOException
     */
    public static File[] getLexiconFiles(String lexDir, String prefix, String suffix) throws IOException {

        File path = new File(lexDir);
        if (path.exists() == false)
            throw new IOException("Lexicon directory [" + lexDir + "] does'n exists.");

        /*
         * load all the lexicon file under the lexicon path that start with
         * __prefix and end with __suffix.
         */
        final String __suffix = suffix;
        final String __prefix = prefix;
        File[] files = path.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.startsWith(__prefix) && name.endsWith(__suffix));
            }
        });

        return files;
    }

    /**
     * load all the words in the specified lexicon file into the dictionary. <br />
     * 
     * @param config
     * @param dic
     * @param file
     * @param charset
     */
    public static void loadWordsFromFile(JcsegTaskConfig config, Dictionary dic, File file, String charset) {

        InputStreamReader ir = null;
        BufferedReader br = null;

        try {
            ir = new InputStreamReader(new FileInputStream(file), charset);
            br = new BufferedReader(ir);

            String line = null;
            boolean isFirstLine = true;
            int t = -1;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if ("".equals(line))
                    continue;
                // swept the notes
                if (line.charAt(0) == '#' && line.length() > 1)
                    continue;
                // the first line fo the lexicon file.
                if (isFirstLine == true) {
                    t = Dictionary.getIndex(line);
                    // System.out.println(line+", "+t);
                    isFirstLine = false;
                    if (t >= 0)
                        continue;
                }

                // handle the stopwords
                if (t == ILexicon.STOP_WORD) {
                    if (EnSCMix.isFWEnChar(line.charAt(0)) || line.length() <= config.MAX_LENGTH) {
                        dic.add(ILexicon.STOP_WORD, line, Segmenter.T_CJK_WORD);
                    }
                    continue;
                }

                // special lexicon
                if (line.indexOf('/') == -1) {
                    /*
                     * Here: 1. english and chinese mixed words, 2. chinese and
                     * english mixed words, 3. english punctuation words, don't
                     * have to limit its length.
                     */
                    boolean olen = (t == ILexicon.EC_MIXED_WORD);
                    olen = olen || (t == ILexicon.CE_MIXED_WORD);
                    olen = olen || (t == ILexicon.EN_PUN_WORD);
                    if (olen || line.length() <= config.MAX_LENGTH) {
                        dic.add(t, line, Segmenter.T_CJK_WORD);
                    }
                }
                // normal words lexicon file
                else {
                    String[] wd = line.split("/");

                    if (wd.length < 4) { // format check
                        System.out.println("Lexicon File: " + file.getAbsolutePath() + "#" + wd[0]
                                + " format error. -ignored");
                        continue;
                    }
                    if (wd.length == 5) { // single word degree check.
                        if (!EnSCMix.isDigit(wd[4])) {
                            System.out.println("Lexicon File: " + file.getAbsolutePath() + "#" + wd[0]
                                    + " format error(single word " + "degree should be an integer). -ignored");
                            continue;
                        }
                    }

                    // length limit(CJK_WORD only)
                    if ((t == ILexicon.CJK_WORD) && wd[0].length() > config.MAX_LENGTH)
                        continue;

                    if (dic.get(t, wd[0]) == null) {
                        if (wd.length > 4)
                            dic.add(t, wd[0], Integer.parseInt(wd[4]), Segmenter.T_CJK_WORD);
                        else
                            dic.add(t, wd[0], Segmenter.T_CJK_WORD);
                    }

                    Token w = dic.get(t, wd[0]);
                    // System.out.println(wd.length);
                    // set the pinying of the word.
                    if (config.LOAD_CJK_PINYIN && !"null".equals(wd[2])) {
                        w.pinyin(wd[2]);
                    }

                    boolean li = (t == ILexicon.CJK_WORD);

                    // set the syn words.
                    String[] arr = w.synm();
                    if (config.LOAD_CJK_SYN && !"null".equals(wd[3])) {
                        String[] syns = wd[3].split(",");
                        for (int j = 0; j < syns.length; j++) {
                            syns[j] = syns[j].trim();
                            /*
                             * Here: filter the syn words that its length is
                             * greater than Config.MAX_LENGTH
                             */
                            if (li && syns[j].length() > config.MAX_LENGTH)
                                continue;
                            /*
                             * Here: check the syn word is not exists, make sure
                             * the same syn word won't appended. (dictionary
                             * reload)
                             * 
                             * @date 2013-09-02
                             */
                            if (arr != null) {
                                int length = arr.length;
                                boolean add = true;
                                for (int i = 0; i < length; i++) {
                                    if (syns[j].equals(arr[i])) {
                                        add = false;
                                        break;
                                    }
                                }
                                if (!add)
                                    continue;
                            }
                            w.addSynm(syns[j]);
                        }
                    }

                    // set the word's part of speech
                    arr = w.tags();
                    if (config.LOAD_CJK_POS && !"null".equals(wd[1])) {
                        String[] pos = wd[1].split(",");
                        for (int j = 0; j < pos.length; j++) {
                            pos[j] = pos[j].trim();
                            /*
                             * Here: check the part of speech is not exists,
                             * make sure the same part of speech won't
                             * appended.(dictionary reload)
                             * 
                             * @date 2013-09-02
                             */
                            if (arr != null) {
                                int length = arr.length;
                                boolean add = true;
                                for (int i = 0; i < length; i++) {
                                    if (pos[j].equals(arr[i])) {
                                        add = false;
                                        break;
                                    }
                                }
                                if (!add)
                                    continue;
                            }
                            w.addTag(pos[j].trim());
                        }
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ir != null)
                    ir.close();
                if (br != null)
                    br.close();
            } catch (IOException e) {
            }
        }
    }
}
