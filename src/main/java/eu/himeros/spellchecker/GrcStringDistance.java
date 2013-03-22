
package eu.himeros.spellchecker;

import eu.himeros.alignment.StringAligner;
import eu.himeros.alignment.UpperCaseSimEvaluator;
import org.apache.lucene.search.spell.StringDistance;

/**
 *
 * @author federico
 */
public class GrcStringDistance implements StringDistance {
    StringAligner sa=new StringAligner(new UpperCaseSimEvaluator());

    public GrcStringDistance(){
        super();
    }

    public float getDistance(String s1, String s2) {
        sa.align(s1, s2);
        return 1/(float)sa.getEditDistance();
    }

}
