package manager;

import java.io.*;
import java.util.HashMap;

import models.*;

public class MemoryAccountManager implements Serializable, AccountManager {
    private static final long serialVersionUID = 1L;
    private static MemoryAccountManager instance = null;
    private HashMap<String, Account> accounts;
    private final String FILE_PATH = "accounts.post";

    private MemoryAccountManager() {
        accounts = new HashMap<>();
    }

    public static MemoryAccountManager getInstance() {
        if (instance == null) {
            synchronized (MemoryAccountManager.class) {
                if (instance == null) {
                    instance = new MemoryAccountManager();
                }
            }
        }
        return instance;
    }

    public void addAccount(Account account) {
        accounts.put(account.getId(), account);
        saveData();
    }

    public Account getAccountById(String id) {
        return accounts.get(id);
    }


    public void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(accounts);
        } catch (IOException e) {
            System.out.println("계정 데이터를 저장하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void loadData() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return; // 파일이 없으면 초기화된 상태 유지
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
            accounts = (HashMap<String, Account>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("계정 데이터를 로드하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
