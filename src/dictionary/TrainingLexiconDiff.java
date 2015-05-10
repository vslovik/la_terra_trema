package dictionary;

import algorithms.Queue;
import algorithms.TST;
import score.TrigramScoreTool;

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
public class TrainingLexiconDiff implements TrigramScoreTool {

    protected TST<Integer> indexCorpus;

    protected int counter;

    public TrainingLexiconDiff(String baseDictFile, String corpusFile) {

        MorfItYesNoDictionary d = new MorfItYesNoDictionary(baseDictFile);

        indexCorpus = new TST<Integer>();

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

    public int score(Queue<String> trigram)
    {
        if(trigram.size() != 3)
            throw new IllegalArgumentException("Size of trigram != 3");
        trigram.dequeue();
        String central = trigram.dequeue();
        if(contains(central)) {
            return get(central);
        }
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

    public static void main(String[] args)
    {
        System.gc();
        Runtime rt = Runtime.getRuntime();

        // /home/lera/Desktop/LAUREA/la_terra_trema/morfit/morph-it_048.txt /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente_cleaned_no_tags.pos
        String baseDictFile  = args[0]; // /home/lera/Desktop/LAUREA/la_terra_trema/morfit/morph-it_048.txt
        String corpusFile  = args[1]; // /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente_cleaned_no_tags.pos

        TrainingLexiconDiff diff = new TrainingLexiconDiff(baseDictFile, corpusFile);

        System.out.println("Words: \n");
        for (String word : diff.words()) {
            System.out.println(word);
        }

        System.out.println(diff.size());

        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println("Memory usage: " + usedMB + "Mb\n");

    }
}