import manager.AccountManager;
import manager.BookManager;
import manager.MemoryAccountManager;
import manager.MemoryBookManager;
import models.*;
import record.*;

import java.util.List;
import java.util.Scanner;
import java.time.LocalDate;

public class UserInterface {
    private User user;
    private Scanner scanner;
    private BookManager bookManager = MemoryBookManager.getInstance();
    private AccountManager accountManager = MemoryAccountManager.getInstance();

    public UserInterface(User user) {
        this.user = user;
        this.scanner = new Scanner(System.in);
    }

    public void showMenu() {
        while (true) {
            System.out.println("--------------------------------------------------------------------------");
            System.out.println(" 사용자 메뉴");
            System.out.println("--------------------------------------------------------------------------");
            System.out.println("1. 도서 검색");
            System.out.println("2. 도서 대출");
            System.out.println("3. 도서 반납");
            System.out.println("4. 대출 현황 확인");
            System.out.println("5. 연혁 열람");
            System.out.println("6. 로그아웃");
            System.out.println("--------------------------------------------------------------------------");
            System.out.print("원하는 작업의 번호를 입력하세요: ");
            int choice = getUserChoice(1, 6);
            switch (choice) {
                case 1:
                    System.out.println("도서 검색 화면으로 이동합니다.");
                    handleSearchBook();
                    break;
                case 2:
                    System.out.println("도서 대출 화면으로 이동합니다.");
                    handleBorrowBook();
                    break;
                case 3:
                    System.out.println("도서 반납 화면으로 이동합니다.");
                    handleReturnBook();
                    break;
                case 4:
                    System.out.println("대출 현황 확인 화면으로 이동합니다.");
                    handleViewBorrowedBooks();
                    break;
                case 5:
                    System.out.println("연혁 열람 화면으로 이동합니다.");
                    handleBookHistoryRecord();
                    break;
                case 6:
                    System.out.println("로그아웃하고 초기화면으로 이동합니다.");
                    return;
                default:
                    break;
            }
        }
    }

    private int getUserChoice(int min, int max) {
        while (true) {
            if (scanner.hasNextInt()) {
                int choice = scanner.nextInt();
                scanner.nextLine();
                if (choice >= min && choice <= max) {
                    return choice;
                }
            } else {
                scanner.nextLine();
            }
            System.out.print("잘못된 입력입니다. " + min + "~" + max + " 사이의 번호로 다시 입력해주세요: ");
        }
    }

    private void handleSearchBook() {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" 도서 검색 화면");
        System.out.println("--------------------------------------------------------------------------");


        String keyword;
        while (true) {
            System.out.print("검색할 키워드를 입력하세요 (제목 또는 저자): ");
            keyword = scanner.nextLine().trim();

            // 영어가 아닌 문자 포함 여부 검사
            if (keyword.isEmpty()) {
                System.out.println("검색어를 입력해주세요.");
            } else if (!keyword.matches("[a-zA-Z\\s\\d]+")) {
                System.out.println("잘못된 입력입니다. (영어 형태로 입력해주세요.)");
            } else {
                break;
            }

            if (!retryPrompt()) return;
        }
        List<Book> results = bookManager.searchBooks(keyword);
        if (results.isEmpty()) {
            System.out.println("해당 키워드에 일치하는 도서가 존재하지 않습니다. 사용자 메뉴 화면으로 이동합니다.");
        } else {
            System.out.println("--------------------------------------------------------------------------");
            System.out.println("검색 결과:");
            for (Book book : results) {
                List<BookCopy> copies = book.getCopies();
                for (BookCopy copy : copies) {
                    // Author 정보를 "name #id" 형식으로 변환
                    String authors = book.getAuthors().stream()
                            .map(author -> author.getName() + " #" + author.getId())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("no author");

                    System.out.println("사본ID: " + copy.getCopyId() + " - 도서ID: " + book.getId() + " - " + book.getTitle() + " - " + authors);
                }
            }
            System.out.println("--------------------------------------------------------------------------");
        }
    }

    private void handleBorrowBook() {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" 도서 대출 화면");
        System.out.println("--------------------------------------------------------------------------");

        if (user.hasOverdueBooks()) {
            System.out.println("연체된 미반납 도서가 있어 대출할 수 없습니다. 먼저 연체된 도서를 반납해주세요.");
            return;
        }

        while (true) {
            System.out.print("대출할 도서 사본의 ID를 입력하세요: ");
            String inputId = scanner.nextLine();
            if (!isValidBookId(inputId)) {
                System.out.println("잘못된 입력입니다. (정수 형태로 입력해주세요.)");
                if (!retryPrompt()) return;
                continue;
            }

            int bookCopyId = Integer.parseInt(inputId);
            BookCopy bookCopy = bookManager.getBookCopyById(bookCopyId);
            if (bookCopy == null) {
                System.out.println("입력하신 ID에 해당하는 도서 사본이 존재하지 않습니다. 사용자 메뉴 화면으로 이동합니다.");
                return;
            }

            if (user.hasBorrowedBook(bookCopy)) {
                System.out.println("입력하신 ID에 해당하는 도서 사본을 이미 대출 했습니다. 사용자 메뉴 화면으로 이동합니다.");
                return;
            }

            // 대출 시작일 지정
            LocalDate borrowDate = LastAccessRecord.getInstance().getLastAccessDate();

            // 반납 기한 지정 (설정된 기본 대출 기간)
            int borrowPeriod = bookManager.getBorrowPeriod();
            LocalDate scheduledReturnDate = borrowDate.plusDays(borrowPeriod);
            bookCopy.borrow();
            BorrowRecord newBorrowRecord = new BorrowRecord(user.getId(), bookCopy.getBookId(), bookCopy.getCopyId(), borrowDate, scheduledReturnDate);

            user.addBorrowRecord(newBorrowRecord);
            bookManager.saveData();
            accountManager.saveData();

            // 대출 시 기록
            bookManager.addBorrowRecord(newBorrowRecord);

            // 로그 테스트용
            System.out.println("대출 시작일: " + newBorrowRecord.getBorrowDate()); // 디버깅용
            System.out.println("반납 예정일: " + newBorrowRecord.getScheduledReturnDate()); // 디버깅용

            System.out.println("도서 대출이 성공적으로 완료되었습니다. 사용자 메뉴 화면으로 이동합니다.");
            return;
        }
    }

    private void handleReturnBook() {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" 도서 반납 화면");
        System.out.println("--------------------------------------------------------------------------");
        while (true) {
            System.out.print("반납할 도서 사본의 ID를 입력하세요: ");
            String inputId = scanner.nextLine();
            if (!isValidBookId(inputId)) {
                System.out.println("잘못된 입력입니다. (정수 형태로 입력해주세요.)");
                if (!retryPrompt()) return;
                continue;
            }

            int bookCopyId = Integer.parseInt(inputId);
            BookCopy bookCopy = bookManager.getBookCopyById(bookCopyId);
            if (bookCopy == null) {
                System.out.println("입력하신 ID에 해당하는 도서 사본이 존재하지 않습니다. 사용자 메뉴 화면으로 이동합니다.");
                return;
            }

            if (!user.hasBorrowedBook(bookCopy)) {
                System.out.println("해당 도서 사본은 귀하가 대출한 도서가 아닙니다.");
                return;
            }

            bookCopy.returned();
            ReturnRecord newReturnRecord = new ReturnRecord(user.getId(), bookCopy.getBookId(), bookCopy.getCopyId(), LastAccessRecord.getInstance().getLastAccessDate());
            user.addReturnRecord(newReturnRecord);
            System.out.println("도서 반납이 성공적으로 완료되었습니다. 사용자 메뉴 화면으로 이동합니다.");
            bookManager.saveData();
            accountManager.saveData();

            // 반납 시 기록
            bookManager.addReturnRecord(newReturnRecord);
            return;
        }
    }

    private void handleViewBorrowedBooks() {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" 대출 현황 확인 화면");
        System.out.println("--------------------------------------------------------------------------");
        while (true) {
            System.out.print("대출 현황을 확인할 도서 사본의 ID를 입력하세요: ");
            String inputId = scanner.nextLine();
            if (!isValidBookId(inputId)) {
                System.out.println("잘못된 입력입니다. (정수 형태로 입력해주세요.)");
                if (!retryPrompt()) return;
                continue;
            }

            int bookCopyId = Integer.parseInt(inputId);
            BookCopy bookCopy = bookManager.getBookCopyById(bookCopyId);
            if (bookCopy == null) {
                System.out.println("입력하신 ID에 해당하는 도서 사본이 존재하지 않습니다. 사용자 메뉴 화면으로 이동합니다.");
                return;
            }

            if (!bookCopy.isBorrowed()) {
                System.out.println("검색하신 도서 사본이 대출이 가능합니다. 사용자 메뉴 화면으로 이동합니다.");
            } else {
                System.out.println("검색하신 도서 사본이 이미 대출 중입니다. 사용자 메뉴 화면으로 이동합니다.");
            }
            return;
        }
    }

    private void handleBookHistoryRecord() {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(" 연혁 열람 화면");
        System.out.println("--------------------------------------------------------------------------");
        while (true) {
            System.out.print("연혁을 열람할 도서 사본의 ID를 입력하세요: ");
            String inputId = scanner.nextLine();
            if (!isValidBookId(inputId)) {
                System.out.println("잘못된 입력입니다. (정수 형태로 입력해주세요.)");
                if (!retryPrompt()) return;
                continue;
            }

            int bookCopyId = Integer.parseInt(inputId);
            BookCopy bookCopy = bookManager.getBookCopyById(bookCopyId);
            if (bookCopy == null) {
                System.out.println("입력하신 ID에 해당하는 도서 사본이 존재하지 않습니다. 사용자 메뉴 화면으로 이동합니다.");
                return;
            }

            // 삭제일 출력
            LocalDate deletedDate = bookCopy.getDeletedDate();
            if (deletedDate != null) {
                System.out.println("입력하신 ID에 해당하는 도서 사본은 삭제되었습니다.");
            } else {           // 도서 연혁 출력
                System.out.println("--------------------------------------------------------------------------");
                Book book = bookManager.getBookById(bookCopy.getBookId());
                String authors = book.getAuthors().stream()
                        .map(author -> author.getName() + " #" + author.getId())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("no author");
                System.out.printf("도서: %s (저자: %s)%n", book.getTitle(), authors);

                // 입고일 출력
                System.out.printf("입고일: %s%n", bookCopy.getAddedDate() != null ? bookCopy.getAddedDate() : "알 수 없음");

                // 대출/반납 기록 출력
                System.out.println("대출/반납 기록:");
                List<BorrowRecord> borrowRecords = bookManager.getBorrowRecordsByCopyId(bookCopyId);
                List<ReturnRecord> returnRecords = bookManager.getReturnRecordsByCopyId(bookCopyId);

                for (BorrowRecord borrowRecord : borrowRecords) {
                    System.out.println("[대출]");
                    System.out.printf("- 대출자 ID: %s%n", borrowRecord.getUserId());
                    System.out.printf("- 대출 날짜: %s%n", borrowRecord.getBorrowDate());
                    System.out.printf("- 반납 기한: %s%n", borrowRecord.getScheduledReturnDate());

                    // 대응되는 반납 기록 확인
                    ReturnRecord correspondingReturnRecord = returnRecords.stream()
                            .filter(returnRecord -> returnRecord.getCopyId() == borrowRecord.getCopyId() &&
                                    !returnRecord.getReturnDate().isBefore(borrowRecord.getBorrowDate()))
                            .findFirst()
                            .orElse(null);

                    if (correspondingReturnRecord != null) {
                        System.out.println("[반납]");
                        System.out.printf("- 반납 날짜: %s%n", correspondingReturnRecord.getReturnDate());
                    } else {
                        System.out.println("- 현재 대출 중");
                    }
                }
            }

            System.out.println("사용자 메뉴 화면으로 이동합니다.");
            return;
        }
    }


    private boolean isValidBookId(String id) {
        return id.matches("^(0|[1-9]\\d*)$");
    }

    private boolean retryPrompt() {
        System.out.print("다시 입력하시겠습니까? (y / 다른 키를 입력하면 사용자 메뉴 화면으로 이동합니다.): ");
        String retry = scanner.nextLine();
        return "y".equals(retry);
    }
}
