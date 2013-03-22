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

import eu.himeros.transcoder.Transcoder;
import java.io.*;

/**
 *
 * @author Federico Boschetti <federico.boschetti.73@gmail.com>
 */
public class AdjustClassicalOcr extends AdjustOcr{
    BufferedReader br=null;
    BufferedWriter bw=null;
    Transcoder transGreek=null;
    Transcoder transLatin=null;

    public AdjustClassicalOcr(){}

    public AdjustClassicalOcr(String transGreekName, String transLatinName){
        transGreek=new Transcoder(transGreekName);
        transLatin=new Transcoder(transLatinName);
    }

    public AdjustClassicalOcr(InputStream transGreekIs, InputStream transLatinIs){
        transGreek=new Transcoder(transGreekIs);
        transLatin=new Transcoder(transLatinIs);
    }
    
    public void setTransGreek(String transGreekName){
        transGreek=new Transcoder(transGreekName);
    }

    public void setTransGreek(InputStream transGreekIs){
        transGreek=new Transcoder(transGreekIs);
    }
    
    public void setTransLatin(String transLatinName){
        transLatin=new Transcoder(transLatinName);
    }

    public void setTransLatin(InputStream transLatinIs){
        transLatin=new Transcoder(transLatinIs);
    }

    private boolean testLatin(String str){
        double len=str.length();
        int greekChars=0;
        for(int i=0;i<str.length();i++){
            if(str.charAt(i)>256){
                greekChars++;
            }
            if(greekChars>=(len/2)) return false;
        }
        return true;
    }

    @Override
    public void adjust(String inFileName, String outFileName){
        String s="";
        try{
            br=new BufferedReader(new FileReader(inFileName));
            bw=new BufferedWriter(new FileWriter(outFileName));
            String l;
            while((l=br.readLine())!=null){
                l=l.trim();
                l=l.replaceAll(" +"," ");
                String[] items=l.split(" ");
                for(String item:items){
                    if(testLatin(item)){
                        item=transLatin.parse(" "+item+" ").trim();
                    }else{
                        item=transGreek.parse(" "+item+" ").trim();
                        item=item.replaceAll("([^.,:;]*)[.,:;·]([\u0370-\u03FF\u1F00-\u1FFF]+)","$1$2");
                    }
                    s+=item+" ";
                }
                bw.write(s.trim());bw.newLine();
                s="";
            }
            bw.close();
        }catch(Exception ex){ex.printStackTrace(System.err);}
    }
    
    @Override
    public String adjust(String str){
        StringBuilder s=new StringBuilder(1024);
        str.trim();
        str=str.replaceAll(" +"," ");
        String[] items=str.split(" ");
        for(String item:items){
            if(testLatin(item)){
                item=transLatin.parse(" "+item+" ").trim();
            }else{
                item=transGreek.parse(" "+item+" ").trim();
                item=item.replaceAll("([^.,:;]*)[.,:;·]([\u0370-\u03FF\u1F00-\u1FFF]+)","$1$2");
            }
            s.append(item).append(" ");
        }
        return s.toString();
    }
    
    @Override
    public void makeAdjusters(String[] adjusterNames){
        setAdjusterNames(adjusterNames);
        setTransGreek(adjusterNames[0]);
        setTransLatin(adjusterNames[1]);
    }
}
