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

    protected TST<Node> suffixIndex;
    protected ST<Integer, String> suffixIndexR;
    protected int suffixThreshold = 10;
    protected int maxSuffixLength = 4;

    class Node
    {
        protected int freq;
        protected int index;
        protected ST<Integer, Node> neighbors = new ST<>();
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
                while (suffix.length() > 0) {
                    addSuffix(suffix);
                    suffix = suffix.substring(1, suffix.length() - 1);
                }
            }
        }
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
     * @param token Suffix
     */
    protected Node addNode(String token)
    {
        return addNode(tokenIndex, tokenIndexR, token);
    }

    /**
     * Add Node to the collector
     *
     * @param suffix Suffix
     */
    protected Node addSuffix(String suffix)
    {
        return addNode(suffixIndex, suffixIndexR, suffix);
    }

    /**
     * Add Node to the collector
     *
     * @param token Token
     * @return Node
     */
    protected Node addNode(TST<Node> index, ST<Integer, String> indexR, String token)
    {
        Node node;
        if (token.length() == 0) throw new IllegalArgumentException("Empty token.");
        node = index.get(token);
        if (node == null) {
            node = new Node();
            node.index = index.size();
            node.freq = 1;
            index.put(token, node);
            indexR.put(node.index, token);
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
                    System.out.println("unknown  1:" + token);
                }
            } else {
                node = takeNext(prev, token);
                if (node == null) {
                    System.out.println("unknown  2:" + token); // ToDo: smoothing for trigram counts
                }
            }
        }

        if (node == null) {
            return scoreSuffixGram(q, n);
        }

        if (n == 1) {
            return (double) node.freq / tokenIndex.get("START").freq ;
        } else if (n > 1 && prev != null) {
            return (double) node.freq / (double) prev.freq;
        }

        return 0;
    }

    /**
     * Score n-gram
     *
     * @param q n-gram queue
     * @param n n-gram rang
     * @return score
     */
    public double scoreSuffixGram(Queue<String> q, int n)
    {
        if (q.size() < n) throw new IllegalArgumentException();

        String token, suffix;
        Node prev = null, node = null;
        while (q.size() > 0) {
            token = q.dequeue();
            prev = node;
            if (prev == null) {
                node = suffixNode(token);
                if (node == null) {
                    System.out.println("s unknown  1:" + token);
                }
            } else {
                node = takeNextSuffixNode(prev, token);
                if (node == null) {
                    System.out.println("s unknown  2:" + token); // ToDo: smoothing for trigram counts
                }
            }
        }

        if (node == null) {
            return scoreSuffixGram(q, n);
        }

        if (n == 1) {
            return (double) node.freq / tokenIndex.get("START").freq ;
        } else if (n > 1 && prev != null) {
            return (double) node.freq / (double) prev.freq;
        }

        return 0;
    }

    protected Node suffixNode(String token)
    {
        Node node = null;
        String suffix = getSuffix(token);
        while(suffix.length() > 0) {
            node = suffixIndex.get(suffix);
            if (node != null) break;
            suffix = suffix.substring(1, suffix.length() - 1);
        }

        return node;
    }

    /**
     * scorePhaseLength
     *
     * @param tokens Token queue
     * @return score
     */
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
     * Take next node
     *
     * @param prev Previous node
     * @param token Token
     * @return Node
     */
    protected Node takeNextSuffixNode(Node prev, String token)
    {
        String suffix = getSuffix(token);
        while(suffix.length() > 0) {
            for (int index : prev.neighbors.keys()) {
                String t = suffixIndexR.get(index);

                if (t.equals(suffix)) {
                    return prev.neighbors.get(index);
                }
            }
            suffix = suffix.substring(1, suffix.length() - 1);
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
     * Count
     *
     * @param token Token
     * @return count
     */
    public int count(String token) {
        Node node = tokenIndex.get(token);
        if (node == null) {
            return 0;
        }

        return node.freq;
    }

    /**
     * suffixSmoothingFactor
     *
     * @return factor
     */
    public double suffixSmoothingFactor()
    {
        int sum = 0;
        for(String token: tokenIndex.keys()) {
            sum += tokenIndex.get(token).freq;
        }
        double avg = (double) sum / (double) tokenIndex.size();

        double teta = 0.0;
        for(String token: tokenIndex.keys()) {
            teta += Math.pow(tokenIndex.get(token).freq - avg, 2);
        }

        return teta / (double) tokenIndex.size();
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
