import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class NotecardManager {

    private static final String DATA_FILE    = "notecards.txt";
    private static final String RECYCLE_FILE = "recyclingBin.txt";
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final List<Notecard> cards = new ArrayList<>();

    // ------------------------------------------------------------------ //
    //  Load / Save                                                          //
    // ------------------------------------------------------------------ //

    /** Called once on startup. */
    public void load() {
        cards.clear();
        Path p = Paths.get(DATA_FILE);
        if (!Files.exists(p)) return;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty()) continue;
                try {
                    cards.add(Notecard.deserialize(line));
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: skipping malformed line in " + DATA_FILE);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read " + DATA_FILE + ": " + e.getMessage());
        }
    }

    /** Rewrites the entire data file. */
    public void saveAll() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(DATA_FILE, false))) {
            for (Notecard c : cards) {
                pw.println(c.serialize());
            }
        } catch (IOException e) {
            System.err.println("Could not write " + DATA_FILE + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  Card management                                                      //
    // ------------------------------------------------------------------ //

    public void addCard(Notecard card) {
        cards.add(card);
        saveAll();
    }

    public List<Notecard> getAllCards() {
        return Collections.unmodifiableList(cards);
    }

    /** Returns every unique tag across all cards, sorted. */
    public List<String> getAllTags() {
        return cards.stream()
                .flatMap(c -> c.getTags().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /** Returns all cards that have the given tag. */
    public List<Notecard> getCardsByTag(String tag) {
        return cards.stream()
                .filter(c -> c.getTags().contains(tag))
                .collect(Collectors.toList());
    }

    /**
     * Deletes the card and writes it to recyclingBin.txt with a deletion
     * timestamp, then persists the main file.
     */
    public void deleteCard(Notecard card) {
        cards.remove(card);
        saveAll();
        writeToRecycleBin(card);
    }

    // ------------------------------------------------------------------ //
    //  Recycle bin                                                          //
    // ------------------------------------------------------------------ //

    private void writeToRecycleBin(Notecard card) {
        String deletedAt = LocalDateTime.now().format(DT_FMT);
        // Format: DELETED_AT<|>serialized_card
        String entry = deletedAt + "<|>" + card.serialize();
        try (PrintWriter pw = new PrintWriter(new FileWriter(RECYCLE_FILE, true))) {
            pw.println(entry);
        } catch (IOException e) {
            System.err.println("Could not write to " + RECYCLE_FILE + ": " + e.getMessage());
        }
    }

    /**
     * Called on program exit.
     * Reads recyclingBin.txt, drops entries older than 30 days, rewrites.
     */
    public void purgeOldRecycledCards() {
        Path p = Paths.get(RECYCLE_FILE);
        if (!Files.exists(p)) return;

        List<String> kept = new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minus(30, ChronoUnit.DAYS);

        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty()) continue;
                // First field is the timestamp, rest is the card
                int idx = line.indexOf("<|>");
                if (idx < 0) continue; // malformed, discard
                try {
                    LocalDateTime deletedAt = LocalDateTime.parse(line.substring(0, idx), DT_FMT);
                    if (deletedAt.isAfter(cutoff)) {
                        kept.add(line); // still within 30 days, keep
                    }
                    // otherwise silently drop (older than 30 days)
                } catch (Exception e) {
                    // Can't parse timestamp — keep it to be safe
                    kept.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read " + RECYCLE_FILE + ": " + e.getMessage());
            return;
        }

        // Rewrite
        try (PrintWriter pw = new PrintWriter(new FileWriter(RECYCLE_FILE, false))) {
            for (String line : kept) {
                pw.println(line);
            }
        } catch (IOException e) {
            System.err.println("Could not write " + RECYCLE_FILE + ": " + e.getMessage());
        }
    }
}
