package model;

import org.hibernate.annotations.NaturalId;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Audited
public class Person extends PersistableObject {

    public enum Gender {
        MALE, FEMALE
    }

    @ManyToOne
    Company company;

    @Column(length = 200)
    @NaturalId
    String firstName;

    @Column(length = 200)
    @NaturalId
    String lastName;

    LocalDateTime bornDateTime;

    @Lob
    String description;

    @Enumerated(EnumType.STRING)
    Gender gender;

    Integer age;


    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDateTime getBornDateTime() {
        return bornDateTime;
    }

    public void setBornDateTime(LocalDateTime bornDateTime) {
        this.bornDateTime = bornDateTime;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
