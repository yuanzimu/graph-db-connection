package gov.pnnl.stucco.dbconnect.titan;

import gov.pnnl.stucco.dbconnect.Condition;
import gov.pnnl.stucco.dbconnect.DBConnectionBase;
import gov.pnnl.stucco.dbconnect.DBConstraint;
import gov.pnnl.stucco.dbconnect.StuccoDBException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.tinkerpop.rexster.client.RexProException;
import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.client.RexsterClientFactory;

public class TitanDBConnection extends DBConnectionBase {
   
    private RexsterClient graphDB = null;
    private Logger logger = null;
    private Map<String, Object> configuration = new HashMap<String,Object>();
    private Map<String, String> vertIDCache; //TODO could really split this into a simple cache class.
    private Set<String> vertIDCacheRecentlyRead;
    private String dbType = null;
    private static int VERT_ID_CACHE_LIMIT = 10000;
    private static String[] HIGH_FORWARD_DEGREE_EDGE_LABELS = {"hasFlow"}; //TODO: update as needed.  Knowing these allows some queries to be optimized.

    
    public TitanDBConnection(Map<String, Object> configuration) {
        this.configuration.putAll(configuration);
        logger = LoggerFactory.getLogger(TitanDBConnection.class);
        vertIDCache = new HashMap<String, String>((int) (VERT_ID_CACHE_LIMIT * 1.5));
        vertIDCacheRecentlyRead = new HashSet<String>((int) (VERT_ID_CACHE_LIMIT * 1.5));
    }

    @Override
    public void updateVertex(String id, Map<String, Object> properties) 
    {
        String[] keys = properties.keySet().toArray(new String[0]);
        for(int i=0; i<keys.length; i++){
            updateVertexProperty(id, keys[i], properties.get(keys[i]));
//            graphDB.commit();
            try {
                commit();
            } catch (RexProException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                logger.error("");
                throw new StuccoDBException("could not create update vertex due to DB error");
            } //added
        }
    }

    @Override
    protected void setPropertyInDB(String id, String key, Object newValue) {
        HashMap<String, Object> param = new HashMap<String, Object>();

        String cardinality = findCardinality(id, key, newValue);
        param.put("ID", Long.parseLong(id));
        param.put("KEY", key);
        param.put("VALUE", newValue);
        
        
        //TODO: verify how the new value is doing this.  Overriding or adding
        String query = "g.v(ID).setProperty(KEY, VALUE)";
        if (cardinality.equals("SET")) {
            query = "g.v(ID).addProperty(KEY, VALUE)";
        }
        
        try {
            Object rtn = executeGremlin(query, param);
        } catch (RexProException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /** 
     * {@inheritDoc}
     * <p>Gets the properties of a vertex selected by RID. 
     * 
     * TODO: deal with exceptions
     * 
     * @return1 Map of properties (or null if vertex not found)
     */
    @Override
    public Map<String, Object> getVertByID(String id) {
        if(id == null || id.isEmpty())
            return null;
        
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("ID", Integer.parseInt(id));
        Object query_ret = null;
        try {
            query_ret = executeGremlin("g.v(ID).map();", param);
        } catch (RexProException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (query_ret == null) {
            return null;
        }
        List<Map<String, Object>> query_ret_list = (List<Map<String, Object>>)query_ret;
        Map<String, Object> propertyMap = query_ret_list.get(0);
        convertListPropertiesToSets(propertyMap);

        return propertyMap;
    }
    
    private Map<String,Object> convertListPropertiesToSets(Map<String,Object> properties)
    {
        for(Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof List)
            {
                Set newValue = new HashSet((List) value);
                entry.setValue(newValue);
            }
        }
        return properties;
    }
     
    @Override
    public Map<String, Object> getVertByName(String name){
        if(name == null || name.isEmpty())
            return null;

        Map<String, Object> param = new HashMap<String, Object>();
        param.put("NAME", name);
        Object query_ret = null;
        try {
            query_ret = executeGremlin("g.query().has(\"name\",NAME).vertices().toList();", param);
        } catch (RexProException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        List<Map<String,Object>> query_ret_list = (List<Map<String,Object>>)query_ret;
        if(query_ret_list.size() == 0){
            //logger.info("findVert found 0 matching verts for name:" + name); //this is too noisy, the invoking function can complain if it wants to...
            return null;
        }else if(query_ret_list.size() > 1){
            logger.warn("findVert found more than 1 matching verts for name: " + name + " so returning the first item.");
            //return null;
        }
        //return the map of the properties from 
        Map<String, Object> gremlinMap = query_ret_list.get(0);
        
        // gremlin returns all the vertex properties associated with the "_properties" key
        Map<String, Object> propertyMap = (Map<String, Object>)gremlinMap.get("_properties");
        convertListPropertiesToSets(propertyMap);
        return propertyMap;
    }

    /**
     * {@inheritDoc}
     * <p>  function will check vertIDCache first, 
     * if id is not in there, then it is calling the findVert function
     */
    @Override
    public String getVertIDByName(String name) {
        String id = vertIDCacheGet(name);
        if(id != null){
            return id;
        }else{
            Map<String, Object> vert = getVertByName(name);
            if(vert == null) 
                id = null;
            else 
                id = vert.get("_id").toString();
            if(id != null){
                vertIDCachePut(name, id);
            }
            return id;
        }
    }
    
    private void vertIDCachePut(String name, String id){
        if(vertIDCache.size() >= VERT_ID_CACHE_LIMIT){
            logger.info("vertex id cache exceeded limit of " + VERT_ID_CACHE_LIMIT + 
                    " ... evicting " + (vertIDCache.size() - vertIDCacheRecentlyRead.size()) + " unused items.");
            vertIDCache.keySet().retainAll(vertIDCacheRecentlyRead);
            vertIDCacheRecentlyRead.clear();
        }
        vertIDCache.put(name, id);
    }
    
    private String vertIDCacheGet(String name){
        if(vertIDCache.containsKey(name)){ 
            vertIDCacheRecentlyRead.add(name);
            return vertIDCache.get(name);
        }else{
            return null;
        }
        
    }

    @Override
    public String addVertex(Map<String, Object> properties) {
        String graphType = null;
        try {
            graphType = getDBType();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        String name = (String)properties.get("name");
        Long newID = null;
        if(name == null || name.isEmpty()){
            String msg = String.format("cannot add vertex with missing or invalid vertID");
            throw new IllegalArgumentException(msg);
        }
        
        //convert any multi-valued properties to a set form.
        convertAllMultiValuesToSet(properties);
        
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("VERT_PROPS", properties);
        
        if(graphType == "TitanGraph") {
            try {
                newID = (Long)((List<Object>) executeGremlin("v = g.addVertex(null, VERT_PROPS);v.getId();", param)).get(0);
            } catch (RexProException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        vertIDCachePut(name, newID.toString());
        
        //TODO: confirm before proceeding
//        ret = false;
//        tryCommit(COMMIT_TRY_LIMIT);
//        int tryCount = 0;
//        //Confirm before proceeding
//        while(ret == false && tryCount < WRITE_CONFIRM_TRY_LIMIT){
//            //System.out.println("waiting for " + tryCount + " seconds in addVertexFromMap()");
//            waitFor(1000*tryCount + 1);
//            //TODO: why are we making potentially two calls here?
//            if( getVertByID(newID.toString()) != null && findVert(name) != null){
//                ret = true;
//            }
//            tryCount += 1;
//        }

        try {
            commit();
        } catch (RexProException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return newID.toString();
    }

    @Override
    public List<String> getVertIDsByConstraints(List<DBConstraint> constraints) {
        if(constraints == null || constraints.size() == 0)
            return null;

        Map<String, Object> param = new HashMap<String, Object>();
        String query = "g.V";
        for(int i=0; i<constraints.size(); i++){
            DBConstraint c = constraints.get(i);
            String cond = c.condString(c.getCond());
            String key = c.getProp().toUpperCase()+i;
            Object value = c.getVal();
            param.put(key, value);
            
            query += ".has(\"" + c.getProp() + "\"," + cond + "," + key + ")";
        }
        //query += ".vertices().toList();";
        query += ";";
        Object query_ret = null;
        try {
            query_ret = executeGremlin(query, param);
        } catch (RexProException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        List<Map<String,Object>> query_ret_list = (List<Map<String,Object>>)query_ret;
        
        List<String> vertIDs = new ArrayList<String>();
        if(query_ret_list != null) {
            for(Map<String,Object> vertProp : query_ret_list) {
                String id = (String)vertProp.get("_id");
                Map<String, Object> props = (Map<String, Object>)vertProp.get("_properties");
                String name = (String) props.get("name");
                //TODO: do we need name or _id?
                vertIDs.add(id);
            }
        }
        
        return vertIDs;
    }

    @Override
    public void addEdge(String inVertID, String outVertID, String relation){
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot add edge with missing or invalid relation");
        }
        if(inVertID == null || inVertID.equals("") ){
            throw new IllegalArgumentException("cannot add edge with missing or invalid inVertID");
        }
        if(outVertID == null || outVertID.equals("")){
            throw new IllegalArgumentException("cannot add edge with missing or invalid outVertID");
        }
        
        int edgeCount = getEdgeCountByRelation(inVertID, outVertID, relation);
        if(edgeCount >= 1) {
            //edge already exists, do nothing and return false.
            // (if you wanted to update its properties, this is not the method for that)
            //logger.debug("Attempted to add a duplicate edge.  ignoring .  Edge was " + edge);
            return;
        }
        
       
        Map<String, Object> props = new HashMap<String, Object>();
        //TODO: shouldn't the ID's be LONGs?
        props.put("ID_IN", Integer.parseInt(inVertID));
        props.put("ID_OUT", Integer.parseInt(outVertID));
        props.put("LABEL", relation);
        
        //and now finally add edge to graph.  If it fails, return false here. if it was ok, then we can continue below.
        try {
            Object ret = executeGremlin("g.addEdge(g.v(ID_OUT),g.v(ID_IN),LABEL)", props);
        } catch (RexProException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        
        //confirm before proceeding
//        tryCommit(COMMIT_TRY_LIMIT);
//        int tryCount = 0;
//        //Confirm before proceeding
//        while(ret == false && tryCount < WRITE_CONFIRM_TRY_LIMIT){
//            //System.out.println("waiting for " + tryCount + " seconds in addEdgeFromJSON()");
//            waitFor(1000*tryCount + 1);
//            //System.out.println("confirming edge was added: attempt " + tryCount);
//            if( getEdgeCount(inv_id, outv_id, label) >= 1){
//                ret = true;
//            }
//            tryCount += 1;
//        }
    }

    @Override
    public List<String> getInVertIDsByRelation(String outVertID, String relation) {
        
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invlid relation");
        }
        if(outVertID == null || outVertID.equals("")){
            throw new IllegalArgumentException("cannot get edge with missing or invalid outVertID");
        }
        
        Object query_ret = getVertByID(outVertID);
        if(query_ret == null){
            logger.warn("getInVertIDsByRelation could not find outVertID:" + outVertID);
            throw new IllegalArgumentException("missing or invalid outVertID");
        }
        
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("ID", Long.parseLong(outVertID));
        param.put("LABEL", relation);
        Object rtnValue = null;

        try {
            rtnValue = executeGremlin("g.v(ID).out(LABEL);", param);
        } catch (RexProException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        List<String> relatedIDs = extractIDList((List<Map<String,Object>>)rtnValue);
        
        return relatedIDs;
    }

    /**
     * Converts a list of property maps to a list of the IDs contained
     */
    private List<String> extractIDList(List<Map<String, Object>> gremlinMapList) {
        List<String> relatedIDs = new ArrayList<String>();
        for(Map<String, Object> item : gremlinMapList) {
            String id = item.get("_id").toString();
            relatedIDs.add(id);
        }
        return relatedIDs;
    }

    @Override
    public List<String> getOutVertIDsByRelation(String inVertID, String relation){
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invlid relation");
        }
        if(inVertID == null || inVertID.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid outVertID");
        }
        
        Object query_ret = getVertByID(inVertID);
        if(query_ret == null){
            logger.warn("getOutVertIDsByRelation could not find inVertID:" + inVertID);
            throw new IllegalArgumentException("missing or invalid inVertID");
        }

        Map<String, Object> param = new HashMap<String, Object>();
        param.put("ID", Long.parseLong(inVertID));
        param.put("LABEL", relation);
        Object rtnValue = null;

        try {
            rtnValue = executeGremlin("g.v(ID).in(LABEL);", param);
        } catch (RexProException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        List<String> relatedIDs = extractIDList((List<Map<String,Object>>)rtnValue);
        return relatedIDs;
    }

    @Override
    public List<String> getVertIDsByRelation(String vertID, String relation) {
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invlid relation");
        }
        if(vertID == null || vertID.equals("")) {
            throw new IllegalArgumentException("cannot get edge with missing or invalid vertID");
        }
        
        Object query_ret = getVertByID(vertID);
        if(query_ret == null){
            logger.warn("getVertIDsByRelation could not find vertID:" + vertID);
            throw new IllegalArgumentException("missing or invalid vertID");
        }
        
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("ID", Long.parseLong(vertID));
        param.put("LABEL", relation);
        Object rtnValue = null;

        try {
            rtnValue = executeGremlin("g.v(ID).both(LABEL);", param);
        } catch (RexProException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        List<String> relatedIDs = extractIDList((List<Map<String,Object>>)rtnValue);
        return relatedIDs;
    }

    /*
     * Only used by removeAllVertices()
     */
    private void removeCachedVertices(){
        //NB: this query is slow enough that connection can time out if the DB starts with many vertices.

        if(vertIDCache.isEmpty())
            return;

        //clear the cache now.
        vertIDCache.clear();
        vertIDCacheRecentlyRead.clear();// = new HashSet<String>((int) (VERT_ID_CACHE_LIMIT * 1.5));
    }
    
    @Override
    public void removeVertByID(String id) {
        
        // remove the cached vertices
        removeCachedVertices();
        
        Map<String,Object> params = new HashMap<String, Object>();
        params.put("ID", id);
        
        String query = String.format("g.removeVertex(ID)", params);
        try {
            executeGremlin(query);
        } catch (RexProException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void removeEdgeByRelation(String v1, String v2, String relation) {
       
        Map<String,Object> params = new HashMap<String, Object>();
        params.put("V1", v1);
        params.put("V2", v2);
        params.put("LABEL", relation);
        String query = String.format("g.removeEdge(V1,V2,LABEL)", params);
        try {
            executeGremlin(query);
        } catch (RexProException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public int getEdgeCountByRelation(String inVertID, String outVertID, String relation) {
        int edgeCount = 0;
        Object query_ret = null;

        Object vertIDProps = getVertByID(outVertID);
        if(vertIDProps == null){
            logger.warn("getEdgeCount could not find out_id:" + outVertID);
            throw new IllegalArgumentException("invalid outVertID: "+ outVertID);
        }
        vertIDProps = getVertByID(inVertID);
        if(vertIDProps == null){
            logger.warn("getEdgeCount could not find inv_id:" + inVertID);
            throw new IllegalArgumentException("invalid inVertID: "+ inVertID);
        }

        Map<String, Object> param = new HashMap<String, Object>();
        param.put("ID_OUT", Long.parseLong(outVertID));
        param.put("ID_IN", Long.parseLong(inVertID));
        param.put("LABEL", relation);

        boolean highDegree = false;
        for(String currLabel : HIGH_FORWARD_DEGREE_EDGE_LABELS){
            if(relation.equals(currLabel)){
                highDegree = true;
                break;
            }
        }

        if(!highDegree){
            try {
                query_ret = executeGremlin("g.v(ID_OUT).outE(LABEL).inV().id;", param);
            } catch (RexProException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            List<Long> query_ret_list = (List<Long>)query_ret;
            //System.out.println("query ret list contains " + query_ret_list.size() + " items.");
            for(Long item : query_ret_list){
                if(Long.parseLong(inVertID) == item)
                    edgeCount++;
            }
            return edgeCount;
        }else{
            try {
                query_ret = executeGremlin("g.v(ID_IN).inE(LABEL).outV().id;", param);
            } catch (RexProException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            List<Long> query_ret_list = (List<Long>)query_ret;
            //System.out.println("query ret list contains " + query_ret_list.size() + " items.");
            for(Long item : query_ret_list){
                if(Long.parseLong(outVertID) == item)
                    edgeCount++;
            }
            return edgeCount;
        }
    }
    
    @Override
    public void close() {
        if(graphDB != null){
            try {
                graphDB.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            graphDB = null;
        }
    }
    
    /**
     * need to commit this transaction
     */
    private void commit() throws RexProException, IOException{
        String graphType = getDBType();
        if(graphType != "TinkerGraph")
            executeGremlin("g.commit()");
    }

    @Override
    public void open() {
        logger.info("connecting to DB...");
        
        try {
            // add the configuration options into the structure that rexster needs them in
            Configuration configOpts = new PropertiesConfiguration();
            for(Map.Entry<String, Object> entry : configuration.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                configOpts.addProperty(key, value);
            }
            
            graphDB = RexsterClientFactory.open(configOpts); //this just throws "Exception."  bummer.
        } catch (Exception e) {
            graphDB = null;
            logger.warn(e.getLocalizedMessage());
            logger.warn(getStackTrace(e));
            throw new StuccoDBException("could not create rexster client connection to Titan");
        }
        
        int connectionWaitTime = (Integer)configuration.get("connection-wait-time");
        //if wait time given, then wait that long, so the connection can set up.  (Mostly needed for travis-ci tests)
        if(connectionWaitTime > 0){
            try {
                logger.info( "waiting for " + connectionWaitTime + " seconds for connection to establish..." );
                Thread.sleep(connectionWaitTime*1000); //in ms.
            }
            catch (InterruptedException ie) { 
                // Restore the interrupted status
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private String getDBType() throws IOException{
        if(this.dbType == null){
            String type = null;
            try{
                type = graphDB.execute("g.getClass()").get(0).toString();
            }catch(Exception e){
                logger.error("Could not find graph type!",e);
                throw new IOException("Could not find graph type!");
            }
            if( type.equals("class com.tinkerpop.blueprints.impls.tg.TinkerGraph") ){
                this.dbType = "TinkerGraph";
            }else if( type.equals("class com.thinkaurelius.titan.graphdb.database.StandardTitanGraph") ){
                this.dbType = "TitanGraph";
            }else{
                throw new IOException("Could not find graph type - unknown type!");
            }
        }
        return this.dbType;
    }

    private static String getStackTrace(Exception e){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAllVertices() {
        
        // remove the cached vertices
        removeCachedVertices();

        //NB: this query is slow enough that connection can time out if the DB starts with many vertices.
        String query = String.format("g.V.remove();g");
        try {
            executeGremlin(query);
        } catch (RexProException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    @Override
    public DBConstraint getConstraint(String property, Condition condition,
            Object value) {
        return new TitanDBConstraint(property, condition, value);
    }

    @Override
    public void buildIndex(String indexConfig) throws IOException {
        IndexDefinitionsToTitanGremlin loader = new IndexDefinitionsToTitanGremlin();
        loader.setDBConnection(this);
        loader.parse(new File(indexConfig));
        
    }
    
    /**
     * return the raw handle to the DB
     */
    public RexsterClient getGraphDB() {
        return graphDB;
    }

    //TODO make private
    //wrapper to reduce boilerplate
    //TODO wrapper throws away any return value, 
    //  it'd be nice to use this even when we want the query's retval... but then we're back w/ exceptions & don't gain much.
    public Object executeGremlin(String query, Map<String,Object> params) throws RexProException, IOException{
        if(this.graphDB == null)
            throw new IllegalArgumentException(); 
        
        //Adding a trailing return 'g' on everything: 
        // no execute() args can end up returning null, due to known API bug.
        // returning 'g' everywhere is just the simplest workaround for it, since it is always defined.
//        query += ";g";
        Object rtnValue = graphDB.execute(query, params);
        
        //TODO: there should be a commit() here?
        return rtnValue;
    }
    //likewise.
    public Object executeGremlin(String query) throws RexProException, IOException{
        if(this.graphDB == null)
            throw new IllegalArgumentException(); 
        Object rtnValue = null;
        rtnValue = graphDB.execute(query,null);
        //TODO: there should be a commit() here?
        return rtnValue;
    }
    


}
