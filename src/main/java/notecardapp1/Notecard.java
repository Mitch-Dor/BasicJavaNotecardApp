import java.util.ArrayList;
import java.util.List;

public class Notecard {
    private String term;
    private String definition;
    private List<String> tags;

    public Notecard(String term, String definition, List<String> tags) {
        this.term = term;
        this.definition = definition;
        this.tags = new ArrayList<>(tags);
    }

    // --- Getters ---

    public String getTerm() {
        return term;
    }

    public String getDefinition() {
        return definition;
    }

    public List<String> getTags() {
        return new ArrayList<>(tags);
    }

    // --- Updaters ---

    public void setTerm(String term) {
        this.term = term;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public void setTags(List<String> tags) {
        this.tags = new ArrayList<>(tags);
    }

    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    // --- Print helpers ---

    public void printTerm() {
        System.out.println("Term: " + term);
    }

    public void printDefinition() {
        System.out.println("Definition: " + definition);
    }

    public void printTags() {
        System.out.println("Tags: " + (tags.isEmpty() ? "(none)" : String.join(", ", tags)));
    }

    public void printCard() {
        printTerm();
        printDefinition();
        printTags();
    }

    // --- Serialization ---

    /**
     * Serialize to a single line:
     *   TERM<|>DEFINITION<|>tag1,tag2,tag3
     * Pipe-bracketed delimiter is unlikely to appear in normal text.
     */
    public String serialize() {
        String tagStr = String.join(",", tags);
        return escapeField(term) + "<|>" + escapeField(definition) + "<|>" + escapeField(tagStr);
    }

    public static Notecard deserialize(String line) {
        String[] parts = line.split("\\Q<|>\\E", -1);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Malformed notecard line: " + line);
        }
        String term = unescapeField(parts[0]);
        String definition = unescapeField(parts[1]);
        List<String> tags = new ArrayList<>();
        String tagStr = unescapeField(parts[2]);
        if (!tagStr.isBlank()) {
            for (String t : tagStr.split(",", -1)) {
                if (!t.isBlank()) tags.add(t);
            }
        }
        return new Notecard(term, definition, tags);
    }

    // Simple newline escaping so each card stays on one line
    private static String escapeField(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String unescapeField(String s) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (char c : s.toCharArray()) {
            if (escaped) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    default  -> sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
