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

    protected int N = 100;
//    protected TST<Integer> indexTrain;
    protected TST<Integer> indexCorpus;

    protected int counter;

    public TrainingLexiconDiff(String baseDictFile, String trainFile, String corpusFile) {

        MorfItYesNoDictionary d = new MorfItYesNoDictionary(baseDictFile);

 //       indexTrain = new TST<Integer>();
        indexCorpus = new TST<Integer>();

        String line, word;
        BufferedReader br;

//        try {
//            br = new BufferedReader(new InputStreamReader(new FileInputStream(trainFile), "utf8"));
//            while ((line = br.readLine()) != null) {
//                word = getToken(line);
//                if (word.length() > 2 && !word.matches("^(http|@|#|tco).*") && !d.contains(word)) {
//                    addTrainEntry(word);
//                }
//            }
//            br.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.out.println(e.toString());
//        }

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(corpusFile), "utf8"));
            while ((line = br.readLine()) != null) {
                word = getToken(line);
                //if (word.matches("\\w+") && word.length() > 2 && !word.matches("^(http|@|#|tco).*") && !d.contains(word) && !trainContains(word)) {
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

//    public boolean trainContains(String form)
//    {
//        return indexTrain.get(form) != null;
//    }

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

//    protected void addTrainEntry(String form)
//    {
//        indexTrain.put(form, 1);
//    }

    public Iterable<String> words()
    {
        return indexCorpus.keys();
    }

    protected String getToken(String line)
    {
        //This we let sed to do
//        String s = line.replace(" ", "\t");
//        String[] a = s.toLowerCase().split("\t");
//        String word = a[0];

        return line.toLowerCase().replace("rt", "").replaceAll("[‘'~&;,.\"]", "").replaceAll("^\\d+$", "");

       // This we let sed to do
//        word = word.
//                replaceAll("aa+$","a").
//                replaceAll("àà+$","à").
//                replaceAll("ee+$","e").
//                replaceAll("èè+$","è").
//                replaceAll("éé+$","é").
//                replaceAll("oo+$","o").
//                replaceAll("òò+$","ò").
//                replaceAll("uu+$","u").
//                replaceAll("ùù+$","ù").
//                replaceAll("ii+$","i").
//                replaceAll("ìì+$","ì").

//                replaceAll("^(\\w)\\1+","$1"). // letter is repeated at the beginning of the token
//                replaceAll("(\\w)\\1+$","$1"). // letter is repeated at the end of the token
//                replaceAll("(\\w{2})\\1+$","$1$1"). // token consist of the repeated pair of letters, lets repeat once, not more
//                replaceAll("^(\\w{2})\\1+","$1$1").
//                replaceAll("(\\w)\\1{2}\\1+","$1$1$1"); // letter is repeated more than 3 times in the middle of the word, lets repeat only three times

//        return word;
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

        // /home/lera/Desktop/LAUREA/la_terra_trema/morfit/morph-it_048.txt /home/lera/Desktop/LAUREA/Training_pos_isst-paisa-devLeg.pos /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente.pos
        String baseDictFile  = args[0]; // /home/lera/Desktop/LAUREA/la_terra_trema/morfit/morph-it_048.txt
        String trainFile  = args[1]; // /home/lera/Desktop/LAUREA/Training_pos_isst-paisa-devLeg.pos
        String corpusFile  = args[2]; // /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente_cleaned_no_tags.pos

        TrainingLexiconDiff diff = new TrainingLexiconDiff(baseDictFile, trainFile, corpusFile);

        System.out.println("Words: \n");
        for (String word : diff.words()) {
            System.out.println(word);
        }

        System.out.println(diff.size());

        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println("Memory usage: " + usedMB + "Mb\n");

    }
}