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
     * Tag dictionary constructor
     */
    public TagDictionary() {
        tokenIndex = new TST<>();
    }

    /**
     * Tag dictionary constructor
     *
     * @param corpusFile Corpus file
     */
    public TagDictionary(String corpusFile)
    {
        tokenIndex = new TST<>();

        String line;
        BufferedReader br;
        Queue<String> tokens, tags;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(corpusFile), "utf8"));

            tokens = new Queue<>();
            tags = new Queue<>();

            while ((line = br.readLine()) != null) {

                if(line.equals("")) {
                    if (tokens.size() > 0) {
                        addTaggedPhrase(tokens, tags);
                        tokens = new Queue<>();
                        tags = new Queue<>();
                    }
                    continue;
                }

                String[] arr = line.split("\t");
                if (arr.length < 2) throw new IllegalArgumentException("Invalid token\ttag line");
                tokens.enqueue(arr[0]);
                tags.enqueue(arr[1]);
            }

            if(tokens.size() > 0) {
                addTaggedPhrase(tokens, tags);
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
     * @param token Token
     * @param tag Tag
     */
    protected void addNode(String token, String tag)
    {
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
     * @param tokens Token queue
     * @param tags Tag queue
     */
    public void addTaggedPhrase(Queue<String> tokens, Queue<String> tags)
    {
        Queue<String> tagsCopy = new Queue<String>();
        for(String tag: tags) {
            tagsCopy.enqueue(tag);
        }
        String tag;
        for (String token: tokens) {
            tag = tagsCopy.dequeue();
            addNode(token, tag);
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
     * Count
     *
     * @param token Token
     * @param tag Tag
     * @return count
     */
    public int count(String token, String tag) {
        Node node = tokenIndex.get(token);
        if (node == null) {
           return 0;
        }
        if (!node.tags.contains(tag)) {
           return 0;
        }

        return node.tags.get(tag);
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
