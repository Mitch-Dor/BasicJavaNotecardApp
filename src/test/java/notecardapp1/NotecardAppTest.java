import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

class NotecardAppTest {

    private NotecardManager manager;
    private final String DATA_FILE = "notecards.txt";
    private final String RECYCLE_FILE = "recyclingBin.txt";

    @BeforeEach
    void setUp() throws IOException {
        // Clean up any files from previous runs to ensure isolation
        Files.deleteIfExists(Paths.get(DATA_FILE));
        Files.deleteIfExists(Paths.get(RECYCLE_FILE));
        manager = new NotecardManager();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up after tests
        Files.deleteIfExists(Paths.get(DATA_FILE));
        Files.deleteIfExists(Paths.get(RECYCLE_FILE));
    }

    @Test
    @DisplayName("Test creating and saving a notecard")
    void testCreateAndSave() {
        Notecard card = new Notecard("Java", "A programming language", Arrays.asList("coding", "tech"));
        manager.addCard(card);

        // Reload a new manager instance to see if it persisted to disk
        NotecardManager newManager = new NotecardManager();
        newManager.load();
        
        List<Notecard> cards = newManager.getAllCards();
        assertEquals(1, cards.size());
        assertEquals("Java", cards.get(0).getTerm());
        assertTrue(cards.get(0).getTags().contains("coding"));
    }

    @Test
    @DisplayName("Test editing an existing notecard")
    void testEditNotecard() {
        Notecard card = new Notecard("Initial", "Definition", new ArrayList<>());
        manager.addCard(card);

        // Edit the object
        card.setTerm("Updated");
        card.setDefinition("New Def");
        card.addTag("test-tag");
        manager.saveAll();

        // Verify persistence
        NotecardManager loader = new NotecardManager();
        loader.load();
        Notecard saved = loader.getAllCards().get(0);
        
        assertEquals("Updated", saved.getTerm());
        assertEquals("New Def", saved.getDefinition());
        assertTrue(saved.getTags().contains("test-tag"));
    }

    @Test
    @DisplayName("Test deleting a card moves it to recycling bin")
    void testDeleteAndRecycle() throws IOException {
        Notecard card = new Notecard("To Delete", "Bye bye", new ArrayList<>());
        manager.addCard(card);
        
        manager.deleteCard(card);

        // 1. Should be gone from main manager
        assertTrue(manager.getAllCards().isEmpty());

        // 2. Should exist in recyclingBin.txt
        List<String> lines = Files.readAllLines(Paths.get(RECYCLE_FILE));
        assertFalse(lines.isEmpty(), "Recycle bin file should not be empty");
        assertTrue(lines.get(0).contains("To Delete"));
    }

    @Test
    @DisplayName("Test purging old recycled cards (older than 30 days)")
    void testPurgeRecyclingBin() throws IOException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        
        // 1. Create a "fake" old entry (32 days ago)
        String oldDate = LocalDateTime.now().minusDays(32).format(fmt);
        String oldEntry = oldDate + "<|>" + new Notecard("Old", "Old", new ArrayList<>()).serialize();
        
        // 2. Create a recent entry (2 days ago)
        String recentDate = LocalDateTime.now().minusDays(2).format(fmt);
        String recentEntry = recentDate + "<|>" + new Notecard("Recent", "Recent", new ArrayList<>()).serialize();

        // Manually write to the recycle file
        try (PrintWriter pw = new PrintWriter(new FileWriter(RECYCLE_FILE))) {
            pw.println(oldEntry);
            pw.println(recentEntry);
        }

        // Run the purge
        manager.purgeOldRecycledCards();

        // Verify results
        List<String> remainingLines = Files.readAllLines(Paths.get(RECYCLE_FILE));
        assertEquals(1, remainingLines.size(), "Only one card should remain");
        assertTrue(remainingLines.get(0).contains("Recent"), "The recent card should be the one remaining");
        assertFalse(remainingLines.get(0).contains("Old"), "The old card should have been purged");
    }

    @Test
    @DisplayName("Test tag filtering logic")
    void testTagFiltering() {
        manager.addCard(new Notecard("Java", "...", List.of("coding")));
        manager.addCard(new Notecard("Python", "...", List.of("coding", "scripting")));
        manager.addCard(new Notecard("London", "...", List.of("geography")));

        List<Notecard> codingCards = manager.getCardsByTag("coding");
        List<Notecard> geoCards = manager.getCardsByTag("geography");

        assertEquals(2, codingCards.size());
        assertEquals(1, geoCards.size());
    }
}