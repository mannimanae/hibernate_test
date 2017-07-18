import model.Company;
import model.Person;
import org.hibernate.Session;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PersistenceTest {

    @BeforeClass
    public static void setup() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        session.createQuery("delete from Person").executeUpdate();
        session.createQuery("delete from Company ").executeUpdate();
        session.close();
    }

    @Test
    public void insertAndCascading() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        final Company c1 = ModelFactory.createRandomCompany();
        session.save(c1);
        session.getTransaction().commit();
        session.close();

        session = HibernateUtil.getSessionFactory().openSession();

        String q = "from Company c join fetch c.persons pers where c.name = :name";
        Company c1Clone = (Company) session.createQuery(q)
                .setParameter("name", c1.getName())
                .getSingleResult();

        assertEquals(c1, c1Clone);
        assertEquals(c1.getPersons().size(), c1Clone.getPersons().size());
    }

    @Test
    public void testAlteringAndAuditing() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        Person p = ModelFactory.createPerson();
        session.save(p);
        session.getTransaction().commit();
        session.close();

        session = HibernateUtil.getSessionFactory().openSession();

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

    @AfterClass
    public static void shutdown() throws Exception {
        HibernateUtil.shutdown();
    }

}
