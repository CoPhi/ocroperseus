
package eu.himeros.spellchecker;

import eu.himeros.alignment.StringAligner;
import eu.himeros.alignment.UpperCaseSimEvaluator;
import org.apache.lucene.search.spell.StringDistance;

/**
 *
 * @author federico
 */
public class LaStringDistance implements StringDistance {
   

    public LaStringDistance(){
        super();
    }

    public float getDistance(String s1, String s2) {
        return 1;
    }

}
