package model;

import java.awt.peer.ListPeer;
import java.util.List;

public class Scenario {
    Town town;
    List<Company> companyList;
    List<Person> personList;

    public Scenario(Town town, List<Company> companyList, List<Person> personList) {
        this.town = town;
        this.companyList = companyList;
        this.personList = personList;
    }

    public Town getTown() {
        return town;
    }

    public List<Company> getCompanyList() {
        return companyList;
    }

    public List<Person> getPersonList() {
        return personList;
    }
}
