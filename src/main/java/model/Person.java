package model;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.NaturalId;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Audited
@FetchProfile(
        name = "person.complete",
        fetchOverrides = {
                @FetchProfile.FetchOverride(
                        entity = Person.class,
                        association = "companies",
                        mode = FetchMode.JOIN
                )
        }
)
public class Person extends PersistableObject {

    public enum Gender {
        MALE, FEMALE
    }

    @ManyToOne
    @NaturalId
    Town town;

    @ManyToMany
    Set<Company> companies = new HashSet<>();

    @Column
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

    public Person() { }

    public Town getTown() {
        return town;
    }

    public void setTown(Town town) {
        this.town = town;
    }

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

    public Set<Company> getCompanies() {
        return companies;
    }

    public void setCompanies(Set<Company> companies) {
        this.companies = companies;
    }
}
