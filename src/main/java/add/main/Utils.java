package add.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by fermadeiral
 */
public class Utils {
    private static Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static String getFullPath(String projectRoot, String fileName) {
        if (!fileName.startsWith("/")) {
            fileName = "/" + fileName;
        }
        if (!fileName.contains(projectRoot)) {
            fileName = projectRoot + fileName;
        }
        return fileName;
    }

    public static List<String> fileToLines(String filename) {
        List<String> lines = new LinkedList<String>();
        String line;
        try {
            InputStream is = new FileInputStream(filename);
            int BUFFER_SIZE = 8192;

            BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.ISO_8859_1), BUFFER_SIZE);
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            LOGGER.error(e.toString());
        }
        return lines;
    }

}
