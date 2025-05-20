import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class DocumentsProcessor implements IDocumentsProcessor {
    @Override
    public Map<String, List<String>> processDocuments(String directoryPath, int n) {
        File dir = new File(directoryPath);
        File[] files = dir.listFiles();
        Map<String, List<String>> resultMap = new HashMap<>();

        if (files == null || files.length == 0) {
            System.err.println("Directory is empty or invalid: " + directoryPath);
            return resultMap;
        }

        for (File f : files) {
            if (f.isFile() && f.length() > 0) { // Skip empty files
                try (Reader r = new FileReader(f)) {
                    resultMap.put(f.getName(), new ArrayList<>());
                    DocumentIterator di = new DocumentIterator(r, n);
                    while (di.hasNext()) {
                        resultMap.get(f.getName()).add(di.next());
                    }
                } catch (IOException e) {
                    System.err.println("Found error while reading: " + f.getName());
                }
            }
        }

        return resultMap;
    }

    @Override
    public List<Tuple<String, Integer>> storeNGrams(Map<String, List<String>> docs,
                                                    String nwordFilePath) {
        List<Tuple<String, Integer>> resultList = new ArrayList<>();
        File nGramFile = new File(nwordFilePath);

        if (checkDir(nGramFile)) {
            return resultList;
        }

        try (FileWriter writer = new FileWriter(nGramFile, true)) {
            for (String key : docs.keySet()) {
                int byteCount = 0;
                for (String nGram : docs.get(key)) {
                    String nGramWithSpace = nGram + " ";
                    writer.write(nGramWithSpace);
                    byteCount += nGramWithSpace.getBytes().length;
                }
                resultList.add(new Tuple<>(key, byteCount));
            }
        } catch (IOException e) {
            System.err.println("Error writing to n-gram file: " + e.getMessage());
        }

        return resultList;
    }

    private boolean checkDir(File nGramFile) {
        File dir = nGramFile.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        if (nGramFile.exists()) {
            nGramFile.delete();
        }

        try {
            nGramFile.createNewFile();
        } catch (Exception e) {
            System.err.println("Error creating new file : " + e.getMessage());
            return true;
        }
        return false;
    }

    public List<Tuple<String, Integer>> processAndStore(String directoryPath,
                                                        String sequenceFile, int n) {

        List<Tuple<String, Integer>> resultList = new ArrayList<>();
        File dir = new File(directoryPath);
        File[] files = dir.listFiles();

        File nGramFile = new File(sequenceFile);
        if (checkDir(nGramFile)) {
            return resultList;
        }

        int byteCnt;
        String nGram;
        assert files != null;
        for (File f : files) {
            if (f.isFile()) {
                byteCnt = 0;
                try {
                    Reader r = new FileReader(f);
                    DocumentIterator di = new DocumentIterator(r, n);
                    FileWriter w = new FileWriter(nGramFile, true);
                    while (di.hasNext()) {
                        nGram = di.next();
                        nGram += " ";
                        w.write(nGram);
                        byteCnt += nGram.getBytes().length;
                    }
                    w.close();
                    r.close();
                    resultList.add(new Tuple<>(f.getName(), byteCnt));

                } catch (IOException e) {
                    System.err.println("Error happened when writing on nGramTxt " +
                            e.getMessage());
                }

            }
        }

        return resultList;
    }

    @Override
    public TreeSet<Similarities> computeSimilarities(String nwordFilePath,
                                                     List<Tuple<String, Integer>> fileindex) {
        TreeSet<Similarities> resultTreeSet = new TreeSet<>();

        List<List<Similarities>> simList = new ArrayList<>();
        for (int i = 0; i < fileindex.size(); i++) {
            ArrayList<Similarities> iRow = new ArrayList<>();
            for (int j = i + 1; j < fileindex.size(); j++) {
                iRow.add(new Similarities(fileindex.get(i).getLeft(), fileindex.get(j).getLeft()));
            }
            simList.add(iRow);
        }

        ArrayList<Integer> cumByteIdxList = new ArrayList<>();
        int byteCum = 0;
        for (Tuple<String, Integer> t: fileindex) {
            byteCum += t.getRight();
            cumByteIdxList.add(byteCum);
        }

        File nwordsFile = new File(nwordFilePath);
        Reader r;
        DocumentIterator di;
        try {
            r = new FileReader(nwordsFile);
            di = new DocumentIterator(r, 1);
        } catch (IOException e) {
            System.err.println("Error opening nwordsFile : " + e.getMessage());
            return resultTreeSet;
        }

        HashMap<String, List<Integer>> nwordMap = new HashMap<>();
        int byteSum = 0;
        Similarities sim;
        while (di.hasNext()) {
            String currNword = di.next();
            byteSum += currNword.length() + 1;

            int currIdx = fileIndexWithBinarySearch(cumByteIdxList, byteSum);

            if (!nwordMap.containsKey(currNword)) {
                nwordMap.put(currNword, new ArrayList<>());
            } else {
                for (int prevIdx: nwordMap.get(currNword)) {
                    if (prevIdx == currIdx) {
                        continue;
                    }
                    sim = simList.get(prevIdx).get(currIdx - prevIdx - 1);
                    sim.setCount(sim.getCount() + 1);
                }
            }
            nwordMap.get(currNword).add(currIdx);
        }

        try {
            r.close();
        } catch (IOException e) {
            System.err.println("Error closing nwordsFile : " + e.getMessage());
            return resultTreeSet;
        }

        for (List<Similarities> l : simList) {
            for (Similarities s : l) {
                if (s.getCount() > 0) {
                    resultTreeSet.add(s);
                }
            }
        }

        return resultTreeSet;
    }

    public static int fileIndexWithBinarySearch(List<Integer> byteCumList,
                                                int byteVal) {
        if (byteVal < 0 || byteVal > byteCumList.get(byteCumList.size() - 1)) {
            return -1;
        }

        int idx = Collections.binarySearch(byteCumList, byteVal);

        if (idx < 0) {
            idx = (idx + 1) * (-1);
        }

        return idx;
    }

    public static Reader createPartialReader(File file, int start, int length)
            throws IOException {
        try (RandomAccessFile ra = new RandomAccessFile(file, "r")) {
            ra.seek(start);
            byte[] buffer = new byte[(int) length];
            ra.readFully(buffer);

            return new InputStreamReader(new ByteArrayInputStream(buffer));
        }
    }

    @Override
    public void printSimilarities(TreeSet<Similarities> sims, int threshold) {
        while (!sims.isEmpty()) {
            Similarities s = sims.pollLast();
            assert s != null;
            if (s.getCount() < threshold) {
                break;
            }
            System.out.println(s);
        }
    }
}