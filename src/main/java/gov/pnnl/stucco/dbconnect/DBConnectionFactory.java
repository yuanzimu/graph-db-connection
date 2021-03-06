package gov.pnnl.stucco.dbconnect;
/**
 * $OPEN_SOURCE_DISCLAIMER$
 *
 */
import gov.pnnl.stucco.dbconnect.inmemory.InMemoryDBConnectionFactory;
import gov.pnnl.stucco.dbconnect.orientdb.OrientDBConnectionFactory;
import gov.pnnl.stucco.dbconnect.postgresql.PostgresqlDBConnectionFactory;

import java.io.File;
import java.io.FileInputStream; 
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
/**
 * DBConnectionFactory Factory to support various graph DBs
 *
 */

public abstract class DBConnectionFactory {
    
    /**
     * 
     * Types of Graph DBs we can or would like to support
     *
     */
    public static enum Type {
        TITAN,
        INMEMORY,
        ORIENTDB,
        NEO4J,
        POSTGRESQL
    }
    
    /** configuration information for the DB we will be using */
    protected Map<String,Object> configuration = new HashMap<String,Object>();

    /**
     * Base Constructor, but everything will happen in the concrete classes
     */
    public DBConnectionFactory() {

    }

    /**
     * Return a factory of the DB we want and then request the type of connection we need it for
     * @param factoryType
     * @return
     */
    public static DBConnectionFactory getFactory(Type factoryType) {
        switch (factoryType) {
            case INMEMORY:
                return new InMemoryDBConnectionFactory();
            case NEO4J:
                break;
            case ORIENTDB:
                return new OrientDBConnectionFactory();
            case TITAN:
                break;
            case POSTGRESQL:
                return new PostgresqlDBConnectionFactory();
            default:
                break;
        }
        return null;
    }
    
    /**
     * Sets the configuration with a simple map, this assumes the configuration
     * options are in a flat structure
     * @param configuration
     */
    protected void setConfiguration(Map<String, Object> configuration) {
        this.configuration.clear();
        this.configuration.putAll(configuration);
    }
    
    /**
     * Sets the configuration information by loading that information from 
     * a yaml file
     * @param configFilename
     */
    public void setConfiguration(String configFilename) {
        Map<String, Object> configuration = dbConfigFromFile(configFilename);
        this.setConfiguration(configuration);
    }

    /**
     * gets the alignment DB connection interface for the DB type factory
     * @return
     */
    public abstract DBConnectionAlignment getDBConnectionAlignment();
    
    /**
     * gets the indexer DB connection interface for the DB type factory
     * @return
     */
    public abstract DBConnectionIndexerInterface getDBConnectionIndexer();
    
    /**
     * gets the test DB connection interface for the DB type factory
     * @return
     */
    public abstract DBConnectionTestInterface getDBConnectionTestInterface();

    /**
     * gets the configuration information from the file and pulls out the 
     * appropriate database connection section
     * @param configFilePath
     * @return
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> dbConfigFromFile(String configFilePath){
        File configFile = new File(configFilePath);
        Map<String, Object> fullConfigMap = configMapFromFile(configFile);
        Map<String, Object> dbConfigMap = (Map<String, Object>) fullConfigMap.get("database_connection");

        return dbConfigMap;
    }

    /**
     * loads the configuration yaml file and puts it into a map
     * @param configFile
     * @return
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> configMapFromFile(File configFile){
        Map<String, Object> configMap = null;
        try {
            Yaml yaml = new Yaml();
            InputStream stream = new FileInputStream(configFile);

            configMap = (Map<String, Object>) yaml.load( stream );


        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return configMap;
    }
    

    @SuppressWarnings("unused")
    public static void main(String... args) {
        DBConnectionFactory factory = DBConnectionFactory.getFactory(DBConnectionFactory.Type.INMEMORY);
        System.err.println();
    }

}