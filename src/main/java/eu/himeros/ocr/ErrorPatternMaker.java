/*
 * Copyright © 2009 Perseus Project - Tufts University <http://www.perseus.tufts.edu>
 *
 * This file is part of UniCollatorPerseus.
 *
 * AlignmentPerseus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * AlignmentPerseus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with AlignmentPerseus.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.himeros.ocr;

import eu.himeros.alignment.AnchorFinder;
import eu.himeros.alignment.StringAligner;
import eu.himeros.alignment.UpperCaseSimEvaluator;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Create the Error Pattern Files.
 *
 * @author Federico Boschetti <federico.boschetti.73@gmail.com>
 */
public class ErrorPatternMaker {

    private String errPat1 = "";
    private String errPat2 = "";
    private String key = "";
    private Double one = new Double(1.0);
    private Double cnt = new Double(0.0);
    private HashMap<String, Double> hm = new HashMap<String, Double>();
    private HashMap<String, Double> hmCm = new HashMap<String, Double>(); // column marginals
    private HashMap<String, Double> hmRm = new HashMap<String, Double>(); //row marginals
    private HashSet<String> errorSet = new HashSet<String>();
    private HashSet<String> errNgramSet = new HashSet<String>();
    private Object[] arr;
    private double totSum = 0.0; // init sum of column (or row) marginals
    private int engineNum = 3; // number of engines: in this implementation it is fixed to 3;
    private char gapChar = '\u00B0'; //default gap character
    private char crChar = '\u00B6'; //default carret return (paragraph) symbol character
    private String anchorFilter;
    private int nglen = 4; // default ngram length

    /**
     * Default Constructor.
     */
    public ErrorPatternMaker() {
    }

    /**
     * Add an error pattern.
     *
     * @param str1 the OCR chunk.
     * @param str2 the ground truth chunk.
     */
    public void add(String str1, String str2) {
        int ng;
        int eq;
        int len = str1.length();
        for (int i = 0; i < len; i++) {
            errPat1 = "";
            errPat2 = "";
            cnt = new Double(0.0);
            ng = 0;
            while (ng < nglen) {
                if (i + ng >= str1.length()) {
                    str1 = str1 + gapChar;
                    str2 = str2 + gapChar;
                }
                errPat1 += "" + str1.charAt(i + ng);
                errPat2 += "" + str2.charAt(i + ng);
                eq = checkNgram(errPat1, errPat2);
                key = (ng + 1) + "\t" + errPat2 + "\t" + errPat1;
                cnt = hm.get(key);
                if (cnt == null) {
                    hm.put(key, one);
                } else {
                    hm.put(key, new Double(cnt.doubleValue() + 1.0));
                }
                if (eq == 0 && (ng + 1) > 1) {
                    errNgramSet.add((ng + 1) + "\t" + errPat2);
                }
                ng++;
            }
        }
    }

    /**
     * Assign the probability that an OCR character output is correct.
     */
    //TODO ATTENTION!!! HERE THERE IS A BIG PROBLEM WITH ROW AND COLUMN INVERSION: VERIFY!!!
    public void assignProbability() {
        String[] ngGtOcr;
        Double rowVal;
        Double colVal;
        double dval;
        double rm; // row marginal
        double cm; // col marginal
        double charProb; //char probability
        //make marginals of columns and rows
        for (String keyStr : hm.keySet()) {
            ngGtOcr = keyStr.split("\t");
            dval = hm.get(keyStr).doubleValue();
            computeMarginals(ngGtOcr, dval);
        }
        //assign probabilities to cells
        for (String keyStr : hm.keySet()) {
            ngGtOcr = keyStr.split("\t");
            dval = hm.get(keyStr).doubleValue();
            colVal = hmCm.get(ngGtOcr[1]);
            cm = colVal.doubleValue();
            charProb = Math.pow(cm / totSum, 1 / engineNum); // NEW
            rowVal = hmRm.get(ngGtOcr[2]);
            rm = rowVal.doubleValue();
            //ht.put(keyStr, new Double((dval/cm)*charProb)); // NEW <-- ATTENTION!!! cm instead of rm!!!
            hm.put(keyStr, new Double((dval / rm) * charProb));
            //ht.put(keyStr, new Double(dval/rm)); // OLD
        }
    }

    /**
     * Compute Column and Row marginals
     *
     * @param ngGtOcr
     * @param dval
     */
    private void computeMarginals(String[] ngGtOcr, double dval) {
        cnt = hmCm.get(ngGtOcr[1]);
        if (cnt == null) {
            cnt = new Double(0.0);
        }
        hmCm.put(ngGtOcr[1], new Double(cnt.doubleValue() + dval));
        cnt = hmRm.get(ngGtOcr[2]);
        if (cnt == null) {
            cnt = new Double(0.0);
        }
        hmRm.put(ngGtOcr[2], new Double(cnt.doubleValue() + dval));
        totSum += dval;
    }

    /**
     * Set the number of OCR engines.
     *
     * @param engineNum the number of OCR engines.
     */
    public void setEngineNum(int engineNum) {
        this.engineNum = engineNum;
    }

    /**
     * Check if the ngrams are equal (1) or not (0).
     *
     * @param str1 the first ngram.
     * @param str2 the second ngram.
     * @return the comparison result (1 if equal, 0 if different).
     */
    private int checkNgram(String str1, String str2) {
        for (int i = 0; i < str1.length(); i++) {
            if (str1.charAt(i) == str2.charAt(i)) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * Make the error set.
     */
    public void makeErrorSet() {
        for (String keyStr : hm.keySet()) {
            errorSet.add(keyStr.split("\t")[0]);
        }
    }

    /**
     * Get the array of error patterns.
     *
     * @return the array of error patterns.
     */
    public Object[] getErrorPatterns() {
        arr = hm.entrySet().toArray();
        Arrays.sort(arr, new Comparator() {
            public int compare(Object o1, Object o2) {
                if (((Double) ((Map.Entry) o1).getValue()).doubleValue() > ((Double) ((Map.Entry) o2).getValue()).doubleValue()) {
                    return (-1);
                } else if (((Double) ((Map.Entry) o1).getValue()).doubleValue() < ((Double) ((Map.Entry) o2).getValue()).doubleValue()) {
                    return (1);
                } else {
                    return (0);
                }
            };
        });
        return arr;
    }

    /**
     * Write the error patterns to the file
     *
     * @param bw the BufferedWriter that writes into the file.
     */
    public void writeErrorPatterns(BufferedWriter bw) {
        String keyMap;
        String[] keyItems;
        double valMap;
        assignProbability();
        getErrorPatterns();
        for (int i = 0; i < arr.length; i++) {
            try {
                keyMap = (String) ((Map.Entry) arr[i]).getKey();
                keyItems = keyMap.split("\t");
                valMap = ((Double) ((Map.Entry) (arr[i])).getValue()).doubleValue();
                if (valMap == 1.0 && keyItems[1].equals(keyItems[2])) {
                } else {
                    bw.write(keyItems[1] + "\t" + keyItems[2] + "\t" + valMap);
                    bw.newLine();
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    /**
     * Set the gap character.
     *
     * @param gapChar the gap character.
     */
    public void setGapChar(char gapChar) {
        this.gapChar = gapChar;
    }

    public char getCrChar() {
        return crChar;
    }

    public void setCrChar(char crChar) {
        this.crChar = crChar;
    }

    public String getAnchorFilter() {
        return anchorFilter;
    }

    public void setAnchorFilter(String anchorFilter) {
        this.anchorFilter = anchorFilter;
    }

    /**
     * Set the gap character, providing a single character string.
     *
     * @param gapTag the single character string containing the gap tag.
     */
    public void setGapTag(String gapTag) {
        this.gapChar = gapTag.charAt(0);
    }

    public void make(String[] ocrFileNames, String[] gtFileNames, String outFileName) {
        StringAligner alg;
        String algs[];
        BufferedReader br;
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(outFileName));
        } catch (IOException ex) {
            Logger.getLogger(ErrorPatternMaker.class.getName()).log(Level.SEVERE, null, ex);
        }
        String line;
        int len = 2;
        for (int k = 0; k < ocrFileNames.length; k++) {
            String[] inFileNames = new String[len];
            inFileNames[0] = ocrFileNames[k];
            inFileNames[1] = gtFileNames[k];
            String[] texts = new String[len];
            alg = new StringAligner(new UpperCaseSimEvaluator());
            for (int i = 0; i < len; i++) {
                try {
                    br = new BufferedReader(new FileReader(inFileNames[i]));
                    while ((line = br.readLine()) != null) {
                        texts[i] += line + crChar; //substitute CR with the paragraph sign
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                }
            }
            char c;
            int[] beg = new int[len];
            int[] end = new int[len];
            for (int i = 0; i < len; i++) {
                beg[i] = 0;
            }
            AnchorFinder af = new AnchorFinder(inFileNames,anchorFilter);
            int[][] anchors = af.getAnchorPositions();
            for (int j = 0; j < anchors[0].length; j++) {
                for (int i = 0; i < len; i++) {
                    end[i] = anchors[i][j];
                    while (end[i] < texts[i].length() && (c = texts[i].charAt(end[i])) != ' ' && c != crChar) {
                        end[i]++;
                    }
                    end[i]++;
                }
                try {
                    if (end[0] > beg[0] && end[1] > beg[1]) {
                        algs = alg.align(texts[0].substring(beg[0], end[0]), texts[1].substring(beg[1], end[1]));
                        add(algs[0], algs[1]);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                    continue;
                }
                for (int i = 0; i < len; i++) {
                    beg[i] = end[i];
                }
            }
        }
        makeErrorSet();
        writeErrorPatterns(bw);
        try {
            bw.close();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    public void make(File[] ocrFiles, File[] gtFiles, String outFileName) {
        String[] ocrFileNames = new String[ocrFiles.length];
        String[] gtFileNames = new String[gtFiles.length];
        for (int i = 0; i < ocrFiles.length; i++) {
            ocrFileNames[i] = ocrFiles[i].getAbsolutePath();
            gtFileNames[i] = gtFiles[i].getAbsolutePath();
        }
        make(ocrFileNames, gtFileNames, outFileName);
    }
    
    public void useNegativeAnchorFilter(){
        anchorFilter="[^"+anchorFilter.substring(1,anchorFilter.length());
    }
}