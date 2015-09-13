package score;

/***********************************************************************************
 * Execution:
 * java score.TopScored [file to extract features from] [file with phrases to score]
 *
 * ********************************************************************************/

import algorithms.MinPQ;
import algorithms.MaxPQ;
import features.NGramCollector;
import features.TagDictionary;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *  The <tt>TopScored</tt> class represents a client that scores
 *  every phrase in the input stream and collects <em>N</em>
 *  high/low scored items.
 *
 *  @author Valeriya Slovikovskaya vslovik@gmail.com
 */
public class TopScored {

    protected int N = 500;
    protected MinPQ<ScoredTaggedPhrase> pqT;
    protected MaxPQ<ScoredTaggedPhrase> pqB;

    public TopScored() {
        pqT = new MinPQ<>();
        pqB = new MaxPQ<>();
    }

    /**
     * Collect NGrams from input file
     *
     * @param inputFile Input file
     * @param nc Token NGram collector
     * @param tnc Tag NGram collector
     */
    public void collect(String inputFile, NGramCollector nc, NGramCollector tnc, TagDictionary td) {
        String line;
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "utf8"));

            ScoredTaggedPhrase sp = new ScoredTaggedPhrase();

            while ((line = br.readLine()) != null) {
                if (line.equals("")) {
                    sp.add(tnc, td);
                    sp = new ScoredTaggedPhrase();
                    continue;
                }
                sp.enqueue(line);
            }

            br.close();

            if (sp.tokens.size() > 0)
                sp.add(tnc, td);

            td.buildSuffixIndex(tnc.suffixSmoothingFactor());
            nc.smoothTrigramCounts();
            tnc.smoothTrigramCounts();
            nc.buildSuffixIndex();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    /**
     * Scores phrases from input file
     *
     * @param inputFile Input file
     * @param tnc Tag NGram collector
     */
    public void score(String inputFile, NGramCollector tnc, TagDictionary td) {
        String line;
        BufferedReader br;
        ScoredTaggedPhrase sp;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "utf8"));

            sp = new ScoredTaggedPhrase();

            while ((line = br.readLine()) != null) {
                if (line.equals("")) {
                    sp.score(tnc, td);
                    rank(sp);
                    sp = new ScoredTaggedPhrase();
                    continue;
                }
                sp.enqueue(line);
            }

            br.close();

            if (sp.tokens.size() > 0) {
                sp.score(tnc, td);
                rank(sp);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    /**
     * Decide to discard scored phrase
     * or include it into top- or
     * bottom- phrase list
     *
     * @param sp scored phrase
     */
    public void rank(ScoredTaggedPhrase sp) {
        if (sp.tokens.size() == 0 || sp.tags.size() == 0)
            return;

        double min = 0.0;
        if (!pqT.isEmpty())
            min = pqT.min().score;

        if (pqT.size() < N || sp.score > min) {

            pqT.insert(sp);
            if (pqT.size() == N + 1)
                pqT.delMin();
        }

        double max = Double.POSITIVE_INFINITY;
        if (!pqB.isEmpty())
            max = pqB.max().score;

        if (pqB.size() < N || sp.score < max) {
            pqB.insert(sp);
            if (pqB.size() == N + 1)
                pqB.delMax();
        }
    }

    /**
     * Prints <em>N</em> high-scored phrases
     */
    public void  printTop() {
        for (ScoredTaggedPhrase ph : pqT) {
            ph.print();
            System.out.println();
        }
    }

    /**
     * Prints <em>N</em> low-scored phrases
     */
    public void printBottom() {
        for (ScoredTaggedPhrase ph : pqB) {
            ph.print();
            System.out.println();
        }
    }

    /**
     * Shows memory usage
     *
     * @param rt Runtime object
     */
    protected static void showMemoryUsage(Runtime rt) {
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println("Memory usage: " + usedMB + "Mb\n");
    }

    /**
     * Start memory usage measures
     *
     * @return Runtime object
     */
    protected static Runtime start()
    {
        System.gc();
        return Runtime.getRuntime();
    }

    public static void main(String[] args) {

        String trainingFile  = args[0];
        String corpusFile  = args[1];

        NGramCollector nc = new NGramCollector();
        NGramCollector tnc = new NGramCollector();
        TagDictionary td = new TagDictionary();

        TopScored top = new TopScored();
        top.collect(trainingFile, nc, tnc, td);
        top.score(corpusFile, tnc, td);
        top.printBottom();
    }

}
