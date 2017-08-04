import model.Company;
import model.Person;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

public class ModelFactory {

    static final String[] FNAMES = new String[] { "Adam", "Eva", "Stephan", "Matthias", "Sina", "Jessica", "Klaus", "Till" };
    static final Random RNDM = new Random();


    public static Person createPerson() {
        Person p = new Person();
        p.setFirstName(FNAMES[RNDM.nextInt(FNAMES.length)]);
        p.setLastName(UUID.randomUUID().toString());
        p.setAge(new Random().nextInt(100));
        p.setDescription(randomString(200));
        p.setBornDateTime(LocalDateTime.now());

        if(RNDM.nextBoolean())
            p.setGender(Person.Gender.MALE);
        else
            p.setGender(Person.Gender.FEMALE);

        return p;
    }

    public static Collection<Person> createPersonList(int length) {
        List<Person> ps = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            ps.add(createPerson());
        }
        return ps;
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
