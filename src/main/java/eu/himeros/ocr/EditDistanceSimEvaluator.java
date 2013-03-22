/*
 * Copyright © 2009 Perseus Project - Tufts University <http://www.perseus.tufts.edu>
 *
 * This file is part of EditDistancePerseus.
 *
 * EditDistancePerseus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * EditDistancePerseus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with EditDistancePerseus.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.himeros.ocr;

//TODO transpositions not yet implemented: i.e. only transp=false is supported

//import com.aliasi.spell.EditDistance;
import eu.himeros.alignment.SimEvaluator;
import eu.himeros.alignment.StringAligner;
/**
 * Evaluate similarity of two strings by their edit distance normalized.
 *
 * @author Federico Boschetti <federico.boschetti.73@gmail.com>
 */
public class EditDistanceSimEvaluator extends SimEvaluator{
    private static boolean zeroCenter;
    private static boolean transpBool=false;

    /**
     * Default Constructor.
     */
    public EditDistanceSimEvaluator(){}


    /**
     * Constructor with the transposition parameter.
     * N.B. transposition not yet implemented. Only transp=false is supported.
     * @param transp the boolean transposition switch.
     */
    public EditDistanceSimEvaluator(boolean transp){
        transpBool=transp;
    }

    /**
     * Return zero centered result, if the boolean zeroCenter is true.
     *
     * @param res the result.
     * @return the (possibly zero centered) result.
     */
    private static double makeRes(double res){
        if(zeroCenter){
            return (res*2)-1;
        }else{
            return res;
        }
    }

    /**
     * Determine if transposition of one character is a singular edit operation.
     * N.B. transposition not yet implemented. Only transp=false is supported.
     * @param transp the boolean transposition switch.
     */
    public static void setTransp(boolean transp){
        transpBool=transp;
    }

    /**
     * Get value of transposition.
     * N.B. transposition not yet implemented. Only transp=false is supported.
     * @return boolean trasposition switch.
     */
    public static boolean isTransp(){
        return transpBool;
    }

    /**
     * Determine if the result is zero centered (i.e. in the range [-1,1] or not (i.e. in the range [0,1])
     * @param _zeroCenter
     */
    public static void setZeroCenter(boolean _zeroCenter){
        zeroCenter=_zeroCenter;
    }

    /**
     * Return the number of edit operations (i.e. substitutions, insertions, deletions and, possibly, transpositions counted as a single operation)
     * necessary to trasform the first string into the second one.
     * N.B. transposition not yet implemented. Only transp=false is supported.
     * @param str1 the first string.
     * @param str2 the second string.
     * @param transp the boolean transposition switch (if true, trasposition of one character is evaluated as one edit operation,
     * otherwise it is evaluated as two operations: deletion+insertion).
     * 
     * @return the edit distance.
     */
    public static int editDistance(String str1, String str2, boolean transp){
        StringAligner sa=new StringAligner();
        //return EditDistance.editDistance(str1, str2, transp);
        sa.align(str1, str2);
        return sa.getEditDistance();
    }

    /**
     * Calculate the normalized edit distance, in the range [0,1]
     * N.B. transposition not yet implemented. Only transp=false is supported.
     *
     * @param str1 the first string.
     * @param str2 the second string.
     * @param transp the boolean transposition switch.
     * @return the normalized edit distance.
     */
    private static double normNoZeroCenterEditDistance(String str1, String str2, boolean transp){
        StringAligner sa=new StringAligner();
        sa.align(str1,str2);
        double ed=(double)sa.getEditDistance();
        //double ed=(double)EditDistance.editDistance(str1, str2, transp);
        double longestSeq=(double)Math.max(str1.length(), str2.length());
        return ed/longestSeq;
    }

    /**
     * Calculate the normalized edit distance, zero centered (i.e. in the range [-1,1]
     * N.B. transposition not yet implemented. Only transp=false is supported.
     *
     * @param str1 the first string.
     * @param str2 the second string.
     * @param transp the boolean transposition switch.
     * @return the normalized edit distance, zero centered.
     */
    public static double normEditDistance(String str1, String str2, boolean transp){
        StringAligner sa=new StringAligner();
        sa.align(str1,str2);
        double ed=(double)sa.getEditDistance();
        //double ed=(double)EditDistance.editDistance(str1, str2, transp);
        double longestSeq=(double)Math.max(str1.length(), str2.length());
        return makeRes(ed/longestSeq);
    }

    /**
     * Calculate the similarity score between strings.
     * N.B. transposition not yet implemented. Only transp=false is supported.
     *
     * @param str1 the first string.
     * @param str2 the second string.
     * @param transp the boolean transposition switch.
     * @return the similarity score.
     */
    public static double simScore(String str1, String str2, boolean transp){
        return makeRes(1-(double)normNoZeroCenterEditDistance(str1,str2, transp));
    }

    /**
     * Override the <code>eval</code> method of <code>SimEvaluator</code> superclass.
     * @param str1 the first string.
     * @param str2 the second string.
     * @return the similarity score.
     */
    @Override
    public double eval(String str1, String str2){
        return simScore(str1,str2,transpBool);
    }

}
