import model.Company;
import model.Person;
import model.Scenario;
import model.Town;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

public class ModelFactory {

    static final String[] FNAMES = new String[] { "Adam", "Eva", "Stephan", "Matthias", "Sina", "Jessica", "Klaus", "Till" };
    static final String[] LNAMES = new String[] { "Schmidt", "Müller", "Reiter", "Becker", "Schmitz", "Bauer", "Metzger" };
    static final Random RNDM = new Random();


    public static Person createPerson() {
        Person p = new Person();
        p.setFirstName(FNAMES[RNDM.nextInt(FNAMES.length)]);
        final String lastName = String.format("%s (%s)", LNAMES[RNDM.nextInt(LNAMES.length)], UUID.randomUUID());
        p.setLastName(lastName);
        p.setAge(new Random().nextInt(100));
        p.setDescription(randomString(200));
        p.setBornDateTime(LocalDateTime.now());
        p.setGender(RNDM.nextBoolean() ? Person.Gender.MALE : Person.Gender.FEMALE);
        return p;
    }

    public static Company createCompany() {
        String name = UUID.randomUUID().toString();
        Company c = new Company(name);
        return c;
    }

    public static Scenario createRandomScenario(int nc, int np) {
        Town townSpringfield = new Town();
        townSpringfield.setName("Springfield");

        List<Person> persons = new ArrayList<>();
        List<Company> companies = new ArrayList<>();

        for (int i = 0; i < np; i++) {
            persons.add(ModelFactory.createPerson());
        }

        for (int i = 0; i < nc; i++) {
            companies.add(ModelFactory.createCompany());
        }

        for (Person p : persons) {
            townSpringfield.getInhabitants().add(p);
            p.setTown(townSpringfield);

            final int rndCompany = RNDM.nextInt(companies.size());
            Company c = companies.get(rndCompany);

            c.getPersons().add(p);
            p.getCompanies().add(c);
        }

        for(Company c : companies) {
            final int rndPerson = RNDM.nextInt(persons.size());
            Person p = persons.get(rndPerson);

            c.getPersons().add(p);
            p.getCompanies().add(c);
        }

        Scenario s = new Scenario(townSpringfield, companies, persons);
        return s;
    }

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    public static String randomString( int len ){
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ )
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }



}
