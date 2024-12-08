package models;

import java.io.Serializable;

// models.Account 추상 클래스
public abstract class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected String password;

    public Account(String id, String password) {
        this.id = id;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public boolean authenticate(String password) {
        return this.password.equals(password);
    }
}

