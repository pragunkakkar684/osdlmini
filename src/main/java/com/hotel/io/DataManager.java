package com.hotel.io;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic class that handles saving and loading any Serializable object list.
 *
 * KEY CONCEPTS:
 *
 * GENERICS (bounded) — <T extends Serializable> means T must be Serializable.
 *   The compiler will reject DataManager<SomeNonSerializableClass>.
 *   This is called a BOUNDED TYPE PARAMETER.
 *
 * BYTE STREAMS — ObjectOutputStream / ObjectInputStream are byte streams.
 *   They convert Java objects into a sequence of bytes (binary format) for filing.
 *   This is different from character streams (BufferedWriter) which write text.
 *
 * SERIALIZATION   — writing object → bytes → file  (ObjectOutputStream)
 * DESERIALIZATION — reading file → bytes → object  (ObjectInputStream)
 *
 * try-with-resources — the (Resource r = ...) syntax automatically closes
 *   streams even if an exception occurs. No need for a finally block.
 */
public class DataManager<T extends Serializable> {

    private final String filePath;

    public DataManager(String filePath) {
        this.filePath = filePath;
        ensureDirectoryExists(filePath);
    }

    /**
     * SERIALIZATION — saves a list of objects as binary bytes to a .dat file.
     *
     * Stream chain:
     *   List<T>  →  ObjectOutputStream  →  FileOutputStream  →  file on disk
     *             (wraps)                 (actual file writer)
     *
     * FileOutputStream  = byte stream to a file
     * ObjectOutputStream = wraps it, adds ability to write whole Java objects
     */
    public void save(List<T> items) throws IOException {
        // try-with-resources: streams auto-closed after block exits
        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(items); // serializes entire List<T> to bytes
        }
    }

    /**
     * DESERIALIZATION — loads bytes from file and reconstructs List<T>.
     *
     * Stream chain:
     *   file on disk  →  FileInputStream  →  ObjectInputStream  →  List<T>
     *
     * @SuppressWarnings("unchecked") suppresses the unavoidable cast warning.
     *  We KNOW the file contains List<T> because we wrote it — so the cast is safe.
     */
    @SuppressWarnings("unchecked")
    public List<T> load() throws IOException, ClassNotFoundException {
        File file = new File(filePath);

        // Return empty list if no file exists yet (first launch)
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            return (List<T>) ois.readObject(); // deserializes bytes → List<T>
        }
    }

    /** Creates parent directories if they don't exist (e.g., data/ folder) */
    private void ensureDirectoryExists(String path) {
        File parent = new File(path).getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs(); // creates all missing folders in the path
        }
    }

    public String getFilePath() { return filePath; }
}
