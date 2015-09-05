package features;

import algorithms.ST;
import algorithms.Queue;
import algorithms.TST;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The <tt>NGramCollector</tt> class represents N-Gram collection
 */
public class NGramCollector {

    public int N = 3;
    protected TST<Node> tokenIndex;
    protected ST<Integer, String> tokenIndexR;
    protected ST<Integer, Integer> phraseLengths;
    protected double factor;

    class Node
    {
        protected int freq;
        protected int index;
        protected ST<Integer, Node> neighbors = new ST<Integer, Node>();
    }

    /**
     * N-gram collector constructor
     */
    public NGramCollector() {
        tokenIndex = new TST<>();
        tokenIndexR = new ST<>();
        phraseLengths = new ST<>();
    }

    /**
     * N-gram collector constructor
     *
     * @param corpusFile Corpus file
     */
    public NGramCollector(String corpusFile)
    {
        tokenIndex = new TST<>();
        tokenIndexR = new ST<>();
        phraseLengths = new ST<>();

        String line;
        BufferedReader br;
        Queue<String> tokens;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(corpusFile), "utf8"));

            tokens = new Queue<>();

            while ((line = br.readLine()) != null) {

                if(line.equals("") && tokens.size() > 0) {
                    addPhrase(tokens);
                    tokens = new Queue<>();
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

        calculateFactor();
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
     * Add N-gram to the collector
     *
     * @param tokens Tokens
     */
    protected void addFirstGram(Queue<String> tokens)
    {
        if (tokens.size() == 0) throw new IllegalArgumentException("Empty tokens queue.");

        Node prev = null, node;
        int i = 0;
        for (String token: tokens) {

            node = tokenIndex.get(token);
            if (prev != null) {
                node = addNeighbor(prev, node.index);
            }

            i++;
            if (i == N) {
                break;
            } else {
                prev = node;
            }
        }
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
     * Score n-gram
     *
     * @param q n-gram queue
     * @param n n-gram rang
     * @return score
     */
    public double scoreGram(Queue<String> q, int n)
    {
        if (q.size() < n) throw new IllegalArgumentException();

        String token;
        Node prev = null, node = null;
        while (q.size() > 0) {
            token = q.dequeue();
            prev = node;
            if (prev == null) {
                node = tokenIndex.get(token);
                if (node == null) {
                    System.out.println("lll  1:" + token); // ToDo Handle unknown words
                    return 0;
                }
            } else {
                node = takeNext(prev, token);
                if (node == null) {
                    System.out.println("lll  2:" + token); // ToDo Handle unknown words
                    return 0;
                }
            }
        }

        if (n == 1 && node != null) {
            return (double) node.freq / tokenIndex.get("START").freq ;
        } else if (n > 1 && node != null && prev != null){
            return (double) node.freq / (double) prev.freq;
        }

        return 0;
    }

    public double scorePhaseLength(Queue<String> tokens)
    {
        if (null == phraseLengths.get(tokens.size() - 2)) {
            System.out.println("tokens size          " + tokens.size()); // ToDo handle unknown phrase length
            return (double) 1 / (double) tokenIndex.get("START").freq;
        } else {
            return (double) phraseLengths.get(tokens.size() - 2) / (double) tokenIndex.get("START").freq;
        }
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
     * Calculate factor
     */
    public void calculateFactor()
    {
        for (String token: tokenIndex.keys()) {
            Node node = tokenIndex.get(token);
            calculateFactor(null, node);
        }
    }

    /**
     * Calculate factor
     */
    protected void calculateFactor(Node prev, Node node) {
        if (node.neighbors.size() == 0) {
            if (prev != null) {
                factor += (double) node.freq / (double) prev.freq;
            }
        } else {
            for(int index: node.neighbors.keys()) {
                Node n = node.neighbors.get(index);
                calculateFactor(node, n);
            }
        }
    }

    /**
     * Add phrase to the collector
     *
     * @param tokens Token queue
     */
    public void addPhrase(Queue<String> tokens)
    {
        addLength(tokens);

        Queue<String> copy = new Queue<>();
        for(String token: tokens) {
            copy.enqueue(token);
        }
        for (String token: copy) {
            addNode(token);
        }
         while(copy.size() > 0) {
            addFirstGram(copy);
            copy.dequeue();
        }
    }

    /**
     * Keep phrase length frequencies
     *
     * @param tokens Token queue
     */
    protected void addLength(Queue<String> tokens)
    {
        int lenKey = tokens.size() - 2;
        int lenValue;
        if (!phraseLengths.contains(lenKey)) {
            lenValue = 1;
        } else {
            lenValue = 1 + phraseLengths.get(lenKey);
        }
        phraseLengths.put(lenKey, lenValue);
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
    }

}
