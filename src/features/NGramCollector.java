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
public class NGramCollector {

    public int N = 3;
    protected TST<Node> tokenIndex;
    protected ST<Integer, String> tokenIndexR;
    protected ST<Integer, Integer> phraseLengths;

    protected TST<Node> suffixIndex;
    protected ST<Integer, String> suffixIndexR;
    protected int suffixThreshold = 5;
    protected int maxSuffixLength = 4;

    protected double[] lambda = new double[N];

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
        suffixIndex = new TST<>();
        suffixIndexR = new ST<>();
        phraseLengths = new ST<>();
        for(int i = 0; i < N; i++) {
            lambda[i] = 0.0;
        }
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
        suffixIndex = new TST<>();
        suffixIndexR = new ST<>();
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
            smoothTrigramCounts();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    /**
     * smoothTrigramCount
     */
    public void smoothTrigramCounts() {
        for (String token : tokenIndex.keys()) {
            Node node = tokenIndex.get(token);
            if (N == 3) { // Implemented only for N = 3
                smoothingLambda(node, null, node);
            }
        }

        normalizeLambdas();
    }

    /**
     * smoothingLambda
     *
     * @param first First node
     * @param prev Second node
     * @param node Last node
     */
    protected void smoothingLambda(Node first, Node prev, Node node)
    {
        Node n;
        if (node.neighbors.size() == 0 && node.freq > 0 && prev != null) {

            double uni = (double) (tokenIndex.get(tokenIndexR.get(node.index)).freq - 1) / (double) (tokenIndex.size() - 1);
            double bi  = (double) (prev.freq - 1) / (double) (tokenIndex.get(tokenIndexR.get(prev.index)).freq - 1);
            double tri = 0.0;
            for(int index: first.neighbors.keys()) {
                if (prev.index == index) {
                    tri = (double) (node.freq - 1) / (double) (first.neighbors.get(index).freq - 1);
                }
            }

            if (uni > bi && uni > tri) {
                lambda[0] += node.freq;
            } else if (bi > uni && bi > tri) {
                lambda[1] += node.freq;
            } else if (tri > uni && tri > bi) {
                lambda[2] += node.freq;
            }

        } else {
            for(int index: node.neighbors.keys()) {
                n = node.neighbors.get(index);
                smoothingLambda(first, node, n);
            }
        }
    }

    /**
     * getSmoothedTrigramScore
     *
     * @param index Index
     * @param indexR Reverse index
     * @param first First node
     * @param prev Prev node
     * @param node Last node
     * @return score
     */
    protected double getSmoothedTrigramScore(TST<Node> index, ST<Integer, String> indexR, Node first, Node prev, Node node) {
        double uni = (double) index.get(indexR.get(node.index)).freq / (double) index.size();
        double bi  = (double) prev.freq / (double) index.get(indexR.get(prev.index)).freq;

        double tri = 0.0;
            for (int i : first.neighbors.keys()) {
                if (prev.index == i) {
                    tri = (double) node.freq / (double) first.neighbors.get(i).freq;
                }
            }

        return lambda[0]*uni + lambda[1]*bi + lambda[2]*tri;
    }

    /**
     * getSmoothedTrigramScore
     *
     * @param index Index
     * @param indexR Reverse index
     * @param first First node
     * @param prev Prev node
     * @param token Last trigram token
     * @return score
     */
    protected double getSmoothedTrigramScore(TST<Node> index, ST<Integer, String> indexR, Node first, Node prev, String token) {
        Node n = index.get(token);
        double uni = (n == null ? 0.0 : n.freq) / (double) index.size();
        double bi  = (double) prev.freq / (double) index.get(indexR.get(prev.index)).freq;

        return lambda[0]*uni + lambda[1]*bi;
    }

    /**
     * normalizeLambdas
     */
    protected void normalizeLambdas() {
        double sum = 0.0;
        for (double l: lambda) {
            sum += l;
        }
        if (sum > 0) {
            for (int i = 0; i < N; i++) {
                lambda[i] = lambda[i] / sum;
            }
        }
    }

    /**
     * Calculate factor
     */
    public void buildSuffixIndex()
    {
        Queue<String> q;
        for (String token: tokenIndex.keys()) {
            Node node = tokenIndex.get(token);
            if (node.freq < suffixThreshold) {
                q = new Queue<>();
                q.enqueue(token);
                addSuffixGrams(q, node);
            }
        }
    }

    /**
     * Calculate factor
     */
    protected void addSuffixGrams(Queue<String> q, Node node) {
        Node n;
        if (node.neighbors.size() == 0) {
            addSuffixGrams(q, new Queue<>());
        } else {
            for(int index: node.neighbors.keys()) {
                n = node.neighbors.get(index);
                q.enqueue(tokenIndexR.get(index));
                addSuffixGrams(q, n);
            }
        }
    }

    /**
     * addSuffixGrams
     *
     * @param t Tokens
     * @param s Suffix
     */
    protected void addSuffixGrams(Queue<String> t, Queue<String> s) {
        if (t.size() == 0) {

            //System.out.println(s);
            Queue<String> copy = new Queue<>();
            for(String token: s) {
                copy.enqueue(token);
            }
            for (String token: copy) {
                addSuffix(token);
            }
            while(copy.size() > 0) {
                addSuffixGram(copy);
                copy.dequeue();
            }

            return;
        }

        Queue<String> tCopy = new Queue<>();
        for (String token : t) {
            tCopy.enqueue(token);
        }

        Queue<String> sCopy;
        String suffix;

        suffix = getSuffix(tCopy.dequeue());
        while (suffix.length() > 0) {
            sCopy = new Queue<>();
            for (String suff : s) {
                sCopy.enqueue(suff);
            }
            sCopy.enqueue(suffix);
            addSuffixGrams(tCopy, sCopy);
            suffix = suffix.length() == 1 ? "" : suffix.substring(1, suffix.length());
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
        addGram(tokenIndex, tokens);
    }

    /**
     * Add N-gram to the collector
     *
     * @param tokens Tokens
     */
    protected void addSuffixGram(Queue<String> tokens)
    {
        addGram(suffixIndex, tokens);
    }

    /**
     * Add Gram
     *
     * @param index Index
     * @param tokens Tokens
     */
    protected void addGram(TST<Node> index, Queue<String> tokens) {
        if (tokens.size() == 0) throw new IllegalArgumentException("Empty tokens queue.");

        Node prev = null, node;
        int i = 0;
        for (String token: tokens) {
            //System.out.println("---" + token);
            node = index.get(token);
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

        Queue<String> qCopy = new Queue<>();
        for (String suff : q) {
            qCopy.enqueue(suff);
        }

        String token;
        Node prev = null, node = null, first = null;
        while (q.size() > 0) {
            token = q.dequeue();
            prev = node;
            if (prev == null) {
                node = tokenIndex.get(token);
                first = node;
                //if (node == null) {
                    //System.out.println("unknown  1:" + token);
                //}
            } else {
                node = takeNext(prev, token);
                if (node == null) {
                    //System.out.println("unknown  2:" + token);
                    if(n == 3 && first != prev) {
                        return getSmoothedTrigramScore(tokenIndex, tokenIndexR, first, prev, token);
                    }
                }
            }
        }

        if (node == null) {
            //System.out.println("unknown  1:" + token);
            return scoreSuffixGram(qCopy, n);
        }

        if (n == 1) {
            return (double) node.freq / tokenIndex.get("START").freq ;
        } else if (n > 1 && prev != null) {
            if (n == 3) {
                return getSmoothedTrigramScore(tokenIndex, tokenIndexR, first, prev, node);
            } else {
                return (double) node.freq / (double) prev.freq;
            }
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

        String token;
        Node prev = null, node = null, first = null;
        while (q.size() > 0) {
            token = q.dequeue();
            prev = node;
            if (prev == null) {
                node = suffixNode(token);
                first = node;
            } else {
                node = takeNextSuffixNode(prev, token);
                if (node == null) {
                    if(n == 3 && first != prev) {
                        return getSmoothedTrigramScore(suffixIndex, suffixIndexR, first, prev, token);
                    }
                }
            }
        }

        if (node == null) {
            return 0.0;
        }

        if (n == 1) {
            return (double) node.freq / tokenIndex.get("START").freq ;
        } else if (n > 1 && prev != null) {
            if (n == 3) {
                return getSmoothedTrigramScore(suffixIndex, suffixIndexR, first, prev, node);
            } else {
                return (double) node.freq / (double) prev.freq;
            }
        }

        return 0;
    }

    /**
     * suffixNode
     *
     * @param token Token
     * @return node
     */
    protected Node suffixNode(String token)
    {
        Node node = null;
        String suffix = getSuffix(token);
        while(suffix.length() > 0) {
            node = suffixIndex.get(suffix);
            if (node != null) break;
            suffix = suffix.length() == 1 ? "" : suffix.substring(1, suffix.length());
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
        return 1;
//        if (null == phraseLengths.get(tokens.size() - 2)) {
//            System.out.println("tokens size          " + tokens.size()); // ToDo handle unknown phrase length
//            return (double) 1 / (double) tokenIndex.get("START").freq;
//        } else {
//            return (double) phraseLengths.get(tokens.size() - 2) / (double) tokenIndex.get("START").freq;
//        }
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
            suffix = suffix.length() == 1 ? "" : suffix.substring(1, suffix.length());
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
