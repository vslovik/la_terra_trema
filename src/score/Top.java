// ToDo: deal with the same tweet phrases
package score;

import algorithms.Queue;
import algorithms.MinPQ;

import features.NGramCollector;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Top {

    protected int N = 1000;
    protected MinPQ<ScoredPhrase> pq;

    public Top(String corpusFile, PhraseScoreTool[] scoreTools)
    {
        if(scoreTools.length == 0)
            throw new IllegalArgumentException("No scoring tools used");

        pq = new MinPQ<ScoredPhrase>();

        BufferedReader br;
        ScoredPhrase sp;
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(corpusFile), "utf8"));

            sp = new ScoredPhrase();

            while ((line = br.readLine()) != null) {

                if(line.equals("") && sp.lines.size() > 0) {
                    for(PhraseScoreTool tool: scoreTools)
                        sp.score += tool.score(sp.lines);
                    collectPhrase(sp);
                    sp = new ScoredPhrase(new Queue<String>(), 0);

                    continue;
                }

                sp.lines.enqueue(line);
            }

            if(sp.lines.size() > 0) {
                for(PhraseScoreTool tool: scoreTools)
                    sp.score += tool.score(sp.lines);
                collectPhrase(sp);
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    protected void collectPhrase(ScoredPhrase sp)
    {
        if (sp.score == 0 || sp.lines.size() == 0)
            return;

        double min = 0.0;
        if (!pq.isEmpty())
            min = pq.min().score;

        if (pq.size() < N || sp.score > min) {

            pq.insert(sp);
            if (pq.size() == N + 1)
                pq.delMin();
        }
    }

    protected static void showMemoryUsage(Runtime rt)
    {
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println("Memory usage: " + usedMB + "Mb\n");
    }

    public static void main(String[] args)
    {
        System.gc();
        Runtime rt = Runtime.getRuntime();

        String trainingFile  = args[0]; // /home/lera/Desktop/LAUREA/Training_pos_isst-paisa-devLeg_no_words.pos
        String corpusFile  = args[1]; // /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente_cleaned_no_words.pos
        NGramCollector diff = new NGramCollector(trainingFile);

        showMemoryUsage(rt);

        PhraseScoreTool[] scoreTools = {diff};
        Top top = new Top(corpusFile, scoreTools);

        showMemoryUsage(rt);

        int i = 0;
        for (ScoredPhrase ph: top.pq){
            i++;
            System.out.println(Integer.toString(i));
            System.out.println("score: " + Double.toString(ph.score));
            System.out.println("");
            System.out.println(ph.lines.toString());
            System.out.println("");
        }
    }

}
