package dictionary;

import algorithms.MinPQ;
import algorithms.Queue;
import algorithms.TST;
import score.ScoredToken;
import score.PhraseScoreTool;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The <tt>MorfItYesNoDictionary</tt> represents MorfIt!
 * A free morphological lexicon for the Italian Language
 *
 * For performance purposes only word forms (not tags) are kept
 *
 *  @author Valeriya Slovikovskaya
 */
public class LexiconDiff implements PhraseScoreTool {

    protected TST<Integer> indexCorpus;
    protected MinPQ<ScoredToken> topTokens;

    protected int ttN = 50000;
    protected int counter;

    public LexiconDiff(String baseDictFile, String corpusFile) {

        MorfItYesNoDictionary d = new MorfItYesNoDictionary(baseDictFile);

        indexCorpus = new TST<Integer>();
        topTokens = new MinPQ<ScoredToken>();

        String line, word;
        BufferedReader br;

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(corpusFile), "utf8"));
            while ((line = br.readLine()) != null) {
                word = getToken(line);
                if (word.matches("\\w+") && word.length() > 2 && !word.matches("^(http|@|#|tco).*") && !d.contains(word)) {
                    counter += 1;
                    addCorpusEntry(word);

                    if(counter % 50000 == 0)
                        System.out.println(indexCorpus.size());
//                    if (counter  > 500000)
//                        break;
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    public boolean contains(String form)
    {
        return indexCorpus.get(form) != null;
    }

    public int get(String form)
    {
        return indexCorpus.get(form);
    }

    public int size()
    {
        return indexCorpus.size();
    }

    public double score(Queue<String> trigram)
    {
        if(trigram.size() != 3)
            throw new IllegalArgumentException("Size of trigram != 3");

        String[] t = new String[3];
        int i = 0;
        for(String e:trigram) {
            t[i++] = e;
        }
        if (t[0].length() == 1 && t[2].length() == 1) // punctuation
            return 0;
        if(contains(t[1])) {
            int freq = get(t[1]);
            if(freq > 20) {
                return 1;
            }
            return 0;
        }
        return 0;
    }

    public int score(String token)
    {
        String tkn = getToken(token);
        if(contains(tkn))
            return get(tkn);
        return 0;
    }

    public Iterable<String> words()
    {
        return indexCorpus.keys();
    }

    protected String getToken(String line)
    {
        return line.toLowerCase().replace("rt", "").replaceAll("[‘'~&;,.\"]", "").replaceAll("^\\d+$", "");
    }

    protected void addCorpusEntry(String form)
    {
        if (!contains(form)) {
            indexCorpus.put(form, 1);
        } else {
            int frequency = indexCorpus.get(form);
            frequency += 1;

            indexCorpus.put(form, frequency);
        }
    }

    protected void collectToken(ScoredToken st)
    {
        if (st.getScore() == 0)
            return;

        int min = 0;
        if (!topTokens.isEmpty())
            min = topTokens.min().getScore();

        if (topTokens.size() < ttN && st.getScore() >= min) {
            topTokens.insert(st);
            if (topTokens.size() == ttN + 1)
                topTokens.delMin();
        }
    }

    public static void main(String[] args)
    {
        System.gc();
        Runtime rt = Runtime.getRuntime();

        // /home/lera/Desktop/LAUREA/la_terra_trema/morfit/morph-it_048.txt /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente_cleaned_no_tags.pos
        String baseDictFile  = args[0]; // /home/lera/Desktop/LAUREA/la_terra_trema/morfit/morph-it_048.txt
        String corpusFile  = args[1]; // /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente_cleaned_no_tags.pos

        LexiconDiff diff = new LexiconDiff(baseDictFile, corpusFile);

        // System.out.println("Words: \n");
        for (String word : diff.words()) {
            //System.out.println(word);
            diff.collectToken(new ScoredToken(word, diff.score(word)));
        }

        System.out.println("Top tokens: \n");
        int i = 0;
        for (ScoredToken t: diff.topTokens){
            i++;
            System.out.println(Integer.toString(i) + " " + t.getToken() + " " + t.getScore());
        }

        System.out.println(diff.size());

        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println("Memory usage: " + usedMB + "Mb\n");

    }
}