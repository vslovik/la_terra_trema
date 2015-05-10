// ToDo: deal with the same tweet phrases
package score;

import algorithms.Queue;
import algorithms.MinPQ;

import dictionary.TrainingLexiconDiff;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Top {

    protected int N = 1000;
    protected MinPQ<ScoredPhrase> pq;

    public Top(String corpusFile, TrigramScoreTool[] scoreTools)
    {
        if(scoreTools.length == 0)
            throw new IllegalArgumentException("No scoring tools used");

        pq = new MinPQ<ScoredPhrase>();

        String line, word;
        BufferedReader br;
        ScoredPhrase sp;
        Queue<String> trigram;

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(corpusFile), "utf8"));

            sp = new ScoredPhrase(new Queue<String>(), 0);
            trigram = new Queue<String>();

            while ((line = br.readLine()) != null) {

                if(line.equals("")) {
                    collectPhrase(sp);
                    sp = new ScoredPhrase(new Queue<String>(), 0);
                    trigram = new Queue<String>();
                    continue;
                }

                sp.lines.enqueue(line);

                word = getToken(line);

                if(trigram.size() < 3) {
                    trigram.enqueue(word);
                } else {
                    sp.score += scoreTools[0].score(trigram);
                    trigram.dequeue();
                }

            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    protected String getToken(String line)
    {
        return line;
//        String s = line.replace(" ", "\t");
//        String[] a = s.toLowerCase().split("\t");
//        return a[0];
    }

    protected void collectPhrase(ScoredPhrase sp)
    {
        if (sp.score == 0 || sp.lines.size() == 0)
            return;

        int min = 0;
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

        String baseDictFile  = args[0]; // /home/lera/Desktop/LAUREA/la_terra_trema/morfit/morph-it_048.txt
        String corpusFile  = args[1]; // /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente_cleaned_no_tags.pos

        TrainingLexiconDiff diff = new TrainingLexiconDiff(baseDictFile, corpusFile);

        showMemoryUsage(rt);

        TrigramScoreTool[] scoreTools = {diff};
        Top top = new Top(corpusFile, scoreTools);

        showMemoryUsage(rt);

        int i = 0;
        for (ScoredPhrase ph: top.pq){
            i++;
            System.out.println(Integer.toString(i));
            System.out.println("score: " + Integer.toString(ph.score));
            System.out.println("");
            for(String line: ph.lines)
                System.out.println(line);
            System.out.println("");
        }
    }

}
