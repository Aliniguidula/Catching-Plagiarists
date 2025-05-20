import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

public class DocumentIterator implements Iterator<String> {
    private final Reader r;
    private int currentCharacter = -1;
    private final int n;
    private final Queue<String> nGramQueue;

    public DocumentIterator(Reader r, int n) {
        if (r == null) {
            throw new NullPointerException("Reader cannot be null");
        }
        this.r = r;
        this.n = n;
        this.nGramQueue = new LinkedList<>();
        skipNonLetters();
    }

    private void skipNonLetters() {
        try {
            while (currentCharacter != -1 && !Character.isLetter(currentCharacter)) {
                currentCharacter = r.read();
            }
        } catch (IOException e) {
            currentCharacter = -1;
        }
    }

    @Override
    public boolean hasNext() {
        try {
            if (currentCharacter == -1) {
                currentCharacter = r.read();
                skipNonLetters();
            }
            return currentCharacter != -1;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        StringBuilder nGramBuilder = new StringBuilder();
        try {
            while (nGramQueue.size() < n && hasNext()) {
                StringBuilder wordBuilder = new StringBuilder();
                while (Character.isLetter(currentCharacter)) {
                    wordBuilder.append((char) currentCharacter);
                    currentCharacter = r.read();
                }
                if (wordBuilder.length() > 0) {
                    nGramQueue.offer(wordBuilder.toString().toLowerCase());
                }
                skipNonLetters();
            }

            if (nGramQueue.size() == n) {
                nGramBuilder.append(String.join("", nGramQueue));
                nGramQueue.poll();
            }
        } catch (IOException e) {
            throw new NoSuchElementException();
        }

        return nGramBuilder.toString();
    }
}