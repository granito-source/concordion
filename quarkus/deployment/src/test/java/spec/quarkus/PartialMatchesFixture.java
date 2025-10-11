package spec.quarkus;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import io.granito.concordion.quarkus.QuarkusConcordionFixture;

@QuarkusConcordionFixture
public class PartialMatchesFixture {
    private final Set<String> usernamesInSystem = new HashSet<>();

    public void setUpUser(String username)
    {
        usernamesInSystem.add(username);
    }

    public SortedSet<String> getSearchResultsFor(String searchString)
    {
        var matches = new TreeSet<String>();

        for (var username: usernamesInSystem)
            if (username.contains(searchString))
                matches.add(username);

        return matches;
    }
}
