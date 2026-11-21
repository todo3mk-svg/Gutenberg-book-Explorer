package com.student;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class App {
    private static final int READING_LIST_SIZE = 20;
    private static final String GUTENBERG_URL = "https://www.gutenberg.org";

    private static final Book[] readingList = new Book[READING_LIST_SIZE];
    private static int readingListCount = 0;
    private static Book[] currentResults = new Book[0];

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            int choice = readChoice(scanner);

            if (choice == 1) {
                exploreBooks();
            } else if (choice == 2) {
                System.out.print("Enter a title: ");
                String title = scanner.nextLine().trim();
                searchBook(title);
            } else if (choice == 3) {
                System.out.print("Enter an author: ");
                String author = scanner.nextLine().trim();
                searchBook(author, true);
            } else if (choice == 4) {
                addBook(scanner);
            } else if (choice == 5) {
                viewReadingList();
            } else if (choice == 6) {
                removeBook(scanner);
            } else if (choice == 7) {
                displayBookDetails(scanner);
            } else if (choice == 0) {
                running = false;
                System.out.println("Goodbye.");
            } else {
                System.out.println("Invalid choice. Enter a number from the menu.");
            }
        }

        scanner.close();
    }

    static void printMenu() {
        System.out.println();
        System.out.println("===== GUTENBERG BOOK EXPLORER =====");
        System.out.println("1. Explore Books");
        System.out.println("2. Search by Title");
        System.out.println("3. Search by Author");
        System.out.println("4. Add Book to Reading List");
        System.out.println("5. View Reading List");
        System.out.println("6. Remove Book");
        System.out.println("7. Display Book Details");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }

    static int readChoice(Scanner scanner) {
        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    static void exploreBooks() {
        try {
            Document document = connect(GUTENBERG_URL + "/browse/scores/top");
            Elements links = document.select("a[href^=/ebooks/], a[href*=ebooks]");

            Set<String> urls = new LinkedHashSet<>();
            for (Element link : links) {
                String url = link.absUrl("href");
                if (isBookUrl(url)) {
                    urls.add(url);
                }
                if (urls.size() == 20) {
                    break;
                }
            }

            if (urls.isEmpty()) {
                System.out.println("No books were found on the Gutenberg catalog.");
                currentResults = new Book[0];
                return;
            }

            currentResults = loadBooks(urls, null, false);
            printSearchResults(currentResults);
        } catch (IOException exception) {
            System.out.println("Connection error: " + exception.getMessage());
        }
    }

    static void searchBook(String title) {
        searchBook(title, false);
    }

    static void searchBook(String keyword, boolean searchByAuthor) {
        if (keyword == null || keyword.trim().isEmpty()) {
            System.out.println("Search text cannot be empty.");
            return;
        }

        try {
            String encoded = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
            Document document = connect(GUTENBERG_URL + "/ebooks/search/?query=" + encoded);
            Elements links = document.select("li.booklink a.link[href*=ebooks], a.link[href*=ebooks]");

            Set<String> urls = new LinkedHashSet<>();
            for (Element link : links) {
                String url = link.absUrl("href");
                if (isBookUrl(url)) {
                    urls.add(url);
                }
                if (urls.size() == 30) {
                    break;
                }
            }

            if (urls.isEmpty()) {
                System.out.println("No books matched your search.");
                currentResults = new Book[0];
                return;
            }

            currentResults = loadBooks(urls, keyword.trim(), searchByAuthor);
            if (currentResults.length == 0) {
                System.out.println("No books matched your search.");
            } else {
                printSearchResults(currentResults);
            }
        } catch (IOException exception) {
            System.out.println("Connection error: " + exception.getMessage());
        }
    }

    static void addBook(Scanner scanner) {
        if (readingListCount >= READING_LIST_SIZE) {
            System.out.println("The reading list is full. Remove a book before adding another.");
            return;
        }
        if (currentResults.length == 0) {
            System.out.println("Search or explore for books first.");
            return;
        }

        System.out.print("Enter the result number to add: ");
        int number = readChoice(scanner);
        if (number < 1 || number > currentResults.length) {
            System.out.println("Invalid book number.");
            return;
        }

        for (int i = 0; i < readingListCount; i++) {
            if (readingList[i].equals(currentResults[number - 1])) {
                System.out.println("That book is already in the reading list.");
                return;
            }
        }

        readingList[readingListCount] = currentResults[number - 1];
        readingListCount++;
        System.out.println("Added: " + currentResults[number - 1].title);
    }

    static void viewReadingList() {
        if (readingListCount == 0) {
            System.out.println("Your reading list is empty.");
            return;
        }

        System.out.println("Your reading list:");
        for (int i = 0; i < readingListCount; i++) {
            System.out.println((i + 1) + ". " + readingList[i].title
                    + " - " + readingList[i].author);
        }
    }

    static void removeBook(Scanner scanner) {
        if (readingListCount == 0) {
            System.out.println("Your reading list is empty.");
            return;
        }

        viewReadingList();
        System.out.print("Enter the reading-list number to remove: ");
        int number = readChoice(scanner);
        if (number < 1 || number > readingListCount) {
            System.out.println("Invalid book number.");
            return;
        }

        int index = number - 1;
        String removedTitle = readingList[index].title;
        for (int i = index; i < readingListCount - 1; i++) {
            readingList[i] = readingList[i + 1];
        }
        readingList[readingListCount - 1] = null;
        readingListCount--;
        System.out.println("Removed: " + removedTitle);
    }

    static void displayBookDetails(Scanner scanner) {
        if (readingListCount == 0) {
            System.out.println("Your reading list is empty.");
            return;
        }

        viewReadingList();
        System.out.print("Enter the reading-list number: ");
        int number = readChoice(scanner);
        if (number < 1 || number > readingListCount) {
            System.out.println("Invalid book number.");
            return;
        }

        Book book = readingList[number - 1];
        System.out.println("Title: " + book.title);
        System.out.println("Author: " + book.author);
        System.out.println("Gutenberg URL: " + book.url);
    }

    private static Document connect(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("GutenbergBookExplorer/1.0")
                .timeout(15000)
                .get();
    }

    private static boolean isBookUrl(String url) {
        if (!url.startsWith(GUTENBERG_URL + "/ebooks/")) {
            return false;
        }
        String suffix = url.substring((GUTENBERG_URL + "/ebooks/").length());
        return suffix.matches("\\d+/?");
    }

    private static Book[] loadBooks(Set<String> urls, String keyword, boolean searchByAuthor) {
        java.util.ArrayList<Book> books = new java.util.ArrayList<>();
        for (String url : urls) {
            try {
                Document document = connect(url);
                String title = firstMetaContent(document,
                        "meta[name=citation_title]", "meta[property=og:title]");
                if (title.isEmpty()) {
                    title = document.title();
                }
                title = cleanTitle(title);

                Element authorElement = document.selectFirst("meta[name=citation_author]");
                String author = "Unknown";
                if (authorElement != null) {
                    author = authorElement.attr("content").trim();
                }
                if (author.isEmpty()) {
                    author = "Unknown";
                }

                if (keyword == null
                        || (searchByAuthor
                                ? author.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))
                                : title.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)))) {
                    books.add(new Book(title, author, url));
                }
                if (books.size() == 20) {
                    break;
                }
            } catch (IOException exception) {
                // Skip an individual unavailable catalog entry rather than failing the whole search.
            }
        }
        return books.toArray(new Book[0]);
    }

    private static String firstMetaContent(Document document, String... selectors) {
        for (String selector : selectors) {
            Element element = document.selectFirst(selector);
            if (element != null) {
                String content = element.attr("content").trim();
                if (!content.isEmpty()) {
                    return content;
                }
            }
        }
        return "";
    }

    private static String cleanTitle(String title) {
        title = title.replaceAll("\\s*\\|\\s*Project Gutenberg.*$", "").trim();
        return title.isEmpty() ? "Untitled" : title;
    }

    private static void printSearchResults(Book[] books) {
        System.out.println("Found " + books.length + " book(s):");
        for (int i = 0; i < books.length; i++) {
            System.out.println((i + 1) + ". " + books[i].title + " - " + books[i].author);
        }
    }

    static class Book {
        final String title;
        final String author;
        final String url;

        Book(String title, String author, String url) {
            this.title = title;
            this.author = author;
            this.url = url;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Book)) {
                return false;
            }
            Book other = (Book) object;
            return url.equals(other.url);
        }

        @Override
        public int hashCode() {
            return url.hashCode();
        }
    }
}
