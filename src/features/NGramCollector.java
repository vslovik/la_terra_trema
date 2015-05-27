package features;

import algorithms.ST;
import algorithms.Queue;
import algorithms.TST;
import score.PhraseScoreTool;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The <tt>NGramCollector</tt> class represents N-Gram collection
 */
public class NGramCollector implements PhraseScoreTool {

    protected int N = 3;
    protected TST<Node> tokenIndex;
    protected ST<Integer, String> tokenIndexR;

    class Node
    {
        protected int freq;
        protected int index;
        protected ST<Integer, Node> neighbors = new ST<Integer, Node>();
    }

    /**
     * N-gram collector constructor
     *
     */
    public NGramCollector() {
        tokenIndex = new TST<Node>();
        tokenIndexR = new ST<Integer, String>();
    }

    /**
     * N-gram collector constructor
     *
     * @param corpusFile Corpus file
     */
    public NGramCollector(String corpusFile)
    {
        tokenIndex = new TST<Node>();
        tokenIndexR = new ST<Integer, String>();

        String line;
        BufferedReader br;
        Queue<String> tokens;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(corpusFile), "utf8"));

            tokens = new Queue<String>();

            while ((line = br.readLine()) != null) {

                if(line.equals("") && tokens.size() > 0) {
                    addPhrase(tokens);
                    tokens = new Queue<String>();
                    continue;
                }

                tokens.enqueue(line);
            }

            if(tokens.size() > 0) {
                addPhrase(tokens);
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    /**
     * Calculate phrase score
     *
     * @param tokens Phrase tokens
     * @return score
     */
    public double score(Queue<String> tokens)
    {
        Queue<String> copy = new Queue<String>();
        for(String token: tokens) {
            copy.enqueue(token);
        }
        double score = 0;
        while(copy.size() > 0) {
            score += scoreNGram(copy);
            copy.dequeue();
        }

        return score / tokens.size();
    }

    /**
     * Print N-Grams
     *
     * @param token Token
     */
    public void printNGrams(String token)
    {
        Node node = printUniGram(token);
        if (node == null) return;
        printNGrams(token, node.neighbors);
    }

    /**
     * Add N-gram to the collector
     *
     * @param tokens Tokens
     */
    protected void addNGram(Queue<String> tokens)
    {
        if (tokens.size() == 0) throw new IllegalArgumentException("Empty tokens queue.");

        Node prev = null, node;
        int i = 0;
        for (String token: tokens) {

            node = tokenIndex.get(token);
            if (prev != null && i < N) {
                node = addNeighbor(prev, node.index);
            }

            prev = node;
            i++;
        }
    }

    /**
     * Add Node to the collector
     *
     * @param token Token
     * @return Node
     */
    protected Node addNode(String token)
    {
        Node node;
        if (token.length() == 0) throw new IllegalArgumentException("Empty token.");
        node = tokenIndex.get(token);
        if (node == null) {
            node = new Node();
            node.index = tokenIndex.size();
            node.freq = 1;
            tokenIndex.put(token, node);
            tokenIndexR.put(node.index, token);
        } else {
            node.freq += 1;
        }

        return node;
    }

    /**
     * Add neighbor to node
     *
     * @param prev Previous node
     * @param index Next node index
     *
     * @return Node
     */
    protected Node addNeighbor(Node prev, int index)
    {
        if (prev == null) throw new IllegalArgumentException("Invalid node.");
        Node neighbor = prev.neighbors.get(index);
        if (neighbor == null) {
            neighbor = new Node();
            neighbor.index = index;
            neighbor.freq = 1;
            prev.neighbors.put(neighbor.index, neighbor);
        } else {
            neighbor.freq += 1;
        }

        return neighbor;
    }

    /**
     * Count N-Gram score
     *
     * @param tokens Token queue
     * @return score
     */
    protected double scoreNGram(Queue<String> tokens)
    {
        double score = 0;
        int i = 0;
        Node node, prev = null;

        for (String token : tokens) {
            if (i >= N) break;

            if (i == 0) {
                node = tokenIndex.get(token);
                if (node == null) return score;
                score += (double) node.freq / tokenIndexR.size();
            } else {

                node = takeNext(prev, token);
                if (node == null) return score;
                if (i == 1) {
                    score += (double) node.freq / prev.freq;
                }
            }

            prev = node;
            i++;
        }

        return score;
    }

    /**
     * Take next node
     * 
     * @param prev Previous node
     * @param token Token
     * @return Node
     */
    protected Node takeNext(Node prev, String token)
    {
        for (int index: prev.neighbors.keys()) {
            String t = tokenIndexR.get(index);
            if (t.equals(token)) {
                return prev.neighbors.get(index);
            }
        }
        
        return null;        
    }

    /**
     * Print uni gram
     *
     * @param token Token
     * @return Node
     */
    protected Node printUniGram(String token)
    {
        Node node = tokenIndex.get(token);
        if(node == null) return null;
        System.out.println(token + " " + node.freq);

        return node;
    }

    /**
     * Print N-Grams
     *
     * @param history Backward history
     * @param neighbors Neighbors
     */
    protected void printNGrams(String history, ST<Integer, Node> neighbors)
    {
        if (neighbors.size() == 0) return;
        for (int index: neighbors.keys()) {
            Node n = neighbors.get(index);
            System.out.println(history + " " + tokenIndexR.get(index) +  " " + n.freq);
            printNGrams(history + " " + tokenIndexR.get(index), n.neighbors);
        }
    }

    /**
     * Add phrase to the collector
     *
     * @param tokens Token queue
     */
    public void addPhrase(Queue<String> tokens)
    {
        Queue<String> copy = new Queue<String>();
        for(String token: tokens) {
            copy.enqueue(token);
        }
        for (String token: copy) {
            addNode(token);
        }
         while(copy.size() > 0) {
            addNGram(copy);
            copy.dequeue();
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
    public static void main(String[] args)
    {
        String file = "/home/lera/Desktop/LAUREA/la_terra_trema/test_data/montale.txt";
        NGramCollector nc = new NGramCollector(file);

        for(String token: nc.tokenIndex.keys()) {
            nc.printNGrams(token);
        }

        String file1 = "/home/lera/Desktop/LAUREA/la_terra_trema/test_data/montale0.txt";

        String[] files = {file, file1};
        for (String f: files) {
            String line;
            BufferedReader br;
            Queue<String> tokens;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf8"));

                tokens = new Queue<String>();

                while ((line = br.readLine()) != null) {

                    if(line.equals("") && tokens.size() > 0) {
                        System.out.println(tokens.toString() + ": " + nc.score(tokens));
                        continue;
                    }

                    tokens.enqueue(line);
                }

                if(tokens.size() > 0) {
                    System.out.println(tokens.toString() + ": " + nc.score(tokens));
                }

                br.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(e.toString());
            }
        }
    }

}
