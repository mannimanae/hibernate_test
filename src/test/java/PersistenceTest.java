import model.Company;
import model.Person;
import model.Town;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.query.Query;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class PersistenceTest {

    static Town town;
    static Company testCompany;
    static Person testPerson1;
    static Person testPerson2;

    @BeforeClass
    public static void setup() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();

        testCompany = new Company("Test Company");

        testPerson1 = ModelFactory.createPerson();
        testPerson1.getCompanies().add(testCompany);
        testCompany.getPersons().add(testPerson1);

        testPerson2 = ModelFactory.createPerson();
        testPerson2.getCompanies().add(testCompany);
        testCompany.getPersons().add(testPerson2);

        town = new Town();
        town.setName("Springfield");

        testPerson1.setTown(town);
        town.getInhabitants().add(testPerson1);

        testPerson2.setTown(town);
        town.getInhabitants().add(testPerson2);

        session.beginTransaction();
        session.save(town);
        session.save(testCompany);
        session.save(testPerson1);
        session.save(testPerson2);

        session.getTransaction().commit();

        session.close();
    }

    @Test(expected = PersistenceException.class)
    public void invalidInsert() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        Company invalidCompany = new Company(testCompany.getName());
        session.saveOrUpdate(invalidCompany);
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void validInsert() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        Company testCompany2 = new Company("Test Company 2");
        testCompany2.setBusinessName(testCompany.getBusinessName());
        session.save(testCompany2);
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void getPerson() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        final Person pClone = session.get(Person.class, testPerson1.getDatabaseId());
        assertEquals(testPerson1, pClone);
        session.close();
    }

    @Test
    public void getCompany() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        final Company cClone = session.get(Company.class, testCompany.getDatabaseId());
        assertEquals(testCompany, cClone);
        assertTrue(testCompany.getPersons().contains(testPerson1));
        assertTrue(testCompany.getPersons().contains(testPerson2));
        session.close();
    }

    @Test
    public void getTown() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.enableFetchProfile("town.complete");
        session.enableFetchProfile("person.complete");
        final Town t = session.get(Town.class, town.getDatabaseId());
        assertNotNull(t);
        session.close();
    }

    @Test
    public void getTownByNaturalId() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.enableFetchProfile("town.complete");
        session.enableFetchProfile("person.complete");

        final Optional<Town> townOptional = session.byNaturalId(Town.class)
                .using("name", town.getName())
                .loadOptional();
        assertTrue(townOptional.isPresent());
        assertNotNull(townOptional.get());
        session.close();
    }

    @Test
    public void getTownByQuery() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.enableFetchProfile("town.complete");
        session.enableFetchProfile("person.complete");
        final List<Town> townList = session.createQuery("from Town t", Town.class).getResultList();
        assertEquals(1, townList.size());
        session.close();
    }

    @Test
    public void getTownByCriteria() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.enableFetchProfile("town.complete");
        session.enableFetchProfile("person.complete");
        final Criteria criteria = session.createCriteria(Town.class);
        criteria.add(Restrictions.eq("name", town.getName()));
        criteria.setResultTransformer(criteria.DISTINCT_ROOT_ENTITY);
        assertEquals(1, criteria.list().size());
    }

    @Test
    public void queryByAttribute() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        final String query = "from Company c left join fetch c.persons p left join fetch p.town where c.name = :name";
        final Company cCloneJoin = session.createQuery(query, Company.class)
                .setParameter("name", testCompany.getName())
                .getSingleResult();
        assertEquals(testCompany, cCloneJoin);
    }

    @Test
    public void attributeChange() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Person p = session.get(Person.class, testPerson1.getDatabaseId());

        final Integer oldAge = p.getAge();
        p.setAge(oldAge * 2);

        session.beginTransaction();
        session.saveOrUpdate(p);
        session.getTransaction().commit();
        session.close();

        session = HibernateUtil.getSessionFactory().openSession();

        final AuditReader auditReader = AuditReaderFactory.get(session);
        assertTrue(auditReader.isEntityClassAudited(Person.class));
        final List<Number> revisions = auditReader.getRevisions(Person.class, p.getDatabaseId());
        assertEquals(2, revisions.size());

        final Person outdatedPerson = auditReader.find(Person.class, p.getDatabaseId(), revisions.get(0));
        assertEquals(oldAge, outdatedPerson.getAge());
    }

    @Test
    public void relationshipChange() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        final Company c = session.get(Company.class, testCompany.getDatabaseId());
        assertEquals(2, c.getPersons().size());

        final Optional<Person> personOpt = c.getPersons()
                .parallelStream()
                .filter(p -> p.getFirstName().equals(testPerson2.getFirstName()))
                .filter(p -> p.getLastName().equals(testPerson2.getLastName()))
                .findAny();

        assertTrue(personOpt.isPresent());

        session.beginTransaction();

        Person firedPerson = personOpt.get();
        firedPerson.getCompanies().remove(c);
        c.getPersons().remove(personOpt.get());

        assertEquals(1, c.getPersons().size());
        assertEquals(0, firedPerson.getCompanies().size());

        session.saveOrUpdate(c);
        session.getTransaction().commit();
        session.close();

        session = HibernateUtil.getSessionFactory().openSession();
        final Company c2 = session.get(Company.class, testCompany.getDatabaseId());
        assertEquals(1, c2.getPersons().size());

        final AuditReader auditReader = AuditReaderFactory.get(session);
        assertTrue(auditReader.isEntityClassAudited(Person.class));
        final List<Number> revisions = auditReader.getRevisions(Person.class, firedPerson.getDatabaseId());
        assertEquals(2, revisions.size());

        final Person historizedPerson = auditReader.find(Person.class, firedPerson.getDatabaseId(), 1);
        assertTrue(historizedPerson.getCompanies().contains(c));

        session.close();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        HibernateUtil.shutdown();
    }

}
