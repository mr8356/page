package manager;

import java.io.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import models.*;
import record.BorrowRecord;
import record.ReturnRecord;

import java.util.ArrayList;

public class MemoryBookManager implements Serializable, BookManager {
    private static final long serialVersionUID = 1L;
    private static MemoryBookManager instance = null;
    private HashMap<Integer, Book> books;
    private static int nextId = 1000; // 도서 ID 자동 증가
    private static int nextCopyId = 1; // 도서 ID 자동 증가
    private final String FILE_PATH = "books.post";

    private int borrowPeriod = 7; // 최초 기본 7일

    private List<BorrowRecord> borrowRecords = new ArrayList<>();
    private List<ReturnRecord> returnRecords = new ArrayList<>();
    private List<BookCopy> deletedCopies = new ArrayList<>();

    private MemoryBookManager() {
        books = new HashMap<>();
    }

    private AuthorManager authorManager = new AuthorManager();

    public static MemoryBookManager getInstance() {
        if (instance == null) {
            synchronized (MemoryBookManager.class) {
                if (instance == null) {
                    instance = new MemoryBookManager();
                }
            }
        }
        return instance;
    }

    //todo 도서 추가 메서드 (동명 동저자 존재 시 번호 추가)
    public Book addBook(String title, List<String> authorNames, int quantity) {
        int num = 1;

        if (authorNames.size() > 5) {
            throw new IllegalArgumentException("저자는 최대 5명까지만 입력 가능합니다.");
        }

        // 저자 변환: String -> Author
        List<Author> authors = new ArrayList<>();
        for (String authorName : authorNames) {
            authors.add(authorManager.getOrCreateAuthor(authorName));
        }

        // 저자가 없으면 기본값 설정
        if (authors.isEmpty()) {
            authors.add(new Author("no author", -1));
        }

        // 동명 동저자 책이 존재하는지 확인
        for (Book existingBook : books.values()) {
            // 제목에서 "(숫자)" 제거
            String baseTitle = existingBook.getTitle().replaceAll("\\s*\\(\\d+\\)$", "");

            // 기존 저자의 이름 리스트 추출
            List<String> existingAuthorNames = new ArrayList<>();
            for (Author author : existingBook.getAuthors()) {
                existingAuthorNames.add(author.getName());
            }

            // 제목과 저자 이름 리스트가 동일한지 비교
            if (baseTitle.equalsIgnoreCase(title) &&
                    existingAuthorNames.equals(authorNames)) {
                num++; // 동명 동저자 책 존재 시 번호 증가
            }
        }

        // Book 생성자에서 num 값 추가
        Book book = new Book(nextId++, title, authors, quantity, num);
        books.put(book.getId(), book);
        saveData(); // 도서 추가 시 저장
        return book;
    }

    // 도서 반납기한 설정
    public void setBorrowPeriod(int borrowPeriod) {
        this.borrowPeriod = borrowPeriod;
    }

    // 도서 사본 삭제 메서드
    public void removeBookCopy(int copyId, LocalDate currentDate) {
        for (Book book : books.values()) {
            List<BookCopy> copies = book.getCopies();
            for (int i = 0; i < copies.size(); i++) {
                if (copies.get(i).getCopyId() == copyId) {
                    BookCopy deletedCopy = copies.get(i);
                    deletedCopy.setDeletedDate(currentDate);
                    deletedCopies.add(deletedCopy);
                    copies.remove(i);
                    saveData(); // 도서 사본 삭제 시 저장
                    return;
                }
            }
        }
    }

    // 도서 ID로 검색
    public Book getBookById(int id) {
        return books.get(id);
    }

    public BookCopy getBookCopyById(int bookCopyId) {
        for (Book book : books.values()) {
            for (BookCopy copy : book.getCopies()) {
                if (copy.getCopyId() == bookCopyId) {
                    return copy;
                }
            }
        }

        for (BookCopy copy : deletedCopies) {
            if (copy.getCopyId() == bookCopyId) {
                return copy;
            }
        }
        return null;
    }

    // 제목 또는 저자로 도서 검색
    public List<Book> searchBooks(String keyword) {
        List<Book> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        for (Book book : books.values()) {
            if (book.getTitle().toLowerCase().contains(lowerKeyword) ||
                    book.getAuthors().stream().anyMatch(author -> author.getName().toLowerCase().contains(lowerKeyword))) {
                results.add(book);
            }
        }
        return results;
    }

    // 모든 도서 로드
    @SuppressWarnings("unchecked")
    public void loadData() {
        nextCopyId = 1;
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return; // 파일이 없으면 초기화된 상태 유지
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
            books = (HashMap<Integer, Book>) ois.readObject();
            // 다음 ID 설정
            for (int id : books.keySet()) {
                if (id >= nextId) {
                    nextId = id + 1;
                }
            }
            // 다음 복사본 ID 설정
            for (Book book : books.values()) {
                for (BookCopy copy : book.getCopies()) {
                    if (copy.getCopyId() >= nextCopyId) {
                        nextCopyId = copy.getCopyId() + 1;
                    }
                }
            }
            for (BookCopy copy : deletedCopies) {
                if (copy.getCopyId() >= nextCopyId) {
                    nextCopyId = copy.getCopyId() + 1;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("도서 데이터를 로드하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 모든 도서 저장
    public void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(books);
        } catch (IOException e) {
            System.out.println("도서 데이터를 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }


    public int getBorrowPeriod() {
        return borrowPeriod;
    }

    public List<BorrowRecord> getBorrowRecordsByCopyId(int copyId) {
        List<BorrowRecord> result = new ArrayList<>();
        for (BorrowRecord record : borrowRecords) {
            if (record.getCopyId() == copyId) {
                result.add(record);
            }
        }
        return result;
    }

    public List<ReturnRecord> getReturnRecordsByCopyId(int copyId) {
        List<ReturnRecord> result = new ArrayList<>();
        for (ReturnRecord record : returnRecords) {
            if (record.getCopyId() == copyId) {
                result.add(record);
            }
        }
        return result;
    }

    public void addBorrowRecord(BorrowRecord record) {
        borrowRecords.add(record);
    }

    public void addReturnRecord(ReturnRecord record) {
        returnRecords.add(record);
    }

    public static int getNextCopyId() {
        return nextCopyId++;
    }
}
