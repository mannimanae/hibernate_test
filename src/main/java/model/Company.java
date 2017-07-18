package model;

import org.hibernate.annotations.*;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.Set;

@Entity
@Audited
public class Company extends PersistableObject {

    @Column(length = 50)
    @NaturalId
    private String name;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    @Cascade(CascadeType.ALL)
    Set<Person> persons;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Person> getPersons() {
        return persons;
    }

    public void setPersons(Set<Person> persons) {
        this.persons = persons;
    }
}
