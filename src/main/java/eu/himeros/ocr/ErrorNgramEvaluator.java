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

import eu.himeros.alignment.AnchorFinder;
import eu.himeros.alignment.StringAligner;
import eu.himeros.alignment.UpperCaseSimEvaluator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;

/**
 *
 * @author Federico Boschetti <federico.boschetti.73@gmail.com>
 */
public class ErrorNgramEvaluator extends ErrorEvaluator{
    private StringBuffer sb1=null;
    private StringBuffer sb2=null;
    private int[][] anchorposs=null;
    private DecimalFormat df=new DecimalFormat("0.00");
    private int totS1Chars=0;
    private int totS2Chars=0;
    private int totMatchChars=0;
    private int totCorrects=0;
    private int totSubs=0;
    private int totInss=0;
    private int totDels=0;
    private String anchorFilter;
    private char crChar='\u00B6';
    
    /**
     *
     */
    public ErrorNgramEvaluator(){}

    /**
     *
     * @param args
     */
    public ErrorNgramEvaluator(String[] args){
        make(args);
    }

    /**
     *
     * @param args
     */

    public void make(String xFileName, String yFileName, String resFileName){
        String[] args=new String[3];
        args[0]=xFileName;
        args[1]=yFileName;
        args[2]=resFileName;
        make(args);
    }

    @Override
    public void make(String[] args){
    sb1=new StringBuffer(1000000);
    sb2=new StringBuffer(1000000);
    anchorposs=null;
        try{
            String[] fileNames=new String[2];
            fileNames[0]=args[0];
            fileNames[1]=args[1];
            AnchorFinder af=new AnchorFinder(fileNames,anchorFilter);
            anchorposs=af.getAnchorPositions();
            br1=new BufferedReader(new FileReader(args[0]));
            br2=new BufferedReader(new FileReader(args[1]));
            bw=new BufferedWriter(new FileWriter(args[2]));
            alg=new StringAligner(new UpperCaseSimEvaluator());
            String[] algs;
            String line;
            double perc; //percentage of errors+approxs per line
            totS1Chars=0;
            totS2Chars=0;
            totMatchChars=0;
            totInss=0;
            totDels=0;
            totSubs=0;
            totCorrects=0;
            String s1,s2;
            while((line=br1.readLine())!=null){
                sb1.append(line).append(crChar);
            }
            while((line=br2.readLine())!=null){
                sb2.append(line).append(crChar);
            }
            bw.write("First line is the scanned text line");bw.newLine();
            bw.write("   \u00b0 represents gap required to align the strings");bw.newLine();
            bw.write("Second line is the alignment mask:");bw.newLine();
            bw.write("   | matching chars, # error, ~ error between similar chars, + ins, - del");bw.newLine();
            bw.write("   numbers show: [matches/(matches+subs+inss+dels)=accuracy%]");bw.newLine();
            bw.write("Third line is the diplomatic transcription of the original text (ground truth)");bw.newLine();bw.newLine();bw.newLine();
            //find anchors
            double totperc=0;
            int beg1=0;
            int beg2=0;
            int end1=0;
            int end2=0;
            StringBuffer sbx1=new StringBuffer();
            StringBuffer sbx2=new StringBuffer();
            StringBuffer sbx3=new StringBuffer();
            for(int i=0;i<anchorposs[0].length;i++){
                end1=anchorposs[0][i];
                end2=anchorposs[1][i];
                s1=sb1.substring(beg1,end1);
                s2=sb2.substring(beg2,end2);
                algs=alg.align(s1, s2);
                //mixed=alg.getMixedString(); //!!!!
                //errs=alg.getErrorSum();
                //appxs=alg.getApproxSum();
                //totErrs+=errs;
                //totAppxs+=appxs;
                perc=EditDistanceSimEvaluator.simScore(s1, s2, false)*100;
                totperc+=perc;
                sbx1.append(algs[0]);
                sbx2.append(algs[2]);
                sbx3.append(algs[1]);
            }
            int fBeg=0;
            for(int j=0;j<sbx1.length();j++){
                if(sbx1.charAt(j)==crChar||j==sbx1.length()-1){
                    bw.write(sbx1.substring(fBeg,j+1));bw.newLine();
                    bw.write(sbx2.substring(fBeg,j+1)+lineReport(sbx2.substring(fBeg,j+1)));bw.newLine();
                    bw.write(sbx3.substring(fBeg,j+1));bw.newLine();
                    bw.newLine();
                    fBeg=j+1;
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
            bw.close();
        }catch(Exception ex){ex.printStackTrace(System.err);}
    }

    public String lineReport(String line){
        int corrects=0;
        int subs=0;
        int inss=0;
        int dels=0;
        for(int i=0;i<line.length();i++){
            switch(line.charAt(i)){
                case '|':
                    corrects++;
                    totCorrects++;
                    break;
                case '#':
                case '~':
                    subs++;
                    totSubs++;
                    break;
                case '-':
                    dels++;
                    totDels++;
                    break;
                case '+':
                    inss++;
                    totInss++;
                    break;
            }
        }
        totS1Chars+=corrects+subs+inss;
        totS2Chars+=corrects+subs+dels;
        totMatchChars+=corrects+subs+dels+inss;
        return " ["+corrects+"/"+"("+corrects+"+"+subs+"+"+inss+"+"+dels+")="+corrects+"/"+(corrects+subs+inss+dels)+"="+df.format(((double)corrects/((double)corrects+(double)subs+(double)inss+(double)dels))*100)+"%]";
    }

    public String getAnchorFilter() {
        return anchorFilter;
    }

    public void setAnchorFilter(String anchorFilter) {
        this.anchorFilter = anchorFilter;
    }

    public char getCrChar() {
        return crChar;
    }

    public void setCrChar(char crChar) {
        this.crChar = crChar;
    }
    
    /**
     *
     * @param args
     */
    public static void main(String[] args){
        ErrorNgramEvaluator eev=new ErrorNgramEvaluator(args);
    }

}
