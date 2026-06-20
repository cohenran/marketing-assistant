package com.duo.marketing.input;

import com.duo.marketing.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads your own input files: pain points, brand voice, and visual-asset inventory.
 * All your data, in your own words — no scraping, no third-party terms.
 */
@Service
public class InputFiles {

    private static final Logger log = LoggerFactory.getLogger(InputFiles.class);

    private final AppProperties props;

    public InputFiles(AppProperties props) {
        this.props = props;
    }

    public String painPoints() { return read(props.painPointsFile(), "pain points"); }

    public String voice() { return read(props.voiceFile(), "brand voice"); }

    public String assets() { return read(props.assetsFile(), "asset inventory"); }

    private String read(String file, String label) {
        if (file == null || file.isBlank()) return "";
        Path path = Path.of(file);
        if (!Files.exists(path)) {
            log.warn("{} file not found at {} — continuing without it", label, path.toAbsolutePath());
            return "";
        }
        try {
            return Files.readString(path).trim();
        } catch (IOException e) {
            log.warn("Could not read {} ({}): {}", label, path, e.getMessage());
            return "";
        }
    }
}
