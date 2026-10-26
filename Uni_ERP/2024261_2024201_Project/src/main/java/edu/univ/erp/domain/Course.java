package edu.univ.erp.domain;

public record Course(int id, String code, String title, int credits, String description, String department) {

    @Override
    public String toString() {
        return code + " · " + title;
    }
}
