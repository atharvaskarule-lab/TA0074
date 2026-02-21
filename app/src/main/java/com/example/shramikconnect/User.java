package com.example.shramikconnect;

public class User {
    public String name, phone, role, password, address, email, profileImageUrl, profession, availability;
    public double rating;
    public int earnings;

    public User() {
        // Required empty constructor for Firebase
    }

    public User(String name, String phone, String role, String password) {
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.password = password;
        this.availability = "Available";
        this.rating = 0.0;
        this.earnings = 0;
    }
}