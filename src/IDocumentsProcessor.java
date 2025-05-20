import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * @author ericfouh
 */
public interface IDocumentsProcessor {
        /**
         * @param directoryPath - the path to the directory
         * @param n             - the size of n-gram to use
         * @return collection of files with n-grams in each
         */
        Map<String, List<String>> processDocuments(String directoryPath, int n);

        /**
         * We write ngrams sequentially in the file. they are separated by
         * a space
         * 
         * @param docs          - map of string with list of all ngrams
         * @param nwordFilePath of the file to store the ngrams
         * @return a list of file and size (in byte) of character written in file
         *         path
         */
        List<Tuple<String, Integer>> storeNGrams(
                        Map<String, List<String>> docs,
                        String nwordFilePath);

        List<Tuple<String, Integer>> processAndStore(String directoryPath, String sequenceFile, int n);

        /**
         * @param nwordFilePath of the file to store the n-grams
         * @param fileindex     - a list of tuples representing each file and its size
         *                      in nwordFile
         * @return a TreeSet of file similarities. Each Similarities instance
         *         encapsulates the files (two) and the number of n-grams
         *         they have in common
         */

        public TreeSet<Similarities> computeSimilarities(
                        String nwordFilePath,
                        List<Tuple<String, Integer>> fileindex);

        /**
         * @param sims      - the TreeSet of Similarities
         * @param threshold - only Similarities with a count greater than threshold
         *                  are printed
         */
        public void printSimilarities(TreeSet<Similarities> sims, int threshold);
}
