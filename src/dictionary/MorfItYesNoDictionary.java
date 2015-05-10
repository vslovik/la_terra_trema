package dictionary;

import algorithms.TST;

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
public class MorfItYesNoDictionary {

    protected TST<Boolean> formIndex;

    public MorfItYesNoDictionary(String filename) {

        formIndex = new TST<Boolean>();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "utf8"));

            String s;
            while ((s = br.readLine()) != null) {
                addEntry(stringToForm(s));
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

    public int size()
    {
        return formIndex.size();
    }

    protected String stringToForm(String s)
    {
        String[] a = s.split("\t");

        return a[0];
    }

    protected void addEntry(String form)
    {
        formIndex.put(form, true);
    }

    public static void main(String[] args)
    {
        System.gc();
        Runtime rt = Runtime.getRuntime();

        String filename  = args[0]; // /home/lera/Desktop/LAUREA/la_terra_trema/morfit/morph-it_048.txt

        MorfItYesNoDictionary d = new MorfItYesNoDictionary(filename);

        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println("Memory usage: " + usedMB + "Mb\n");

        System.out.println("Test phrase parsing result: ");

        String s = "Quello che si temeva sta accadendo: macchine date a fuoco, lancio di oggetti mentre le forze dell’ordine rispondono con lacrimogeni. Assalto di decine di manifestanti con cappuccio alle vetrine dei negozi, cassonetto rivoltati, gente che fugge. Corso Magenta messo a ferro e fuoco  di Fabio Abati";

        for (String word: s.toLowerCase().split(" ")){
            if(word.length() > 0 && d.contains(word)) {
                System.out.println(word);
                System.out.println("");
            }
        }

        System.out.println(d.size()); // 405400  check! have to be 405411!
    }
}