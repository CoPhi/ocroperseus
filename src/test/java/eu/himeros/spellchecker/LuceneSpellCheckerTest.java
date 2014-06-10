/*
 * This file is part of eu.himeros_ocroperseus_jar_1.0-SNAPSHOT
 *
 * Copyright (C) 2012 federico[DOT]boschetti[DOT]73[AT]gmail[DOT]com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.himeros.spellchecker;

import eu.himeros.alignment.UpperCaseSimEvaluator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author federico[DOT]boschetti[DOT]73[AT]gmail[DOT]com
 */
public class LuceneSpellCheckerTest extends TestCase{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     * 
     */
    
    public LuceneSpellCheckerTest( String testName ){
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite(){
        return new TestSuite( eu.himeros.spellchecker.LuceneSpellCheckerTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testLuceneSpellChecker(){
        //assertTrue( true );
        System.setProperty("eu.himeros.spellcheckers","grc@/home/luca/lab009/test-ocr/lucene-grc");
        UpperCaseSimEvaluator.setResourceName("eu/himeros/resources/transcoders/low2up.txt");
        LuceneSpellChecker.init(System.getProperty("eu.himeros.spellcheckers"));
        String[] results=LuceneSpellChecker.spellcheck("καβαΣις","grc",5);
        for(String result:results){
            System.err.println(result);
        }
    }

}
