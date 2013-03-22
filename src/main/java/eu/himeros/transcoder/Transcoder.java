/*
 * Copyright © 2009 Perseus Project - Tufts University <http://www.perseus.tufts.edu>
 *
 * This file is part of TranscoderPerseus.
 *
 * TranscoderPerseus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * TranscoderPerseus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with TranscoderPerseus.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.himeros.transcoder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * General purpose transcoder.
 *
 * @author Federico Boschetti <federico.boschetti.73@gmail.com>
 */
public class Transcoder {

    private Hashtable<String, String> directHt;
    private Hashtable<String, String> reverseHt;
    private Hashtable<String, String> ht;    
    private int maxLen = 0;
    private Pattern pattern=null;
    private String regexp=null;
    private BufferedReader br =null;

    /**
     * Default Constructor.
     */
    public Transcoder() {}

    /**
     * Constructor that receives the name of the file containing the table of patterns for transcodification.
     * @see #setTranscoder(java.lang.String)
     *
     * @param transFileName
     */
    public Transcoder(String transFileName) {
        setTranscoder(transFileName);
    }

    public Transcoder(InputStream transIs){
        setTranscoder(transIs);
    }

    /**
     * Constructor that receives the patterns for transcodification contained in a hashtable.
     *
     * @param ht the hashtable containing the input codes (keys) and the output codes (values).
     */
    public Transcoder(Hashtable<String,String> ht){
        this.ht=directHt=ht;
        makeReverseHt();
    }

    /**
     * Constructor that receives the patterns for transcodification in a hashtable, reverting keys and values.
     * The relation between keys and values should be biunivocal.
     *
     * @param ht the hashtable containing the input codes (keys) and the output codes (values).
     * @param reverse false: do not reverse the keys and values; true: reverse keys and values.
     */
    public Transcoder(Hashtable<String,String> ht, boolean reverse){
        this.ht=directHt=ht;
        makeReverseHt();
        reverse(reverse);
    }

    /**
     * Constructor that receives the name of the file containing the table of patterns for transcodification
     * and a boolean parameter to switch left side and right side codes of the table.
     *
     * @param transFileName
     * @param reverse
     */
    public Transcoder(String transFileName, boolean reverse){
        setTranscoder(transFileName);
        reverse(reverse);
    }

    /**
     * Reverse (or not) keys and values of the hashtable containing the codes for transcodification.
     *
     * @param reverse false: do not reverse the keys and values; true: reverse keys and values.
     */
    public void reverse(boolean reverse){
        if(reverse){
            ht=reverseHt;
        }else{
            ht=directHt;
        }
    }

    /**
     * Reverse keys and values of the hashtable containing the codes for transcodification.
     */
    public void reverse(){
        ht=reverseHt;
    }

    /**
     * Set the name of the file containing the table for transcodification, typically in the form:<br/>
     * <code>\u005CuXXXX+ \u005Ct \u005CuYYYY+</code>,<br/>
     * i.e. one or more unicode utf-8 chars or codes, followed by tabulation,
     * followed by one or more utf-8 chars or codes<br/>
     * \u005Cuxxxx (i.e. backslash followed by <code>u</code> followed by four lowercase <code>x</code>)
     * on the right side of tabulation
     * means that the code(s) on the left side must be cancelled.
     * Examples of transcoder files are stored in resources/transcoders.
     *
     * @param transFileName
     */
    public void setTranscoder(String transFileName){
        try {
            /* read code file, transform utf8s in chars and put
            into a hashtable the sequences of char codes */
            File transFile = new File(transFileName);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(transFile), "UTF-8"));
        }catch(Exception ex){ex.printStackTrace();}
        makeTranscoder();
    }

    public void setTranscoder(InputStream transIs){
        try{
            br = new BufferedReader(new InputStreamReader(transIs, "UTF-8"));
        }catch(Exception ex){ex.printStackTrace();}
        makeTranscoder();
    }

    public void makeTranscoder(){
        try{
            String codeFrom=null;
            String codeTo=null;
            directHt = new Hashtable<String, String>();
            reverseHt = new Hashtable<String, String>();
            String line=null;
            String lineLeft = "";
            String lineRight = "";
            String[] items=null;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("//")) {
                    items=line.split("\t");
                    lineLeft=items[0];
                    lineRight=items[1];
                    if (lineLeft.contains("\\u")) {
                        codeFrom = utf8sToChars(lineLeft);
                    } else {
                        codeFrom = lineLeft;
                    }
                    if (codeFrom.length() > maxLen) {
                        maxLen = codeFrom.length();
                    }
                    if(lineRight.contains("\\u")){
                        codeTo = utf8sToChars(lineRight);
                    }else{
                        codeTo=lineRight;
                    }
                    directHt.put(codeFrom, codeTo);
                    reverseHt.put(codeTo, codeFrom);
                }
            }
            ht=directHt;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the name of the file containing the table for transcodification and a boolean to exchange (or not)
     * left side codes with right side codes.
     *
     * @param transFileName
     * @param reverse
     *
     * @see #setTranscoder(java.lang.String)
     */
    public void setTranscoder(String transFileName, boolean reverse){
            setTranscoder(transFileName);
            reverse(reverse);
        }

    /**
     * Set the hashtable containing the codes for transcodification.
     *
     * @param ht hashtable.
     */
    public void setTranscoder(Hashtable<String,String> ht){
        this.ht=directHt=ht;
        makeReverseHt();
    }

    /**
     * Get the hashtable containing the codes for transcodification.
     * @return the hashtable.
     */
    public Hashtable<String,String> getTranscoder(){
        return ht;
    }

    /**
     * Get the hashtable containing the codes (not affected by reversing operations).
     *
     * @return the hashtable.
     */
    public Hashtable<String,String> getDirectTranscoder(){
        return directHt;
    }

    /**
     * Get the hashtable containing the codes, inverting keys and values.
     *
     * @return the reversed (i.e. values to keys) hashtable.
     */
    public Hashtable<String,String> getReverseTranscoder(){
        return reverseHt;
    }

    /**
     * Get the complete table of codes for transcodification, dividing keys and values by a tabulation, line by line.
     *
     * @return the string containing the formatted table of codes.
     */
    public String getCodes() {
        String res="";
        Set<String> keys=ht.keySet();
        String key="";
        for(Iterator<String> i=keys.iterator();i.hasNext();){
            key=i.next();
            res+=key+"\t"+ht.get(key)+"\n";
        }
        return res;
    }

    /**
     * Given a key, return the value contained in the hashtable.
     *
     * @param key the key that must be transcoded
     * @return the transcoded result
     */
    public String decode(String key){
        return ht.get(key);
    }

    /**
     * Given a key character, return the value contained in the hashtable.
     *
     * @param key the key character that must be transcoded
     * @return the transcoded result
     */
    public String decode(char key){
        return ht.get(""+key);
    }

    /**
     * Transcode \u005CuXXXX unicode point code sequences in character sequences. \u005Cuxxxx special code is transformed in an empty string.
     *
     * @param str string containing unicode points in \u005CuXXXX notation
     * @return sequence of unicode characters
     */
    private String utf8sToChars(String str) {
        if(str.equals("\\uxxxx")) return "";
        StringBuffer result =new StringBuffer();
        Pattern p=Pattern.compile("(?:\\\\)u([0-9A-Fa-f]{4})");
        Matcher m=p.matcher(str);
        while(m.find()){
            m.appendReplacement(result,""+((char)Long.parseLong(m.group(1),16)));
        }
        m.appendTail(result);
        return result.toString();
    }

    /**
     * Parse the input string and return the transcoded result.
     *
     * @param inStr input string
     * @return the transcoded output
     */
    public String parse(String inStr){
        if(inStr==null||inStr.length()==0) return "";
        inStr += addSpaces(maxLen);
        String frag = "";
        String outStr = "";
        String code = "";
        int len = inStr.length();
        int iLeft = 0;
        int iRight = maxLen;
        while(iRight <= len){
            while(iRight > iLeft){
                frag = inStr.substring(iLeft, iRight);
                code = ht.get(frag);
                if (code != null) {
                    outStr += code;
                    break;
                }
                iRight--;
            }
            if(iRight==iLeft){
                iRight+=1;
                outStr+=frag;
            }
            iLeft = iRight;
            iRight += maxLen;
        }
        if(outStr.length()>1){
            return outStr.substring(0,outStr.length()-1);
        }else return outStr;
    }

    /**
     * Creates a string with <code>n</code> spaces.
     *
     * @param n number of spaces
     * @return the sequence of <code>n</code> spaces
     */
    private String addSpaces(int n) {
        String res = "";
        for (int i = 0; i < n; i++) {
            res += " ";
        }
        return res;
    }

    /**
     * Parse a string containing starting and ending tags to isolate the chunks that must be transcoded.
     *
     * @param inStr input string.
     * @param regexp regular expression to determine the starting and ending tags that surround the substring to transcode (e.g. "\\&lt;greek\\&gt;(.*?)\\&lt;/greek\\&gt;").
     * @return transcoded string.
     */
    public String parse(String inStr, String regexp){
         if(pattern==null||(regexp!=null&&regexp.equals(this.regexp))){
             this.regexp=regexp;
             pattern=Pattern.compile(regexp);
         }
        Matcher matcher=pattern.matcher(inStr);
        StringBuffer sb=new StringBuffer();
        while(matcher.find()){
            if(matcher.groupCount()==3){
                matcher.appendReplacement(sb, matcher.group(1)+parse(matcher.group(2)+matcher.group(3)));
            }else{
                matcher.appendReplacement(sb, parse(matcher.group(1)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Parse a string containing starting and ending tags to isolate the chunks that must be transcoded.
     *
     * @param inStr input string.
     * @param startTagRegexp regular expression to determine the starting tag (e.g. "\\&lt;greek\\&gt;").
     * @param endTagRegexp regular expression to determine the ending tag (e.g. "\\&lt;/greek\\&gt;").
     * @param del cancel (or not) the starting and ending tags.
     * @return transcoded string.
     */
    public String parse(String inStr, String startTagRegexp, String endTagRegexp, boolean del){
        if(!del){
            startTagRegexp="("+startTagRegexp+")";
            endTagRegexp="("+endTagRegexp+")";
        }
        return parse(inStr, startTagRegexp+"(.*?)"+endTagRegexp);
    }

    /**
     * Reverses keys and values in the hashtable containing the codes for transcodification.
     */
    private void makeReverseHt(){
        String val=null;
        String key=null;
        Enumeration<String> keys=directHt.keys();
        while(keys.hasMoreElements()){
            key=keys.nextElement();
            val=directHt.get(key);
            reverseHt.put(val, key);
        }
    }

    /**
     * In case of wrong parameters provided to the <code>main</code> method, print usage.
     */
    private static void printUsage(){
            System.out.println("usage 1: eu.himeros.transcoder.Transcoder <inFile> <outFile> <transFile>");
            System.out.println("usage 2: eu.himeros.transcoder.Transcoder <inFile> <outFile> <transFile> <regexp>");
            System.out.println("usage 3: eu.himeros.transcoder.Transcoder <inFile> <outFile> <transFile> <startTagRegexp> <endTagRegexp> <booleanDelTag>");
    }

    /**
     * Main method that receives the following args:
     * <code>inFile</code> - the file to transcode<br/>
     * <code>outFile</code> - the output file<br/>
     * <code>transFile</code> - the table for transcodification, typically in the form:<br/>
     * <code>\u005CuXXXX+ \u005Ct \u005CuYYYY+</code>,<br/>
     * i.e. one or more unicode utf-8 chars or codes, followed by tabulation,
     * followed by one or more utf-8 chars or codes<br/>
     * \u005Cuxxxx (i.e. backslash followed by <code>u</code> followed by four lowercase <code>x</code>)
     * on the right side of tabulation
     * means that the code(s) on the left side must be cancelled.
     * <code>booleanReverse</code> - false: left codes transformed in right codes; true: right codes transformed in left codes.<br/>
     * <code>startTagRegex</code> - regex that determine the starting tag that delimits the substring to transcode.<br/>
     * <code>endTagRegex</code> - regex that determine the ending tag that delimits the substring to transcode.<br/>
     * <code>booleanDelTag</code> - Delete (or not) the tag containing the substring to transcode.<br/>
     *
     * @param args <code>inFile</code> <code>outFile</code> <code>transFile</code> <code>reverseBool</code> (<code>triggerTag</code>||<code>triggerAttr</code>=<code>Val</code>)
     *
     */
    public static void main(String[] args){
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8"));
            String line=null;
            Transcoder trans=new Transcoder(args[2]);
            while((line=br.readLine())!=null){
                if(args.length==3){
                    bw.write(trans.parse(line));bw.newLine();
                }else if(args.length==4){
                    bw.write(trans.parse(line,args[3]));
                }else{
                    bw.write(trans.parse(line,args[3],args[4],Boolean.parseBoolean(args[5])));
                }
            }
            bw.close();
        }catch(Exception ex){
            printUsage();
            ex.printStackTrace();
        }
    }

}