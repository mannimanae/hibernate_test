import model.Company;
import model.Person;
import model.Scenario;
import model.Town;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.query.Query;
import org.hsqldb.lib.StopWatch;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class HibernateNativeTest {

    @BeforeClass
    public static void setup() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();

        session.beginTransaction();
        session.save(TestMock.TOWN);
        session.save(TestMock.COMPANY1);
        session.save(TestMock.PERSON1);
        session.save(TestMock.PERSON2);

        session.getTransaction().commit();

        session.close();
    }

    @Test(expected = PersistenceException.class)
    public void invalidInsert() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        Company invalidCompany = new Company(TestMock.COMPANY1.getName());
        session.saveOrUpdate(invalidCompany);
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void validInsert() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        Company testCompany2 = new Company("Test Company 2");
        testCompany2.setBusinessName(TestMock.COMPANY1.getBusinessName());
        session.save(testCompany2);
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void getPerson() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        final Person pClone = session.get(Person.class, TestMock.PERSON1.getDatabaseId());
        assertEquals(TestMock.PERSON1, pClone);
        session.close();
    }

    @Test
    public void getCompany() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        final Company cClone = session.get(Company.class, TestMock.COMPANY1.getDatabaseId());
        assertEquals(TestMock.COMPANY1, cClone);
        assertTrue(TestMock.COMPANY1.getPersons().contains(TestMock.PERSON1));
        assertTrue(TestMock.COMPANY1.getPersons().contains(TestMock.PERSON2));
        session.close();
    }

    @Test
    public void getTown() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.enableFetchProfile("town.complete");
        session.enableFetchProfile("person.complete");
        final Town t = session.get(Town.class, TestMock.TOWN.getDatabaseId());
        assertNotNull(t);
        session.close();
    }

    @Test
    public void getTownByNaturalId() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.enableFetchProfile("town.complete");
        session.enableFetchProfile("person.complete");

        final Optional<Town> townOptional = session.byNaturalId(Town.class)
                .using("name", TestMock.TOWN.getName())
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
        final List<Town> townList = session.createQuery("from Town t where t.name = :name", Town.class)
                .setParameter("name", TestMock.TOWN.getName())
                .getResultList();
        assertEquals(1, townList.size());
        session.close();
    }

    @Test
    public void getTownByCriteria() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.enableFetchProfile("town.complete");
        session.enableFetchProfile("person.complete");
        final Criteria criteria = session.createCriteria(Town.class);
        criteria.add(Restrictions.eq("name", TestMock.TOWN.getName()));
        criteria.setResultTransformer(criteria.DISTINCT_ROOT_ENTITY);
        assertEquals(1, criteria.list().size());
    }

    @Test
    public void getTownByJPACriteria() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        final CriteriaBuilder cb = session.getCriteriaBuilder();
        final CriteriaQuery<Town> cq = cb.createQuery(Town.class);
        final Root<Town> from = cq.from(Town.class);
        final ParameterExpression<String> nameParameter = cb.parameter(String.class);
        cq.select(from).where(cb.equal(from.get("name"), nameParameter));

        final String hintName = "javax.persistence.fetchgraph";
        final String entityGraphName = "graph.town.complete";

        // EG kann man per Hand bauen

        final Query<Town> q = session.createQuery(cq);
        q.setParameter(nameParameter, "Springfield");
        q.setHint(hintName, session.getEntityGraph(entityGraphName));

        final Town town = q.getSingleResult();
        assertNotNull(town);
    }

    @Test
    public void queryByAttribute() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        final String query = "from Company c left join fetch c.persons p left join fetch p.town where c.name = :name";
        final Company cCloneJoin = session.createQuery(query, Company.class)
                .setParameter("name", TestMock.COMPANY1.getName())
                .getSingleResult();
        assertEquals(TestMock.COMPANY1, cCloneJoin);
    }

    @Test
    public void attributeChange() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Person p = session.get(Person.class, TestMock.PERSON1.getDatabaseId());

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
        final Company c = session.get(Company.class, TestMock.COMPANY1.getDatabaseId());
        assertEquals(2, c.getPersons().size());

        final Optional<Person> personOpt = c.getPersons()
                .parallelStream()
                .filter(p -> p.getFirstName().equals(TestMock.PERSON2.getFirstName()))
                .filter(p -> p.getLastName().equals(TestMock.PERSON2.getLastName()))
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
        final Company c2 = session.get(Company.class, TestMock.COMPANY1.getDatabaseId());
        assertEquals(1, c2.getPersons().size());

        final AuditReader auditReader = AuditReaderFactory.get(session);
        assertTrue(auditReader.isEntityClassAudited(Person.class));
        final List<Number> revisions = auditReader.getRevisions(Person.class, firedPerson.getDatabaseId());
        assertEquals(2, revisions.size());

        final Person historizedPerson = auditReader.find(Person.class, firedPerson.getDatabaseId(), 1);
        assertTrue(historizedPerson.getCompanies().contains(c));

        session.close();
    }

    @Test
    public void saveScenario() throws Exception {
        final Scenario s = ModelFactory.createRandomScenario(1000, 1000);
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();

        session.saveOrUpdate(s.getTown());

        for(Person p : s.getPersonList())
            session.saveOrUpdate(p);

        for(Company c : s.getCompanyList())
            session.saveOrUpdate(c);

        session.getTransaction().commit();

        StopWatch w = new StopWatch();
        w.start();

        session = HibernateUtil.getSessionFactory().openSession();
        final String query = "select distinct c from Company c left join fetch c.persons p left join fetch p.town";
        session.createQuery(query, Company.class).getResultList();
        session.close();

        w.stop();

        System.out.println(w.elapsedTimeToMessage("Select Companies"));
    }

    @AfterClass
    public static void shutdown() throws Exception {
        HibernateUtil.shutdown();
    }

}
