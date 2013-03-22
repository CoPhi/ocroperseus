/*
 * Copyright © 2009 Perseus Project - Tufts University <http://www.perseus.tufts.edu>
 *
 * This file is part of UniCollatorPerseus.
 *
 * OcroPerseus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * OcroPerseus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with OcroPerseus.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.himeros.ocr;

import eu.himeros.alignment.StringAligner;
import eu.himeros.alignment.UpperCaseSimEvaluator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.Vector;

/**
 *
 * @author Federico Boschetti <federico.boschetti.73@gmail.com>
 */

public class ErrorEvaluator {
    BufferedReader br1=null;
    BufferedReader br2=null;
    BufferedWriter bw=null;
    StringAligner alg=null;
    Vector<String> v1=null;
    Vector<String> v2=null;

    /**
     *
     */
    public ErrorEvaluator(){}

    /**
     *
     * @param args
     */
    public ErrorEvaluator(String[] args){
        make(args);
    }

    /**
     *
     * @param args
     */
    public void make(String[] args){
        try{
            v1=new Vector<String>(1000);
            v2=new Vector<String>(1000);
            br1=new BufferedReader(new FileReader(args[0]));
            br2=new BufferedReader(new FileReader(args[1]));
            bw=new BufferedWriter(new FileWriter(args[2]));
            alg=new StringAligner(new UpperCaseSimEvaluator());
            String[] algs;
            String line;
            double perc; //percentage of errors+approxs per line
            int errs;
            int appxs;
            int inss;
            int dels;
            int subs;
            int corrects;
            int totInss=0;
            int totDels=0;
            int totSubs=0;
            int totCorrects=0;
            int totErrs=0;
            int totAppxs=0;
            int totChars=0;
            int totS1Chars=0;
            int totS2Chars=0;
            int totMatchChars=0;
            String s1=null,s2=null;
            DecimalFormat df=new DecimalFormat("0.00");
            while((line=br1.readLine())!=null){
                v1.add(line);
            }
            while((line=br2.readLine())!=null){
                v2.add(line);
            }
            bw.write("First line is the scanned text line");bw.newLine();
            bw.write("   \u00b0 represents gap required to align the strings");bw.newLine();
            bw.write("Second line is the alignment mask:");bw.newLine();
            bw.write("   | matching chars, # error, ~ error between similar chars, + ins, - del");bw.newLine();
            bw.write("   numbers show: [matches/(matches+subs+inss+dels)=accuracy%]");bw.newLine();
            bw.write("Third line is the diplomatic transcription of the original text (ground truth)");bw.newLine();bw.newLine();bw.newLine();
            double totperc=0;
            for(int i=0;i<v1.size();i++){
                if(i<v2.size()){
                    s1=v1.elementAt(i);
                    s2=v2.elementAt(i);
                    algs=alg.align(s1, s2);
                    errs=alg.getErrorSum();
                    appxs=alg.getApproxSum();
                    inss=alg.getInsSum();
                    dels=alg.getDelSum();
                    subs=alg.getSubSum();
                    corrects=alg.getMatches();
                    totChars+=s2.length();
                    totS1Chars+=s1.length();
                    totS2Chars+=s2.length();
                    totMatchChars+=algs[2].length();
                    totInss+=inss;
                    totDels+=dels;
                    totSubs+=subs;
                    totCorrects+=corrects;
                    totErrs+=errs;
                    totAppxs+=appxs;
                    perc=EditDistanceSimEvaluator.simScore(s1, s2, false)*100;
                    totperc+=perc;
                    bw.write(algs[0]);bw.newLine();
                    bw.write(algs[2]+" ["+corrects+"/"+"("+corrects+"+"+subs+"+"+inss+"+"+dels+")="+corrects+"/"+(corrects+subs+inss+dels)+"="+df.format(((double)corrects/((double)corrects+(double)subs+(double)inss+(double)dels))*100)+"%]");bw.newLine();
                    bw.write(algs[1]);bw.newLine();bw.newLine();
                }
            }
            bw.newLine();
            bw.write("Report");bw.newLine();bw.newLine();
            bw.write("total # of chars in ocr: "+totS1Chars);bw.newLine();
            bw.write("total # of chars in ground truth: "+totS2Chars);bw.newLine();
            bw.write("(total # of edit operations: "+totMatchChars+")");bw.newLine();bw.newLine();
            bw.write("total # of matching chars: "+totCorrects);bw.newLine();bw.newLine();
            bw.write("total # of substitutions: "+totSubs);bw.newLine();
            bw.write("total # of insertions: "+totInss);bw.newLine();
            bw.write("total # of deletions: "+totDels);bw.newLine();bw.newLine();
            double recall=(double)totCorrects/((double)totCorrects+(double)totSubs+(double)totDels);
            double precision=(double)totCorrects/((double)totCorrects+(double)totInss+(double)totSubs);
            double accuracy=(double)totCorrects/((double)totCorrects+(double)totSubs+(double)totInss+(double)totDels);
            double fscore=2*recall*precision/(recall+precision);
            bw.write("precision [matches/(matches+subs+inss)]: "+totCorrects+"/("+totCorrects+"+"+totSubs+"+"+totInss+")="+df.format(precision*100)+"%");bw.newLine();
            bw.write("recall [matches/(matches+subs+dels)]: "+totCorrects+"/("+totCorrects+"+"+totSubs+"+"+totDels+")="+df.format(recall*100)+"%");bw.newLine();
            bw.write("accuracy [matches/(matches+subs+inss+dels)]: "+totCorrects+"/("+totCorrects+"+"+totSubs+"+"+totInss+"+"+totDels+ ")="+df.format(accuracy*100)+"%");bw.newLine();bw.newLine();
            bw.write("f-score [2*recall*precision/(recall+precision)]: "+2+"*"+df.format(recall)+"*"+df.format(precision)+"/("+df.format(recall)+"+"+df.format(precision)+")="+df.format(fscore*100)+"%");bw.newLine();bw.newLine();
            bw.write("similarity rate [average of 1-normalized_edit_distance]: "+df.format(totperc/(double)v1.size())+"%");
            bw.close();
        }catch(Exception ex){ex.printStackTrace(System.err);}
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args){
        ErrorEvaluator eev=new ErrorEvaluator(args);
    }
}
