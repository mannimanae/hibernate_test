import model.Company;
import model.Person;
import model.Scenario;
import model.Town;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hsqldb.lib.StopWatch;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

public class HibernateJPATest {

    static EntityManagerFactory EMFACT;
    static Map<String, String> PROPS = new HashMap<>();

    @BeforeClass
    public static void setUp() throws Exception {
        //PROPS.put("hibernate.hbm2ddl.auto", "validate");

        EMFACT = Persistence.createEntityManagerFactory("hibernate_test");
        final EntityManager em = EMFACT.createEntityManager();

        try {
            em.getTransaction().begin();
            em.persist(TestMock.TOWN);
            em.persist(TestMock.COMPANY1);
            em.persist(TestMock.PERSON1);
            em.persist(TestMock.PERSON2);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        EMFACT.close();
    }

    @Test(expected = PersistenceException.class)
    public void invalidInsert() throws Exception {
        final EntityManager em = EMFACT.createEntityManager(PROPS);
        em.getTransaction().begin();
        Company invalidCompany = new Company(TestMock.COMPANY1.getName());
        em.persist(invalidCompany);
        em.getTransaction().commit();
        em.close();
    }

    @Test
    public void validInsert() throws Exception {
        final EntityManager em = EMFACT.createEntityManager(PROPS);
        em.getTransaction().begin();
        Company testCompany2 = new Company("Test Company 2");
        testCompany2.setBusinessName(TestMock.COMPANY1.getBusinessName());
        em.persist(testCompany2);
        em.getTransaction().commit();
        em.close();
    }

    @Test
    public void getPerson() throws Exception {
        final EntityManager em = EMFACT.createEntityManager(PROPS);
        final Person pClone = em.find(Person.class, TestMock.PERSON1.getDatabaseId());
        assertEquals(TestMock.PERSON1, pClone);
        em.close();
    }

    @Test
    public void getCompany() throws Exception {
        final EntityManager em = EMFACT.createEntityManager(PROPS);
        final Company cClone = em.find(Company.class, TestMock.COMPANY1.getDatabaseId());
        assertEquals(TestMock.COMPANY1, cClone);
        assertTrue(TestMock.COMPANY1.getPersons().contains(TestMock.PERSON1));
        assertTrue(TestMock.COMPANY1.getPersons().contains(TestMock.PERSON2));
        em.close();
    }

    @Test
    public void getTown() throws Exception {
        final EntityManager em = EMFACT.createEntityManager(PROPS);
        final TypedQuery<Town> townQuery = em.createQuery("from Town t where t.name = :name", Town.class);
        final String hintName = "javax.persistence.fetchgraph";
        final String eg1 = "graph.town.complete";
        townQuery.setHint(hintName, em.getEntityGraph(eg1));
        final Town t = townQuery.setParameter("name", TestMock.TOWN.getName())
                .getSingleResult();
        assertNotNull(t);
        em.close();
    }

    @Test
    public void getTownCriteria() throws Exception {
        final EntityManager em = EMFACT.createEntityManager(PROPS);
        final String hintName = "javax.persistence.fetchgraph";
        final String entityGraphName = "graph.town.complete";

        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Town> cq = cb.createQuery(Town.class);
        final Root<Town> from = cq.from(Town.class);
        final ParameterExpression<String> nameParameter = cb.parameter(String.class);
        cq.select(from).where(cb.equal(from.get("name"), nameParameter));
        final TypedQuery<Town> q = em.createQuery(cq);
        q.setParameter(nameParameter, "Springfield");
        q.setHint(hintName, em.getEntityGraph(entityGraphName));
        final Town t = q.getSingleResult();
        assertNotNull(t);
    }

    @Test
    public void saveScenario() throws Exception {
        final Scenario s = ModelFactory.createRandomScenario(1000, 1000);

        EntityManager em = EMFACT.createEntityManager(PROPS);
        em.getTransaction().begin();

        em.persist(s.getTown());

        for(Person p : s.getPersonList())
            em.persist(p);

        for(Company c : s.getCompanyList())
            em.persist(c);

        em.getTransaction().commit();
        em.close();


        StopWatch w = new StopWatch();
        w.start();
        em = EMFACT.createEntityManager(PROPS);

        final String query = "select distinct c from Company c left join fetch c.persons p left join fetch p.town";
        em.createQuery(query, Company.class).getResultList();
        em.close();

        w.stop();

        System.out.println(w.elapsedTimeToMessage("Select Companies"));
    }

    @Test
    public void attributeChange() throws Exception {
        EntityManager em = EMFACT.createEntityManager(PROPS);
        Person p = em.find(Person.class, TestMock.PERSON1.getDatabaseId());

        final Integer oldAge = p.getAge();
        p.setAge(oldAge * 2);

        em.getTransaction().begin();
        em.merge(p);
        em.getTransaction().commit();
        em.close();

        em = EMFACT.createEntityManager(PROPS);

        final AuditReader auditReader = AuditReaderFactory.get(em);
        assertTrue(auditReader.isEntityClassAudited(Person.class));
        final List<Number> revisions = auditReader.getRevisions(Person.class, p.getDatabaseId());
        assertEquals(2, revisions.size());

        final Person outdatedPerson = auditReader.find(Person.class, p.getDatabaseId(), revisions.get(0));
        assertEquals(oldAge, outdatedPerson.getAge());
    }

    @Test
    public void relationshipChange() throws Exception {
        EntityManager em = EMFACT.createEntityManager(PROPS);

        final Company c = em.find(Company.class, TestMock.COMPANY1.getDatabaseId());
        assertEquals(2, c.getPersons().size());

        final Optional<Person> personOpt = c.getPersons()
                .parallelStream()
                .filter(p -> p.getFirstName().equals(TestMock.PERSON2.getFirstName()))
                .filter(p -> p.getLastName().equals(TestMock.PERSON2.getLastName()))
                .findAny();

        assertTrue(personOpt.isPresent());

        em.getTransaction().begin();

        Person firedPerson = personOpt.get();
        firedPerson.getCompanies().remove(c);
        c.getPersons().remove(personOpt.get());

        assertEquals(1, c.getPersons().size());
        assertEquals(0, firedPerson.getCompanies().size());

        em.merge(c);
        em.getTransaction().commit();
        em.close();

        em = EMFACT.createEntityManager(PROPS);
        final Company c2 = em.find(Company.class, TestMock.COMPANY1.getDatabaseId());
        assertEquals(1, c2.getPersons().size());

        final AuditReader auditReader = AuditReaderFactory.get(em);
        assertTrue(auditReader.isEntityClassAudited(Person.class));
        final List<Number> revisions = auditReader.getRevisions(Person.class, firedPerson.getDatabaseId());
        assertEquals(2, revisions.size());

        final Person historizedPerson = auditReader.find(Person.class, firedPerson.getDatabaseId(), 1);
        assertTrue(historizedPerson.getCompanies().contains(c));

        em.close();
    }
}
