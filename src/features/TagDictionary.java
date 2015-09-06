package features;

import algorithms.Queue;
import algorithms.ST;
import algorithms.TST;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The <tt>TagDictionary</tt> class represents Token - Tag dictionary
 */
public class TagDictionary {

    protected TST<Node> tokenIndex;

    class Node
    {
        protected int freq;
        protected String token;
        protected ST<String, Integer> tags = new ST<>();
    }

    /**
     * N-gram collector constructor
     *
     * @param corpusFile Corpus file
     */
    public TagDictionary(String corpusFile)
    {
        tokenIndex = new TST<>();

        String line;
        BufferedReader br;
        Queue<String> pairs;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(corpusFile), "utf8"));

            pairs = new Queue<>();

            while ((line = br.readLine()) != null) {

                if(line.equals("") && pairs.size() > 0) {
                    addTaggedPhrase(pairs);
                    pairs = new Queue<>();
                    continue;
                }

                pairs.enqueue(line);
            }

            if(pairs.size() > 0) {
                addTaggedPhrase(pairs);
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    /**
     * Add Node to the collector
     *
     * @param pair Token - Tag pair
     */
    protected void addNode(String pair)
    {
        String[] arr = pair.split("\t");
        if (arr.length < 2) throw new IllegalArgumentException("Invalid Token - Tag pair.");
        String token = arr[0];
        String tag = arr[1];

        Node node;
        if (token.length() == 0) throw new IllegalArgumentException("Empty token.");
        if (tag.length() == 0) throw new IllegalArgumentException("Empty tag.");
        node = tokenIndex.get(token);
        if (node == null) {
            node = new Node();
            node.freq = 1;
            node.token = token;
            node.tags = new ST<>();
            node.tags.put(tag, 1);
        } else {
            node.freq += 1;
            if (node.tags.contains(tag)) {
                node.tags.put(tag, node.tags.get(tag) + 1);
            } else {
                node.tags.put(tag, 1);
            }
        }
    }

    /**
     * Add tagged phrase to the dictionary
     *
     * @param pairs Token\tTag queue
     */
    public void addTaggedPhrase(Queue<String> pairs)
    {
        Queue<String> copy = new Queue<String>();
        for(String pair: pairs) {
            copy.enqueue(pair);
        }
        for (String pair: copy) {
            addNode(pair);
        }
    }

    /**
     *  Size
     *
     * @return collection size
     */
    public int size()
    {
        return tokenIndex.size();
    }

    /**
     * Unit tests the <tt>NGramCollector</tt> data type.
     */
    public static void main(String[] args) // ToDo write the test
    {
        String file = "/home/lera/Desktop/LAUREA/la_terra_trema/test_data/montale.txt";
        TagDictionary nc = new TagDictionary(file);

        // print
    }

}
