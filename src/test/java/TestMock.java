import model.Company;
import model.Person;
import model.Town;

public class TestMock {

    public static Town TOWN;
    public static Company COMPANY1;
    public static Person PERSON1;
    public static Person PERSON2;

    static {
        COMPANY1 = new Company("Test Company");

        PERSON1 = ModelFactory.createPerson();
        PERSON1.getCompanies().add(COMPANY1);
        COMPANY1.getPersons().add(PERSON1);

        PERSON2 = ModelFactory.createPerson();
        PERSON2.getCompanies().add(COMPANY1);
        COMPANY1.getPersons().add(PERSON2);

        TOWN = new Town();
        TOWN.setName("Test Town");

        PERSON1.setTown(TOWN);
        TOWN.getInhabitants().add(PERSON1);

        PERSON2.setTown(TOWN);
        TOWN.getInhabitants().add(PERSON2);
    }


}
