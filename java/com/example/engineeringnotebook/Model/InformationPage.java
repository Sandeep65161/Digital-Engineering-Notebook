package com.example.engineeringnotebook.Model;

public class InformationPage {
    private String notebookNo;
    private String name;
    private String signature;
    private String date;
    private String issuedBy;
    private String dateIssued;
    private String phone;
    private String email;
    private String company;
    private String department;
    private String address;
    private String city;
    private String state;
    private String zip;
    private String dateCompleted;
    private String continuedFrom;
    private String continuedTo;
    private int pagesFilled;


    public InformationPage() {}

    public InformationPage(String notebookNo, String name, String signature, String date, String issuedBy, String dateIssued, String phone, String email, String company, String department, String address, String city, String state, String zip, String dateCompleted, String pageFilled, String continuedFrom, String ContinuedTo)

    {
        this.notebookNo = notebookNo;
        this.name = name;
        this.signature = signature;
        this.date = date;
        this.issuedBy=issuedBy;
        this.dateIssued=dateIssued;
        this.phone=phone;
        this.email=email;
        this.company=company;
        this.department=department;
        this.address=address;
        this.city=city;
        this.state=state;
        this.zip=zip;
        this.dateCompleted=dateCompleted;
        this.pagesFilled=pagesFilled;
        this.continuedFrom=continuedFrom;
        this.continuedTo=continuedTo;

    }

    public String getNotebookNo() {
        return notebookNo;
    }

    public void setNotebookNo(String notebookNo) {
        this.notebookNo = notebookNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public String getDateIssued() {
        return dateIssued;
    }

    public void setDateIssued(String dateIssued) {
        this.dateIssued = dateIssued;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(String dateCompleted) {
        this.dateCompleted = dateCompleted;
    }
    public int getPagesFilled() {
        return pagesFilled;
    }

    public void setPagesFilled(int pagesFilled) {
        this.pagesFilled = pagesFilled;
    }
}
