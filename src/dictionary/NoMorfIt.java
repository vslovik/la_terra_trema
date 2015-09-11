package dictionary;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class NoMorfIt {
    public static void main(String[] args)
    {
        System.gc();
        Runtime rt = Runtime.getRuntime();

        String lexicon  = args[0]; // /home/lera/Desktop/LAUREA/la_terra_trema/morfit/morph-it_048.txt
        String corpus  = args[1]; // /home/lera/Desktop/LAUREA/Training_pos_isst-paisa-devLeg.pos
                                 // /home/lera/Desktop/LAUREA/corpus_annotato_automaticamente.pos

        MorfItYesNoDictionary d = new MorfItYesNoDictionary(lexicon);

        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println("Memory usage: " + usedMB + "Mb\n");

        System.out.println("Corpus parsing result: ");

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(corpus), "utf8"));
            String s;
            while ((s = br.readLine()) != null) {
                s = s.replace(" ", "\t");
                String[] a = s.toLowerCase().split("\t");
                String word = a[0];
                word = word.replace(".", "").replace("rt", "").replace("\\", "").replaceAll("['~&;,\"]", "");
                word = word.replaceAll("\\d+", "");
                if (word.length() > 2
                        && !s.matches("^(http|@|#|tco).*")
                        && !d.contains(word)) {
                    System.out.println(word);
                    System.out.println("");
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }
}
