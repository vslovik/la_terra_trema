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
        pqT = new MinPQ<ScoredTaggedPhrase>();
        pqB = new MaxPQ<ScoredTaggedPhrase>();
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

        //if ((pqB.size() < N || sp.score < max) && sp.score > 0.01) {
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

            tokens = new Queue<String>();
            tokens.enqueue("START"); // start of sentence marker
            tags = new Queue<String>();
            tags.enqueue("START"); // start of sentence marker

            int i = 0;
            while ((line = br.readLine()) != null) {

                if(line.equals("") && tokens.size() > 0) {
                    tokens.enqueue("STOP"); // end of sentence marker
                    tags.enqueue("STOP"); // end of sentence marker
                    nc.addPhrase(tokens);
                    tokens = new Queue<String>();
                    tokens.enqueue("START"); // start of sentence marker
                    tnc.addPhrase(tags);
                    tags = new Queue<String>();
                    tags.enqueue("START"); // start of sentence marker
                    continue;
                }

                String[] arr = line.split("\t");
                if (arr.length < 2) {
                    System.out.println(line);
                    continue;
                    //System.exit(1);
                }

                tokens.enqueue(arr[0]);
                tags.enqueue(arr[1]);
//                if(i > 200000)
//                    break;
                i++;
            }

            if(tokens.size() >= N) {
                nc.addPhrase(tokens);
                tnc.addPhrase(tags);
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
            sp.tokens.enqueue("START"); // start of sentence marker);
            sp.tags.enqueue("START"); // start of sentence marker);

            int i  = 0;
            while ((line = br.readLine()) != null) {

                if(line.equals("") ) {

                    if (sp.tokens.size() > 1) {
                        sp.tokens.enqueue("STOP"); // start of sentence marker);
                        sp.score += score(nc, sp.tokens);
                        if (sp.tags.size() > 1) {
                            sp.tags.enqueue("STOP"); // start of sentence marker);
                            sp.score += score(tnc, sp.tags);
                        }

                        collectPhrase(sp);
                    }
                    sp = new ScoredTaggedPhrase();
                    sp.tokens.enqueue("START"); // start of sentence marker);
                    sp.tags.enqueue("START"); // start of sentence marker);


                    continue;
                }

                String[] arr = line.split("\t");
                if (arr.length < 2) {
                    System.out.println(line);
                    continue;
                    //System.exit(1);
                }

                sp.tokens.enqueue(arr[0]);
                sp.tags.enqueue(arr[1]);

                if (i > 1000000) {
                    break;
                }
                i++;
            }

            if(sp.tokens.size() > 1) {
                sp.tokens.enqueue("STOP"); // start of sentence marker);
                sp.score += score(nc, sp.tokens);
                if (sp.tags.size() > 1) {
                    sp.tags.enqueue("STOP"); // start of sentence marker);
                    sp.score += score(tnc, sp.tags);
                }
                collectPhrase(sp);
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    /**
     * Calculate phrase score
     *
     * @param nc Token collector
     * @param tokens Phrase tokens
     * @return score
     */
    public double score(NGramCollector nc, Queue<String> tokens)
    {
        Queue<String> copy = new Queue<String>(); // ToDo Do we need the copy?
        for(String token: tokens) {
            copy.enqueue(token);
        }
        return scoreFirstGram(nc, copy, nc.N - 1) + scorePhrase(nc, copy);
    }

    /**
     * Count N-Gram score
     *
     * @param nc Token collector
     * @param tokens Token queue
     * @return score
     */
    protected double scorePhrase(NGramCollector nc, Queue<String> tokens)
    {
        if(tokens.size() < nc.N) throw new IllegalArgumentException();

        double ph_sc = nc.scorePhaseLength(tokens);
        double score = scoreFirstGram(nc, tokens, nc.N);
        while(tokens.size() > nc.N) {
            tokens.dequeue();
            score += scoreFirstGram(nc, tokens, nc.N);
        }

        return ph_sc * score / (tokens.size() - nc.N + 1);
    }

    /**
     * Score first gram
     *
     * @param nc Token collector
     * @param tokens Token queue
     * @param n nGram rang
     * @return score
     */
    protected double scoreFirstGram(NGramCollector nc, Queue<String> tokens, int n) {
        if (tokens.size() < n) throw new IllegalArgumentException();

        Queue<String> q = new Queue<>();
        for (String token : tokens) {
            q.enqueue(token);
            if (q.size() == n) {
                break;
            }
        }

        return nc.scoreGram(q, n);
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
