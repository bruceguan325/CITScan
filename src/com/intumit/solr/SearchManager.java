package com.intumit.solr;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.cloud.ZooKeeperException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.config.ColumnNameMappingFacade;
import com.intumit.solr.servlet.SolrDispatchFilter;
import com.intumit.solr.util.CollectionsAdminUtils;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.systemconfig.WiseSystemConfig;

public class SearchManager {
	private static final Logger LOG = LoggerFactory.getLogger(SearchManager.class);

	private static final String DEFAULT_KEY = "DEFAULT_KEY";

	private static Boolean cloudMode = null;
	private static Boolean esClientOnlyMode = null;
	static CoreContainer container = null;
	static Map<String, CloudSolrServer> cloudServers = new HashMap<String, CloudSolrServer>();
	static CuratorFramework curatorClient = null;
	private static HttpClient myHttpClient;
	
	public static SolrServer getServer() {
		if (!isCloudMode())
			throw new RuntimeException("Cluster mode Not Supported, please use getServer(coreName) instead.");

		if (container == null && !cloudMode) {
			init();
		}
		SolrServer server = null;
		
        try {
            Thread.sleep(8000);
        }
        catch (InterruptedException ignore) {}

        if (isCloudMode()) {
            if (cloudServers.containsKey(DEFAULT_KEY)) {
                server = cloudServers.get(DEFAULT_KEY);
            }
            else {
                CloudSolrServer css = new CloudSolrServer(System.getProperty("zkHost"));
                css.connect();

                cloudServers.put(DEFAULT_KEY, css);
                server = css;
            }
        }
		return server;
	}

	/**
	 * 因應以後會有一些統計使用的 core, 全部用編號可能不是很合適
	 * 
	 * @param collectionName
	 * @return
	 */
	public static SolrServer getServer(String collectionName) {
		if (container == null && !isCloudMode()) {
			init();
		}
		SolrServer server = null;
		if (isCloudMode()) {
			if (cloudServers.containsKey(collectionName)) {
				server = cloudServers.get(collectionName);
			}
			else {
				CloudSolrServer css = new CloudSolrServer(System.getProperty("zkHost"));
				css.setDefaultCollection(collectionName);
				css.connect();
				
				cloudServers.put(collectionName, css);
				
				server = css;
			}
		}
		else {
			server = new EmbeddedSolrServer(container, collectionName);
		}
		return server;
	}
	
	public static boolean isCloudMode() {
		if (cloudMode == null)
			cloudMode = System.getProperty("zkHost") != null;
		
		return cloudMode.booleanValue();
	}
	
	public static boolean isESClientOnlyMode() {
		if (esClientOnlyMode == null)
			esClientOnlyMode = System.getProperty("esClientOnlyMode") != null;
		
		return esClientOnlyMode.booleanValue();
	}
	
	public static CuratorFramework getCuratorClient() {
		if (curatorClient == null) {
			RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
			String zkConnStr = System.getProperty("zkHost");
			curatorClient = CuratorFrameworkFactory.newClient(zkConnStr, retryPolicy);
			curatorClient.start();
		}
		return curatorClient;
	}
	
	public static boolean isInitialized() {
		return container != null;
	}

	/**
	 * 因應以後會有一些統計使用的 core, 全部用編號可能不是很合適
	 * 
	 * @param coreName
	 * @return
	 */
	public static SolrServer getLocalServer(String coreName) {
		if (container == null) {
			init();
		}
		SolrServer server = new EmbeddedSolrServer(container, coreName);
		return server;
	}
	
	static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

		public java.security.cert.X509Certificate[] getAcceptedIssuers()

				{

			return null;

		}

		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)

				{

		}

		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)

				{

		}
	} };
	
	static class NullHostnameVerifier implements HostnameVerifier {
	    public boolean verify(String hostname, SSLSession session) {
	        return true;
	    }
	}

	/**
	 * 使用遠端 Server
	 * 
	 * @param coreUrl 完整包含 coreName 的 URL
	 * @return
	 */
	public static SolrServer getRemoteServer(String coreUrl) {
		if (myHttpClient == null) {
			try {
				RequestConfig defaultRequestConfig = RequestConfig.custom().setConnectTimeout(5000)
						.setSocketTimeout(20000).build();
				
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, trustAllCerts, new java.security.SecureRandom());

				myHttpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig)
						.setHostnameVerifier(new AllowAllHostnameVerifier()).setSslcontext(sc).build();
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
		HttpSolrServer server = new HttpSolrServer(coreUrl, myHttpClient);
		// server.setConnectionTimeout(10000);
		// server.setSoTimeout(10000);
		// server.setMaxRetries(1);
		// server.setFollowRedirects(true);
		return server;
	}

	/**
	 * 透過 synchronized 防止真正 production server 一啟動就有太多 connection 連線,導致
	 * CoreContainer initialize 太多次（會出現奇怪的現象...）
	 */
	synchronized private static void init() {
		if (isCloudMode()) {
			System.out.println("***************** [ZK :: " + System.getProperty("zkHost") + " ] ******************");
			CloudSolrServer cloudServer = ((CloudSolrServer)getServer());
			
			final ZkStateReader zkReader = cloudServer.getZkStateReader();
			final SolrZkClient zkClient = cloudServer.getZkStateReader().getZkClient();
			
			/**
			 * ZooKeeper Watcher for Live Nodes monitoring, 
			 * and dynamic update firewall rules in the SolrDispatcherFilter.setLiveNodes()
			 */
			synchronized (zkReader.getUpdateLock()) {
				try {
					List<String> liveNodes = zkClient.getChildren(ZkStateReader.LIVE_NODES_ZKNODE, new Watcher() {

						@Override
						public void process(WatchedEvent event) {
							// session events are not change events,
							// and do not remove the watcher
							if (EventType.None.equals(event.getType())) {
								return;
							}
							try {
								// delayed approach
								// ZkStateReader.this.updateClusterState(false,
								// true);
								List<String> liveNodes = zkClient.getChildren(ZkStateReader.LIVE_NODES_ZKNODE,
										this, true);
								System.out.println("Updating live nodes (Watcher)... " + liveNodes.size());

								Set<String> uris = new HashSet<String>();
								for (String nodeName : liveNodes) {
									int idx = -1;
									if ((idx = nodeName.indexOf(":")) != -1) {
										String ip = nodeName.substring(0, idx);
										System.out.println("Live nodes: " + ip);
										uris.add(ip);
									}
								}
								SolrDispatchFilter.setLiveNodes(uris);
							}
							catch (KeeperException e) {
								if (e.code() == KeeperException.Code.SESSIONEXPIRED
										|| e.code() == KeeperException.Code.CONNECTIONLOSS) {
									System.out.println("ZooKeeper watch triggered, but Solr cannot talk to ZK");
									return;
								}
								e.printStackTrace();
								throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR, "", e);
							}
							catch (InterruptedException e) {
								// Restore the interrupted status
								Thread.currentThread().interrupt();
								System.out.println(e.getMessage());
								return;
							}
						}

					}, true);
					

					
					Set<String> uris = new HashSet<String>();
					for (String nodeName: liveNodes) {
						int idx = -1;
						if ( (idx = nodeName.indexOf(":")) != -1) {
							String ip = nodeName.substring(0, idx);
							System.out.println("Live nodes: " + ip);
							uris.add(ip);
						}
					}
					SolrDispatchFilter.setLiveNodes(uris);
				}
				catch (KeeperException e) {
					e.printStackTrace();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		if (container == null) {
			try {
				container = new CoreContainer();
				container.load();
				System.out
						.println("****************** Initialize Core Container ******************");
				
				if (WiseSystemConfig.get().getLbModeEnable()) {
					ClusterMembershipListener.getInstance().checkAndSwitchMode();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Keep the same coreContainer reference between SolrDispatchFilter and SearchManager.
	 * By doing this, we can prevent some non-synchronized situation after core administration.
	 * @param cores
	 */
	public static void setLocalCores(CoreContainer cores) {
		container = cores;
		init();
	}

	public static CoreContainer getLocalCores() {
		if (container == null) {
			init();
		}
		return container;
	}

	public static boolean commit(String collectionName) {
		try {
			getServer(collectionName).commit();
			return true;
		} 
		catch (SolrException e) {
		    if (isCloudMode()) {// && collectionName.matches(".*?_shard[0-9]+_replica[0-9]+")) {
		        collectionName = StringUtils.substringBefore(collectionName, "_shard");
	            try {
                    getServer(collectionName).commit();
                    return true;
                }
                catch (SolrServerException | IOException e1) {
                    e1.printStackTrace();
                }
		    }
		    else {
		        e.printStackTrace();
		    }
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
        catch (SolrServerException e) {
            e.printStackTrace();
        }
		return false;
	}

	/**
	 * 傳回 coreN 的數量 (N為數字) 但不包含 core-XXXX (所以 core-keyword 不會算在裡頭)
	 * 
	 * @return
	 */
	public static int getCoreCount() {
		if (container == null) {
			init();
		}

		int coreCount = 0;
		for (Iterator<String> itr = getAllCoreNames().iterator(); itr
				.hasNext();) {
			String coreName = itr.next();
			if (coreName.matches("core[0-9]+")) {
				coreCount++;
			}
		}
		return coreCount;// container.getCores().size();
	}
	

	/**
	 * @deprecated After SolrCloud added, the concept of multi cores should be convert to collections 
	 * @return
	 */
	public static List<String> getAllCoreNames() {
		return getAllCollectionNames();
	}
	
	/**
	 * @return
	 */
	public static List<String> getAllCollectionNames() {
		if (container == null) {
			init();
		}

		List<String> collectionNames = new ArrayList<String>();
		if (isCloudMode()) {
			CollectionsAdminUtils cautil = new CollectionsAdminUtils(getClusterState());
            collectionNames.addAll(cautil.listCollectionNames());
		}
		else {
			for (Iterator<SolrCore> itr = container.getCores().iterator(); itr.hasNext();) {
				SolrCore core = itr.next();
				collectionNames.add(core.getName());
			}
		}
		return collectionNames;
	}

	// 略過*:* [ TO ]等query
	static String containsSolrWords = ".*( TO |[\\[\\]\\*:]).*";

	public static class Entry {
		String key;
		Integer value;

		public Entry(String key, Integer value) {
			super();
			this.key = key;
			this.value = value;
		}

		public void inc() {
			value = value + 1;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return key + ":" + value;
		}
	}

	public static Map<String,String> getColumnNames() {
		if (container == null) {
			init();
		}

		List<String> coreNames = new ArrayList<String>();
		Map<String,String> result = new TreeMap<String,String>();
		HashSet<String> fields = new HashSet<String>();
		for (Iterator<SolrCore> itr = container.getCores().iterator(); itr
				.hasNext();) {
			SolrCore core = itr.next();
			if (core.getName().contains("-"))
				continue;
			SolrIndexSearcher search = core.getSearcher().get();
			ArrayList<String> fieldNames = new ArrayList<String>(search.getFieldNames());
			fields.addAll(fieldNames);
		}
		for(String field:fields){
			result.put(field, ColumnNameMappingFacade.getInstance().getMappingName(field));
		}
		return result;
	}
	
	public static ClusterState getClusterState() {
		if (isCloudMode()) {
			if (cloudServers.size() > 0) {
				return ((CloudSolrServer)getServer()).getZkStateReader().getClusterState();
			}
		}
		
		return null;
	}
	
	static Map<String, String> coreUrl4WriteMap = new HashMap<String, String>();
	public static void setCoreUrl4Write(String coreName, String masterUrl) {
		coreUrl4WriteMap.put(coreName, masterUrl);
		
		System.out.println("** coreUrl4WriteMap **");
		System.out.println(coreUrl4WriteMap);
	}
	public static String getCoreUrl4Write(String coreName) {
		return coreUrl4WriteMap.get(coreName);
	}
	
	
	/**
	 * Elasticsearch Area
	 */
	static Node esNode = null;
	
	public static void initESNode() {
		if (isESClientOnlyMode()) {
			esNode = nodeBuilder()
				.settings(
						ImmutableSettings.settingsBuilder()
//							.put("path.home", "../kernel/es")
							.put("index.percolator.map_unmapped_fields_as_string", true)
							.put("http.enabled", false)
						)
				.data(false)
		        .client(true)
				.node();
		}
		else {
			String clusterName = System.getProperty("esClusterName");
			Boolean enableESHttpModule = Boolean.parseBoolean(StringUtils.defaultString(System.getProperty("esEnableHttp"), "true"));
			String homePath = WiSeEnv.getHomePath();
			Builder settings = ImmutableSettings.settingsBuilder()
					.put("path.home", homePath + "/es")
					.put("index.percolator.map_unmapped_fields_as_string", true)
					.put("http.enabled", enableESHttpModule);
			
			if (clusterName != null) {
				esNode = nodeBuilder()
					.clusterName(clusterName)
					.settings(settings)
					.node();
			}
			else {
				esNode = nodeBuilder()
					.settings(settings)
					.node();
			}
		}
	}
	
	public static Client getESClient() {
		return esNode != null ? esNode.client() : null;
	}

	public static void closeESNode() {
		if (esNode != null)
			esNode.close();
	}

}
