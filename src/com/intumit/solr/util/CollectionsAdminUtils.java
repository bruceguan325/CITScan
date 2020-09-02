package com.intumit.solr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.cloud.OverseerCollectionProcessor;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.params.CollectionParams.CollectionAction;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.json.JSONException;
import org.json.JSONObject;

public class CollectionsAdminUtils {
    static final String COLL_ADMIN_PATH = "/admin/collections";

    private ClusterState clusterState;

    public CollectionsAdminUtils(ClusterState clusterState) {
        this.clusterState = clusterState;
    }

    public String findLiveNodeBaseUrl(String nodeName) {
        try {
            return "http://" + StringUtils.replace(java.net.URLDecoder.decode(nodeName, "UTF-8"), "_", "/");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Replica findReplica(String replicaName) {
        Set<String> colls = clusterState.getCollections();

        for (String coll : colls) {
            Collection<Slice> slices = clusterState.getActiveSlices(coll);

            if (slices == null) break;

            for (Slice slice : slices) {
                for (Replica replica : slice.getReplicas()) {
                    if (StringUtils.equals(replica.getName(), replicaName)) return replica;
                }
            }
        }

        return null;
    }

    private String firstLiveNode() {
        Set<String> liveNodes = clusterState.getLiveNodes();

        for (String nodeName : liveNodes) {
            return nodeName;
        }

        return null;
    }
    
    void createCollection(String collectionName, int numShards,
            int numReplicas, int maxShardsPerNode, SolrServer client, String createNodeSetStr)
            throws SolrServerException, IOException {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("action", CollectionParams.CollectionAction.CREATE.toString());
        params.set(OverseerCollectionProcessor.NUM_SLICES, numShards);
        params.set(OverseerCollectionProcessor.REPLICATION_FACTOR, numReplicas);
        params.set(OverseerCollectionProcessor.MAX_SHARDS_PER_NODE, maxShardsPerNode);
        if (createNodeSetStr != null) params.set(OverseerCollectionProcessor.CREATE_NODE_SET, createNodeSetStr);

        params.set("name", collectionName);
        SolrRequest request = new QueryRequest(params);
        request.setPath(COLL_ADMIN_PATH);

        client.request(request);
    }
    
    public Collection<String> listCollectionNames() {
        return clusterState.getCollections();
    }
    
    public Map<Slice, Collection<Replica>> getDetailOfNode(String collName, String nodeName) {
        Map<Slice, Collection<Replica>> map = new HashMap<>();
        Collection<Slice> slices = clusterState.getActiveSlices(collName);
        
        for (Slice slice: slices) {
            Collection<Replica> replicasOnTheNode = new ArrayList<>();
            
            for (Replica r: slice.getReplicas()) {
                if (StringUtils.equals(nodeName, r.getNodeName())) {
                    replicasOnTheNode.add(r);
                }
            }
            
            map.put(slice, replicasOnTheNode);
        }
        
        return map;
    }
    
    public Collection<Replica> listReplicas(Slice shard) {
        return shard.getReplicas();
    }
    
    public JSONObject deleteReplica(String collName, String shardName, String replicaName)
            throws IOException, JSONException {
        String liveNodeBaseUrl = findLiveNodeBaseUrl(firstLiveNode());
        String url = liveNodeBaseUrl + COLL_ADMIN_PATH + "?wt=json&action="
                + CollectionAction.DELETEREPLICA.name() + "&collection=" + collName + "&shard="
                + shardName + "&replica=" + replicaName;

        String jsonStr = getUrlContent(url);

        return new JSONObject(jsonStr);
    }
    
    public JSONObject addReplica(String collName, String nodeName, String shardName)
            throws IOException, JSONException {
        String liveNodeBaseUrl = findLiveNodeBaseUrl(firstLiveNode());
        String url = liveNodeBaseUrl + COLL_ADMIN_PATH + "?wt=json&action="
                + CollectionAction.ADDREPLICA.name() + "&collection=" + collName + "&shard="
                + shardName + "&node=" + URLEncoder.encode(nodeName, "UTF-8");

        String jsonStr = getUrlContent(url);

        return new JSONObject(jsonStr);
    }

    String getUrlContent(String urlStr) throws IOException {
        StringBuilder builder = new StringBuilder();
        URL url = new URL(urlStr);
        URLConnection connection = url.openConnection();
        connection
                .setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows; U; Windows NT 6.0; zh-TW; rv:1.9.0.8) Gecko/2009032609 Firefox/3.0.8 (.NET CLR 3.5.30729)");
        connection.setRequestProperty("Content-Type", "text/xml; charset:UTF-8;");
        connection.setRequestProperty("Accept-Language", "zh-tw,en-us;q=0.7,en;q=0.3");
        connection.setRequestProperty("Accept-Charset", "utf-8");

        HttpURLConnection httpConn = (HttpURLConnection)connection;
        InputStream _is;
        if (httpConn.getResponseCode() >= 400) {
            /* error from server */
            _is = httpConn.getErrorStream();
        }
        else {
            _is = httpConn.getInputStream();
        }

        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(_is, "UTF-8"));
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        return builder.toString();
    }

    public static String getBaseUrl(Replica replica) {
        return replica.getStr("base_url");
    }
}
