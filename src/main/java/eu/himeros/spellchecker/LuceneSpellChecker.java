/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.himeros.spellchecker;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.NGramDistance;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.spell.StringDistance;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.util.Version;


/**
 *
 * @author federico
 */
public class LuceneSpellChecker {
    static SpellChecker spellchecker;
    static HashMap<String,SpellChecker> hm=new HashMap();
    /**
     * @param args the command line arguments
     */
    public static void init(String spellcheckers) {
        try {
            String[] spellcheckerStrs=spellcheckers.replaceAll(" ","").split("\\|");
            for(String spellcheckerStr:spellcheckerStrs){
                String[] strs=spellcheckerStr.split("@");
                File fileOrDirDict=new File(strs[1]);
                if(fileOrDirDict.isFile()){
                    File fileDict=fileOrDirDict;
                    File dirDict=fileOrDirDict.getParentFile();
                    NIOFSDirectory spellIndexDirectory =new NIOFSDirectory(dirDict, NoLockFactory.getNoLockFactory());
                    spellchecker = new SpellChecker(spellIndexDirectory);
                    IndexWriterConfig iwc=new IndexWriterConfig(Version.LUCENE_36,new WhitespaceAnalyzer(Version.LUCENE_36));
                    spellchecker.indexDictionary(new PlainTextDictionary(fileDict),iwc,true);
                }else if(fileOrDirDict.isDirectory()){
                    File dirDict=fileOrDirDict;
                    NIOFSDirectory spellIndexDirectory =new NIOFSDirectory(dirDict, NoLockFactory.getNoLockFactory());
                    System.err.println(spellIndexDirectory.toString());
                    spellchecker = new SpellChecker(spellIndexDirectory);
                    spellchecker.setSpellIndex(spellIndexDirectory);
                }
                hm.put(strs[0], spellchecker);
                String langStringDistancePrefix=strs[0].substring(0,1).toUpperCase()+strs[0].substring(1,strs[0].length());
                StringDistance sd;
                try{
                    sd=(StringDistance)Class.forName("eu.himeros.spellchecker."+langStringDistancePrefix+"StringDistance").getConstructor().newInstance();
                }catch(ClassNotFoundException cnfe){
                    sd=new NGramDistance();
                }
                spellchecker.setStringDistance(sd);
            }
        }catch(Exception ex){
            Logger.getLogger(LuceneSpellChecker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String[] spellcheck(String word, String lang, int numSuggestions){
        try{
            spellchecker=hm.get(lang);
            return spellchecker.suggestSimilar(word,numSuggestions);
        }catch (IOException ex) {
            Logger.getLogger(LuceneSpellChecker.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
//
    public static boolean exist(String word, String lang){
        try{
            spellchecker=hm.get(lang);
            return spellchecker.exist(word);
        }catch(Exception ex){
            Logger.getLogger(LuceneSpellChecker.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
}