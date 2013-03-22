/*
 * This software is licensed under GPLv3 <http://www.gnu.org/licenses/gpl-3.0.html>
 * 
 */

package eu.himeros.ocr;

/**
 *
 * @author Federico Boschetti <federico.boschetti.73@gmail.com>
 */
public abstract class AdjustOcr {
    protected String[] adjusterNames;
    
    public AdjustOcr(){}

    public abstract void adjust(String inFileName, String outFileName);
    
    public abstract String adjust(String text);
    
    public static AdjustOcr newInstance(String className){
        try{
            return (AdjustOcr)Class.forName(className).getConstructor().newInstance();
        }catch(Exception ex){
            return null;
        }
    }
    
    public String[] getAdjusterNames() {
        return adjusterNames;
    }

    public void setAdjusterNames(String[] adjusterNames) {
        this.adjusterNames = adjusterNames;
    }
    
    public abstract void makeAdjusters(String[] adjusterNames);

}
