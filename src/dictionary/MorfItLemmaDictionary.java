package dictionary;

import algorithms.ST;
import algorithms.TST;
import algorithms.Bag;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The <tt>MorfItLemmaDictionary</tt> represents MorfIt!
 * A free morphological lexicon for the Italian Language
 *
 * For performance purposes only word form and lemma (not tags) are kept
 *
 *  @author Valeriya Slovikovskaya
 */
public class MorfItLemmaDictionary {

    protected ST<Integer, String> lemmaIndexR;
    protected TST<Bag<Integer>> formIndex;

    private static class Dto {
        private String form;
        private String lemma;
    }

    public String getLemma(int lemmaKey)
    {
        return lemmaIndexR.get(lemmaKey);
    }

    public MorfItLemmaDictionary(String filename) {

        lemmaIndexR = new ST<Integer, String>();
        formIndex = new TST<Bag<Integer>>();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "utf8"));

            String s;

            while ((s = br.readLine()) != null) {
                addEntry(stringToDto(s));
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    public boolean contains(String form)
    {
        return formIndex.get(form) != null;
    }

    public Bag<Integer> get(String form)
    {
        if(!contains(form)) return null;
        return formIndex.get(form);
    }

    protected Dto stringToDto(String s)
    {
        Dto dto = new Dto();

        String[] a = s.split("\t");

        dto.form = a[0];
        dto.lemma = a[1];

        return dto;
    }

    protected void addEntry(Dto dto)
    {
        int lemmaKey = addLemmaToIndex(dto, dto.lemma);

        Bag<Integer> bag;

        if (formIndex.contains(dto.form)) {
            bag = formIndex.get(dto.form);
            for(Integer lk: bag) {
                if (lk == lemmaKey)
                    return;
            }
        } else {
            bag = new Bag<Integer>();
        }

        bag.add(lemmaKey);
        formIndex.put(dto.form, bag);
    }

    private int addLemmaToIndex(Dto dto, String lemma)
    {
        if (formIndex.contains(lemma))
            for (int lemmaKey: formIndex.get(dto.lemma))
                if (lemma.equals(lemmaIndexR.get(lemmaKey)))
                    return lemmaKey;

        Bag<Integer> bag = new Bag<Integer>();
        int lemmaKey = lemmaIndexR.size();

        bag.add(lemmaKey);
        formIndex.put(dto.lemma, bag);
        lemmaIndexR.put(lemmaKey, dto.lemma);

        return lemmaKey;
    }

    public static void main(String[] args)
    {
        System.gc();
        Runtime rt = Runtime.getRuntime();

        String filename  = args[0]; // /home/lera/Desktop/LAUREA/la_terra_trema/morfit/morph-it_048.txt

        MorfItLemmaDictionary d = new MorfItLemmaDictionary(filename);

        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println("Memory usage: " + usedMB + "Mb\n");

        System.out.println("Test phrase parsing result: ");

        String s = "Quello che si temeva sta accadendo: macchine date a fuoco, lancio di oggetti mentre le forze dell’ordine rispondono con lacrimogeni. Assalto di decine di manifestanti con cappuccio alle vetrine dei negozi, cassonetto rivoltati, gente che fugge. Corso Magenta messo a ferro e fuoco  di Fabio Abati";

        for (String word: s.toLowerCase().split(" ")){
            if(word.length() > 0 && d.contains(word)) {
                for (Integer lemmaKey: d.get(word)) {
                    System.out.println(word);
                    System.out.println(d.getLemma(lemmaKey));
                    System.out.println("");
                }
            }
        }

        System.out.println("Lemma index size: " + d.lemmaIndexR.size() + "\n"); // 35056
        System.out.println("Form index size: " + d.formIndex.size() + "\n"); // 405411
    }
}