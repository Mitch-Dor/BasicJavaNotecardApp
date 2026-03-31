import java.util.*;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final NotecardManager manager = new NotecardManager();

    public static void main(String[] args) {
        manager.load();
        System.out.println("=== Notecard App ===");

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt();
            switch (choice) {
                case 1 -> System.out.println("(Review notecard set — not yet implemented)");
                case 2 -> System.out.println("(Learn — not yet implemented)");
                case 3 -> addNotecardFlow();
                case 4 -> editNotecardFlow();
                case 0 -> running = false;
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }

        manager.purgeOldRecycledCards();
        System.out.println("Goodbye!");
    }

    // ------------------------------------------------------------------ //
    //  Menus                                                                //
    // ------------------------------------------------------------------ //

    private static void printMainMenu() {
        System.out.println();
        System.out.println("--- Main Menu ---");
        System.out.println("1: Review notecard set");
        System.out.println("2: Learn");
        System.out.println("3: Add notecards");
        System.out.println("4: Edit notecards");
        System.out.println("0: Exit");
        System.out.print("Choice: ");
    }

    // ------------------------------------------------------------------ //
    //  Add Notecard Flow                                                    //
    // ------------------------------------------------------------------ //

    private static void addNotecardFlow() {
        System.out.println("\n--- Add a New Notecard ---");

        System.out.print("Enter the term: ");
        String term = readLine();
        if (term.isBlank()) {
            System.out.println("Term cannot be blank. Returning to main menu.");
            return;
        }

        System.out.print("Enter the definition: ");
        String definition = readLine();

        // Tag assignment loop
        List<String> chosenTags = assignTagsInteractively(new ArrayList<>());

        Notecard card = new Notecard(term, definition, chosenTags);
        manager.addCard(card);

        System.out.println("\nNotecard saved!");
        card.printCard();
    }

    /**
     * Interactive tag picker / creator.
     * @param currentTags  tags the card already has (used when editing)
     * @return the final tag list
     */
    private static List<String> assignTagsInteractively(List<String> currentTags) {
        List<String> tags = new ArrayList<>(currentTags);

        while (true) {
            List<String> allTags = manager.getAllTags();
            System.out.println("\n--- Tag Assignment ---");
            System.out.println("Current tags on this card: " +
                    (tags.isEmpty() ? "(none)" : String.join(", ", tags)));

            if (!allTags.isEmpty()) {
                System.out.println("Existing tags:");
                for (int i = 0; i < allTags.size(); i++) {
                    String marker = tags.contains(allTags.get(i)) ? " [assigned]" : "";
                    System.out.printf("  %d: %s%s%n", i + 1, allTags.get(i), marker);
                }
            } else {
                System.out.println("(No existing tags yet)");
            }

            System.out.println();
            System.out.println("Options:");
            System.out.println("  Type a number to toggle an existing tag");
            System.out.println("  Type a new tag name to create & assign it");
            System.out.println("  Press Enter with no input to finish");
            System.out.print("Tag choice: ");
            String input = readLine();

            if (input.isBlank()) {
                break;
            }

            // Try to parse as number → toggle existing tag
            try {
                int idx = Integer.parseInt(input.trim()) - 1;
                if (idx >= 0 && idx < allTags.size()) {
                    String tag = allTags.get(idx);
                    if (tags.contains(tag)) {
                        tags.remove(tag);
                        System.out.println("Removed tag: " + tag);
                    } else {
                        tags.add(tag);
                        System.out.println("Added tag: " + tag);
                    }
                } else {
                    System.out.println("Number out of range.");
                }
            } catch (NumberFormatException e) {
                // Treat as new tag name
                String newTag = input.trim();
                if (newTag.contains(",") || newTag.contains("<|>")) {
                    System.out.println("Tag names may not contain ',' or '<|>'. Please choose another name.");
                } else if (tags.contains(newTag)) {
                    System.out.println("Tag '" + newTag + "' is already assigned to this card.");
                } else {
                    tags.add(newTag);
                    System.out.println("Created and assigned new tag: " + newTag);
                }
            }
        }

        return tags;
    }

    // ------------------------------------------------------------------ //
    //  Edit Notecard Flow                                                   //
    // ------------------------------------------------------------------ //

    private static void editNotecardFlow() {
        List<Notecard> all = new ArrayList<>(manager.getAllCards());
        if (all.isEmpty()) {
            System.out.println("No notecards to edit.");
            return;
        }

        System.out.println("\n--- Edit / Delete a Notecard ---");
        System.out.println("Search by:");
        System.out.println("  1: Browse all cards");
        System.out.println("  2: Filter by tag");
        System.out.print("Choice: ");
        int browse = readInt();

        List<Notecard> pool;
        if (browse == 2) {
            pool = filterByTag();
            if (pool == null) return; // user aborted
        } else {
            pool = all;
        }

        if (pool.isEmpty()) {
            System.out.println("No cards found.");
            return;
        }

        // List cards for selection
        System.out.println();
        for (int i = 0; i < pool.size(); i++) {
            System.out.printf("  %d: %s%n", i + 1, pool.get(i).getTerm());
        }
        System.out.print("Select a card by number (0 to cancel): ");
        int sel = readInt();
        if (sel < 1 || sel > pool.size()) {
            System.out.println("Cancelled.");
            return;
        }

        Notecard card = pool.get(sel - 1);
        editSingleCard(card);
    }

    private static List<Notecard> filterByTag() {
        List<String> allTags = manager.getAllTags();
        if (allTags.isEmpty()) {
            System.out.println("No tags exist yet.");
            return null;
        }
        System.out.println("Tags:");
        for (int i = 0; i < allTags.size(); i++) {
            System.out.printf("  %d: %s%n", i + 1, allTags.get(i));
        }
        System.out.print("Select tag number (0 to cancel): ");
        int t = readInt();
        if (t < 1 || t > allTags.size()) {
            System.out.println("Cancelled.");
            return null;
        }
        return manager.getCardsByTag(allTags.get(t - 1));
    }

    private static void editSingleCard(Notecard card) {
        boolean editing = true;
        while (editing) {
            System.out.println();
            card.printCard();
            System.out.println();
            System.out.println("What would you like to edit?");
            System.out.println("  1: Term");
            System.out.println("  2: Definition");
            System.out.println("  3: Tags");
            System.out.println("  4: Delete this notecard");
            System.out.println("  0: Done editing");
            System.out.print("Choice: ");
            int choice = readInt();

            switch (choice) {
                case 1 -> {
                    System.out.print("New term (leave blank to keep \"" + card.getTerm() + "\"): ");
                    String newTerm = readLine();
                    if (!newTerm.isBlank()) {
                        card.setTerm(newTerm);
                        manager.saveAll();
                        System.out.println("Term updated.");
                    }
                }
                case 2 -> {
                    System.out.print("New definition (leave blank to keep current): ");
                    String newDef = readLine();
                    if (!newDef.isBlank()) {
                        card.setDefinition(newDef);
                        manager.saveAll();
                        System.out.println("Definition updated.");
                    }
                }
                case 3 -> {
                    List<String> updatedTags = assignTagsInteractively(card.getTags());
                    card.setTags(updatedTags);
                    manager.saveAll();
                    System.out.println("Tags updated.");
                }
                case 4 -> {
                    if (confirmDeletion(card)) {
                        editing = false;
                    }
                }
                case 0 -> editing = false;
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static boolean confirmDeletion(Notecard card) {
        System.out.println();
        System.out.println("Are you sure you want to delete \"" + card.getTerm() + "\"?");
        System.out.println("  1: Yes, delete it");
        System.out.println("  2: No, keep it");
        System.out.print("Choice: ");
        int confirm = readInt();
        if (confirm == 1) {
            manager.deleteCard(card);
            System.out.println("Notecard deleted. (Saved to recycling bin for 30 days.)");
            return true;
        } else {
            System.out.println("Deletion cancelled.");
            return false;
        }
    }

    // ------------------------------------------------------------------ //
    //  Input helpers                                                        //
    // ------------------------------------------------------------------ //

    private static String readLine() {
        return scanner.hasNextLine() ? scanner.nextLine() : "";
    }

    private static int readInt() {
        String line = readLine().trim();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
