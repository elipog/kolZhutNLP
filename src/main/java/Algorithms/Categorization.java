package Algorithms;

import Model.Category;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by epogrebezky on 12/30/14.
 */
public class Categorization {

    HashMap<Integer,String> idx2Category;
    HashMap<String,HashSet<Integer>> words2idx;

    private static Categorization instance;

    public static Categorization getInstance(){
        if(instance == null){
            instance = new Categorization();
        }
        return instance;
    }

    private Categorization(){
        initIdx2Category();
        initWords2idx();
    }

    private void initWords2idx() {
        try {
            this.words2idx = new HashMap<String, HashSet<Integer>>();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse("src/main/resources/categories_output.xml");
            NodeList categories = doc.getElementsByTagName("sentence");
            // go over all categories
            for (int i = 0; i < categories.getLength(); i++) {
                NodeList tokens = categories.item(i).getChildNodes();
                //iterate tokens...
                for (int j = 0; j < tokens.getLength() - 1; j++) {
                    NodeList analysis = tokens.item(j).getChildNodes();
                    for (int k = 0; k < analysis.getLength(); k++) {
                        NodeList analysis_kids = analysis.item(k).getChildNodes();
                        for (int l = 0; l < analysis_kids.getLength(); l++) {
                            if (analysis_kids.item(l).getNodeName().equals("base")) {
                                if (analysis_kids.item(l).getAttributes().getNamedItem("transliteratedLexiconItem") != null) {
                                    String key = analysis_kids.item(l).getAttributes().getNamedItem("transliteratedLexiconItem").getNodeValue();
                                    if (!words2idx.containsKey(key)) {
                                        words2idx.put(key, new HashSet<Integer>());
                                    }
                                    words2idx.get(key).add(i);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
        }

    }

    private void initIdx2Category() {
        try {
            this.idx2Category = new HashMap<Integer, String>();
            BufferedReader br = new BufferedReader(new FileReader("src/main/resources/categories.csv"));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                int i = 0;
                while (line != null) {
                    idx2Category.put(i, line);
                    i++;
                    line = br.readLine();
                }
                String everything = sb.toString();
            } finally {
                br.close();
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
        };
    }

    public List<Category> getCategories(String request){

        try {
            List<Category> res = new ArrayList<Category>();
            PrintWriter writer = new PrintWriter("temp/data.input");
            writer.println(request);
            writer.close();

            Process proc = Runtime.getRuntime().exec("java -Xmx1024m -jar tools/morphAnalyzer/tokenizer.jar  temp/data.input temp/tagged.output");
            proc = Runtime.getRuntime().exec("java -jar tools/morphAnalyzer/morphAnalyzer.jar false temp/tagged.output temp/mophs.xml");

            Thread.sleep(2000);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse("temp/mophs.xml");

            NodeList tokens = doc.getElementsByTagName("token");
            //iterate tokens...
            HashMap<Integer,Integer> categoriesInQuery = new HashMap<Integer,Integer>();
            for (int j = 0; j < tokens.getLength(); j++) {
                NodeList analysis = tokens.item(j).getChildNodes();
                for (int k = 0; k < analysis.getLength(); k++) {
                    NodeList analysis_kids = analysis.item(k).getChildNodes();
                    for (int l = 0; l < analysis_kids.getLength(); l++) {
                        if (analysis_kids.item(l).getNodeName().equals("base")) {
                            if (analysis_kids.item(l).getAttributes().getNamedItem("transliteratedLexiconItem") != null) {
                                String key = analysis_kids.item(l).getAttributes().getNamedItem("transliteratedLexiconItem").getNodeValue();
                                if (words2idx.containsKey(key)) {
                                    for (Integer i : words2idx.get(key))
                                        if(categoriesInQuery.containsKey(i))
                                            categoriesInQuery.put(i,categoriesInQuery.get(i)+1);
                                        else
                                            categoriesInQuery.put(i,1);

                                }
                            }
                        }
                    }
                }
            }
            for(Integer i: categoriesInQuery.keySet()){
                Category c = new Category();
                c.setName(idx2Category.get(i));
                c.setRelevance(categoriesInQuery.get(i));
                res.add(c);
            }
            res.sort(new Comparator<Category>() {
                @Override
                public int compare(Category c1, Category c2) {
                    if(c1.getRelevance() == c2.getRelevance()) return  0;
                    return c1.getRelevance() < c2.getRelevance() ? 1 : -1;
                }
            });

            try {
                Files.delete(Paths.get("temp/data.input"));
                Files.delete(Paths.get("temp/tagged.output"));
                Files.delete(Paths.get("temp/mophs.xml"));

            } catch (Exception x) {
                // File permission problems are caught here.
                System.err.println(x);
            }


            return res;
        }catch(Exception e){
            System.out.println(e.getMessage());
            return null;
        }
    }
}
