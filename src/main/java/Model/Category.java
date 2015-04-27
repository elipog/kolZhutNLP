package Model;

/**
 * Created by epogrebezky on 12/30/14.
 */
public class Category  {

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public int getRelevance() {
        return relevance;
    }

    public void setRelevance(int relevance) {
        this.relevance = relevance;
    }

    private int relevance;
    private String name;
}
