package manager;

import models.Book;
import models.BookCopy;
import record.BorrowRecord;
import record.ReturnRecord;

import java.time.LocalDate;
import java.util.List;

public interface BookManager {
    // 도서 추가 메서드
    public Book addBook(String title, List<String> authors, int quantity);

    // 도서 삭제 메서드
    public void removeBookCopy(int copyId, LocalDate removeDate);

    // 도서 ID로 검색
    public Book getBookById(int id);

    // 제목 또는 저자로 도서 검색
    public List<Book> searchBooks(String keyword);
    // 모든 도서 로드

    public BookCopy getBookCopyById(int bookCopyId);

    public void loadData();

    // 모든 도서 저장
    public void saveData();

    // 반납 기한 설정
    void setBorrowPeriod(int borrowPeriod);

    int getBorrowPeriod();

    // 대출 기록 조회
    List<BorrowRecord> getBorrowRecordsByCopyId(int copyId);

    // 반납 기록 조회
    List<ReturnRecord> getReturnRecordsByCopyId(int copyId);

    // 대출 기록 추가
    void addBorrowRecord(BorrowRecord record);

    // 반납 기록 추가
    void addReturnRecord(ReturnRecord record);

}
