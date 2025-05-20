import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class DocumentsProcessorTest {
    private DocumentsProcessor d;
    private File smDir;
    private File nwordFile;

    @Before
    public void setUp() throws IOException {
        d = new DocumentsProcessor();
        smDir = new File("sm_doc_set");
        if (!smDir.exists()) {
            smDir.mkdir();
        }

        for (char c = 'a'; c <= 'z'; c++) {
            File file = new File(smDir, c + ".txt");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("This is a test file for letter " + c + ".\n");
            }
        }

        nwordFile = new File("nword.txt");
        if (nwordFile.exists()) {
            nwordFile.delete();
        }
        nwordFile.createNewFile();
    }

    @After
    public void tearDown() {
        if (smDir.exists()) {
            for (File file : smDir.listFiles()) {
                file.delete();
            }
            smDir.delete();
        }
        if (nwordFile.exists()) {
            nwordFile.delete();
        }
    }

    @Test
    public void readFilesInDirectoryTest() {
        Map<String, List<String>> fileMap = d.processDocuments(smDir.getPath(), 2);
        assertNotNull(fileMap);
        assertEquals(26, fileMap.size());
    }

    @Test
    public void generateNGramTest() {
        Map<String, List<String>> fileMapN2 = d.processDocuments(smDir.getPath(), 2);
        List<String> firstFileNGrams = fileMapN2.get(fileMapN2.keySet().iterator().next());
        assertNotNull(firstFileNGrams);
        assertTrue(firstFileNGrams.size() > 0);
    }

    @Test
    public void storeNGramsDirectoryFileGenerationTest() {
        Map<String, List<String>> fileMapN3 = d.processDocuments(smDir.getPath(), 3);
        d.storeNGrams(fileMapN3, nwordFile.getPath());
        assertTrue(nwordFile.exists());
        assertTrue(nwordFile.length() > 0);
    }

    @Test
    public void storeNGramsTest() {
        Map<String, List<String>> fileMapN3 = d.processDocuments(smDir.getPath(), 3);
        List<Tuple<String, Integer>> summary = d.storeNGrams(fileMapN3, nwordFile.getPath());
        assertEquals(26, summary.size());
    }

    @Test
    public void computeSimilaritiesNumberOfSimsTest() {
        Map<String, List<String>> fileMapN3 = d.processDocuments(smDir.getPath(), 3);
        List<Tuple<String, Integer>> summary = d.storeNGrams(fileMapN3, nwordFile.getPath());
        TreeSet<Similarities> ts = d.computeSimilarities(nwordFile.getPath(), summary);
        assertTrue(ts.size() > 0);
    }

    @Test
    public void createPartialReaderTest() throws IOException {
        File firstFile = new File(smDir, "a.txt");
        try (Reader oneRead = DocumentsProcessor.createPartialReader(firstFile, 1, 19)) {
            DocumentIterator di = new DocumentIterator(oneRead, 1);
            assertNotNull(di.next());
            assertTrue(di.hasNext());
        }
    }

    @Test
    public void computeSimilaritiesNoMatchesTest() {
        Map<String, List<String>> fileMap = d.processDocuments(smDir.getPath(), 3);
        List<Tuple<String, Integer>> summary = d.storeNGrams(fileMap, nwordFile.getPath());

        TreeSet<Similarities> ts = d.computeSimilarities("non_matching_file.txt", summary);
        assertTrue(ts.isEmpty());
    }

    @Test
    public void computeSimilaritiesInvalidFilePathTest() {
        Map<String, List<String>> fileMap = d.processDocuments(smDir.getPath(), 3);
        List<Tuple<String, Integer>> summary = d.storeNGrams(fileMap, nwordFile.getPath());

        TreeSet<Similarities> ts = d.computeSimilarities("invalid_file_path.txt", summary);
        assertTrue(ts.isEmpty());
    }

    @Test
    public void processDocumentsWithDifferentNGramSizesTest() {
        Map<String, List<String>> fileMapN2 = d.processDocuments(smDir.getPath(), 2);
        Map<String, List<String>> fileMapN4 = d.processDocuments(smDir.getPath(), 4);

        assertNotNull(fileMapN2);
        assertNotNull(fileMapN4);

        assertTrue(fileMapN2.size() > 0);
        assertTrue(fileMapN4.size() > 0);
    }

    @Test
    public void storeNGramsEmptyTest() {
        Map<String, List<String>> emptyMap = new HashMap<>();
        List<Tuple<String, Integer>> summary = d.storeNGrams(emptyMap, nwordFile.getPath());

        assertTrue(summary.isEmpty());
    }

    @Test
    public void performanceTestWithLargeDataset() throws IOException {
        File largeDir = new File("large_doc_set");
        if (!largeDir.exists()) {
            largeDir.mkdir();
        }

        for (int i = 0; i < 100; i++) {
            File file = new File(largeDir, "file" + i + ".txt");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Test content for file " + i + ".\n");
            }
        }

        Map<String, List<String>> fileMap = d.processDocuments(largeDir.getPath(), 3);
        List<Tuple<String, Integer>> summary = d.storeNGrams(fileMap, nwordFile.getPath());
        TreeSet<Similarities> ts = d.computeSimilarities(nwordFile.getPath(), summary);

        assertTrue(ts.size() > 0);

        for (File file : largeDir.listFiles()) {
            file.delete();
        }
        largeDir.delete();
    }

    @Test
    public void handleFilesWithSpecialCharactersTest() throws IOException {
        File specialCharFile = new File(smDir, "file_#@!.txt");
        try (FileWriter writer = new FileWriter(specialCharFile)) {
            writer.write("Test content with special characters in the file name.\n");
        }

        Map<String, List<String>> fileMap = d.processDocuments(smDir.getPath(), 3);
        assertTrue(fileMap.containsKey("file_#@!.txt"));

        specialCharFile.delete();
    }

    @Test
    public void testStoreNGramsWithEmptyMap() {
        Map<String, List<String>> emptyMap = new HashMap<>();
        List<Tuple<String, Integer>> summary = d.storeNGrams(emptyMap, nwordFile.getPath());
        assertTrue(summary.isEmpty());
    }

    @Test
    public void testNonLetterFile() throws IOException {
        File nonLetterFile = new File(smDir, "non_letter.txt");
        try (FileWriter writer = new FileWriter(nonLetterFile)) {
            writer.write("12345 !@#$%^&*()\n");
        }

        Map<String, List<String>> fileMap = d.processDocuments(smDir.getPath(), 2);
        assertTrue(fileMap.containsKey("non_letter.txt"));
        assertTrue(fileMap.get("non_letter.txt").isEmpty());

        nonLetterFile.delete();
    }

    @Test(expected = NullPointerException.class)
    public void testNullReader() {
        new DocumentIterator(null, 2);
    }

    @Test
    public void testLargeNGramSize() {
        Map<String, List<String>> fileMap = d.processDocuments(smDir.getPath(), 100);
        assertNotNull(fileMap);
        assertTrue(fileMap.size() > 0);
    }

    @Test
    public void testOverlappingNGrams() throws IOException {
        File overlapFile = new File(smDir, "overlap.txt");
        try (FileWriter writer = new FileWriter(overlapFile)) {
            writer.write("a b c d e f g h i j k l m n o p q r s t u v w x y z\n");
        }

        Map<String, List<String>> fileMap = d.processDocuments(smDir.getPath(), 3);
        List<String> nGrams = fileMap.get("overlap.txt");
        assertNotNull(nGrams);
        assertTrue(nGrams.contains("abc"));
        assertTrue(nGrams.contains("bcd"));
        assertTrue(nGrams.contains("cde"));

        overlapFile.delete();
    }

    @Test
    public void testMixedContentFile() throws IOException {
        File mixedFile = new File(smDir, "mixed.txt");
        try (FileWriter writer = new FileWriter(mixedFile)) {
            writer.write("a1b2c3 d4e5f6 g7h8i9\n");
        }

        Map<String, List<String>> fileMap = d.processDocuments(smDir.getPath(), 2);
        assertTrue(fileMap.containsKey("mixed.txt"));
        List<String> nGrams = fileMap.get("mixed.txt");
        assertNotNull(nGrams);
        assertTrue(nGrams.contains("ab"));
        assertTrue(nGrams.contains("bc"));
        assertTrue(nGrams.contains("de"));
        assertTrue(nGrams.contains("ef"));
        assertTrue(nGrams.contains("gh"));
        assertTrue(nGrams.contains("hi"));

        mixedFile.delete();
    }

}