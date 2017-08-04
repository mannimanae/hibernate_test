package model;

import org.hibernate.annotations.NaturalId;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
@Audited
public class Company extends PersistableObject {

    @Column
    @NaturalId
    private String name;

    private String businessName;

    @ManyToMany(mappedBy = "companies")
    Set<Person> persons = new HashSet<>();

    Company() {}

    public Company(String name) {
        this.name = name;
        this.businessName = name;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getName() {
        return name;
    }

    public Set<Person> getPersons() {
        return persons;
    }
}
