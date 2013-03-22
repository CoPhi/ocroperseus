/*
 * This software is licensed under GPLv3 <http://www.gnu.org/licenses/gpl-3.0.html>
 * 
 */

/*
 * OcroPerseus.java
 *
 * Created on Apr 16, 2009, 10:13:24 AM
 */
package eu.himeros.ocr.gui;

import eu.himeros.alignment.AlignFormatter;
import eu.himeros.alignment.AnchorFinder;
import eu.himeros.alignment.MultiAligner;
import eu.himeros.alignment.UpperCaseSimEvaluator;
import eu.himeros.ocr.AdjustClassicalOcr;
import eu.himeros.ocr.AdjustOcr;
import eu.himeros.ocr.ErrorNgramEvaluator;
import eu.himeros.ocr.ErrorPatternMaker;
import eu.himeros.spellchecker.GrcStringDistance;
import eu.himeros.spellchecker.LuceneSpellChecker;
import java.awt.Cursor;
import java.awt.Desktop;
import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

/**
 *
 * @author federico
 */
public class OcroPerseus extends javax.swing.JFrame {

    private final int ENGS = 3; //number of engines
    private File f = null;
    private String propFileName = null;
    private Properties props = null;
    private Properties sysProps = null;
    private JTextField currentJTextField = null;
    private Desktop desk = null;
    private Logger log = Logger.getLogger(OcroPerseus.class.getName());
    private BufferedReader propFileBr = null;
    private BufferedWriter propFileBw = null;
    private String adjPrefix = "";
    private Task task = null;
    private boolean aborted=false;
    private String command=null;

    private class Task extends SwingWorker<Void, Void> {

        @Override
        public Void doInBackground() throws IOException {
            int opTot=1;
            int currOp=1;
            if(jCheckBox4.isSelected()) opTot++;
            if(jCheckBox6.isSelected()) opTot++;
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            if(command.equals("Align")){
                freezeForAlignment(true);
                jProgressBar1.setStringPainted(true);
                // write properties to project.properties
                writeProps();
                //adjust ocr
                if (jCheckBox1.isSelected()) {
                    adjPrefix = "_";
                    adjustOcr();
                } else {
                    adjPrefix = "";
                }
                //make error patterns
                if (jCheckBox4.isSelected()) {
                    jLabel7.setText(""+currOp+"/"+opTot+" Making Error Patterns");
                    makeErrorPatterns();
                    currOp++;
                }
                //aligned merging
                jLabel7.setText(""+currOp+"/"+opTot+" Making Alignments");
                align();
                currOp++;
                //evaluate accuracy
                if(jCheckBox6.isSelected()&&!aborted){
                    jLabel7.setText(""+currOp+"/"+opTot+" Making Accuracy Reports");
                    evaluateAccuracy();
                }
                jLabel7.setText("");
                aborted=false;
                jProgressBar1.setStringPainted(false);
                jButton15.setActionCommand("Align");
                freezeForAlignment(false);
            }else if(command.equals("Compare")){
                jProgressBar2.setStringPainted(true);
                compare();
                jProgressBar2.setStringPainted(false);
                jButton17.setActionCommand("Compare");
            }
            setCursor(Cursor.getDefaultCursor());
            return null;
        }

        @Override
        public void done() {
            log.info("done!");
            if(command.equals("Align")){
                jProgressBar1.setValue(0);
                jProgressBar1.setStringPainted(false);
                jButton15.setActionCommand("Align");
                jButton15.setText("Align");
            }else if(command.equals("Compare")){
                jProgressBar2.setValue(0);
                jProgressBar2.setStringPainted(false);
                jButton17.setActionCommand("Compare");
                jButton17.setText("Compare");
            }
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /** Creates new form OcroPerseus */

    public OcroPerseus(String wd){
        System.setProperty("eu.himeros.user.dir", wd);
        init();
    }

    public OcroPerseus() {
        init();
    }
    private void init(){
        //Jaspeller.init();
        AlignFormatter.setGapTag("\u00B0");
        sysProps = new Properties();
        try {
            //sysProps.load(this.getClass().getResourceAsStream("/eu/himeros/resources/properties/init.properties"));
            sysProps.load(new FileInputStream(System.getProperty("wd")+System.getProperty("file.separator")+"init.properties"));
            // Store user.home as a custom property in case of reuse
            System.setProperty("eu.himeros.user.home", System.getProperty("user.home"));
            // Copy the working directory user.dir as user.home
            System.setProperty("user.home", System.getProperty("wd"));
            System.setProperty("user.dir", System.getProperty("wd"));
            for (String key : sysProps.stringPropertyNames()) {
                System.setProperty(key, sysProps.getProperty(key));
            }
            UpperCaseSimEvaluator.setResourceName(System.getProperty("eu.himeros.alignment.evaluator"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        GrcStringDistance sd=new GrcStringDistance();
        LuceneSpellChecker.init(System.getProperty("eu.himeros.spellcheckers"));
        props = new Properties();
        log.setLevel(Level.FINE);
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
            if (Desktop.isDesktopSupported()) {
                desk = Desktop.getDesktop();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        initComponents();
        enableAll(false);
        jDialog1.setVisible(false);
        jTextPane1.setContentType("text/html");
        String html="<html><head><title>OcroPerseus</title></head><body><center><h1><font color=\"red\">OcroPerseus</font></h1></center>"+
            "<center><h2>Tools for Multiple Alignment and Accuracy Evaluation<br/><font size=\"-1\">by Federico Boschetti &lt;federico.boschetti.73@gmail.com&gt;</font></h2></center>"+
            "<font color=\"blue\"><b>Alignment Tab</b></font><br/>"+
            "The <b>Project File</b> contains all the properties of the project, in particular the list of document directories.<br/>"+
            "A new Project File is created providing the name of a working directory or a Project File is loaded providing the name of an existing file.<br/>"+
            "<b>Images Dir</b> contains the scanned pages of a document, <b>1st</b>, <b>2nd</b> and <b>3d OCR Egine Dir</b>s contains the output of different engines on the same documents." +
            "OcroPerseus is based on the rule that aligned and/or compared files <i>must</i> have the same names.<br/>"+
            "<b>Training Ground Truth Dir</b> contains a small amount of documents corrected by hand (usually 5-10), corresponding to some documents in the OCR directories, "+
            "that are used to generate the Error Patterns in the <b>Error Patterns Dir</b>. To avoid Error Pattern Generation and/or to use existing patterns, it is necessary to uncheck <b>Make Error Patterns</b>.<br/>"+
            "<b>Aligned Output Dir</b> contains the result of triple, spellchecked alignment<br/>"+
            "<b>Accuracy Ground Truth Dir</b> contains some documents corrected by hand, corresponding to some OCR documents, for the evaluation of the accuracy. Reports are collected in the <b>Accuracy Reports Dir</b>. "+
            "To avoid the generation of reports, it is necessary to uncheck <b>Make Accuracy Reports</b>. "+
            "Optionally, Alignment Reports can be created checking <b>Make Alignment Reports</b>.<br/> "+
            "<b>Adjust Ocr</b> is checked by default, to normalize Greek and Latin OCR output. "+
            "Self generated directory (for OCR adjustment, alignment reports, etc.) are preceded by an underscore."+
            "<br/><br/><font color=\"blue\"><b>Comparison Tab</b></font></color><br/>"+
            "<b>Target Dir</b> contains the documents that must be evaluated; <b>Standard Dir</b> contains the Ground Truth (or other standards for comparison) and <b>Result Dir contains the generated accuracy reports.</b>"+
            "</body></html>";
        jTextPane1.setText(html);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jDialog1 = new javax.swing.JDialog();
        jFileChooser1 = new javax.swing.JFileChooser();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jButton4 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jTextField14 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        jTextField13 = new javax.swing.JTextField();
        jTextField10 = new javax.swing.JTextField();
        jTextField11 = new javax.swing.JTextField();
        jButton6 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jLabel14 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jTextField2 = new javax.swing.JTextField();
        jTextField1 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jButton15 = new javax.swing.JButton();
        jButton16 = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        jCheckBox4 = new javax.swing.JCheckBox();
        jProgressBar1 = new javax.swing.JProgressBar();
        jCheckBox5 = new javax.swing.JCheckBox();
        jCheckBox6 = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jButton7 = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jTextField7 = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jTextField8 = new javax.swing.JTextField();
        jButton8 = new javax.swing.JButton();
        jLabel16 = new javax.swing.JLabel();
        jButton9 = new javax.swing.JButton();
        jTextField9 = new javax.swing.JTextField();
        jButton17 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jProgressBar2 = new javax.swing.JProgressBar();
        jLabel8 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        cutMenuItem = new javax.swing.JMenuItem();
        copyMenuItem = new javax.swing.JMenuItem();
        pasteMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        contentsMenuItem = new javax.swing.JMenuItem();
        aboutMenuItem = new javax.swing.JMenuItem();

        jDialog1.setLocationByPlatform(true);
        jDialog1.setMinimumSize(new java.awt.Dimension(800, 439));
        jDialog1.setModal(true);

        jFileChooser1.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        jFileChooser1.setMaximumSize(new java.awt.Dimension(459, 600));
        jFileChooser1.setMinimumSize(new java.awt.Dimension(459, 600));
        jFileChooser1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFileChooser1ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jDialog1Layout = new org.jdesktop.layout.GroupLayout(jDialog1.getContentPane());
        jDialog1.getContentPane().setLayout(jDialog1Layout);
        jDialog1Layout.setHorizontalGroup(
            jDialog1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jDialog1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jFileChooser1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 794, Short.MAX_VALUE)
                .addContainerGap())
        );
        jDialog1Layout.setVerticalGroup(
            jDialog1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jDialog1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jFileChooser1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(105, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jButton4.setText("...");
        jButton4.setActionCommand("4");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText("Training Ground Truth Dir:");

        jButton2.setText("...");
        jButton2.setActionCommand("2");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("...");
        jButton3.setActionCommand("3");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setText("2nd OCR Engine Dir:");

        jTextField14.setEditable(false);

        jLabel5.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText("3d OCR Engine Dir:");

        jButton1.setText("...");
        jButton1.setActionCommand("1");
        jButton1.setName("propdir"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel10.setText("Error Patterns Dir:");

        jTextField13.setEditable(false);

        jTextField10.setEditable(false);

        jTextField11.setEditable(false);

        jButton6.setText("...");
        jButton6.setActionCommand("6");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jButton5.setText("...");
        jButton5.setActionCommand("5");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jLabel14.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel14.setText("Accuracy Reports Dir:");

        jLabel13.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel13.setText("Accuracy Ground Truth Dir:");

        jLabel11.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel11.setText("Aligned Output Dir:");

        jButton10.setText("...");
        jButton10.setActionCommand("10");
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        jButton11.setText("...");
        jButton11.setActionCommand("11");
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        jButton13.setText("...");
        jButton13.setActionCommand("13");
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });

        jButton14.setText("...");
        jButton14.setActionCommand("14");
        jButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton14ActionPerformed(evt);
            }
        });

        jTextField2.setEditable(false);

        jTextField1.setName("propdir"); // NOI18N

        jLabel1.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("Project File:");

        jTextField6.setEditable(false);

        jTextField5.setEditable(false);

        jLabel3.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("1st OCR Engine Dir:");

        jTextField4.setEditable(false);

        jLabel2.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Images Dir:");

        jTextField3.setEditable(false);

        jButton15.setText("Align");
        jButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton15ActionPerformed(evt);
            }
        });

        jButton16.setText("Reset");
        jButton16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton16ActionPerformed(evt);
            }
        });

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("AdjustOcr");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jCheckBox4.setText("Make Error Patterns");
        jCheckBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox4ActionPerformed(evt);
            }
        });

        jCheckBox5.setText("Make Alignment Reports");
        jCheckBox5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox5ActionPerformed(evt);
            }
        });

        jCheckBox6.setText("Make Accuracy Reports");
        jCheckBox6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox6ActionPerformed(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("DejaVu Sans", 0, 24));
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("Alignment");

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 144, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 144, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 157, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 144, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel6)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel11, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel13)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 173, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jCheckBox4)
                    .add(jCheckBox1)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jLabel9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                            .add(jTextField6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                            .add(jTextField5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                            .add(jTextField4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                            .add(jTextField3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                            .add(jTextField2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                            .add(jTextField1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                            .add(jTextField10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                            .add(jTextField11, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                            .add(jTextField13, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                            .add(jTextField14, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                    .add(jProgressBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(jCheckBox6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 195, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .add(jCheckBox5))
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                        .add(jButton15)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jButton16)))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 38, Short.MAX_VALUE)
                                .add(jLabel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 193, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .add(6, 6, 6)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jButton10)
                            .add(jButton11)
                            .add(jButton14)
                            .add(jButton13)
                            .add(jButton2)
                            .add(jButton1)
                            .add(jButton3)
                            .add(jButton4)
                            .add(jButton5)
                            .add(jButton6))))
                .add(32, 32, 32))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(25, 25, 25)
                .add(jLabel9)
                .add(18, 18, 18)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton1)
                    .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jTextField2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton3)
                    .add(jTextField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton4)
                    .add(jTextField4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel4))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton5)
                    .add(jTextField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel5))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton6)
                    .add(jTextField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel6))
                .add(8, 8, 8)
                .add(jCheckBox4)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton10)
                    .add(jTextField10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel10))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton11)
                    .add(jTextField11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel11))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton13)
                    .add(jTextField13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel13))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jButton14)
                            .add(jTextField14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel14))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jCheckBox6)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jCheckBox5)
                        .add(6, 6, 6))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(37, 37, 37)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jButton15)
                            .add(jButton16))))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jCheckBox1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jLabel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jProgressBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Align", jPanel2);

        jButton7.setText("...");
        jButton7.setActionCommand("1");
        jButton7.setName("<none>"); // NOI18N
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jLabel12.setFont(new java.awt.Font("DejaVu Sans", 1, 13)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel12.setText("Target Dir:");

        jTextField7.setEditable(false);
        jTextField7.setName("comp1"); // NOI18N

        jLabel15.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel15.setText("Standard Dir:");

        jTextField8.setEditable(false);
        jTextField8.setName("comp2"); // NOI18N

        jButton8.setText("...");
        jButton8.setActionCommand("1");
        jButton8.setName("<none>"); // NOI18N
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jLabel16.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel16.setText("Result Dir:");

        jButton9.setText("...");
        jButton9.setActionCommand("1");
        jButton9.setName("<none>"); // NOI18N
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        jTextField9.setName("comp3"); // NOI18N

        jButton17.setText("Compare");
        jButton17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton17ActionPerformed(evt);
            }
        });

        jButton18.setText("Reset");
        jButton18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton18ActionPerformed(evt);
            }
        });

        jLabel8.setFont(new java.awt.Font("DejaVu Sans", 0, 24)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("Comparison");

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .addContainerGap(80, Short.MAX_VALUE)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(jLabel15)
                        .add(18, 18, 18)
                        .add(jTextField8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 523, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(jButton8))
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(jLabel16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 80, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel6Layout.createSequentialGroup()
                                .add(jTextField9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 523, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(jButton9))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, jProgressBar2, 0, 0, Short.MAX_VALUE)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel6Layout.createSequentialGroup()
                                    .add(jButton17)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                    .add(jButton18)))))
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(jLabel12, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 80, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jTextField7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 523, Short.MAX_VALUE))
                        .add(18, 18, 18)
                        .add(jButton7)))
                .add(61, 61, 61))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(31, 31, 31)
                .add(jLabel8)
                .add(28, 28, 28)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton7)
                    .add(jTextField7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel12))
                .add(18, 18, 18)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton8)
                    .add(jTextField8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel15))
                .add(18, 18, 18)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton9)
                    .add(jTextField9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel16))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton17)
                    .add(jButton18))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jProgressBar2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(346, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Compare", jPanel6);

        jTextPane1.setContentType("text/html\n");
        jTextPane1.setEditable(false);
        jScrollPane1.setViewportView(jTextPane1);
        jTextPane1.getAccessibleContext().setAccessibleDescription("text/html");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(23, 23, 23)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 779, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(21, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(62, Short.MAX_VALUE)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 515, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(45, 45, 45))
        );

        jTabbedPane1.addTab("Help", jPanel1);

        fileMenu.setText("File");

        openMenuItem.setText("Open");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);

        saveMenuItem.setText("Save");
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setText("Save As ...");
        fileMenu.add(saveAsMenuItem);

        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setText("Edit");

        cutMenuItem.setText("Cut");
        cutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cutMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(cutMenuItem);

        copyMenuItem.setText("Copy");
        editMenu.add(copyMenuItem);

        pasteMenuItem.setText("Paste");
        editMenu.add(pasteMenuItem);

        deleteMenuItem.setText("Delete");
        editMenu.add(deleteMenuItem);

        menuBar.add(editMenu);

        helpMenu.setText("Help");

        contentsMenuItem.setText("Contents");
        helpMenu.add(contentsMenuItem);

        aboutMenuItem.setText("About");
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 827, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 653, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void cutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cutMenuItemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cutMenuItemActionPerformed

    private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
        jDialog1.setResizable(false);
        jDialog1.setAlwaysOnTop(true);
        jDialog1.pack();
        jDialog1.setVisible(true);
    }//GEN-LAST:event_openMenuItemActionPerformed

    private void jFileChooser1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFileChooser1ActionPerformed
        String selFilePath = "";
        if (evt.getActionCommand().equals("ApproveSelection")) {
            selFilePath = (f = jFileChooser1.getSelectedFile()).getAbsolutePath();
            if (currentJTextField.getName()!=null&&currentJTextField.getName().equals("propdir")) {
                if (f.isFile()) {
                    propFileName = f.getAbsolutePath();
                    try {
                        readProps();
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(OcroPerseus.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    propFileName = selFilePath += File.separator + "project.properties";
                    File propFile=new File(propFileName);
                    if(propFile.exists()){
                        try {
                            readProps();
                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(OcroPerseus.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                enableAll(true);
                try {
                    writeProps();
                } catch (IOException ex) {
                    Logger.getLogger(OcroPerseus.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if(currentJTextField.getName()!=null&&!currentJTextField.getName().startsWith("comp")){
                enableAll(true);
                try {
                    writeProps();
                } catch (IOException ex) {
                    Logger.getLogger(OcroPerseus.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            currentJTextField.setText(selFilePath);
            jFileChooser1.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        jDialog1.setVisible(false);
    }//GEN-LAST:event_jFileChooser1ActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        try {
            writeProps();
        } catch (IOException ex) {
            Logger.getLogger(OcroPerseus.class.getName()).log(Level.SEVERE, null, ex);
        }
}//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton16ActionPerformed
        resetProps();
}//GEN-LAST:event_jButton16ActionPerformed

    private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton15ActionPerformed
        if(evt.getActionCommand().equals("Align")){
            command="Align";
            jButton15.setActionCommand("Stop");
            jButton15.setText("Stop");
            task = new Task();
            task.execute();
        }else if(evt.getActionCommand().equals("Stop")){
            jButton15.setActionCommand("Align");
            jButton15.setText("Align");
            aborted=true;
        }
}//GEN-LAST:event_jButton15ActionPerformed

    private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton14ActionPerformed
        currentJTextField = jTextField14;
        jDialog1.setVisible(true);
}//GEN-LAST:event_jButton14ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
        currentJTextField = jTextField13;
        jDialog1.setVisible(true);
}//GEN-LAST:event_jButton13ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        currentJTextField = jTextField11;
        jDialog1.setVisible(true);
}//GEN-LAST:event_jButton11ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        currentJTextField = jTextField10;
        jDialog1.setVisible(true);
}//GEN-LAST:event_jButton10ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        currentJTextField = jTextField5;
        jDialog1.setVisible(true);
}//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        currentJTextField = jTextField6;
        jDialog1.setVisible(true);
}//GEN-LAST:event_jButton6ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        jFileChooser1.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        currentJTextField = jTextField1;
        jDialog1.setVisible(true);
}//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        currentJTextField = jTextField3;
        jDialog1.setVisible(true);
}//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        currentJTextField = jTextField2;
        jDialog1.setVisible(true);
}//GEN-LAST:event_jButton2ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        currentJTextField = jTextField4;
        jDialog1.setVisible(true);
}//GEN-LAST:event_jButton4ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        jFileChooser1.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        currentJTextField = jTextField7;
        jDialog1.setVisible(true);
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        jFileChooser1.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        currentJTextField = jTextField8;
        jDialog1.setVisible(true);
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        jFileChooser1.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        currentJTextField = jTextField9;
        jDialog1.setVisible(true);
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton17ActionPerformed
        command="Compare";
        if(evt.getActionCommand().equals("Compare")){
            jButton17.setActionCommand("Stop");
            jButton17.setText("Stop");
            task = new Task();
            task.execute();
        }else if(evt.getActionCommand().equals("Stop")){
            jButton17.setActionCommand("Compare");
            jButton17.setText("Compare");
            aborted=true;
        }
    }//GEN-LAST:event_jButton17ActionPerformed

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton18ActionPerformed
        resetComps();
    }//GEN-LAST:event_jButton18ActionPerformed

    private void jCheckBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox4ActionPerformed
        jTextField6.setEnabled(jCheckBox4.isSelected());
        jButton6.setEnabled(jCheckBox4.isSelected());
        try {
            writeProps();
        } catch (IOException ex) {
            Logger.getLogger(OcroPerseus.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBox4ActionPerformed

    private void jCheckBox5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox5ActionPerformed
        try {
            writeProps();
        } catch (IOException ex) {
            Logger.getLogger(OcroPerseus.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBox5ActionPerformed

    private void jCheckBox6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox6ActionPerformed
        jTextField13.setEnabled(jCheckBox6.isSelected());
        jButton13.setEnabled(jCheckBox6.isSelected());
        jTextField14.setEnabled(jCheckBox6.isSelected());
        jButton14.setEnabled(jCheckBox6.isSelected());
        try {
            writeProps();
        } catch (IOException ex) {
            Logger.getLogger(OcroPerseus.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBox6ActionPerformed

    private void readProps() throws FileNotFoundException {
        propFileBr = new BufferedReader(new FileReader(propFileName));
        try {
            props.load(propFileBr);
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
        String basedirSep=new File(propFileName).getParent()+"/";
        jTextField2.setText(basedirSep+props.getProperty("eu.himeros.ocr.images.dir"));
        jTextField3.setText(basedirSep+props.getProperty("eu.himeros.ocr.e1.dir"));
        jTextField4.setText(basedirSep+props.getProperty("eu.himeros.ocr.e2.dir"));
        jTextField5.setText(basedirSep+props.getProperty("eu.himeros.ocr.e3.dir"));
        jTextField6.setText(basedirSep+props.getProperty("eu.himeros.ocr.traininggroundtruth.dir"));
        jTextField10.setText(basedirSep+props.getProperty("eu.himeros.ocr.errorpatterns.dir"));
        jTextField11.setText(basedirSep+props.getProperty("eu.himeros.ocr.alignment.dir"));
        jTextField13.setText(basedirSep+props.getProperty("eu.himeros.ocr.accuracygroundtruth.dir"));
        jTextField14.setText(basedirSep+props.getProperty("eu.himeros.ocr.accuracyreports.dir"));
        jCheckBox4.setSelected(Boolean.parseBoolean(props.getProperty("eu.himeros.ocr.overwriteerrorpatterns")));
        jCheckBox1.setSelected(Boolean.parseBoolean(props.getProperty("eu.himeros.ocr.adjustocr")));
        jCheckBox5.setSelected(Boolean.parseBoolean(props.getProperty("eu.himeros.ocr.algreps")));
        jCheckBox6.setSelected(Boolean.parseBoolean(props.getProperty("eu.himeros.ocr.accreps")));
        try {
            propFileBr.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    private void writeProps() throws IOException {
        String jtf1 = jTextField1.getText();
        if (jtf1 != null && !jtf1.equals("") && !jtf1.equals(propFileName)) {
            propFileName = jTextField1.getText();
        }
        propFileBw = new BufferedWriter(new FileWriter(propFileName));
        props.setProperty("eu.himeros.ocr.images.dir", jTextField2.getText());
        props.setProperty("eu.himeros.ocr.e1.dir", jTextField3.getText());
        props.setProperty("eu.himeros.ocr.e2.dir", jTextField4.getText());
        props.setProperty("eu.himeros.ocr.e3.dir", jTextField5.getText());
        props.setProperty("eu.himeros.ocr.traininggroundtruth.dir", jTextField6.getText());
        props.setProperty("eu.himeros.ocr.errorpatterns.dir", jTextField10.getText());
        props.setProperty("eu.himeros.ocr.alignment.dir", jTextField11.getText());
        props.setProperty("eu.himeros.ocr.accuracygroundtruth.dir", jTextField13.getText());
        props.setProperty("eu.himeros.ocr.accuracyreports.dir", jTextField14.getText());
        props.setProperty("eu.himeros.ocr.overwriteerrorpatterns", Boolean.toString(jCheckBox4.isSelected()));
        props.setProperty("eu.himeros.ocr.adjustocr", Boolean.toString(jCheckBox1.isSelected()));
        props.setProperty("eu.himeros.ocr.algreps", Boolean.toString(jCheckBox5.isSelected()));
        props.setProperty("eu.himeros.ocr.accreps", Boolean.toString(jCheckBox6.isSelected()));
        try {
            //props.store(propFileBw, "OcroPerseus Properties");
            propFileBw.write("eu.himeros.ocr.image.dir="+new File(jTextField2.getText()).getName());propFileBw.newLine();
            propFileBw.write("eu.himeros.ocr.e1.dir="+new File(jTextField3.getText()).getName());propFileBw.newLine();
            propFileBw.write("eu.himeros.ocr.e2.dir="+new File(jTextField4.getText()).getName());propFileBw.newLine();
            propFileBw.write("eu.himeros.ocr.e3.dir="+new File(jTextField5.getText()).getName());propFileBw.newLine();
            propFileBw.write("eu.himeros.ocr.traininggroundtruth.dir="+new File(jTextField6.getText()).getName());propFileBw.newLine();
            propFileBw.write("eu.himeros.ocr.errorpatterns.dir="+new File(jTextField10.getText()).getName());propFileBw.newLine();
            propFileBw.write("eu.himeros.ocr.alignment.dir="+new File(jTextField11.getText()).getName());propFileBw.newLine();
            propFileBw.write("eu.himeros.ocr.accuracygroundtruth.dir="+new File(jTextField13.getText()).getName());propFileBw.newLine();
            propFileBw.write("eu.himeros.ocr.accuracyreports.dir="+new File(jTextField14.getText()).getName());propFileBw.newLine();
            propFileBw.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    private void resetProps() {
        jTextField1.setText("");
        jTextField2.setText("");
        jTextField3.setText("");
        jTextField4.setText("");
        jTextField5.setText("");
        jTextField6.setText("");
        jTextField10.setText("");
        jTextField11.setText("");
        jTextField13.setText("");
        jTextField14.setText("");
        jCheckBox4.setSelected(false);
        jCheckBox5.setSelected(false);
        jCheckBox6.setSelected(false);
    }

    private void resetComps() { //reset comparison dirs
        jTextField7.setText("");
        jTextField8.setText("");
        jTextField9.setText("");
    }

    private void adjustOcr() {
        //AdjustOcr Button
        String fileName;
        String adjFileName;
        File adjf;
        AdjustOcr adjocr;
        //try{
            //adjocr = new AdjustClassicalOcr(ClassLoader.getSystemResourceAsStream(System.getProperty("eu.himeros.ocr2grk")), ClassLoader.getSystemResourceAsStream(System.getProperty("eu.himeros.ocr2lat")));
        //}catch(Exception ex){
            //adjocr = new AdjustClassicalOcr(System.getProperty("eu.himeros.ocr2grk"), System.getProperty("eu.himeros.ocr2lat"));
        //}    
        adjocr=AdjustOcr.newInstance("eu.himeros.ocr.AdjustClassicalOcr");
        
        adjocr.makeAdjusters(new String[]{System.getProperty("eu.himeros.ocr2grk"),System.getProperty("eu.himeros.ocr2lat")});
        try {
            ArrayList<String> v = new ArrayList<String>();
            String[] ps = {"eu.himeros.ocr.e1.dir", "eu.himeros.ocr.e2.dir", "eu.himeros.ocr.e3.dir", "eu.himeros.ocr.traininggroundtruth.dir", "eu.himeros.ocr.accuracygroundtruth.dir"};
            for (String p : ps) {
                f = new File(props.getProperty(p));
                File fAdjDir=new File(f.getParent()+File.separator+"_"+f.getName());
                if(!fAdjDir.exists()) fAdjDir.mkdirs();
                File[] fs = f.listFiles();
                if (fs == null) {
                    continue;
                }
                for (File file : fs) {
                    fileName = file.getAbsolutePath();
                    adjFileName = fAdjDir.getAbsolutePath() + File.separator + file.getName();
                    adjf = new File(adjFileName);
                    if (!adjf.exists() && !((new File(fileName)).getName().startsWith(adjPrefix))) {
                        adjocr.adjust(fileName, adjFileName);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    private void makeErrorPatterns() {
        log.info("makeErrorPatterns()");
        ErrorPatternMaker epm = new ErrorPatternMaker();
        epm.setAnchorFilter(System.getProperty("eu.himeros.anchorfilter"));
        epm.useNegativeAnchorFilter();
        //Read gt files
        File trainingGtDir = new File(props.getProperty("eu.himeros.ocr.traininggroundtruth.dir"));
        if(jCheckBox1.isSelected()){
            trainingGtDir=new File(trainingGtDir.getParent()+File.separator+"_"+trainingGtDir.getName());
        }
        File[] trainingGtFiles=trainingGtDir.listFiles();
        String eptDirName = props.getProperty("eu.himeros.ocr.errorpatterns.dir");
        jProgressBar1.setMinimum(0);
        jProgressBar1.setMaximum(3);
        for (int i = 0; i < 3; i++) {
            jProgressBar1.setValue(i+1);
            //Read correspondent ocr files
            File eDir = new File(props.getProperty("eu.himeros.ocr.e" + 1 + ".dir"));
            if(jCheckBox1.isSelected()){
                eDir=new File(eDir.getParent()+File.separator+"_"+eDir.getName());
            }
            File[] eFiles = findFiles(eDir, trainingGtFiles);
            //Make the error patterns
            epm.make(eFiles, trainingGtFiles, eptDirName + File.separator + "e" + (i + 1) + ".ept");
        }
    }

    private void align() throws FileNotFoundException {
        log.info("align()");
        File algRepDir=new File(props.getProperty("eu.himeros.ocr.alignment.dir"));
        algRepDir=new File(algRepDir.getParent()+File.separator+"_algnreps_"+algRepDir.getName());
        if(jCheckBox5.isSelected()&&!algRepDir.exists()){
            algRepDir.mkdirs();
        }
        MultiAligner ma = new MultiAligner();
        ma.setCharFilter(System.getProperty("eu.himeros.anchorfilter"));
        String[] fileNames = new String[ENGS];
        String[] engines = new String[ENGS];
        String multiAlignedFileName;
        String mergedFileName;
        String eptDir = props.getProperty("eu.himeros.ocr.errorpatterns.dir"); // error pattern directory
        ArrayList[] fileNameV = new ArrayList[ENGS];
        ArrayList[] v = new ArrayList[ENGS];
        int[] beg = new int[ENGS];
        int[] end = new int[ENGS];
        File[] fDirs = new File[ENGS];
        String fName;
        String line;
        BufferedReader[] brs = new BufferedReader[ENGS];
        AnchorFinder af;
        int[][] anchors;
        File fx;
        for (int i = 0; i < ENGS; i++) { //BEG populate the file vectors for each engine
            int idx = i + 1;
            log.info("e" + idx);
            ma.addErrorPatternFile(eptDir + File.separator + "e" + idx + ".ept");
            fileNameV[i] = new ArrayList<String>();
            fDirs[i] = new File(props.getProperty("eu.himeros.ocr.e" + idx + ".dir"));
            if(jCheckBox1.isSelected()){
                fDirs[i]=new File(fDirs[i].getParent()+File.separator+"_"+fDirs[i].getName());
            }
            for(File fTemp:fDirs[i].listFiles()){
                fileNameV[i].add(fTemp.getAbsolutePath());
            }
        }//END populate the file vectors for each engine
        int tot = fileNameV[0].size();
        jProgressBar1.setMinimum(0);
        jProgressBar1.setMaximum(tot);
        int counter = 0;
        for (Object fileObj : fileNameV[0]) {
            if(aborted==true){return;}
            counter++;
            log.info("" + (counter) + "/" + tot);
            jProgressBar1.setValue(counter);
            fileNames[0] = (String) fileObj;
            fx = new File(fileNames[0]);
            fName = fx.getName();
            multiAlignedFileName=algRepDir.getAbsolutePath()+File.separator+fName;
            mergedFileName = props.getProperty("eu.himeros.ocr.alignment.dir") + File.separator + fName;
            for (int i = 1; i < ENGS; i++) {
                fileNames[i] = fDirs[i] + File.separator + fName;
                if (!fileNameV[i].contains(fileNames[i])) {
                    fileNames[i] = null;
                }
            }
            for (int i = 0; i < ENGS; i++) {
                beg[i] = 0;
                v[i] = new ArrayList();
                engines[i] = "";
                brs[i] = new BufferedReader(new FileReader(fileNames[i]));
                try {
                    while ((line = brs[i].readLine()) != null) {
                        engines[i] += line + "\u00B6";
                    }
                } catch (IOException ex) {
                    Logger.getLogger(OcroPerseus.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            af = new AnchorFinder(fileNames, "[\\u0370-\\u03FF\\u1F00-\\u1FFF\\u2019]");
            anchors = af.getAnchorPositions();
            for (int j = 0; j < anchors[0].length; j++) {
                for (int i = 0; i < ENGS; i++) {
                    end[i] = anchors[i][j];
                    char c;
                    while (end[i] < engines[i].length() && (c = engines[i].charAt(end[i])) != ' ' && c != '\u00B6') {
                        end[i]++;
                    }
                    end[i]++;
                }
                for (int i = 0; i < 3; i++) {
                    v[i].add(engines[i].substring(beg[i], end[i]));
                }
                for (int i = 0; i < 3; i++) {
                    beg[i] = end[i];
                }
            }
            if(jCheckBox5.isSelected()){
                ma.exec(v, multiAlignedFileName, mergedFileName);
            }else{
                ma.exec(v, mergedFileName);
            }
            
        }
    }

    private void compare(){
        ErrorNgramEvaluator ene=new ErrorNgramEvaluator();
        ene.setAnchorFilter(System.getProperty("eu.himeros.anchorfilter"));
        File targetDir=new File(jTextField7.getText());
        File standardDir=new File(jTextField8.getText());
        File compDir=new File(jTextField9.getText());
        String targetFileName;
        String standardFileName;
        String compFileName;
        String fileName;
        ArrayList<String> targetFileNameV=new ArrayList<String>();
        for(File targetFile:targetDir.listFiles()){
            targetFileNameV.add(targetFile.getName());
        }
        int tot=standardDir.listFiles().length;
        jProgressBar2.setMinimum(0);
        jProgressBar2.setMaximum(tot);
        int counter=0;
        for(File standardFile:standardDir.listFiles()){
            if(aborted==true){aborted=false;return;}
            counter++;
            jProgressBar2.setValue(counter);
            fileName=standardFile.getName();
            if(targetFileNameV.contains(fileName)){
               targetFileName=targetDir.getAbsolutePath()+File.separator+fileName;
               standardFileName=standardFile.getAbsolutePath();
               compFileName=compDir.getAbsolutePath()+File.separator+fileName;
               ene.make(targetFileName, standardFileName, compFileName);
               log.info(targetFileName);
            }
        }
    }
    
    private void evaluateAccuracy(){
        ErrorNgramEvaluator ene=new ErrorNgramEvaluator();
        ene.setAnchorFilter(System.getProperty("eu.himeros.anchorfilter"));
        File targetDir=new File(jTextField11.getText());
        File standardDir=new File(jTextField13.getText());
        if(jCheckBox1.isSelected()){
            standardDir=new File(standardDir.getParent()+File.separator+"_"+standardDir.getName());
        }
        File compDir=new File(jTextField14.getText());
        String targetFileName;
        String standardFileName;
        String compFileName;
        String fileName;
        ArrayList<String> targetFileNameV=new ArrayList<String>();
        for(File targetFile:targetDir.listFiles()){
            targetFileNameV.add(targetFile.getName());
        }
        int tot=standardDir.listFiles().length;
        jProgressBar1.setMinimum(0);
        jProgressBar1.setMaximum(tot);
        int counter=0;
        for(File standardFile:standardDir.listFiles()){
            if(aborted==true){aborted=false;return;}
            counter++;
            jProgressBar1.setValue(counter);
            fileName=standardFile.getName();
            if(targetFileNameV.contains(fileName)){
               targetFileName=targetDir.getAbsolutePath()+File.separator+fileName;
               standardFileName=standardFile.getAbsolutePath();
               compFileName=compDir.getAbsolutePath()+File.separator+fileName;
               ene.make(targetFileName, standardFileName, compFileName);
               log.info(targetFileName);
            }
        }
    }

    private File[] findFiles(File fDir, File[] selectors) {
        File[] res = new File[selectors.length];
        int i = 0;
        for (File sel : selectors) {
            res[i++] = (new File(fDir.getAbsolutePath() + File.separator + sel.getName()));
        }
        return res;
    }

    private void enableAll(boolean enabled) {
        jTextField2.setEnabled(enabled);
        jTextField3.setEnabled(enabled);
        jTextField4.setEnabled(enabled);
        jTextField5.setEnabled(enabled);
        jTextField6.setEnabled(enabled && jCheckBox4.isSelected());
        jTextField10.setEnabled(enabled);
        jTextField11.setEnabled(enabled);
        jTextField13.setEnabled(enabled && jCheckBox6.isSelected());
        jTextField14.setEnabled(enabled && jCheckBox6.isSelected());
        jButton2.setEnabled(enabled);
        jButton3.setEnabled(enabled);
        jButton4.setEnabled(enabled);
        jButton5.setEnabled(enabled);
        jButton6.setEnabled(enabled && jCheckBox4.isSelected());
        jButton10.setEnabled(enabled);
        jButton11.setEnabled(enabled);
        jButton13.setEnabled(enabled && jCheckBox6.isSelected());
        jButton14.setEnabled(enabled && jCheckBox6.isSelected());
        jButton15.setEnabled(enabled);
        jButton16.setEnabled(enabled);
        jCheckBox1.setEnabled(enabled);
        jCheckBox4.setEnabled(enabled);
        jCheckBox5.setEnabled(enabled);
        jCheckBox6.setEnabled(enabled);
    }

    private void freezeForAlignment(boolean frozen){
        boolean enabled=!frozen;
        enableAll(enabled);
        jTextField1.setEnabled(enabled);
        jButton1.setEnabled(enabled);
        jButton15.setEnabled(true);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new OcroPerseus().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenuItem contentsMenuItem;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JMenuItem cutMenuItem;
    private javax.swing.JMenuItem deleteMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox4;
    private javax.swing.JCheckBox jCheckBox5;
    private javax.swing.JCheckBox jCheckBox6;
    private javax.swing.JDialog jDialog1;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JProgressBar jProgressBar2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JTextField jTextField14;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    // End of variables declaration//GEN-END:variables
}
