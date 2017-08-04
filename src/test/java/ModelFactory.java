import model.Company;
import model.Person;

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

    public static Collection<Company> createRandomCompanies(int n, int np) {
        List<Company> cList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Company c = createCompany();
            for (int j = 0; j < RNDM.nextInt(np) + 1; j++) {
                Person p = createPerson();
                p.getCompanies().add(c);
                c.getPersons().add(p);
            }
            cList.add(c);
        }
        return cList;
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
