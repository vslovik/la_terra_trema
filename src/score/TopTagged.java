package score;

import algorithms.MinPQ;
import algorithms.MaxPQ;
import algorithms.Queue;
import features.NGramCollector;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class TopTagged {

    protected int N = 5000;
    protected MinPQ<ScoredTaggedPhrase> pqT;
    protected MaxPQ<ScoredTaggedPhrase> pqB;

    public TopTagged()
    {
        pqT = new MinPQ<>();
        pqB = new MaxPQ<>();
    }

    public void collectPhrase(ScoredTaggedPhrase sp)
    {
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

    protected static void showMemoryUsage(Runtime rt)
    {
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println("Memory usage: " + usedMB + "Mb\n");
    }

    /**
     * Collect training corpus phrases
     *
     * @param trainingFile Training file
     * @param nc Token collector
     * @param tnc Tag collector
     */
    public void collect(String trainingFile, NGramCollector nc, NGramCollector tnc)
    {
        String line;
        BufferedReader br;
        Queue<String> tokens;
        Queue<String> tags;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(trainingFile), "utf8"));

            ScoredTaggedPhrase sp = new ScoredTaggedPhrase();

            while ((line = br.readLine()) != null) {

                if(line.equals("") ) {
                    if (sp.tokens.size() > 0) {
                        sp.add(nc, tnc);
                        sp = new ScoredTaggedPhrase();
                    }

                    continue;
                }

                sp.enqueue(line);
            }

            if(sp.tokens.size() >= N) {
                sp.add(nc, tnc);
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }

        nc.calculateFactor();
        tnc.calculateFactor();
    }

    /**
     * Score corpus phrases on base of training corpus collections
     *
     * @param corpusFile Corpus file
     * @param nc Token collector
     * @param tnc Training collector
     */
    public void score(String corpusFile, NGramCollector nc, NGramCollector tnc)
    {
        String line;
        BufferedReader br;
        ScoredTaggedPhrase sp;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(corpusFile), "utf8"));

            sp = new ScoredTaggedPhrase();

            while ((line = br.readLine()) != null) {

                if(line.equals("") ) {

                    if (sp.tokens.size() > 0) {
                        sp.score(nc, tnc);
                        collectPhrase(sp);
                        sp = new ScoredTaggedPhrase();
                    }

                    continue;
                }

                sp.enqueue(line);

            }

            if(sp.tokens.size() > 1) {
                sp.score(nc, tnc);
                collectPhrase(sp);
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    public void printTop()
    {
        int i = 0;
        for (ScoredTaggedPhrase ph: pqT){
            i++;
            System.out.println(Integer.toString(i));
            System.out.println("score: " + Double.toString(ph.score));
            System.out.println("");
            System.out.println(ph.tokens.toString());
            System.out.println(ph.tags.toString());
            System.out.println("");
        }
    }

    public void printBottom()
    {
        int i = 0;
        for (ScoredTaggedPhrase ph: pqB){
            i++;
            System.out.println(Integer.toString(i));
            System.out.println("score: " + Double.toString(ph.score));
            System.out.println("");
            System.out.println(ph.tokens.toString());
            System.out.println(ph.tags.toString());
            System.out.println("");
        }
    }

    public static void main(String[] args)
    {
        System.gc();
        Runtime rt = Runtime.getRuntime();

//        String trainingFile  = args[0]; // /home/lera/LAUREA/input/Training_pos_isst-paisa-devLeg.pos
//        String corpusFile  = args[1]; // /home/lera/jproject/gold/gold

        String trainingFile  = "/home/lera/LAUREA/input/Training_pos_isst-paisa-devLeg-head-80732.pos";
        String corpusFile  = "/home/lera/jproject/gold/gold";

        NGramCollector nc = new NGramCollector();
        NGramCollector tnc = new NGramCollector();
        TopTagged top = new TopTagged();

        top.collect(trainingFile, nc, tnc);

        showMemoryUsage(rt);

        //System.out.println(tnc.get("STOP"));

        top.score(corpusFile, nc, tnc);

        showMemoryUsage(rt);

        top.printTop();
        //top.printBottom();

        System.out.println(nc.size());
        System.out.println(tnc.size());

    }

}
