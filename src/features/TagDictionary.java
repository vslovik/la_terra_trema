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
    protected TST<Node> suffixIndex;
    protected int suffixThreshold = 10;
    protected int maxSuffixLength = 4;

    class Node
    {
        protected int freq;
        protected String token;
        protected ST<String, Double> tags = new ST<>();
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

            buildSuffixIndex();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    /**
     * buildSuffixIndex
     */
    public void buildSuffixIndex() {
        String suffix;
        for (String token : tokenIndex.keys()) {
            Node node = tokenIndex.get(token);
            if (node.freq < suffixThreshold) {
                suffix = getSuffix(token);
                for (String tag : node.tags.keys()) {
                    while (suffix.length() > 0) {
                        addSuffix(suffix, tag);
                        suffix = suffix.substring(1, suffix.length() - 1);
                    }
                }
            }
        }
    }

    /**
     * buildSuffixIndex
     *
     * @param teta Smoothing factor
     */
    public void buildSuffixIndex(double teta)
    {
        buildSuffixIndex();
        smoothSuffixCounts(teta);
    }

    /**
     * getSuffix
     *
     * @param token Token
     * @return Suffix
     */
    protected String getSuffix(String token)
    {
        if (token.length() == 0) throw new IllegalArgumentException("Empty token");
        int length = token.length();
        if (length > maxSuffixLength) {
            return token.substring(length - maxSuffixLength, length - 1);
        } else {
            return token;
        }
    }

    /**
     * Add Node to the token-tag dictionary
     *
     * @param token Token
     * @param tag Tag
     */
    protected void addNode(String token, String tag)
    {
        addNode(tokenIndex, token, tag);
    }

    /**
     * Add Node to the suffix-tag dictionary
     *
     * @param suffix Suffix
     * @param tag Tag
     */
    protected void addSuffix(String suffix, String tag)
    {
        addNode(suffixIndex, suffix, tag);
    }

    /**
     * Add Node to the collector
     *
     * @param token Token
     * @param tag Tag
     */
    protected void addNode(TST<Node> index, String token, String tag)
    {
        Node node;
        if (token.length() == 0) throw new IllegalArgumentException("Empty token.");
        if (tag.length() == 0) throw new IllegalArgumentException("Empty tag.");
        node = index.get(token);
        if (node == null) {
            node = new Node();
            node.freq = 1;
            node.token = token;
            node.tags = new ST<>();
            node.tags.put(tag, 1.0);
        } else {
            node.freq += 1;
            if (node.tags.contains(tag)) {
                node.tags.put(tag, node.tags.get(tag) + 1.0);
            } else {
                node.tags.put(tag, 1.0);
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
    public double count(String token, String tag) {
        Node node;
        node = tokenIndex.get(token);

        if (node == null) {
            return 0;
        }

        if (!node.tags.contains(tag)) {
            throw new IllegalArgumentException("Empty tags.");
        }

        return node.tags.get(tag);
    }

    /**
     * Count
     *
     * @param token Token
     * @param tag Tag
     * @return count
     */
    public double suffixCount(String token, String tag) {
        Node node = null;
        String suffix = getSuffix(token);
        while(suffix.length() > 0) {
            node = suffixIndex.get(suffix);
            if (node != null) {
                break;
            }
            suffix = suffix.substring(1, suffix.length() - 1);
        }

        if (node == null) {
            return 0;
        }

        if (!node.tags.contains(tag)) {
            throw new IllegalArgumentException("Empty tags."); // ToDo: change type of exeptio
        }

        return (double) node.tags.get(tag);
    }

    /**
     * smoothSuffixCounts
     *
     * @param teta smoothing factor
     */
     public void smoothSuffixCounts(double teta)
     {
         Node n, prev = null;
         double freq;
         String suffix;
         for(String token: tokenIndex.keys()) {
             Node node = tokenIndex.get(token);
             if (node.freq < suffixThreshold) {
                 suffix = getSuffix(token);
                 for(String tag: node.tags.keys()) {
                     while(suffix.length() > 0) {
                         n = suffixIndex.get(suffix);
                         if (prev != null) {
                             freq = (node.tags.get(tag) + teta * prev.tags.get(tag)) / (1 + teta);
                             node.tags.put(tag, freq);
                         }
                         suffix = suffix.substring(1, suffix.length() - 1);
                         prev = n;
                     }
                 }
             }
         }
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
