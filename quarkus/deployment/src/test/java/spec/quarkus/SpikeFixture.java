package spec.quarkus;

import java.util.ArrayList;
import java.util.Collection;

import io.granito.concordion.quarkus.ConcordionFixture;

@ConcordionFixture
public class SpikeFixture {
    public String getGreetingFor(String name)
    {
        return "Hello " + name + "!";
    }

    public void doSomething()
    {
    }

    public Collection<Person> getPeople()
    {
        return new ArrayList<>() {{
            add(new Person("John", "Travolta"));
        }};
    }

    public static class Person {
        public Person(String firstName, String lastName)
        {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String firstName;

        public String lastName;
    }
}
