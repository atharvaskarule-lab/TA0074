package com.example.shramikconnect;

public class User {

    public String name, phone, role, password;

    public User() {
        // Required empty constructor
    }

    public User(String name, String phone, String role, String password) {
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.password = password;
    }
}