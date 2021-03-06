package gov.pnnl.stucco.dbconnect.inmemory;

import gov.pnnl.stucco.dbconnect.Condition;
import gov.pnnl.stucco.dbconnect.DBConnectionBase;
import gov.pnnl.stucco.dbconnect.DBConstraint;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.slf4j.Logger;

/**
 * This class represents a concrete implementation of an in-memory DB Connection Type
 *
 */
public class InMemoryDBConnection extends DBConnectionBase{

    /** logger variable to track activities in this class*/
    private Logger logger = null;

    /** contains all the vertices via a map of maps where the values are a property map*/
    private Map<String, Map<String, Object>> vertices = null;

    /** contains a mapping of vertexIDs to the actual vertex canonical name*/
    //private Map<String, String> vertIDs = null;

    /** 
     * contains a map of edges and their properties.
     * 
     * <p> Note: We're keeping this structure even though edge IDs are no
     * longer exposed in the interface, in order to minimize code changes.
     */
    private Map<String, Map<String, Object>> edges = null; //TODO: make/use an Edge class, to store inV, outV, label?  And maybe index that.

    //private Map<String, String> edgeIDs = null; //edges don't have meaningful names.
    
    /** 
     * Index of vert fields.
     * This is a map the field name to a map of values to matching IDs
     * (eg. "name" -> some vertex name -> set of vert ID(s) which have that name)
     */
    private Map<String, Map<String, Set<String>>> indexedVertFields = null;

    /**
     * Constructor of an InMemory type of DB Connection
     */
    public InMemoryDBConnection(){
        vertices = new HashMap<String, Map<String, Object>>();
        //vertIDs = new HashMap<String, String>();
        edges = new HashMap<String, Map<String, Object>>();
        //edgeIDs = new HashMap<String, String>(); //edges don't have meaningful names.
        indexedVertFields = new HashMap<String, Map<String, Set<String>>>();
        //initialize any indexes.
        indexedVertFields.put("name", new HashMap<String, Set<String>>());
    }

    /**
     * return the number of vertices
     * @return count
     */
    @Override
    public long getVertCount(){
        return vertices.size();
    }

    /**
     * TODO: implementation
     * return the number of vertices
     * @return count
     */
    @Override
    public long getVertCountByConstraints(List<DBConstraint> constraints) {
        return 0L;
    }

    /**
     * return the number of edges
     * @return count
     */
    @Override
    public long getEdgeCount(){
        return edges.size();
    }

    /**
     * return the vertex's property map given the vertex ID
     * @param vertID
     * @return property map
     */
    @Override
    public Map<String, Object> getVertByID(String vertID){
        return vertices.get(vertID);
    }

    /**
     * returns list of edge info maps for the outgoing edges of this vertex
     * @param vertName
     * @return list of edge property maps
     */
    @Override
    public List<Map<String, Object>> getOutEdges(String outVertID) throws IllegalArgumentException{
        if(outVertID == null || outVertID.equals("") || !vertices.containsKey(outVertID)){
            throw new IllegalArgumentException("cannot get edge with missing or invalid outVertID");
        }
        List<Map<String, Object>> foundEdges = new LinkedList<Map<String, Object>>();
        for(Map<String, Object> currEdge : edges.values()){
            if( ((String)currEdge.get("outVertID")).equals(outVertID) ){
                //inVertID = currEdge.get("inVertID");
                //outVertID = currEdge.get("outVertID");
                //relation = currEdge.get("relation");
                foundEdges.add( currEdge );
            }
        }
        return foundEdges;
    }

    /**
     * returns list of edge info maps for the incoming edges of this vertex
     * @param vertName
     * @return list of edge property maps
     */
    @Override
    public List<Map<String, Object>> getInEdges(String inVertID) throws IllegalArgumentException{
        if(inVertID == null || inVertID.equals("") || !vertices.containsKey(inVertID)){
            throw new IllegalArgumentException("cannot get edge with missing or invalid inVertID");
        }
        List<Map<String, Object>> foundEdges = new LinkedList<Map<String, Object>>();
        for(Map<String, Object> currEdge : edges.values()){
            if( ((String)currEdge.get("inVertID")).equals(inVertID) ){
                foundEdges.add( currEdge );
            }
        }
        return foundEdges;
    }

    /**
     * returns list of edge info maps for the outgoing edges of this vertex
     * @param vertName
     * @return list of edge property maps
     */
    @Override
    public List<Map<String, Object>> getOutEdgesPage(String outVertID, int offset, int limit) throws IllegalArgumentException{
        if(outVertID == null || outVertID.equals("") || !vertices.containsKey(outVertID)){
            throw new IllegalArgumentException("cannot get edge with missing or invalid outVertID");
        }
        List<Map<String, Object>> foundEdges = new LinkedList<Map<String, Object>>();
        for(Map<String, Object> currEdge : edges.values()){
            if( ((String)currEdge.get("outVertID")).equals(outVertID) ){
                //inVertID = currEdge.get("inVertID");
                //outVertID = currEdge.get("outVertID");
                //relation = currEdge.get("relation");
                foundEdges.add( currEdge );
            }
        }
        List<Map<String, Object>> foundEdgesPage = new LinkedList<Map<String, Object>>();
        if (foundEdges.size() > offset) {
            int end = Math.min(foundEdges.size(), offset + limit);
            for (int i = offset; i < end; i++) {
                foundEdgesPage.add(foundEdges.get(i));
            }
        }

        return foundEdgesPage;
    }

    /**
     * returns list of edge info maps for the incoming edges of this vertex
     * @param vertName
     * @return list of edge property maps
     */
    @Override
    public List<Map<String, Object>> getInEdgesPage(String inVertID, int offset, int limit) throws IllegalArgumentException{
        if(inVertID == null || inVertID.equals("") || !vertices.containsKey(inVertID)){
            throw new IllegalArgumentException("cannot get edge with missing or invalid inVertID");
        }
        List<Map<String, Object>> foundEdges = new LinkedList<Map<String, Object>>();
        for(Map<String, Object> currEdge : edges.values()){
            if( ((String)currEdge.get("inVertID")).equals(inVertID) ){
                foundEdges.add( currEdge );
            }
        }
        List<Map<String, Object>> foundEdgesPage = new LinkedList<Map<String, Object>>();
        if (foundEdges.size() > offset) {
            int end = Math.min(foundEdges.size(), offset + limit);
            for (int i = offset; i < end; i++) {
                foundEdgesPage.add(foundEdges.get(i));
            }
        }

        return foundEdgesPage;
    }

    /**
     * return a list of Incoming vertices based on their edge type relation
     * @param outVertID
     * @param relation
     * @return list of vertices
     */
    @Override
    public List<String> getInVertIDsByRelation(String outVertID, String relation){
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid relation");
        }
        if(outVertID == null || outVertID.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid outVertID");
        }

        List<String> relatedIDs = new LinkedList<String>();
        for(Map<String, Object> currEdge : edges.values()){
            if( ((String)currEdge.get("relation")).equals(relation) ){
                if( ((String)currEdge.get("outVertID")).equals(outVertID) ){
                    relatedIDs.add( (String)currEdge.get("inVertID") ); //TODO: check valid state here?
                }
            }
        }
        return relatedIDs;
    }

    /**
     * return a list of Outgoing vertices based on their edge type relation
     * @param inVertID
     * @param relation
     * @return list of vertices
     */
    @Override
    public List<String> getOutVertIDsByRelation(String inVertID, String relation){
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid relation");
        }
        if(inVertID == null || inVertID.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid inVertID");
        }

        List<String> relatedIDs = new LinkedList<String>();
        for(Map<String, Object> currEdge : edges.values()){
            if( ((String)currEdge.get("relation")).equals(relation) ){
                if( ((String)currEdge.get("inVertID")).equals(inVertID) ){
                    relatedIDs.add( (String)currEdge.get("outVertID") ); //TODO: check valid state here?
                }
            }
        }
        return relatedIDs;
    }

    /**
     * get the list of incoming or outgoing vertices based on edge relationship
     * @param vertID
     * @param relation
     * @return list of vertices
     */
    @Override
    public List<String> getVertIDsByRelation(String vertID, String relation){
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid relation");
        }
        if(vertID == null || vertID.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid inVertID");
        }

        List<String> relatedIDs = new LinkedList<String>();
        for(Map<String, Object> currEdge : edges.values()){
            if( ((String)currEdge.get("relation")).equals(relation) ){
                if( ((String)currEdge.get("inVertID")).equals(vertID) ){
                    relatedIDs.add( (String)currEdge.get("outVertID") ); //TODO: check valid state here?
                }else if( ((String)currEdge.get("outVertID")).equals(vertID) ){
                    relatedIDs.add( (String)currEdge.get("inVertID") ); //TODO: check valid state here?
                }
            }
        }
        return relatedIDs;
    }

    @Override
    public List<String> getInVertIDsByRelation(String v1, String relation, List<DBConstraint> constraints){
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid relation");
        }
        if(v1 == null || v1.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid Vertex ID");
        }
        //TODO: consider faster implementations
        Set<String> constraintIDs = new HashSet<String>(getVertIDsByConstraints(constraints));
        Set<String> neighborIDs = new HashSet<String>(getInVertIDsByRelation(v1, relation));
        constraintIDs.retainAll(neighborIDs);
        List<String> ret = new LinkedList<String>(constraintIDs);
        return ret;
    }

    @Override
    public List<String> getOutVertIDsByRelation(String v1, String relation, List<DBConstraint> constraints){
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid relation");
        }
        if(v1 == null || v1.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid Vertex ID");
        }
        //TODO: consider faster implementations
        Set<String> constraintIDs = new HashSet<String>(getVertIDsByConstraints(constraints));
        Set<String> neighborIDs = new HashSet<String>(getOutVertIDsByRelation(v1, relation));
        constraintIDs.retainAll(neighborIDs);
        List<String> ret = new LinkedList<String>(constraintIDs);
        return ret;
    }

    @Override
    public List<String> getVertIDsByRelation(String v1, String relation, List<DBConstraint> constraints){
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid relation");
        }
        if(v1 == null || v1.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid Vertex ID");
        }
        //TODO: consider faster implementations
        Set<String> constraintIDs = new HashSet<String>(getVertIDsByConstraints(constraints));
        Set<String> neighborIDs = new HashSet<String>(getVertIDsByRelation(v1, relation));
        constraintIDs.retainAll(neighborIDs);
        List<String> ret = new LinkedList<String>(constraintIDs);
        return ret;
    }

    @Override
    public int getEdgeCountByRelation(String inVertID, String outVertID, String relation){
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid relation");
        }
        if(inVertID == null || inVertID.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid inVertID");
        }
        if(outVertID == null || outVertID.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid outVertID");
        }

        int count = 0;
        for(Map<String, Object> currEdge : edges.values()) {
            if( currEdge.get("relation").equals(relation) && 
                currEdge.get("outVertID").equals(outVertID)  && 
                currEdge.get("inVertID").equals(inVertID) ) {
                count++;
            }
        }
        return count;
    }

    /**
     * get a list of vertex IDs based on a list of constraints
     * @param constraints list of constraints
     * @return list of vertex IDs
     */
    @Override
    public List<String> getVertIDsByConstraints(List<DBConstraint> constraints){
        Set<String> candidateIDs = null;
        Set<String> nonMatchingIDs = new HashSet<String>();
        List<String> matchingIDs = new LinkedList<String>();

        //First, generate candidateIDs set.
        //Note that after candidateIDs is populated here, it will not be modified.
        //TODO: really, we want to create a set of candidate ids for each index used, then find the overlap,
        //  then match against any constraints that weren't used.
        boolean indicesUsed = false;
        if(indexedVertFields.size() > 0){ //TODO: needs better test coverage for use of indices
            //This should use indexed fields to find candidateIDs, then find the nonMatchingIDs below as usual.
            //we need to decide if only exact matches are allowed, or if ranges & etc. are ok here.
            //also, somehow indicate that the constraints used here are 'done', so they aren't re-checked below.
            candidateIDs = new HashSet<String>();
            for(DBConstraint c : constraints){
                if(c.getCond() != Condition.eq)
                    continue;
                if(indexedVertFields.containsKey(c.getProp())){
                    indicesUsed = true;
                    Map<String, Set<String>> currIndex = indexedVertFields.get(c.getProp());
                    String currValue = c.getVal().toString();
                    Set<String> currSet = currIndex.get(currValue);
                    if(currSet != null){
                        candidateIDs.addAll(currSet);
                    }
                }
            }
        }
        if(!indicesUsed){
            //if no initial matchingIDs set was generated yet, use all IDs
            candidateIDs = vertices.keySet();
        }

        //make set of non-matching candidates, based on constraints
        for(String id : candidateIDs){
            Map<String, Object> candidateVert = vertices.get(id);
            for(DBConstraint c : constraints){
                Object candidateValue = candidateVert.get(c.getProp());
                if( !compare(candidateValue, c.getCond(), c.getVal()) ){
                    nonMatchingIDs.add(id);
                    break;
                }
            }
        }

        // build the matchingIDs list, based on candidateIDs and nonMatchingIDs
        for(String id : candidateIDs){
            if( !nonMatchingIDs.contains(id) ){
                matchingIDs.add(id);
            }
        }

        return matchingIDs;
    }


    /**
     * Perform a query/search of the DB using the following constraints on the request
     * @param constraints - list of constraint objects
     * @return list of vertex IDs
     */
    @Override
    public List<String> getVertIDsByConstraints(List<DBConstraint> constraints, int offset, int limit) {
        Set<String> candidateIDs = null;
        Set<String> nonMatchingIDs = new HashSet<String>();
        List<String> matchingIDs = new LinkedList<String>();

        //First, generate candidateIDs set.
        //Note that after candidateIDs is populated here, it will not be modified.
        //TODO: really, we want to create a set of candidate ids for each index used, then find the overlap,
        //  then match against any constraints that weren't used.
        boolean indicesUsed = false;
        if(indexedVertFields.size() > 0){ //TODO: needs better test coverage for use of indices
            //This should use indexed fields to find candidateIDs, then find the nonMatchingIDs below as usual.
            //we need to decide if only exact matches are allowed, or if ranges & etc. are ok here.
            //also, somehow indicate that the constraints used here are 'done', so they aren't re-checked below.
            candidateIDs = new HashSet<String>();
            for(DBConstraint c : constraints){
                if(c.getCond() != Condition.eq)
                    continue;
                if(indexedVertFields.containsKey(c.getProp())){
                    indicesUsed = true;
                    Map<String, Set<String>> currIndex = indexedVertFields.get(c.getProp());
                    String currValue = c.getVal().toString();
                    Set<String> currSet = currIndex.get(currValue);
                    if(currSet != null){
                        candidateIDs.addAll(currSet);
                    }
                }
            }
        }
        if(!indicesUsed){
            //if no initial matchingIDs set was generated yet, use all IDs
            candidateIDs = vertices.keySet();
        }

        //make set of non-matching candidates, based on constraints
        for(String id : candidateIDs){
            Map<String, Object> candidateVert = vertices.get(id);
            for(DBConstraint c : constraints){
                Object candidateValue = candidateVert.get(c.getProp());
                if( !compare(candidateValue, c.getCond(), c.getVal()) ){
                    nonMatchingIDs.add(id);
                    break;
                }
            }
        }

        // build the matchingIDs list, based on candidateIDs and nonMatchingIDs
        for(String id : candidateIDs){
            if( !nonMatchingIDs.contains(id) ){
                matchingIDs.add(id);
            }
        }


        if (candidateIDs.size() < offset) {
            List<String> ids = new ArrayList<String>(candidateIDs);
            int end = java.lang.Math.min(offset + limit, ids.size());
            for (int i = offset; i < end ; i++) {
                String id = ids.get(i);
                if (!nonMatchingIDs.contains(id)) {
                    matchingIDs.add(id);
                }
           }
        }

        return matchingIDs;
    };

    /**
     * method to compare two objects that can use the conditional object
     * @param o1
     * @param cond
     * @param o2
     * @return true or false
     */
    private boolean compare(Object o1, Condition cond, Object o2){

        if(o1 == null && cond == Condition.eq && o2 == null)
            return true;

        if(o1 == null || o2 == null)
            return false;

        if(cond == Condition.eq){
            return o1.equals(o2);
        }
        if(cond == Condition.neq){
            return !o1.equals(o2);
        }
        if(cond == Condition.gt){
            if(o1 instanceof Comparable && o2 instanceof Comparable){
                Comparable c1 = (Comparable)o1;
                Comparable c2 = (Comparable)o2;
                return ( c1.compareTo(c2) > 0 );
            }else{
                return false;
            }
        }
        if(cond == Condition.gte){
            if(o1 instanceof Comparable && o2 instanceof Comparable){
                Comparable c1 = (Comparable)o1;
                Comparable c2 = (Comparable)o2;
                return ( c1.compareTo(c2) >= 0 );
            }else{
                return false;
            }
        }
        if(cond == Condition.lt){
            if(o1 instanceof Comparable && o2 instanceof Comparable){
                Comparable c1 = (Comparable)o1;
                Comparable c2 = (Comparable)o2;
                return ( c1.compareTo(c2) < 0 );
            }else{
                return false;
            }
        }
        if(cond == Condition.lte){
            if(o1 instanceof Comparable && o2 instanceof Comparable){
                Comparable c1 = (Comparable)o1;
                Comparable c2 = (Comparable)o2;
                return ( c1.compareTo(c2) <= 0 );
            }else{
                return false;
            }
        }
        if(cond == Condition.contains){
            if(o1 instanceof Collection){
                Collection c1 = (Collection)o1;
                return c1.contains(o2);
            }else{
                return false;
            }
        }
        if(cond == Condition.substring){
            if(o1 instanceof String){
                String s1 = (String)o1;
                String s2 = "";
                if(o2 instanceof String || o2 instanceof Character){
                    s2 += o2;
                    return s1.contains(s2);
                }
            }
            return false;
        }

        return false;
    }

    /**
     * Determines based on object type whether a value (in the incoming lists) is contained
     * in the other other incoming list 
     * @param o1
     * @param o2
     * @return true or false
     */
    private boolean contains(Object o1, Object o2){
        //TODO: confirm that all of these are behaving as a user would expect for all type combinations.
        //eg. "asdf4.222" does not contain (Double)4.2 or (Integer)4
        //[101.0, 102.0] does not contain 101, and [101, 102] does not contain 101.0
        if(o1 instanceof Collection){
            Collection c1 = (Collection)o1;
            return c1.contains(o2);
        }else if(o1 instanceof byte[]){
            byte[] a1 = (byte[])o1;
            for(int i=0; i<a1.length; i++){
                //System.out.println("val is " + a1[i]);
                if( ((Byte)a1[i]).equals(o2)) return true;
            }
        }else if(o1 instanceof short[]){
            short[] a1 = (short[])o1;
            for(int i=0; i<a1.length; i++){
                //System.out.println("val is " + a1[i]);
                if( ((Short)a1[i]).equals(o2)) return true;
            }
        }else if(o1 instanceof int[]){
            int[] a1 = (int[])o1;
            for(int i=0; i<a1.length; i++){
                //System.out.println("val is " + a1[i]);
                if( ((Integer)a1[i]).equals(o2)) return true;
            }
        }else if(o1 instanceof long[]){
            long[] a1 = (long[])o1;
            for(int i=0; i<a1.length; i++){
                //System.out.println("val is " + a1[i]);
                if( ((Long)a1[i]).equals(o2)) return true;
            }
        }else if(o1 instanceof float[]){
            float[] a1 = (float[])o1;
            for(int i=0; i<a1.length; i++){
                //System.out.println("val is " + a1[i]);
                if( ((Float)a1[i]).equals(o2)) return true;
            }
        }else if(o1 instanceof double[]){
            double[] a1 = (double[])o1;
            for(int i=0; i<a1.length; i++){
                //System.out.println("val is " + a1[i]);
                if( ((Double)a1[i]).equals(o2)) return true;
            }
        }else if(o1 instanceof boolean[]){
            boolean[] a1 = (boolean[])o1;
            for(int i=0; i<a1.length; i++){
                //System.out.println("val is " + a1[i]);
                if( ((Boolean)a1[i]).equals(o2)) return true;
            }
        }
        return false;
    }

    /**
     * remove a vertex by a vertex ID
     * @param vertID
     */
    @Override
    public void removeVertByID(String vertID){
        Map<String,Object> vert = vertices.get(vertID);
        if(vert != null){
            removeVertFromIndex(vert, vertID);
            vertices.remove(vertID);

            for(String edgeID: edges.keySet()){
                Map<String, Object> currEdge = edges.get(edgeID);
                if( ((String)currEdge.get("inVertID")).equals(vertID) ){
                    edges.remove(edgeID);
                }
                else if( ((String)currEdge.get("outVertID")).equals(vertID) ){
                    edges.remove(edgeID);
                }
            }
        }
    }

    /**
     * add a vertex given a property map
     * @param vert - property map
     * @return vertexID
     */
    @Override
    public String addVertex(Map<String, Object> vert){
        // make sure all multi-value properties are sets
        convertAllMultiValuesToSet(vert);
        String vertID = String.valueOf( UUID.randomUUID() );
        vertices.put(vertID, vert);

        //update any indices
        addVertToIndex(vert, vertID);

        return vertID;
    }

    private void addVertToIndex(Map<String, Object> vert, String vertID){
        for(String prop : vert.keySet()){
            if(indexedVertFields.containsKey(prop)){
                Map<String, Set<String>> currIndex = indexedVertFields.get(prop);
                String currValue = vert.get(prop).toString();
                Set<String> currSet = currIndex.get(currValue);
                if(currSet == null){
                    currSet = new HashSet<String>();
                    currIndex.put(currValue, currSet);
                }
                currSet.add(vertID);
            }
        }
    }

    private void removeVertFromIndex(Map<String, Object> vert, String vertID){
        for(String prop : vert.keySet()){
            if(indexedVertFields.containsKey(prop)){
                Map<String, Set<String>> currIndex = indexedVertFields.get(prop);
                String currValue = vert.get(prop).toString();
                currIndex.get(currValue).remove(vertID);
            }
        }
    }

    /**
     * add and edge
     * @param inVertID ID of the incoming vertex edge
     * @param outVertID - ID of the outgoing vertex edge
     * @param relation - type of edge relation
     */
    @Override
    public void addEdge(String inVertID, String outVertID, String relation){
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot add edge with missing or invlid relation");
        }
        if(inVertID == null || inVertID.equals("") || !vertices.containsKey(inVertID)){
            throw new IllegalArgumentException("cannot add edge with missing or invalid inVertID");
        }
        if(outVertID == null || outVertID.equals("") || !vertices.containsKey(outVertID)){
            throw new IllegalArgumentException("cannot add edge with missing or invalid outVertID");
        }
        //TODO: check if edge is duplicate??  For now, just add it, duplicates are ok I guess.

        Map<String, Object> newEdge = new HashMap<String, Object>();
        newEdge.put("inVertID", inVertID);
        newEdge.put("outVertID", outVertID);
        newEdge.put("relation", relation);

        String edgeID = String.valueOf( UUID.randomUUID() );
        edges.put(edgeID, newEdge);
        //TODO: update any indices
    }

    /**
     * overwrite or add new properties to an existing vertex's property map
     * @param vertID
     * @param newVert - property map
     */
    @Override
    public void updateVertex(String vertID, Map<String, Object> newVert){
        Map<String, Object> oldVert = vertices.get(vertID);
        if(oldVert == null){
            throw new IllegalArgumentException("invalid vertex ID");
        }

        removeVertFromIndex(oldVert, vertID);
        for(Map.Entry<String, Object> entry: newVert.entrySet()){
            String key = entry.getKey();
            Object newValue = entry.getValue();
            updateVertexProperty(vertID, key, newValue);
        }
        addVertToIndex(newVert, vertID);
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public void open() {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeEdgeByRelation(String inVertID, String outVertID, String relation){
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot add edge with missing or invlid relation");
        }
        if(inVertID == null || inVertID.equals("") || !vertices.containsKey(inVertID)){
            throw new IllegalArgumentException("cannot add edge with missing or invalid inVertID");
        }
        if(outVertID == null || outVertID.equals("") || !vertices.containsKey(outVertID)){
            throw new IllegalArgumentException("cannot add edge with missing or invalid outVertID");
        }
        
        // collect the edge IDs that need to be removed
        Set<String> edgeIDs = new HashSet<String>();
        for(Map.Entry<String, Map<String, Object>> entry : edges.entrySet()) {
            String edgeID = entry.getKey();
            Map<String, Object> currEdge = entry.getValue();
            if( currEdge.get("relation").equals(relation) && 
                currEdge.get("outVertID").equals(outVertID)  && 
                currEdge.get("inVertID").equals(inVertID) ) {
                edgeIDs.add(edgeID);
            }
        }
        
        //remove the IDs we found
        for(String edgeID : edgeIDs) {
            edges.remove(edgeID);
        }
    }

    @Override
    public void removeAllVertices() {
        vertices.clear();
        indexedVertFields.clear();
        edges.clear();
        
    }

    @Override
    public DBConstraint getConstraint(String property, Condition condition,
            Object value) {
        
        return new InMemoryConstraint(property, condition, value);
    }

    @Override
    public void buildIndex(String indexConfig) {
        // NO-OP
        
    }
    
    @Override
    protected void setPropertyInDB(String id, String key, Object newValue) {
        
        vertices.get(id).put(key, newValue);
    }

    @Override
    public void loadState(String filePath) {
        try {
            InputStream is = new FileInputStream(filePath);
            String textContents = IOUtils.toString( is );
            is.close();

            JSONObject contents = new JSONObject(textContents);
            JSONObject vertsJSON = contents.getJSONObject("vertices");
            JSONArray edgesJSON = contents.getJSONArray("edges");
            //add vertices
            for( Object id : vertsJSON.keySet() ) {
                JSONObject jsonVert = vertsJSON.getJSONObject(id.toString());
                String description = jsonVert.optString("description");
                if(description != null && !description.equals("")){
                    //This is kind of an odd workaround, to prevent ui from treating, eg, "URI: www.blah.com | Type: URL |" as a URL instead of a string.
                    //TODO: this is really a problem in the UI, as far as we care it's still just a string either way.
                    jsonVert.put("description", " " + description);
                }else{
                    //ui assumes everything has a description, this is a workaround to avoid having empty text in various places.
                    jsonVert.put("description", jsonVert.optString("name"));
                }
                Map<String, Object> vert = jsonVertToMap(jsonVert);
                vertices.put(id.toString(), vert);
                String name = (String)vert.get("name");
                addVertToIndex(vert, id.toString());
                //System.out.println("loaded vertex named " + name + " with id: " + id); //for debugging
            }
            //add edges.
            for( int i=0; i<edgesJSON.length(); i++ ) {
                JSONObject edge = edgesJSON.getJSONObject(i);
                try {
                    String inVertID = edge.getString("inVertID");
                    String outVertID = edge.getString("outVertID");
                    String relation = edge.getString("relation");
                    int matchingEdgeCount = getEdgeCountByRelation(inVertID, outVertID, relation);
                    if(matchingEdgeCount == 0){
                        addEdge(inVertID, outVertID, relation);
                    }
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    System.err.println("error when loading edge: " + edge);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //TODO: tests
    @Override
    public void saveState(String filePath) {
        try {
            OutputStream os = new FileOutputStream(filePath);
            PrintStream printStream = new PrintStream(os);
            //printStream.print("String");

            JSONObject vertsJSON = new JSONObject();
            JSONArray edgesJSON = new JSONArray();

            //add vertices
            for( String id : vertices.keySet() ) {
                Map<String, Object> vert = vertices.get(id);
                JSONObject currEdge = new JSONObject();
                for( String prop : vert.keySet() ){
                    //TODO: confirm this handles sets properly
                    currEdge.put(prop, vert.get(prop));
                }
                edgesJSON.put(currEdge);
            }

            //add edges.
            for( String id : edges.keySet() ) {
                Map<String, Object> edge = edges.get(id);
                JSONObject currEdge = new JSONObject();
                for( String prop : edge.keySet() ){
                    currEdge.put(prop, edge.get(prop));
                }
                edgesJSON.put(currEdge);
            }

            JSONObject contents = new JSONObject();
            contents.put("vertices", vertsJSON);
            contents.put("edges", edgesJSON);

            printStream.print(contents.toString(2));
            printStream.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void bulkLoadGraph(JSONObject graph) {}

    /**
     * gets the number of edges in the graph dest id = inVertID
     */
    @Override
    public long getInEdgeCount(String inVertID) {
        return 0L;
    };

    /**
     * gets the number of edges in the graph with src id = outVertID
     */
    @Override
    public long getOutEdgeCount(String outVertID) {
        return 0L;
    }
}
