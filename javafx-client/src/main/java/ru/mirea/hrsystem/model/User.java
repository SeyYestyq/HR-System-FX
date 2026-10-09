package ru.mirea.hrsystem.model;

public class User {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String companyName;

    public User() {}

    public User(Long id, String email, String fullName, String role, String companyName) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.companyName = companyName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    @Override
    public String toString() {
        if (companyName != null && !companyName.isBlank()) {
            return companyName + " (" + email + ")";
        }
        return fullName + " (" + email + ")";
    }
}
