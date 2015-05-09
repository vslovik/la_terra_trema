package dictionary;

import algorithms.ST;
import algorithms.TST;
import algorithms.Bag;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The <tt>MorfItDictionary</tt> represents MorfIt!
 * A free morphological lexicon for the Italian Language
 *
 * See:
 * Eros Zanchetta and Marco Baroni (2005) Morph-it!
 * "A free corpus-based morphological resource for the Italian language",
 * proceedings of Corpus Linguistics 2005, University of Birmingham,
 * Birmingham, UK.
 *
 * Morf-It!:
 * http://sslmitdev-online.sslmit.unibo.it/linguistics/morph-it/demo.php
 * http://sslmitdev-online.sslmit.unibo.it/linguistics/downloads/readme-morph-it.txt
 *
 *  @author Valeriya Slovikovskaya
 */
public class MorfItDictionary {

    protected TST<Short> labelIndex;
    protected ST<Short, String> labelIndexR;
    protected ST<Integer, String> lemmaIndexR;
    protected TST<Bag<FormIndexEntry>> formIndex;

    private static class Dto {
        private String form;
        private String lemma;
        private String label;
    }

    private class FormIndexEntry {
        protected int lemmaKey;
        protected Short labelKey;

        public String getLemma()
        {
            return lemmaIndexR.get(lemmaKey);
        }

        public String getLabel()
        {
            return labelIndexR.get(labelKey);
        }
    }

    public MorfItDictionary(String filename) {

        labelIndex = new TST<Short>();
        labelIndexR = new ST<Short, String>();
        lemmaIndexR = new ST<Integer, String>();
        formIndex = new TST<Bag<FormIndexEntry>>();

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

    public Bag<FormIndexEntry> get(String form)
    {
        if(!contains(form)) return null;
        return formIndex.get(form);
    }

    public Iterable<String> getLabels()
    {
        return labelIndex.keys();
    }

    protected Dto stringToDto(String s)
    {
        Dto dto = new Dto();

        String[] a = s.split("\t");

        dto.form = a[0];
        dto.lemma = a[1];
        dto.label = a[2];

        return dto;
    }

    protected void addEntry(Dto dto)
    {
        FormIndexEntry fe = new FormIndexEntry();
        fe.labelKey = addToLabelIndex(dto);
        fe.lemmaKey = addLemmaToIndex(dto, dto.lemma);

        Bag<FormIndexEntry> bag;

        if (formIndex.contains(dto.form)) {
            bag = formIndex.get(dto.form);
            if(dto.form.equals(dto.lemma)) {
                for (FormIndexEntry ffe: formIndex.get(dto.lemma)) {
                    if (ffe.labelKey == null) {
                        ffe.labelKey = fe.labelKey;
                        return;
                    }
                }
            }
        } else {
            bag = new Bag<FormIndexEntry>();
        }

        bag.add(fe);
        formIndex.put(dto.form, bag);
    }

    private int addLemmaToIndex(Dto dto, String lemma)
    {
        if (formIndex.contains(lemma))
            for (FormIndexEntry ffe: formIndex.get(dto.lemma))
                if (lemma.equals(lemmaIndexR.get(ffe.lemmaKey)))
                    return ffe.lemmaKey;

        Bag<FormIndexEntry> bag = new Bag<FormIndexEntry>();
        FormIndexEntry fe = new FormIndexEntry();
        fe.lemmaKey = lemmaIndexR.size();

        bag.add(fe);
        formIndex.put(dto.lemma, bag);
        lemmaIndexR.put(fe.lemmaKey, dto.lemma);

        return fe.lemmaKey;
    }

    private Short addToLabelIndex(Dto dto)
    {
        if (!labelIndex.contains(dto.label)) {
            Short labelKey = (short) labelIndex.size();
            labelIndex.put(dto.label, labelKey);
            labelIndexR.put(labelKey, dto.label);

            return labelKey;
        }

        return labelIndex.get(dto.label);
    }

    public static void main(String[] args)
    {
        System.gc();
        Runtime rt = Runtime.getRuntime();

        String filename  = args[0]; // /home/lera/Desktop/LAUREA/la_terra_trema/morfit/morph-it_048.txt

        MorfItDictionary d = new MorfItDictionary(filename);

        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println("Memory usage: " + usedMB + "Mb\n");

        System.out.println("Tags: \n");
        for (String label : d.getLabels())
            System.out.println(label);

        System.out.println("Test phrase parsing result: ");

        String s;

        s = "Quello che si temeva sta accadendo: macchine date a fuoco, lancio di oggetti mentre le forze dell’ordine rispondono con lacrimogeni. Assalto di decine di manifestanti con cappuccio alle vetrine dei negozi, cassonetto rivoltati, gente che fugge. Corso Magenta messo a ferro e fuoco  di Fabio Abati";

        for (String word: s.toLowerCase().split(" ")){
            if(word.length() > 0 && d.contains(word)) {
                for (FormIndexEntry fe: d.get(word)) {
                    System.out.println(word);
                    System.out.println(fe.getLemma());
                    System.out.println(fe.getLabel());
                    System.out.println("");
                }
            }
        }

        System.out.println("Lemma index size: " + d.lemmaIndexR.size() + "\n"); // 35056
        System.out.println("Form index size: " + d.formIndex.size() + "\n"); // 405411
    }
}