package gov.pnnl.stucco.dbconnect.elasticsearch;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import gov.pnnl.stucco.dbconnect.Condition;
import gov.pnnl.stucco.dbconnect.DBConstraint;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for ElasticsearchDBConnection
 */
public class ElasticsearchDBConnectionTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case 
     */
    public ElasticsearchDBConnectionTest( String testName ) {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite( ElasticsearchDBConnectionTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        try {
            ElasticsearchDBConnection es = new ElasticsearchDBConnection(); //new ElasticsearchDBConnection("./config/elasticsearch.yaml");
            Connection connection = es.getConnection();

            connection.open();
//            connection.printDBContent();
//            JSONObject vertex = connection.getVertByName("128.219.49.14");
//            JSONObject vertex = connection.getVertByName("80");
//            JSONObject vertex = connection.getVertByName("50.7.55.82:80");
//            JSONObject vertex = connection.getVertByName("128.219.49.14:38828_through_50.7.55.82:80");
//            JSONArray array = connection.getVertByType("observable",0,5);
//            JSONArray array = connection.getOutEdgesPage("56942", "port", 0, 5);
            List<DBConstraint> constraintList = new ArrayList<DBConstraint>();
//            constraintList.add(new ElasticsearchDBConstraint("ip", Condition.eq, "104.28.14.96"));
//            constraintList.add(new ElasticsearchDBConstraint("port", Condition.neq, "57588"));
            constraintList.add(new ElasticsearchDBConstraint("observableType", Condition.eq, "ip"));
            constraintList.add(new ElasticsearchDBConstraint("name", Condition.neq, "104.28.14.96"));
            constraintList.add(new ElasticsearchDBConstraint("name", Condition.neq, "128.219.49.14:38828_through_50.7.55.82:80"));
//            constraintList.add(new ElasticsearchDBConstraint("name", Condition.eq, ""));
            connection.countVertsByConstraints(constraintList);
//            System.out.println(vertex);
            connection.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } /*catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
        assertTrue( true );
    }
}